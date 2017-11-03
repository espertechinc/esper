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

/**
 * For use with on-merge clauses, deletes from a named window if matching rows are found.
 */
public class OnMergeMatchedDeleteAction implements OnMergeMatchedAction {
    private static final long serialVersionUID = 0L;

    private Expression whereClause;

    /**
     * Ctor.
     *
     * @param whereClause condition for action, or null if none required
     */
    public OnMergeMatchedDeleteAction(Expression whereClause) {
        this.whereClause = whereClause;
    }

    /**
     * Ctor.
     */
    public OnMergeMatchedDeleteAction() {
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

    @Override
    public void toEPL(StringWriter writer) {
        writer.write("delete");
        if (whereClause != null) {
            writer.write(" where ");
            whereClause.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
    }
}