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
import com.espertech.esper.common.internal.epl.agg.method.stddev.AggregationForgeFactoryStddev;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

/**
 * Represents the stddev(...) aggregate function is an expression tree.
 */
public class ExprStddevNode extends ExprAggregateNodeBase {
    private boolean hasFilter;

    public ExprStddevNode(boolean distinct) {
        super(distinct);
    }

    public AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        hasFilter = positionalParams.length > 1;
        if (hasFilter) {
            optionalFilter = positionalParams[1];
        }
        Class childType = super.validateNumericChildAllowFilter(hasFilter);
        DataInputOutputSerdeForge distinctSerde = isDistinct ? validationContext.getSerdeResolver().serdeForAggregationDistinct(childType, validationContext.getStatementRawInfo()) : null;
        return new AggregationForgeFactoryStddev(this, childType, distinctSerde);
    }

    public boolean isHasFilter() {
        return hasFilter;
    }

    public final boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        if (!(node instanceof ExprStddevNode)) {
            return false;
        }

        return true;
    }

    public String getAggregationFunctionName() {
        return "stddev";
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return true;
    }
}
