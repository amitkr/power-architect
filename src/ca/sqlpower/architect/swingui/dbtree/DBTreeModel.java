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
package ca.sqlpower.architect.swingui.dbtree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.ArchitectRuntimeException;
import ca.sqlpower.architect.ArchitectUtils;
import ca.sqlpower.architect.SQLExceptionNode;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLObjectEvent;
import ca.sqlpower.architect.SQLObjectListener;
import ca.sqlpower.architect.SQLObjectRoot;
import ca.sqlpower.architect.SQLRelationship;
import ca.sqlpower.swingui.SPSUtils;

public class DBTreeModel implements TreeModel, SQLObjectListener, java.io.Serializable {

	private static Logger logger = Logger.getLogger(DBTreeModel.class);

    /**
     * Controls this model's "testing" mode.  When in testing mode,
     * the checks for whether or not events are on the Swing Event Dispatch
     * Thread are bypassed.
     */
    private boolean testMode = false;
    
	protected SQLObject root;

	/**
	 * Creates a tree model with all of the SQLDatabase objects in the
	 * given session's root object in its root list of databases.
	 *
	 * @param root A SQLObject that contains all the databases you
	 * want in the tree.  This does not necessarily have to be the
	 * root object associated with the given session, but it normally
	 * will be.
	 */
	public DBTreeModel(SQLObjectRoot root) throws ArchitectException {
		this.root = root;
		this.treeModelListeners = new LinkedList();
		ArchitectUtils.listenToHierarchy(this, root);
	}

	public Object getRoot() {
		if (logger.isDebugEnabled()) logger.debug("DBTreeModel.getRoot: returning "+root); //$NON-NLS-1$
		return root;
	}

	public Object getChild(Object parent, int index) {
		if (logger.isDebugEnabled()) logger.debug("DBTreeModel.getChild("+parent+","+index+")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try {
			if (logger.isDebugEnabled()) logger.debug("returning "+((SQLObject) parent).getChild(index)); //$NON-NLS-1$
			return ((SQLObject) parent).getChild(index);
		} catch (Exception e) {
			SQLExceptionNode fakeChild = putExceptionNodeUnder((SQLObject) parent, e);
			return fakeChild;
		}
	}

