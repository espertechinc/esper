/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.example.cycledetect;

import com.espertech.esper.plugin.PlugInAggregationMultiFunctionFactory;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionDeclarationContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionHandler;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionValidationContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

public class CycleDetectorAggregationFactory implements PlugInAggregationMultiFunctionFactory {
    private ExprEvaluator fromExpression;
    private ExprEvaluator toExpression;

    public void addAggregationFunction(PlugInAggregationMultiFunctionDeclarationContext declarationContext) {
        // provides an opportunity to inspect where used
    }

    public PlugInAggregationMultiFunctionHandler validateGetHandler(PlugInAggregationMultiFunctionValidationContext validationContext) {
        if (validationContext.getParameterExpressions().length == 2) {
            fromExpression = validationContext.getParameterExpressions()[0].getExprEvaluator();
            toExpression = validationContext.getParameterExpressions()[1].getExprEvaluator();
        }
        return new CycleDetectorAggregationHandler(this, validationContext);
    }

    public ExprEvaluator getToExpression() {
        return toExpression;
    }

    public ExprEvaluator getFromExpression() {
        return fromExpression;
    }
}
