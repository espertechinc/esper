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
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EnumToMapScalarLambdaForgeEval implements EnumEval {

    private final EnumToMapScalarLambdaForge forge;
    private final ExprEvaluator innerExpression;
    private final ExprEvaluator secondExpression;

    public EnumToMapScalarLambdaForgeEval(EnumToMapScalarLambdaForge forge, ExprEvaluator innerExpression, ExprEvaluator secondExpression) {
        this.forge = forge;
        this.innerExpression = innerExpression;
        this.secondExpression = secondExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        if (enumcoll.isEmpty()) {
            return Collections.emptyMap();
        }

        Map map = new HashMap();
        ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[1], forge.resultEventType);
        eventsLambda[forge.streamNumLambda] = resultEvent;
        Object[] props = resultEvent.getProperties();

        Collection<Object> values = (Collection<Object>) enumcoll;
        for (Object next : values) {

            props[0] = next;

            Object key = innerExpression.evaluate(eventsLambda, isNewData, context);
            Object value = secondExpression.evaluate(eventsLambda, isNewData, context);
            map.put(key, value);
        }

        return map;
    }

    public static CodegenExpression codegen(EnumToMapScalarLambdaForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenMember resultTypeMember = context.makeAddMember(ObjectArrayEventType.class, forge.resultEventType);
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        CodegenBlock block = context.addMethod(Map.class, EnumToMapScalarLambdaForgeEval.class).add(premade).begin()
                .ifCondition(exprDotMethod(premade.enumcoll(), "isEmpty"))
                .blockReturn(staticMethod(Collections.class, "emptyMap"))
                .declareVar(Map.class, "map", newInstance(HashMap.class))
                .declareVar(ObjectArrayEventBean.class, "resultEvent", newInstance(ObjectArrayEventBean.class, newArray(Object.class, constant(1)), member(resultTypeMember.getMemberId())))
                .assignArrayElement(premade.eps(), constant(forge.streamNumLambda), ref("resultEvent"))
                .declareVar(Object[].class, "props", exprDotMethod(ref("resultEvent"), "getProperties"));
        CodegenBlock forEach = block.forEach(Object.class, "next", premade.enumcoll())
                .assignArrayElement("props", constant(0), ref("next"))
                .declareVar(Object.class, "key", forge.innerExpression.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context))
                .declareVar(Object.class, "value", forge.secondExpression.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context))
                .expression(exprDotMethod(ref("map"), "put", ref("key"), ref("value")));
        CodegenMethodId method = block.methodReturn(ref("map"));
        return localMethodBuild(method).passAll(args).call();
    }
}
