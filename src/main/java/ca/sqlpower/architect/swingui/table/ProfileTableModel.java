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
package ca.sqlpower.architect.swingui.table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ddl.DDLGenerator;
import ca.sqlpower.architect.ddl.DDLUtils;
import ca.sqlpower.architect.profile.ColumnProfileResult;
import ca.sqlpower.architect.profile.ColumnValueCount;
import ca.sqlpower.architect.profile.ProfileManager;
import ca.sqlpower.architect.profile.TableProfileResult;
import ca.sqlpower.architect.profile.event.ProfileChangeEvent;
import ca.sqlpower.architect.profile.event.ProfileChangeListener;
import ca.sqlpower.architect.profile.output.ProfileColumn;
import ca.sqlpower.object.SPObjectUtils;
import ca.sqlpower.sqlobject.SQLCatalog;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLObjectRuntimeException;
import ca.sqlpower.sqlobject.SQLSchema;
import ca.sqlpower.sqlobject.SQLTable;
import ca.sqlpower.swingui.table.CleanupTableModel;

public class ProfileTableModel extends AbstractTableModel implements CleanupTableModel {

    /**
     * Requesting the value at this column index will give back the
     * entire ColumnProfileResult object that provides the data for
     * the specified row.  This constant is package private because
     * no users outside the table package should be using it.
     *
     * <p>See {@link ProfileJTable#getColumnProfileResultForRow(int)}.
     */
    static final int CPR_PSEUDO_COLUMN_INDEX = -1;

    private static final Logger logger = Logger.getLogger(ProfileTableModel.class);

    /**
     * The source of data for this table model.
     */
    private final ProfileManager profileManager;

    /**
     * A list of profile results to show in the ProfileResultsViewer
     */
    private List<ColumnProfileResult> resultList;

    /**
     * Only tables in this list will have the results of their columns shown.
     */
    private List<TableProfileResult> tableResultsToScan = new ArrayList<TableProfileResult>();

    /**
     * Refreshes this table model (possibly firing a table model event) whenever
     * a change in the profile manager is detected.
     */
    private final ProfileChangeListener profileChangeHandler = new ProfileChangeListener() {
        public void profilesRemoved(ProfileChangeEvent e) { refresh(); }
        public void profilesAdded(ProfileChangeEvent e) { refresh(); }
        public void profileListChanged(ProfileChangeEvent e) { refresh(); }
    };

    /**
     * Creates a new table model attached to the given profile manager. Be sure
     * to call {@link #cleanup()} when this table model is no longer needed
     * (this will be done automatically if you are using a SQLPower enhanced
     * JTable; see {@link CleanupTableModel} for details).
     * 
     * @param profileManager The profile manager this table model is based on.
     */
    public ProfileTableModel(ProfileManager profileManager) {
        this.profileManager = profileManager;
        profileManager.addProfileChangeListener(profileChangeHandler);
        refresh();
    }
    
    /**
     * De-registers listeners that were installed when this table model was created.
     * To avoid memory leaks, it is important to call this cleanup method when the
     * table model is no longer needed.
     */
    public void cleanup() {
        logger.debug("Cleaning up...");
        profileManager.removeProfileChangeListener(profileChangeHandler);
    }

    @Override
    public String getColumnName(int col) {
        return ProfileColumn.values()[col].getName();
    }

    public int getRowCount() {
        return resultList.size();
    }

    public int getColumnCount() {
        return ProfileColumn.values().length;
    }

