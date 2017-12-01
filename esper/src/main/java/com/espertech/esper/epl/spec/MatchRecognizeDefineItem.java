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

import java.io.Serializable;

/**
 * Specification for a "define" construct within a match_recognize.
 */
public class MatchRecognizeDefineItem implements Serializable {
    private String identifier;
    private ExprNode expression;
    private static final long serialVersionUID = -7736241770279336651L;

    /**
     * Ctor.
     *
     * @param identifier variable name
     * @param expression expression
     */
    public MatchRecognizeDefineItem(String identifier, ExprNode expression) {
        this.identifier = identifier;
        this.expression = expression;
    }

    /**
     * Returns the variable name.
     *
     * @return name
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the expression.
     *
     * @return expression
     */
    public ExprNode getExpression() {
        return expression;
    }

    /**
     * Sets the validated expression
     *
     * @param validated to set
     */
    public void setExpression(ExprNode validated) {
        this.expression = validated;
    }
}
