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

package ca.sqlpower.architect.swingui.olap;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.olap.OLAPRootObject;
import ca.sqlpower.architect.olap.OLAPSession;
import ca.sqlpower.architect.olap.MondrianModel.Schema;
import ca.sqlpower.architect.swingui.ArchitectSwingSession;
import ca.sqlpower.architect.swingui.PlayPenContentPane;
import ca.sqlpower.architect.swingui.olap.action.ExportSchemaAction;
import ca.sqlpower.architect.swingui.olap.action.ImportSchemaAction;
import ca.sqlpower.architect.swingui.olap.action.OLAPEditAction;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.swingui.SPSUtils;
import ca.sqlpower.util.TransactionEvent;

import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.debug.FormDebugPanel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class OLAPSchemaManager {
    private static Logger logger = Logger.getLogger(OLAPSchemaManager.class);

    /**
     * The GUI panel.  Lives inside the dialog {@link #d}.
     */
    private JPanel panel;

    /**
     * The Dialog that contains all the GUI;
     */
    private JDialog d;

    /**
     * The current owner of the dialog.  Gets updated in the showDialog() method.
     */
    private Window currentOwner;
    
    private ArchitectSwingSession session;

    private final Action editOLAPSchemaAction = new AbstractAction("Edit...") { 
        public void actionPerformed(ActionEvent e) {
            OLAPSession oSession = getSelectedOSession();
            if (oSession != null) {
                new OLAPEditAction(session, oSession).actionPerformed(e);
            }
        }
    };

    private final Action removeOLAPSchemaAction = new AbstractAction("Remove") { 
        public void actionPerformed(ActionEvent e) {
            OLAPSession oSession = getSelectedOSession();
            if (oSession != null) {
                Schema schema = oSession.getSchema();
                int option = JOptionPane.showConfirmDialog(
                        d,
                        "Do you want to delete this OLAP Schema? " + schema.getName(), //$NON-NLS-1$
                        "Remove", 
                        JOptionPane.YES_NO_OPTION);
                if (option != JOptionPane.YES_OPTION) {
                    return;
                }
                session.getOLAPRootObject().removeOLAPSession(oSession);
                
                editOLAPSchemaAction.setEnabled(false);
                removeOLAPSchemaAction.setEnabled(false);
            }
        }
    };
    
    private final Action closeAction = new AbstractAction("Close") { //$NON-NLS-1$
        public void actionPerformed(ActionEvent e) {
            d.dispose();
        }
    };

    private final Action exportSchemaAction = new AbstractAction("Export...") {
        public void actionPerformed(ActionEvent e) {
            OLAPSession oSession = getSelectedOSession();
            if (oSession != null) {
                ExportSchemaAction actualExportAction = new ExportSchemaAction(session, oSession.getSchema());
                actualExportAction.actionPerformed(e);
            }
        }
    };
    
    /**
     * The table that contains the list of all schemas in the project.
     */
    private JTable osessionTable;
    
    /**
     * The schema collection of the session this schema
     * manager belongs to.
     */
    private final List<OLAPSession> olapSessions;
    
    private final HashMap<OLAPSession, PlayPenContentPane> olapContentPanes;

    /**
     * Creates a new schema manager with the default set of action buttons.
     */
    public OLAPSchemaManager(ArchitectSwingSession session) {
        this.session = session;
        olapSessions = session.getOLAPRootObject().getChildren();
        olapContentPanes = new HashMap<OLAPSession, PlayPenContentPane>();
        for (PlayPenContentPane olapCP : session.getWorkspace().getOlapContentPanes()) {
            if (olapSessions.contains(olapCP.getModelContainer())) {
                olapContentPanes.put((OLAPSession) olapCP.getModelContainer(), olapCP);
            } else {
                throw new IllegalStateException(
                        "OLAPSessions listed under content panes do not match those under OLAP root");
            }
        }
    }

    /**
     * Makes sure this schema manager is visible,
     * focused, and in a dialog owned by the given owner.
     *
     * @param owner the Frame or Dialog that should own the
     *              OLAPSchemaManager dialog.
     */
    public void showDialog(Window owner) {
        if (d != null && d.isVisible() && currentOwner == owner) {
            d.setVisible(true);  // even if the dialog is already visible, this brings it to the front and gives it focus
            d.requestFocus();    // this will rob focus from the previous focus owner
            return;
        }

        if (d != null) {
            d.dispose();
        }
        
        if (panel == null) {
            panel = createPanel();
        }
        
        if (panel.getParent() != null) {
            panel.getParent().remove(panel);
        }
        if (owner instanceof Dialog) {
            d = new JDialog((Dialog) owner);
        } else if (owner instanceof Frame) {
            d = new JDialog((Frame) owner);
        } else {
            throw new IllegalArgumentException(
                    "Owner has to be a Frame or Dialog.  You provided a " + //$NON-NLS-1$
                    (owner == null ? null : owner.getClass().getName()));
        }

        currentOwner = owner;
        d.setTitle("OLAP Schema Manager"); 
        d.getContentPane().add(panel);
        d.pack();
        d.setLocationRelativeTo(owner);
        SPSUtils.makeJDialogCancellable(d, closeAction);
        d.setVisible(true);
        d.requestFocus();
    }

    /**
     * Closes the current dialog. It is safe to call this even if the dialog is not visible.
     */
    public void closeDialog() {
        if (d != null) {
            d.dispose();
        }
    }
    
    
    private JPanel createPanel() {
        FormLayout layout = new FormLayout(
                "6dlu, fill:min(160dlu;default):grow, 6dlu, pref, 6dlu", // columns //$NON-NLS-1$
                " 6dlu,10dlu,6dlu,fill:min(180dlu;default):grow,10dlu"); // rows //$NON-NLS-1$

        layout.setColumnGroups(new int [][] { {1,3,5}});
        CellConstraints cc = new CellConstraints();

        PanelBuilder pb;
        JPanel p = logger.isDebugEnabled()  ? new FormDebugPanel(layout) : new JPanel(layout);
        pb = new PanelBuilder(layout,p);
        pb.setDefaultDialogBorder();

        pb.add(new JLabel("Available OLAP Schemas"), cc.xy(2, 2)); //$NON-NLS-1$

        TableModel tm = new SchemaTableModel(session.getOLAPRootObject());
        osessionTable = new JTable(tm);
        osessionTable.setTableHeader(null);
        osessionTable.setShowGrid(false);
        osessionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        osessionTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                boolean enableActions = getSelectedOSession() != null;
                removeOLAPSchemaAction.setEnabled(enableActions);
                editOLAPSchemaAction.setEnabled(enableActions);
                exportSchemaAction.setEnabled(enableActions);

                if (evt.getClickCount() == 2) {
                    editOLAPSchemaAction.actionPerformed(null);
                }
            }
        });

        JScrollPane sp = new JScrollPane(osessionTable);

        pb.add(sp, cc.xy(2, 4));

        ButtonStackBuilder bsb = new ButtonStackBuilder();

        JButton newOLAPSchemaButton = new JButton(new OLAPEditAction(session, null));
        newOLAPSchemaButton.setText("New...");
        bsb.addGridded(newOLAPSchemaButton);
        bsb.addRelatedGap();
        
        JButton importOLAPSchemaButton = new JButton(new ImportSchemaAction(session));
        importOLAPSchemaButton.setText("Import...");
        bsb.addGridded(importOLAPSchemaButton);
        bsb.addGridded(new JButton(exportSchemaAction));
        bsb.addRelatedGap();
        
        bsb.addGridded(new JButton(editOLAPSchemaAction));
        bsb.addRelatedGap();
        bsb.addGridded(new JButton(removeOLAPSchemaAction));
        
        removeOLAPSchemaAction.setEnabled(false);
        editOLAPSchemaAction.setEnabled(false);
        exportSchemaAction.setEnabled(false);

        bsb.addUnrelatedGap();
        bsb.addGridded(new JButton(closeAction));

        pb.add(bsb.getPanel(), cc.xy(4,4));
        return pb.getPanel();

    }

    private class SchemaTableModel extends AbstractTableModel implements SPListener {

        public SchemaTableModel(OLAPRootObject rootObj) {
            super();
            rootObj.addSPListener(this);
            for (OLAPSession osession : olapSessions) {
                osession.getSchema().addSPListener(this);
            }
        }

        public int getRowCount() {
            return olapSessions == null? 0 : olapSessions.size();
        }

        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return "Schema";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return Schema.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return olapSessions == null ? null : olapSessions.get(rowIndex).getSchema().getName();
        }
        
        public void childAdded(SPChildEvent e) {
            if (e.getChild() instanceof OLAPSession) {
                fireTableDataChanged();
                ((OLAPSession) e.getChild()).getSchema().addSPListener(this);
            }
        }

        public void childRemoved(SPChildEvent e) {
            if (e.getChild() instanceof OLAPSession) {
                fireTableDataChanged();
                ((OLAPSession) e.getChild()).getSchema().removeSPListener(this);
            }
        }

        public void propertyChanged(PropertyChangeEvent evt) {
            fireTableDataChanged();
        }

        public void transactionEnded(TransactionEvent e) {
            //no-op
        }

        public void transactionRollback(TransactionEvent e) {
            //no-op            
        }

        public void transactionStarted(TransactionEvent e) {
            //no-op            
        }

    }

    /**
     * Returns the first selected Schema object from the list.
     * Returns null if there are not any selected schema
     */
    public OLAPSession getSelectedOSession() {
        int selectedRow = osessionTable.getSelectedRow();
        if (selectedRow == -1) {
            return null;
        }
        return (OLAPSession) olapSessions.get(selectedRow);
    }
    
    public PlayPenContentPane getContentPane(OLAPSession session) {
        return olapContentPanes.get(session);
    }

}
