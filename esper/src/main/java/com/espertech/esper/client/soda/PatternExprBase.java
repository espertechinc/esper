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
 * Abstract base class for all pattern expressions.
 */
public abstract class PatternExprBase implements PatternExpr {
    private static final long serialVersionUID = 0L;

    private String treeObjectName;
    private List<PatternExpr> children;

    public String getTreeObjectName() {
        return treeObjectName;
    }

    public void setTreeObjectName(String treeObjectName) {
        this.treeObjectName = treeObjectName;
    }

    /**
     * Ctor.
     */
    protected PatternExprBase() {
        children = new ArrayList<PatternExpr>();
    }

    public List<PatternExpr> getChildren() {
        return children;
    }

    public void setChildren(List<PatternExpr> children) {
        this.children = children;
    }

    /**
     * Adds a sub-expression to the pattern expression.
     *
     * @param expression to add
     */
    protected void addChild(PatternExpr expression) {
        children.add(expression);
    }

    public final void toEPL(StringWriter writer, PatternExprPrecedenceEnum parentPrecedence, EPStatementFormatter formatter) {
        if (this.getPrecedence().getLevel() < parentPrecedence.getLevel()) {
            writer.write("(");
            toPrecedenceFreeEPL(writer, formatter);
            writer.write(")");
        } else {
            toPrecedenceFreeEPL(writer, formatter);
        }
    }

    /**
     * Renders the expressions and all it's child expression, in full tree depth, as a string in
     * language syntax.
     *
     * @param writer    is the output to use
     * @param formatter for newline-whitespace formatting
     */
    public abstract void toPrecedenceFreeEPL(StringWriter writer, EPStatementFormatter formatter);
}
