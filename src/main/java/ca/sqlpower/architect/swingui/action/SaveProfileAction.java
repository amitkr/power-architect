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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import ca.sqlpower.architect.profile.ColumnProfileResult;
import ca.sqlpower.architect.profile.ProfileResult;
import ca.sqlpower.architect.profile.TableProfileResult;
import ca.sqlpower.architect.profile.output.ProfileCSVFormat;
import ca.sqlpower.architect.profile.output.ProfileFormat;
import ca.sqlpower.architect.profile.output.ProfileHTMLFormat;
import ca.sqlpower.architect.profile.output.ProfilePDFFormat;
import ca.sqlpower.architect.swingui.ASUtils;
import ca.sqlpower.architect.swingui.table.ProfileJTable;
import ca.sqlpower.swingui.SPSUtils;

public class SaveProfileAction extends AbstractAction {

    public class ProfileResultsTree {
        Map<TableProfileResult, Set<ColumnProfileResult>>  resultTree = new TreeMap<TableProfileResult, Set<ColumnProfileResult>>();
        
        public void addTableProfileResult(TableProfileResult tpr){
            if (!resultTree.containsKey(tpr)) {
                resultTree.put(tpr, new HashSet<ColumnProfileResult>());
            }
        }
        
        public void addColumnProfileResult(ColumnProfileResult cpr){
            TableProfileResult tpr = cpr.getParentResult();
            if (!resultTree.containsKey(tpr)) {
                resultTree.put(tpr, new TreeSet<ColumnProfileResult>());
            }
            ((Set<ColumnProfileResult>)resultTree.get(tpr)).add(cpr);
        }
        
        public List<ProfileResult> getDepthFirstList() {
            List<ProfileResult> depthFirstList = new ArrayList<ProfileResult>();
            for (TableProfileResult tpr : resultTree.keySet()) {
                depthFirstList.add(tpr);
                for (ColumnProfileResult cpr : ((Set<ColumnProfileResult>)resultTree.get(tpr))) {
                    depthFirstList.add(cpr);
                }
            }
            return depthFirstList;
        }
    }
    
    /** The set of valid file types for saving the report in */
    public enum SaveableFileType { 
        HTML(SPSUtils.HTML_FILE_FILTER), 
        PDF(SPSUtils.PDF_FILE_FILTER), 
        CSV(SPSUtils.CSV_FILE_FILTER);
        
        /**
         * A file filter that matches the type in the enum.
         */
        private final FileFilter filter;

        private SaveableFileType(FileFilter filter) {
            this.filter = filter;
        }

        public FileFilter getFilter() {
            return filter;
        }
    }

    /**
     * The component whose window ancestor will own dialogs created by this action.
     */
    private Component dialogOwner;

    private ProfileJTable viewTable;

    /**
     * These file types are the available types for exporting to for this
     * action. The file types will be available to be selected in the file
     * chooser dialog.
     */
    private final SaveableFileType[] fileTypes; 

    /**
     * Creates a new action which will, when invoked, offer to save profile results
     * in one of several deluxe file formats.
     * 
     * @param dialogOwner The component whose window ancestor will own dialogs created by this action.
     * @param viewTable The (eww) jtable which contains the profile results to be exported.
     * XXX this should be a collection of TableProfileResult objects, not a view component that houses them
     */
    public SaveProfileAction(Component dialogOwner, String name, ProfileJTable viewTable, SaveableFileType ... fileTypes) {
        super(name); //$NON-NLS-1$
        this.dialogOwner = dialogOwner;
        this.viewTable = viewTable;
        this.fileTypes = fileTypes;
    }


