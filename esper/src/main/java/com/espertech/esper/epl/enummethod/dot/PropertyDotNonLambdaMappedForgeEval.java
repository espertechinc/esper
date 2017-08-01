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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class PropertyDotNonLambdaMappedForgeEval implements ExprEvaluator {

    private final PropertyDotNonLambdaMappedForge forge;
    private final ExprEvaluator paramEval;

    public PropertyDotNonLambdaMappedForgeEval(PropertyDotNonLambdaMappedForge forge, ExprEvaluator paramEval) {
        this.forge = forge;
        this.paramEval = paramEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean event = eventsPerStream[forge.getStreamId()];
        if (event == null) {
            return null;
        }
        String key = (String) paramEval.evaluate(eventsPerStream, isNewData, context);
        return forge.getMappedGetter().get(event, key);
    }

    public static CodegenExpression codegen(PropertyDotNonLambdaMappedForge forge, CodegenContext context, CodegenParamSetExprPremade params) {
        String method = context.addMethod(forge.getEvaluationType(), PropertyDotNonLambdaMappedForgeEval.class).add(params).begin()
                .declareVar(EventBean.class, "event", arrayAtIndex(params.passEPS(), constant(forge.getStreamId())))
                .ifRefNullReturnNull("event")
                .declareVar(String.class, "key", forge.getParamForge().evaluateCodegen(params, context))
                .methodReturn(forge.getMappedGetter().eventBeanGetMappedCodegen(context, ref("event"), ref("key")));
        return localMethodBuild(method).passAll(params).call();
    }

}
