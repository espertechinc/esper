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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprArrayNodeForgeEval implements ExprEvaluator, ExprEnumerationEval {

    private final ExprArrayNodeForge forge;
    private final ExprEvaluator[] evaluators;

    public ExprArrayNodeForgeEval(ExprArrayNodeForge forge, ExprEvaluator[] evaluators) {
        this.forge = forge;
        this.evaluators = evaluators;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprArray(forge.getForgeRenderable());
        }
        Object array = Array.newInstance(forge.getArrayReturnType(), evaluators.length);
        int index = 0;
        for (ExprEvaluator child : evaluators) {
            Object result = child.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (result != null) {
                if (forge.isMustCoerce()) {
                    Number boxed = (Number) result;
                    Object coercedResult = forge.getCoercer().coerceBoxed(boxed);
                    Array.set(array, index, coercedResult);
                } else {
                    Array.set(array, index, result);
                }
            }
            index++;
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprArray(array);
        }
        return array;
    }

    public static CodegenExpression codegen(ExprArrayNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(forge.getEvaluationType(), ExprArrayNodeForgeEval.class, codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(forge.getEvaluationType(), "array", newArrayByLength(forge.getArrayReturnType(), constant(forge.getForgeRenderable().getChildNodes().length)));
        for (int i = 0; i < forge.getForgeRenderable().getChildNodes().length; i++) {
            ExprForge child = forge.getForgeRenderable().getChildNodes()[i].getForge();
            Class childType = child.getEvaluationType();
            String refname = "r" + i;
            block.declareVar(childType, refname, child.evaluateCodegen(childType, methodNode, exprSymbol, codegenClassScope));

            if (child.getEvaluationType().isPrimitive()) {
                if (!forge.isMustCoerce()) {
                    block.assignArrayElement("array", constant(i), ref(refname));
                } else {
                    block.assignArrayElement("array", constant(i), forge.getCoercer().coerceCodegen(ref(refname), child.getEvaluationType()));
                }
            } else {
                CodegenBlock ifNotNull = block.ifCondition(notEqualsNull(ref(refname)));
                if (!forge.isMustCoerce()) {
                    ifNotNull.assignArrayElement("array", constant(i), ref(refname));
                } else {
                    ifNotNull.assignArrayElement("array", constant(i), forge.getCoercer().coerceCodegen(ref(refname), child.getEvaluationType()));
                }
            }
        }
        block.methodReturn(ref("array"));
        return localMethod(methodNode);
    }


    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (forge.getForgeRenderable().getChildNodes().length == 0) {
            return Collections.emptyList();
        }

        ArrayDeque resultList = new ArrayDeque(evaluators.length);
        for (ExprEvaluator child : evaluators) {
            Object result = child.evaluate(eventsPerStream, isNewData, context);
            if (result != null) {
                if (forge.isMustCoerce()) {
                    Number boxed = (Number) result;
                    Object coercedResult = forge.getCoercer().coerceBoxed(boxed);
                    resultList.add(coercedResult);
                } else {
                    resultList.add(result);
                }
            }
        }

        return resultList;
    }

    public static CodegenExpression codegenEvaluateGetROCollectionScalar(ExprArrayNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprNode[] children = forge.getForgeRenderable().getChildNodes();
        if (children.length == 0) {
            return staticMethod(Collections.class, "emptyList");
        }
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Collection.class, ExprArrayNodeForgeEval.class, codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(ArrayDeque.class, "resultList", newInstance(ArrayDeque.class, constant(children.length)));
        int count = -1;
        for (ExprNode child : children) {
            count++;
            String refname = "r" + count;
            ExprForge childForge = child.getForge();
            Class returnType = childForge.getEvaluationType();
            if (returnType == null) {
                continue;
            }
            block.declareVar(returnType, refname, childForge.evaluateCodegen(returnType, methodNode, exprSymbol, codegenClassScope));
            CodegenExpression nonNullTest = returnType.isPrimitive() ? constantTrue() : notEqualsNull(ref(refname));
            CodegenBlock blockIfNotNull = block.ifCondition(nonNullTest);
            CodegenExpression added = ref(refname);
            if (forge.isMustCoerce()) {
                added = forge.getCoercer().coerceCodegen(ref(refname), childForge.getEvaluationType());
            }
            blockIfNotNull.expression(exprDotMethod(ref("resultList"), "add", added));
        }
        block.methodReturn(ref("resultList"));
        return localMethod(methodNode);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }
}
