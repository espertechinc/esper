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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

/**
 * Descriptor for an on-set assignment.
 */
public class OnTriggerSetAssignment {
    private ExprNode expression;

    /**
     * Ctor.
     *
     * @param expression expression providing new variable value
     */
    public OnTriggerSetAssignment(ExprNode expression) {
        this.expression = expression;
    }

    /**
     * Returns the expression providing the new variable value, or null if none
     *
     * @return assignment expression
     */
    public ExprNode getExpression() {
        return expression;
    }

    /**
     * Sets the expression providing the new variable value
     *
     * @param expression assignment expression, or null if none
     */
    public void setExpression(ExprNode expression) {
        this.expression = expression;
    }
}