    /**
     * Get value at table cell(row,column), return the most top value on
     * column "TOP_VALUE", not the whole list!
     * @param rowIndex
     * @param columnIndex
     * @return
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        ColumnProfileResult cpr = resultList.get(rowIndex);
        if (columnIndex == CPR_PSEUDO_COLUMN_INDEX) {
            return cpr;
        } else {
            return getColumnValueFromProfile(ProfileColumn.values()[columnIndex], cpr);
        }
    }

    private static Object getColumnValueFromProfile(ProfileColumn column,
             ColumnProfileResult columnProfile) {
        SQLColumn col = columnProfile.getProfiledObject();
        int rowCount = columnProfile.getParentResult().getRowCount();

        switch(column) {
        case DATABASE:
            return SPObjectUtils.getAncestor(col,SQLDatabase.class);
        case CATALOG:
            return SPObjectUtils.getAncestor(col,SQLCatalog.class);
        case  SCHEMA:
            return SPObjectUtils.getAncestor(col,SQLSchema.class);
        case TABLE:
            return SPObjectUtils.getAncestor(col,SQLTable.class);
        case COLUMN:
            return col;
        case RUNDATE:
            return columnProfile.getCreateStartTime();
        case RECORD_COUNT:
            return rowCount;
        case DATA_TYPE:
            try {
                DDLGenerator gddl = DDLUtils.createDDLGenerator(col.getParent().getParentDatabase().getDataSource());
                return gddl.columnType(col);
            } catch (Exception e) {
                throw new SQLObjectRuntimeException(new SQLObjectException(
                        "Unable to get DDL information.  Do we have a valid data source?", e));
            }
        case NULL_COUNT:
            return columnProfile.getNullCount();
        case PERCENT_NULL:
            return rowCount == 0 ? null :  (double)columnProfile.getNullCount() / rowCount ;
        case UNIQUE_COUNT:
            return columnProfile.getDistinctValueCount();
        case  PERCENT_UNIQUE:
            return rowCount == 0 ? null : (double)columnProfile.getDistinctValueCount() / rowCount;
        case  MIN_LENGTH:
            return columnProfile.getMinLength();
        case  MAX_LENGTH:
            return columnProfile.getMaxLength();
        case  AVERAGE_LENGTH:
            return columnProfile.getAvgLength();
        case  MIN_VALUE:            //  min Value
            return columnProfile.getMinValue();
        case  MAX_VALUE:
            return columnProfile.getMaxValue();
        case  AVERAGE_VALUE:
            return columnProfile.getAvgValue();
        case  TOP_VALUE:
            return columnProfile.getValueCount();
        default:
            throw new IllegalArgumentException(
                    String.format("ProfileColumn enum value %s not handled", column));
        }
    }

     /**
      * Get top N value at table cell(row), on column "TOP_VALUE"
      * @param rowIndex
      * @return List of top N Value
      */
     public List<ColumnValueCount> getTopNValueAt(int rowIndex) {
         return resultList.get(rowIndex).getValueCount();
     }

    public boolean isErrorColumnProfile(int row) {
        ColumnProfileResult columnProfile = resultList.get(row);
        return columnProfile.getException() != null;

    }
    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public void refresh(){
        resultList = new ArrayList<ColumnProfileResult>();
        for (TableProfileResult tpr : tableResultsToScan) {
            for (ColumnProfileResult cpr : tpr.getColumnProfileResults()) {
                resultList.add(cpr);
            }
        }
        Collections.sort(resultList);
        fireTableDataChanged();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        ProfileColumn pc = ProfileColumn.values()[columnIndex];
        switch(pc) {
        case DATABASE:
            return SQLDatabase.class;
        case CATALOG:
            return SQLCatalog.class;
        case SCHEMA:
            return SQLSchema.class;
        case TABLE:
            return SQLTable.class;
        case COLUMN:
            return SQLColumn.class;
        case RUNDATE:
            return Long.class;
        case RECORD_COUNT:
            return Integer.class;
        case DATA_TYPE:
            return String.class;
        case NULL_COUNT:
            return Integer.class;
        case PERCENT_NULL:
            return BigDecimal.class;
        case UNIQUE_COUNT:
            return Integer.class;
        case PERCENT_UNIQUE:
            return BigDecimal.class;
        case MIN_LENGTH:
            return Integer.class;
        case MAX_LENGTH:
            return Integer.class;
        case AVERAGE_LENGTH:
            return BigDecimal.class;
        case MIN_VALUE:
            return Object.class;
        case MAX_VALUE:
            return Object.class;
        case AVERAGE_VALUE:
            return Object.class;
        case TOP_VALUE:
            return Object.class;
        default:
            throw new IllegalArgumentException(
                    String.format("ProfileColumn value %s unknown", pc));
        }
    }

    public List<ColumnProfileResult> getResultList() {
        return resultList;
    }
    
    public void addTableResultToScan(TableProfileResult tpr) {
        tableResultsToScan.add(tpr);
    }
    
    public List<TableProfileResult> getTableResultsToScan() {
        return tableResultsToScan;
    }

    public void clearScanList() {
        tableResultsToScan.clear();
    }

    public void removeTableResultToScan(TableProfileResult result) {
        tableResultsToScan.remove(result);
    }
}
