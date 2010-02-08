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
package ca.sqlpower.architect.diff;

import java.sql.Types;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLTable;

public class SQLRelationshipComparatorTest extends TestCase {
	
	SQLRelationshipComparator relComparator = new SQLRelationshipComparator(); 
	Comparator<SQLObject> comparator = new SQLObjectComparator();
	
	SQLTable table1L;
	SQLTable table2L;
	SQLRelationship left;
	
	protected void setUp() throws Exception {
		table1L = makeTable(1);
		table2L = makeTable(3);
		left= new SQLRelationship();
		table1L.addToPK(table1L.getColumn(0));
		left.addMapping(table1L.getColumn(0),table2L.getColumn(1));
	}

	public void testCompareRelationShipWithOneNull() throws SQLObjectException {		
		assertEquals("The source is null, should be -1", -1, relComparator.compare(null, left));
		assertEquals("The target is null, should be 1", 1, relComparator.compare(left,null));
	}
		
	public void testCompareSameRelationShip() throws SQLObjectException {		
		SQLTable table1R = makeTable(1);
		SQLTable table2R = makeTable(3);
		SQLRelationship right = new SQLRelationship();
		table1R.addToPK(table1R.getColumn(0));
		right.addMapping(table1R.getColumn(0),table2R.getColumn(1));
		assertEquals("Should be same relationship", 0,relComparator.compare(left,right));
	}
	

	public void testCompareWithDifferentMappings() throws SQLObjectException {		
		SQLTable table1R = makeTable(1);
		SQLTable table2R = makeTable(3);
		SQLRelationship right = new SQLRelationship();
		table1R.addToPK(table1R.getColumn(0));
		right.addMapping(table1R.getColumn(0),table2R.getColumn(0));//Different mapping here
		assertNotSame("Shouldn't be same relationship", 0,relComparator.compare(left,right));
	}
	
	public void testCompareWithExtraMapping() throws SQLObjectException {
		SQLTable table1R = makeTable(1);
		SQLTable table2R = makeTable(3);
		SQLRelationship right = new SQLRelationship();
		table1R.addToPK(table1R.getColumn(0));
		right.addMapping(table1R.getColumn(0),table2R.getColumn(1));
		right.addMapping(table1R.getColumn(0),table2R.getColumn(2));
		assertNotSame("Shouldn't be same relationship", 0,relComparator.compare(left,right));
	}
	
	public void testCompareColumn() throws SQLObjectException {
		Set<SQLColumn> list1 = generateColumnList(3);
		Set<SQLColumn> list2 = generateColumnList(3);
		assertEquals(0, relComparator.compareColumns(list1, list2));
		
		list1.add(new SQLColumn());
		assertEquals(1, relComparator.compareColumns(list1, list2));		
		assertEquals(-1, relComparator.compareColumns(list2, list1));
	}
	
	public Set<SQLColumn> generateColumnList(int num) throws SQLObjectException {
		Set<SQLColumn> colList = new TreeSet<SQLColumn>(comparator);
		for (int ii=1; ii <= num; ii++){
			colList.add(new SQLColumn(new SQLTable(),"col"+ii,Types.INTEGER,3, 0)); 
		}		
		return colList;
	}
	
	private SQLTable makeTable(int i) throws SQLObjectException {
		SQLTable t = new SQLTable(null, "table_"+i, "remark on this", "TABLE", true);
		for (int j = 0; j < i; j++) {
			t.addColumn(new SQLColumn(t, "column_"+j, Types.INTEGER, 3, 0));
		}
		return t;
	}
}
