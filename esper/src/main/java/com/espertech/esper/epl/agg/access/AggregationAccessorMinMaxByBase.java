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
package com.espertech.esper.epl.agg.access;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the aggregation accessor that provides the result for the "maxBy" aggregation function.
 */
public abstract class AggregationAccessorMinMaxByBase implements AggregationAccessor {
    protected final boolean max;

    protected AggregationAccessorMinMaxByBase(boolean max) {
        this.max = max;
    }

    public Collection<EventBean> getEnumerableEvents(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean bean = getEnumerableEvent(state, eventsPerStream, isNewData, context);
        if (bean == null) {
            return null;
        }
        return Collections.singletonList(bean);
    }

    public void getEnumerableEventsCodegen(AggregationAccessorForgeGetCodegenContext context) {
        AggregationStateSortedCodegen forge = (AggregationStateSortedCodegen) context.getAccessStateForge();
        context.getMethod().getBlock()
                .declareVar(EventBean.class, "bean", max ? forge.getLastValueCodegen(context.getColumn(), context.getClassScope(), context.getMethod()) : forge.getFirstValueCodegen(context.getColumn(), context.getClassScope(), context.getMethod()))
                .ifRefNullReturnNull("bean")
                .methodReturn(staticMethod(Collections.class, "singletonList", ref("bean")));
    }

    public Collection<Object> getEnumerableScalar(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public void getEnumerableScalarCodegen(AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().methodReturn(constantNull());
    }

    public EventBean getEnumerableEvent(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (max) {
            return ((AggregationStateSorted) state).getLastValue();
        } else {
            return ((AggregationStateSorted) state).getFirstValue();
        }
    }

    public void getEnumerableEventCodegen(AggregationAccessorForgeGetCodegenContext context) {
        AggregationStateSortedCodegen sorted = (AggregationStateSortedCodegen) context.getAccessStateForge();
        if (max) {
            context.getMethod().getBlock().methodReturn(sorted.getLastValueCodegen(context.getColumn(), context.getClassScope(), context.getMethod()));
        } else {
            context.getMethod().getBlock().methodReturn(sorted.getFirstValueCodegen(context.getColumn(), context.getClassScope(), context.getMethod()));
        }
    }
}