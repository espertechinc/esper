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
package com.espertech.esper.common.internal.epl.expression.agg.method;

import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.method.leaving.AggregationForgeFactoryLeaving;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

/**
 * Represents the leaving() aggregate function is an expression tree.
 */
public class ExprLeavingAggNode extends ExprAggregateNodeBase {
    public ExprLeavingAggNode(boolean distinct) {
        super(distinct);
    }

    public AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        if (optionalFilter == null && positionalParams.length > 0) {
            throw makeExceptionExpectedParamNum(0, 0);
        }
        return new AggregationForgeFactoryLeaving(this);
    }

    public String getAggregationFunctionName() {
        return "leaving";
    }

    public final boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        return node instanceof ExprLeavingAggNode;
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return true;
    }
}