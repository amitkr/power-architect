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

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.swingui.ArchitectSwingSession;
import ca.sqlpower.architect.swingui.ColumnMappingPanel;
import ca.sqlpower.architect.swingui.DBTree;
import ca.sqlpower.architect.swingui.PlayPen;
import ca.sqlpower.architect.swingui.PlayPenComponent;
import ca.sqlpower.architect.swingui.Relationship;
import ca.sqlpower.architect.swingui.RelationshipEditPanel;
import ca.sqlpower.architect.swingui.Selectable;
import ca.sqlpower.architect.swingui.TabbedDataEntryPanel;
import ca.sqlpower.architect.swingui.event.SelectionEvent;
import ca.sqlpower.architect.swingui.event.SelectionListener;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.swingui.DataEntryPanelBuilder;

public class EditRelationshipAction extends AbstractArchitectAction implements SelectionListener {
	private static final Logger logger = Logger.getLogger(EditRelationshipAction.class);

	/**
	 * The DBTree instance that is associated with this Action.
	 */
	protected final DBTree dbt; 
	
	public EditRelationshipAction(ArchitectSwingSession session) {
		super(session, Messages.getString("EditRelationshipAction.name"), Messages.getString("EditRelationshipAction.description"), "edit_relationship"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		setEnabled(false);
        playpen.addSelectionListener(this);
        setupAction(playpen.getSelectedItems());
        dbt = frame.getDbTree();
	}

	public void actionPerformed(ActionEvent evt) {
		if (evt.getActionCommand().equals(PlayPen.ACTION_COMMAND_SRC_PLAYPEN)) {
			List<PlayPenComponent> selection = playpen.getSelectedItems();
			if (selection.size() < 1) {
				JOptionPane.showMessageDialog(playpen, Messages.getString("EditRelationshipAction.noRelationshipsSelected")); //$NON-NLS-1$
			} else if (selection.size() > 1) {
				JOptionPane.showMessageDialog(playpen, Messages.getString("EditRelationshipAction.multipleItemsSelected")); //$NON-NLS-1$
			} else if (selection.get(0) instanceof Relationship) {
				Relationship r = (Relationship) selection.get(0);
				makeDialog(r.getModel());
			} else {
				JOptionPane.showMessageDialog(playpen, Messages.getString("EditRelationshipAction.pleaseSelectRelationship")); //$NON-NLS-1$
			}
		} else if (evt.getActionCommand().equals(DBTree.ACTION_COMMAND_SRC_DBTREE)) {
			TreePath [] selections = dbt.getSelectionPaths();
			if (selections.length < 1) {
				JOptionPane.showMessageDialog(dbt, Messages.getString("EditRelationshipAction.noRelationshipsSelected")); //$NON-NLS-1$
			} else if (selections.length > 2) {
			    JOptionPane.showMessageDialog(playpen, Messages.getString("EditRelationshipAction.multipleItemsSelected")); //$NON-NLS-1$
			} else {
			    TreePath tp = selections[0];
			    SQLObject so = (SQLObject) tp.getLastPathComponent();
			    
			    // there are two instances of a relationship on the tree
			    if (selections.length == 2) {
			        SQLObject secondObj = (SQLObject) selections[1].getLastPathComponent();
			        if (!so.equals(secondObj)) {
			            JOptionPane.showMessageDialog(dbt, Messages.getString("EditRelationshipAction.pleaseSelectRelationship")); //$NON-NLS-1$
			            return;
			        }
			    }
			    
				if (so instanceof SQLRelationship) {
					SQLRelationship sr = (SQLRelationship) so;
					makeDialog(sr);
				} else {
					JOptionPane.showMessageDialog(dbt, Messages.getString("EditRelationshipAction.pleaseSelectRelationship")); //$NON-NLS-1$
				}
			}
		} else {
			// unrecognized action source, do nothing...
		}							
	}

	private void makeDialog(SQLRelationship sqr) {
		logger.debug("making edit relationship dialog"); //$NON-NLS-1$
		
		Relationship r = session.getPlayPen().getSelectedRelationShips().get(0);
        RelationshipEditPanel editPanel = new RelationshipEditPanel(r);
        ColumnMappingPanel mappingPanel = new ColumnMappingPanel(session, sqr);
        
        TabbedDataEntryPanel panel = new TabbedDataEntryPanel();
        panel.addTab(Messages.getString("EditRelationshipAction.relationshipTab"), editPanel); //$NON-NLS-1$
        panel.addTab(Messages.getString("EditRelationshipAction.mappingsTab"), mappingPanel); //$NON-NLS-1$
        
		final JDialog editDialog = DataEntryPanelBuilder.createDataEntryPanelDialog(
				panel,
				frame, 
				Messages.getString("EditRelationshipAction.dialogTitle"), Messages.getString("EditRelationshipAction.okOption")); //$NON-NLS-1$ //$NON-NLS-2$
				
		editPanel.setEditDialog(editDialog);
		editDialog.pack();
		editDialog.setLocationRelativeTo(frame);
		editDialog.setVisible(true);
	}

	public void setupAction(List<PlayPenComponent> selectedItems) {
		if (selectedItems.size() == 0) {
			setEnabled(false);
			logger.debug("Disabling edit relationship"); //$NON-NLS-1$
			putValue(SHORT_DESCRIPTION, Messages.getString("EditRelationshipAction.shortDescription")); //$NON-NLS-1$
		} else {
			Selectable item = (Selectable) selectedItems.get(0);
			if (item instanceof Relationship )				
				setEnabled(true);
		}
	}
		
	public void itemSelected(SelectionEvent e) {
		setupAction(playpen.getSelectedItems());
		
	}

	public void itemDeselected(SelectionEvent e) {
		setupAction(playpen.getSelectedItems());
	}
	
}
