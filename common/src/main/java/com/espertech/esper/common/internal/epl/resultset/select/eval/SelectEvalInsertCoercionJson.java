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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectEvalInsertCoercionJson implements SelectExprProcessorForge {

    private final JsonEventType source;
    private final JsonEventType target;

    public SelectEvalInsertCoercionJson(JsonEventType source, JsonEventType target) {
        this.source = source;
        this.target = target;
    }

    public EventType getResultEventType() {
        return target;
    }

    public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        methodNode.getBlock()
            .declareVar(source.getDetail().getUnderlyingClassName(), "src", castUnderlying(source.getDetail().getUnderlyingClassName(), arrayAtIndex(refEPS, constant(0))))
            .declareVar(target.getDetail().getUnderlyingClassName(), "und", newInstance(target.getDetail().getUnderlyingClassName()));
        for (Map.Entry<String, JsonUnderlyingField> entryTarget : target.getDetail().getFieldDescriptors().entrySet()) {
            JsonUnderlyingField src = source.getDetail().getFieldDescriptors().get(entryTarget.getKey());
            if (src == null) {
                continue;
            }
            methodNode.getBlock().assignRef("und." + entryTarget.getValue().getFieldName(), ref("src." + src.getFieldName()));
        }
        methodNode.getBlock().methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedJson", ref("und"), resultEventType));
        return methodNode;
    }
}
