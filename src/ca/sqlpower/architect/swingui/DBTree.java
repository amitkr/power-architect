/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.sqlpower.architect.swingui;

import java.awt.Cursor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.ArchitectUtils;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLDatabase;
import ca.sqlpower.architect.SQLExceptionNode;
import ca.sqlpower.architect.SQLIndex;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLRelationship;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.swingui.SPDataSourcePanel;

public class DBTree extends JTree implements DragSourceListener {
	static Logger logger = Logger.getLogger(DBTree.class);
	
	protected DragSource ds;
	protected JPopupMenu popup;
	protected JMenu connectionsMenu;
	protected SPDataSourcePanel spDataSourcePanel;
	protected NewDBCSAction newDBCSAction;
	protected DBCSPropertiesAction dbcsPropertiesAction;
	protected RemoveDBCSAction removeDBCSAction;
	protected ShowInPlayPenAction showInPlayPenAction;
    protected SetConnAsTargetDB setConnAsTargetDB;

    /**
     * The architect session, so we can access common objects
     */
    private final ArchitectSwingSession session;

	/**
	 * This is set to true when the SPDataSourcePanel is editting a new
	 * connection spec.  The dialog's "ok" and "cancel" button
	 * handlers need to do different things for new and existing
	 * specs.
	 */
	protected boolean panelHoldsNewDBCS;


	// ----------- CONSTRUCTORS ------------

	private DBTree(ArchitectSwingSession session) {
        this.session = session;
		setUI(new MultiDragTreeUI());
		setRootVisible(false);
		setShowsRootHandles(true);
		ds = new DragSource();
		ds.createDefaultDragGestureRecognizer
			(this, DnDConstants.ACTION_COPY, new DBTreeDragGestureListener());

        setConnAsTargetDB = new SetConnAsTargetDB(null);
		newDBCSAction = new NewDBCSAction();
		dbcsPropertiesAction = new DBCSPropertiesAction();
		removeDBCSAction = new RemoveDBCSAction();
		showInPlayPenAction = new ShowInPlayPenAction();
		addMouseListener(new PopupListener());
        setCellRenderer(new DBTreeCellRenderer(session));
	}

	public DBTree(ArchitectSwingSession session, List<SQLDatabase> initialDatabases) throws ArchitectException {
		this(session);
		setDatabaseList(initialDatabases);
	}



	// ----------- INSTANCE METHODS ------------

	public void setDatabaseList(List<SQLDatabase> databases) throws ArchitectException {
		setModel(new DBTreeModel(databases,session));
	}

	/**
	 * Returns a list of all the databases in this DBTree's model.
	 */
	public List getDatabaseList() {
		ArrayList databases = new ArrayList();
		TreeModel m = getModel();
		int dbCount = m.getChildCount(m.getRoot());
		for (int i = 0; i < dbCount; i++) {
			databases.add(m.getChild(m.getRoot(), i));
		}
		return databases;
	}

	/**
     * Before adding a new connection to the SwingUIProject, check to see
     * if it exists as a connection in the project (which means they're in this
     * tree's model).
	 */
	public boolean dbcsAlreadyExists(SPDataSource spec) throws ArchitectException {
		SQLObject so = (SQLObject) getModel().getRoot();
		// the children of the root, if they exists, are always SQLDatabase objects
		Iterator it = so.getChildren().iterator();
		boolean found = false;
		while (it.hasNext() && found == false) {
			SPDataSource dbcs = ((SQLDatabase) it.next()).getDataSource();
			if (spec==dbcs) {
				found = true;
			}
		}
		return found;
	}

	/**
     * Pass in a spec, and look for a duplicate in the list of DBCS objects in
     * User Settings.  If we find one, return a handle to it.  If we don't find
     * one, return null.
	 */
	public SPDataSource getDuplicateDbcs(SPDataSource spec) {
		SPDataSource dup = null;
		boolean found = false;
		Iterator it = session.getUserSettings().getConnections().iterator();
		while (it.hasNext() && found == false) {
			SPDataSource dbcs = (SPDataSource) it.next();
			if (spec.equals(dbcs)) {
				dup = dbcs;
				found = true;
			}
		}
		return dup;
	}


