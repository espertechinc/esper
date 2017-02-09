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

/**
 * Represents the "window" aggregation function.
 */
public class WindowProjectionExpression extends AccessProjectionExpressionBase {
    private static final long serialVersionUID = -8775046889683797339L;

    /**
     * Ctor.
     */
    public WindowProjectionExpression() {
    }

    /**
     * Ctor.
     *
     * @param expression to aggregate
     */
    public WindowProjectionExpression(Expression expression) {
        super(expression);
    }

    @Override
    public String getAggregationFunctionName() {
        return "window";
    }
}