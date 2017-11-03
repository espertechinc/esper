/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.client.soda;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

/**
 * For use with on-merge clauses, updates rows in a named window if matching rows are found.
 */
public class OnMergeMatchedUpdateAction implements OnMergeMatchedAction {
    private static final long serialVersionUID = 0L;

    private List<Assignment> assignments = Collections.emptyList();
    private Expression whereClause;

    /**
     * Ctor.
     */
    public OnMergeMatchedUpdateAction() {
    }

    /**
     * Ctor.
     *
     * @param assignments assignments of values to columns
     * @param whereClause optional condition or null
     */
    public OnMergeMatchedUpdateAction(List<Assignment> assignments, Expression whereClause) {
        this.assignments = assignments;
        this.whereClause = whereClause;
    }

    /**
     * Returns the action condition, or null if undefined.
     *
     * @return condition
     */
    public Expression getWhereClause() {
        return whereClause;
    }

    /**
     * Sets the action condition, or null if undefined.
     *
     * @param whereClause to set, or null to remove the condition
     */
    public void setWhereClause(Expression whereClause) {
        this.whereClause = whereClause;
    }

    /**
     * Returns the assignments to execute against any rows found in a named window
     *
     * @return assignments
     */
    public List<Assignment> getAssignments() {
        return assignments;
    }

    /**
     * Sets the assignments to execute against any rows found in a named window
     *
     * @param assignments to set
     */
    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }

    @Override
    public void toEPL(StringWriter writer) {
        writer.write("update ");
        UpdateClause.renderEPLAssignments(writer, assignments);
        if (whereClause != null) {
            writer.write(" where ");
            whereClause.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
    }
}