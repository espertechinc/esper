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
import com.espertech.esper.common.internal.epl.agg.method.nth.AggregationForgeFactoryNth;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

/**
 * Represents the nth(...) and aggregate function is an expression tree.
 */
public class ExprNthAggNode extends ExprAggregateNodeBase {
    /**
     * Ctor.
     *
     * @param distinct - flag indicating unique or non-unique value aggregation
     */
    public ExprNthAggNode(boolean distinct) {
        super(distinct);
    }

    public AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        String message = "The nth aggregation function requires two parameters, an expression returning aggregation values and a numeric index constant";
        if (this.positionalParams.length != 2) {
            throw new ExprValidationException(message);
        }

        ExprNode first = this.positionalParams[0];
        ExprNode second = this.positionalParams[1];
        if (!second.getForge().getForgeConstantType().isCompileTimeConstant()) {
            throw new ExprValidationException(message);
        }

        Number num = (Number) second.getForge().getExprEvaluator().evaluate(null, true, null);
        int size = num.intValue();

        if (optionalFilter != null) {
            this.positionalParams = ExprNodeUtilityMake.addExpression(positionalParams, optionalFilter);
        }

        Class childType = first.getForge().getEvaluationType();
        DataInputOutputSerdeForge serde = validationContext.getSerdeResolver().serdeForAggregationDistinct(childType, validationContext.getStatementRawInfo());
        DataInputOutputSerdeForge distinctValueSerde = isDistinct ? serde : null;
        return new AggregationForgeFactoryNth(this, childType, serde, distinctValueSerde, size);
    }

    public String getAggregationFunctionName() {
        return "nth";
    }

    public final boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        return node instanceof ExprNthAggNode;
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return false;
    }
}