    public void actionPerformed(ActionEvent e) {

        final ProfileResultsTree objectToSave = new ProfileResultsTree();

        if ( viewTable.getSelectedRowCount() > 1 ) {
            int selectedRows[] = viewTable.getSelectedRows();
            Set<TableProfileResult> selectedTable = new HashSet<TableProfileResult>();
            HashSet<ColumnProfileResult> selectedColumn = new HashSet<ColumnProfileResult>();
            for ( int i=0; i<selectedRows.length; i++ ) {
                int rowid = selectedRows[i];
                ColumnProfileResult result = viewTable.getColumnProfileResultForRow(rowid);
                selectedTable.add(result.getParentResult());
                selectedColumn.add(result);
            }

            boolean fullSelection = true;
            for (TableProfileResult tpr : selectedTable) {
                for (ColumnProfileResult cpr : tpr.getColumnProfileResults()) {
                    if ( !selectedColumn.contains(cpr) ) {
                        fullSelection = false;
                        break;
                    }
                }
            }

            int response = 0;
            if ( !fullSelection ) {
                response = JOptionPane.showOptionDialog(
                        dialogOwner,
                        Messages.getString("SaveProfileAction.saveOnlySelectedPortion"), //$NON-NLS-1$
                        Messages.getString("SaveProfileAction.saveOnlySelectedPortionDialogTitle"), //$NON-NLS-1$
                        0,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[] {Messages.getString("SaveProfileAction.savePartialOption"),Messages.getString("SaveProfileAction.saveEntireTableOption")}, //$NON-NLS-1$ //$NON-NLS-2$
                        Messages.getString("SaveProfileAction.saveEntireTableOption")); //$NON-NLS-1$
            }

            if (response == 1) { // entire table

                for (TableProfileResult tpr : selectedTable) {
                    for (ColumnProfileResult cpr : tpr.getColumnProfileResults()) {
                        objectToSave.addColumnProfileResult(cpr);
                    }
                }
            } else { // partial table
                for ( int i=0; i<selectedRows.length; i++ ) {
                    int rowid = selectedRows[i];
                    ColumnProfileResult result = viewTable.getColumnProfileResultForRow(rowid);
                    objectToSave.addColumnProfileResult(result);
                }
            }
        } else {
            for (int i = 0; i < viewTable.getRowCount(); i++) {
                ColumnProfileResult result = viewTable.getColumnProfileResultForRow(i);
                objectToSave.addColumnProfileResult(result);
            }
        }


        JFileChooser chooser = new JFileChooser();

        chooser.removeChoosableFileFilter(chooser.getAcceptAllFileFilter());
        for (SaveableFileType type : fileTypes) {
            chooser.addChoosableFileFilter(type.getFilter());
        }

        File file = null;
        SaveableFileType type;
        while ( true ) {
            // Ask the user to pick a file
            int response = chooser.showSaveDialog(dialogOwner);

            if (response != JFileChooser.APPROVE_OPTION) {
                return;
            }
            file = chooser.getSelectedFile();
            final FileFilter fileFilter = chooser.getFileFilter();
            String fileName = file.getName();
            int x = fileName.lastIndexOf('.');
            boolean gotType = false;
            SaveableFileType ntype = null;
            if (x != -1) {
                // pick file by filename the user typed
                String ext = fileName.substring(x+1);
                try {
                    ntype = SaveableFileType.valueOf(ext.toUpperCase());
                    gotType = true;
                } catch (IllegalArgumentException iex) {
                    gotType = false;
                }
            }

            if (gotType) {
                type = ntype;
            } else {
                // force filename to end with correct extention
                if (fileFilter == SPSUtils.HTML_FILE_FILTER) {
                    if (!fileName.endsWith(".html")) { //$NON-NLS-1$
                        file = new File(file.getPath()+".html"); //$NON-NLS-1$
                    }
                    type = SaveableFileType.HTML;
                } else if (fileFilter == SPSUtils.PDF_FILE_FILTER){
                    if (!fileName.endsWith(".pdf")) { //$NON-NLS-1$
                        file = new File(file.getPath()+".pdf"); //$NON-NLS-1$
                    }
                    type = SaveableFileType.PDF;
                } else if (fileFilter == SPSUtils.CSV_FILE_FILTER){
                    if (!fileName.endsWith(".csv")) { //$NON-NLS-1$
                        file = new File(file.getPath()+".csv"); //$NON-NLS-1$
                    }
                    type = SaveableFileType.CSV;
                } else {
                    throw new IllegalStateException(Messages.getString("SaveProfileAction.unexpectedFileFilter")); //$NON-NLS-1$
                }
            }
            if (file.exists()) {
                response = JOptionPane.showConfirmDialog(
                        dialogOwner,
                        Messages.getString("SaveProfileAction.fileAlreadyExists", file.getPath()), //$NON-NLS-1$ //$NON-NLS-2$
                        Messages.getString("SaveProfileAction.fileAlreadyExistsDialogTitle"), JOptionPane.YES_NO_OPTION); //$NON-NLS-1$
                if (response != JOptionPane.NO_OPTION) {
                    break;
                }
            } else {
                break;
            }
        }

        // Clone file object for use in inner class, can not make "file" final as we change it to add extension
        final File file2 = new File(file.getPath());
        final SaveableFileType type2 = type;
        Runnable saveTask = new Runnable() {
            public void run() {

                OutputStream out = null;
                try {
                    ProfileFormat prf = null;
                    out = new BufferedOutputStream(new FileOutputStream(file2));
                    switch(type2) {
                    case HTML:
                        final String encoding = "utf-8"; //$NON-NLS-1$
                        prf = new ProfileHTMLFormat(encoding);
                        break;
                    case PDF:
                        prf = new ProfilePDFFormat();
                        break;
                    case CSV:
                        prf = new ProfileCSVFormat();
                        break;
                    default:
                        throw new IllegalArgumentException(Messages.getString("SaveProfileAction.unknownType")); //$NON-NLS-1$
                    }
                    prf.format(out, objectToSave.getDepthFirstList());
                } catch (Exception ex) {
                    //FIXME: This should generate and send an error report
                    ASUtils.showExceptionDialogNoReport(dialogOwner,
                        Messages.getString("SaveProfileAction.couldNotSaveReport"), ex); //$NON-NLS-1$
                } finally {
                    if ( out != null ) {
                        try {
                            out.flush();
                            out.close();
                        } catch (IOException ex) {
                            //FIXME: This should generate and send an error report
                            ASUtils.showExceptionDialogNoReport(dialogOwner,
                                Messages.getString("SaveProfileAction.couldNotCloseReport"), ex); //$NON-NLS-1$
                        }
                    }
                }
            }
        };
        new Thread(saveTask).start();

    }
}
