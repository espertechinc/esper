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
package com.espertech.esper.common.internal.epl.enummethod.eval.aggregate;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeBasePlain;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumAggregateEvent extends EnumForgeBasePlain {
    private final ExprForge initialization;
    private final ExprForge innerExpression;
    private final ObjectArrayEventType eventType;
    private final int numParameters;

    public EnumAggregateEvent(int streamCountIncoming, ExprForge initialization, ExprForge innerExpression, ObjectArrayEventType eventType, int numParameters) {
        super(streamCountIncoming);
        this.initialization = initialization;
        this.innerExpression = innerExpression;
        this.eventType = eventType;
        this.numParameters = numParameters;
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator init = initialization.getExprEvaluator();
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                Object value = init.evaluate(eventsLambda, isNewData, context);

                if (enumcoll.isEmpty()) {
                    return value;
                }

                Collection<EventBean> beans = (Collection<EventBean>) enumcoll;
                ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[3], eventType);
                eventsLambda[getStreamNumLambda()] = resultEvent;
                Object[] props = resultEvent.getProperties();
                props[2] = enumcoll.size();

                int count = -1;
                for (EventBean next : beans) {
                    count++;
                    props[0] = value;
                    props[1] = count;
                    eventsLambda[getStreamNumLambda() + 1] = next;
                    value = inner.evaluate(eventsLambda, isNewData, context);
                }

                return value;
            }
        };
    }

    public CodegenExpression codegen(EnumForgeCodegenParams premade, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField typeMember = codegenClassScope.addFieldUnshared(true, ObjectArrayEventType.class, cast(ObjectArrayEventType.class, EventTypeUtility.resolveTypeCodegen(eventType, EPStatementInitServices.REF)));

        Class initType = initialization.getEvaluationType();
        Class innerType = innerExpression.getEvaluationType();

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(initType, EnumAggregateEvent.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);

        CodegenBlock block = methodNode.getBlock();
        block.declareVar(initType, "value", initialization.evaluateCodegen(initType, methodNode, scope, codegenClassScope))
            .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
            .blockReturn(ref("value"));
        block.declareVar(ObjectArrayEventBean.class, "resultEvent", newInstance(ObjectArrayEventBean.class, newArrayByLength(Object.class, constant(numParameters - 1)), typeMember))
            .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(getStreamNumLambda()), ref("resultEvent"))
            .declareVar(Object[].class, "props", exprDotMethod(ref("resultEvent"), "getProperties"));
        if (numParameters > 3) {
            block.assignArrayElement("props", constant(2), exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size"));
        }
        if (numParameters > 2) {
            block.declareVar(int.class, "count", constant(-1));
        }

        CodegenBlock forEach = block.forEach(EventBean.class, "next", EnumForgeCodegenNames.REF_ENUMCOLL)
            .assignArrayElement("props", constant(0), ref("value"));
        if (numParameters > 2) {
            forEach.incrementRef("count")
                .assignArrayElement("props", constant(1), ref("count"));
        }

        forEach.assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(getStreamNumLambda() + 1), ref("next"))
            .assignRef("value", innerExpression.evaluateCodegen(innerType, methodNode, scope, codegenClassScope))
            .blockEnd();
        block.methodReturn(ref("value"));
        return localMethod(methodNode, premade.getEps(), premade.getEnumcoll(), premade.getIsNewData(), premade.getExprCtx());
    }

    @Override
    public int getStreamNumSize() {
        return streamNumLambda + 2;
    }
}
