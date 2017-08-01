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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
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

    public static CodegenExpression codegen(EnumAggregateScalarForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenMember resultTypeMember = context.makeAddMember(ObjectArrayEventType.class, forge.resultEventType);
        CodegenMember evalTypeMember = context.makeAddMember(ObjectArrayEventType.class, forge.evalEventType);
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        CodegenBlock block = context.addMethod(forge.initialization.getEvaluationType(), EnumAggregateScalarForgeEval.class).add(premade).begin()
                .declareVar(forge.initialization.getEvaluationType(), "value", forge.initialization.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context))
                .ifCondition(exprDotMethod(premade.enumcoll(), "isEmpty"))
                .blockReturn(ref("value"))
                .declareVar(ObjectArrayEventBean.class, "resultEvent", newInstance(ObjectArrayEventBean.class, newArray(Object.class, constant(1)), ref(resultTypeMember.getMemberName())))
                .declareVar(ObjectArrayEventBean.class, "evalEvent", newInstance(ObjectArrayEventBean.class, newArray(Object.class, constant(1)), ref(evalTypeMember.getMemberName())))
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda), ref("resultEvent"))
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda + 1), ref("evalEvent"))
                .declareVar(Object[].class, "resultProps", exprDotMethod(ref("resultEvent"), "getProperties"))
                .declareVar(Object[].class, "evalProps", exprDotMethod(ref("evalEvent"), "getProperties"));
        block.forEach(Object.class, "next", premade.enumcoll())
                .assignArrayElement("resultProps", constant(0), ref("value"))
                .assignArrayElement("evalProps", constant(0), ref("next"))
                .assignRef("value", forge.innerExpression.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context))
                .blockEnd();
        return localMethodBuild(block.methodReturn(ref("value"))).passAll(args).call();
    }
}
