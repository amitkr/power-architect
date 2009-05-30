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

package ca.sqlpower.architect.ddl.critic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.sqlpower.architect.ArchitectUtils;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLRelationship;
import ca.sqlpower.sqlobject.SQLTable;

public class RelationshipMappingTypeCritic implements Critic<SQLObject> {

    public List<Criticism<SQLObject>> criticize(SQLObject so) {
        if (!(so instanceof SQLRelationship)) return Collections.emptyList();
        SQLRelationship subject = (SQLRelationship) so;
        List<Criticism<SQLObject>> criticisms = new ArrayList<Criticism<SQLObject>>();
        for (SQLRelationship.ColumnMapping cm : subject.getMappings()) {
            if (ArchitectUtils.columnsDiffer(cm.getFkColumn(), cm.getPkColumn())) {
                final SQLColumn parentColumn = cm.getPkColumn();
                final SQLTable parentTable = parentColumn.getParentTable();
                final SQLColumn childColumn = cm.getFkColumn();
                final SQLTable childTable = childColumn.getParentTable();
                criticisms.add(new Criticism<SQLObject>(
                        subject,
                        "Columns related by FK constraint have different types",
                        new QuickFix("Change type of " + childTable.getName() + "." + childColumn.getName() + " (child column) to parent's type") {
                            @Override
                            public void apply() {
                                childColumn.setType(parentColumn.getType());
                                childColumn.setPrecision(parentColumn.getPrecision());
                                childColumn.setScale(parentColumn.getScale());
                            }
                        },
                        new QuickFix("Change type of " + parentTable.getName() + "." + parentColumn.getName() + " (parent column) to child's type") {
                            @Override
                            public void apply() {
                                parentColumn.setType(childColumn.getType());
                                parentColumn.setPrecision(childColumn.getPrecision());
                                parentColumn.setScale(childColumn.getScale());
                            }
                        }
                ));
            }
        }
        return criticisms;
    }

}
