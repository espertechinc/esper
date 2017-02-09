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
 * Represents a rollup or cube in a group-by clause.
 */
public class GroupByClauseExpressionRollupOrCube implements GroupByClauseExpression {
    private static final long serialVersionUID = 2632212485394913696L;
    private boolean cube;
    private List<GroupByClauseExpression> expressions;

    /**
     * Ctor.
     *
     * @param cube        true for cube, false for rollup
     * @param expressions group-by expressions as part of rollup or cube
     */
    public GroupByClauseExpressionRollupOrCube(boolean cube, List<GroupByClauseExpression> expressions) {
        this.cube = cube;
        this.expressions = expressions;
    }

    /**
     * Ctor.
     */
    public GroupByClauseExpressionRollupOrCube() {
    }

    /**
     * Returns the rollup or cube group-by expressions.
     *
     * @return expressions
     */
    public List<GroupByClauseExpression> getExpressions() {
        return expressions;
    }

    /**
     * Sets the rollup or cube group-by expressions.
     *
     * @param expressions expressions to set
     */
    public void setExpressions(List<GroupByClauseExpression> expressions) {
        this.expressions = expressions;
    }

    /**
     * Returns true for cube, false for rollup.
     *
     * @return cube
     */
    public boolean isCube() {
        return cube;
    }

    /**
     * Set to true for cube, false for rollup.
     *
     * @param cube cube indicator
     */
    public void setCube(boolean cube) {
        this.cube = cube;
    }

    public void toEPL(StringWriter writer) {
        if (cube) {
            writer.append("cube(");
        } else {
            writer.append("rollup(");
        }
        String delimiter = "";
        for (GroupByClauseExpression child : expressions) {
            writer.write(delimiter);
            child.toEPL(writer);
            delimiter = ", ";
        }
        writer.append(")");
    }
}
