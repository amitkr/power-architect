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
package ca.sqlpower.architect.swingui.action;

import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.architect.swingui.ArchitectSwingSession;
import ca.sqlpower.architect.swingui.PlayPen;
import ca.sqlpower.architect.swingui.TablePane;

public class InsertColumnAction extends AbstractTableTargetedAction {
	private static final Logger logger = Logger.getLogger(InsertColumnAction.class);
	
	public InsertColumnAction(ArchitectSwingSession session) {
        super(session, Messages.getString("InsertColumnAction.name"), Messages.getString("InsertColumnAction.description"), "new_column"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		putValue(ACTION_COMMAND_KEY, PlayPen.ACTION_COMMAND_SRC_PLAYPEN);
		putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_C,0));
		setEnabled(false);
	}



    void processSQLObject(SQLObject so) throws ArchitectException {
        SQLTable st = null;
        int idx = 0;
        if (so instanceof SQLTable) {
        	logger.debug("user clicked on table, so we shall try to add a column to the end of the table."); //$NON-NLS-1$
        	st = (SQLTable) so;
        	idx = st.getColumnsFolder().getChildCount();
        	logger.debug("SQLTable click -- idx set to: " + idx);						 //$NON-NLS-1$
        } else if (so instanceof SQLColumn) {
        	// iterate through the column list to figure out what position we are in...
        	logger.debug("trying to determine insertion index for table."); //$NON-NLS-1$
        	SQLColumn sc = (SQLColumn) so;
        	st = sc.getParentTable();
        	idx = st.getColumnIndex(sc);
        	if (idx == -1)  {
        		// not found
        		logger.debug("did not find column, inserting at start of table."); //$NON-NLS-1$
        		idx = 0;
        	}
        }
        if (st == null) {
            throw new NullPointerException("The SQLObject must be a instance of SQLTable or SQLColumn");
        } else {
            st.addColumn(idx, new SQLColumn());
        }
        EditColumnAction editColumnAction = new EditColumnAction(session);
        editColumnAction.showDialog(st, idx);
        
    }

    void processTablePane(TablePane tp) throws ArchitectException {
        int idx = tp.getSelectedItemIndex();
        
        if (idx < 0) idx = tp.getModel().getColumnsFolder().getChildCount();
        
        EditColumnAction editColumnAction = new EditColumnAction(session);
        
        //The actual column is added to the table when the user presses OK
        editColumnAction.showDialog(tp.getModel(), idx,true, tp);
    }

    @Override
    public void disableAction() {
        setEnabled(false);
        logger.debug("Disabling Insert Column Action"); //$NON-NLS-1$
        putValue(SHORT_DESCRIPTION, Messages.getString("InsertColumnAction.shortDescription")); //$NON-NLS-1$
    }

}
