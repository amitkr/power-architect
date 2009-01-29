/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.architect.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectSession;
import ca.sqlpower.architect.UserPrompter;
import ca.sqlpower.architect.UserPrompter.UserPromptResponse;
import ca.sqlpower.architect.profile.event.ProfileChangeEvent;
import ca.sqlpower.architect.profile.event.ProfileChangeListener;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLObjectPreEvent;
import ca.sqlpower.sqlobject.SQLObjectPreEventListener;
import ca.sqlpower.sqlobject.SQLTable;

/**
 * The default ProfileManager implementation. Creates profiles of tables,
 * optionally using a separate worker thread.
 * 
 * @version $Id$
 */
public class ProfileManagerImpl implements ProfileManager {
    
    /**
     * Watches the session's root object, and reacts when SQLDatabase items
     * are removed. In that case, it ensures there are no dangling references
     * from the profiled tables back to the removed database or its children.
     * If there are, the user is asked to decide to either cancel the operation
     * or allow the ETL lineage (SQLColumn.sourceColumn) references to be broken.
     */
    private class DatabaseRemovalWatcher implements SQLObjectPreEventListener {

        public void dbChildrenPreRemove(SQLObjectPreEvent e) {
            logger.debug("Pre-remove on profile manager");
            UserPrompter up = session.createUserPrompter(
                    "{0} tables have been profiled from the database {1}.\n" +
                    "\n" +
                    "If you proceed, the profiling information from the database" +
                    " will be removed.",
                    "Remove Profiles", "Keep Profiles", "Cancel");
            for (SQLObject so : e.getChildren()) {
                SQLDatabase db = (SQLDatabase) so;
                List<TableProfileResult> refs = new ArrayList<TableProfileResult>(); 
                for (TableProfileResult tpr : getResults()) {
                    if (tpr.getProfiledObject().getParentDatabase() != null && tpr.getProfiledObject().getParentDatabase().equals(db)) {
                        refs.add(tpr);
                    }
                }
                if (!refs.isEmpty()) {
                    UserPromptResponse response = up.promptUser(refs.size(), db.getName());
                    if (response == UserPromptResponse.OK) {
                        logger.debug("We got the ok to delete.");
                        // disconnect those columns' source columns
                        for (TableProfileResult tpr : refs) {
                            results.remove(tpr);
                        }
                    } else if (response == UserPromptResponse.NOT_OK) {
                        e.veto();
                    } else if (response == UserPromptResponse.CANCEL) {
                        e.veto();
                    }
                }
            }
        }

    }

    private static final Logger logger = Logger.getLogger(ProfileManagerImpl.class);
    
    /**
     * The current list of listeners who want to know when the contents
     * of this profile manager change.
     */
    private final List<ProfileChangeListener> profileChangeListeners = new ArrayList<ProfileChangeListener>();

    /**
     * All profile results in this profile manager. IMPORTANT: Do not modify this list
     * directly. Always use {@link #addResults(List)},
     * {@link #removeResults(List)}, and {@link #clear()}.
     */
    private final List<TableProfileResult> results = new ArrayList<TableProfileResult>();
    
    /**
     * The session this manager is associated with.
     */
    private final ArchitectSession session;
    
    /**
     * The defaults that new profile results will be created with.
     * XXX these are specific to the remote database profiler!
     */
    private ProfileSettings defaultProfileSettings = new ProfileSettings();

    /**
     * The Profile Executor manages the thread that actually does the work
     * of creating the profiles.
     */
    private ExecutorService profileExecutor = Executors.newSingleThreadExecutor();

    /**
     * The creator that will be used to create profiles.
     */
    private TableProfileCreator creator = new RemoteDatabaseProfileCreator(getDefaultProfileSettings());
    
    /**
     * A list of the different existing profile creators that can be used.
     */
    private List<TableProfileCreator> profileCreators = Arrays.asList(
            (TableProfileCreator)new RemoteDatabaseProfileCreator(getDefaultProfileSettings()),
            new LocalReservoirProfileCreator(getDefaultProfileSettings()));
    
