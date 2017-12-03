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

import com.espertech.esper.client.soda.ExpressionBase;
import com.espertech.esper.client.soda.ExpressionPrecedenceEnum;
import com.espertech.esper.util.StringValue;

import java.io.StringWriter;

/**
 * Substitution parameter that represents a node in an expression tree for which to supply a parameter value
 * before statement creation time.
 */
public abstract class SubstitutionParameterExpressionBase extends ExpressionBase {
    private static final long serialVersionUID = -4221977145126784238L;
    private Object constant;
    private boolean isSatisfied;

    protected abstract void toPrecedenceFreeEPLUnsatisfied(StringWriter writer);

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (!isSatisfied) {
            toPrecedenceFreeEPLUnsatisfied(writer);
        } else {
            StringValue.renderConstantAsEPL(writer, constant);
        }
    }

    /**
     * Returns the constant value that the expression represents.
     *
     * @return value of constant
     */
    public Object getConstant() {
        return constant;
    }

    /**
     * Returns true if the parameter is satisfied, or false if not.
     *
     * @return true if the actual value is supplied, false if not
     */
    public boolean isSatisfied() {
        return isSatisfied;
    }

    /**
     * Sets the constant value that the expression represents.
     *
     * @param constant is the value, or null to indicate the null value
     */
    public void setConstant(Object constant) {
        this.constant = constant;
        isSatisfied = true;
    }
}
