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
import com.espertech.esper.epl.expression.codegen.CodegenLegoCompareEquals;
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
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.GT;

/**
 * Represents the in-clause (set check) function in an expression tree.
 */
public class ExprInNodeForgeEvalWColl implements ExprEvaluator {
    private final ExprInNodeForge forge;
    private final ExprEvaluator[] evaluators;

    public ExprInNodeForgeEvalWColl(ExprInNodeForge forge, ExprEvaluator[] evaluators) {
        this.forge = forge;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprIn(forge.getForgeRenderable());
        }
        Boolean result = evaluateInternal(eventsPerStream, isNewData, exprEvaluatorContext);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprIn(result);
        }
        return result;
    }

    private Boolean evaluateInternal(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object inPropResult = evaluators[0].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        boolean isNotIn = forge.getForgeRenderable().isNotIn();

        int len = evaluators.length - 1;
        boolean hasNullRow = false;
        for (int i = 1; i <= len; i++) {
            Object rightResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

            if (rightResult == null) {
                continue;
            }
            if (rightResult instanceof Collection) {
                if (inPropResult == null) {
                    return null;
                }
                Collection coll = (Collection) rightResult;
                if (coll.contains(inPropResult)) {
                    return !isNotIn;
                }
            } else if (rightResult instanceof Map) {
                if (inPropResult == null) {
                    return null;
                }
                Map coll = (Map) rightResult;
                if (coll.containsKey(inPropResult)) {
                    return !isNotIn;
                }
            } else if (rightResult.getClass().isArray()) {
                int arrayLength = Array.getLength(rightResult);
                if ((arrayLength > 0) && (inPropResult == null)) {
                    return null;
                }
                for (int index = 0; index < arrayLength; index++) {
                    Object item = Array.get(rightResult, index);
                    if (item == null) {
                        hasNullRow = true;
                        continue;
                    }
                    if (!forge.isMustCoerce()) {
                        if (inPropResult.equals(item)) {
                            return !isNotIn;
                        }
                    } else {
                        if (!(item instanceof Number)) {
                            continue;
                        }
                        Number left = forge.getCoercer().coerceBoxed((Number) inPropResult);
                        Number right = forge.getCoercer().coerceBoxed((Number) item);
                        if (left.equals(right)) {
                            return !isNotIn;
                        }
                    }
                }
            } else {
                if (inPropResult == null) {
                    return null;
                }
                if (!forge.isMustCoerce()) {
                    if (inPropResult.equals(rightResult)) {
                        return !isNotIn;
                    }
                } else {
                    Number left = forge.getCoercer().coerceBoxed((Number) inPropResult);
                    Number right = forge.getCoercer().coerceBoxed((Number) rightResult);
                    if (left.equals(right)) {
                        return !isNotIn;
                    }
                }
            }
        }

        if (hasNullRow) {
            return null;
        }
        return isNotIn;
    }

    public static CodegenExpression codegen(ExprInNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprForge[] forges = ExprNodeUtilityCore.getForges(forge.getForgeRenderable().getChildNodes());
        boolean isNot = forge.getForgeRenderable().isNotIn();
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, ExprInNodeForgeEvalWColl.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(boolean.class, "hasNullRow", constantFalse());

        Class leftTypeUncoerced = forges[0].getEvaluationType();
        Class leftTypeCoerced = forge.getCoercionType();

        block.declareVar(leftTypeUncoerced, "left", forges[0].evaluateCodegen(leftTypeUncoerced, methodNode, exprSymbol, codegenClassScope));
        block.declareVar(forge.getCoercionType(), "leftCoerced", !forge.isMustCoerce() ? ref("left") : forge.getCoercer().coerceCodegenMayNullBoxed(ref("left"), leftTypeUncoerced, methodNode, codegenClassScope));

        for (int i = 1; i < forges.length; i++) {
            Class reftype = forges[i].getEvaluationType();
            ExprForge refforge = forges[i];
            String refname = "r" + i;

            if (reftype == null) {
                block.assignRef("hasNullRow", constantTrue());
                continue;
            }

            block.declareVar(reftype, refname, refforge.evaluateCodegen(reftype, methodNode, exprSymbol, codegenClassScope));

            if (JavaClassHelper.isImplementsInterface(reftype, Collection.class)) {
                CodegenBlock ifRightNotNull = block.ifCondition(notEqualsNull(ref(refname)));
                {
                    if (!leftTypeUncoerced.isPrimitive()) {
                        ifRightNotNull.ifRefNullReturnNull("left");
                    }
                    ifRightNotNull.ifCondition(exprDotMethod(ref(refname), "contains", ref("left")))
                            .blockReturn(!isNot ? constantTrue() : constantFalse());
                }
            } else if (JavaClassHelper.isImplementsInterface(reftype, Map.class)) {
                CodegenBlock ifRightNotNull = block.ifCondition(notEqualsNull(ref(refname)));
                {
                    if (!leftTypeUncoerced.isPrimitive()) {
                        ifRightNotNull.ifRefNullReturnNull("left");
                    }
                    ifRightNotNull.ifCondition(exprDotMethod(ref(refname), "containsKey", ref("left")))
                            .blockReturn(!isNot ? constantTrue() : constantFalse());
                }
            } else if (reftype.isArray()) {
                CodegenBlock ifRightNotNull = block.ifCondition(notEqualsNull(ref(refname)));
                {
                    if (!leftTypeUncoerced.isPrimitive()) {
                        ifRightNotNull.ifCondition(and(relational(arrayLength(ref(refname)), GT, constant(0)), equalsNull(ref("left"))))
                                .blockReturn(constantNull());
                    }
                    CodegenBlock forLoop = ifRightNotNull.forLoopIntSimple("index", arrayLength(ref(refname)));
                    {
                        forLoop.declareVar(reftype.getComponentType(), "item", arrayAtIndex(ref(refname), ref("index")));
                        forLoop.declareVar(boolean.class, "itemNull", reftype.getComponentType().isPrimitive() ? constantFalse() : equalsNull(ref("item")));
                        CodegenBlock itemNotNull = forLoop.ifCondition(ref("itemNull"))
                                .assignRef("hasNullRow", constantTrue())
                                .ifElse();
                        {
                            if (!forge.isMustCoerce()) {
                                itemNotNull.ifCondition(CodegenLegoCompareEquals.codegenEqualsNonNullNoCoerce(ref("leftCoerced"), leftTypeCoerced, ref("item"), reftype.getComponentType()))
                                        .blockReturn(!isNot ? constantTrue() : constantFalse());
                            } else {
                                if (JavaClassHelper.isNumeric(reftype.getComponentType())) {
                                    itemNotNull.ifCondition(CodegenLegoCompareEquals.codegenEqualsNonNullNoCoerce(ref("leftCoerced"), leftTypeCoerced, forge.getCoercer().coerceCodegen(ref("item"), reftype.getComponentType()), forge.getCoercionType()))
                                            .blockReturn(!isNot ? constantTrue() : constantFalse());
                                }
                            }
                        }
                    }
                }
            } else {
                CodegenBlock ifRightNotNull = reftype.isPrimitive() ? block : block.ifRefNotNull(refname);
                {
                    if (!leftTypeUncoerced.isPrimitive()) {
                        ifRightNotNull.ifRefNullReturnNull("left");
                    }
                    if (!forge.isMustCoerce()) {
                        ifRightNotNull.ifCondition(CodegenLegoCompareEquals.codegenEqualsNonNullNoCoerce(ref("leftCoerced"), leftTypeCoerced, ref(refname), reftype))
                                .blockReturn(!isNot ? constantTrue() : constantFalse());
                    } else {
                        ifRightNotNull.ifCondition(CodegenLegoCompareEquals.codegenEqualsNonNullNoCoerce(ref("leftCoerced"), leftTypeCoerced, forge.getCoercer().coerceCodegen(ref(refname), reftype), forge.getCoercionType()))
                                .blockReturn(!isNot ? constantTrue() : constantFalse());
                    }
                }
                if (!reftype.isPrimitive()) {
                    block.ifRefNull(refname).assignRef("hasNullRow", constantTrue());
                }
            }
        }

        block.ifCondition(ref("hasNullRow")).blockReturn(constantNull());
        block.methodReturn(isNot ? constantTrue() : constantFalse());
        return localMethod(methodNode);
    }
}
