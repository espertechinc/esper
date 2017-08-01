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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventAdapterService;

import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class SelectExprProcessorTypableMapEval implements ExprEvaluator {
    private final SelectExprProcessorTypableMapForge forge;
    private final ExprEvaluator innerEvaluator;

    public SelectExprProcessorTypableMapEval(SelectExprProcessorTypableMapForge forge, ExprEvaluator innerEvaluator) {
        this.forge = forge;
        this.innerEvaluator = innerEvaluator;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Map<String, Object> values = (Map<String, Object>) innerEvaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (values == null) {
            return forge.eventAdapterService.adapterForTypedMap(Collections.<String, Object>emptyMap(), forge.mapType);
        }
        return forge.eventAdapterService.adapterForTypedMap(values, forge.mapType);
    }

    public static CodegenExpression codegen(SelectExprProcessorTypableMapForge forge, CodegenContext context, CodegenParamSetExprPremade params) {
        CodegenMember eventAdapterService = context.makeAddMember(EventAdapterService.class, forge.eventAdapterService);
        CodegenMember mapType = context.makeAddMember(EventType.class, forge.mapType);
        String method = context.addMethod(EventBean.class, SelectExprProcessorTypableMapEval.class).add(params).begin()
                .declareVar(Map.class, "values", forge.innerForge.evaluateCodegen(params, context))
                .declareVarNoInit(Map.class, "map")
                .ifRefNull("values")
                .assignRef("values", staticMethod(Collections.class, "emptyMap"))
                .blockEnd()
                .methodReturn(exprDotMethod(ref(eventAdapterService.getMemberName()), "adapterForTypedMap", ref("values"), ref(mapType.getMemberName())));
        return localMethodBuild(method).passAll(params).call();
    }

}
