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
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprEqualsAllAnyNodeForgeEvalAllWColl implements ExprEvaluator {
    private final ExprEqualsAllAnyNodeForge forge;
    private final ExprEvaluator[] evaluators;

    public ExprEqualsAllAnyNodeForgeEvalAllWColl(ExprEqualsAllAnyNodeForge forge, ExprEvaluator[] evaluators) {
        this.forge = forge;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprEqualsAnyOrAll(forge.getForgeRenderable());
        }
        Object result = evaluateInternal(eventsPerStream, isNewData, exprEvaluatorContext);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprEqualsAnyOrAll((Boolean) result);
        }
        return result;
    }

    private Object evaluateInternal(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {

        Object leftResult = evaluators[0].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

        // coerce early if testing without collections
        if (forge.isMustCoerce() && (leftResult != null)) {
            leftResult = forge.getCoercer().coerceBoxed((Number) leftResult);
        }

        return compareAll(forge.getForgeRenderable().isNot(), leftResult, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    private Object compareAll(boolean isNot, Object leftResult, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        int len = forge.getForgeRenderable().getChildNodes().length - 1;
        boolean hasNonNullRow = false;
        boolean hasNullRow = false;
        for (int i = 1; i <= len; i++) {
            Object rightResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

            if (rightResult == null) {
                hasNullRow = true;
            } else if (rightResult instanceof Collection) {
                if (leftResult == null) {
                    return null;
                }
                hasNonNullRow = true;
                Collection coll = (Collection) rightResult;
                if ((!isNot && !coll.contains(leftResult)) || (isNot && coll.contains(leftResult))) {
                    return false;
                }
            } else if (rightResult instanceof Map) {
                if (leftResult == null) {
                    return null;
                }
                hasNonNullRow = true;
                Map coll = (Map) rightResult;
                if ((!isNot && !coll.containsKey(leftResult)) || (isNot && coll.containsKey(leftResult))) {
                    return false;
                }
            } else if (rightResult.getClass().isArray()) {
                int arrayLength = Array.getLength(rightResult);
                for (int index = 0; index < arrayLength; index++) {
                    Object item = Array.get(rightResult, index);
                    if (item == null) {
                        hasNullRow = true;
                        continue;
                    }
                    if (leftResult == null) {
                        return null;
                    }
                    hasNonNullRow = true;
                    if (!forge.isMustCoerce()) {
                        if ((!isNot && !leftResult.equals(item)) || (isNot && leftResult.equals(item))) {
                            return false;
                        }
                    } else {
                        if (!(item instanceof Number)) {
                            continue;
                        }
                        Number left = forge.getCoercer().coerceBoxed((Number) leftResult);
                        Number right = forge.getCoercer().coerceBoxed((Number) item);
                        if ((!isNot && !left.equals(right)) || (isNot && left.equals(right))) {
                            return false;
                        }
                    }
                }
            } else {
                if (leftResult == null) {
                    return null;
                }
                if (!forge.isMustCoerce()) {
                    if ((!isNot && !leftResult.equals(rightResult)) || (isNot && leftResult.equals(rightResult))) {
                        return false;
                    }
                } else {
                    Number left = forge.getCoercer().coerceBoxed((Number) leftResult);
                    Number right = forge.getCoercer().coerceBoxed((Number) rightResult);
                    if ((!isNot && !left.equals(right)) || (isNot && left.equals(right))) {
                        return false;
                    }
                }
            }
        }

        if ((!hasNonNullRow) || hasNullRow) {
            return null;
        }
        return true;
    }

    public static CodegenExpression codegen(ExprEqualsAllAnyNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprForge[] forges = ExprNodeUtilityCore.getForges(forge.getForgeRenderable().getChildNodes());
        boolean isNot = forge.getForgeRenderable().isNot();
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, ExprEqualsAllAnyNodeForgeEvalAllWColl.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock();

        Class leftTypeUncoerced = forges[0].getEvaluationType();
        block.declareVar(leftTypeUncoerced, "left", forges[0].evaluateCodegen(leftTypeUncoerced, methodNode, exprSymbol, codegenClassScope));
        block.declareVar(forge.getCoercionTypeBoxed(), "leftCoerced", !forge.isMustCoerce() ? ref("left") : forge.getCoercer().coerceCodegenMayNullBoxed(ref("left"), leftTypeUncoerced, methodNode, codegenClassScope));
        block.declareVar(boolean.class, "hasNonNullRow", constantFalse());
        block.declareVar(boolean.class, "hasNullRow", constantFalse());

        for (int i = 1; i < forges.length; i++) {
            ExprForge refforge = forges[i];
            String refname = "r" + i;
            Class reftype = forges[i].getEvaluationType();

            if (JavaClassHelper.isImplementsInterface(reftype, Collection.class)) {
                block.ifRefNullReturnNull("left")
                        .declareVar(Collection.class, refname, refforge.evaluateCodegen(Collection.class, methodNode, exprSymbol, codegenClassScope))
                        .ifCondition(equalsNull(ref(refname)))
                        .assignRef("hasNullRow", constantTrue())
                        .ifElse()
                        .assignRef("hasNonNullRow", constantTrue())
                        .ifCondition(notOptional(!isNot, exprDotMethod(ref(refname), "contains", ref("left")))).blockReturn(constantFalse());
            } else if (JavaClassHelper.isImplementsInterface(reftype, Map.class)) {
                block.ifRefNullReturnNull("left")
                        .declareVar(Map.class, refname, refforge.evaluateCodegen(Map.class, methodNode, exprSymbol, codegenClassScope))
                        .ifCondition(equalsNull(ref(refname)))
                        .assignRef("hasNullRow", constantTrue())
                        .ifElse()
                        .assignRef("hasNonNullRow", constantTrue())
                        .ifCondition(notOptional(!isNot, exprDotMethod(ref(refname), "containsKey", ref("left")))).blockReturn(constantFalse());
            } else if (reftype.isArray()) {
                CodegenBlock arrayBlock = block.ifRefNullReturnNull("left")
                        .declareVar(reftype, refname, refforge.evaluateCodegen(reftype, methodNode, exprSymbol, codegenClassScope))
                        .ifCondition(equalsNull(ref(refname)))
                        .assignRef("hasNullRow", constantTrue())
                        .ifElse();

                CodegenBlock forLoop = arrayBlock.forLoopIntSimple("i", arrayLength(ref(refname)));
                CodegenExpression arrayAtIndex = arrayAtIndex(ref(refname), ref("i"));
                forLoop.declareVar(forge.getCoercionTypeBoxed(), "item", forge.getCoercer() == null ? arrayAtIndex : forge.getCoercer().coerceCodegenMayNullBoxed(arrayAtIndex, reftype.getComponentType(), methodNode, codegenClassScope));

                CodegenBlock forLoopElse = forLoop.ifCondition(equalsNull(ref("item"))).assignRef("hasNullRow", constantTrue()).ifElse();
                forLoopElse.assignRef("hasNonNullRow", constantTrue());
                forLoopElse.ifCondition(notOptional(!isNot, exprDotMethod(ref("leftCoerced"), "equals", ref("item")))).blockReturn(constantFalse());
            } else {
                block.ifRefNullReturnNull("leftCoerced");
                block.declareVar(forge.getCoercionTypeBoxed(), refname, forge.getCoercer() == null ? refforge.evaluateCodegen(forge.getCoercionTypeBoxed(), methodNode, exprSymbol, codegenClassScope) : forge.getCoercer().coerceCodegenMayNullBoxed(refforge.evaluateCodegen(forge.getCoercionTypeBoxed(), methodNode, exprSymbol, codegenClassScope), reftype, methodNode, codegenClassScope));
                CodegenBlock ifRightNotNull = block.ifRefNotNull(refname);
                {
                    ifRightNotNull.assignRef("hasNonNullRow", constantTrue());
                    ifRightNotNull.ifCondition(notOptional(!isNot, exprDotMethod(ref("leftCoerced"), "equals", ref(refname)))).blockReturn(constantFalse());
                }
                ifRightNotNull.ifElse()
                        .assignRef("hasNullRow", constantTrue());
            }
        }
        block.ifCondition(or(not(ref("hasNonNullRow")), ref("hasNullRow"))).blockReturn(constantNull());
        block.methodReturn(constantTrue());
        return localMethod(methodNode);
    }

}
