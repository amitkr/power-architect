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

import java.awt.Color;
import java.awt.Point;
import java.sql.Types;

import ca.sqlpower.architect.swingui.event.SelectionEvent;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLTable;

public class TestRelationship extends TestPlayPenComponent<Relationship> {

	Relationship rel;
    TablePane tp1;
    TablePane tp2;
	
	protected void setUp() throws Exception {
		super.setUp();
		SQLTable t1 = new SQLTable(session.getTargetDatabase(), true);
        t1.addColumn(new SQLColumn(t1, "pkcol_1", Types.INTEGER, 10,0));
        t1.addColumn(new SQLColumn(t1, "fkcol_1", Types.INTEGER, 10,0));
        t1.getColumnByName("pkcol_1").setPrimaryKeySeq(0);

		session.getTargetDatabase().addChild(t1);
		pp.addTablePane(tp1 = new TablePane(t1, pp.getContentPane()), new Point(0,0));
		SQLTable t2 = new SQLTable(session.getTargetDatabase(), true);
        t2.addColumn(new SQLColumn(t2, "col_1", Types.INTEGER, 10,0));
        t2.addColumn(new SQLColumn(t2, "fkcol", Types.INTEGER, 10,0));      

		session.getTargetDatabase().addChild(t2);
		pp.addTablePane(tp2 = new TablePane(t2, pp.getContentPane()), new Point(0,0));
		SQLRelationship sqlrel = new SQLRelationship();
		sqlrel.attachRelationship(t1, t2, false);
        sqlrel.addMapping(t1.getColumnByName("pkcol_1"), 
                t2.getColumnByName("fkcol"));
		rel = new Relationship(sqlrel, pp.getContentPane());
		
		
		// layoutNode properties determined by model
        copyIgnoreProperties.add("headNode");
        copyIgnoreProperties.add("tailNode");
        
        // popup can be shared between copy and original
        copySameInstanceIgnoreProperties.add("popup");
        
        copySameInstanceIgnoreProperties.add("fkTable");
        copySameInstanceIgnoreProperties.add("pkTable");
	}
	
    public void testHighlightWithRelationshipTypeChange() throws SQLObjectException {               
        rel.setSelected(true,SelectionEvent.SINGLE_SELECT);
        assertEquals(Color.RED,tp1.getColumnHighlight(0));
        assertEquals(Color.RED,tp2.getColumnHighlight(1));
        assertEquals(tp2.getForegroundColor(), tp2.getColumnHighlight(0));
        rel.setSelected(false,SelectionEvent.SINGLE_SELECT);
        
        assertEquals(tp1.getForegroundColor(), tp1.getColumnHighlight(0));
        assertEquals(tp2.getForegroundColor(), tp2.getColumnHighlight(1));
        assertEquals(tp2.getForegroundColor(), tp2.getColumnHighlight(0));
        
        rel.setSelected(true,SelectionEvent.SINGLE_SELECT);
        rel.getModel().setIdentifying(true);       
        
        assertEquals(Color.RED,tp1.getColumnHighlight(0));
        SQLColumn fkCol = tp2.getModel().getColumnByName("fkcol");
        assertEquals(0, tp2.getModel().getColumnIndex(fkCol));
        assertEquals(Color.RED,tp2.getColumnHighlight(0));
        assertEquals(tp2.getForegroundColor(), tp2.getColumnHighlight(1));      
    }
    
    private void setupRefCountTests(SQLDatabase db, SQLTable pkTable, SQLTable fkTable, SQLRelationship sourceRel) throws SQLObjectException {
        pkTable.setName("pkTable");
        pkTable.addColumn(new SQLColumn(pkTable, "PKTableCol1", Types.INTEGER, 1, 0));
        pkTable.addColumn(new SQLColumn(pkTable, "PKTableCol2", Types.INTEGER, 1, 0));
        pkTable.getColumn(0).setPrimaryKeySeq(0);
        db.addChild(pkTable);
        
        fkTable.setName("child");
        fkTable.addColumn(new SQLColumn(fkTable, "FKTableCol1", Types.INTEGER, 1, 0));
        db.addChild(fkTable);
        
        sourceRel.addMapping(pkTable.getColumn(0), fkTable.getColumn(0));
        sourceRel.attachRelationship(pkTable, fkTable, true);        
    }
    
