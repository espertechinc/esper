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
import java.util.List;

/**
 * Represents the "grouping sets" keywords.
 */
public class GroupByClauseExpressionGroupingSet implements GroupByClauseExpression {
    private static final long serialVersionUID = 6071844123689652600L;
    private List<GroupByClauseExpression> expressions;

    /**
     * Ctor.
     *
     * @param expressions group-by expressions withing grouping set
     */
    public GroupByClauseExpressionGroupingSet(List<GroupByClauseExpression> expressions) {
        this.expressions = expressions;
    }

    /**
     * Ctor.
     */
    public GroupByClauseExpressionGroupingSet() {
    }

    /**
     * Returns list of expressions in grouping set.
     *
     * @return group-by expressions
     */
    public List<GroupByClauseExpression> getExpressions() {
        return expressions;
    }

    /**
     * Sets the list of expressions in grouping set.
     *
     * @param expressions group-by expressions
     */
    public void setExpressions(List<GroupByClauseExpression> expressions) {
        this.expressions = expressions;
    }

    public void toEPL(StringWriter writer) {
        writer.write("grouping sets(");
        String delimiter = "";
        for (GroupByClauseExpression child : expressions) {
            writer.write(delimiter);
            child.toEPL(writer);
            delimiter = ", ";
        }
        writer.write(")");
    }
}
