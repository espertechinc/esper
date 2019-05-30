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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForgeGetCodegenContext;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Iterator;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;

/**
 * Represents the aggregation accessor that provides the result for the "maxBy" aggregation function.
 */
public class AggregationAccessorSortedTable implements AggregationAccessorForge {
    private final boolean max;
    private final Class componentType;
    private final TableMetaData table;

    public AggregationAccessorSortedTable(boolean max, Class componentType, TableMetaData table) {
        this.max = max;
        this.componentType = componentType;
        this.table = table;
    }

    public void getValueCodegen(AggregationAccessorForgeGetCodegenContext context) {
        CodegenExpressionField eventToPublic = TableDeployTimeResolver.makeTableEventToPublicField(table, context.getClassScope(), this.getClass());
        AggregatorAccessSorted sorted = (AggregatorAccessSorted) context.getAccessStateForge().getAggregator();
        CodegenExpression size = sorted.sizeCodegen();
        CodegenExpression iterator = max ? sorted.getReverseIteratorCodegen() : sorted.iteratorCodegen();

        context.getMethod().getBlock().ifCondition(equalsIdentity(size, constant(0))).blockReturn(constantNull())
            .declareVar(JavaClassHelper.getArrayType(componentType), "array", newArrayByLength(componentType, size))
            .declareVar(int.class, "count", constant(0))
            .declareVar(Iterator.class, "it", iterator)
            .whileLoop(exprDotMethod(ref("it"), "hasNext"))
            .declareVar(EventBean.class, "bean", cast(EventBean.class, exprDotMethod(ref("it"), "next")))
            .assignArrayElement(ref("array"), ref("count"), exprDotMethod(eventToPublic, "convertToUnd", ref("bean"), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT))
            .incrementRef("count")
            .blockEnd()
            .methodReturn(ref("array"));
    }

    public void getEnumerableEventsCodegen(AggregationAccessorForgeGetCodegenContext context) {
        AggregatorAccessSorted sorted = (AggregatorAccessSorted) context.getAccessStateForge().getAggregator();
        context.getMethod().getBlock().methodReturn(sorted.collectionReadOnlyCodegen());
    }

    public void getEnumerableScalarCodegen(AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().methodReturn(constantNull());
    }

    public void getEnumerableEventCodegen(AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().methodReturn(constantNull());
    }
}