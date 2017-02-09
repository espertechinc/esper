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
import java.util.ArrayList;
import java.util.List;

/**
 * A clause to assign new values to variables based on a triggering event arriving.
 */
public class OnSetClause extends OnClause {
    private static final long serialVersionUID = 0L;

    private List<Assignment> assignments;

    /**
     * Creates a new on-set clause for setting variables, and adds a variable to set.
     *
     * @param expression is the assignment expression providing the new variable value
     * @return on-set clause
     */
    public static OnSetClause create(Expression expression) {
        OnSetClause clause = new OnSetClause();
        clause.addAssignment(expression);
        return clause;
    }

    /**
     * Ctor.
     */
    public OnSetClause() {
        assignments = new ArrayList<Assignment>();
    }

    /**
     * Adds a variable to set to the clause.
     *
     * @param expression expression providing the new variable value
     * @return clause
     */
    public OnSetClause addAssignment(Expression expression) {
        assignments.add(new Assignment(expression));
        return this;
    }

    /**
     * Returns the list of variable assignments.
     *
     * @return pair of variable name and expression
     */
    public List<Assignment> getAssignments() {
        return assignments;
    }

    /**
     * Sets a list of variable assignments.
     *
     * @param assignments list of pairs of variable name and expression
     */
    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }

    /**
     * Renders the clause in EPL.
     *
     * @param writer    to output to
     * @param formatter for newline-whitespace formatting
     */
    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        formatter.beginOnSet(writer);
        UpdateClause.renderEPLAssignments(writer, assignments);
    }
}
