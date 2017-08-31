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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
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

    public static CodegenExpression codegen(PropertyDotNonLambdaIndexedForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(forge.getEvaluationType(), PropertyDotNonLambdaIndexedForgeEval.class, codegenClassScope);

        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        Class evaluationType = forge.getParamForge().getEvaluationType();
        methodNode.getBlock()
                .declareVar(EventBean.class, "event", arrayAtIndex(refEPS, constant(forge.getStreamId())))
                .ifRefNullReturnNull("event")
                .declareVar(evaluationType, "key", forge.getParamForge().evaluateCodegen(evaluationType, methodNode, exprSymbol, codegenClassScope))
                .methodReturn(forge.getIndexedGetter().eventBeanGetIndexedCodegen(methodNode, codegenClassScope, ref("event"), ref("key")));
        return localMethod(methodNode);
    }
}