    /**
     * A Callable interface which populates a single profile result then returns it.
     * Profile results don't throw the exceptions their populate() methods encounter,
     * but this callable wrapper knows about that, and digs up and rethrows the
     * exceptions encountered by populate().  This is more normal, and is also
     * compatible with the ExecutorService implementation provided in the standard
     * Java library (it handles exceptions and restarts the work queue properly).
     */
    private class ProfileResultCallable implements Callable<TableProfileResult> {
        
        /**
         * The table profile result this Callable populates.
         */
        private TableProfileResult tpr;

        ProfileResultCallable(TableProfileResult tpr) {
            if (tpr == null) throw new NullPointerException("Can't populate a null profile result!");
            this.tpr = tpr;
        }
        
        /**
         * Populates the profile result, throwing any exceptions encountered.
         */
        public TableProfileResult call() throws Exception {
            creator.doProfile(tpr);
            // XXX the creator should stash a reference to the exception in tpr, then throw it anyway 
            if (tpr.getException() != null) {
                throw tpr.getException();
            }
            return tpr;
        }
    }
    
    public ProfileManagerImpl(ArchitectSession session) {
        this.session = session;
        if (session != null && session.getRootObject() != null) {
            session.getRootObject().addSQLObjectPreEventListener(new DatabaseRemovalWatcher());
        }
    }
    
    /**
     * This is the method that everything which wants to add a profile result
     * must call in order to add the result. It takes care of setting SQLObject
     * client properties, firing events, and actually adding the profile to the
     * set of profile results in this profile manager.
     */
    private void addResults(List<TableProfileResult> newResults) {
        results.addAll(newResults);
        fireProfilesAdded(newResults);
        for (TableProfileResult newResult : newResults) {
            SQLTable table = newResult.getProfiledObject();
            table.putClientProperty(ProfileManager.class, PROFILE_COUNT_PROPERTY, getResults(table).size());
        }
    }
    
    /* docs inherited from interface */
    public TableProfileResult createProfile(SQLTable table) throws SQLObjectException {
        TableProfileResult tpr = new TableProfileResult(table, getDefaultProfileSettings());
        addResults(Collections.singletonList(tpr));
        
        try {
            profileExecutor.submit(new ProfileResultCallable(tpr)).get();
            assert (tpr.getProgressMonitor().isFinished());
        } catch (InterruptedException ex) {
            logger.info("Profiling was interrupted (likely because this manager is being shut down)");
        } catch (ExecutionException ex) {
            throw new SQLObjectException("Profile execution failed", ex);
        }
        
        return tpr;
    }

    /* docs inherited from interface */
    public Collection<Future<TableProfileResult>> asynchCreateProfiles(Collection<SQLTable> tables) {
        
        List<TableProfileResult> profiles = new ArrayList<TableProfileResult>();
        for (SQLTable t : tables) {
            profiles.add(new TableProfileResult(t, getDefaultProfileSettings()));
        }
        
        addResults(profiles);
        
        List<Future<TableProfileResult>> results = new ArrayList<Future<TableProfileResult>>();
        for (TableProfileResult tpr : profiles) {
            results.add(scheduleProfile(tpr));
        }
        return results;
    }

    /* docs inherited from interface */
    public Future<TableProfileResult> scheduleProfile(TableProfileResult result) {
        return profileExecutor.submit(new ProfileResultCallable(result));
    }
    
    /* docs inherited from interface */
    public void clear() {
        List<TableProfileResult> oldResults = new ArrayList<TableProfileResult>(results);
        results.clear();
        fireProfilesRemoved(oldResults);
        for (TableProfileResult oldResult : oldResults) {
            SQLTable table = oldResult.getProfiledObject();
            table.putClientProperty(ProfileManager.class, PROFILE_COUNT_PROPERTY, 0);
        }
    }

    /* docs inherited from interface */
    public List<TableProfileResult> getResults() {
        // this could be optimized by caching the current result list snapshot, but enh.
        return Collections.unmodifiableList(new ArrayList<TableProfileResult>(results));
    }

