/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

package ca.sqlpower.architect;

import java.util.List;

import ca.sqlpower.architect.ddl.DDLGenerator;
import ca.sqlpower.architect.olap.OLAPRootObject;
import ca.sqlpower.architect.profile.ProfileManager;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLObjectRoot;
import ca.sqlpower.util.DefaultUserPrompterFactory;
import ca.sqlpower.util.UserPrompter;
import ca.sqlpower.util.UserPrompter.UserPromptOptions;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;

/**
 * Basic implementation of ArchitectSession, for testing
 */
public class TestingArchitectSession implements ArchitectSession {

    private final ArchitectSessionContext context;
    
    private final DefaultUserPrompterFactory defaultUPF = new DefaultUserPrompterFactory();

    /**
     * Creates a new testing session within the given context.
     */
    public TestingArchitectSession(ArchitectSessionContext context) {
        this.context = context;
    }
    
    public ArchitectSessionContext getContext() {
        return context;
    }

    public DDLGenerator getDDLGenerator() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    public ProfileManager getProfileManager() {
        // TODO Auto-generated method stub
        return null;
    }

    public CoreProject getProject() {
        // TODO Auto-generated method stub
        return null;
    }

    public SQLObjectRoot getRootObject() {
        // TODO Auto-generated method stub
        return null;
    }

    public SQLDatabase getTargetDatabase() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setDDLGenerator(DDLGenerator generator) {
        // TODO Auto-generated method stub

    }

    public void setName(String argName) {
        // TODO Auto-generated method stub

    }

    public void setProject(CoreProject project) {
        // TODO Auto-generated method stub

    }

    public void setSourceDatabaseList(List<SQLDatabase> databases) throws SQLObjectException {
        // TODO Auto-generated method stub

    }

    public OLAPRootObject getOLAPRootObject() {
        return null;
    }

    /**
     * Returns a default prompter
     */
    public UserPrompter createUserPrompter(String question, UserPromptType responseType, UserPromptOptions optionType, UserPromptResponse defaultResponseType,
            Object defaultResponse, String ... buttonNames) {
        return defaultUPF.createUserPrompter(question, responseType, optionType,
                defaultResponseType, defaultResponse, buttonNames);
    }

    public SQLDatabase getDatabase(SPDataSource ds) {
        // TODO Auto-generated method stub
        return null;
    }
}
