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
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.core.ExprWildcard;

/**
 * Represents the count(...) and count(*) and count(distinct ...) aggregate function is an expression tree.
 */
public class ExprCountNode extends ExprAggregateNodeBase {
    private static final long serialVersionUID = 1859320277242087598L;

    private boolean hasFilter;

    /**
     * Ctor.
     *
     * @param distinct - flag indicating unique or non-unique value aggregation
     */
    public ExprCountNode(boolean distinct) {
        super(distinct);
    }

    public AggregationMethodFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        if (positionalParams.length > 2 || positionalParams.length == 0) {
            throw makeExceptionExpectedParamNum(1, 2);
        }

        Class childType = null;
        boolean ignoreNulls = false;

        if (positionalParams.length == 1 && positionalParams[0] instanceof ExprWildcard) {
            validateNotDistinct();
            // defaults
        } else if (positionalParams.length == 1) {
            childType = positionalParams[0].getForge().getEvaluationType();
            ignoreNulls = true;
        } else if (positionalParams.length == 2) {
            hasFilter = true;
            if (!(positionalParams[0] instanceof ExprWildcard)) {
                childType = positionalParams[0].getForge().getEvaluationType();
                ignoreNulls = true;
            } else {
                validateNotDistinct();
            }
            super.validateFilter(positionalParams[1].getForge());
        }

        return validationContext.getEngineImportService().getAggregationFactoryFactory().makeCount(validationContext.getStatementExtensionSvcContext(), this, ignoreNulls, childType);
    }

    public String getAggregationFunctionName() {
        return "count";
    }

    public boolean isHasFilter() {
        return hasFilter;
    }

    public final boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        if (!(node instanceof ExprCountNode)) {
            return false;
        }

        return true;
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return true;
    }

    private void validateNotDistinct() throws ExprValidationException {
        if (super.isDistinct()) {
            throw new ExprValidationException("Invalid use of the 'distinct' keyword with count and wildcard");
        }
    }
}
