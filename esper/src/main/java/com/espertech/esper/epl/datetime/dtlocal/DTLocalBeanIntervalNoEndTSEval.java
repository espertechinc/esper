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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class DTLocalBeanIntervalNoEndTSEval implements DTLocalEvaluator {
    private final EventPropertyGetter getter;
    private final DTLocalEvaluator inner;

    public DTLocalBeanIntervalNoEndTSEval(EventPropertyGetter getter, DTLocalEvaluator inner) {
        this.getter = getter;
        this.inner = inner;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object timestamp = getter.get((EventBean) target);
        if (timestamp == null) {
            return null;
        }
        return inner.evaluate(timestamp, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalBeanIntervalNoEndTSForge forge, CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(forge.returnType, DTLocalBeanIntervalNoEndTSEval.class, codegenClassScope).addParam(EventBean.class, "target");

        methodNode.getBlock()
                .declareVar(forge.getterResultType, "timestamp", CodegenLegoCast.castSafeFromObjectType(forge.getterResultType, forge.getter.eventBeanGetCodegen(ref("target"), methodNode, codegenClassScope)))
                .ifRefNullReturnNull("timestamp")
                .methodReturn(forge.inner.codegen(ref("timestamp"), forge.getterResultType, methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, inner);
    }
}
