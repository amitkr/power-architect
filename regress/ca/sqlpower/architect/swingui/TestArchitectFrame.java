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

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.prefs.Preferences;

import junit.framework.TestCase;

public class TestArchitectFrame extends TestCase {
	
	private ArchitectFrame af;
    private ArchitectSwingSession session;
    
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
        " <play-pen zoom=\"12.3\" viewportX=\"200\" viewportY=\"20\">" +
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

	@Override
	protected void setUp() throws Exception {
		super.setUp();
        TestingArchitectSwingSessionContext context = new TestingArchitectSwingSessionContext();
		session = context.createSession();
        af = session.getArchitectFrame();
        
        File tmp = File.createTempFile("Architect", "Test");
        
        FileWriter fw = new FileWriter(tmp);
        fw.write(testData);
        fw.close();
        
        ArchitectFrame.main(new String[]{tmp.getAbsolutePath()});
	}
	
	public void testAutoLayoutAction() {
		assertNotNull(af.getAutoLayoutAction());
		assertSame(session.getPlayPen(), af.getAutoLayoutAction().getPlayPen());
		
		// FIXME: should check that the toolbar button of the action exists!
	}
	
	/**
	 * Regression test for 1336. Loading a file from the command prompt should be remembered.
	 */
	public void testFileLoadFromCMDPrompt() throws Exception {
	    Preferences prefs = Preferences.userNodeForPackage(ArchitectSwingSessionImpl.class);

	    String recentFile = prefs.get("recentFile0", null);
	    
	    System.out.println(recentFile);
	    System.out.println("Key count " + prefs.keys().length);
	    System.out.println("Keys are " + Arrays.toString(prefs.keys()));
	}
}
