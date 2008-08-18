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

import org.apache.log4j.Logger;

import ca.sqlpower.architect.olap.OLAPObject;
import ca.sqlpower.architect.olap.MondrianModel.Cube;
import ca.sqlpower.architect.swingui.PlayPenComponent;
import ca.sqlpower.architect.swingui.PlayPenComponentUI;

public class BasicCubePaneUI extends OLAPPaneUI<Cube, OLAPObject> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(BasicCubePaneUI.class);

    public static PlayPenComponentUI createUI() {
        return new BasicCubePaneUI();
    }

    @Override
    public void installUI(PlayPenComponent c) {
        super.installUI((CubePane) c);
        CubePane cube = (CubePane) olapPane; 
        PaneSection<OLAPObject> dimensionSection = new PaneSectionImpl(cube.getModel().getDimensions(), "Dimensions:");
        PaneSection<OLAPObject> measureSection = new PaneSectionImpl(cube.getModel().getMeasures(), "Measures:");
        paneSections.add(dimensionSection);
        paneSections.add(measureSection);
    }
    
    @Override
    public void uninstallUI(PlayPenComponent c) {
        super.uninstallUI(c);
        paneSections.clear();
    }
    
}