	public int getChildCount(Object parent) {
		if (logger.isDebugEnabled()) logger.debug("DBTreeModel.getChildCount("+parent+")"); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			if (logger.isDebugEnabled()) logger.debug("returning "+((SQLObject) parent).getChildCount()); //$NON-NLS-1$
			return ((SQLObject) parent).getChildCount();
		} catch (Exception e) {
			putExceptionNodeUnder((SQLObject) parent, e);
			return 1; // XXX: could be incorrect if exception was not a populate problem!
		}
	}

	public boolean isLeaf(Object parent) {
		if (logger.isDebugEnabled()) logger.debug("DBTreeModel.isLeaf("+parent+"): returning "+!((SQLObject) parent).allowsChildren()); //$NON-NLS-1$ //$NON-NLS-2$
		return !((SQLObject) parent).allowsChildren();
	}

	public void valueForPathChanged(TreePath path, Object newValue) {
		throw new UnsupportedOperationException("model doesn't support editting yet"); //$NON-NLS-1$
	}

	public int getIndexOfChild(Object parent, Object child) {
		try {
			if (logger.isDebugEnabled()) logger.debug("DBTreeModel.getIndexOfChild("+parent+","+child+"): returning "+((SQLObject) parent).getChildren().indexOf(child)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return ((SQLObject) parent).getChildren().indexOf(child);
		} catch (ArchitectException e) {
			//logger.error("Couldn't get index of child "+child, e);
			//return -1;
			throw new ArchitectRuntimeException(e);
		}
	}

	// -------------- treeModel event source support -----------------
	protected LinkedList treeModelListeners;

	public void addTreeModelListener(TreeModelListener l) {		
		treeModelListeners.add(l);
	}

	public void removeTreeModelListener(TreeModelListener l) {
		treeModelListeners.remove(l);
	}

	protected void fireTreeNodesInserted(TreeModelEvent e) {
		if (logger.isDebugEnabled()) logger.debug("Firing treeNodesInserted event: "+e); //$NON-NLS-1$
		final TreeModelEvent ev =e; 
		Runnable notifier = new Runnable(){
			public void run() {
				Iterator it = treeModelListeners.iterator();
				while (it.hasNext()) {
					((TreeModelListener) it.next()).treeNodesInserted(ev);
				}
			}
		};
		// TODO FIXME XXX Replace this with an alternate method leads to nasty behavior.  There are 3 others too
			notifier.run();
		
	}
	
	protected void fireTreeNodesRemoved(TreeModelEvent e) {
		if (logger.isDebugEnabled()) logger.debug("Firing treeNodesRemoved event "+e); //$NON-NLS-1$
		final TreeModelEvent ev =e; 
		Runnable notifier = new Runnable(){
			public void run() {
				Iterator it = treeModelListeners.iterator();
				while (it.hasNext()) {
					((TreeModelListener) it.next()).treeNodesRemoved(ev);
				}
			}
		};
//		 TODO FIXME XXX Replace this with an alternate method leads to nasty behavior.  There are 3 others too
		notifier.run();
		
	}

	protected void fireTreeNodesChanged(TreeModelEvent e) {
		final TreeModelEvent ev =e; 
		Runnable notifier = new Runnable(){
			public void run() {
				Iterator it = treeModelListeners.iterator();
				while (it.hasNext()) {
					((TreeModelListener) it.next()).treeNodesChanged(ev);
				}
			}
		};
//		 TODO FIXME XXX Replace this with an alternate method leads to nasty behavior.  There are 3 others too
		notifier.run();
		
		
	}

	protected void fireTreeStructureChanged(TreeModelEvent e) {
		logger.debug("firing TreeStructuredChanged. source="+e.getSource()); //$NON-NLS-1$
		final TreeModelEvent ev =e; 		
		Runnable notifier = new Runnable(){
			public void run() {
				Iterator it = treeModelListeners.iterator();
				while (it.hasNext()) {
					((TreeModelListener) it.next()).treeStructureChanged(ev);
				}
			}
		};
//		 TODO FIXME XXX Replace this with an alternate method leads to nasty behavior.  There are 3 others too
			notifier.run();
		
	}

	/**
	 * Returns the path from the conceptual, hidden root node (of type
	 * DBTreeRoot) to the given node.
	 * 
	 * <p>NOTE: This method doesn't work for SQLRelationship objects,
	 * because they have two parents! Use getPkPathToRelationship and
	 * getFkPathToRelationship instead.
	 *
	 * @throws IllegalArgumentException if <code>node</code> is of class SQLRelationship.
	 */
	public SQLObject[] getPathToNode(SQLObject node) {
		if (node instanceof SQLRelationship) {
			throw new IllegalArgumentException("This method does not work for SQLRelationship. Use getPkPathToRelationship() and getFkPathToRelationship() instead."); //$NON-NLS-1$
		}
		LinkedList path = new LinkedList();
		while (node != null && node != root) {
			path.add(0, node);
			node = node.getParent();
		}
		path.add(0, root);
		return (SQLObject[]) path.toArray(new SQLObject[path.size()]);
	}

	public SQLObject[] getPkPathToRelationship(SQLRelationship rel) {
		SQLObject[] pathToPkTable = getPathToNode(rel.getPkTable());
		SQLObject[] path = new SQLObject[pathToPkTable.length + 2];
		System.arraycopy(pathToPkTable, 0, path, 0, pathToPkTable.length);
        path[path.length - 2] = rel.getPkTable().getExportedKeysFolder();
        path[path.length - 1] = rel;
		return path;
	}

	public SQLObject[] getFkPathToRelationship(SQLRelationship rel) {
		SQLObject[] pathToFkTable = getPathToNode(rel.getFkTable());
		SQLObject[] path = new SQLObject[pathToFkTable.length + 2];
		System.arraycopy(pathToFkTable, 0, path, 0, pathToFkTable.length);
        path[path.length - 2] = rel.getFkTable().getImportedKeysFolder();
		path[path.length - 1] = rel;
		return path;
	}
	
	/**
     * Returns the path from the conceptual, hidden root node (of type
     * DBTreeRoot) to the given node.
     * 
     * If the node is not a relationship then the list will only contain
     * one path to the object. Otherwise the list will contain the path
     * to the primary key then the path to the foreign key.
	 */
	public List<SQLObject[]> getPathsToNode(SQLObject node) {
	    List<SQLObject[]> nodePaths = new ArrayList<SQLObject[]>();
	    if (node instanceof SQLRelationship) {
	        SQLRelationship rel = (SQLRelationship) node;
	        nodePaths.add(getPkPathToRelationship(rel));
	        nodePaths.add(getFkPathToRelationship(rel));
	    } else {
	        nodePaths.add(getPathToNode(node));
	    }
	    return nodePaths;
	}

	/**
	 * Creates a SQLExceptionNode with the given Throwable and places
	 * it under parent.
	 *
	 * @return the node that has been added to parent.
	 */
	protected SQLExceptionNode putExceptionNodeUnder(final SQLObject parent, Throwable ex) {
		// dig for root cause and message
		logger.info("Adding exception node under "+parent, ex); //$NON-NLS-1$
		String message = ex.getMessage();
		Throwable cause = ex;
		while (cause.getCause() != null) {
			cause = cause.getCause();
			if (cause.getMessage() != null && cause.getMessage().length() > 0) {
				message = cause.getMessage();
			}
		}
		
		if (message == null || message.length() == 0) {
			message = "Check application log for details"; //$NON-NLS-1$
		}
		
		final SQLExceptionNode excNode = new SQLExceptionNode(ex, message);
		excNode.setParent((SQLObject) parent);

		/* This is likely to fail, but it should convince the parent that it is populated */
		try {
			parent.getChildCount();
		} catch (ArchitectException e) {
			logger.error("Couldn't populate parent node of exception"); //$NON-NLS-1$
		}

		try {
			for(int i=0; i< parent.getChildCount(); i++){
				parent.removeChild(0);
			}
			parent.addChild(excNode);
		} catch (ArchitectException e) {
			logger.error("Couldn't add SQLExceptionNode \""+excNode.getName()+"\" to tree model:", e); //$NON-NLS-1$ //$NON-NLS-2$
			SPSUtils.showExceptionDialogNoReport("Failed to add SQLExceptionNode to tree model.", e); //$NON-NLS-1$
		}
		return excNode;
	}

	// --------------------- SQLObject listener support -----------------------
	public void dbChildrenInserted(SQLObjectEvent e) {
        if (logger.isDebugEnabled()) {
            logger.debug("dbChildrenInserted. source="+e.getSource() //$NON-NLS-1$
                    +" indices: "+Arrays.asList(e.getChangedIndices()) //$NON-NLS-1$
                    +" children: "+Arrays.asList(e.getChildren())); //$NON-NLS-1$
        }
		if (logger.isDebugEnabled()) {
			if (e.getSQLSource() instanceof SQLRelationship) {
				SQLRelationship r = (SQLRelationship) e.getSQLSource();
				logger.debug("dbChildrenInserted SQLObjectEvent: "+e //$NON-NLS-1$
							 +"; pk path="+Arrays.asList(getPkPathToRelationship(r)) //$NON-NLS-1$
							 +"; fk path="+Arrays.asList(getFkPathToRelationship(r))); //$NON-NLS-1$
			} else {
				logger.debug("dbChildrenInserted SQLObjectEvent: "+e //$NON-NLS-1$
							 +"; tree path="+Arrays.asList(getPathToNode(e.getSQLSource()))); //$NON-NLS-1$
			}
		}
		try {
			SQLObject[] newEventSources = e.getChildren();
			for (int i = 0; i < newEventSources.length; i++) {
				ArchitectUtils.listenToHierarchy(this, newEventSources[i]);
			}
		} catch (ArchitectException ex) {
			logger.error("Error listening to added object", ex); //$NON-NLS-1$
		}

        if ((!SwingUtilities.isEventDispatchThread()) && (!testMode)) {
            logger.debug("Not refiring because this is not the EDT."); //$NON-NLS-1$
            return;
        }

		// relationships have two parents (pktable and fktable) so we need to fire two TMEs
		if (e.getSQLSource() instanceof SQLRelationship) {
			TreeModelEvent tme = new TreeModelEvent
				(this,
				 getPkPathToRelationship((SQLRelationship) e.getSQLSource()),
				 e.getChangedIndices(),
				 e.getChildren());
			fireTreeNodesInserted(tme);

			tme = new TreeModelEvent
				(this,
				 getFkPathToRelationship((SQLRelationship) e.getSQLSource()),
				 e.getChangedIndices(),
				 e.getChildren());
			fireTreeNodesInserted(tme);
		} else {
			TreeModelEvent tme = new TreeModelEvent
				(this,
				 getPathToNode(e.getSQLSource()),
				 e.getChangedIndices(),
				 e.getChildren());
			fireTreeNodesInserted(tme);
		}
	}

	public void dbChildrenRemoved(SQLObjectEvent e) {
        if (logger.isDebugEnabled()) {
            logger.debug("dbchildrenremoved. source="+e.getSource() //$NON-NLS-1$
                    +" indices: "+Arrays.asList(e.getChangedIndices()) //$NON-NLS-1$
                    +" children: "+Arrays.asList(e.getChildren())); //$NON-NLS-1$
        }
		if (logger.isDebugEnabled()) logger.debug("dbChildrenRemoved SQLObjectEvent: "+e); //$NON-NLS-1$
		try {
			SQLObject[] oldEventSources = e.getChildren();
			for (int i = 0; i < oldEventSources.length; i++) {
				ArchitectUtils.unlistenToHierarchy(this, oldEventSources[i]);
			}
		} catch (ArchitectException ex) {
			logger.error("Error unlistening to removed object", ex); //$NON-NLS-1$
		}

        if ((!SwingUtilities.isEventDispatchThread()) && (!testMode)) {
            logger.debug("Not refiring because this is not the EDT."); //$NON-NLS-1$
            return;
        }

		if (e.getSQLSource() instanceof SQLRelationship) {
			TreeModelEvent tme = new TreeModelEvent
				(this,
				 getPkPathToRelationship((SQLRelationship) e.getSQLSource()),
				 e.getChangedIndices(),
				 e.getChildren());
			fireTreeNodesRemoved(tme);

			tme = new TreeModelEvent
				(this,
				 getFkPathToRelationship((SQLRelationship) e.getSQLSource()),
				 e.getChangedIndices(),
				 e.getChildren());
			fireTreeNodesRemoved(tme);
		} else {
			TreeModelEvent tme = new TreeModelEvent
				(this,
				 getPathToNode(e.getSQLSource()),
				 e.getChangedIndices(),
				 e.getChildren());
			fireTreeNodesRemoved(tme);
		}
	}
	
	public void dbObjectChanged(SQLObjectEvent e) {
		logger.debug("dbObjectChanged. source="+e.getSource()); //$NON-NLS-1$
        if ((!SwingUtilities.isEventDispatchThread()) && (!testMode)) {
            logger.debug("Not refiring because this is not the EDT."); //$NON-NLS-1$
            return;
        }
		if (logger.isDebugEnabled()) logger.debug("dbObjectChanged SQLObjectEvent: "+e); //$NON-NLS-1$
		processSQLObjectChanged(e);
	}

    /**
     * The profile manager needs to fire events from different threads.
     * So we need to get around the check.
     */
    private void processSQLObjectChanged(SQLObjectEvent e) {
        if (e.getPropertyName().equals("name") &&  //$NON-NLS-1$
				!e.getNewValue().equals(e.getSQLSource().getName()) ) {
			logger.error("Name change event has wrong new value. new="+e.getNewValue()+"; real="+e.getSQLSource().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		SQLObject source = e.getSQLSource();
		if (source instanceof SQLRelationship) {
			SQLRelationship r = (SQLRelationship) source;
			fireTreeNodesChanged(new TreeModelEvent(this, getPkPathToRelationship(r)));
			fireTreeNodesChanged(new TreeModelEvent(this, getFkPathToRelationship(r)));
		} else {
			fireTreeNodesChanged(new TreeModelEvent(this, getPathToNode(source)));
		}
    }

	public void dbStructureChanged(SQLObjectEvent e) {
		logger.debug("dbStructureChanged. source="+e.getSource()); //$NON-NLS-1$
		try {			
			ArchitectUtils.listenToHierarchy(this, e.getSQLSource());
		} catch (ArchitectException ex) {
			logger.error("Couldn't listen to hierarchy rooted at "+e.getSQLSource(), ex); //$NON-NLS-1$
		}
        if ((!SwingUtilities.isEventDispatchThread()) && (!testMode)) {
            logger.debug("Not refiring because this is not the EDT."); //$NON-NLS-1$
            return;
        }
		TreeModelEvent tme = new TreeModelEvent(this, getPathsToNode(e.getSQLSource()).get(0));
		fireTreeStructureChanged(tme);
	}
    
    /**
     * Sets the {@link #testMode} flag.
     */
    public void setTestMode(boolean v) {
        testMode = v;
    }
}
