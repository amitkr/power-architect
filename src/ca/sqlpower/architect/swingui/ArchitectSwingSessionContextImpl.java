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
package ca.sqlpower.architect.swingui;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.ImageIcon;
import javax.swing.JDialog;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectSession;
import ca.sqlpower.architect.ArchitectSessionContext;
import ca.sqlpower.architect.ArchitectSessionContextImpl;
import ca.sqlpower.architect.CoreUserSettings;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.Olap4jDataSource;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.swingui.SPSUtils;
import ca.sqlpower.swingui.db.DataSourceDialogFactory;
import ca.sqlpower.swingui.db.DataSourceTypeDialogFactory;
import ca.sqlpower.swingui.db.DatabaseConnectionManager;
import ca.sqlpower.swingui.dbtree.SQLObjectSelection;
import ca.sqlpower.swingui.event.SessionLifecycleEvent;
import ca.sqlpower.swingui.event.SessionLifecycleListener;

/**
 * Instances of this class provide the basic global (non-project-specific) settings
 * and facilities to an invocation of the Architect's Swing user interface.  You
 * need an instance of one of these in order to start the Architect's Swing UI.
 * <p>
 * It may one day be desirable for this to be an interface, but there didn't seem
 * to be a need for it when we first created this class.
 */
public class ArchitectSwingSessionContextImpl implements ArchitectSwingSessionContext, ClipboardOwner {
    
    private static final Logger logger = Logger.getLogger(ArchitectSwingSessionContextImpl.class);
    
    private static final boolean MAC_OS_X = (System.getProperty("os.name").toLowerCase().startsWith("mac os x")); //$NON-NLS-1$ //$NON-NLS-2$
    
    /**
     * This dummy transferable is placed on the local clipboard if the system 
     * clipboard is lost. This is used instead of a null value as setting
     * the local clipboard to have null as its content causes an NPE.
     */
    private class DummyTransferable implements Transferable {

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            // TODO Auto-generated method stub
            return null;
        }

