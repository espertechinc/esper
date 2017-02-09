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
 * Pattern observer expression observes occurances such as timer-at (crontab) and timer-interval.
 */
public class PatternObserverExpr extends EPBaseNamedObject implements PatternExpr {
    private static final long serialVersionUID = 0L;

    private String treeObjectName;

    public void setChildren(List<PatternExpr> children) {
        // this expression has no child expressions
    }

    /**
     * Ctor.
     */
    public PatternObserverExpr() {
    }

    public String getTreeObjectName() {
        return treeObjectName;
    }

    public void setTreeObjectName(String treeObjectName) {
        this.treeObjectName = treeObjectName;
    }

    /**
     * Ctor - for use to create a pattern expression tree, without pattern child expression.
     *
     * @param namespace  is the guard object namespace
     * @param name       is the guard object name
     * @param parameters is guard object parameters
     */
    public PatternObserverExpr(String namespace, String name, Expression[] parameters) {
        super(namespace, name, Arrays.asList(parameters));
    }

    /**
     * Ctor - for use to create a pattern expression tree, without pattern child expression.
     *
     * @param namespace  is the guard object namespace
     * @param name       is the guard object name
     * @param parameters is guard object parameters
     */
    public PatternObserverExpr(String namespace, String name, List<Expression> parameters) {
        super(namespace, name, parameters);
    }

    public List<PatternExpr> getChildren() {
        return new ArrayList<PatternExpr>();
    }

    public PatternExprPrecedenceEnum getPrecedence() {
        return PatternExprPrecedenceEnum.ATOM;
    }

    public void toEPL(StringWriter writer, PatternExprPrecedenceEnum parentPrecedence, EPStatementFormatter formatter) {
        if (this.getPrecedence().getLevel() < parentPrecedence.getLevel()) {
            writer.write("(");
            toPrecedenceFreeEPL(writer);
            writer.write(")");
        } else {
            toPrecedenceFreeEPL(writer);
        }
    }

    /**
     * Renders the expressions and all it's child expression, in full tree depth, as a string in
     * language syntax.
     *
     * @param writer is the output to use
     */
    public void toPrecedenceFreeEPL(StringWriter writer) {
        super.toEPL(writer);
    }

}
