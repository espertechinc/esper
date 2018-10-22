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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

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

    public CodegenExpression codegen(CodegenExpression target, Class targetType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(returnType, DTLocalBeanReformatForge.class, codegenClassScope).addParam(EventBean.class, "target");

        CodegenBlock block = methodNode.getBlock()
                .declareVar(getterResultType, "timestamp", getter.eventBeanGetCodegen(ref("target"), methodNode, codegenClassScope));
        if (!getterResultType.isPrimitive()) {
            block.ifRefNullReturnNull("timestamp");
        }
        block.methodReturn(inner.codegen(ref("timestamp"), getterResultType, methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, target);
    }
}