        public DataFlavor[] getTransferDataFlavors() {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            // TODO Auto-generated method stub
            return false;
        }
        
    }
    
    /**
     * The dummy transferable to place on the local clipboard to avoid an
     * NPE when setting the contents to null.
     */
    private final Transferable dummyTransferable = new DummyTransferable();
    
    /**
     * A more structured interface to the prefs node.  Might be going away soon.
     */
    CoreUserSettings userSettings;
    
    /**
     * This is the context that some work delegates to.
     */
    private ArchitectSessionContext delegateContext;
   
    /**
     * The database connection manager GUI for this session context (because all sessions
     * share the same set of database connections).
     */
    private final DatabaseConnectionManager dbConnectionManager;

    /**
     * The Preferences editor for this application context.
     */
    private final PreferencesEditor prefsEditor;
    
    /**
     * This internal clipboard allows copying and pasting objects within
     * the app to stay as objects. The system clipboard throws modification
     * exceptions when it is used with SQLObjects.
     */
    private final Clipboard clipboard = new Clipboard("Internal clipboard");
    
    /**
     * This factory just passes the request through to the {@link ASUtils#showDbcsDialog(Window, SPDataSource, Runnable)}
     * method.
     */
    private final DataSourceDialogFactory dsDialogFactory = new DataSourceDialogFactory() {

        public JDialog showDialog(Window parentWindow, JDBCDataSource dataSource, Runnable onAccept) {
            return ASUtils.showDbcsDialog(parentWindow, dataSource, onAccept);
        }

        public JDialog showDialog(Window parentWindow, Olap4jDataSource dataSource,
                DataSourceCollection<? super JDBCDataSource> dsCollection, Runnable onAccept) {
            throw new UnsupportedOperationException("There is no editor dialog for Olap4j connections in Architect.");
        }
        
    };
    
    /**
     * This factory just passes the request through to the {@link ASUtils#showDbcsDialog(Window, SPDataSource, Runnable)}
     * method.
     */
    private final DataSourceTypeDialogFactory dsTypeDialogFactory = new DataSourceTypeDialogFactory() {
        public Window showDialog(Window owner) {
            return prefsEditor.showJDBCDriverPreferences(owner, ArchitectSwingSessionContextImpl.this);
        }
    };

    /**
     * Creates a new session context.  You will normally only need one of these
     * per JVM, but there is no technical barrier to creating multiple contexts.
     * <p>
     * Important note: This constructor must be called on the Swing Event Dispatch
     * Thread.  See SwingUtilities.invokeLater() for a way of ensuring this method
     * is called on the proper thread.
     * 
     * @throws SQLObjectException 
     */
    public ArchitectSwingSessionContextImpl() throws SQLObjectException {
        delegateContext = new ArchitectSessionContextImpl();
        
        System.setProperty("apple.laf.useScreenMenuBar", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        
        userSettings = new CoreUserSettings(getPrefs());

        // this doesn't appear to have any effect on the motion threshold
        // in the Playpen, but it does seem to work on the DBTree...
        logger.debug("current motion threshold is: " + System.getProperty("awt.dnd.drag.threshold")); //$NON-NLS-1$ //$NON-NLS-2$
        System.setProperty("awt.dnd.drag.threshold","10"); //$NON-NLS-1$ //$NON-NLS-2$
        logger.debug("new motion threshold is: " + System.getProperty("awt.dnd.drag.threshold")); //$NON-NLS-1$ //$NON-NLS-2$

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        
        dbConnectionManager = new DatabaseConnectionManager(getPlDotIni(), dsDialogFactory,dsTypeDialogFactory);
        prefsEditor = new PreferencesEditor();

        // sets the icon so exception dialogs handled by SPSUtils instead
        // of ASUtils can still have the correct icon
        SPSUtils.setMasterIcon(new ImageIcon(ASUtils.getFrameIconImage()));
        
        logger.debug("toolkit has system clipboard " + Toolkit.getDefaultToolkit().getSystemClipboard());
        clipboard.setContents(dummyTransferable, this);
    }
    
    /**
     * Loads the XML project description from the input stream,
     * optionally creating the GUI for you.
     * <p>
     * <b>Important Note:</b> If you set showGUI to true, this method
     * must be called on the Swing Event Dispatch Thread.  If this is
     * not possible or practical, call this method with showGUI false,
     * then call {@link ArchitectSwingSession#initGUI()} on the returned
     * session using the event dispatch thread some time later on.
     * @throws IOException If the file is not found or can't be read.
     * @throws SQLObjectException if there is some problem with the file
     * @throws IllegalStateException if showGUI==true and this method was
     * not called on the Event Dispatch Thread.
     */
    public ArchitectSwingSession createSession(InputStream in, boolean showGUI) throws SQLObjectException, IOException {
        ArchitectSwingSession session = createSessionImpl(Messages.getString("ArchitectSwingSessionContextImpl.projectLoadingDialogTitle"), false, null); //$NON-NLS-1$
        
        try {
            session.getProject().load(in, getPlDotIni());

            if (showGUI) {
                session.initGUI();
            }
        
            return session;
        } catch (SQLObjectException ex) {
            try {
                session.close();
            } catch (Exception e) {
                logger.error("Session cleanup failed after botched read. Eating this secondary exception:", e); //$NON-NLS-1$
            }
            throw ex;
        } catch (IOException ex) {
            try {
                session.close();
            } catch (Exception e) {
                logger.error("Session cleanup failed after botched read. Eating this secondary exception:", e); //$NON-NLS-1$
            }
            throw ex;
        } catch (Exception ex) {
            try {
                session.close();
            } catch (Exception e) {
                logger.error("Session cleanup failed after botched read. Eating this secondary exception:", e); //$NON-NLS-1$
            }
            throw new RuntimeException(ex);
        }
    }
    
    /* javadoc inherited from interface */
    public ArchitectSwingSession createSession() throws SQLObjectException {
        return createSession(true);
    }

    /* javadoc inherited from interface */
    public ArchitectSwingSession createSession(boolean showGUI) throws SQLObjectException {
        return createSessionImpl(Messages.getString("ArchitectSwingSessionContextImpl.defaultNewProjectName"), showGUI, null); //$NON-NLS-1$
    }
    
    /* javadoc inherited from interface */
    public ArchitectSwingSession createSession(InputStream in) throws SQLObjectException, IOException {
        return createSession(in, true);
    }

    public ArchitectSwingSession createSession(ArchitectSwingSession openingSession) throws SQLObjectException {
        return createSessionImpl(Messages.getString("ArchitectSwingSessionContextImpl.defaultNewProjectName"), true, openingSession); //$NON-NLS-1$
    }

    /**
     * This is the one createSession() implementation to which all other
     * overloads of createSession() actually delegate their work.
     * <p>
     * This method tracks all sessions that have been successfully created in
     * the {@link #sessions} field.
     * 
     * @param projectName
     *            The name of the project being opened in the new sesssion
     * @param showGUI
     *            If true, then displays the GUI. If false, do not show the GUI
     * @param openingSession
     *            If showGUI is true, then positions the new session window
     *            relative to the openingSession's window. If null, then just
     *            positions the new windows according to the most recently
     *            stored user preference.
     * @return An new ArchitectSwingSession with the given project name.
     * @throws SQLObjectException
     * @throws IllegalStateException
     *             if showGUI==true and this method was not called on the Event
     *             Dispatch Thread.
     */
    private ArchitectSwingSession createSessionImpl(String projectName, boolean showGUI, ArchitectSwingSession openingSession) throws SQLObjectException {
        logger.debug("About to create a new session for project \"" + projectName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
        ArchitectSwingSession session = new ArchitectSwingSessionImpl(this, projectName);
        getSessions().add(session);
        session.addSessionLifecycleListener(sessionLifecycleListener);
        
        if (showGUI) {
            logger.debug("Creating the Architect frame..."); //$NON-NLS-1$
            session.initGUI(openingSession);
        
            if (openingSession == null && getSessions().size() == 1) {
                showWelcomeScreen(session.getArchitectFrame());
            }
        }
        
        return session;
    }
    
    /**
     * Removes the closed session from the list, and terminates the VM
     * if there are no more sessions.
     */
    private SessionLifecycleListener<ArchitectSwingSession> sessionLifecycleListener =
        new SessionLifecycleListener<ArchitectSwingSession>() {
        public void sessionClosing(SessionLifecycleEvent<ArchitectSwingSession> e) {
            getSessions().remove(e.getSource());
            if (getSessions().isEmpty() && exitAfterAllSessionsClosed) {
                System.exit(0);
            }
        }
    };

    /**
     * Defaults to false, which is required by the interface spec.
     */
    private boolean exitAfterAllSessionsClosed = false;

    /* (non-Javadoc)
     * @see ca.sqlpower.architect.swingui.ArchitectSwingSessionContext#isMacOSX()
     */
    public boolean isMacOSX() {
        return MAC_OS_X;
    }

    /* (non-Javadoc)
     * @see ca.sqlpower.architect.swingui.ArchitectSwingSessionContext#getPrefs()
     */
    public Preferences getPrefs() {
        return delegateContext.getPrefs();
    }
    
    /* (non-Javadoc)
     * @see ca.sqlpower.architect.swingui.ArchitectSwingSessionContext#getUserSettings()
     */
    public CoreUserSettings getUserSettings() {
        return userSettings;
    }

    public Collection<ArchitectSession> getSessions() {
        return delegateContext.getSessions();
    }
    
    private void showWelcomeScreen(Component dialogOwner) {
        // should almost certainly move this into the swing context
        if (getUserSettings().getSwingSettings().getBoolean(ArchitectSwingUserSettings.SHOW_WELCOMESCREEN, true)) {
            WelcomeScreen ws = new WelcomeScreen(this);
            ws.showWelcomeDialog(dialogOwner);
        }
    }

    public void showConnectionManager(Window owner) {
        dbConnectionManager.showDialog(owner);
    }

    public void showPreferenceDialog(Window owner) {
        prefsEditor.showPreferencesDialog(owner, ArchitectSwingSessionContextImpl.this);
    }
    
    /**
     * Attempts to close all sessions that were created by this context.  The
     * user might abort some or all of the session closes by choosing to cancel
     * when the "prompt for unsaved modifications" step happens.
     */
    public void closeAll() {
        List<ArchitectSession> doomedSessions =
            new ArrayList<ArchitectSession>(getSessions());
        
        for (ArchitectSession s : doomedSessions) {
            ((ArchitectSwingSession) s).close();
        }
    }

    public boolean getExitAfterAllSessionsClosed() {
        return exitAfterAllSessionsClosed;
    }

    public void setExitAfterAllSessionsClosed(boolean allowExit) {
        exitAfterAllSessionsClosed = allowExit;
    }

    public List<JDBCDataSource> getConnections() {
        return delegateContext.getConnections();
    }

    public DataSourceCollection getPlDotIni() {
        return delegateContext.getPlDotIni();
    }

    public String getPlDotIniPath() {
        return delegateContext.getPlDotIniPath();
    }

    public void setPlDotIniPath(String plDotIniPath) {
        delegateContext.setPlDotIniPath(plDotIniPath);
    }
    
    public Transferable getClipboardContents() {
        logger.debug("local clipboard contents are " + clipboard.getContents(null));
        if (clipboard.getContents(null) != dummyTransferable) {
            logger.debug("Getting clipboard contents from local clipboard");
            return clipboard.getContents(null);
        }
        logger.debug("Getting clipboard contents from system");
        return Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
    }
    
    public void setClipboardContents(Transferable t) {
        clipboard.setContents(t, this);
        logger.debug("Setting local clipboard contents");
        if (t instanceof SQLObjectSelection) {
            ((SQLObjectSelection) t).setLocal(false);
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, this);
        logger.debug("toolkit pasting to system clipboard " + Toolkit.getDefaultToolkit().getSystemClipboard());
        if (t instanceof SQLObjectSelection) {
            ((SQLObjectSelection) t).setLocal(true);
        }
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        this.clipboard.setContents(dummyTransferable, this);
        logger.debug("Context lost clipboard ownership");
    }
}
