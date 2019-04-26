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
import com.espertech.esper.common.internal.epl.agg.method.count.AggregationForgeFactoryCountEver;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.core.ExprWildcard;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

/**
 * Represents the "countever" aggregate function is an expression tree.
 */
public class ExprCountEverNode extends ExprAggregateNodeBase {

    public ExprCountEverNode(boolean distinct) {
        super(distinct);
    }

    public AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        if (positionalParams.length > 2) {
            throw makeExceptionExpectedParamNum(0, 2);
        }
        if (super.isDistinct) {
            throw new ExprValidationException("Aggregation function '" + getAggregationFunctionName() + "' does now allow distinct");
        }

        boolean ignoreNulls = false;
        if (positionalParams.length == 0) {
            // no parameters is allowed
        } else {
            ignoreNulls = !(positionalParams[0] instanceof ExprWildcard);
            if (positionalParams.length == 2) {
                super.validateFilter(positionalParams[1]);
                optionalFilter = positionalParams[1];
            }
        }
        Class childType = getChildNodes()[0].getForge().getEvaluationType();
        DataInputOutputSerdeForge distinctSerde = isDistinct ? validationContext.getSerdeResolver().serdeForAggregationDistinct(childType, validationContext.getStatementRawInfo()) : null;
        return new AggregationForgeFactoryCountEver(this, ignoreNulls, childType, distinctSerde);
    }

    public boolean hasFilter() {
        return positionalParams.length == 2;
    }

    public String getAggregationFunctionName() {
        return "countever";
    }

    public final boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        return node instanceof ExprCountEverNode;
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return true;
    }
}