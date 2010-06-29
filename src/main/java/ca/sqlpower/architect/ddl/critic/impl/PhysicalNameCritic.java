/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

package ca.sqlpower.architect.ddl.critic.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import ca.sqlpower.architect.ArchitectProject;
import ca.sqlpower.architect.ddl.critic.Critic;
import ca.sqlpower.architect.ddl.critic.CriticAndSettings;
import ca.sqlpower.architect.ddl.critic.Criticism;
import ca.sqlpower.architect.ddl.critic.QuickFix;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLIndex.Column;
import ca.sqlpower.sqlobject.SQLRelationship.ColumnMapping;
import ca.sqlpower.util.UserPrompter;
import ca.sqlpower.util.UserPrompter.UserPromptOptions;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;
import ca.sqlpower.util.UserPrompterFactory.UserPromptType;

/**
 * Criticizes the physical name of all SQLObjects based on the parameters given
 * to the constructor.
 */
public class PhysicalNameCritic extends CriticAndSettings {

    private final Pattern legalNamePattern;
    private final int maxNameLength;
    private final String platformName;

    /**
     * If the {@link SQLObject}s criticized do not match the pattern or is
     * longer than the given max length criticisms will be created to warn the
     * user.
     * 
     * @param platformName
     *            The name of the platform we are criticizing. See
     *            {@link Critic.StarterPlatformTypes} for examples.
     * @param legalNamePattern
     *            A {@link Pattern} that the names must match to prevent errors.
     * @param maxNameLength
     *            Names longer than this value in character count will be marked
     *            as an error.
     */
    @Constructor
    public PhysicalNameCritic(
            @ConstructorParameter(propertyName="platformName") String platformName, 
            @ConstructorParameter(propertyName="legalNamePattern") Pattern legalNamePattern, 
            @ConstructorParameter(propertyName="maxNameLength") int maxNameLength) {
        super(platformName, "Physical name restrictions");
        this.platformName = platformName;
        this.legalNamePattern = legalNamePattern;
        this.maxNameLength = maxNameLength;
        
    }
    
    public List<Criticism> criticize(final Object subject) {
        if (!(subject instanceof SQLObject)) return Collections.emptyList();
        
        //Column mappings and the columns in SQLIndex do not get written in
        //a DDL statement so we can ignore criticizing them.
        if (subject instanceof ColumnMapping || subject instanceof Column) return Collections.emptyList();
        
        final SQLObject so = (SQLObject) subject;
        final String physName = so.getPhysicalName();

        List<Criticism> criticisms = new ArrayList<Criticism>();
        
        if (physName == null || physName.trim().length() == 0){
            criticisms.add(new Criticism(
                    so,
                    "No physical name for " + so.getName(),
                    this,
                    new QuickFix("Copy logical name to physical name") {
                        public void apply() {
                            so.setPhysicalName(so.getName());
                        }
                    }));
			return criticisms;
		}

        if (physName.length() > getMaxNameLength()) {
            criticisms.add(new Criticism(
                    so,
                    "Physical name too long for " + getPlatformName(),
                    this,
                    new QuickFix("Truncate name to " + so.getPhysicalName().substring(0, getMaxNameLength())) {
                        public void apply() {
                            if (so.getPhysicalName() != null && so.getPhysicalName().length() > getMaxNameLength()) {
                                so.setPhysicalName(so.getPhysicalName().substring(0, getMaxNameLength()));
                            }
                        }
                    }));
        }
		
        if (!getLegalNamePattern().matcher(physName).matches()) {
            criticisms.add(new Criticism(
                    so,
                    "Physical name not legal for " + so.getPhysicalName(),
                    this,
                    new QuickFix("Enter a new physical name...") {
                        @Override
                        public void apply() {
                            ArchitectProject project = getParent().getParent().getParent();
                            UserPrompter prompter = project.getSession().createUserPrompter("Enter a legal physical name", 
                                    UserPromptType.TEXT, UserPromptOptions.OK_CANCEL, UserPromptResponse.CANCEL, 
                                    physName, "OK", "Cancel");
                            if (UserPromptResponse.OK.equals(prompter.promptUser())) {
                                so.setPhysicalName((String) prompter.getUserSelectedResponse());
                            }
                        }
                    }
                    ));
        }
        
        return criticisms;
    }

    @Accessor
    public Pattern getLegalNamePattern() {
        return legalNamePattern;
    }

    @Accessor
    public int getMaxNameLength() {
        return maxNameLength;
    }

    @Accessor
    public String getPlatformName() {
        return platformName;
    }
    
}
