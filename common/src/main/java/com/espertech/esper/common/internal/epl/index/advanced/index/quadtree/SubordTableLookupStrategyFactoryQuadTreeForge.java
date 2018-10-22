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
package com.espertech.esper.common.internal.epl.index.advanced.index.quadtree;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyFactoryForge;

import java.util.function.Function;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubordTableLookupStrategyFactoryQuadTreeForge implements SubordTableLookupStrategyFactoryForge {

    private final ExprForge x;
    private final ExprForge y;
    private final ExprForge width;
    private final ExprForge height;
    private final boolean isNWOnTrigger;
    private final int streamCountOuter;
    private final LookupStrategyDesc lookupStrategyDesc;

    public SubordTableLookupStrategyFactoryQuadTreeForge(ExprForge x, ExprForge y, ExprForge width, ExprForge height, boolean isNWOnTrigger, int streamCountOuter, LookupStrategyDesc lookupStrategyDesc) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.isNWOnTrigger = isNWOnTrigger;
        this.streamCountOuter = streamCountOuter;
        this.lookupStrategyDesc = lookupStrategyDesc;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod methodNode = parent.makeChild(SubordTableLookupStrategyFactoryQuadTree.class, this.getClass(), classScope);
        Function<ExprForge, CodegenExpression> toExpr = forge -> ExprNodeUtilityCodegen.codegenEvaluator(forge, methodNode, this.getClass(), classScope);
        methodNode.getBlock()
                .declareVar(SubordTableLookupStrategyFactoryQuadTree.class, "sts", newInstance(SubordTableLookupStrategyFactoryQuadTree.class))
                .exprDotMethod(ref("sts"), "setX", toExpr.apply(x))
                .exprDotMethod(ref("sts"), "setY", toExpr.apply(y))
                .exprDotMethod(ref("sts"), "setWidth", toExpr.apply(width))
                .exprDotMethod(ref("sts"), "setHeight", toExpr.apply(height))
                .exprDotMethod(ref("sts"), "setNWOnTrigger", constant(isNWOnTrigger))
                .exprDotMethod(ref("sts"), "setStreamCountOuter", constant(streamCountOuter))
                .exprDotMethod(ref("sts"), "setLookupExpressions", constant(lookupStrategyDesc.getExpressionsTexts()))
                .methodReturn(ref("sts"));
        return localMethod(methodNode);
    }

    public LookupStrategyDesc getLookupStrategyDesc() {
        return lookupStrategyDesc;
    }
}
