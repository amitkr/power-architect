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

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import ca.sqlpower.architect.ArchitectUtils;
import ca.sqlpower.architect.DepthFirstSearch;
import ca.sqlpower.architect.ddl.DDLGenerator;
import ca.sqlpower.architect.ddl.DDLStatement;
import ca.sqlpower.architect.ddl.DDLUtils;
import ca.sqlpower.architect.swingui.action.DatabaseConnectionManagerAction;
import ca.sqlpower.architect.swingui.dbtree.DBTreeCellRenderer;
import ca.sqlpower.architect.swingui.dbtree.DBTreeModel;
import ca.sqlpower.sql.DataMover;
import ca.sqlpower.sql.DatabaseListChangeEvent;
import ca.sqlpower.sql.DatabaseListChangeListener;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLCatalog;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLObjectRoot;
import ca.sqlpower.sqlobject.SQLSchema;
import ca.sqlpower.sqlobject.SQLTable;
import ca.sqlpower.swingui.SPSUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class DataMoverPanel {
    
    /**
     * The panel that holds the UI.
     */
    private JPanel panel;
    
    /**
     * Tree of source objects that you might want to copy.
     */
    private JTree sourceTree;
    
    /**
     * Tree of target objects that you can copy into.
     */
    private JTree destTree;
    
    /**
     * The root object of the source and destination trees.
     */
    private SQLObjectRoot treeRoot;
    
    /**
     * Checkbox for selecting whether or not to wipe out the destination
     * table's contents before loading.
     */
    private JCheckBox truncateDestinationTableBox;
    
    /**
     * The session that this Data Mover belongs to. 
     */
    private ArchitectSwingSession session;
    
    public DataMoverPanel(ArchitectSwingSession session) throws SQLObjectException {
        this.session = session;
        
        setupDBTrees();
        
        sourceTree = new JTree(new DBTreeModel(treeRoot));
        sourceTree.setRootVisible(false);
        sourceTree.setShowsRootHandles(true);
        sourceTree.setCellRenderer(new DBTreeCellRenderer());
        
        destTree = new JTree(new DBTreeModel(treeRoot));
        destTree.setRootVisible(false);
        destTree.setShowsRootHandles(true);
        destTree.setCellRenderer(new DBTreeCellRenderer());
        
        PanelBuilder pb = new PanelBuilder(
                new FormLayout(
                        "pref:grow,8dlu,pref:grow", //$NON-NLS-1$
                        "pref,4dlu,fill:pref:grow,4dlu,pref,4dlu,pref")); //$NON-NLS-1$
        pb.getLayout().addGroupedColumn(1);
        pb.getLayout().addGroupedColumn(3);
        CellConstraints cc = new CellConstraints();
        
        pb.addLabel(Messages.getString("DataMoverPanel.sourceLabel"), cc.xy(1, 1)); //$NON-NLS-1$
        pb.addLabel(Messages.getString("DataMoverPanel.destinationLabel"), cc.xy(3, 1)); //$NON-NLS-1$
        
        pb.add(new JScrollPane(sourceTree), cc.xy(1, 3));
        pb.add(new JScrollPane(destTree), cc.xy(3, 3));
        
        session.getContext().getPlDotIni().addDatabaseListChangeListener(new DatabaseListChangeListener() {
            public void databaseAdded(DatabaseListChangeEvent e) {
                try {
                    setupDBTrees();                            
                } catch (SQLObjectException ex) {
                    SPSUtils.showExceptionDialogNoReport(panel, Messages.getString("DataMoverPanel.couldNotFindDB"), ex); //$NON-NLS-1$
                }
            }
            public void databaseRemoved(DatabaseListChangeEvent e) {
                try {
                    setupDBTrees();                            
                } catch (SQLObjectException ex) {
                    SPSUtils.showExceptionDialogNoReport(panel, Messages.getString("DataMoverPanel.couldNotFindDB"), ex); //$NON-NLS-1$
                }
            }
        });

        pb.add(new JButton(new DatabaseConnectionManagerAction(session)), cc.xy(1, 5));
        pb.add(truncateDestinationTableBox = new JCheckBox(Messages.getString("DataMoverPanel.truncateDestinationTableOption")), cc.xy(3, 5)); //$NON-NLS-1$

        pb.add(ButtonBarFactory.buildOKCancelBar(
                    new JButton(okAction), new JButton(cancelAction)),
               cc.xyw(1, 7, 3));
        
        pb.setDefaultDialogBorder();
        panel = pb.getPanel();
    }

    /**
     * Sets the trees in the data mover panel to have all of the connections
     * in the current context.
     */
    private void setupDBTrees() throws SQLObjectException {
        if (treeRoot == null) {
            treeRoot = new SQLObjectRoot();
        } else {
            for(int i = treeRoot.getChildCount() - 1; i >= 0; i--) {
                treeRoot.removeChild(i);
            }
        }
        for (SPDataSource ds : session.getContext().getConnections()) {
            treeRoot.addChild(new SQLDatabase(ds));
        }
    }
    
    private Action okAction = new AbstractAction(Messages.getString("DataMoverPanel.okButton")) { //$NON-NLS-1$
        public void actionPerformed(ActionEvent e) {
            try {
                doDataMove();
            } catch (Exception ex) {
                ASUtils.showExceptionDialog(session, Messages.getString("DataMoverPanel.failedToMoveData"), ex); //$NON-NLS-1$
            }
        }
    };

    private Action cancelAction = new AbstractAction(Messages.getString("DataMoverPanel.cancelButton")) { //$NON-NLS-1$
        public void actionPerformed(ActionEvent e) {
            try {
                Window w = SPSUtils.getWindowInHierarchy(panel);
                if (w != null) w.dispose();
            } catch (Exception ex) {
                ASUtils.showExceptionDialog(session, Messages.getString("DataMoverPanel.failedToMoveData"), ex); //$NON-NLS-1$
            }
        }
    };
    
    public JPanel getPanel() {
        return panel;
    }
    
    public void doDataMove() throws SQLException, SQLObjectException {
        final TreePath[] sourcePaths = sourceTree.getSelectionPaths();
        int tableCount = 0;
        int rowCount = 0;
        
        List<SQLTable> sourceTables = new ArrayList<SQLTable>();
        for (TreePath sourcePath : sourcePaths) {
            sourceTables.add((SQLTable) sourcePath.getLastPathComponent());
        }
        
        DepthFirstSearch dfs = new DepthFirstSearch(sourceTables);
        
        for (SQLTable sourceTable : dfs.getFinishOrder()) {
            int thisCount = moveSingleTable(sourceTable);
            if (thisCount == -1) {
                int choice = JOptionPane.showConfirmDialog(panel, Messages.getString("DataMoverPanel.continueCopyingRemainingTablesOption")); //$NON-NLS-1$
                if (choice != JOptionPane.YES_OPTION) {
                    break;
                }
            } else {
                tableCount += 1;
                rowCount += thisCount;
            }
        }
        JOptionPane.showMessageDialog(panel, Messages.getString("DataMoverPanel.dataCopyResults", String.valueOf(tableCount), String.valueOf(rowCount))); //$NON-NLS-1$
    }
    
    /**
     * Moves the data from the table identified by 
     * @param sourcePath
     * @return The number of rows moved, or -1 if the user canceled the operation.
     * @throws SQLException
     * @throws SQLObjectException
     */
    private int moveSingleTable(final SQLTable sourceTable) throws SQLException, SQLObjectException {
        final SQLDatabase sourceDB = getParentDatabase(sourceTable);
        
        final TreePath destPath = destTree.getSelectionPath();
        final SQLObject destObject = (SQLObject) destPath.getLastPathComponent();
        final SQLDatabase destDB = getParentDatabase(destObject);
        
        String destCatalogName = null;
        String destSchemaName = null;
        String destTableName = sourceTable.getName();
        SQLObject tmpSqlObj = destObject;
        while (tmpSqlObj != null) {
            // walk up the ancestors and set table, catalog, and schema name as appropriate
            if (tmpSqlObj instanceof SQLTable) destTableName = tmpSqlObj.getName();
            if (tmpSqlObj instanceof SQLCatalog) destCatalogName = tmpSqlObj.getName();
            if (tmpSqlObj instanceof SQLSchema) destSchemaName = tmpSqlObj.getName();
            tmpSqlObj = tmpSqlObj.getParent();
        }
        
        boolean needToCreate = false;
        SQLTable destTable = destDB.getTableByName(
                destCatalogName, destSchemaName, destTableName);
        if (destTable == null) {
            needToCreate = true;
            destTable = ArchitectUtils.addSimulatedTable(
                destDB, destCatalogName, destSchemaName, destTableName);
        }
        
        Connection sourceCon = null;
        Connection destCon = null;
        try {
            sourceCon = sourceDB.getConnection();
            destCon = destDB.getConnection();
            
            final String sourceQualifiedName = DDLUtils.toQualifiedName(
                    sourceTable.getCatalogName(),
                    sourceTable.getSchemaName(),
                    sourceTable.getName());
            final String destQualifiedName = DDLUtils.toQualifiedName(
                    destCatalogName,
                    destSchemaName,
                    destTableName);

            if (needToCreate) {
                int choice = JOptionPane.showConfirmDialog(panel, Messages.getString("DataMoverPanel.destinationTableDoesNotExist", destQualifiedName)); //$NON-NLS-1$
                if (choice != JOptionPane.YES_OPTION) return -1;
                
                DDLGenerator ddlg = DDLUtils.createDDLGenerator(destDB.getDataSource());
                ddlg.generateDDLStatements(Collections.singletonList(destTable));
                Statement stmt = null;
                try {
                    stmt = destCon.createStatement();
                    for (DDLStatement ddlstmt : ddlg.getDdlStatements()) {
                        stmt.executeUpdate(ddlstmt.getSQLText());
                    }
                } finally {
                    if (stmt != null) stmt.close();
                }
            }
            
            DataMover mover = new DataMover(destCon, sourceCon);
            mover.setCreatingDestinationTable(false);
            mover.setTruncatingDestinationTable(truncateDestinationTableBox.isSelected());
            mover.setDebug(true);  // when true, debug data goes to System.out

//            JOptionPane.showMessageDialog(panel,
//                    "About to copy\n"+sourceQualifiedName+"\nto\n"+destQualifiedName);
            
            int count = mover.copyTable(destQualifiedName, sourceQualifiedName);
            
            return count;
        } catch (InstantiationException ex) {
            throw new RuntimeException("Couldn't create DDL Generator", ex); //$NON-NLS-1$
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Couldn't create DDL Generator", ex); //$NON-NLS-1$
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Couldn't create DDL Generator", ex); //$NON-NLS-1$
        } finally {
            sourceCon.close();
            destCon.close();
        }
    }
    
    /**
     * Returns the nearest parent of obj that is an instance of SQLDatabase,
     * or null if there is no SQLDatabase ancestor of obj.
     * 
     * @param obj The object to find the parent database of.
     * @return The nearest SQLDatabase ancestor of obj, or null if there is no
     * such ancestor.
     */
    private static SQLDatabase getParentDatabase(SQLObject obj) {
        while (obj != null) {
            if (obj instanceof SQLDatabase) {
                return (SQLDatabase) obj;
            }
            obj = obj.getParent();
        }
        return null;
    }
}
