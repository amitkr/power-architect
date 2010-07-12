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

import javax.swing.JDialog;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.swingui.ArchitectFrame;
import ca.sqlpower.architect.swingui.CompareDMDialog;

public class CompareDMAction extends AbstractArchitectAction {
	private static final Logger logger = Logger.getLogger(CompareDMAction.class);
	
	public CompareDMAction(ArchitectFrame frame) {		
		super(frame, Messages.getString("CompareDMAction.name"),Messages.getString("CompareDMAction.description"), "compare_DM"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void actionPerformed(ActionEvent e) {
		
		logger.debug("Compare Action started"); //$NON-NLS-1$
				
		// shows the dialog
		final JDialog d = new CompareDMDialog(getSession());
		d.setVisible(true);
	}

}
