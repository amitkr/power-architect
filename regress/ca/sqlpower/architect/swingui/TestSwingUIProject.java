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

import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.BasicConfigurator;
import org.apache.tools.ant.filters.StringInputStream;

import ca.sqlpower.ArchitectTestCase;
import ca.sqlpower.architect.ArchitectSession;
import ca.sqlpower.architect.ArchitectSessionContext;
import ca.sqlpower.architect.ProjectLoader;
import ca.sqlpower.architect.TestUtils;
import ca.sqlpower.architect.TestingArchitectSessionContext;
import ca.sqlpower.architect.ProjectSettings.ColumnVisibility;
import ca.sqlpower.architect.ddl.SQLServerDDLGenerator;
import ca.sqlpower.architect.olap.MondrianModel;
import ca.sqlpower.architect.olap.OLAPSession;
import ca.sqlpower.architect.profile.ColumnProfileResult;
import ca.sqlpower.architect.profile.ProfileManager;
import ca.sqlpower.architect.profile.TableProfileResult;
import ca.sqlpower.architect.swingui.dbtree.DBTreeModel;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.JDBCDataSourceType;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sqlobject.SQLCatalog;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLIndex;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLObjectRoot;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLSchema;
import ca.sqlpower.sqlobject.SQLTable;
import ca.sqlpower.sqlobject.StubSQLObject;
import ca.sqlpower.sqlobject.SQLIndex.AscendDescend;
import ca.sqlpower.sqlobject.SQLIndex.Column;
import ca.sqlpower.testutil.MockJDBCDriver;

/**
 * Test case, mainly for loading and saving via SwingUIProject.
 */
public class TestSwingUIProject extends ArchitectTestCase {
	
	private SwingUIProjectLoader project;
	private static final String ENCODING="UTF-8";
	private boolean deleteOnExit = false;
	private PlDotIni plIni;
    private ArchitectSwingSession session;
    private TestingArchitectSwingSessionContext context;
    
    private class TestSQLDatabase extends SQLDatabase {

        private List<StubSQLObject> stubs = new ArrayList<StubSQLObject>();
        
        @Override
        public List<Class<? extends SPObject>> getAllowedChildTypes() {
            List<Class<? extends SPObject>> types = new ArrayList<Class<? extends SPObject>>();
            types.add(StubSQLObject.class);
            return types;
        }

        @Override
        protected void addChildImpl(SPObject child, int index) {
            if (child instanceof StubSQLObject) {
                stubs.add(index, (StubSQLObject) child);
            }
        }
        
        @Override
        public List<? extends SQLObject> getChildren() {
            return stubs;
        }
        
        @Override
        public List<? extends SQLObject> getChildrenWithoutPopulating() {
            return stubs;
        }
        
    }
    
	/*
	 * Test method for 'ca.sqlpower.architect.swingui.SwingUIProject.SwingUIProject(String)'
	 */
	public void setUp() throws Exception {
        context = new TestingArchitectSwingSessionContext();
        session = context.createSession(false);
        project = session.getProjectLoader();
        plIni = new PlDotIni();
        // TODO add some database types and a test that loading the project finds them
	}

	private final String testData =
        "<?xml version='1.0'?>" +
        "<architect-project version='0.1'>" +
        " <project-name>TestSwingUIProject</project-name>" +
        " <project-data-sources>" +
        "  <data-source id='DS0'>" +
        "   <property key='Logical' value='Not Configured' />" +
        "  </data-source>" +
        " </project-data-sources>" +
        " <source-databases>" +
        " </source-databases>" +
        " <target-database dbcs-ref='DS0'>" +
        "  <table id='TAB0' populated='true' primaryKeyName='id' remarks='' name='Customers' >" +
        "   <folder id='FOL1' populated='true' name='Columns' type='1' >" +
        "    <column id='COL2' populated='true' autoIncrement='false' name='id' defaultValue='' nullable='0' precision='10' primaryKeySeq='0' referenceCount='1' remarks='' scale='0' type='4' />" +
        "    <column id='COL3' populated='true' autoIncrement='false' name='name' defaultValue='' nullable='0' precision='10' referenceCount='1' remarks='' scale='0' type='4' />" +
        "   </folder>" +
        "   <folder id='FOL4' populated='true' name='Exported Keys' type='3' >" +
        "   </folder>" +
        "   <folder id='FOL5' populated='true' name='Imported Keys' type='2' >" +
        "   </folder>" +
        "   <folder id='FOL13' populated='true' name='Indices' type='4' >" +
        "   </folder>" +
        "  </table>" +
        "  <table id='TAB6' populated='true' primaryKeyName='id' remarks='' name='Orders' >" +
        "   <folder id='FOL7' populated='true' name='Columns' type='1' >" +
        "    <column id='COL8' populated='true' autoIncrement='false' name='i&amp;d' defaultValue='' " +
        "    remarks=\"This isn't a problem\" nullable='0' precision='10' primaryKeySeq='0' referenceCount='1' scale='0' type='4' />" +
        "    <column id='COL9' populated='true' autoIncrement='false' name='customer&lt;id' defaultValue='' nullable='0' precision='10' referenceCount='1' remarks='' scale='0' type='4' />" +
        "   </folder>" +
        "   <folder id='FOL10' populated='true' name='Exported Keys' type='3' >" +
        "   </folder>" +
        "   <folder id='FOL11' populated='true' name='Imported Keys' type='2' >" +
        "   </folder>" +
        "   <folder id='FOL14' populated='true' name='Indices' type='4' >" +
        "   </folder>" +
        "  </table>" +
        "  <table id=\"TAB1830\" populated=\"true\" name=\"mm_project\" objectType=\"TABLE\" physicalName=\"MM_PROJECT\" remarks=\"\" >" +
        "   <folder id=\"FOL1831\" populated=\"true\" name=\"Columns\" physicalName=\"Columns\" type=\"1\" >" +
        "    <column id=\"COL1832\" populated=\"true\" autoIncrement=\"true\" autoIncrementSequenceName=\"mm_project_oid_seq\" name=\"project_oid\" nullable=\"0\" physicalName=\"PROJECT_OID\" precision=\"22\" primaryKeySeq=\"0\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"4\" />" +
        "    <column id=\"COL1833\" populated=\"true\" autoIncrement=\"false\" name=\"FOLDER_OID\" nullable=\"1\" physicalName=\"FOLDER_OID\" precision=\"22\" referenceCount=\"2\" remarks=\"\" scale=\"0\" type=\"4\" />" +
        "    <column id=\"COL1834\" populated=\"true\" autoIncrement=\"false\" name=\"project_name\" nullable=\"1\" physicalName=\"PROJECT_NAME\" precision=\"80\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"12\" />" +
        "   </folder>" +
        "   <folder id=\"FOL1889\" populated=\"true\" name=\"Exported Keys\" physicalName=\"Exported Keys\" type=\"3\" >" +
        "   </folder>" +
        "   <folder id=\"FOL1890\" populated=\"true\" name=\"Imported Keys\" physicalName=\"Imported Keys\" type=\"2\" >" +
        "   </folder>" +
        "   <folder id=\"FOL1891\" populated=\"true\" name=\"Indices\" physicalName=\"Indices\" type=\"4\" >" +
        "    <index id=\"IDX1892\" populated=\"true\" index-type=\"BTREE\" name=\"mm_project_pk\" physicalName=\"PL_MATCH_PK\" primaryKeyIndex=\"true\" unique=\"true\" >" +
        "     <index-column id=\"IDC1893\" populated=\"true\" ascending=\"false\" column-ref=\"COL1832\" descending=\"false\" name=\"project_oid\" physicalName=\"MATCH_OID\" />" +
        "    </index>" +
        "    <index id=\"IDX1894\" populated=\"true\" index-type=\"BTREE\" name=\"PL_MATCH_UNIQUE\" physicalName=\"PL_MATCH_UNIQUE\" primaryKeyIndex=\"false\" unique=\"true\" >" +
        "     <index-column id=\"IDC1895\" populated=\"true\" ascending=\"false\" column-ref=\"COL1834\" descending=\"false\" name=\"project_name\" physicalName=\"MATCH_ID\" />" +
        "    </index>" +
        "   </folder>" +
        "  </table>" +
        "  <relationships>" +
        "   <relationship id='REL12' populated='true' deferrability='0' deleteRule='0' fk-table-ref='TAB0' fkCardinality='6' identifying='true' name='Orders_Customers_fk' pk-table-ref='TAB6' pkCardinality='2' updateRule='0' >" +
        "    <column-mapping id='CMP13' populated='true' fk-column-ref='COL2' pk-column-ref='COL8' />" +
        "   </relationship>" +
        "   <reference ref-id='REL12' />" +
        "  </relationships>" +
        " </target-database>" +
        " <ddl-generator type='ca.sqlpower.architect.ddl.GenericDDLGenerator' allow-connection='true'> </ddl-generator>" + 
        " <compare-dm-settings ddlGenerator='ca.sqlpower.architect.ddl.SQLServerDDLGenerator' outputFormatAsString='ENGLISH'>" +        
        " <source-stuff datastoreTypeAsString='PROJECT' connectName='Arthur_test' " +
        " schema='ARCHITECT_REGRESS' filepath='' />"+
        "<target-stuff datastoreTypeAsString='FILE' filePath='Testpath' /> </compare-dm-settings>"+
        " <play-pen zoom=\"12.3\" viewportX=\"200\" viewportY=\"20\" relationship-style=\"rectilinear\" showPrimaryTag=\"true\" showForeignTag=\"true\" showAlternateTag=\"true\"  columnVisibility=\"PK_FK\" >" +
        "  <table-pane table-ref='TAB0' x='85' y='101' />" +
        "  <table-pane table-ref='TAB6' x='196' y='38' />" +
        "  <table-link relationship-ref='REL12' pk-x='76' pk-y='60' fk-x='114' fk-y='30' />" +
        " </play-pen>" +
        " <profiles topNCount=\"10\">" +
        "  <profile-result ref-id=\"TAB0\" type=\"ca.sqlpower.architect.profile.TableProfileResult\" createStartTime=\"1185828799320\" createEndTime=\"1185828807187\" exception=\"false\"   rowCount=\"234937\"/>" +
        "  <profile-result ref-id=\"COL2\" type=\"ca.sqlpower.architect.profile.ColumnProfileResult\" createStartTime=\"1185828799479\" createEndTime=\"1185828801322\" exception=\"false\" avgLength=\"5.6169228346322635\" minLength=\"5\" maxLength=\"6\" nullCount=\"0\" distinctValueCount=\"234937\">" +
        "   <avgValue type=\"java.math.BigDecimal\" value=\"127470.085669775301\"/>" +
        "   <maxValue type=\"java.lang.Integer\" value=\"500001\"/>" +
        "   <minValue type=\"java.lang.Integer\" value=\"10001\"/>" +
        "   <topNvalue count=\"1\" type=\"java.lang.Integer\" value=\"10001\"/>" +
        "   <topNvalue count=\"1\" type=\"java.lang.Integer\" value=\"26384\"/>" +
        "   <topNvalue count=\"1\" type=\"java.lang.Integer\" value=\"26383\"/>" +
        "   <topNvalue count=\"1\" type=\"java.lang.Integer\" value=\"26382\"/>" +
        "   <topNvalue count=\"1\" type=\"java.lang.Integer\" value=\"26381\"/>" +
        "   <topNvalue count=\"1\" type=\"java.lang.Integer\" value=\"26380\"/>" +
        "   <topNvalue count=\"1\" type=\"java.lang.Integer\" value=\"26379\"/>" +
        "   <topNvalue count=\"1\" type=\"java.lang.Integer\" value=\"26378\"/>" +
        "   <topNvalue count=\"1\" type=\"java.lang.Integer\" value=\"26377\"/>" +
        "   <topNvalue count=\"1\" type=\"java.lang.Integer\" value=\"26376\"/>" +
        "  </profile-result>" +
        " </profiles>" +
        "</architect-project>";
	
