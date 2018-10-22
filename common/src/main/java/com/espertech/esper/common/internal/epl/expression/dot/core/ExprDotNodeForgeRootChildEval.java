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
package com.espertech.esper.common.internal.epl.expression.dot.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEnumerationEval;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode;
import com.espertech.esper.common.internal.rettype.EPTypeCodegenSharable;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotNodeForgeRootChildEval implements ExprEvaluator, ExprEnumerationEval {
    private final ExprDotNodeForgeRootChild forge;
    private final ExprDotEvalRootChildInnerEval innerEvaluator;
    private final ExprDotEval[] evalIteratorEventBean;
    private final ExprDotEval[] evalUnpacking;

    public ExprDotNodeForgeRootChildEval(ExprDotNodeForgeRootChild forge, ExprDotEvalRootChildInnerEval innerEvaluator, ExprDotEval[] evalIteratorEventBean, ExprDotEval[] evalUnpacking) {
        this.forge = forge;
        this.innerEvaluator = innerEvaluator;
        this.evalIteratorEventBean = evalIteratorEventBean;
        this.evalUnpacking = evalUnpacking;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object inner = innerEvaluator.evaluate(eventsPerStream, isNewData, context);
        if (inner != null) {
            inner = ExprDotNodeUtility.evaluateChain(forge.forgesUnpacking, evalUnpacking, inner, eventsPerStream, isNewData, context);
        }
        return inner;
    }

    public static CodegenExpression codegen(ExprDotNodeForgeRootChild forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class innerType = EPTypeHelper.getCodegenReturnType(forge.innerForge.getTypeInfo());
        Class evaluationType = forge.getEvaluationType();
        CodegenMethod methodNode = codegenMethodScope.makeChild(evaluationType, ExprDotNodeForgeRootChildEval.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(innerType, "inner", forge.innerForge.codegenEvaluate(methodNode, exprSymbol, codegenClassScope));
        if (!innerType.isPrimitive() && evaluationType != void.class) {
            block.ifRefNullReturnNull("inner");
        }

        CodegenExpression typeInformation = constantNull();
        if (codegenClassScope.isInstrumented()) {
            typeInformation = codegenClassScope.addOrGetFieldSharable(new EPTypeCodegenSharable(forge.innerForge.getTypeInfo(), codegenClassScope));
        }

        block.apply(InstrumentationCode.instblock(codegenClassScope, "qExprDotChain", typeInformation, ref("inner"), constant(forge.forgesUnpacking.length)));
        CodegenExpression expression = ExprDotNodeUtility.evaluateChainCodegen(methodNode, exprSymbol, codegenClassScope, ref("inner"), innerType, forge.forgesUnpacking, null);
        if (evaluationType == void.class) {
            block.expression(expression)
                    .apply(InstrumentationCode.instblock(codegenClassScope, "aExprDotChain"))
                    .methodEnd();
        } else {
            block.declareVar(evaluationType, "result", expression)
                    .apply(InstrumentationCode.instblock(codegenClassScope, "aExprDotChain"))
                    .methodReturn(ref("result"));
        }
        return localMethod(methodNode);
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object inner = innerEvaluator.evaluateGetROCollectionEvents(eventsPerStream, isNewData, context);
        if (inner != null) {
            inner = ExprDotNodeUtility.evaluateChain(forge.forgesIteratorEventBean, evalIteratorEventBean, inner, eventsPerStream, isNewData, context);
            if (inner instanceof Collection) {
                return (Collection<EventBean>) inner;
            }
        }
        return null;
    }

    public static CodegenExpression codegenEvaluateGetROCollectionEvents(ExprDotNodeForgeRootChild forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(forge.getEvaluationType(), ExprDotNodeForgeRootChildEval.class, codegenClassScope);

        CodegenExpression typeInformation = constantNull();
        if (codegenClassScope.isInstrumented()) {
            typeInformation = codegenClassScope.addOrGetFieldSharable(new EPTypeCodegenSharable(forge.innerForge.getTypeInfo(), codegenClassScope));
        }

        methodNode.getBlock()
                .declareVar(Collection.class, "inner", forge.innerForge.evaluateGetROCollectionEventsCodegen(methodNode, exprSymbol, codegenClassScope))
                .apply(InstrumentationCode.instblock(codegenClassScope, "qExprDotChain", typeInformation, ref("inner"), constant(forge.forgesUnpacking.length)))
                .ifRefNull("inner")
                .apply(InstrumentationCode.instblock(codegenClassScope, "aExprDotChain"))
                .blockReturn(constantNull())
                .declareVar(forge.getEvaluationType(), "result", ExprDotNodeUtility.evaluateChainCodegen(methodNode, exprSymbol, codegenClassScope, ref("inner"), Collection.class, forge.forgesIteratorEventBean, null))
                .apply(InstrumentationCode.instblock(codegenClassScope, "aExprDotChain"))
                .methodReturn(ref("result"));
        return localMethod(methodNode);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object inner = innerEvaluator.evaluateGetROCollectionScalar(eventsPerStream, isNewData, context);
        if (inner != null) {
            inner = ExprDotNodeUtility.evaluateChain(forge.forgesIteratorEventBean, evalIteratorEventBean, inner, eventsPerStream, isNewData, context);
            if (inner instanceof Collection) {
                return (Collection) inner;
            }
        }
        return null;
    }

    public static CodegenExpression codegenEvaluateGetROCollectionScalar(ExprDotNodeForgeRootChild forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(forge.getEvaluationType(), ExprDotNodeForgeRootChildEval.class, codegenClassScope);

        CodegenExpression typeInformation = constantNull();
        if (codegenClassScope.isInstrumented()) {
            typeInformation = codegenClassScope.addOrGetFieldSharable(new EPTypeCodegenSharable(forge.innerForge.getTypeInfo(), codegenClassScope));
        }

        methodNode.getBlock().declareVar(Collection.class, "inner", forge.innerForge.evaluateGetROCollectionScalarCodegen(methodNode, exprSymbol, codegenClassScope))
                .apply(InstrumentationCode.instblock(codegenClassScope, "qExprDotChain", typeInformation, ref("inner"), constant(forge.forgesUnpacking.length)))
                .ifRefNull("inner")
                .apply(InstrumentationCode.instblock(codegenClassScope, "aExprDotChain"))
                .blockReturn(constantNull())
                .declareVar(forge.getEvaluationType(), "result", ExprDotNodeUtility.evaluateChainCodegen(methodNode, exprSymbol, codegenClassScope, ref("inner"), Collection.class, forge.forgesIteratorEventBean, null))
                .apply(InstrumentationCode.instblock(codegenClassScope, "aExprDotChain"))
                .methodReturn(ref("result"));
        return localMethod(methodNode);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

}
