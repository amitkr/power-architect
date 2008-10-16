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

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import ca.sqlpower.architect.ArchitectSession;
import ca.sqlpower.architect.SQLCatalog;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLDatabase;
import ca.sqlpower.architect.SQLIndex;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLRelationship;
import ca.sqlpower.architect.SQLSchema;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.architect.SQLIndex.Column;
import ca.sqlpower.architect.swingui.ArchitectSwingSessionContext;
import ca.sqlpower.architect.swingui.Messages;
import ca.sqlpower.swingui.SPSUtils;

/**
 * The DBTreeCellRenderer renders nodes of a JTree which are of
 * type SQLObject.  This class is much older than November 2006; it
 * was pulled out of the DBTree.java compilation unit into its own
 * file on this date so it could be used more naturally as the cell
 * renderer for a different JTree.
 *
 * @author fuerth
 * @version $Id$
 */
public class DBTreeCellRenderer extends DefaultTreeCellRenderer {
	public static final ImageIcon dbIcon = SPSUtils.createIcon("Database", "SQL Database", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
    public static final ImageIcon dbProfiledIcon = SPSUtils.createIcon("Database_profiled", "SQL Database", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
	public static final ImageIcon targetIcon = SPSUtils.createIcon("Database_target", "SQL Database", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
	public static final ImageIcon cataIcon = SPSUtils.createIcon("Catalog", "SQL Catalog", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
	public static final ImageIcon schemaIcon = SPSUtils.createIcon("Schema", "SQL Schema", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
	public static final ImageIcon tableIcon = SPSUtils.createIcon("Table", "SQL Table", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
    public static final ImageIcon tableProfiledIcon = SPSUtils.createIcon("Table_profiled", "SQL Table", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
	public static final ImageIcon exportedKeyIcon = SPSUtils.createIcon("ExportedKey", "Exported key", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
    public static final ImageIcon importedKeyIcon = SPSUtils.createIcon("ImportedKey", "Imported key", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
	public static final ImageIcon ownerIcon = SPSUtils.createIcon("Owner", "Owner", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
    public static final ImageIcon indexIcon = SPSUtils.createIcon("Index", "Index", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
    public static final ImageIcon pkIndexIcon = SPSUtils.createIcon("Index_key", "Primary Key Index", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
    public static final ImageIcon uniqueIndexIcon = SPSUtils.createIcon("Index_unique", "Unique Index", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
    public static final ImageIcon columnIcon = SPSUtils.createIcon("Column", "Column", ArchitectSwingSessionContext.ICON_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
    private final ArchitectSession session;
   
    
	public DBTreeCellRenderer(ArchitectSession session) {
        super();
        this.session = session;
    }


    public Component getTreeCellRendererComponent(JTree tree,
												  Object value,
												  boolean sel,
												  boolean expanded,
												  boolean leaf,
												  int row,
												  boolean hasFocus) {
		setText(value.toString());
		if (value instanceof SQLDatabase) {
			SQLDatabase db = (SQLDatabase) value;
			if (db.isPlayPenDatabase()) {
				setIcon(targetIcon);
                setText(Messages.getString("DBTreeCellRenderer.playpenNodeText")); //$NON-NLS-1$
			} else {
				setIcon(dbIcon);
			}
		} else if (value instanceof SQLCatalog) {
			if (((SQLCatalog) value).getNativeTerm().equals("owner")) { //$NON-NLS-1$
				setIcon(ownerIcon);
			} else if (((SQLCatalog) value).getNativeTerm().equals("database")) { //$NON-NLS-1$
				setIcon(dbIcon);
			} else if (((SQLCatalog) value).getNativeTerm().equals("schema")) { //$NON-NLS-1$
				setIcon(schemaIcon);
			} else {
				setIcon(cataIcon);
			}
		} else if (value instanceof SQLSchema) {
			if (((SQLSchema) value).getNativeTerm().equals("owner")) { //$NON-NLS-1$
				setIcon(ownerIcon);
			} else {
				setIcon(schemaIcon);
			}
		} else if (value instanceof SQLTable) {
            
			SQLTable table = (SQLTable) value;
            if (session.getProfileManager().getResults(table).size() > 0) {
                setIcon(tableProfiledIcon);
            } else {
                setIcon(tableIcon);
            }
            if ((table).getObjectType() != null) {
			    setText((table).getName()+" ("+(table).getObjectType()+")"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
			    setText((table).getName());
			}
		} else if (value instanceof SQLRelationship) {
            //XXX ARRRRRRGGGGGHHHHHHH!!!! No way of knowing which end of a relationship we're
            // looking at because the relationship has two parents.  Maybe able to do it with the row number.
            if (true) {
                setIcon(exportedKeyIcon);
            } else {
                setIcon(importedKeyIcon);
            }
		} else if (value instanceof SQLIndex) {
            SQLIndex i = (SQLIndex) value;
            if (i.isPrimaryKeyIndex()) {
                setIcon(pkIndexIcon);
            } else if (i.isUnique()) {
                setIcon(uniqueIndexIcon);
            } else {
                setIcon(indexIcon);
            }
        } else if (value instanceof SQLColumn) {
            tagColumn((SQLColumn)value);
            setIcon(columnIcon);
        } else if (value instanceof Column) {
            tagColumn(((Column)value).getColumn());
            setIcon(columnIcon);
        } else {
			setIcon(null);
		}

		this.selected = sel;
		this.hasFocus = hasFocus;

		if (value instanceof SQLObject) {
		    if (((SQLObject) value).isPopulated()) {
		        setForeground(Color.black);
		    } else {
		        setForeground(Color.lightGray);
		    }
		}
	    setToolTipText(getText());
		return this;
	}
    
     /**
     * Determines what tag to append to the given column
     */
    private void tagColumn(SQLColumn col) {
        StringBuffer tag = new StringBuffer();
        StringBuffer fullTag = new StringBuffer();
        boolean isPK = col.isPrimaryKey();
        boolean isFK = col.isForeignKey();
        boolean isAK = col.isUniqueIndexed() && !isPK;
        boolean emptyTag = true;
        if (isPK) {
            tag.append("P"); //$NON-NLS-1$
            emptyTag = false;
        } 
        if (isFK) {
            tag.append("F"); //$NON-NLS-1$
            emptyTag = false;
        }
        if (isAK) {
            tag.append("A"); //$NON-NLS-1$
            emptyTag = false;
        }
        
        if (!emptyTag) {
            tag.append("K"); //$NON-NLS-1$
            fullTag.append("  [ "); //$NON-NLS-1$
            fullTag.append(tag);
            fullTag.append(" ]"); //$NON-NLS-1$
            setText(getText() + fullTag.toString());
        }
    }
}