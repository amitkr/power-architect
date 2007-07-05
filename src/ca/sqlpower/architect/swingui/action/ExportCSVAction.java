/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.sqlpower.architect.swingui.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.ArchitectRuntimeException;
import ca.sqlpower.architect.etl.ExportCSV;
import ca.sqlpower.architect.swingui.PlayPen;

public class ExportCSVAction extends AbstractAction {

    private static final Logger logger = Logger.getLogger(ExportCSVAction.class);
    
    /**
     * The play pen that this action operates on.
     */
    private final PlayPen playpen;
    
    /**
     * The frame that will own the dialog(s) created by this action.
     * Neither argument is allowed to be null.
     */
    private final JFrame parentFrame;
    
    public ExportCSVAction(JFrame parentFrame, PlayPen playpen) {
        super("Export CSV");
        
        if (playpen == null) throw new NullPointerException("Null playpen");
        this.playpen = playpen;

        if (parentFrame == null) throw new NullPointerException("Null parentFrame");
        this.parentFrame = parentFrame;
    }
    
    public void actionPerformed(ActionEvent e) {
        FileWriter output = null;
        try {
            ExportCSV export = new ExportCSV(playpen.getDatabase().getTables());

            File file = null;

            JFileChooser fileDialog = new JFileChooser();
            fileDialog.setSelectedFile(new File("map.csv"));

            if (fileDialog.showSaveDialog(parentFrame) == JFileChooser.APPROVE_OPTION){
                file = fileDialog.getSelectedFile();
            } else {
                return;
            }

            output = new FileWriter(file);
            output.write(export.getCSVMapping());
            output.flush();
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        } catch (ArchitectException e1) {
            throw new ArchitectRuntimeException(e1);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e1) {
                    logger.error("IO Error", e1);
                }
            }
        }
    }
}
