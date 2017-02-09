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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;

/**
 * Represents a single item in a SELECT-clause, with a name assigned
 * either by the engine or by the user specifying an "as" tag name.
 */
public class SelectClauseExprCompiledSpec implements SelectClauseElementCompiled {
    private ExprNode selectExpression;
    private String assignedName;
    private String providedName;
    private boolean isEvents;

    /**
     * Ctor.
     *
     * @param selectExpression - the expression node to evaluate for matching events
     * @param assignedName     - cannot be null as a name is always assigned or system-determined
     * @param providedName     - name provided
     * @param isEvents         - is events
     */
    public SelectClauseExprCompiledSpec(ExprNode selectExpression, String assignedName, String providedName, boolean isEvents) {
        this.selectExpression = selectExpression;
        this.assignedName = assignedName;
        this.providedName = providedName;
        this.isEvents = isEvents;
    }

    /**
     * Returns the expression node representing the item in the select clause.
     *
     * @return expression node for item
     */
    public ExprNode getSelectExpression() {
        return selectExpression;
    }

    /**
     * Returns the name of the item in the select clause.
     *
     * @return name of item
     */
    public String getAssignedName() {
        return assignedName;
    }

    /**
     * Sets the select expression to use.
     *
     * @param selectExpression to set
     */
    public void setSelectExpression(ExprNode selectExpression) {
        this.selectExpression = selectExpression;
    }

    /**
     * Sets the column name for the select expression.
     *
     * @param assignedName is the column name
     */
    public void setAssignedName(String assignedName) {
        this.assignedName = assignedName;
    }

    public String getProvidedName() {
        return providedName;
    }

    public boolean isEvents() {
        return isEvents;
    }

    public void setEvents(boolean events) {
        isEvents = events;
    }
}
