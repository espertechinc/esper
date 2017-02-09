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
 * Base expression.
 */
public abstract class ExpressionBase implements Expression {
    private static final long serialVersionUID = 0L;

    private String treeObjectName;
    private List<Expression> children;

    public String getTreeObjectName() {
        return treeObjectName;
    }

    public void setTreeObjectName(String treeObjectName) {
        this.treeObjectName = treeObjectName;
    }

    /**
     * Ctor.
     */
    public ExpressionBase() {
        children = new ArrayList<Expression>();
    }

    /**
     * Returns the list of sub-expressions to the current expression.
     *
     * @return list of child expressions
     */
    public List<Expression> getChildren() {
        return children;
    }

    /**
     * Adds a new child expression to the current expression.
     *
     * @param expression to add
     */
    public void addChild(Expression expression) {
        children.add(expression);
    }

    public void setChildren(List<Expression> children) {
        this.children = children;
    }

    public final void toEPL(StringWriter writer, ExpressionPrecedenceEnum parentPrecedence) {
        if (this.getPrecedence().getLevel() < parentPrecedence.getLevel()) {
            writer.write("(");
            toPrecedenceFreeEPL(writer);
            writer.write(")");
        } else {
            toPrecedenceFreeEPL(writer);
        }
    }

    /**
     * Renders child expression of a function in a comma-separated list.
     *
     * @param functionName function name
     * @param children     child nodes
     * @param writer       writer
     */
    protected static void toPrecedenceFreeEPL(String functionName, List<Expression> children, StringWriter writer) {
        writer.write(functionName);
        writer.write("(");
        toPrecedenceFreeEPL(children, writer);
        writer.write(')');
    }

    /**
     * Render expression list
     *
     * @param children expressions to render
     * @param writer   writer to render to
     */
    public static void toPrecedenceFreeEPL(List<Expression> children, StringWriter writer) {
        String delimiter = "";
        for (Expression expr : children) {
            writer.write(delimiter);
            expr.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            delimiter = ",";
        }
    }

    /**
     * Render an aggregation function with distinct and parameter expressions
     *
     * @param writer   to render to
     * @param name     function name
     * @param distinct distinct flag
     * @param children parameters to render
     */
    protected static void renderAggregation(StringWriter writer, String name, boolean distinct, List<Expression> children) {
        writer.write(name);
        writer.write("(");
        if (distinct) {
            writer.write("distinct ");
        }
        String delimiter = "";
        for (Expression param : children) {
            writer.write(delimiter);
            delimiter = ",";
            param.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
        writer.write(")");
    }

    /**
     * Renders the expressions and all it's child expression, in full tree depth, as a string in
     * language syntax.
     *
     * @param writer is the output to use
     */
    public abstract void toPrecedenceFreeEPL(StringWriter writer);
}
