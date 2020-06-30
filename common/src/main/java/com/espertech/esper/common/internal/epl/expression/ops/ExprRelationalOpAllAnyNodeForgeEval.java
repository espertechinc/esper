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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.*;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.type.RelationalOpEnum;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprRelationalOpAllAnyNodeForgeEval implements ExprEvaluator {

    private final ExprRelationalOpAllAnyNodeForge forge;
    private final ExprEvaluator[] evaluators;

    public ExprRelationalOpAllAnyNodeForgeEval(ExprRelationalOpAllAnyNodeForge forge, ExprEvaluator[] evaluators) {
        this.forge = forge;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return evaluateInternal(eventsPerStream, isNewData, exprEvaluatorContext);
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
        ExprForge[] forges = ExprNodeUtilityQuery.getForges(forge.getForgeRenderable().getChildNodes());
        EPTypeClass valueLeftType = (EPTypeClass) forges[0].getEvaluationType();
        boolean isAll = forge.getForgeRenderable().isAll();
        if (forges.length == 1) {
            return constant(isAll);
        }

        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), ExprRelationalOpAllAnyNodeForgeEval.class, codegenClassScope);

        // when null-type value and "all" the result is always null
        if (isAll) {
            for (int i = 1; i < forges.length; i++) {
                EPType refType = forges[i].getEvaluationType();
                if (refType == null || refType == EPTypeNull.INSTANCE) {
                    methodNode.getBlock().methodReturn(constantNull());
                    return localMethod(methodNode);
                }
            }
        }

        CodegenBlock block = methodNode.getBlock()
            .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "hasNonNullRow", constantFalse());
        block.declareVar(valueLeftType, "valueLeft", forges[0].evaluateCodegen(valueLeftType, methodNode, exprSymbol, codegenClassScope));

        for (int i = 1; i < forges.length; i++) {
            ExprForge refforge = forges[i];
            String refName = "r" + i;
            EPType refType = refforge.getEvaluationType();

            if ((refType == null || refType == EPTypeNull.INSTANCE) && !isAll) {
                continue;
            }

            EPTypeClass refClass = (EPTypeClass) refType;
            block.declareVar(refClass, refName, refforge.evaluateCodegen(refClass, methodNode, exprSymbol, codegenClassScope));
            if (JavaClassHelper.isImplementsInterface(refClass, Collection.class)) {
                CodegenBlock blockIfNotNull = block.ifCondition(notEqualsNull(ref(refName)));
                {
                    CodegenBlock forEach = blockIfNotNull.forEach(EPTypePremade.OBJECT.getEPType(), "item", ref(refName));
                    {
                        CodegenBlock ifNotNumber = forEach.ifCondition(not(instanceOf(ref("item"), EPTypePremade.NUMBER.getEPType())));
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
                                ifLeftNotNull.ifCondition(notOptional(isAll, forge.getComputer().codegen(ref("valueLeft"), valueLeftType, cast(EPTypePremade.NUMBER.getEPType(), ref("item")), EPTypePremade.NUMBER.getEPType())))
                                    .blockReturn(isAll ? constantFalse() : constantTrue());
                            }
                        }
                    }
                }
            } else if (JavaClassHelper.isImplementsInterface(refClass, Map.class)) {
                CodegenBlock blockIfNotNull = block.ifCondition(notEqualsNull(ref(refName)));
                {
                    CodegenBlock forEach = blockIfNotNull.forEach(EPTypePremade.OBJECT.getEPType(), "item", exprDotMethod(ref(refName), "keySet"));
                    {
                        CodegenBlock ifNotNumber = forEach.ifCondition(not(instanceOf(ref("item"), EPTypePremade.NUMBER.getEPType())));
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
                                ifLeftNotNull.ifCondition(notOptional(isAll, forge.getComputer().codegen(ref("valueLeft"), valueLeftType, cast(EPTypePremade.NUMBER.getEPType(), ref("item")), EPTypePremade.NUMBER.getEPType())))
                                    .blockReturn(isAll ? constantFalse() : constantTrue());
                            }
                        }
                    }
                }
            } else if (refClass.getType().isArray()) {
                CodegenBlock blockIfNotNull = block.ifCondition(notEqualsNull(ref(refName)));
                {
                    EPTypeClass componentType = JavaClassHelper.getArrayComponentType(refClass);
                    CodegenBlock forLoopArray = blockIfNotNull.forLoopIntSimple("index", arrayLength(ref(refName)));
                    {
                        forLoopArray.declareVar(JavaClassHelper.getBoxedType(componentType), "item", arrayAtIndex(ref(refName), ref("index")));
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
                                ifLeftNotNull.ifCondition(notOptional(isAll, forge.getComputer().codegen(ref("valueLeft"), valueLeftType, ref("item"), EPTypePremade.NUMBER.getEPType())))
                                    .blockReturn(isAll ? constantFalse() : constantTrue());
                            }
                        }
                    }
                }
            } else if (!(JavaClassHelper.isSubclassOrImplementsInterface(JavaClassHelper.getBoxedType(refClass), Number.class))) {
                if (!refClass.getType().isPrimitive()) {
                    block.ifRefNullReturnNull(refName);
                }
                block.assignRef("hasNonNullRow", constantTrue());
                if (isAll) {
                    block.blockReturn(constantNull());
                }
            } else {
                if (refClass.getType().isPrimitive()) {
                    block.assignRef("hasNonNullRow", constantTrue());
                    block.ifCondition(notOptional(isAll, forge.getComputer().codegen(ref("valueLeft"), valueLeftType, ref(refName), refClass)))
                        .blockReturn(isAll ? constantFalse() : constantTrue());
                } else {
                    if (isAll) {
                        block.ifRefNullReturnNull(refName);
                    }
                    CodegenBlock ifRefNotNull = block.ifRefNotNull(refName);
                    {
                        ifRefNotNull.assignRef("hasNonNullRow", constantTrue());
                        CodegenBlock ifLeftNotNull = ifRefNotNull.ifCondition(notEqualsNull(ref("valueLeft")));
                        ifLeftNotNull.ifCondition(notOptional(isAll, forge.getComputer().codegen(ref("valueLeft"), valueLeftType, ref(refName), EPTypePremade.NUMBER.getEPType())))
                            .blockReturn(isAll ? constantFalse() : constantTrue());
                    }
                }
            }
        }

        block.ifCondition(not(ref("hasNonNullRow")))
            .blockReturn(constantNull());
        if (!valueLeftType.getType().isPrimitive()) {
            block.ifRefNullReturnNull("valueLeft");
        }
        block.methodReturn(constant(isAll));
        return localMethod(methodNode);
    }
}
