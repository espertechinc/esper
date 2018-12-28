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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForgeGetCodegenContext;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotUnderlying;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

/**
 * Represents the aggregation accessor that provides the result for the "maxBy" aggregation function.
 */
public class AggregationAccessorMinMaxByNonTable extends AggregationAccessorMinMaxByBase implements AggregationAccessorForge {
    public AggregationAccessorMinMaxByNonTable(boolean max) {
        super(max);
    }

    public void getValueCodegen(AggregationAccessorForgeGetCodegenContext context) {
        AggregatorAccessSorted forge = (AggregatorAccessSorted) context.getAccessStateForge().getAggregator();
        context.getMethod().getBlock().declareVar(EventBean.class, "event", max ? forge.getLastValueCodegen(context.getClassScope(), context.getMethod()) : forge.getFirstValueCodegen(context.getClassScope(), context.getMethod()))
            .ifRefNullReturnNull("event")
            .methodReturn(exprDotUnderlying(ref("event")));
    }
}