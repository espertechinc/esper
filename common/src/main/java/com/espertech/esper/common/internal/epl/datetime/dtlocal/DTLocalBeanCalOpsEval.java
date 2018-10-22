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
package com.espertech.esper.common.internal.epl.datetime.dtlocal;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

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

    public static CodegenExpression codegen(DTLocalBeanCalOpsForge forge, CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(forge.innerReturnType, DTLocalBeanCalOpsEval.class, codegenClassScope).addParam(EventBean.class, "target");

        methodNode.getBlock().declareVar(forge.getterReturnType, "timestamp", CodegenLegoCast.castSafeFromObjectType(forge.getterReturnType, forge.getter.eventBeanGetCodegen(ref("target"), methodNode, codegenClassScope)));
        if (!forge.getterReturnType.isPrimitive()) {
            methodNode.getBlock().ifRefNullReturnNull("timestamp");
        }
        methodNode.getBlock().methodReturn(forge.inner.codegen(ref("timestamp"), forge.getterReturnType, methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, inner);
    }
}
