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

import java.awt.Color;
import java.awt.Window;
import java.awt.event.MouseEvent;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import ca.sqlpower.architect.layout.LayoutEdge;
import ca.sqlpower.architect.layout.LayoutNode;
import ca.sqlpower.architect.olap.OLAPObject;
import ca.sqlpower.architect.olap.MondrianModel.DimensionUsage;
import ca.sqlpower.architect.swingui.ASUtils;
import ca.sqlpower.architect.swingui.PlayPen;
import ca.sqlpower.architect.swingui.PlayPenComponent;
import ca.sqlpower.architect.swingui.PlayPenContentPane;
import ca.sqlpower.architect.swingui.event.PlayPenContentEvent;
import ca.sqlpower.architect.swingui.event.PlayPenContentListener;
import ca.sqlpower.architect.swingui.event.SelectionEvent;
import ca.sqlpower.swingui.DataEntryPanel;
import ca.sqlpower.swingui.DataEntryPanelBuilder;

/**
 * A component that visually indicates a usage of one olap pane by another. For example,
 * a dimension usage or a cube usage.
 */
public class UsageComponent extends PlayPenComponent implements LayoutEdge {

    private final OLAPObject model;
    private final OLAPPane<?, ?> pane1;
    private final OLAPPane<?, ?> pane2;
    
    private final OLAPPanesWatcher olapPanesWatcher = new OLAPPanesWatcher();

    /**
     * Creates a copy of the given UsageComponent.
     * 
     * <p>BUG: The new copy will not react properly to its associated panes
     * moving around. It should, however, be sufficient for printing or PDF
     * generation.
     * 
     * @param copyMe The UsageComponent to copy.
     */
    public UsageComponent(UsageComponent copyMe, PlayPenContentPane parent) {
        super(copyMe, parent);
        model = copyMe.model;
        // TODO refactor listener strategy so listeners could be set up here
        pane1 = copyMe.pane1;
        pane2 = copyMe.pane2;
        updateUI();
    }
    
    public UsageComponent(PlayPenContentPane parent, OLAPObject model, OLAPPane<?, ?> pane1, OLAPPane<?, ?> pane2) {
        super(parent);
        this.model = model;
        this.pane1 = pane1;
        this.pane2 = pane2;
        setOpaque(false);
        setForegroundColor(Color.BLACK);
        updateUI();
        parent.addPlayPenContentListener(olapPanesWatcher);
    }
    
    /**
     * Creates and installs the UI delegate. This generic UsageComponent creates
     * a UsageComponentUI, which should be sufficient for most purposes. If your
     * subclass needs a different UI, override this method and install a different
     * one.
     * <p>
     * Nasty warning: If you override this method, be aware that it's going to get
     * called before your constructor has been invoked. How weird is that?
     */
    protected void updateUI() {
        UsageComponentUI ui = new UsageComponentUI();
        ui.installUI(this);
        setUI(ui);
    }

    @Override
    public OLAPObject getModel() {
        return model;
    }

    @Override
    public String getName() {
        return model.getName();
    }

    @Override
    public void handleMouseEvent(MouseEvent evt) {
        PlayPen pp = getPlayPen();
        if (evt.getID() == MouseEvent.MOUSE_PRESSED) {
            if (!isSelected()) {
                pp.selectNone();
                setSelected(true, SelectionEvent.SINGLE_SELECT);
            }
        } else if (evt.getID() == MouseEvent.MOUSE_MOVED || evt.getID() == MouseEvent.MOUSE_DRAGGED) {
            setSelected(getUI().intersects(pp.getRubberBand()), SelectionEvent.SINGLE_SELECT);
        } else if (evt.getID() == MouseEvent.MOUSE_CLICKED) {
            if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
                if (model instanceof DimensionUsage) {
                    try {
                        DimensionUsage du = (DimensionUsage) model;
                        DataEntryPanel panel = new DimensionUsageEditPanel(du);
                        if (panel != null) {
                            Window owner = SwingUtilities.getWindowAncestor(getPlayPen());
                            JDialog dialog = DataEntryPanelBuilder.createDataEntryPanelDialog(panel, owner,
                                    "Modify Properties", "OK");
                            dialog.setLocationRelativeTo(owner);
                            dialog.setVisible(true);
                        }
                    } catch (Exception e) {
                        ASUtils.showExceptionDialogNoReport(SwingUtilities.getWindowAncestor(getPlayPen()),
                                "Failed to create edit dialog!", e);
                    }
                }
            }
        }
    }

    @Override
    public UsageComponentUI getUI() {
        return (UsageComponentUI) super.getUI();
    }
    
    /**
     * Returns one of the panes that this usage component connects together.
     */
    public OLAPPane<?, ?> getPane1() {
        return pane1;
    }
    
    /**
     * Returns one of the panes that this usage component connects together.
     */
    public OLAPPane<?, ?> getPane2() {
        return pane2;
    }

    /**
     * Watches the panes that the usage is connected to, if either are removed,
     * then the component will remove itself.
     */
    private class OLAPPanesWatcher implements PlayPenContentListener {

        public void PlayPenComponentAdded(PlayPenContentEvent e) {
            // do nothing.
        }

        public void PlayPenComponentRemoved(PlayPenContentEvent e) {
            if (e.getPlayPenComponent() == pane1 || e.getPlayPenComponent() == pane2) {
                getPlayPen().getContentPane().remove(UsageComponent.this);
            }
        }
    }

    public LayoutNode getHeadNode() {
        return pane2;
    }

    public LayoutNode getTailNode() {
        return pane1;
    }
}
