/*
 * Created September 28, 2006.
 * 
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.architect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.SQLTable.Folder;

/**
 * The SQLIndex class represents an index on a table in a relational database.
 * 
 * @author fuerth
 */
public class SQLIndex extends SQLObject {
    
    private static final Logger logger = Logger.getLogger(SQLIndex.class);

    /**
     * An enumeration of the types of indices that JDBC recognises.
     */
    public static enum IndexType {
        /**
         * Table statistics that are returned in conjuction with a table's index descriptions.
         */
        STATISTIC(DatabaseMetaData.tableIndexStatistic),
        
        /**
         * A clustered index.
         */
        CLUSTERED(DatabaseMetaData.tableIndexClustered),
        
        /**
         * A hashed index.
         */
        HASHED(DatabaseMetaData.tableIndexHashed),
        
        /**
         * An index that is not clustered or hashed, or table statisics.
         */
        OTHER(DatabaseMetaData.tableIndexOther);
        
        private short jdbcType;
        
        IndexType(short jdbcType) {
            this.jdbcType = jdbcType;
        }
     
        /**
         * Returns this index type's JDBC code (one of
         * DatabaseMetaData.tableIndexStatistic,
         * DatabaseMetaData.tableIndexClustered,
         * DatabaseMetaData.tableIndexHashed, or
         * DatabaseMetaData.tableIndexOther).
         */
        public short getJdbcType() {
            return jdbcType;
        }
        
        public static IndexType forJdbcType(short jdbcType) {
            if (jdbcType == DatabaseMetaData.tableIndexStatistic) return STATISTIC;
            if (jdbcType == DatabaseMetaData.tableIndexClustered) return CLUSTERED;
            if (jdbcType == DatabaseMetaData.tableIndexHashed) return HASHED;
            if (jdbcType == DatabaseMetaData.tableIndexOther) return OTHER;
            throw new IllegalArgumentException("Unknown JDBC index type code: " + jdbcType);
        }
    }
    
    /**
     * A simple placeholder for a column.  We're not using real SQLColumn instances here so that the
     * tree of SQLObjects can remain tree-like.  If we put the real SQLColumns in here, the columns
     * would appear in two places in the tree (here and under the table's columns folder)!
     */
    public class Column extends SQLObject {

        /**
         * The column in the table that this index column represents.  Might be null if this index column
         * represents an expression rather than a single column value.
         */
        private SQLColumn column;
        
        /**
         * Indicates if this index applies to ascending values.
         */
        private boolean ascending;

        /**
         * Indicates if this index applies to descending values.
         */
        private boolean descending;
        
        /**
         * Creates a Column object that corresponds to a particular SQLColumn.
         */
        public Column(SQLColumn col, boolean ascending, boolean descending) {
            this(col.getName(), ascending, descending);
            this.column = col;
        }

        /**
         * Creates a Column object that does not correspond to a particular column
         * (such as an expression index).
         */
        public Column(String name, boolean ascending, boolean descending) {
            setName(name);
            this.ascending = ascending;
            this.descending = descending;
        }
        
        @Override
        public boolean allowsChildren() {
            return false;
        }

        @Override
        public Class<? extends SQLObject> getChildType() {
            return null;
        }

        @Override
        public SQLObject getParent() {
            return SQLIndex.this;
        }

        @Override
        public String getShortDisplayName() {
            return getName();
        }

        @Override
        protected void populate() throws ArchitectException {
            // nothing to do
        }

        @Override
        protected void setParent(SQLObject parent) {
            if (parent != SQLIndex.this) {
                throw new UnsupportedOperationException("You can't change an Index.Column's parent");
            }
        }
        
        public SQLColumn getColumn() {
            return column;
        }

        public void setColumn(SQLColumn column) {
            this.column = column;
        }

        public boolean isAscending() {
            return ascending;
        }

        public void setAscending(boolean ascending) {
            this.ascending = ascending;
        }

        public boolean isDescending() {
            return descending;
        }

        public void setDescending(boolean descending) {
            this.descending = descending;
        }
        
        @Override
        public String toString() {
            return getName();
        }
    }
    
    /**
     * The parent folder that owns this index object.
     */
    private SQLTable.Folder<SQLIndex> parent;
    
    /**
     * Flags whether or not this index enforces uniqueness.
     */
    private boolean unique;

    /**
     * This index's qualifier.
     */
    private String qualifier;
    
    /**
     * The type of this index.
     */
    private IndexType type;
    
    /**
     * The filter condition on this index, if any.  According to the ODBC programmer's reference,
     * this is probably a property of the index as a whole (as opposed to the individual index columns),
     * but it doesn't say that explicitly.  According to the JDBC spec, this could be anything at all.
     */
    private String filterCondition;
    
    public SQLIndex(String name, boolean unique, String qualifier, IndexType type, String filter) {
        setName(name);
        children = new ArrayList();
        this.unique = unique;
        this.qualifier = qualifier;
        this.type = type;
        this.filterCondition = filter;
    }

    /**
     * Indices are associated with one or more table columns.  The children of this index represent those columns,
     * and the order in which the index applies to them.
     */
    @Override
    public boolean allowsChildren() {
        return true;
    }

    @Override
    public Class<? extends SQLObject> getChildType() {
        return Column.class;
    }

    /**
     * Returns the table folder that owns this index.
     */
    @Override
    public SQLObject getParent() {
        return parent;
    }

