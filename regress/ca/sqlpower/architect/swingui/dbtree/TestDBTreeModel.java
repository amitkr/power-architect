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

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import junit.framework.TestCase;
import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.ArchitectSession;
import ca.sqlpower.architect.ArchitectSessionContextImpl;
import ca.sqlpower.architect.ArchitectSessionImpl;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLDatabase;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLRelationship;
import ca.sqlpower.architect.SQLTable;

public class TestDBTreeModel extends TestCase {

    private ArchitectSession session;
    
    private static class LoggingSwingTreeModelListener implements TreeModelListener {

        private int changeCount;
        private int insertCount;
        private int removeCount;
        private int structureChangeCount;
        private List<TreeModelEvent> eventLog = new ArrayList<TreeModelEvent>();
        
        public void treeNodesChanged(TreeModelEvent e) {
            changeCount++;
            eventLog.add(e);
        }

        public void treeNodesInserted(TreeModelEvent e) {
            insertCount++;
            eventLog.add(e);
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            removeCount++;
            eventLog.add(e);
        }

        public void treeStructureChanged(TreeModelEvent e) {
            structureChangeCount++;
            eventLog.add(e);
        }
        
        public int getChangeCount() {
            return changeCount;
        }
        
        public int getInsertCount() {
            return insertCount;
        }
        
        public int getRemoveCount() {
            return removeCount;
        }
        
        public int getStructureChangeCount() {
            return structureChangeCount;
        }
        
        public List<TreeModelEvent> getEventLog() {
            return eventLog;
        }
        
        @Override
        public String toString() {
            return String.format("[insert: %d, remove: %d, change: %d, structure: %d, eventLog: %s]",
                    insertCount, removeCount, changeCount, structureChangeCount, eventLog); 
        }
    }


    private DBTreeModel tm;

    protected void setUp() throws Exception {
        session = new ArchitectSessionImpl(new ArchitectSessionContextImpl(), "TestSession");
        tm = new DBTreeModel(session.getRootObject());
        tm.setTestMode(true);
	}
	
    public void testRefireRelationshipMappingEvents() throws ArchitectException {
        SQLObject treeRoot = (SQLObject) tm.getRoot();
        SQLDatabase db = new SQLDatabase();
        db.setName("test database");

        SQLTable t = new SQLTable(db, true);
        t.setName("Table");
        
        SQLColumn c = new SQLColumn(null, "column", Types.ARRAY, 1, 1);

        treeRoot.addChild(db);
        db.addChild(t);
        t.addColumn(c);
        c.setPrimaryKeySeq(0);
        
        SQLRelationship r = new SQLRelationship();
        r.setName("my relationship is cooler than your silly columnmnm");
        r.attachRelationship(t, t, true);
        
        LoggingSwingTreeModelListener l = new LoggingSwingTreeModelListener();
        tm.addTreeModelListener(l);

        r.removeChild(0);
        
        System.out.println(l);
        
        assertEquals(2, l.getRemoveCount());
        
        TreePath expectPkPath = new TreePath(new Object[] { treeRoot, db, t, t.getExportedKeysFolder(), r });
        TreePath expectFkPath = new TreePath(new Object[] { treeRoot, db, t, t.getImportedKeysFolder(), r });
        
        Set<TreePath> actualPaths = new HashSet<TreePath>();
        for (TreeModelEvent tme : l.getEventLog()) {
            actualPaths.add(tme.getTreePath());
        }

        assertTrue(actualPaths.contains(expectFkPath));
        assertTrue(actualPaths.contains(expectPkPath));
    }
    
    public void testDBTreeRootParentNull() throws Exception {
        assertNull(((SQLObject)tm.getRoot()).getParent());
    }
    
    public void testDBTreeRootMatchesSessionRoot() throws Exception {
        assertEquals(session.getRootObject(), tm.getRoot());
    }
}
