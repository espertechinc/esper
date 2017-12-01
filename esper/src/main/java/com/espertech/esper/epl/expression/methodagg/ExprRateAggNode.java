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
package com.espertech.esper.epl.expression.methodagg;

import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.time.ExprTimestampNode;
import com.espertech.esper.util.JavaClassHelper;

/**
 * Represents the rate(...) and aggregate function is an expression tree.
 */
public class ExprRateAggNode extends ExprAggregateNodeBase {
    private static final long serialVersionUID = -1616393720555472129L;

    /**
     * Ctor.
     *
     * @param distinct - flag indicating unique or non-unique value aggregation
     */
    public ExprRateAggNode(boolean distinct) {
        super(distinct);
    }

    public AggregationMethodFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.positionalParams.length == 0) {
            throw new ExprValidationException("The rate aggregation function minimally requires a numeric constant or expression as a parameter.");
        }

        // handle "ever"
        ExprNode first = this.positionalParams[0];
        if (first.isConstantResult()) {
            String message = "The rate aggregation function requires a numeric constant or time period as the first parameter in the constant-value notation";
            long intervalTime;
            if (first instanceof ExprTimePeriod) {
                double secInterval = ((ExprTimePeriod) first).evaluateAsSeconds(null, true, validationContext.getExprEvaluatorContext());
                intervalTime = validationContext.getEngineImportService().getTimeAbacus().deltaForSecondsDouble(secInterval);
            } else if (ExprNodeUtilityCore.isConstantValueExpr(first)) {
                if (!JavaClassHelper.isNumeric(first.getForge().getEvaluationType())) {
                    throw new ExprValidationException(message);
                }
                Number num = (Number) first.getForge().getExprEvaluator().evaluate(null, true, validationContext.getExprEvaluatorContext());
                intervalTime = validationContext.getEngineImportService().getTimeAbacus().deltaForSecondsNumber(num);
            } else {
                throw new ExprValidationException(message);
            }

            if (optionalFilter == null) {
                this.positionalParams = ExprNodeUtilityCore.EMPTY_EXPR_ARRAY;
            } else {
                this.positionalParams = new ExprNode[]{optionalFilter};
            }
            return validationContext.getEngineImportService().getAggregationFactoryFactory().makeRate(validationContext.getStatementExtensionSvcContext(), this, true, intervalTime, validationContext.getTimeProvider(), validationContext.getEngineImportService().getTimeAbacus());
        }

        String message = "The rate aggregation function requires a property or expression returning a non-constant long-type value as the first parameter in the timestamp-property notation";
        Class boxedParamOne = JavaClassHelper.getBoxedType(first.getForge().getEvaluationType());
        if (boxedParamOne != Long.class) {
            throw new ExprValidationException(message);
        }
        if (first.isConstantResult()) {
            throw new ExprValidationException(message);
        }
        if (first instanceof ExprTimestampNode) {
            throw new ExprValidationException("The rate aggregation function does not allow the current engine timestamp as a parameter");
        }
        if (this.positionalParams.length > 1) {
            if (!JavaClassHelper.isNumeric(this.positionalParams[1].getForge().getEvaluationType())) {
                throw new ExprValidationException("The rate aggregation function accepts an expression returning a numeric value to accumulate as an optional second parameter");
            }
        }
        boolean hasDataWindows = ExprNodeUtilityRich.hasRemoveStreamForAggregations(first, validationContext.getStreamTypeService(), validationContext.isResettingAggregations());
        if (!hasDataWindows) {
            throw new ExprValidationException("The rate aggregation function in the timestamp-property notation requires data windows");
        }
        if (optionalFilter != null) {
            positionalParams = ExprNodeUtilityCore.addExpression(positionalParams, optionalFilter);
        }
        return validationContext.getEngineImportService().getAggregationFactoryFactory().makeRate(validationContext.getStatementExtensionSvcContext(), this, false, -1, validationContext.getTimeProvider(), validationContext.getEngineImportService().getTimeAbacus());
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