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
package com.espertech.esper.example.cycledetect;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionDeclarationContext;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionForge;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionHandler;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

public class CycleDetectorAggregationForge implements AggregationMultiFunctionForge {
    private ExprNode fromExpression;
    private ExprNode toExpression;

    public void addAggregationFunction(AggregationMultiFunctionDeclarationContext declarationContext) {
        // provides an opportunity to inspect where used
    }

    public AggregationMultiFunctionHandler validateGetHandler(AggregationMultiFunctionValidationContext validationContext) {
        if (validationContext.getParameterExpressions().length == 2) {
            fromExpression = validationContext.getParameterExpressions()[0];
            toExpression = validationContext.getParameterExpressions()[1];
        }
        return new CycleDetectorAggregationHandler(this, validationContext);
    }

    public ExprNode getFromExpression() {
        return fromExpression;
    }

    public ExprNode getToExpression() {
        return toExpression;
    }
}
