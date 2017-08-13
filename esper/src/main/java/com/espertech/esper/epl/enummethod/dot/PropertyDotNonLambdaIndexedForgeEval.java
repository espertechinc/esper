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
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class PropertyDotNonLambdaIndexedForgeEval implements ExprEvaluator {

    private final PropertyDotNonLambdaIndexedForge forge;
    private final ExprEvaluator paramEval;

    public PropertyDotNonLambdaIndexedForgeEval(PropertyDotNonLambdaIndexedForge forge, ExprEvaluator paramEval) {
        this.forge = forge;
        this.paramEval = paramEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean event = eventsPerStream[forge.getStreamId()];
        if (event == null) {
            return null;
        }
        Integer key = (Integer) paramEval.evaluate(eventsPerStream, isNewData, context);
        return forge.getIndexedGetter().get(event, key);
    }

    public static CodegenExpression codegen(PropertyDotNonLambdaIndexedForge forge, CodegenContext context, CodegenParamSetExprPremade params) {
        CodegenMethodId method = context.addMethod(forge.getEvaluationType(), PropertyDotNonLambdaIndexedForgeEval.class).add(params).begin()
                .declareVar(EventBean.class, "event", arrayAtIndex(params.passEPS(), constant(forge.getStreamId())))
                .ifRefNullReturnNull("event")
                .declareVar(forge.getParamForge().getEvaluationType(), "key", forge.getParamForge().evaluateCodegen(params, context))
                .methodReturn(forge.getIndexedGetter().eventBeanGetIndexedCodegen(context, ref("event"), ref("key")));
        return localMethodBuild(method).passAll(params).call();

    }
}
