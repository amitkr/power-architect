package regress.ca.sqlpower.architect;

import java.beans.PropertyChangeEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import ca.sqlpower.architect.ArchitectDataSource;
import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.SQLDatabase;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLTable;

public class TestSQLDatabase extends SQLTestCase {
	
	/**
	 * Creates a wrapper around the normal test suite which runs the
	 * OneTimeSetup and OneTimeTearDown procedures.
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTestSuite(TestSQLDatabase.class);
		TestSetup wrapper = new TestSetup(suite) {
			protected void setUp() throws Exception {
				oneTimeSetUp();
			}
			protected void tearDown() throws Exception {
				oneTimeTearDown();
			}
		};
		return wrapper;
	}

	/**
	 * One-time initialization code.  The special {@link #suite()} method arranges for
	 * this method to be called one time before the individual tests are run.
	 * @throws Exception 
	 */
	public static void oneTimeSetUp() throws Exception {
		System.out.println("TestSQLDatabase.oneTimeSetUp()");
		SQLDatabase mydb = new SQLDatabase(getDataSource());
		Connection con = mydb.getConnection();
		Statement stmt = null;

		try {
			stmt = con.createStatement();
			
			/*
			 * Setting up a clean db for each of the tests
			 */
			try {
				stmt.executeUpdate("DROP TABLE REGRESSION_TEST1");
				stmt.executeUpdate("DROP TABLE REGRESSION_TEST2");
			}
			catch (SQLException sqle ){
				System.out.println("+++ TestSQLDatabase exception should be for dropping a non-existant table");
				sqle.printStackTrace();
			}
			
			stmt.executeUpdate("CREATE TABLE REGRESSION_TEST1 (t1_c1 numeric(10))");
			stmt.executeUpdate("CREATE TABLE REGRESSION_TEST2 (t2_c1 char(10))");
			
		} finally {
			if (stmt != null) stmt.close();
			mydb.disconnect();
		}
	}

	/**
	 * One-time cleanup code.  The special {@link #suite()} method arranges for
	 * this method to be called one time before the individual tests are run.
	 */
	public static void oneTimeTearDown() {
		System.out.println("TestSQLDatabase.oneTimeTearDown()");
	}

	public TestSQLDatabase(String name) throws Exception {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testGoodConnect() throws ArchitectException {
		assertFalse("db shouldn't have been connected yet", db.isConnected());
		Connection con = db.getConnection();
		assertNotNull("db gave back a null connection", con);
		assertTrue("db should have said it is connected", db.isConnected());
	}

	public void testPopulate() throws ArchitectException {
		db.getConnection(); // causes db to actually connect
		assertFalse("even though connected, should not be populated yet", db.isPopulated());
		db.populate();
		assertTrue("should be populated now", db.isPopulated());

		db.populate(); // it must be allowed to call populate multiple times
	}

	public void testGetTableByName() throws ArchitectException {
		SQLTable table1, table2;
		assertNotNull(table1 = db.getTableByName("REGRESSION_TEST1"));
		assertNotNull(table2 = db.getTableByName("REGRESSION_TEST2"));
		assertNull("should get null for nonexistant table", db.getTableByName("no_such_table"));
		//XXX: test table under catalog and schema
	}
	
	public void testGetSchemaByName() {
		//XXX:
	}
	
	
	public void testIgnoreReset() throws ArchitectException
	{
		
		// Cause db to connect
		db.getChild(0);
		
		db.setIgnoreReset(true);
		assertTrue(db.getIgnoreReset());
		
		db.setDataSource(db.getDataSource());
		assertTrue(db.isPopulated());
		
		db.setIgnoreReset(false);
		assertFalse(db.getIgnoreReset());
		db.setDataSource(db.getDataSource());
		assertFalse(db.isPopulated());
		
	}

	public void testReconnect() throws ArchitectException {
		
		// cause db to actually connect
		assertNotNull(db.getChild(0));

		// cause disconnection
		db.setDataSource(db.getDataSource());
		assertFalse("db shouldn't be connected anymore", db.isConnected());
		assertFalse("db shouldn't be populated anymore", db.isPopulated());

		assertNotNull(db.getChild(1));

		assertTrue("db should be repopulated", db.isPopulated());
		assertTrue("db should be reconnected", db.isConnected());
		assertNotNull("db should be reconnected", db.getConnection());
	}

	public void testMissingDriverConnect() {
		ArchitectDataSource ds = db.getDataSource();
		ds.setDriverClass("ca.sqlpower.xxx.does.not.exist");
		
		SQLDatabase mydb = new SQLDatabase(ds);
		Connection con = null;
		ArchitectException exc = null;
		try {
			assertFalse("db shouldn't have been connected yet", db.isConnected());
			con = mydb.getConnection();
		} catch (ArchitectException e) {
			exc = e;
		}
		assertNotNull("should have got an ArchitectException", exc);
		// XXX: this test should be re-enabled when the product has I18N implemented.
		//assertEquals("error message should have been dbconnect.noDriver", "dbconnect.noDriver", exc.getMessage());
		assertNull("connection should be null", con);
	}

	public void testBadURLConnect() {
		ArchitectDataSource ds = db.getDataSource();
		ds.setUrl("jdbc:bad:moo");
		
		SQLDatabase mydb = new SQLDatabase(ds);
		Connection con = null;
		ArchitectException exc = null;
		try {
			assertFalse("db shouldn't have been connected yet", db.isConnected());
			con = mydb.getConnection();
		} catch (ArchitectException e) {
			exc = e;
		}
		assertNotNull("should have got an ArchitectException", exc);
//		XXX: this test should be re-enabled when the product has I18N implemented.
		//assertEquals("error message should have been dbconnect.connectionFailed", "dbconnect.connectionFailed", exc.getMessage());
		assertNull("connection should be null", con);
	}

	public void testBadPasswordConnect() {
		ArchitectDataSource ds = db.getDataSource();
		ds.setPass("foofoofoofoofooSDFGHJK");  // XXX: if this is the password, we lose.
		
		SQLDatabase mydb = new SQLDatabase(ds);
		Connection con = null;
		ArchitectException exc = null;
		try {
			assertFalse("db shouldn't have been connected yet", db.isConnected());
			con = mydb.getConnection();
		} catch (ArchitectException e) {
			exc = e;
		}
		assertNotNull("should have got an ArchitectException", exc);
		// XXX: this test should be re-enabled when the product has I18N implemented.
		// assertEquals("error message should have been dbconnect.connectionFailed", "dbconnect.connectionFailed", exc.getMessage());
		assertNull("connection should be null", con);
	}
	
	public void testPropertyChange() throws Exception
	{
		try {
			PropertyChangeEvent e = new PropertyChangeEvent(null, null,"1", "2");		
			fail("Property change event didn't reject null source;" + e);
		} catch (IllegalArgumentException ile) {
			System.out.println("Caught expected exception.");
		}
		PropertyChangeEvent e = new PropertyChangeEvent(this, null,"1", "2");		
		db.propertyChange(e);
	}
	
	
	public void testUnpopulatedDB(){
		assertFalse(db.isPopulated());
	}

	public void testAutoPopulate() throws Exception {
		assertFalse(db.isPopulated());		
		SQLObject child = db.getChild(0);
		assertTrue(db.isPopulated());
		assertFalse(child.isPopulated());
	}
	
	
	
}
