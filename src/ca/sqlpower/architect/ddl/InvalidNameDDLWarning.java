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
package ca.sqlpower.architect.ddl;

import java.util.List;

import ca.sqlpower.sqlobject.SQLObject;

/**
 * A DDLWarning for invalid name that can be fixed by calling setName() on
 * one of the involved objects
 */
public class InvalidNameDDLWarning extends AbstractDDLWarning {

    protected String whatQuickFixShouldCallIt;

    public InvalidNameDDLWarning(String message,
            List<SQLObject> involvedObjects,
            String quickFixMesssage,
            SQLObject whichObjectQuickFixRenames,
            String whatQuickFixShouldCallIt)
    {
        super(involvedObjects,
                message,
                true,
                quickFixMesssage,
                whichObjectQuickFixRenames,
                "name");
        this.whatQuickFixShouldCallIt = whatQuickFixShouldCallIt;
    }

    public boolean quickFix() {
        // XXX need differentiator for setName() vs setPhysicalName()
        whichObjectQuickFixFixes.setName(whatQuickFixShouldCallIt);
        return true;
    }
}
