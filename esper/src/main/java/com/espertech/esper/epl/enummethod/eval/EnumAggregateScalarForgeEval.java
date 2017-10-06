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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.*;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EnumAggregateScalarForgeEval implements EnumEval {

    private final EnumAggregateScalarForge forge;
    private final ExprEvaluator initialization;
    private final ExprEvaluator innerExpression;

    public EnumAggregateScalarForgeEval(EnumAggregateScalarForge forge, ExprEvaluator initialization, ExprEvaluator innerExpression) {
        this.forge = forge;
        this.initialization = initialization;
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        Object value = initialization.evaluate(eventsLambda, isNewData, context);

        if (enumcoll.isEmpty()) {
            return value;
        }

        ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[1], forge.resultEventType);
        ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[1], forge.getEvalEventType());
        eventsLambda[forge.streamNumLambda] = resultEvent;
        eventsLambda[forge.streamNumLambda + 1] = evalEvent;
        Object[] resultProps = resultEvent.getProperties();
        Object[] evalProps = evalEvent.getProperties();

        for (Object next : enumcoll) {
            resultProps[0] = value;
            evalProps[0] = next;
            value = innerExpression.evaluate(eventsLambda, isNewData, context);
        }

        return value;
    }

    public static CodegenExpression codegen(EnumAggregateScalarForge forge, EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember resultTypeMember = codegenClassScope.makeAddMember(ObjectArrayEventType.class, forge.resultEventType);
        CodegenMember evalTypeMember = codegenClassScope.makeAddMember(ObjectArrayEventType.class, forge.evalEventType);

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethodNode methodNode = codegenMethodScope.makeChildWithScope(forge.initialization.getEvaluationType(), EnumAggregateScalarForgeEval.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);

        Class initializationEvalType = forge.initialization.getEvaluationType();
        Class innerEvalType = forge.innerExpression.getEvaluationType();
        CodegenBlock block = methodNode.getBlock();
        block.declareVar(initializationEvalType, "value", forge.initialization.evaluateCodegen(initializationEvalType, methodNode, scope, codegenClassScope))
                .ifCondition(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "isEmpty"))
                .blockReturn(ref("value"));
        block.declareVar(ObjectArrayEventBean.class, "resultEvent", newInstance(ObjectArrayEventBean.class, newArrayByLength(Object.class, constant(1)), member(resultTypeMember.getMemberId())))
                .declareVar(ObjectArrayEventBean.class, "evalEvent", newInstance(ObjectArrayEventBean.class, newArrayByLength(Object.class, constant(1)), member(evalTypeMember.getMemberId())))
                .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(forge.streamNumLambda), ref("resultEvent"))
                .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(forge.streamNumLambda + 1), ref("evalEvent"))
                .declareVar(Object[].class, "resultProps", exprDotMethod(ref("resultEvent"), "getProperties"))
                .declareVar(Object[].class, "evalProps", exprDotMethod(ref("evalEvent"), "getProperties"));
        block.forEach(Object.class, "next", EnumForgeCodegenNames.REF_ENUMCOLL)
                .assignArrayElement("resultProps", constant(0), ref("value"))
                .assignArrayElement("evalProps", constant(0), ref("next"))
                .assignRef("value", forge.innerExpression.evaluateCodegen(innerEvalType, methodNode, scope, codegenClassScope))
                .blockEnd();
        block.methodReturn(ref("value"));
        return localMethod(methodNode, args.getEps(), args.getEnumcoll(), args.getIsNewData(), args.getExprCtx());
    }
}
