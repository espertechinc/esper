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
package com.espertech.esper.common.internal.epl.resultset.select.eval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectEvalInsertNoWildcardObjectArrayRemapWWiden implements SelectExprProcessorForge {

    protected final SelectExprForgeContext context;
    protected final EventType resultEventType;
    protected final int[] remapped;
    protected final TypeWidenerSPI[] wideners;

    public SelectEvalInsertNoWildcardObjectArrayRemapWWiden(SelectExprForgeContext context, EventType resultEventType, int[] remapped, TypeWidenerSPI[] wideners) {
        this.context = context;
        this.resultEventType = resultEventType;
        this.remapped = remapped;
        this.wideners = wideners;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public CodegenMethod processCodegen(CodegenExpression resultEventTypeExpr, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return processCodegen(resultEventTypeExpr, eventBeanFactory, codegenMethodScope, exprSymbol, codegenClassScope, context.getExprForges(), resultEventType.getPropertyNames(), remapped, wideners);
    }

    public static CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, ExprForge[] forges, String[] propertyNames, int[] remapped, TypeWidenerSPI[] optionalWideners) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, SelectEvalInsertNoWildcardObjectArrayRemapWWiden.class, codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(Object[].class, "result", newArrayByLength(Object.class, constant(propertyNames.length)));
        for (int i = 0; i < forges.length; i++) {
            CodegenExpression value;
            if (optionalWideners != null && optionalWideners[i] != null) {
                value = forges[i].evaluateCodegen(forges[i].getEvaluationType(), methodNode, exprSymbol, codegenClassScope);
                value = optionalWideners[i].widenCodegen(value, codegenMethodScope, codegenClassScope);
            } else {
                value = forges[i].evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope);
            }
            block.assignArrayElement(ref("result"), constant(remapped[i]), value);
        }
        block.methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedObjectArray", ref("result"), resultEventType));
        return methodNode;
    }
}
