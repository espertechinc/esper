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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

public abstract class SelectEvalBaseFirstProp implements SelectExprProcessorForge {

    private final SelectExprForgeContext selectExprForgeContext;
    private final EventType resultEventType;

    public SelectEvalBaseFirstProp(SelectExprForgeContext selectExprForgeContext, EventType resultEventType) {
        this.selectExprForgeContext = selectExprForgeContext;
        this.resultEventType = resultEventType;
    }

    protected abstract CodegenExpression processFirstColCodegen(EPTypeClass evaluationType, CodegenExpression expression, CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope);

    public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprForge first = selectExprForgeContext.getExprForges()[0];
        EPType evaluationType = first.getEvaluationType();
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPE, this.getClass(), codegenClassScope);
        if (evaluationType == EPTypeNull.INSTANCE) {
            methodNode.getBlock().methodReturn(constantNull());
        } else {
            methodNode.getBlock().methodReturn(processFirstColCodegen((EPTypeClass) evaluationType, first.evaluateCodegen((EPTypeClass) evaluationType, methodNode, exprSymbol, codegenClassScope), resultEventType, eventBeanFactory, methodNode, codegenClassScope));
        }
        return methodNode;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }
}
