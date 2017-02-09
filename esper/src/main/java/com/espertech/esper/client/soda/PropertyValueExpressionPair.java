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
 * Pair of expressions with "equals" operator between.
 */
public class PropertyValueExpressionPair implements Serializable {
    private static final long serialVersionUID = 2207038136736490910L;

    private Expression left;
    private Expression right;

    /**
     * Ctor.
     */
    public PropertyValueExpressionPair() {
    }

    /**
     * Ctor.
     *
     * @param left  expression
     * @param right expression
     */
    public PropertyValueExpressionPair(PropertyValueExpression left, PropertyValueExpression right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Returns left expr.
     *
     * @return left
     */
    public Expression getLeft() {
        return left;
    }

    /**
     * Sets left expr.
     *
     * @param left left
     */
    public void setLeft(Expression left) {
        this.left = left;
    }

    /**
     * Returns right side.
     *
     * @return right side
     */
    public Expression getRight() {
        return right;
    }

    /**
     * Sets right side.
     *
     * @param right to set
     */
    public void setRight(Expression right) {
        this.right = right;
    }
}
