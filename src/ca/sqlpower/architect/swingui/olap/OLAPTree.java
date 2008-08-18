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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.olap.OLAPObject;
import ca.sqlpower.architect.olap.MondrianModel.Schema;
import ca.sqlpower.architect.swingui.ArchitectSwingSession;
import ca.sqlpower.swingui.JTreeCollapseAllAction;
import ca.sqlpower.swingui.JTreeExpandAllAction;

public class OLAPTree extends JTree{

    private static final Logger logger = Logger.getLogger(OLAPTree.class);
    
    private final ContextMenuFactory menuFactory;
    
    private final JTreeCollapseAllAction collapseAllAction;
    private final JTreeExpandAllAction expandAllAction;
    
    public OLAPTree(ArchitectSwingSession session, OLAPEditSession oSession, Schema schema) {
        setModel(new OLAPTreeModel(schema));
        addMouseListener(new PopupListener());
        collapseAllAction = new JTreeCollapseAllAction(this, "Collapse All");
        expandAllAction = new JTreeExpandAllAction(this, "Expand All");
        menuFactory = new ContextMenuFactory(session, oSession);
    }
    
 // ----------------- popup menu stuff ----------------

    /**
     * A simple mouse listener that activates the OLAPTree's popup menu
     * when the user right-clicks (or some other platform-specific action).
     *
     * @author The Swing Tutorial (Sun Microsystems, Inc.)
     */
    private class PopupListener extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e,true);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e,false);
        }

        private void maybeShowPopup(MouseEvent e, boolean isPress) {

            TreePath p = getPathForLocation(e.getX(), e.getY());
            if (e.isPopupTrigger()) {
                logger.debug("TreePath is: " + p); //$NON-NLS-1$

                // if the item is already selected, don't touch the selection model
                if (!isPathSelected(p)) {
                    setSelectionPath(p);
                }

                OLAPObject lpc = null;
                if (p != null) {
                    logger.debug("selected node object type is: " + p.getLastPathComponent().getClass().getName()); //$NON-NLS-1$
                    lpc = (OLAPObject) p.getLastPathComponent();
                }

                JPopupMenu popup = menuFactory.createContextMenu(lpc);

                if (lpc != null) {
                    popup.addSeparator();
                    popup.add(collapseAllAction);
                    popup.add(expandAllAction);
                }

                popup.show(e.getComponent(),
                        e.getX(), e.getY());
            } else {
                if ( p == null && !isPress && e.getButton() == MouseEvent.BUTTON1 )
                    setSelectionPath(null);
            }
        }
    }
}