	public Set<String> getPropertiesToIgnore() {
	    Set<String> propertiesToIgnore = new HashSet<String>();
	    propertiesToIgnore.add("SPListeners");
        propertiesToIgnore.add("children");
        propertiesToIgnore.add("parent");
        propertiesToIgnore.add("class");
        propertiesToIgnore.add("childCount");
        propertiesToIgnore.add("populated");
        propertiesToIgnore.add("columnsPopulated");
        propertiesToIgnore.add("exportedKeysPopulated");
        propertiesToIgnore.add("importedKeysPopulated");
        propertiesToIgnore.add("indicesPopulated");
        propertiesToIgnore.add("magicEnabled");
        propertiesToIgnore.add("childrenInaccessibleReason");
        propertiesToIgnore.add("session");
        propertiesToIgnore.add("workspaceContainer");
        propertiesToIgnore.add("runnableDispatcher");
        propertiesToIgnore.add("foregroundThread");
        propertiesToIgnore.add("backgroundThread");
        return propertiesToIgnore;
	}
	
	/*
	 * Test method for 'ca.sqlpower.architect.swingui.SwingUIProject.load(InputStream)'
	 */
	public void testLoad() throws Exception {
		// StringReader r = new StringReader(testData);
		ByteArrayInputStream r = new ByteArrayInputStream(testData.getBytes());
		project.load(r, plIni);
		assertFalse(
		        "Project starts out with undo history",
		        session.getUndoManager().canUndoOrRedo());

		DBTree tree = session.getSourceDatabases();
		assertNotNull(tree);
		assertEquals(1, tree.getComponentCount());
		
		SQLDatabase target = session.getTargetDatabase(); 
		assertNotNull(target);
		
		assertEquals("PlayPen Database", target.getName());
		assertEquals(3, target.getChildCount());		
	}
	
    /**
     * Ensures the primary key property of columns loads properly. (from the example file)
     */
    public void testLoadPK() throws Exception {
        testLoad();
        
        SQLDatabase target = session.getTargetDatabase();
        
        SQLTable t1 = target.getChildByName("mm_project", SQLTable.class);
        assertEquals(1, t1.getPkSize());
        assertEquals(t1.getPrimaryKeyIndex().getChild(0).getColumn(), t1.getColumn(0));
        assertTrue(t1.getColumn(0).isPrimaryKey());
        assertFalse(t1.getColumn(1).isPrimaryKey());
    }
    
    private void subroutineForTestSaveLoadPK(SQLTable t) throws SQLObjectException {
        assertEquals(2, t.getPkSize());
        assertTrue(t.getColumn(0).isPrimaryKey());
        assertEquals(t.getPrimaryKeyIndex().getChild(0).getColumn(), t.getColumn(0));
        assertTrue(t.getColumn(1).isPrimaryKey());
        assertEquals(t.getPrimaryKeyIndex().getChild(1).getColumn(), t.getColumn(1));
        assertFalse(t.getColumn(2).isPrimaryKey());
        assertFalse(t.getColumn(0).isDefinitelyNullable());
        assertFalse(t.getColumn(1).isDefinitelyNullable());
        assertTrue(t.getColumn(0).isPrimaryKey());
        assertTrue(t.getColumn(1).isPrimaryKey());
        assertFalse(t.getColumn(2).isPrimaryKey());
    }
    
    /**
     * Ensures the primary key stuff of tables saves and loads properly.
     */
    public void testSaveLoadPK() throws Exception {
        // make a table with a pksize of 2
        SQLDatabase target = session.getTargetDatabase();
        SQLTable t = new SQLTable(null, "test_pk", null, "TABLE", true);
        t.addColumn(new SQLColumn(t, "pk1", Types.CHAR, 10, 0));
        t.addColumn(new SQLColumn(t, "pk2", Types.CHAR, 10, 0));
        t.addColumn(new SQLColumn(t, "nonpk", Types.CHAR, 10, 0));
        t.addToPK(t.getColumn(0));
        t.addToPK(t.getColumn(1));
        target.addChild(t);
        
        subroutineForTestSaveLoadPK(t);
        
        // save it
        File tmp = File.createTempFile("test", ".architect");
        if (deleteOnExit) {
            tmp.deleteOnExit();
        } else {
            System.out.println("testSaveLoadPK: tmp file is "+tmp);
        }
        PrintWriter out = new PrintWriter(tmp,ENCODING);
        assertNotNull(out);
        project.save(out,ENCODING);
        
        // load it back and check
        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
        SwingUIProjectLoader project2 = new SwingUIProjectLoader(session2);
        project2.load(new BufferedInputStream(new FileInputStream(tmp)), plIni);
        
        target = session2.getTargetDatabase();
        t = target.getTableByName("test_pk");
        subroutineForTestSaveLoadPK(t);
    }
    
	/*
	 * Test method for 'ca.sqlpower.architect.swingui.SwingUIProject.save(ProgressMonitor)'
	 */
	public void testSaveProgressMonitor() throws Exception {
		System.out.println("TestSwingUIProject.testSaveProgressMonitor()");
		MockProgressMonitor mockProgressMonitor = new MockProgressMonitor(null, "Hello", "Hello again", 0, 100);
		File file = File.createTempFile("test", "architect");
		project.setFile(file);
		project.save(mockProgressMonitor);
		
        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
		SwingUIProjectLoader p2 = new SwingUIProjectLoader(session2);
		p2.load(new BufferedInputStream(new FileInputStream(file)), plIni);
		File tmp2 = File.createTempFile("test2", ".architect");
		if (deleteOnExit) {
			tmp2.deleteOnExit();
		}
		p2.save(new PrintWriter(tmp2,ENCODING),ENCODING);
		assertEquals(file.length(), tmp2.length());	// Quick test
	}
	
	/*
	 * Test method for 'ca.sqlpower.architect.swingui.SwingUIProject.save(PrintWriter)'
	 * Create two temp files, save our testData project to the first, load that
	 * back in, save it to the second, and compare the two temp files.
	 */
	public void testSave() throws Exception {
		testLoad();
 
        ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		assertNotNull(byteArrayOutputStream);
		project.save(byteArrayOutputStream,ENCODING);
		System.out.println(byteArrayOutputStream.toString());

        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
        SwingUIProjectLoader p2 = new SwingUIProjectLoader(session2);
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toString().getBytes(ENCODING));
        p2.load(byteArrayInputStream, plIni);
		p2.save(byteArrayOutputStream2,ENCODING);

