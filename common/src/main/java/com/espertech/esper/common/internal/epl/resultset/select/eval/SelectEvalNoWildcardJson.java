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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMayVoid;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectEvalNoWildcardJson implements SelectExprProcessorForge {

    private final SelectExprForgeContext selectContext;
    private final JsonEventType jsonEventType;

    public SelectEvalNoWildcardJson(SelectExprForgeContext selectContext, JsonEventType jsonEventType) {
        this.selectContext = selectContext;
        this.jsonEventType = jsonEventType;
    }

    public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
        methodNode.getBlock().declareVar(JsonEventObjectBase.class, "und", newInstance(jsonEventType.getDetail().getUnderlyingClassName()));
        for (int i = 0; i < selectContext.getColumnNames().length; i++) {
            int num = jsonEventType.getColumnNumber(selectContext.getColumnNames()[i]);
            CodegenExpression expression = CodegenLegoMayVoid.expressionMayVoid(Object.class, selectContext.getExprForges()[i], methodNode, exprSymbol, codegenClassScope);
            methodNode.getBlock().expression(exprDotMethod(ref("und"), "setNativeValue", constant(num), expression));
        }
        methodNode.getBlock().methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedJson", ref("und"), resultEventType));
        return methodNode;
    }

    public EventType getResultEventType() {
        return jsonEventType;
    }
}
