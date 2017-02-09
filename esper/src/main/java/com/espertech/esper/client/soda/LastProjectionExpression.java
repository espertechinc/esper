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
 * Represents the "last" aggregation function.
 */
public class LastProjectionExpression extends AccessProjectionExpressionBase {
    private static final long serialVersionUID = -6851246065918661653L;

    /**
     * Ctor.
     */
    public LastProjectionExpression() {
    }

    /**
     * Ctor.
     *
     * @param expression to aggregate
     */
    public LastProjectionExpression(Expression expression) {
        super(expression);
    }

    @Override
    public String getAggregationFunctionName() {
        return "last";
    }
}