    public void testRefCountWithFkTableInsertedFirst() throws SQLObjectException {
        SQLDatabase db = new SQLDatabase(); 
        pp.getSession().getRootObject().addChild(db);
        SQLTable fkTable = new SQLTable(db, true);
        SQLRelationship sourceRel = new SQLRelationship();
        SQLTable pkTable = new SQLTable(db, true);        
        setupRefCountTests(db,pkTable, fkTable, sourceRel);
        
        TablePane FkPane = pp.importTableCopy(fkTable, new Point(10, 10), ASUtils.createDuplicateProperties(session, fkTable));
        TablePane PkPane = pp.importTableCopy(pkTable, new Point(10, 10), ASUtils.createDuplicateProperties(session, pkTable));
                
        assertEquals(2,FkPane.getModel().getColumn(0).getReferenceCount());
        assertEquals(1, PkPane.getModel().getColumn(0).getReferenceCount());    
    }

    public void testRefCountWithPkTableInsertedFirst() throws SQLObjectException {
        SQLDatabase db = new SQLDatabase();
        pp.getSession().getRootObject().addChild(db);
        SQLTable fkTable = new SQLTable(db, true);
        SQLRelationship sourceRel = new SQLRelationship();
        SQLTable pkTable = new SQLTable(db, true);        
        setupRefCountTests(db, pkTable, fkTable, sourceRel);
        
        TablePane PkPane = pp.importTableCopy(pkTable, new Point(10, 10), ASUtils.createDuplicateProperties(session, pkTable));        
        TablePane FkPane = pp.importTableCopy(fkTable, new Point(10, 10), ASUtils.createDuplicateProperties(session, fkTable));

        assertEquals(2, FkPane.getModel().getColumn(0).getReferenceCount());
        assertEquals(1, PkPane.getModel().getColumn(0).getReferenceCount());    
    }
    
    public void testRefCountWithMultipleTablesInserted() throws SQLObjectException{
        SQLDatabase db = new SQLDatabase();
        pp.getSession().getRootObject().addChild(db);
        SQLTable fkTable = new SQLTable(db, true);
        SQLRelationship sourceRel = new SQLRelationship();
        SQLTable pkTable = new SQLTable(db, true);        
        
        setupRefCountTests(db, pkTable, fkTable, sourceRel);
        SQLTable fkTable2 = new SQLTable (db,true);
        fkTable2.addColumn(new SQLColumn(fkTable2, "FKTable2Col1", Types.INTEGER, 1, 0));
        SQLRelationship newRel = new SQLRelationship();
        newRel.addMapping(pkTable.getColumn(0), fkTable2.getColumn(0));
        newRel.attachRelationship(pkTable,fkTable2, true);
               
        
        TablePane PkPane = pp.importTableCopy(pkTable, new Point(10, 10), ASUtils.createDuplicateProperties(session, pkTable));        
        TablePane FkPane = pp.importTableCopy(fkTable, new Point(10, 10), ASUtils.createDuplicateProperties(session, fkTable));
        TablePane FkPane2 = pp.importTableCopy(fkTable2, new Point(10, 10), ASUtils.createDuplicateProperties(session, fkTable2));               
        
        assertEquals (1, PkPane.getModel().getColumn(0).getReferenceCount());
        assertEquals (2, FkPane.getModel().getColumn(0).getReferenceCount());
        assertEquals (2, FkPane2.getModel().getColumn(0).getReferenceCount());        
    }

    @Override
    protected Relationship getTargetCopy() {
        return new Relationship(rel, rel.getParent());
    }

    @Override
    protected Relationship getTarget() {
        return rel;
    }
}
