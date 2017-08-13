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
package com.espertech.esper.epl.datetime.dtlocal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.blocks.CodegenLegoCast;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class DTLocalBeanIntervalWithEndEval implements DTLocalEvaluator {
    private final EventPropertyGetter getterStartTimestamp;
    private final EventPropertyGetter getterEndTimestamp;
    private final DTLocalEvaluatorIntervalComp inner;

    public DTLocalBeanIntervalWithEndEval(EventPropertyGetter getterStartTimestamp, EventPropertyGetter getterEndTimestamp, DTLocalEvaluatorIntervalComp inner) {
        this.getterStartTimestamp = getterStartTimestamp;
        this.getterEndTimestamp = getterEndTimestamp;
        this.inner = inner;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object start = getterStartTimestamp.get((EventBean) target);
        if (start == null) {
            return null;
        }
        Object end = getterEndTimestamp.get((EventBean) target);
        if (end == null) {
            return null;
        }
        return inner.evaluate(start, end, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalBeanIntervalWithEndForge forge, CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(Boolean.class, DTLocalBeanIntervalWithEndEval.class).add(EventBean.class, "target").add(params).begin();
        block.declareVar(forge.getterStartReturnType, "start", CodegenLegoCast.castSafeFromObjectType(forge.getterStartReturnType, forge.getterStartTimestamp.eventBeanGetCodegen(ref("target"), context)));
        if (!forge.getterStartReturnType.isPrimitive()) {
            block.ifRefNullReturnNull("start");
        }
        block.declareVar(forge.getterEndReturnType, "end", CodegenLegoCast.castSafeFromObjectType(forge.getterEndReturnType, forge.getterEndTimestamp.eventBeanGetCodegen(ref("target"), context)));
        if (!forge.getterEndReturnType.isPrimitive()) {
            block.ifRefNullReturnNull("end");
        }
        CodegenMethodId method = block.methodReturn(forge.inner.codegen(ref("start"), ref("end"), params, context));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }
}
