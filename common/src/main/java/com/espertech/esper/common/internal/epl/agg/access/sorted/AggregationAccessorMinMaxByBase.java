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

import java.util.Collections;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the aggregation accessor that provides the result for the "maxBy" aggregation function.
 */
public abstract class AggregationAccessorMinMaxByBase implements AggregationAccessorForge {
    protected final boolean max;

    public AggregationAccessorMinMaxByBase(boolean max) {
        this.max = max;
    }

    public void getEnumerableEventsCodegen(AggregationAccessorForgeGetCodegenContext context) {
        AggregatorAccessSorted forge = (AggregatorAccessSorted) context.getAccessStateForge().getAggregator();
        context.getMethod().getBlock()
            .declareVar(EventBean.class, "bean", max ? forge.getLastValueCodegen(context.getClassScope(), context.getMethod()) : forge.getFirstValueCodegen(context.getClassScope(), context.getMethod()))
            .ifRefNullReturnNull("bean")
            .methodReturn(staticMethod(Collections.class, "singletonList", ref("bean")));
    }

    public void getEnumerableScalarCodegen(AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().methodReturn(constantNull());
    }

    public void getEnumerableEventCodegen(AggregationAccessorForgeGetCodegenContext context) {
        AggregatorAccessSorted sorted = (AggregatorAccessSorted) context.getAccessStateForge().getAggregator();
        if (max) {
            context.getMethod().getBlock().methodReturn(sorted.getLastValueCodegen(context.getClassScope(), context.getMethod()));
        } else {
            context.getMethod().getBlock().methodReturn(sorted.getFirstValueCodegen(context.getClassScope(), context.getMethod()));
        }
    }
}