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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.takewhile;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormScalar;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.GE;
import static com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.takewhile.EnumTakeWhileHelper.takeWhileLastScalarToArray;

public class EnumTakeWhileLastScalar extends ThreeFormScalar {
    private CodegenExpression innerValue;

    public EnumTakeWhileLastScalar(ExprDotEvalParamLambda lambda, ObjectArrayEventType fieldEventType, int numParameters) {
        super(lambda, fieldEventType, numParameters);
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                if (enumcoll.isEmpty()) {
                    return enumcoll;
                }

                ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[3], fieldEventType);
                eventsLambda[getStreamNumLambda()] = evalEvent;
                Object[] props = evalEvent.getProperties();
                props[2] = enumcoll.size();

                if (enumcoll.size() == 1) {
                    Object item = enumcoll.iterator().next();
                    props[0] = item;
                    props[1] = 0;

                    Object pass = inner.evaluate(eventsLambda, isNewData, context);
                    if (pass == null || (!(Boolean) pass)) {
                        return Collections.emptyList();
                    }
                    return Collections.singletonList(item);
                }

                Object[] all = takeWhileLastScalarToArray(enumcoll);
                ArrayDeque<Object> result = new ArrayDeque<Object>();
                int count = -1;

                for (int i = all.length - 1; i >= 0; i--) {
                    props[0] = all[i];
                    count++;
                    props[1] = count;

                    Object pass = inner.evaluate(eventsLambda, isNewData, context);
                    if (pass == null || (!(Boolean) pass)) {
                        break;
                    }

                    result.addFirst(all[i]);
                }

                return result;
            }
        };
    }

    public EPTypeClass returnTypeOfMethod() {
        return EPTypePremade.COLLECTION.getEPType();
    }

    public CodegenExpression returnIfEmptyOptional() {
        return EnumForgeCodegenNames.REF_ENUMCOLL;
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        innerValue = innerExpression.evaluateCodegen(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), methodNode, scope, codegenClassScope);
        EnumTakeWhileHelper.initBlockSizeOneScalar(numParameters, block, innerValue, innerExpression.getEvaluationType());
        block.declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "all", staticMethod(EnumTakeWhileHelper.class, "takeWhileLastScalarToArray", EnumForgeCodegenNames.REF_ENUMCOLL));

        CodegenBlock forEach = block.forLoop(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "i", op(arrayLength(ref("all")), "-", constant(1)), relational(ref("i"), GE, constant(0)), decrementRef("i"))
            .assignArrayElement("props", constant(0), arrayAtIndex(ref("all"), ref("i")));
        if (numParameters >= 2) {
            forEach.incrementRef("count")
                .assignArrayElement("props", constant(1), ref("count"));
        }
        CodegenLegoBooleanExpression.codegenBreakIfNotNullAndNotPass(forEach, innerExpression.getEvaluationType(), innerValue);
        forEach.expression(exprDotMethod(ref("result"), "addFirst", arrayAtIndex(ref("all"), ref("i"))));
    }

    @Override
    public boolean hasForEachLoop() {
        return false;
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        throw new IllegalStateException();
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(ref("result"));
    }
}