    @Override
    public String getShortDisplayName() {
        return getName();
    }

    /**
     * Indices are populated when first created, so populate is a no-op.
     */
    @Override
    protected void populate() throws ArchitectException {
        // nothing to do
    }

    /**
     * Updates this index's parent reference.
     * 
     *  @param parent must be a SQLTable.Folder instance.
     */
    @Override
    protected void setParent(SQLObject parent) {
        this.parent = (Folder<SQLIndex>) parent;
    }
    
    public String getFilterCondition() {
        return filterCondition;
    }

    public void setFilterCondition(String filterCondition) {
        this.filterCondition = filterCondition;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public IndexType getType() {
        return type;
    }

    public void setType(IndexType type) {
        this.type = type;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public void setParent(SQLTable.Folder<SQLIndex> parent) {
        this.parent = parent;
    }

    /**
     * Mainly for use by SQLTable's populate method.  Does not cause
     * SQLObjectEvents to avoid infinite recursion, so you have to
     * generate them yourself at a safe time.
     */
    static void addIndicesToTable(SQLTable addTo,
                                  String catalog,
                                  String schema,
                                  String tableName) 
        throws SQLException, DuplicateColumnException, ArchitectException {
        Connection con = null;
        ResultSet rs = null;
        try {
            con = addTo.getParentDatabase().getConnection();
            DatabaseMetaData dbmd = con.getMetaData();
            logger.debug("SQLIndex.addIndicesToTable: catalog="+catalog+"; schema="+schema+"; tableName="+tableName);
            SQLIndex idx = null;
            rs = dbmd.getIndexInfo(catalog, schema, tableName, false, true);
            while (rs.next()) {
                /*
                 * DatabaseMetadata result set columns:
                 * 
                1  TABLE_CAT String => table catalog (may be null)
                2  TABLE_SCHEM String => table schema (may be null)
                3  TABLE_NAME String => table name
                4  NON_UNIQUE boolean => Can index values be non-unique. false when TYPE is tableIndexStatistic
                5  INDEX_QUALIFIER String => index catalog (may be null); null when TYPE is tableIndexStatistic
                6  INDEX_NAME String => index name; null when TYPE is tableIndexStatistic
                7  TYPE short => index type:
                      tableIndexStatistic - this identifies table statistics that are returned in conjuction with a table's index descriptions
                      tableIndexClustered - this is a clustered index
                      tableIndexHashed - this is a hashed index
                      tableIndexOther - this is some other style of index
                8  ORDINAL_POSITION short => column sequence number within index; zero when TYPE is tableIndexStatistic
                9  COLUMN_NAME String => column name; null when TYPE is tableIndexStatistic
                10 ASC_OR_DESC String => column sort sequence, "A" => ascending, "D" => descending, may be null if sort sequence is not supported; null when TYPE is tableIndexStatistic
                11 CARDINALITY int => When TYPE is tableIndexStatistic, then this is the number of rows in the table; otherwise, it is the number of unique values in the index.
                12 PAGES int => When TYPE is tableIndexStatisic then this is the number of pages used for the table, otherwise it is the number of pages used for the current index.
                13 FILTER_CONDITION String => Filter condition, if any. (may be null)
                 */
                boolean nonUnique = rs.getBoolean(4);
                String qualifier = rs.getString(5);
                String name = rs.getString(6);
                IndexType type = IndexType.forJdbcType(rs.getShort(7));
                int pos = rs.getInt(8);
                String colName = rs.getString(9);
                String ascDesc = rs.getString(10);
                boolean ascending = (ascDesc != null && ascDesc.equals("A"));
                boolean descending = (ascDesc != null && ascDesc.equals("D"));
                String filter = rs.getString(13);
                
                if (pos == 0) {
                    // this is just the table stats, not an index
                    continue;
                } else if (pos == 1) {
                    logger.debug("Found index "+name);
                    idx = new SQLIndex(name, !nonUnique, qualifier, type, filter);
                    addTo.getIndicesFolder().children.add(idx);
                }
                
                logger.debug("Adding column "+colName+" to index "+idx.getName());
                
                Column col;
                if (addTo.getColumnByName(colName, false) != null) {
                    col = idx.new Column(addTo.getColumnByName(colName, false), ascending, descending);
                } else {
                    col = idx.new Column(colName, ascending, descending);  // probably an expression like "col1+col2"
                }
                
                idx.children.add(col); // direct access avoids possible recursive SQLObjectEvents
            }
            rs.close();
            rs = null;

            rs = dbmd.getPrimaryKeys(catalog, schema, tableName);
            while (rs.next()) {
                SQLColumn col = addTo.getColumnByName(rs.getString(4), false);
                //logger.debug(rs.getString(4));
                if (col != null ){
                    col.primaryKeySeq = new Integer(rs.getInt(5));
                    addTo.setPrimaryKeyName(rs.getString(6));
                } else {
                    SQLException exception = new SQLException("Column "+rs.getString(4)+ " not found in "+addTo);
                    throw exception;
                }
            }
            rs.close();
            rs = null;
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException ex) {
                logger.error("Couldn't close result set", ex);
            }
            try {
                if (con != null) con.close();
            } catch (SQLException ex) {
                logger.error("Couldn't close connection", ex);
            }
        }
    }

    @Override
    public String toString() {
        return getName();
    }


}