        assertEquals(byteArrayOutputStream.toString(), byteArrayOutputStream2.toString());
	}
    
    /*
     * Test method for 'ca.sqlpower.architect.swingui.SwingUIProject.save(PrintWriter)'
     * Create two temp files, save our testData project to the first, load that
     * back in, and compare the names are the same.
     */
    public void testSavePersistsTablePanes() throws Exception {
        testLoad();
 
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        project.save(byteArrayOutputStream,ENCODING);
        System.out.println(byteArrayOutputStream.toString());
        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
        SwingUIProjectLoader p2 = new SwingUIProjectLoader(session2);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toString().getBytes(ENCODING));
        p2.load(byteArrayInputStream, plIni);
        List<TablePane> projectTablePanes = session2.getPlayPen().getContentPane().getChildren(TablePane.class);
        List<TablePane> p2TablePanes = session2.getPlayPen().getContentPane().getChildren(TablePane.class);
        assertEquals(projectTablePanes.size(),p2TablePanes.size());
        for (int i=0; i<projectTablePanes.size();i++){
            TablePane tp1 = projectTablePanes.get(i);
            TablePane tp2 = p2TablePanes.get(i);
            assertEquals("Wrong table names",tp1.getName(), tp2.getName());
        }
    }

	/** Save a document and use built-in JAXP to ensure it is at least well-formed XML.
	 * @throws Exception
	 */
	public void testSaveIsWellFormed() throws Exception {
		boolean validate = false;
		testLoad();
		File tmp = File.createTempFile("test", ".architect");
		if (deleteOnExit) {
			tmp.deleteOnExit();
		}
		PrintWriter out = new PrintWriter(tmp,ENCODING);
		assertNotNull(out);
		
		session.setName("FOO<BAR");		// Implicitly testing sanitizeXML method here!
		
		project.save(out,ENCODING);
		
		System.err.println("Parsing " + tmp + "...");

		// Make the document a URL so relative DTD works.
		String uri = "file:" + tmp.getAbsolutePath();

		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		if (validate)
			f.setValidating(true);
		DocumentBuilder p = f.newDocumentBuilder();
		p.parse(uri);
		System.out.println("Parsed OK");
	}
	
	public void testSaveCoversAllDatabaseProperties() throws Exception {
		testLoad();
		DBTree dbTree = session.getSourceDatabases();
		DBTreeModel dbTreeModel = (DBTreeModel) dbTree.getModel();
		
		JDBCDataSource fakeDataSource = new JDBCDataSource(new PlDotIni());
		SQLDatabase db = new SQLDatabase() {
			@Override
			public Connection getConnection() throws SQLObjectException {
				return null;
			}
		};
		db.setDataSource(fakeDataSource);
		db.setPopulated(true);
		((SQLObject) dbTreeModel.getRoot()).addChild(db);
		
		Set<String> propertiesToIgnore = getPropertiesToIgnore();
		propertiesToIgnore.add("tables");
		propertiesToIgnore.add("parentDatabase");
		propertiesToIgnore.add("connection");
		propertiesToIgnore.add("secondaryChangeMode");
		propertiesToIgnore.add("dataSource");  // we set this already!
		propertiesToIgnore.add("playPenDatabase");  // only set by playpen code
		propertiesToIgnore.add("progressMonitor");
		propertiesToIgnore.add("zoomInAction");
		propertiesToIgnore.add("zoomOutAction");
        propertiesToIgnore.add("tableContainer");
		
		Map<String,Object> oldDescription =
			TestUtils.setAllInterestingProperties(db, propertiesToIgnore);
		System.out.println("Properties set " + oldDescription);
		
		
		File tmp = File.createTempFile("test", ".architect");
		System.out.println("File located at " + tmp.getAbsolutePath() + " and is delete on exit? " + deleteOnExit);
		if (deleteOnExit) {
			tmp.deleteOnExit();
		}
		PrintWriter out = new PrintWriter(tmp,ENCODING);
		assertNotNull(out);
		project.save(out,ENCODING);
		
        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
		SwingUIProjectLoader project2 = new SwingUIProjectLoader(session2);
		project2.load(new BufferedInputStream(new FileInputStream(tmp)), plIni);
		
		// grab the second database in the dbtree's model (the first is the play pen)
		db = (SQLDatabase) session2.getSourceDatabases().getDatabaseList().get(1);
		
		System.out.println("DB has child exception " + db.getChildrenInaccessibleReasons());
		
		Map<String, Object> newDescription =
			ca.sqlpower.testutil.TestUtils.getAllInterestingProperties(db, propertiesToIgnore);
		
		assertMapsEqual(oldDescription, newDescription);
	}
	

	public void testSaveCoversAllCatalogProperties() throws Exception {
		JDBCDataSourceType mockType = new JDBCDataSourceType();
		JDBCDataSource ds = new JDBCDataSource(new PlDotIni());
        ds.setParentType(mockType);
		ds.setDisplayName("Schemaless Database");
		ds.getParentType().setJdbcDriver(MockJDBCDriver.class.getName());
		ds.setUser("fake");
		ds.setPass("fake");
		//this creates a mock jdbc database with only catalogs
		ds.setUrl("jdbc:mock:dbmd.catalogTerm=Catalog&catalogs=cat1,cat2,cat3");
		
		DBTree dbTree;
		DBTreeModel dbTreeModel = null;
		
		testLoad();
		dbTree = session.getSourceDatabases();
		dbTreeModel = (DBTreeModel) dbTree.getModel();
		
		SQLDatabase db = new SQLDatabase();
		db.setDataSource(ds);
		db.setPopulated(true);
		
		((SQLObject) dbTreeModel.getRoot()).addChild(db);
	
		SQLCatalog target = new SQLCatalog(db, "my test catalog");
		db.addChild(target);
		
		Set<String> propertiesToIgnore = getPropertiesToIgnore();
		propertiesToIgnore.add("parentDatabase");
        
		propertiesToIgnore.add("secondaryChangeMode");
        propertiesToIgnore.add("tableContainer");

		Map<String,Object> oldDescription =
			TestUtils.setAllInterestingProperties(target, propertiesToIgnore);
		
		
		File tmp = File.createTempFile("test", ".architect");
		if (deleteOnExit) {
			tmp.deleteOnExit();
		}
		PrintWriter out = new PrintWriter(tmp,ENCODING);
		assertNotNull(out);
		project.save(out,ENCODING);
		
        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
		SwingUIProjectLoader project2 = new SwingUIProjectLoader(session2);
		project2.load(new BufferedInputStream(new FileInputStream(tmp)), plIni);
		
		// grab the second database in the dbtree's model (the first is the play pen)
		db = (SQLDatabase) session2.getSourceDatabases().getDatabaseList().get(1);
		
		target = (SQLCatalog) db.getChild(0);
		
		Map<String, Object> newDescription =
			ca.sqlpower.testutil.TestUtils.getAllInterestingProperties(target, propertiesToIgnore);
		
		assertMapsEqual(oldDescription, newDescription);
	}

	public void testSaveCoversAllSchemaProperties() throws Exception {
		testLoad();
		DBTree dbTree = session.getSourceDatabases();
		DBTreeModel dbTreeModel = (DBTreeModel) dbTree.getModel();
		
		JDBCDataSource fakeDataSource = new JDBCDataSource(new PlDotIni());
		SQLDatabase db = new SQLDatabase();
		db.setDataSource(fakeDataSource);
		db.setPopulated(true);
		((SQLObject) dbTreeModel.getRoot()).addChild(db);
		
		SQLSchema target = new SQLSchema(db, "my test schema", true);
		db.addChild(target);
		
		Set<String> propertiesToIgnore = getPropertiesToIgnore();
		propertiesToIgnore.add("parentDatabase");
		propertiesToIgnore.add("secondaryChangeMode");

		Map<String,Object> oldDescription =
			TestUtils.setAllInterestingProperties(target, propertiesToIgnore);
		
		
		File tmp = File.createTempFile("test", ".architect");
		if (deleteOnExit) {
			tmp.deleteOnExit();
		}
		PrintWriter out = new PrintWriter(tmp,ENCODING);
		assertNotNull(out);
		project.save(out,ENCODING);
		
        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
		SwingUIProjectLoader project2 = new SwingUIProjectLoader(session2);
		project2.load(new BufferedInputStream(new FileInputStream(tmp)), plIni);
		
		// grab the second database in the dbtree's model (the first is the play pen)
		db = (SQLDatabase) session2.getSourceDatabases().getDatabaseList().get(1);
		
		target = (SQLSchema) db.getChild(0);
		
		Map<String, Object> newDescription =
			ca.sqlpower.testutil.TestUtils.getAllInterestingProperties(target, propertiesToIgnore);
		
		assertMapsEqual(oldDescription, newDescription);
	}

	public void testSaveCoversAllTableProperties() throws Exception {
		testLoad();
		DBTree dbTree = session.getSourceDatabases();
		DBTreeModel dbTreeModel = (DBTreeModel) dbTree.getModel();
		
		JDBCDataSource fakeDataSource = new JDBCDataSource(new PlDotIni());
		SQLDatabase db = new SQLDatabase();
		db.setDataSource(fakeDataSource);
		db.setPopulated(true);
		((SQLObject) dbTreeModel.getRoot()).addChild(db);
		
		SQLTable target = new SQLTable(db, true);
		db.addChild(target);
		
		Set<String> propertiesToIgnore = getPropertiesToIgnore();
		propertiesToIgnore.add("parentDatabase");
		propertiesToIgnore.add("columnsFolder");
		propertiesToIgnore.add("secondaryChangeMode");

		Map<String,Object> oldDescription =
			TestUtils.setAllInterestingProperties(target, propertiesToIgnore);
		
		
		File tmp = File.createTempFile("test", ".architect");
		if (deleteOnExit) {
			tmp.deleteOnExit();
		}
		PrintWriter out = new PrintWriter(tmp,ENCODING);
		assertNotNull(out);
		project.save(out,ENCODING);
		
        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
		SwingUIProjectLoader project2 = new SwingUIProjectLoader(session2);
		project2.load(new BufferedInputStream(new FileInputStream(tmp)), plIni);
		
		// grab the second database in the dbtree's model (the first is the play pen)
		db = (SQLDatabase) session2.getSourceDatabases().getDatabaseList().get(1);
		
		target = (SQLTable) db.getChild(0);
		
		Map<String, Object> newDescription =
			ca.sqlpower.testutil.TestUtils.getAllInterestingProperties(target, propertiesToIgnore);
		
		assertMapsEqual(oldDescription, newDescription);
	}

	public void testSaveCoversAllColumnProperties() throws Exception {
		final String tableName = "harry";
		testLoad();
		
		SQLDatabase ppdb = session.getTargetDatabase();
		SQLTable table = new SQLTable(ppdb, true);
		table.setName(tableName);
		SQLColumn target = new SQLColumn(table, "my cool test column", Types.INTEGER, 10, 10);
		ppdb.addChild(table);
		table.addColumn(target);
		
		Set<String> propertiesToIgnore = getPropertiesToIgnore();
		propertiesToIgnore.add("parentTable");
		propertiesToIgnore.add("undoEventListeners");
		propertiesToIgnore.add("secondaryChangeMode");
		propertiesToIgnore.add("constraintType");
		propertiesToIgnore.add("checkConstraint");
		propertiesToIgnore.add("enumeration");
		propertiesToIgnore.add("precisionType");
		propertiesToIgnore.add("scaleType");
		propertiesToIgnore.add("platform");
		
		propertiesToIgnore.add("childrenWithoutPopulating");
		
		session.getUndoManager().setLoading(true);
		Map<String,Object> oldDescription =
			TestUtils.setAllInterestingProperties(target, propertiesToIgnore);
		session.getUndoManager().setLoading(false);
		
		// need to set sourceColumn manually because it has to exist in the database.
		{
			// different variable scope
			DBTree dbTree = session.getSourceDatabases();
			DBTreeModel dbTreeModel = (DBTreeModel) dbTree.getModel();
			
			JDBCDataSource fakeDataSource = new JDBCDataSource(new PlDotIni());
			SQLDatabase db = new SQLDatabase();
			db.setDataSource(fakeDataSource);
			db.setPopulated(true);
			((SQLObject) dbTreeModel.getRoot()).addChild(db);
			
			SQLTable sourceTable = new SQLTable(db, true);
			SQLColumn sourceColumn = new SQLColumn(sourceTable, "my cool source column", Types.INTEGER, 10, 10);
			sourceTable.addColumn(sourceColumn);
			db.addChild(sourceTable);

			// make sure target has a source column that can be saved in the project
			target.setSourceColumn(sourceColumn);
			oldDescription.put("sourceColumn", sourceColumn);
		}
		
		File tmp = File.createTempFile("test", ".architect");
		if (deleteOnExit) {
			tmp.deleteOnExit();
		} else {
			System.out.println("testSaveCoversAllColumnProperties: temp file is "+tmp.getAbsolutePath());
		}
		PrintWriter out = new PrintWriter(tmp,ENCODING);
		assertNotNull(out);
		project.save(out,ENCODING);
		
        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
		SwingUIProjectLoader project2 = new SwingUIProjectLoader(session2);
		project2.load(new BufferedInputStream(new FileInputStream(tmp)), plIni);
		
		// grab the second database in the dbtree's model (the first is the play pen)
		ppdb = (SQLDatabase) session2.getTargetDatabase();
		
		target = ((SQLTable) ppdb.getTableByName(tableName)).getColumn(0);
		
		Map<String, Object> newDescription =
			ca.sqlpower.testutil.TestUtils.getAllInterestingProperties(target, propertiesToIgnore);
		
		assertMapsEqual(oldDescription, newDescription);
	}

    public void testSaveCoversAllIndexProperties() throws Exception {
        final String tableName = "rama_llama_dingdong";  // the power of the llama will save you
        testLoad();
        
        SQLDatabase ppdb = session.getTargetDatabase();
        SQLTable table = new SQLTable(ppdb, true);
        table.setName(tableName);
        SQLColumn col = new SQLColumn(table, "first", Types.VARCHAR, 10, 0);
        table.addColumn(col);
        SQLIndex target = new SQLIndex("testy index", false, null, null, null);
        target.addChild(new Column(col, AscendDescend.UNSPECIFIED));
        ppdb.addChild(table);
        table.addChild(target);
        table.addToPK(col);
        
        Set<String> propertiesToIgnore = getPropertiesToIgnore();
        propertiesToIgnore.add("undoEventListeners");

        session.getUndoManager().setLoading(true);
        Map<String,Object> oldDescription =
            TestUtils.setAllInterestingProperties(target, propertiesToIgnore);
        session.getUndoManager().setLoading(false);
        
        File tmp = File.createTempFile("test", ".architect");
        if (deleteOnExit) {
            tmp.deleteOnExit();
        } else {
            System.out.println("testSaveCoversAllIndexProperties: temp file is "+tmp.getAbsolutePath());
        }
        PrintWriter out = new PrintWriter(tmp,ENCODING);
        assertNotNull(out);
        project.save(out,ENCODING);
        
        
        InputStream in = new BufferedInputStream(new FileInputStream(tmp));
        StringBuffer sb = new StringBuffer(2000);
        int c;
        while( (c = in.read()) != -1) {
            sb.append((char)c);
        }
        System.out.println(sb.toString());
        in.close();

        
        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
        SwingUIProjectLoader project2 = new SwingUIProjectLoader(session2);
        project2.load(new BufferedInputStream(new FileInputStream(tmp)), plIni);
        
        ppdb = (SQLDatabase) session2.getTargetDatabase();
        
        SQLTable targetTable = (SQLTable) ppdb.getTableByName(tableName);
        System.out.println("target table=["+targetTable.getName()+"]");
        // child 1 because setPrimaryKeySeq calls normalizePrimaryKey who creates
        // a primary key is there is none made. The primary key is placed as child 0
        // in the list so it shows up first in the DBTree.
        target = (SQLIndex) (targetTable).getIndices().get(1);
        
        Map<String, Object> newDescription =
            ca.sqlpower.testutil.TestUtils.getAllInterestingProperties(target, propertiesToIgnore);
        
        assertMapsEqual(oldDescription, newDescription);
    }

    public void testSaveCoversAllNonPKIndexColumnProperties() throws Exception {
        final String tableName = "delicatessen";
        testLoad();
        
        SQLDatabase ppdb = session.getTargetDatabase();
        SQLTable table = new SQLTable(ppdb, true);
        table.setName(tableName);
        SQLIndex index = new SQLIndex("tasty index", false, null, null, null);
        SQLIndex.Column indexCol = new Column("phogna bologna", AscendDescend.DESCENDING);
        ppdb.addChild(table);
        table.addChild(index);
        index.addChild(indexCol);
        
        Set<String> propertiesToIgnore = getPropertiesToIgnore();
        propertiesToIgnore.add("undoEventListeners");
        propertiesToIgnore.add("primaryKeyIndex");

        session.getUndoManager().setLoading(true);
        Map<String,Object> oldDescription =
            TestUtils.setAllInterestingProperties(index, propertiesToIgnore);
        session.getUndoManager().setLoading(false);
        
        propertiesToIgnore.remove("primaryKeyIndex");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        project.save(byteArrayOutputStream,ENCODING);
        System.out.println(byteArrayOutputStream.toString());
        
        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
        SwingUIProjectLoader project2 = new SwingUIProjectLoader(session2);
        project2.load(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), plIni);
        
        // grab the second database in the dbtree's model (the first is the play pen)
        ppdb = (SQLDatabase) session2.getTargetDatabase();
        
        index = ((SQLTable) ppdb.getTableByName(tableName)).getChildByName(index.getName(), SQLIndex.class);
        
        Map<String, Object> newDescription =
            ca.sqlpower.testutil.TestUtils.getAllInterestingProperties(index, propertiesToIgnore);
        
        assertMapsEqual(oldDescription, newDescription);
    }
    
    public void testSaveCoversAllPKColumnProperties() throws Exception {
        final String tableName = "delicatessen";
        testLoad();
        
        SQLDatabase ppdb = session.getTargetDatabase();
        SQLIndex index = new SQLIndex("tasty index", false, null, null, null);
        SQLTable table = new SQLTable(ppdb, true, index);
        SQLColumn col = new SQLColumn(table,"col",1,0,0);
        table.setName(tableName);
        table.addColumn(col);
        SQLIndex.Column indexCol = new Column(col, AscendDescend.DESCENDING);
        ppdb.addChild(table);
        table.addChild(index);
        
        assertNotNull(table.getPrimaryKeyIndex());
        assertEquals(1, table.getIndices().size());
        assertSame(index, table.getPrimaryKeyIndex());

        index.addChild(indexCol);
        assertEquals(1, table.getIndices().size());
        assertSame(index, table.getPrimaryKeyIndex());
        table.addToPK(col);
        assertEquals(1, table.getIndices().size());
        assertSame(index, table.getPrimaryKeyIndex());
        
        Set<String> propertiesToIgnore = getPropertiesToIgnore();
        propertiesToIgnore.add("undoEventListeners");
        propertiesToIgnore.add("primaryKeyIndex");

        assertSame(index, table.getPrimaryKeyIndex());

        session.getUndoManager().setLoading(true);
        Map<String,Object> oldDescription =
            TestUtils.setAllInterestingProperties(index, propertiesToIgnore);
        session.getUndoManager().setLoading(false);
        
        assertSame(index, table.getPrimaryKeyIndex());
        
        propertiesToIgnore.remove("primaryKeyIndex");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        project.save(byteArrayOutputStream,ENCODING);
        System.out.println(byteArrayOutputStream.toString());
        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
        SwingUIProjectLoader project2 = new SwingUIProjectLoader(session2);
        project2.load(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), plIni);
        
        ppdb = (SQLDatabase) session2.getTargetDatabase();
        System.out.println(ppdb.getTableByName(tableName));
        index = (SQLIndex) ((SQLTable) ppdb.getTableByName(tableName)).getIndices().get(0);
        
        Map<String, Object> newDescription =
            ca.sqlpower.testutil.TestUtils.getAllInterestingProperties(index, propertiesToIgnore);
        
        assertMapsEqual(oldDescription, newDescription);
    }
    
    public void testSaveIndexColumnPointingToAColumn() throws Exception {
        final String tableName = "delicatessen";
        testLoad();
        
        SQLDatabase ppdb = session.getTargetDatabase();
        SQLTable table = new SQLTable(ppdb, true);
        SQLColumn col = new SQLColumn(table,"Column 1",1,1,1);
        table.addColumn(col);
        table.setName(tableName);
        SQLIndex index = new SQLIndex("tasty index", false, null, null, null);
        index.addIndexColumn(col, AscendDescend.DESCENDING);
        ppdb.addChild(table);
        table.addChild(index);
        table.addToPK(col);
        Set<String> propertiesToIgnore = getPropertiesToIgnore();
        propertiesToIgnore.add("undoEventListeners");

        session.getUndoManager().setLoading(true);
        Map<String,Object> oldDescription =
            TestUtils.setAllInterestingProperties(index, propertiesToIgnore);
        session.getUndoManager().setLoading(false);
        
        File tmp = File.createTempFile("test", ".architect");
        if (deleteOnExit) {
            tmp.deleteOnExit();
        } else {
            System.out.println("testSaveCoversAllIndexProperties: temp file is "+tmp.getAbsolutePath());
        }
        PrintWriter out = new PrintWriter(tmp,ENCODING);
        assertNotNull(out);
        project.save(out,ENCODING);
        
        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
        SwingUIProjectLoader project2 = new SwingUIProjectLoader(session2);
        project2.load(new BufferedInputStream(new FileInputStream(tmp)), plIni);
        
        // grab the second database in the dbtree's model (the first is the play pen)
        ppdb = (SQLDatabase) session2.getTargetDatabase();
        
        // child 1 because setPrimaryKeySeq calls normalizePrimaryKey which creates
        // a primary key is there is none made. The primary key is placed as child 0
        // in the list so it shows up first in the DBTree.
        index = (SQLIndex) ((SQLTable) ppdb.getTableByName(tableName)).getIndices().get(1);
        
        Map<String, Object> newDescription =
            ca.sqlpower.testutil.TestUtils.getAllInterestingProperties(index, propertiesToIgnore);
        
        assertMapsEqual(oldDescription, newDescription);
    }

    public void testSaveMultipleIndexColumns() throws Exception {
        final String tableName = "delicatessen";
        testLoad();
        
        SQLDatabase ppdb = session.getTargetDatabase();
        SQLTable table = new SQLTable(ppdb, true);
        SQLColumn col = new SQLColumn(table,"Column 1",1,1,1);
        table.addColumn(col);
        table.setName(tableName);
        ppdb.addChild(table);

        SQLIndex origIndex1 = new SQLIndex("tasty index", false, null, "HASH", null);
        origIndex1.addIndexColumn(col, AscendDescend.DESCENDING);
        table.addChild(origIndex1);
        table.addToPK(col);

        // second index references same column as first index, so
        // origIndex1.getChild(0).equals(origIndex2.getChild(0)) even though
        // they are not the same object
        SQLIndex origIndex2 = new SQLIndex("nasty index", false, null, "HASH", null);
        origIndex2.addIndexColumn(col, AscendDescend.DESCENDING);
        table.addChild(origIndex2);

        ByteArrayOutputStream tempFile = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(tempFile);
        project.save(out, ENCODING);
        
        ArchitectSwingSessionContext context = session.getContext();
        ArchitectSwingSession session2 = context.createSession(false);
        SwingUIProjectLoader p = new SwingUIProjectLoader(session2);
        p.load(new ByteArrayInputStream(tempFile.toByteArray()), context.getPlDotIni());
        
        ppdb = session2.getTargetDatabase();
        
        // child 1 because setPrimaryKeySeq calls normalizePrimaryKey which creates
        // a primary key is there is none made. The primary key is placed as child 0
        // in the list so it shows up first in the DBTree.
        SQLIndex reloadedIndex1 = (SQLIndex) ppdb.getTableByName(tableName).getIndices().get(1);
        SQLIndex reloadedIndex2 = (SQLIndex) ppdb.getTableByName(tableName).getIndices().get(2);
        
        assertEquals(origIndex1.getChildCount(), reloadedIndex1.getChildCount());
        assertEquals(origIndex2.getChildCount(), reloadedIndex2.getChildCount());

    }
    
	public void testNotModifiedWhenFreshlyLoaded() throws Exception {
		testLoad();
		assertFalse("Freshly loaded project should not be marked dirty",
				project.isModified());
	}
	
	public void testSaveCoversCompareDMSettings() throws Exception {
		testLoad();
		CompareDMSettings cds = session.getCompareDMSettings();		
		File tmp = File.createTempFile("test", ".architect");
		assertFalse(cds.getSaveFlag());
		if (deleteOnExit) {
			tmp.deleteOnExit();
		}
		PrintWriter out = new PrintWriter(tmp, ENCODING);
		assertNotNull(out);
		project.save(out, ENCODING);
		assertFalse (cds.getSaveFlag());
		assertEquals(SQLServerDDLGenerator.class, cds.getDdlGenerator());
		assertEquals("ENGLISH", cds.getOutputFormatAsString());
		assertEquals("PROJECT", cds.getSourceSettings().getDatastoreType().toString());
		assertEquals("Arthur_test", cds.getSourceSettings().getConnectName());
		assertEquals("ARCHITECT_REGRESS", cds.getSourceSettings().getSchema());
		assertEquals("FILE", cds.getTargetSettings().getDatastoreType().toString());
		assertEquals("Testpath", cds.getTargetSettings().getFilePath());				
	}
    
    public void testSaveCoversCreateKettleJob() throws Exception {
        testLoad();
        
        Set<String> propertiesToIgnore = new HashSet<String>();
        propertiesToIgnore.add("class");
        propertiesToIgnore.add("cancelled");
        propertiesToIgnore.add("repository"); //TODO add test cases for repository

        Map<String,Object> oldDescription =
            TestUtils.setAllInterestingProperties(session.getKettleJob(), propertiesToIgnore);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        project.save(byteArrayOutputStream, ENCODING);

        System.out.println(byteArrayOutputStream.toString());

        SwingUIProjectLoader project2 = new SwingUIProjectLoader(session);
        project2.load(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), plIni);
        
        Map<String, Object> newDescription =
            ca.sqlpower.testutil.TestUtils.getAllInterestingProperties(session.getKettleJob(), propertiesToIgnore);
        
        assertMapsEqual(oldDescription, newDescription);
    }
    
    public void testSaveAndLoadCoversPlayPen() throws Exception{
        testLoad();
        
        PlayPen oldPP = session.getPlayPen();
        oldPP.setZoom(123.45);
        oldPP.setViewPosition(new Point(5,4));
        session.setRelationshipLinesDirect(false);
        session.setShowPkTag(false);
        session.setShowFkTag(false);
        session.setShowAkTag(false);
        session.setColumnVisibility(ColumnVisibility.ALL);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        project.save(byteArrayOutputStream, ENCODING);

        System.out.println(byteArrayOutputStream.toString());

        SwingUIProjectLoader project2 = new SwingUIProjectLoader(session);
        project2.load(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), plIni);
        
        PlayPen newPP = project2.getSession().getPlayPen();
        ArchitectSwingSession newSession = project2.getSession();
        assertEquals(oldPP.getZoom(), newPP.getZoom());
        assertEquals(oldPP.getViewPosition().getX(), newPP.getViewPosition().getX());
        assertEquals(oldPP.getViewPosition().getY(), newPP.getViewPosition().getY());
        assertEquals("Relationship Line Style", session.getRelationshipLinesDirect(), newSession.getRelationshipLinesDirect());
        assertEquals("PK Tag", session.isShowPkTag(), newSession.isShowPkTag());
        assertEquals("FK Tag", session.isShowFkTag(), newSession.isShowFkTag());
        assertEquals("AK Tag", session.isShowAkTag(), newSession.isShowAkTag());
        assertEquals("Show Option", session.getColumnVisibility(), newSession.getColumnVisibility());
    }
    
    public void testLoadCoversPlayPen()throws Exception{
        BasicConfigurator.configure();
//        Logger.getRootLogger().setLevel(Level.DEBUG);
        testLoad();
        
        PlayPen oldPP = project.getSession().getPlayPen();
        
        assertEquals(12.3, oldPP.getZoom());
        assertEquals(20, oldPP.getViewPosition().y);
        assertEquals(200, oldPP.getViewPosition().x);
        assertEquals("Relationship Line Style", false, session.getRelationshipLinesDirect());
        assertEquals("PK Tag", true, session.isShowPkTag());
        assertEquals("FK Tag", true, session.isShowFkTag());
        assertEquals("AK Tag", true, session.isShowAkTag());
        assertEquals("ShowOption", ColumnVisibility.PK_FK, session.getColumnVisibility());
    }
    
    /**
     * Test for regression of bug 1288. This version has catalogs and schemas.
     */
    public void testSaveDoesntCausePopulateCatalogSchema() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SQLObject dbtreeRoot = (SQLObject) session.getSourceDatabases().getModel().getRoot();

        JDBCDataSource ds = new JDBCDataSource(new PlDotIni());
        ds.setDisplayName("test_database");
        ds.getParentType().setJdbcDriver(MockJDBCDriver.class.getName());
        ds.setUser("fake");
        ds.setPass("fake");
        //this creates a mock jdbc database with catalogs and schemas
        ds.setUrl("jdbc:mock:dbmd.catalogTerm=Catalog&dbmd.schemaTerm=Schema&catalogs=cow_catalog&schemas.cow_catalog=moo_schema,quack_schema&tables.cow_catalog.moo_schema=braaaap,pffft&tables.cow_catalog.quack_schema=duck,goose");

        SQLDatabase db = new SQLDatabase(ds);
        dbtreeRoot.addChild(db);
        
        project.save(out, ENCODING);
        
        SQLTable tab = db.getTableByName("cow_catalog", "moo_schema", "braaaap");
        assertFalse(tab.isColumnsPopulated());
        assertFalse(tab.isRelationshipsPopulated());
        assertFalse(tab.isIndicesPopulated());
    }

    /**
     * Test for regression of bug 1288. This version has schemas only (no catalogs).
     */
    public void testSaveDoesntCausePopulateSchema() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SQLObject dbtreeRoot = (SQLObject) session.getSourceDatabases().getModel().getRoot();

        JDBCDataSource ds = new JDBCDataSource(new PlDotIni());
        ds.setDisplayName("test_database");
        ds.getParentType().setJdbcDriver(MockJDBCDriver.class.getName());
        ds.setUser("fake");
        ds.setPass("fake");
        //this creates a mock jdbc database with catalogs and schemas
        ds.setUrl("jdbc:mock:dbmd.schemaTerm=Schema&schemas=moo_schema,quack_schema&tables.moo_schema=braaaap,pffft&tables.quack_schema=duck,goose");

        SQLDatabase db = new SQLDatabase(ds);
        dbtreeRoot.addChild(db);
   
        SQLSchema mooSchema = db.getChildByName("moo_schema", SQLSchema.class);
        
        // we were only running into this bug on schemas that are already populated
        // so this step is the key to waking up the bug!
        mooSchema.populate();
        
        project.save(out, ENCODING);
        
        SQLTable tab = db.getTableByName(null, "moo_schema", "braaaap");
        assertFalse(tab.isColumnsPopulated());
        assertFalse(tab.isRelationshipsPopulated());
        assertFalse(tab.isIndicesPopulated());
    }

    public void testSaveThrowsExceptionForUnknownSQLObjectSubclass() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SQLObject dbtreeRoot = (SQLObject) session.getSourceDatabases().getModel().getRoot();

        SQLDatabase db = new TestSQLDatabase();
        dbtreeRoot.addChild(db);
        
        StubSQLObject sso = new StubSQLObject();
        db.addChild(sso);
        
        try {
            project.save(out, ENCODING);
            fail("No exception when trying to save unknown type of SQLObject");
        } catch (UnsupportedOperationException ex) {
            // expected result
        }
    }
    
    /**
     * Ensures the profile results in the sample project file get read in properly.
     */
    public void testLoadProfileResults() throws Exception {
        testLoad();
        ProfileManager pm = session.getProfileManager();
        
        assertEquals(1, pm.getResults().size());
        TableProfileResult tpr = pm.getResults().get(0);
        assertEquals("Customers", tpr.getProfiledObject().getName());

        assertEquals(1, tpr.getColumnProfileResults().size());
        ColumnProfileResult cpr = tpr.getColumnProfileResults().get(0);
        assertEquals("id", cpr.getProfiledObject().getName());
        assertEquals(5, cpr.getMinLength());
        assertEquals(6, cpr.getMaxLength());
    }
    
    /**
     * Checks the entire object tree loaded in to ensure all the
     * parent references point to the parents we found the children
     * in.  This actually was a problem when using a CoreProject from
     * the matchmaker app.
     */
    public void testParentsConnectedCoreProject() throws Exception {
        // testing using a core project, just for fun
        ArchitectSessionContext ctx = new TestingArchitectSessionContext();
        ArchitectSession session = ctx.createSession(new BufferedInputStream(new StringInputStream(testData)));
        ProjectLoader prj = session.getProjectLoader();
        SQLObjectRoot rootObject = session.getRootObject();
        recursiveCheckParenting(rootObject, "Root");
    }

    /**
     * The "target" database (the one we edit in the playpen) needs to
     * be marked as such, because it has different behaviour with regards
     * to resetting and connecting.
     */
    public void testPlayPenProperty() throws Exception {
        testLoad();
        SQLDatabase ppdb = (SQLDatabase) project.getSession().getRootObject().getChild(0);
        assertTrue(ppdb.isPlayPenDatabase());
    }

    /**
     * Subroutine of testParentsConnected.
     */
    private void recursiveCheckParenting(SQLObject o, String path) throws Exception {
        System.out.println("Checking children of " + path);
        for (Iterator it = o.getChildren().iterator(); it.hasNext();) {
            SQLObject child = (SQLObject) it.next();
            if (o instanceof SQLObjectRoot) {
                // skip, because database parent pointers are null
            } else if (child instanceof SQLRelationship && ((SQLRelationship) child).getFkTable().equals(o)) {
                // skip, because the primary key table should be the parent
            } else {
                assertSame(path, o, child.getParent());
            }
            recursiveCheckParenting(child, path + "/" + child.getName());
        }
    }
    
    /**
     * The "target" database (the one we edit in the playpen) needs to
     * be marked as such, because it has different behaviour with regards
     * to resetting and connecting.
     */
    public void testPlayPenPropertyCoreProject() throws Exception {
        ArchitectSessionContext ctx = new TestingArchitectSessionContext();
        ArchitectSession session = ctx.createSession(new BufferedInputStream(new StringInputStream(testData)));
        ProjectLoader prj = session.getProjectLoader();
        SQLObjectRoot rootObject = session.getRootObject();
        SQLDatabase ppdb = (SQLDatabase) rootObject.getChild(0);
        assertTrue(ppdb.isPlayPenDatabase());
    }
    
    /**
     * Regression test for bug 1662: after loading a project, all the indexes
     * have to be listening to the columns folder of the table they belong to.
     */
    public void testIndexListensToColumnsAfterLoad() throws Exception {
        testLoad();
        SQLDatabase ppdb = session.getTargetDatabase(); 
        SQLTable t = ppdb.getTableByName("mm_project");
        
        // have to do the same check for every index, because (for example)
        // the PK index didn't exhibit this problem
        for (SQLIndex idx : new ArrayList<SQLIndex>(t.getIndices())) {

            assertTrue(
                    "didn't expect this index to have no columns!",
                    idx.getChildCount() > 0);
            
            List<SQLIndex.Column> indexCols = idx.getChildren(SQLIndex.Column.class);
            List<SQLColumn> colsToRemove = new ArrayList<SQLColumn>();
            for (SQLIndex.Column indexCol : indexCols) {
                colsToRemove.add(indexCol.getColumn());
            }

            for (SQLColumn col : colsToRemove) {
                t.removeChild(col);
            }

            // prove the listener was hooked up
            assertTrue(
                    "Index " + idx + " wasn't listening to columns folder!",
                    idx.getChildCount() == 0);
            if (!idx.isPrimaryKeyIndex()) {
                assertFalse(t.getIndices().contains(idx));
            }
        }
    }
    
    public void testSaveStoresExceptionInOutput() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SQLObject dbtreeRoot = (SQLObject) session.getSourceDatabases().getModel().getRoot();

        SQLDatabase db = new TestSQLDatabase();
        dbtreeRoot.addChild(db);
        
        StubSQLObject sso = new StubSQLObject();
        db.addChild(sso);
        
        try {
            project.save(out, ENCODING);
            fail("No exception when trying to save unknown type of SQLObject");
        } catch (UnsupportedOperationException ex) {
            // expected result
        }
        
        assertTrue(out.toString().contains("UnsupportedOperationException"));
        System.out.println(out.toString());
    }
    
    /**
     * Regression testing for a bug on the forum 2147. When a project with olap
     * sessions is saved and loaded the loaded session will have twice the number
     * of olap sessions.
     */
    public void testSaveAndLoadDoesNotCreateOLAPSessions() throws Exception {
        
        ArchitectSwingSession session = new ArchitectSwingSessionImpl(context, "Test session");
        
        OLAPSession osession = new OLAPSession(new MondrianModel.Schema());
        osession.setDatabase(session.getTargetDatabase());
        session.getOLAPRootObject().addChild(osession);
        session.getOLAPEditSession(osession);
        assertEquals(1, session.getOLAPEditSessions().size());
        
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        session.getProjectLoader().save(byteArrayOutputStream, ENCODING);

        System.out.println(byteArrayOutputStream.toString());

        ArchitectSwingSession session2 = new ArchitectSwingSessionImpl(context, "Load session");
        SwingUIProjectLoader project2 = new SwingUIProjectLoader(session2);
        project2.load(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), plIni);
        
        assertEquals(1, session2.getOLAPEditSessions().size());
    }
    
    /**
     * This tests loading an ascending column on an index works in an older way.
     * This test is to confirm that we keep backwards compatibility.
     */
    public void testHistoricAscendingCol() throws Exception {
        String testData =
            "<?xml version='1.0'?>" +
            "<architect-project version='0.1'>" +
            " <project-name>TestSwingUIProject</project-name>" +
            " <project-data-sources>" +
            "  <data-source id='DS0'>" +
            "   <property key='Logical' value='Not Configured' />" +
            "  </data-source>" +
            " </project-data-sources>" +
            " <target-database dbcs-ref='DS0'>" +
            "  <table id=\"TAB1830\" populated=\"true\" name=\"mm_project\" objectType=\"TABLE\" physicalName=\"MM_PROJECT\" remarks=\"\" >" +
            "   <folder id=\"FOL1831\" populated=\"true\" name=\"Columns\" physicalName=\"Columns\" type=\"1\" >" +
            "    <column id=\"COL1832\" populated=\"true\" autoIncrement=\"true\" autoIncrementSequenceName=\"mm_project_oid_seq\" name=\"project_oid\" nullable=\"0\" physicalName=\"PROJECT_OID\" precision=\"22\" primaryKeySeq=\"0\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"4\" />" +
            "    <column id=\"COL1833\" populated=\"true\" autoIncrement=\"false\" name=\"FOLDER_OID\" nullable=\"1\" physicalName=\"FOLDER_OID\" precision=\"22\" referenceCount=\"2\" remarks=\"\" scale=\"0\" type=\"4\" />" +
            "    <column id=\"COL1834\" populated=\"true\" autoIncrement=\"false\" name=\"project_name\" nullable=\"1\" physicalName=\"PROJECT_NAME\" precision=\"80\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"12\" />" +
            "   </folder>" +
            "   <folder id=\"FOL1889\" populated=\"true\" name=\"Exported Keys\" physicalName=\"Exported Keys\" type=\"3\" >" +
            "   </folder>" +
            "   <folder id=\"FOL1890\" populated=\"true\" name=\"Imported Keys\" physicalName=\"Imported Keys\" type=\"2\" >" +
            "   </folder>" +
            "   <folder id=\"FOL1891\" populated=\"true\" name=\"Indices\" physicalName=\"Indices\" type=\"4\" >" +
            "    <index id=\"IDX1894\" populated=\"true\" index-type=\"BTREE\" name=\"PL_MATCH_UNIQUE\" physicalName=\"PL_MATCH_UNIQUE\" primaryKeyIndex=\"false\" unique=\"true\" >" +
            "     <index-column id=\"IDC1895\" populated=\"true\" ascending=\"true\" column-ref=\"COL1834\" descending=\"false\" name=\"project_name\" physicalName=\"MATCH_ID\" />" +
            "    </index>" +
            "   </folder>" +
            "  </table>" +
            " </target-database>" +
            "</architect-project>";
        
        ByteArrayInputStream r = new ByteArrayInputStream(testData.getBytes());
        project.load(r, plIni);
        SQLTable table = session.getPlayPen().getTables().get(0);
        assertEquals(AscendDescend.ASCENDING, table.getIndexByName("PL_MATCH_UNIQUE").getChildren(SQLIndex.Column.class).get(0).getAscendingOrDescending());
    }
    
    /**
     * This tests loading a descending column on an index works in an older way.
     * This test is to confirm that we keep backwards compatibility.
     */
    public void testHistoricDescendingCol() throws Exception {
        String testData =
            "<?xml version='1.0'?>" +
            "<architect-project version='0.1'>" +
            " <project-name>TestSwingUIProject</project-name>" +
            " <project-data-sources>" +
            "  <data-source id='DS0'>" +
            "   <property key='Logical' value='Not Configured' />" +
            "  </data-source>" +
            " </project-data-sources>" +
            " <target-database dbcs-ref='DS0'>" +
            "  <table id=\"TAB1830\" populated=\"true\" name=\"mm_project\" objectType=\"TABLE\" physicalName=\"MM_PROJECT\" remarks=\"\" >" +
            "   <folder id=\"FOL1831\" populated=\"true\" name=\"Columns\" physicalName=\"Columns\" type=\"1\" >" +
            "    <column id=\"COL1832\" populated=\"true\" autoIncrement=\"true\" autoIncrementSequenceName=\"mm_project_oid_seq\" name=\"project_oid\" nullable=\"0\" physicalName=\"PROJECT_OID\" precision=\"22\" primaryKeySeq=\"0\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"4\" />" +
            "    <column id=\"COL1833\" populated=\"true\" autoIncrement=\"false\" name=\"FOLDER_OID\" nullable=\"1\" physicalName=\"FOLDER_OID\" precision=\"22\" referenceCount=\"2\" remarks=\"\" scale=\"0\" type=\"4\" />" +
            "    <column id=\"COL1834\" populated=\"true\" autoIncrement=\"false\" name=\"project_name\" nullable=\"1\" physicalName=\"PROJECT_NAME\" precision=\"80\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"12\" />" +
            "   </folder>" +
            "   <folder id=\"FOL1889\" populated=\"true\" name=\"Exported Keys\" physicalName=\"Exported Keys\" type=\"3\" >" +
            "   </folder>" +
            "   <folder id=\"FOL1890\" populated=\"true\" name=\"Imported Keys\" physicalName=\"Imported Keys\" type=\"2\" >" +
            "   </folder>" +
            "   <folder id=\"FOL1891\" populated=\"true\" name=\"Indices\" physicalName=\"Indices\" type=\"4\" >" +
            "    <index id=\"IDX1894\" populated=\"true\" index-type=\"BTREE\" name=\"PL_MATCH_UNIQUE\" physicalName=\"PL_MATCH_UNIQUE\" primaryKeyIndex=\"false\" unique=\"true\" >" +
            "     <index-column id=\"IDC1895\" populated=\"true\" ascending=\"false\" column-ref=\"COL1834\" descending=\"true\" name=\"project_name\" physicalName=\"MATCH_ID\" />" +
            "    </index>" +
            "   </folder>" +
            "  </table>" +
            " </target-database>" +
            "</architect-project>";
        
        ByteArrayInputStream r = new ByteArrayInputStream(testData.getBytes());
        project.load(r, plIni);
        SQLTable table = session.getPlayPen().getTables().get(0);
        assertEquals(AscendDescend.DESCENDING, table.getIndexByName("PL_MATCH_UNIQUE").getChildren(SQLIndex.Column.class).get(0).getAscendingOrDescending());
    }
    
    /**
     * This tests loading an unspecified column on an index works in an older way.
     * This test is to confirm that we keep backwards compatibility.
     */
    public void testHistoricUnspecifiedCol() throws Exception {
        String testData =
            "<?xml version='1.0'?>" +
            "<architect-project version='0.1'>" +
            " <project-name>TestSwingUIProject</project-name>" +
            " <project-data-sources>" +
            "  <data-source id='DS0'>" +
            "   <property key='Logical' value='Not Configured' />" +
            "  </data-source>" +
            " </project-data-sources>" +
            " <target-database dbcs-ref='DS0'>" +
            "  <table id=\"TAB1830\" populated=\"true\" name=\"mm_project\" objectType=\"TABLE\" physicalName=\"MM_PROJECT\" remarks=\"\" >" +
            "   <folder id=\"FOL1831\" populated=\"true\" name=\"Columns\" physicalName=\"Columns\" type=\"1\" >" +
            "    <column id=\"COL1832\" populated=\"true\" autoIncrement=\"true\" autoIncrementSequenceName=\"mm_project_oid_seq\" name=\"project_oid\" nullable=\"0\" physicalName=\"PROJECT_OID\" precision=\"22\" primaryKeySeq=\"0\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"4\" />" +
            "    <column id=\"COL1833\" populated=\"true\" autoIncrement=\"false\" name=\"FOLDER_OID\" nullable=\"1\" physicalName=\"FOLDER_OID\" precision=\"22\" referenceCount=\"2\" remarks=\"\" scale=\"0\" type=\"4\" />" +
            "    <column id=\"COL1834\" populated=\"true\" autoIncrement=\"false\" name=\"project_name\" nullable=\"1\" physicalName=\"PROJECT_NAME\" precision=\"80\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"12\" />" +
            "   </folder>" +
            "   <folder id=\"FOL1889\" populated=\"true\" name=\"Exported Keys\" physicalName=\"Exported Keys\" type=\"3\" >" +
            "   </folder>" +
            "   <folder id=\"FOL1890\" populated=\"true\" name=\"Imported Keys\" physicalName=\"Imported Keys\" type=\"2\" >" +
            "   </folder>" +
            "   <folder id=\"FOL1891\" populated=\"true\" name=\"Indices\" physicalName=\"Indices\" type=\"4\" >" +
            "    <index id=\"IDX1894\" populated=\"true\" index-type=\"BTREE\" name=\"PL_MATCH_UNIQUE\" physicalName=\"PL_MATCH_UNIQUE\" primaryKeyIndex=\"false\" unique=\"true\" >" +
            "     <index-column id=\"IDC1895\" populated=\"true\" ascending=\"false\" column-ref=\"COL1834\" descending=\"false\" name=\"project_name\" physicalName=\"MATCH_ID\" />" +
            "    </index>" +
            "   </folder>" +
            "  </table>" +
            " </target-database>" +
            "</architect-project>";
        
        ByteArrayInputStream r = new ByteArrayInputStream(testData.getBytes());
        project.load(r, plIni);
        SQLTable table = session.getPlayPen().getTables().get(0);
        assertEquals(AscendDescend.UNSPECIFIED, table.getIndexByName("PL_MATCH_UNIQUE").getChildren(SQLIndex.Column.class).get(0).getAscendingOrDescending());
    }
    
    /**
     * This tests loading an ascending column on an index works.
     */
    public void testAscendingCol() throws Exception {
        String testData =
            "<?xml version='1.0'?>" +
            "<architect-project version='0.1'>" +
            " <project-name>TestSwingUIProject</project-name>" +
            " <project-data-sources>" +
            "  <data-source id='DS0'>" +
            "   <property key='Logical' value='Not Configured' />" +
            "  </data-source>" +
            " </project-data-sources>" +
            " <target-database dbcs-ref='DS0'>" +
            "  <table id=\"TAB1830\" populated=\"true\" name=\"mm_project\" objectType=\"TABLE\" physicalName=\"MM_PROJECT\" remarks=\"\" >" +
            "   <folder id=\"FOL1831\" populated=\"true\" name=\"Columns\" physicalName=\"Columns\" type=\"1\" >" +
            "    <column id=\"COL1832\" populated=\"true\" autoIncrement=\"true\" autoIncrementSequenceName=\"mm_project_oid_seq\" name=\"project_oid\" nullable=\"0\" physicalName=\"PROJECT_OID\" precision=\"22\" primaryKeySeq=\"0\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"4\" />" +
            "    <column id=\"COL1833\" populated=\"true\" autoIncrement=\"false\" name=\"FOLDER_OID\" nullable=\"1\" physicalName=\"FOLDER_OID\" precision=\"22\" referenceCount=\"2\" remarks=\"\" scale=\"0\" type=\"4\" />" +
            "    <column id=\"COL1834\" populated=\"true\" autoIncrement=\"false\" name=\"project_name\" nullable=\"1\" physicalName=\"PROJECT_NAME\" precision=\"80\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"12\" />" +
            "   </folder>" +
            "   <folder id=\"FOL1889\" populated=\"true\" name=\"Exported Keys\" physicalName=\"Exported Keys\" type=\"3\" >" +
            "   </folder>" +
            "   <folder id=\"FOL1890\" populated=\"true\" name=\"Imported Keys\" physicalName=\"Imported Keys\" type=\"2\" >" +
            "   </folder>" +
            "   <folder id=\"FOL1891\" populated=\"true\" name=\"Indices\" physicalName=\"Indices\" type=\"4\" >" +
            "    <index id=\"IDX1894\" populated=\"true\" index-type=\"CLUSTERED\" name=\"PL_MATCH_UNIQUE\" physicalName=\"PL_MATCH_UNIQUE\" primaryKeyIndex=\"false\" unique=\"true\" >" +
            "     <index-column id=\"IDC1895\" populated=\"true\" ascendingOrDescending=\"ASCENDING\" column-ref=\"COL1834\" name=\"project_name\" physicalName=\"MATCH_ID\" />" +
            "    </index>" +
            "   </folder>" +
            "  </table>" +
            " </target-database>" +
            "</architect-project>";
        
        ByteArrayInputStream r = new ByteArrayInputStream(testData.getBytes());
        project.load(r, plIni);
        SQLTable table = session.getPlayPen().getTables().get(0);
        assertEquals(AscendDescend.ASCENDING, table.getIndexByName("PL_MATCH_UNIQUE").getChildren(SQLIndex.Column.class).get(0).getAscendingOrDescending());
    }
    /**
     * This tests loading a descending column on an index works.
     */
    public void testDescendingCol() throws Exception {
        String testData =
            "<?xml version='1.0'?>" +
            "<architect-project version='0.1'>" +
            " <project-name>TestSwingUIProject</project-name>" +
            " <project-data-sources>" +
            "  <data-source id='DS0'>" +
            "   <property key='Logical' value='Not Configured' />" +
            "  </data-source>" +
            " </project-data-sources>" +
            " <target-database dbcs-ref='DS0'>" +
            "  <table id=\"TAB1830\" populated=\"true\" name=\"mm_project\" objectType=\"TABLE\" physicalName=\"MM_PROJECT\" remarks=\"\" >" +
            "   <folder id=\"FOL1831\" populated=\"true\" name=\"Columns\" physicalName=\"Columns\" type=\"1\" >" +
            "    <column id=\"COL1832\" populated=\"true\" autoIncrement=\"true\" autoIncrementSequenceName=\"mm_project_oid_seq\" name=\"project_oid\" nullable=\"0\" physicalName=\"PROJECT_OID\" precision=\"22\" primaryKeySeq=\"0\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"4\" />" +
            "    <column id=\"COL1833\" populated=\"true\" autoIncrement=\"false\" name=\"FOLDER_OID\" nullable=\"1\" physicalName=\"FOLDER_OID\" precision=\"22\" referenceCount=\"2\" remarks=\"\" scale=\"0\" type=\"4\" />" +
            "    <column id=\"COL1834\" populated=\"true\" autoIncrement=\"false\" name=\"project_name\" nullable=\"1\" physicalName=\"PROJECT_NAME\" precision=\"80\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"12\" />" +
            "   </folder>" +
            "   <folder id=\"FOL1889\" populated=\"true\" name=\"Exported Keys\" physicalName=\"Exported Keys\" type=\"3\" >" +
            "   </folder>" +
            "   <folder id=\"FOL1890\" populated=\"true\" name=\"Imported Keys\" physicalName=\"Imported Keys\" type=\"2\" >" +
            "   </folder>" +
            "   <folder id=\"FOL1891\" populated=\"true\" name=\"Indices\" physicalName=\"Indices\" type=\"4\" >" +
            "    <index id=\"IDX1894\" populated=\"true\" index-type=\"BTREE\" name=\"PL_MATCH_UNIQUE\" physicalName=\"PL_MATCH_UNIQUE\" primaryKeyIndex=\"false\" unique=\"true\" >" +
            "     <index-column id=\"IDC1895\" populated=\"true\" ascendingOrDescending=\"DESCENDING\" column-ref=\"COL1834\" name=\"project_name\" physicalName=\"MATCH_ID\" />" +
            "    </index>" +
            "   </folder>" +
            "  </table>" +
            " </target-database>" +
            "</architect-project>";
        
        ByteArrayInputStream r = new ByteArrayInputStream(testData.getBytes());
        project.load(r, plIni);
        SQLTable table = session.getPlayPen().getTables().get(0);
        assertEquals(AscendDescend.DESCENDING, table.getIndexByName("PL_MATCH_UNIQUE").getChildren(SQLIndex.Column.class).get(0).getAscendingOrDescending());
    }
    /**
     * This tests loading an unspecified column on an index works.
     */
    public void testUnspecifiedCol() throws Exception {
        String testData =
            "<?xml version='1.0'?>" +
            "<architect-project version='0.1'>" +
            " <project-name>TestSwingUIProject</project-name>" +
            " <project-data-sources>" +
            "  <data-source id='DS0'>" +
            "   <property key='Logical' value='Not Configured' />" +
            "  </data-source>" +
            " </project-data-sources>" +
            " <target-database dbcs-ref='DS0'>" +
            "  <table id=\"TAB1830\" populated=\"true\" name=\"mm_project\" objectType=\"TABLE\" physicalName=\"MM_PROJECT\" remarks=\"\" >" +
            "   <folder id=\"FOL1831\" populated=\"true\" name=\"Columns\" physicalName=\"Columns\" type=\"1\" >" +
            "    <column id=\"COL1832\" populated=\"true\" autoIncrement=\"true\" autoIncrementSequenceName=\"mm_project_oid_seq\" name=\"project_oid\" nullable=\"0\" physicalName=\"PROJECT_OID\" precision=\"22\" primaryKeySeq=\"0\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"4\" />" +
            "    <column id=\"COL1833\" populated=\"true\" autoIncrement=\"false\" name=\"FOLDER_OID\" nullable=\"1\" physicalName=\"FOLDER_OID\" precision=\"22\" referenceCount=\"2\" remarks=\"\" scale=\"0\" type=\"4\" />" +
            "    <column id=\"COL1834\" populated=\"true\" autoIncrement=\"false\" name=\"project_name\" nullable=\"1\" physicalName=\"PROJECT_NAME\" precision=\"80\" referenceCount=\"1\" remarks=\"\" scale=\"0\" type=\"12\" />" +
            "   </folder>" +
            "   <folder id=\"FOL1889\" populated=\"true\" name=\"Exported Keys\" physicalName=\"Exported Keys\" type=\"3\" >" +
            "   </folder>" +
            "   <folder id=\"FOL1890\" populated=\"true\" name=\"Imported Keys\" physicalName=\"Imported Keys\" type=\"2\" >" +
            "   </folder>" +
            "   <folder id=\"FOL1891\" populated=\"true\" name=\"Indices\" physicalName=\"Indices\" type=\"4\" >" +
            "    <index id=\"IDX1894\" populated=\"true\" index-type=\"BTREE\" name=\"PL_MATCH_UNIQUE\" physicalName=\"PL_MATCH_UNIQUE\" primaryKeyIndex=\"false\" unique=\"true\" >" +
            "     <index-column id=\"IDC1895\" populated=\"true\" ascendingOrDescending=\"UNSPECIFIED\" column-ref=\"COL1834\" name=\"project_name\" physicalName=\"MATCH_ID\" />" +
            "    </index>" +
            "   </folder>" +
            "  </table>" +
            " </target-database>" +
            "</architect-project>";
        
        ByteArrayInputStream r = new ByteArrayInputStream(testData.getBytes());
        project.load(r, plIni);
        SQLTable table = session.getPlayPen().getTables().get(0);
        assertEquals(AscendDescend.UNSPECIFIED, table.getIndexByName("PL_MATCH_UNIQUE").getChildren(SQLIndex.Column.class).get(0).getAscendingOrDescending());
    }

}