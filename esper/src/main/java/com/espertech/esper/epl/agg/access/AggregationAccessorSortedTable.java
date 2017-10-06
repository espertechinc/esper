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
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.agg.factory.AggregationStateSortedForge;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataInternalEventToPublic;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionCodegenType;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.*;

/**
 * Represents the aggregation accessor that provides the result for the "maxBy" aggregation function.
 */
public class AggregationAccessorSortedTable implements AggregationAccessor, AggregationAccessorForge {
    private final boolean max;
    private final Class componentType;
    private final TableMetadata tableMetadata;

    public AggregationAccessorSortedTable(boolean max, Class componentType, TableMetadata tableMetadata) {
        this.max = max;
        this.componentType = componentType;
        this.tableMetadata = tableMetadata;
    }

    public PlugInAggregationMultiFunctionCodegenType getPluginCodegenType() {
        return PlugInAggregationMultiFunctionCodegenType.CODEGEN_ALL;
    }

    public Object getValue(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        AggregationStateSorted sorted = (AggregationStateSorted) state;
        if (sorted.size() == 0) {
            return null;
        }
        Object array = Array.newInstance(componentType, sorted.size());

        Iterator<EventBean> it;
        if (max) {
            it = sorted.getReverseIterator();
        } else {
            it = sorted.iterator();
        }

        int count = 0;
        for (; it.hasNext(); ) {
            EventBean bean = it.next();
            Object und = tableMetadata.getEventToPublic().convertToUnd(bean, eventsPerStream, isNewData, exprEvaluatorContext);
            Array.set(array, count++, und);
        }
        return array;
    }

    public void getValueCodegen(AggregationAccessorForgeGetCodegenContext context) {
        CodegenMember eventToPublic = context.getClassScope().makeAddMember(TableMetadataInternalEventToPublic.class, tableMetadata.getEventToPublic());
        AggregationStateSortedForge sorted = (AggregationStateSortedForge) context.getAccessStateForge();
        CodegenExpression size = sorted.sizeCodegen(context.getColumn());
        CodegenExpression iterator = max ? sorted.getReverseIteratorCodegen(context.getColumn()) : sorted.iteratorCodegen(context.getColumn());

        context.getMethod().getBlock().ifCondition(equalsIdentity(size, constant(0))).blockReturn(constantNull())
                .declareVar(JavaClassHelper.getArrayType(componentType), "array", newArrayByLength(componentType, size))
                .declareVar(int.class, "count", constant(0))
                .declareVar(Iterator.class, "it", iterator)
                .whileLoop(exprDotMethod(ref("it"), "hasNext"))
                .declareVar(EventBean.class, "bean", cast(EventBean.class, exprDotMethod(ref("it"), "next")))
                .assignArrayElement(ref("array"), ref("count"), exprDotMethod(member(eventToPublic.getMemberId()), "convertToUnd", ref("bean"), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT))
                .increment("count")
                .blockEnd()
                .methodReturn(ref("array"));
    }

    public Collection<EventBean> getEnumerableEvents(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return ((AggregationStateSorted) state).collectionReadOnly();
    }

    public void getEnumerableEventsCodegen(AggregationAccessorForgeGetCodegenContext context) {
        AggregationStateSortedForge sorted = (AggregationStateSortedForge) context.getAccessStateForge();
        context.getMethod().getBlock().methodReturn(sorted.collectionReadOnlyCodegen(context.getColumn()));
    }

    public Collection<Object> getEnumerableScalar(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public void getEnumerableScalarCodegen(AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().methodReturn(constantNull());
    }

    public EventBean getEnumerableEvent(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public void getEnumerableEventCodegen(AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().methodReturn(constantNull());
    }

    public AggregationAccessor getAccessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        return this;
    }
}