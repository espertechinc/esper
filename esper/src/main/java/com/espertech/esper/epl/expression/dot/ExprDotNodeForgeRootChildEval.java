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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEnumerationEval;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

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
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprDot(forge.getParent());
        }
        Object inner = innerEvaluator.evaluate(eventsPerStream, isNewData, context);
        if (inner != null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprDotChain(forge.innerForge.getTypeInfo(), inner, evalUnpacking);
            }
            inner = ExprDotNodeUtility.evaluateChain(forge.forgesUnpacking, evalUnpacking, inner, eventsPerStream, isNewData, context);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprDotChain();
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprDot(inner);
        }
        return inner;
    }

    public static CodegenExpression codegen(ExprDotNodeForgeRootChild forge, CodegenParamSetExprPremade params, CodegenContext context) {
        Class innerType = EPTypeHelper.getCodegenReturnType(forge.innerForge.getTypeInfo());
        Class evaluationType = forge.getEvaluationType();
        CodegenBlock block = context.addMethod(evaluationType, ExprDotNodeForgeRootChildEval.class).add(params).begin()
                .declareVar(innerType, "inner", forge.innerForge.codegenEvaluate(params, context));
        if (!innerType.isPrimitive() && evaluationType != void.class) {
            block.ifRefNullReturnNull("inner");
        }
        CodegenExpression expression = ExprDotNodeUtility.evaluateChainCodegen(context, params, ref("inner"), innerType, forge.forgesUnpacking, null);
        CodegenMethodId method = evaluationType == void.class ? block.expression(expression).methodEnd() : block.methodReturn(expression);
        return localMethodBuild(method).passAll(params).call();
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object inner = innerEvaluator.evaluateGetROCollectionEvents(eventsPerStream, isNewData, context);
        if (inner != null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprDotChain(forge.innerForge.getTypeInfo(), inner, evalUnpacking);
            }
            inner = ExprDotNodeUtility.evaluateChain(forge.forgesIteratorEventBean, evalIteratorEventBean, inner, eventsPerStream, isNewData, context);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprDotChain();
            }
            if (inner instanceof Collection) {
                return (Collection<EventBean>) inner;
            }
        }
        return null;
    }

    public static CodegenExpression codegenEvaluateGetROCollectionEvents(ExprDotNodeForgeRootChild forge, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMethodId method = context.addMethod(forge.getEvaluationType(), ExprDotNodeForgeRootChildEval.class).add(params).begin()
                .declareVar(Collection.class, "inner", forge.innerForge.evaluateGetROCollectionEventsCodegen(params, context))
                .ifRefNullReturnNull("inner")
                .methodReturn(ExprDotNodeUtility.evaluateChainCodegen(context, params, ref("inner"), Collection.class, forge.forgesIteratorEventBean, null));
        return localMethodBuild(method).passAll(params).call();
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object inner = innerEvaluator.evaluateGetROCollectionScalar(eventsPerStream, isNewData, context);
        if (inner != null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprDotChain(forge.innerForge.getTypeInfo(), inner, evalUnpacking);
            }
            inner = ExprDotNodeUtility.evaluateChain(forge.forgesIteratorEventBean, evalIteratorEventBean, inner, eventsPerStream, isNewData, context);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprDotChain();
            }
            if (inner instanceof Collection) {
                return (Collection) inner;
            }
        }
        return null;
    }

    public static CodegenExpression codegenEvaluateGetROCollectionScalar(ExprDotNodeForgeRootChild forge, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMethodId method = context.addMethod(forge.getEvaluationType(), ExprDotNodeForgeRootChildEval.class).add(params).begin()
                .declareVar(Collection.class, "inner", forge.innerForge.evaluateGetROCollectionScalarCodegen(params, context))
                .ifRefNullReturnNull("inner")
                .methodReturn(ExprDotNodeUtility.evaluateChainCodegen(context, params, ref("inner"), Collection.class, forge.forgesIteratorEventBean, null));
        return localMethodBuild(method).passAll(params).call();
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

}
