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
import com.espertech.esper.common.internal.epl.agg.method.rate.AggregationForgeFactoryRate;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimestampNode;
import com.espertech.esper.common.internal.util.JavaClassHelper;

/**
 * Represents the rate(...) and aggregate function is an expression tree.
 */
public class ExprRateAggNode extends ExprAggregateNodeBase {

    /**
     * Ctor.
     *
     * @param distinct - flag indicating unique or non-unique value aggregation
     */
    public ExprRateAggNode(boolean distinct) {
        super(distinct);
    }

    public AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.positionalParams.length == 0) {
            throw new ExprValidationException("The rate aggregation function minimally requires a numeric constant or expression as a parameter.");
        }

        // handle "ever"
        ExprNode first = this.positionalParams[0];
        if (first.getForge().getForgeConstantType().isCompileTimeConstant()) {
            String message = "The rate aggregation function requires a numeric constant or time period as the first parameter in the constant-value notation";
            long intervalTime;
            if (first instanceof ExprTimePeriod) {
                double secInterval = ((ExprTimePeriod) first).evaluateAsSeconds(null, true, null);
                intervalTime = validationContext.getClasspathImportService().getTimeAbacus().deltaForSecondsDouble(secInterval);
            } else if (ExprNodeUtilityQuery.isConstant(first)) {
                if (!JavaClassHelper.isNumeric(first.getForge().getEvaluationType())) {
                    throw new ExprValidationException(message);
                }
                Number num = (Number) first.getForge().getExprEvaluator().evaluate(null, true, null);
                intervalTime = validationContext.getClasspathImportService().getTimeAbacus().deltaForSecondsNumber(num);
            } else {
                throw new ExprValidationException(message);
            }

            if (optionalFilter == null) {
                this.positionalParams = ExprNodeUtilityQuery.EMPTY_EXPR_ARRAY;
            } else {
                this.positionalParams = new ExprNode[]{optionalFilter};
            }
            return new AggregationForgeFactoryRate(this, true, intervalTime, validationContext.getClasspathImportService().getTimeAbacus());
        }

        String message = "The rate aggregation function requires a property or expression returning a non-constant long-type value as the first parameter in the timestamp-property notation";
        Class boxedParamOne = JavaClassHelper.getBoxedType(first.getForge().getEvaluationType());
        if (boxedParamOne != Long.class) {
            throw new ExprValidationException(message);
        }
        if (first.getForge().getForgeConstantType().isConstant()) {
            throw new ExprValidationException(message);
        }
        if (first instanceof ExprTimestampNode) {
            throw new ExprValidationException("The rate aggregation function does not allow the current runtime timestamp as a parameter");
        }
        if (this.positionalParams.length > 1) {
            if (!JavaClassHelper.isNumeric(this.positionalParams[1].getForge().getEvaluationType())) {
                throw new ExprValidationException("The rate aggregation function accepts an expression returning a numeric value to accumulate as an optional second parameter");
            }
        }
        boolean hasDataWindows = ExprNodeUtilityAggregation.hasRemoveStreamForAggregations(first, validationContext.getStreamTypeService(), validationContext.isResettingAggregations());
        if (!hasDataWindows) {
            throw new ExprValidationException("The rate aggregation function in the timestamp-property notation requires data windows");
        }
        if (optionalFilter != null) {
            positionalParams = ExprNodeUtilityMake.addExpression(positionalParams, optionalFilter);
        }
        return new AggregationForgeFactoryRate(this, false, -1, validationContext.getClasspathImportService().getTimeAbacus());
    }

    public String getAggregationFunctionName() {
        return "rate";
    }

    public final boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        return node instanceof ExprRateAggNode;
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return false;
    }
}