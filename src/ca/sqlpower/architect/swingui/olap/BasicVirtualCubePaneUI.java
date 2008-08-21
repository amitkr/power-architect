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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.olap.OLAPObject;
import ca.sqlpower.architect.olap.OLAPUtil;
import ca.sqlpower.architect.olap.MondrianModel.CubeUsages;
import ca.sqlpower.architect.olap.MondrianModel.VirtualCube;
import ca.sqlpower.architect.swingui.PlayPenComponent;
import ca.sqlpower.architect.swingui.PlayPenComponentUI;

public class BasicVirtualCubePaneUI extends OLAPPaneUI<VirtualCube, OLAPObject> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(BasicVirtualCubePaneUI.class);

    private final CubeUsageWatcher cubeUsageWatcher = new CubeUsageWatcher();
    
    public static PlayPenComponentUI createUI() {
        return new BasicVirtualCubePaneUI();
    }
    
    @Override
    public void installUI(PlayPenComponent c) {
        super.installUI(c);
        VirtualCubePane vcp = (VirtualCubePane) c;
        OLAPUtil.listenToHierarchy(vcp.getModel().getCubeUsage(), modelEventHandler, modelEventHandler);
        vcp.getModel().addPropertyChangeListener(cubeUsageWatcher);
    }
    
    @Override
    public void uninstallUI(PlayPenComponent c) {
        VirtualCubePane vcp = (VirtualCubePane) c;
        OLAPUtil.unlistenToHierarchy(vcp.getModel().getCubeUsage(), modelEventHandler, modelEventHandler);
        vcp.getModel().removePropertyChangeListener(cubeUsageWatcher);
        super.uninstallUI(c);
    }

    /**
     * CubeUsages are a property of VirtualCube instead of a child. This means
     * OLAPUtil.listenToHierarchy() doesn't pick them up. So we have to listen
     * and unlisten to this tree of OLAPObjects separately. This situation gets
     * set up in {@link BasicVirtualCubePaneUI#installUI()}.
     * <p>
     * But what if someone comes along and calls
     * {@link VirtualCube#setCubeUsage(CubeUsages)}? Well, it's CubeUsageWatcher
     * to the rescue.
     */
    private class CubeUsageWatcher implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() == olapPane.getModel() && "cubeUsage".equals(evt.getPropertyName())) {
                CubeUsages oldUsages = (CubeUsages) evt.getOldValue();
                CubeUsages newUsages = (CubeUsages) evt.getNewValue();
                OLAPUtil.unlistenToHierarchy(oldUsages, modelEventHandler, modelEventHandler);
                OLAPUtil.listenToHierarchy(newUsages, modelEventHandler, modelEventHandler);
            }
        }
        
    }
}
