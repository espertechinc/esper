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
import com.espertech.esper.type.RelationalOpEnum;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprRelationalOpAllAnyNodeForgeEval implements ExprEvaluator {

    private final ExprRelationalOpAllAnyNodeForge forge;
    private final ExprEvaluator[] evaluators;

    public ExprRelationalOpAllAnyNodeForgeEval(ExprRelationalOpAllAnyNodeForge forge, ExprEvaluator[] evaluators) {
        this.forge = forge;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprRelOpAnyOrAll(forge.getForgeRenderable(), forge.getForgeRenderable().getRelationalOpEnum().getExpressionText());
        }
        Boolean result = evaluateInternal(eventsPerStream, isNewData, exprEvaluatorContext);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprRelOpAnyOrAll(result);
        }
        return result;
    }

    private Boolean evaluateInternal(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (evaluators.length == 1) {
            return false;
        }

        boolean isAll = forge.getForgeRenderable().isAll();
        RelationalOpEnum.Computer computer = forge.getComputer();
        Object valueLeft = evaluators[0].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        int len = evaluators.length - 1;

        if (forge.isHasCollectionOrArray()) {
            boolean hasNonNullRow = false;
            boolean hasRows = false;
            for (int i = 1; i <= len; i++) {
                Object valueRight = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

                if (valueRight == null) {
                    continue;
                }

                if (valueRight instanceof Collection) {
                    Collection coll = (Collection) valueRight;
                    hasRows = true;
                    for (Object item : coll) {
                        if (!(item instanceof Number)) {
                            if (isAll && item == null) {
                                return null;
                            }
                            continue;
                        }
                        hasNonNullRow = true;
                        if (valueLeft != null) {
                            if (isAll) {
                                if (!computer.compare(valueLeft, item)) {
                                    return false;
                                }
                            } else {
                                if (computer.compare(valueLeft, item)) {
                                    return true;
                                }
                            }
                        }
                    }
                } else if (valueRight instanceof Map) {
                    Map coll = (Map) valueRight;
                    hasRows = true;
                    for (Object item : coll.keySet()) {
                        if (!(item instanceof Number)) {
                            if (isAll && item == null) {
                                return null;
                            }
                            continue;
                        }
                        hasNonNullRow = true;
                        if (valueLeft != null) {
                            if (isAll) {
                                if (!computer.compare(valueLeft, item)) {
                                    return false;
                                }
                            } else {
                                if (computer.compare(valueLeft, item)) {
                                    return true;
                                }
                            }
                        }
                    }
                } else if (valueRight.getClass().isArray()) {
                    hasRows = true;
                    int arrayLength = Array.getLength(valueRight);
                    for (int index = 0; index < arrayLength; index++) {
                        Object item = Array.get(valueRight, index);
                        if (item == null) {
                            if (isAll) {
                                return null;
                            }
                            continue;
                        }
                        hasNonNullRow = true;
                        if (valueLeft != null) {
                            if (isAll) {
                                if (!computer.compare(valueLeft, item)) {
                                    return false;
                                }
                            } else {
                                if (computer.compare(valueLeft, item)) {
                                    return true;
                                }
                            }
                        }
                    }
                } else if (!(valueRight instanceof Number)) {
                    if (isAll) {
                        return null;
                    }
                } else {
                    hasNonNullRow = true;
                    if (isAll) {
                        if (!computer.compare(valueLeft, valueRight)) {
                            return false;
                        }
                    } else {
                        if (computer.compare(valueLeft, valueRight)) {
                            return true;
                        }
                    }
                }
            }

            if (isAll) {
                if (!hasRows) {
                    return true;
                }
                if ((!hasNonNullRow) || (valueLeft == null)) {
                    return null;
                }
                return true;
            } else {
                if (!hasRows) {
                    return false;
                }
                if ((!hasNonNullRow) || (valueLeft == null)) {
                    return null;
                }
                return false;
            }
        } else {
            boolean hasNonNullRow = false;
            boolean hasRows = false;
            for (int i = 1; i <= len; i++) {
                Object valueRight = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                hasRows = true;

                if (valueRight != null) {
                    hasNonNullRow = true;
                } else {
                    if (isAll) {
                        return null;
                    }
                }

                if ((valueRight != null) && (valueLeft != null)) {
                    if (isAll) {
                        if (!computer.compare(valueLeft, valueRight)) {
                            return false;
                        }
                    } else {
                        if (computer.compare(valueLeft, valueRight)) {
                            return true;
                        }
                    }
                }
            }

            if (isAll) {
                if (!hasRows) {
                    return true;
                }
                if ((!hasNonNullRow) || (valueLeft == null)) {
                    return null;
                }
                return true;
            } else {
                if (!hasRows) {
                    return false;
                }
                if ((!hasNonNullRow) || (valueLeft == null)) {
                    return null;
                }
                return false;
            }
        }
    }

    public static CodegenExpression codegen(ExprRelationalOpAllAnyNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprForge[] forges = ExprNodeUtilityCore.getForges(forge.getForgeRenderable().getChildNodes());
        Class valueLeftType = forges[0].getEvaluationType();
        boolean isAll = forge.getForgeRenderable().isAll();
        if (forges.length == 1) {
            return constant(isAll);
        }

        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, ExprRelationalOpAllAnyNodeForgeEval.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(boolean.class, "hasNonNullRow", constantFalse())
                .declareVar(valueLeftType, "valueLeft", forges[0].evaluateCodegen(valueLeftType, methodNode, exprSymbol, codegenClassScope));

        for (int i = 1; i < forges.length; i++) {
            ExprForge refforge = forges[i];
            String refname = "r" + i;
            Class reftype = refforge.getEvaluationType();
            block.declareVar(reftype, refname, refforge.evaluateCodegen(reftype, methodNode, exprSymbol, codegenClassScope));

            if (JavaClassHelper.isImplementsInterface(reftype, Collection.class)) {
                CodegenBlock blockIfNotNull = block.ifCondition(notEqualsNull(ref(refname)));
                {
                    CodegenBlock forEach = blockIfNotNull.forEach(Object.class, "item", ref(refname));
                    {
                        CodegenBlock ifNotNumber = forEach.ifCondition(not(instanceOf(ref("item"), Number.class)));
                        {
                            if (isAll) {
                                ifNotNumber.ifRefNullReturnNull("item");
                            }
                        }
                        CodegenBlock ifNotNumberElse = ifNotNumber.ifElse();
                        {
                            ifNotNumberElse.assignRef("hasNonNullRow", constantTrue());
                            CodegenBlock ifLeftNotNull = ifNotNumberElse.ifCondition(notEqualsNull(ref("valueLeft")));
                            {
                                ifLeftNotNull.ifCondition(notOptional(isAll, forge.getComputer().codegen(ref("valueLeft"), valueLeftType, cast(Number.class, ref("item")), Number.class)))
                                        .blockReturn(isAll ? constantFalse() : constantTrue());
                            }
                        }
                    }
                }
            } else if (JavaClassHelper.isImplementsInterface(reftype, Map.class)) {
                CodegenBlock blockIfNotNull = block.ifCondition(notEqualsNull(ref(refname)));
                {
                    CodegenBlock forEach = blockIfNotNull.forEach(Object.class, "item", exprDotMethod(ref(refname), "keySet"));
                    {
                        CodegenBlock ifNotNumber = forEach.ifCondition(not(instanceOf(ref("item"), Number.class)));
                        {
                            if (isAll) {
                                ifNotNumber.ifRefNullReturnNull("item");
                            }
                        }
                        CodegenBlock ifNotNumberElse = ifNotNumber.ifElse();
                        {
                            ifNotNumberElse.assignRef("hasNonNullRow", constantTrue());
                            CodegenBlock ifLeftNotNull = ifNotNumberElse.ifCondition(notEqualsNull(ref("valueLeft")));
                            {
                                ifLeftNotNull.ifCondition(notOptional(isAll, forge.getComputer().codegen(ref("valueLeft"), valueLeftType, cast(Number.class, ref("item")), Number.class)))
                                        .blockReturn(isAll ? constantFalse() : constantTrue());
                            }
                        }
                    }
                }
            } else if (reftype.isArray()) {
                CodegenBlock blockIfNotNull = block.ifCondition(notEqualsNull(ref(refname)));
                {
                    CodegenBlock forLoopArray = blockIfNotNull.forLoopIntSimple("index", arrayLength(ref(refname)));
                    {
                        forLoopArray.declareVar(JavaClassHelper.getBoxedType(reftype.getComponentType()), "item", arrayAtIndex(ref(refname), ref("index")));
                        CodegenBlock ifItemNull = forLoopArray.ifCondition(equalsNull(ref("item")));
                        {
                            if (isAll) {
                                ifItemNull.ifReturn(constantNull());
                            }
                        }
                        CodegenBlock ifItemNotNull = ifItemNull.ifElse();
                        {
                            ifItemNotNull.assignRef("hasNonNullRow", constantTrue());
                            CodegenBlock ifLeftNotNull = ifItemNotNull.ifCondition(notEqualsNull(ref("valueLeft")));
                            {
                                ifLeftNotNull.ifCondition(notOptional(isAll, forge.getComputer().codegen(ref("valueLeft"), valueLeftType, ref("item"), Number.class)))
                                        .blockReturn(isAll ? constantFalse() : constantTrue());
                            }
                        }
                    }
                }
            } else if (!(JavaClassHelper.isSubclassOrImplementsInterface(JavaClassHelper.getBoxedType(reftype), Number.class))) {
                if (!reftype.isPrimitive()) {
                    block.ifRefNullReturnNull(refname);
                }
                block.assignRef("hasNonNullRow", constantTrue());
                if (isAll) {
                    block.blockReturn(constantNull());
                }
            } else {
                if (reftype.isPrimitive()) {
                    block.assignRef("hasNonNullRow", constantTrue());
                    block.ifCondition(notOptional(isAll, forge.getComputer().codegen(ref("valueLeft"), valueLeftType, ref(refname), reftype)))
                            .blockReturn(isAll ? constantFalse() : constantTrue());
                } else {
                    if (isAll) {
                        block.ifRefNullReturnNull(refname);
                    }
                    CodegenBlock ifRefNotNull = block.ifRefNotNull(refname);
                    {
                        ifRefNotNull.assignRef("hasNonNullRow", constantTrue());
                        CodegenBlock ifLeftNotNull = ifRefNotNull.ifCondition(notEqualsNull(ref("valueLeft")));
                        ifLeftNotNull.ifCondition(notOptional(isAll, forge.getComputer().codegen(ref("valueLeft"), valueLeftType, ref(refname), Number.class)))
                                .blockReturn(isAll ? constantFalse() : constantTrue());
                    }
                }
            }
        }

        block.ifCondition(not(ref("hasNonNullRow")))
                .blockReturn(constantNull());
        if (!valueLeftType.isPrimitive()) {
            block.ifRefNullReturnNull("valueLeft");
        }
        block.methodReturn(constant(isAll));
        return localMethod(methodNode);
    }
}
