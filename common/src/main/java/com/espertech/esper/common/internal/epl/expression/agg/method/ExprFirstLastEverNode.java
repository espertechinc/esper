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
import com.espertech.esper.common.internal.epl.agg.method.firstlastever.AggregationForgeFactoryFirstLastEver;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.core.ExprWildcard;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

/**
 * Represents the "firstever" and "lastever: aggregate function is an expression tree.
 */
public class ExprFirstLastEverNode extends ExprAggregateNodeBase {

    private final boolean first;

    /**
     * Ctor.
     *
     * @param first    true if first
     * @param distinct - flag indicating unique or non-unique value aggregation
     */
    public ExprFirstLastEverNode(boolean distinct, boolean first) {
        super(distinct);
        this.first = first;
    }

    public AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        if (positionalParams.length > 2) {
            throw makeExceptionExpectedParamNum(0, 2);
        }
        if (positionalParams.length == 2) {
            super.validateFilter(positionalParams[1]);
            optionalFilter = positionalParams[1];
        }

        Class resultType;
        boolean isWildcard = positionalParams.length == 0 || positionalParams.length > 0 && positionalParams[0] instanceof ExprWildcard;
        if (isWildcard) {
            resultType = validationContext.getStreamTypeService().getEventTypes()[0].getUnderlyingType();
        } else {
            resultType = positionalParams[0].getForge().getEvaluationType();
        }
        DataInputOutputSerdeForge serde = validationContext.getSerdeResolver().serdeForAggregation(resultType, validationContext.getStatementRawInfo());
        return new AggregationForgeFactoryFirstLastEver(this, resultType, serde);
    }

    public boolean hasFilter() {
        return positionalParams.length == 2;
    }

    public String getAggregationFunctionName() {
        return first ? "firstever" : "lastever";
    }

    public final boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        if (!(node instanceof ExprFirstLastEverNode)) {
            return false;
        }
        ExprFirstLastEverNode other = (ExprFirstLastEverNode) node;
        return other.first == this.first;
    }

    public boolean isFirst() {
        return first;
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return true;
    }
}