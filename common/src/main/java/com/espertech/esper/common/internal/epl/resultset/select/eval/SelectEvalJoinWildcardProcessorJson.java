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
import com.espertech.esper.common.client.json.minimaljson.Json;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Processor for select-clause expressions that handles wildcards. Computes results based on matching events.
 */
public class SelectEvalJoinWildcardProcessorJson implements SelectExprProcessorForge {
    private final String[] streamNames;
    private final JsonEventType resultEventType;

    public SelectEvalJoinWildcardProcessorJson(String[] streamNames, JsonEventType resultEventType) {
        this.streamNames = streamNames;
        this.resultEventType = resultEventType;
    }

    public CodegenMethod processCodegen(CodegenExpression resultEventTypeOuter, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        // NOTE: Maintaining result-event-type as out own field as we may be an "inner" select-expr-processor
        CodegenExpressionField mType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(resultEventType, EPStatementInitServices.REF));
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        methodNode.getBlock()
                .declareVar(resultEventType.getUnderlyingType(), "tuple", newInstance(resultEventType.getDetail().getUnderlyingClassName()));
        for (int i = 0; i < streamNames.length; i++) {
            CodegenExpression event = arrayAtIndex(refEPS, constant(i));
            JsonUnderlyingField field = resultEventType.getDetail().getFieldDescriptors().get(streamNames[i]);
            CodegenExpression rhs = cast(field.getPropertyType(), exprDotUnderlying(event));
            methodNode.getBlock().assignRef(exprDotName(ref("tuple"), field.getFieldName()), rhs);
        }
        methodNode.getBlock().methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedJson", ref("tuple"), mType));
        return methodNode;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }
}
