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
package com.espertech.esper.common.internal.epl.agg.access.linear;

import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForgeGetCodegenContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

/**
 * Represents the aggregation accessor that provides the result for the "window" aggregation function.
 */
public class AggregationAccessorWindowWEvalForge implements AggregationAccessorForge {
    private final int streamNum;
    private final ExprForge childNode;
    private final Class componentType;

    /**
     * Ctor.
     *
     * @param streamNum     stream id
     * @param childNode     expression
     * @param componentType type
     */
    public AggregationAccessorWindowWEvalForge(int streamNum, ExprForge childNode, Class componentType) {
        this.streamNum = streamNum;
        this.childNode = childNode;
        this.componentType = componentType;
    }

    public void getValueCodegen(AggregationAccessorForgeGetCodegenContext context) {
        AggregationAccessorWindowWEval.getValueCodegen(this, (AggregationStateLinearForge) context.getAccessStateForge(), context);
    }

    public void getEnumerableEventsCodegen(AggregationAccessorForgeGetCodegenContext context) {
        AggregationAccessorWindowWEval.getEnumerableEventsCodegen(this, (AggregationStateLinearForge) context.getAccessStateForge(), context);
    }

    public void getEnumerableEventCodegen(AggregationAccessorForgeGetCodegenContext context) {
        context.getMethod().getBlock().methodReturn(constantNull());
    }

    public void getEnumerableScalarCodegen(AggregationAccessorForgeGetCodegenContext context) {
        AggregationAccessorWindowWEval.getEnumerableScalarCodegen(this, (AggregationStateLinearForge) context.getAccessStateForge(), context);
    }

    public int getStreamNum() {
        return streamNum;
    }

    public ExprForge getChildNode() {
        return childNode;
    }

    public Class getComponentType() {
        return componentType;
    }
}