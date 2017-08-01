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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.blocks.CodegenLegoCast;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class DTLocalBeanCalOpsEval implements DTLocalEvaluator {
    private final DTLocalBeanCalOpsForge forge;
    private final DTLocalEvaluator inner;

    public DTLocalBeanCalOpsEval(DTLocalBeanCalOpsForge forge, DTLocalEvaluator inner) {
        this.forge = forge;
        this.inner = inner;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object timestamp = forge.getter.get((EventBean) target);
        if (timestamp == null) {
            return null;
        }
        return inner.evaluate(timestamp, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalBeanCalOpsForge forge, CodegenExpression inner, Class innerType, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(forge.innerReturnType, DTLocalBeanCalOpsEval.class).add(EventBean.class, "target").add(params).begin()
                .declareVar(forge.getterReturnType, "timestamp", CodegenLegoCast.castSafeFromObjectType(forge.getterReturnType, forge.getter.eventBeanGetCodegen(ref("target"), context)));
        if (!forge.getterReturnType.isPrimitive()) {
            block.ifRefNullReturnNull("timestamp");
        }
        String method = block.methodReturn(forge.inner.codegen(ref("timestamp"), forge.getterReturnType, params, context));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }
}
