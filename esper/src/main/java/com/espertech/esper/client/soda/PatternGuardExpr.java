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
import java.util.Arrays;
import java.util.List;

/**
 * Guard is the where timer-within pattern object for use in pattern expressions.
 */
public class PatternGuardExpr extends EPBaseNamedObject implements PatternExpr {
    private static final long serialVersionUID = 0L;

    private String treeObjectName;
    private List<PatternExpr> guarded;

    /**
     * Ctor - for use to create a pattern expression tree, without pattern child expression.
     *
     * @param namespace  is the guard object namespace
     * @param name       is the guard object name
     * @param parameters is guard object parameters
     */
    public PatternGuardExpr(String namespace, String name, List<Expression> parameters) {
        super(namespace, name, parameters);
        this.guarded = new ArrayList<PatternExpr>();
    }

    /**
     * Ctor - for use to create a pattern expression tree, without pattern child expression.
     *
     * @param namespace  is the guard object namespace
     * @param name       is the guard object name
     * @param parameters is guard object parameters
     * @param guarded    is the guarded pattern expression
     */
    public PatternGuardExpr(String namespace, String name, Expression[] parameters, PatternExpr guarded) {
        this(namespace, name, Arrays.asList(parameters), guarded);
    }

    /**
     * Ctor - for use to create a pattern expression tree, without pattern child expression.
     *
     * @param namespace      is the guard object namespace
     * @param name           is the guard object name
     * @param parameters     is guard object parameters
     * @param guardedPattern is the guarded pattern expression
     */
    public PatternGuardExpr(String namespace, String name, List<Expression> parameters, PatternExpr guardedPattern) {
        super(namespace, name, parameters);
        this.guarded = new ArrayList<PatternExpr>();
        guarded.add(guardedPattern);
    }

    public List<PatternExpr> getChildren() {
        return guarded;
    }

    /**
     * Set sub pattern.
     *
     * @param guarded sub expression
     */
    public void setGuarded(List<PatternExpr> guarded) {
        this.guarded = guarded;
    }

    /**
     * Get sub expression
     *
     * @return sub pattern
     */
    public List<PatternExpr> getGuarded() {
        return guarded;
    }

    public String getTreeObjectName() {
        return treeObjectName;
    }

    public void setTreeObjectName(String treeObjectName) {
        this.treeObjectName = treeObjectName;
    }

    public PatternExprPrecedenceEnum getPrecedence() {
        return PatternExprPrecedenceEnum.GUARD;
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
    public void toPrecedenceFreeEPL(StringWriter writer, EPStatementFormatter formatter) {
        guarded.get(0).toEPL(writer, getPrecedence(), formatter);
        if (GuardEnum.isWhile(this.getNamespace(), this.getName())) {
            writer.write(" while (");
            this.getParameters().get(0).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.write(")");
        } else {
            writer.write(" where ");
            super.toEPL(writer);
        }
    }

    public void setChildren(List<PatternExpr> children) {
        guarded = children;
    }
}
