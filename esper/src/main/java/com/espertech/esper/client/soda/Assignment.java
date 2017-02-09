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
 * An assignment is an expression specifically for the purpose of usage in updates.
 * Usually an assignment is an equal-expression with the lhs being an event property or variable
 * and the rhs being the new value expression.
 */
public class Assignment implements Serializable {
    private static final long serialVersionUID = -2713092091207302856L;

    private Expression value;

    /**
     * Ctor.
     */
    public Assignment() {
    }

    /**
     * Ctor.
     *
     * @param value value to assign
     */
    public Assignment(Expression value) {
        this.value = value;
    }

    /**
     * Returns expression to eval.
     *
     * @return eval expression
     */
    public Expression getValue() {
        return value;
    }

    /**
     * Sets expression to eval.
     *
     * @param value expression
     */
    public void setValue(Expression value) {
        this.value = value;
    }
}
