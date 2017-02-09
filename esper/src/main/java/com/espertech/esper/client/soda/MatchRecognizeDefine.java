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

import java.io.Serializable;

/**
 * Define-clause in match-recognize expression.
 */
public class MatchRecognizeDefine implements Serializable {
    private static final long serialVersionUID = -2665038146328267165L;

    private String name;
    private Expression expression;

    /**
     * Ctor.
     */
    public MatchRecognizeDefine() {
    }

    /**
     * Ctor.
     *
     * @param name       variable name
     * @param expression expression
     */
    public MatchRecognizeDefine(String name, Expression expression) {
        this.name = name;
        this.expression = expression;
    }

    /**
     * Returns the variable name.
     *
     * @return variable name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the variable name.
     *
     * @param name variable name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the expression.
     *
     * @return expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Sets the expression.
     *
     * @param expression to set
     */
    public void setExpression(Expression expression) {
        this.expression = expression;
    }
}