	/**
	 * Creates an integer array which holds the child indices of each
	 * node starting from the root which lead to node "node."
	 *
	 * @param node A node in this tree.
	 */
	public int[] getDnDPathToNode(SQLObject node) {
		DBTreeModel m = (DBTreeModel) getModel();
		SQLObject[] sop = m.getPathToNode(node);
		int[] dndp = new int[sop.length-1];
		SQLObject current = sop[0];
		for (int i = 1; i < sop.length; i++) {
			dndp[i-1] = m.getIndexOfChild(current, sop[i]);
			current = sop[i];
		}
		return dndp;
	}

	public SQLObject getNodeForDnDPath(int[] path) throws ArchitectException {
		SQLObject current = (SQLObject) getModel().getRoot();
		for (int i = 0; i < path.length; i++) {
			current = current.getChild(path[i]);
		}
		return current;
	}

	public int getRowForNode(SQLObject node) {
		DBTreeModel m = (DBTreeModel) getModel();
		TreePath path = new TreePath(m.getPathToNode(node));
		return getRowForPath(path);
	}


	// -------------- JTree Overrides ---------------------

	public void expandPath(TreePath tp) {
		try {
			session.getArchitectFrame().setCursor(new Cursor(Cursor.WAIT_CURSOR));
			super.expandPath(tp);
		} catch (Exception ex) {
			logger.warn("Unexpected exception while expanding path "+tp, ex);
		} finally {
			session.getArchitectFrame().setCursor(null);
		}
	}

	// ---------- methods of DragSourceListener -----------
	public void dragEnter(DragSourceDragEvent dsde) {
		logger.debug("DBTree: got dragEnter event");
	}

	public void dragOver(DragSourceDragEvent dsde) {
		logger.debug("DBTree: got dragOver event");
	}

	public void dropActionChanged(DragSourceDragEvent dsde) {
		logger.debug("DBTree: got dropActionChanged event");
	}

	public void dragExit(DragSourceEvent dse) {
		logger.debug("DBTree: got dragExit event");
	}

	public void dragDropEnd(DragSourceDropEvent dsde) {
		logger.debug("DBTree: got dragDropEnd event");
	}



	// ----------------- popup menu stuff ----------------

