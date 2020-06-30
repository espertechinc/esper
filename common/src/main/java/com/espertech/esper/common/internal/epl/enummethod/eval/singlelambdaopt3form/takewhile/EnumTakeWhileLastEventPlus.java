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
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormEventPlus;
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
import static com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.takewhile.EnumTakeWhileHelper.takeWhileLastEventBeanToArray;

public class EnumTakeWhileLastEventPlus extends ThreeFormEventPlus {
    private CodegenExpression innerValue;

    public EnumTakeWhileLastEventPlus(ExprDotEvalParamLambda lambda, ObjectArrayEventType indexEventType, int numParameters) {
        super(lambda, indexEventType, numParameters);
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                if (enumcoll.isEmpty()) {
                    return enumcoll;
                }

                Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
                ObjectArrayEventBean indexEvent = new ObjectArrayEventBean(new Object[2], fieldEventType);
                eventsLambda[getStreamNumLambda() + 1] = indexEvent;
                Object[] props = indexEvent.getProperties();
                props[0] = 0;
                props[1] = enumcoll.size();

                if (enumcoll.size() == 1) {
                    EventBean item = beans.iterator().next();
                    eventsLambda[getStreamNumLambda()] = item;

                    Object pass = inner.evaluate(eventsLambda, isNewData, context);
                    if (pass == null || (!(Boolean) pass)) {
                        return Collections.emptyList();
                    }
                    return Collections.singletonList(item);
                }

                EventBean[] all = takeWhileLastEventBeanToArray(enumcoll);
                ArrayDeque<Object> result = new ArrayDeque<Object>();
                int count = -1;

                for (int i = all.length - 1; i >= 0; i--) {
                    count++;
                    props[0] = count;
                    eventsLambda[getStreamNumLambda()] = all[i];

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
        EnumTakeWhileHelper.initBlockSizeOneEventPlus(numParameters, block, innerValue, getStreamNumLambda(), (EPTypeClass) innerExpression.getEvaluationType());
        block.declareVar(EventBean.EPTYPEARRAY, "all", staticMethod(EnumTakeWhileHelper.class, "takeWhileLastEventBeanToArray", EnumForgeCodegenNames.REF_ENUMCOLL));

        CodegenBlock forEach = block.forLoop(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "i", op(arrayLength(ref("all")), "-", constant(1)), relational(ref("i"), GE, constant(0)), decrementRef("i"))
            .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(getStreamNumLambda()), arrayAtIndex(ref("all"), ref("i")))
            .incrementRef("count")
            .assignArrayElement("props", constant(0), ref("count"));

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
