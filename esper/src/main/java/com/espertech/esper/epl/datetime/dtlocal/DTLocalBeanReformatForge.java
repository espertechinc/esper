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
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.event.EventPropertyGetterSPI;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class DTLocalBeanReformatForge implements DTLocalForge {
    private final EventPropertyGetterSPI getter;
    private final Class getterResultType;
    private final DTLocalForge inner;
    private final Class returnType;

    public DTLocalBeanReformatForge(EventPropertyGetterSPI getter, Class getterResultType, DTLocalForge inner, Class returnType) {
        this.getter = getter;
        this.getterResultType = getterResultType;
        this.inner = inner;
        this.returnType = returnType;
    }

    public DTLocalEvaluator getDTEvaluator() {
        return new DTLocalBeanReformatEval(getter, inner.getDTEvaluator());
    }

    public CodegenExpression codegen(CodegenExpression target, Class targetType, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(returnType, DTLocalBeanReformatForge.class).add(EventBean.class, "target").add(params).begin()
                .declareVar(getterResultType, "timestamp", getter.eventBeanGetCodegen(ref("target"), context));
        if (!getterResultType.isPrimitive()) {
            block.ifRefNullReturnNull("timestamp");
        }
        CodegenMethodId method = block.methodReturn(inner.codegen(ref("timestamp"), getterResultType, params, context));
        return localMethodBuild(method).pass(target).passAll(params).call();
    }
}
