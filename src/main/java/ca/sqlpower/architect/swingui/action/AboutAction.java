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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JPanel;

import ca.sqlpower.architect.ArchitectVersion;
import ca.sqlpower.architect.swingui.ArchitectFrame;
import ca.sqlpower.swingui.AboutPanel;
import ca.sqlpower.swingui.CommonCloseAction;
import ca.sqlpower.swingui.JDefaultButton;
import ca.sqlpower.swingui.SPSUtils;

public class AboutAction extends AbstractArchitectAction {

    public AboutAction(ArchitectFrame frame) {
		super(frame, Messages.getString("AboutAction.name"), Messages.getString("AboutAction.description"), "Architect"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void actionPerformed(ActionEvent evt) {
		// This is one of the few JDIalogs that can not get replaced
		// with a call to ArchitectPanelBuilder, because an About
		// box must have only ONE button...
		final JDialog d = new JDialog(frame,
									  Messages.getString("AboutAction.dialogTitle")); //$NON-NLS-1$
		JPanel cp = new JPanel(new BorderLayout(12,12));
		cp.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
		
        ImageIcon imageIcon = SPSUtils.createIcon("architect128", "Architect Logo");
		
		final AboutPanel aboutPanel = new AboutPanel(imageIcon, "SQL Power Architect", "ca/sqlpower/architect/architect.version.properties", ArchitectVersion.APP_FULL_VERSION.toString());
		cp.add(aboutPanel, BorderLayout.CENTER);
			
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			
		Action okAction = new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
					aboutPanel.applyChanges();
					d.setVisible(false);
			}
		};
		okAction.putValue(Action.NAME, Messages.getString("AboutAction.okOption")); //$NON-NLS-1$
		JDefaultButton okButton = new JDefaultButton(okAction);
		buttonPanel.add(okButton);
			
		cp.add(buttonPanel, BorderLayout.SOUTH);
		SPSUtils.makeJDialogCancellable(
				d, new CommonCloseAction(d));
		d.getRootPane().setDefaultButton(okButton);
		d.setContentPane(cp);
		d.pack();
		d.setLocationRelativeTo(frame);
		d.setVisible(true);
		
	}
}