	/**
	 * A simple mouse listener that activates the DBTree's popup menu
	 * when the user right-clicks (or some other platform-specific action).
	 *
	 * @author The Swing Tutorial (Sun Microsystems, Inc.)
	 */
	class PopupListener extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e,true);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e,false);
        }

        private void maybeShowPopup(MouseEvent e, boolean isPress) {

            TreePath p = getPathForLocation(e.getX(), e.getY());
            if (e.isPopupTrigger()) {
				logger.debug("TreePath is: " + p);

				if (p != null) {
					logger.debug("selected node object type is: " + p.getLastPathComponent().getClass().getName());
				}

				// if the item is not already selected, select it (and deselect everything else)
                // if the item is already selected, don't touch the selection model
				if (isTargetDatabaseChild(p)) {
					if (!isPathSelected(p)) {
						setSelectionPath(p);
				    }
				} else {
					// multi-select for menus is not supported outside the Target Database
                    if (!isPathSelected(p)) {
                        setSelectionPath(p);
                    }
				}
				popup = refreshMenu(p);
                popup.show(e.getComponent(),
                           e.getX(), e.getY());
            } else {
                if ( p == null && !isPress && e.getButton() == MouseEvent.BUTTON1 )
                    setSelectionPath(null);
            }
        }
    }

	/**
	 * Creates a context sensitive menu for managing Database Connections. There
     * are several modes of operations:
     *
     * <ol>
     *  <li>click on target database.  the user can modify the properties manually,
     * or select a target from the ones defined in user settings.  If there is
     * nothing defined, then that option is disabled.
     *
     *  <li>click on an DBCS reference in the DBTree.  Bring up the dialog that
     * allows the user to modify this connection.
     *
     *  <li>click on the background of the DBTree.  Allow the user to select DBCS
     * from a list, or create a new DBCS from scratch (which will be added to the
     * User Settings list of DBCS objects).
	 * </ol>
     *
     * <p>FIXME: add in column, table, exported key, imported keys menus; you can figure
     * out where the click came from by checking the TreePath.
	 */
	protected JPopupMenu refreshMenu(TreePath p) {
		logger.debug("refreshMenu is being called.");
		JPopupMenu newMenu = new JPopupMenu();
		newMenu.add(connectionsMenu = new JMenu("Add Source Connection"));
		connectionsMenu.add(new JMenuItem(newDBCSAction));
		connectionsMenu.addSeparator();

		// populate

		for (SPDataSource dbcs : session.getUserSettings().getConnections()) {
			connectionsMenu.add(new JMenuItem(new AddDBCSAction(dbcs)));
		}
		ASUtils.breakLongMenu(session.getArchitectFrame(),connectionsMenu);

		if (!isTargetDatabaseNode(p) && isTargetDatabaseChild(p)) {
			newMenu.addSeparator();
			ArchitectFrame af = session.getArchitectFrame();
			JMenuItem mi;
            
            mi = new JMenuItem();
            mi.setAction(af.getInsertIndexAction());
            mi.setActionCommand(ArchitectSwingConstants.ACTION_COMMAND_SRC_DBTREE);
            newMenu.add(mi);
            if (p.getLastPathComponent() instanceof SQLTable) {
                mi.setEnabled(true);
            } else {
                mi.setEnabled(false);
            }

            newMenu.addSeparator();
            
			mi = new JMenuItem();
			mi.setAction(af.getEditColumnAction());
			mi.setActionCommand(ArchitectSwingConstants.ACTION_COMMAND_SRC_DBTREE);
			newMenu.add(mi);
			if (p.getLastPathComponent() instanceof SQLColumn) {
				mi.setEnabled(true);
			} else {
				mi.setEnabled(false);
			}
            
			mi = new JMenuItem();
			mi.setAction(af.getInsertColumnAction());
			mi.setActionCommand(ArchitectSwingConstants.ACTION_COMMAND_SRC_DBTREE);
			newMenu.add(mi);
			if (p.getLastPathComponent() instanceof SQLTable || p.getLastPathComponent() instanceof SQLColumn) {
				mi.setEnabled(true);
			} else {
				mi.setEnabled(false);
			}

			newMenu.addSeparator();

			mi = new JMenuItem();
			mi.setAction(showInPlayPenAction);
			newMenu.add(mi);

			mi = new JMenuItem();
			mi.setAction(af.getEditTableAction());
			mi.setActionCommand(ArchitectSwingConstants.ACTION_COMMAND_SRC_DBTREE);
			newMenu.add(mi);
			if (p.getLastPathComponent() instanceof SQLTable || p.getLastPathComponent() instanceof SQLColumn) {
				mi.setEnabled(true);
			} else {
				mi.setEnabled(false);
			}

			mi = new JMenuItem();
			mi.setAction(af.getEditRelationshipAction());
			mi.setActionCommand(ArchitectSwingConstants.ACTION_COMMAND_SRC_DBTREE);
			newMenu.add(mi);
			if (p.getLastPathComponent() instanceof SQLRelationship) {
				mi.setEnabled(true);
			} else {
				mi.setEnabled(false);
			}

            mi = new JMenuItem();
            mi.setAction(af.getEditIndexAction());
            mi.setActionCommand(ArchitectSwingConstants.ACTION_COMMAND_SRC_DBTREE);
            newMenu.add(mi);
            if (p.getLastPathComponent() instanceof SQLIndex) {
                mi.setEnabled(true);
            } else {
                mi.setEnabled(false);
            }            

			mi = new JMenuItem();
			mi.setAction(af.getDeleteSelectedAction());
			mi.setActionCommand(ArchitectSwingConstants.ACTION_COMMAND_SRC_DBTREE);
			newMenu.add(mi);
			if (p.getLastPathComponent() instanceof SQLTable ||
			        p.getLastPathComponent() instanceof SQLColumn ||
			        p.getLastPathComponent() instanceof SQLRelationship ||
                    (p.getLastPathComponent() instanceof SQLIndex && 
                           !((SQLIndex) p.getLastPathComponent()).isPrimaryKeyIndex())) {
			    mi.setEnabled(true);
			} else {
				mi.setEnabled(false);
			}
		} else if (!isTargetDatabaseNode(p) && p != null) { // clicked on DBCS item in DBTree
			newMenu.addSeparator();
			if (p.getLastPathComponent() instanceof SQLDatabase) {
				newMenu.add(new JMenuItem(removeDBCSAction));
			}
			JMenuItem popupProperties = new JMenuItem(dbcsPropertiesAction);
			newMenu.add(popupProperties);
            if (p.getLastPathComponent() instanceof SQLDatabase){
                SQLDatabase tempDB=(SQLDatabase)(p.getLastPathComponent());
                JMenuItem setAsDB = new JMenuItem(new SetConnAsTargetDB(tempDB.getDataSource()));
                newMenu.add(setAsDB);
            }
		}

		// Show exception details (SQLException node can appear anywhere in the hierarchy)
		if (p != null && p.getLastPathComponent() instanceof SQLExceptionNode) {
			newMenu.addSeparator();
            final SQLExceptionNode node = (SQLExceptionNode) p.getLastPathComponent();
            newMenu.add(new JMenuItem(new AbstractAction("Show Exception Details") {
                public void actionPerformed(ActionEvent e) {
                    ASUtils.showExceptionDialogNoReport(
                            "Exception Node Report", node.getException());
                }
            }));

            // If the sole child is an exception node, we offer the user a way to re-try the operation
            try {
                final SQLObject parent = node.getParent();
                if (parent.getChildCount() == 1) {
                    newMenu.add(new JMenuItem(new AbstractAction("Retry") {
                        public void actionPerformed(ActionEvent e) {
                            parent.removeChild(0);
                            parent.setPopulated(false);
                            try {
                                parent.getChildren(); // forces populate
                            } catch (ArchitectException ex) {
                                try {
									parent.addChild(new SQLExceptionNode(ex, "New exception during retry"));
								} catch (ArchitectException e1) {
									logger.error("Couldn't add SQLExceptionNode to menu:", e1);
									JOptionPane.showMessageDialog(null, "Failed to add SQLExceptionNode:\n"+e1.getMessage());
								}
                                ASUtils.showExceptionDialogNoReport(
                                        "Exception occurred during retry", ex);
                            }
                        }
                    }));
                }
            } catch (ArchitectException ex) {
                logger.error("Couldn't count siblings of SQLExceptionNode", ex);
            }
		}

		// add in Show Listeners if debug is enabled
		if (logger.isDebugEnabled()) {
			newMenu.addSeparator();
			JMenuItem showListeners = new JMenuItem("Show Listeners");
			showListeners.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						SQLObject so = (SQLObject) getLastSelectedPathComponent();
						if (so != null) {
							JOptionPane.showMessageDialog(DBTree.this, new JScrollPane(new JList(new java.util.Vector(so.getSQLObjectListeners()))));
						}
					}
				});
			newMenu.add(showListeners);
		}
		return newMenu;
	}

	/**
     * Checks to see if the SQLDatabase reference from the the DBTree is the
     * same as the one held by the PlayPen.  If it is, we are looking at the
     * Target Database.
     */
	protected boolean isTargetDatabaseNode(TreePath tp) {
		if (tp == null) {
			return false;
		} else {
			return session.getPlayPen().getDatabase() == tp.getLastPathComponent();
		}
	}

	/**
     * Checks to see if the given tree path contains the playpen SQLDatabase.
     *
     * @return True if <code>tp</code> contains the playpen (target) database.
     *   Note that this is not stritcly limited to children of the target
     * database: it will return true if <code>tp</code> ends at the target
     * database node itself.
     */
	protected boolean isTargetDatabaseChild(TreePath tp) {
		if (tp == null) {
			return false;
		}

		Object[] oo = tp.getPath();
		for (int i = 0; i < oo.length; i++)
			if (session.getPlayPen().getDatabase() == oo[i]) return true;
		return false;
	}

	/**
	 * When invoked, this action adds the DBCS that was given in the
	 * constructor to the DBTree's model.  There is normally one
	 * AddDBCSAction associated with each item in the "Set Connection"
	 * menu.
	 */
	protected class AddDBCSAction extends AbstractAction {
		protected SPDataSource dbcs;

		public AddDBCSAction(SPDataSource dbcs) {
			super(dbcs.getName());
			this.dbcs = dbcs;
		}

		public void actionPerformed(ActionEvent e) {
			addSourceConnection(dbcs);
		}

	}

    /**
     * Adds the given data source to the db tree as a source database
     * connection.
     * 
     * @param dbcs The data source to be added to the db tree.
     */
	private void addSourceConnection(SPDataSource dbcs) {
	    SQLObject root = (SQLObject) getModel().getRoot();
	    try {
	        // check to see if we've already seen this one
	        if (dbcsAlreadyExists(dbcs)) {
	            logger.warn("database already exists in this project.");
	            JOptionPane.showMessageDialog(DBTree.this, "Can't set connection "
	                    + dbcs.getDisplayName()
	                    + ".  It already exists in the current project.",
	                    "Warning", JOptionPane.WARNING_MESSAGE);
	        } else {
	            SQLDatabase newDB = new SQLDatabase(dbcs);
	            root.addChild(root.getChildCount(), newDB);
	            session.getProject().setModified(true);
	            // start a thread to poke the new SQLDatabase object...
	            logger.debug("start poking database " + newDB.getName());
	            Thread thread = new PokeDBThread(newDB);
	            thread.start();
	        }
	    } catch (ArchitectException ex) {
	        logger.warn("Couldn't add new database to tree", ex);
	        JOptionPane.showMessageDialog(DBTree.this, "Couldn't add new connection:\n"+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	    }
	}
    
    protected class SetConnAsTargetDB extends AbstractAction{
        SPDataSource dbcs;

        public SetConnAsTargetDB(SPDataSource dbcs){
            super("Set As Target Database");
            this.dbcs  = dbcs;
        }

        public void actionPerformed(ActionEvent e) {
            session.getPlayPen().setDatabaseConnection(dbcs);
        }
    }

	/**
	 * When invoked, this action creates a new DBCS, sets the
	 * panelHoldsNewDBCS flag, and pops up the propDialog to edit the
	 * new DBCS.
     * <p>
     * If the database connection is created it will be added to the
     * db tree as well as the PL.ini.
	 */
	protected class NewDBCSAction extends AbstractAction {

		public NewDBCSAction() {
			super("New Connection...");
		}

		public void actionPerformed(ActionEvent e) {

            final DataSourceCollection plDotIni = session.getContext().getUserSettings().getPlDotIni();
            final SPDataSource dataSource = new SPDataSource(plDotIni);
            Runnable onAccept = new Runnable() {
                public void run() {
                    addSourceConnection(dataSource);
                    plDotIni.addDataSource(dataSource);
                }
            };
            ASUtils.showDbcsDialog(session.getArchitectFrame(), session, dataSource, onAccept);
            
			panelHoldsNewDBCS = true;
		}
	}

	protected class PokeDBThread extends Thread {
		SQLObject so;
		PokeDBThread (SQLObject so) {
			super();
			this.so = so;
		}
		public void run() {
			try {
				ArchitectUtils.pokeDatabase(so);
			} catch (ArchitectException ex) {
				logger.error("problem poking database " + so.getName(),ex);
			}
			logger.debug("finished poking database " + so.getName());
		}
	}

	/**
	 * The RemoveDBCSAction removes the currently-selected database connection from the project.
	 */
	protected class RemoveDBCSAction extends AbstractAction {

		public RemoveDBCSAction() {
			super("Remove Connection");
		}

		public void actionPerformed(ActionEvent arg0) {
			TreePath tp = getSelectionPath();
			if (tp == null) {
				JOptionPane.showMessageDialog(DBTree.this, "No items were selected.", "Can't remove", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (! (tp.getLastPathComponent() instanceof SQLDatabase) ) {
				JOptionPane.showMessageDialog(DBTree.this, "The selection was not a database", "Can't remove", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (isTargetDatabaseNode(tp)) {
				JOptionPane.showMessageDialog(DBTree.this, "You can't remove the target database", "Can't remove", JOptionPane.WARNING_MESSAGE);
				return;
			}

			try {
			    SQLDatabase selection = (SQLDatabase) tp.getLastPathComponent();
			    SQLObject root = (SQLObject) getModel().getRoot();
			    List dependants = ArchitectUtils.findColumnsSourcedFromDatabase(session.getPlayPen().getDatabase(), selection);
			    if (dependants.size() > 0) {
			        JOptionPane.showMessageDialog(DBTree.this,
			                new Object[] {"The following columns depend on objects in this database:",
			                				new JScrollPane(new JList(dependants.toArray())),
			                				"You can't remove this connection unless you remove these",
			                				"dependencies."},
			                "Can't delete",
			                JOptionPane.INFORMATION_MESSAGE);
			    } else if (root.removeChild(selection)) {
			        selection.disconnect();
			    } else {
			        logger.error("root.removeChild(selection) returned false!");
			        JOptionPane.showMessageDialog(DBTree.this, "Deletion of this database connection failed for an unknown reason.", "Couldn't remove", JOptionPane.ERROR_MESSAGE);
			    }
			} catch (ArchitectException ex) {
				logger.error("Couldn't locate dependant columns", ex);
				JOptionPane.showMessageDialog(DBTree.this,
						"Couldn't search for dependant columns:\n"+ex.getMessage()
						+"\n\nDatabase connection not removed.",
						"Couldn't remove", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * The DBCSPropertiesAction responds to the "Properties" item in
	 * the popup menu.  It determines which item in the tree is
	 * currently selected, then shows its properties
	 * window.
	 */
	protected class DBCSPropertiesAction extends AbstractAction {
		public DBCSPropertiesAction() {
			super("Connection Properties...");
		}

		public void actionPerformed(ActionEvent e) {
			TreePath p = getSelectionPath();
			if (p == null) {
				return;
			}
			Object [] pathArray = p.getPath();
			int ii = 0;
			SQLDatabase sd = null;
			while (ii < pathArray.length && sd == null) {
				if (pathArray[ii] instanceof SQLDatabase) {
					sd = (SQLDatabase) pathArray[ii];
				}
				ii++;
			}
			if (sd != null) {
                ASUtils.showDbcsDialog(session.getArchitectFrame(), session, sd.getDataSource(), null);
                logger.debug("Setting existing DBCS on panel");
			}
		}
	}


	/**
	 * The DBCSPropertiesAction responds to the "Properties" item in
	 * the popup menu.  It determines which item in the tree is
	 * currently selected, then (creates and) shows its properties
	 * window.
	 */
	protected class ShowInPlayPenAction extends AbstractAction {
		public ShowInPlayPenAction() {
			super("Show in Playpen");
		}

		public void actionPerformed(ActionEvent e) {
			TreePath p = getSelectionPath();
			if (p == null) {
				return;
			}
			PlayPen pp = session.getPlayPen();
			SQLObject selection = (SQLObject) p.getLastPathComponent();
            //Since we cannot directly select a SQLColumn directly
            //from the playpen, there is a special case for it
            if (selection instanceof SQLColumn){
                SQLColumn col = (SQLColumn)selection;
                SQLTable table = col.getParentTable();
                TablePane tp = pp.findTablePane(table);
                pp.selectAndShow(table);
                try {
                    tp.columnSelection.set(table.getColumnIndex(col), Boolean.TRUE);
                } catch (ArchitectException e1) {
                  ASUtils.showExceptionDialog(session, "Error in selecting the column!", e1);
                }
            } else
                pp.selectAndShow(selection);

		}
	}

	// --------------- INNER CLASSES -----------------
	/**
	 * Exports the SQLObject which was under the pointer in a DBTree
	 * when the drag gesture started.  If the tree contains
	 * non-SQLObject nodes, you'll get ClassCastExceptions.
	 */
 	public static class DBTreeDragGestureListener implements DragGestureListener {
		public void dragGestureRecognized(DragGestureEvent dge) {
			logger.info("Drag gesture event: "+dge);

			// we only start drags on left-click drags
			InputEvent ie = dge.getTriggerEvent();
			if ( (ie.getModifiers() & InputEvent.BUTTON1_MASK) == 0) {
				return;
			}

			DBTree t = (DBTree) dge.getComponent();
  			TreePath[] p = t.getSelectionPaths();

			if (p ==  null || p.length == 0) {
				// nothing to export
				return;
			} else {
				// export list of DnD-type tree paths
				ArrayList paths = new ArrayList(p.length);
				for (int i = 0; i < p.length; i++) {
					// ignore any playpen tables
					if (t.getDnDPathToNode((SQLObject) p[i].getLastPathComponent())[0]  !=0 )
					{
						paths.add(t.getDnDPathToNode((SQLObject) p[i].getLastPathComponent()));
					}
				}
				logger.info("DBTree: exporting list of DnD-type tree paths");

				// TODO add undo event
				dge.getDragSource().startDrag
					(dge,
					 null, //DragSource.DefaultCopyNoDrop,
					 new DnDTreePathTransferable(paths),
					 t);
			}
 		}
	}
}