    /* docs inherited from interface */
    public List<TableProfileResult> getResults(SQLTable t) {
        List<TableProfileResult> someResults = new ArrayList<TableProfileResult>();
        for (TableProfileResult tpr : results) {
            if (tpr.getProfiledObject().equals(t)) {
                someResults.add(tpr);
            }
        }
        return Collections.unmodifiableList(someResults);
    }

    /* docs inherited from interface */
    public boolean removeProfile(TableProfileResult victim) {
        boolean removed = results.remove(victim);
        if (removed) {
            fireProfilesRemoved(Collections.singletonList(victim));
        }
        SQLTable table = victim.getProfiledObject();
        table.putClientProperty(ProfileManager.class, PROFILE_COUNT_PROPERTY, getResults(table).size());
        return removed;
    }

    /* docs inherited from interface */
    public ProfileSettings getDefaultProfileSettings() {
        return defaultProfileSettings;
    }

    /* docs inherited from interface */
    public void setDefaultProfileSettings(ProfileSettings settings) {
        defaultProfileSettings = settings;
    }

    /* docs inherited from interface */
    public void setProcessingOrder(List<TableProfileResult> tpr) {
        
    }

    /**
     * This is a hook designed so the SwingUIProject can insert profile results
     * into this profile manager as it is reading in a project file.  It is
     * not appropriate to use otherwise.
     * <p>
     * This method fires an event every time it adds a table profile result, because
     * not doing so makes it necessary to create the profile manager view after
     * loading in the profile results, and it is impossible to guarantee that policy
     * from here.
     * <p>
     * The idea is, the SwingUIProject stores all profile results in a flat space
     * (table and column results are sibling elements) so it needs our help to
     * put everything back together into the original hierarchy.  This method hangs
     * onto all TableProfileResult objects given to it, and ignores all other result
     * types, assuming the client code will do the appropriate hookups.
     * <p>
     * You might be asking yourself, "why not store the profile results in the
     * same hierarchy as they had when they were originally created, so we don't
     * need any more error-prone code to recreate what we already had and then
     * threw away?  Beside reducing bugs, it would eliminate the need for this public
     * method and accompanying docs that warn you against using it."  If so, please
     * apply to SQL Power at hr (@) sqlpower.ca. 
     */
    public void loadResult(ProfileResult pr) {
        if (pr instanceof TableProfileResult) {
            TableProfileResult tpr = (TableProfileResult) pr;
            addResults(Collections.singletonList(tpr));
        }
        // the column results will get added to the table result by
        // the project's profile result factory class
    }
    
    /**
     * Adds the given listener to this profile manager.  The listener will be notified
     * of additions and removals of results in this profile manager.
     */
    public void addProfileChangeListener(ProfileChangeListener listener) {
        profileChangeListeners.add(listener);
    }

    /**
     * Removes the given listener.  After removal, the listener will no longer be notified
     * of changes to this profile manager.
     */
    public void removeProfileChangeListener(ProfileChangeListener listener) {
        profileChangeListeners.remove(listener);
    }

    /**
     * Creates and fires a "profilesAdded" event for the given profile results.
     */
    private void fireProfilesAdded(List<TableProfileResult> results) {
        if (results == null) throw new NullPointerException("Can't fire event for null profile list");
        ProfileChangeEvent e = new ProfileChangeEvent(this, results);
        for (int i = profileChangeListeners.size() - 1; i >= 0; i--) {
            profileChangeListeners.get(i).profilesAdded(e);
        }
    }

    /**
     * Creates and fires a "profilesRemoved" event for the given list of profile results.
     */
    private void fireProfilesRemoved(List<TableProfileResult> removedList) {
        if (removedList == null) throw new NullPointerException("Null list not allowed");
        ProfileChangeEvent e = new ProfileChangeEvent(this, removedList);
        for (int i = profileChangeListeners.size() - 1; i >= 0; i--) {
            profileChangeListeners.get(i).profilesRemoved(e);
        }
    }

    public void close() {
        profileExecutor.shutdown();
    }

    public List<TableProfileCreator> getProfileCreators() {
        return Collections.unmodifiableList(profileCreators);
    }

    public TableProfileCreator getCreator() {
        return creator;
    }

    public void setCreator(TableProfileCreator tpc) {
        this.creator = tpc;
    }

}
