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
import com.espertech.esper.common.internal.epl.agg.method.count.AggregationForgeFactoryCount;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.core.ExprWildcard;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

/**
 * Represents the count(...) and count(*) and count(distinct ...) aggregate function is an expression tree.
 */
public class ExprCountNode extends ExprAggregateNodeBase {
    private boolean hasFilter;

    /**
     * Ctor.
     *
     * @param distinct - flag indicating unique or non-unique value aggregation
     */
    public ExprCountNode(boolean distinct) {
        super(distinct);
    }

    public AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
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
            super.validateFilter(positionalParams[1]);
            optionalFilter = positionalParams[1];
        }

        DataInputOutputSerdeForge distinctValueSerde = isDistinct ? validationContext.getSerdeResolver().serdeForAggregationDistinct(childType, validationContext.getStatementRawInfo()) : null;
        return new AggregationForgeFactoryCount(this, ignoreNulls, childType, distinctValueSerde);
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
