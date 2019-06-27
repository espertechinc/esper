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
import com.espertech.esper.common.internal.event.core.TypeBeanOrUnderlying;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Map;

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
        methodNode.getBlock().declareVar(jsonEventType.getUnderlyingType(), "und", newInstance(jsonEventType.getDetail().getUnderlyingClassName()));
        for (int i = 0; i < selectContext.getColumnNames().length; i++) {
            String columnName = selectContext.getColumnNames()[i];
            Class fieldClassBoxed = JavaClassHelper.getBoxedType(jsonEventType.getDetail().getFieldDescriptors().get(columnName).getPropertyType());
            Object propertyType = jsonEventType.getTypes().get(columnName);
            Class evalType = selectContext.getExprForges()[i].getEvaluationType();
            JsonUnderlyingField field = jsonEventType.getDetail().getFieldDescriptors().get(selectContext.getColumnNames()[i]);
            CodegenExpression rhs = null;

            // handle
            if (evalType == EventBean.class) {
                CodegenMethod conversion = methodNode.makeChild(fieldClassBoxed, this.getClass(), codegenClassScope).addParam(Object.class, "value");
                conversion.getBlock()
                    .ifRefNullReturnNull("value")
                    .methodReturn(cast(fieldClassBoxed, exprDotMethod(cast(EventBean.class, ref("value")), "getUnderlying")));
                rhs = localMethod(conversion, CodegenLegoMayVoid.expressionMayVoid(EventBean.class, selectContext.getExprForges()[i], methodNode, exprSymbol, codegenClassScope));
            }
            else if (propertyType instanceof Class) {
                rhs = CodegenLegoMayVoid.expressionMayVoid(fieldClassBoxed, selectContext.getExprForges()[i], methodNode, exprSymbol, codegenClassScope);
            } else if (propertyType instanceof TypeBeanOrUnderlying) {
                Class underlyingType = ((TypeBeanOrUnderlying) propertyType).getEventType().getUnderlyingType();
                CodegenMethod conversion = methodNode.makeChild(underlyingType, this.getClass(), codegenClassScope).addParam(Object.class, "value");
                conversion.getBlock()
                    .ifRefNullReturnNull("value")
                    .ifInstanceOf("value", EventBean.class).blockReturn(cast(underlyingType, exprDotUnderlying(cast(EventBean.class, ref("value")))))
                    .methodReturn(cast(underlyingType, ref("value")));
                rhs = localMethod(conversion, CodegenLegoMayVoid.expressionMayVoid(Object.class, selectContext.getExprForges()[i], methodNode, exprSymbol, codegenClassScope));
            } else if (propertyType instanceof TypeBeanOrUnderlying[]) {
                Class underlyingType = ((TypeBeanOrUnderlying[]) propertyType)[0].getEventType().getUnderlyingType();
                Class underlyingArrayType = JavaClassHelper.getArrayType(underlyingType);
                CodegenMethod conversion = methodNode.makeChild(underlyingArrayType, this.getClass(), codegenClassScope).addParam(Object.class, "value");
                conversion.getBlock()
                    .ifRefNullReturnNull("value")
                    .ifInstanceOf("value", underlyingArrayType).blockReturn(cast(underlyingArrayType, ref("value")))
                    .declareVar(EventBean[].class, "events", cast(EventBean[].class, ref("value")))
                    .declareVar(underlyingArrayType, "array", newArrayByLength(underlyingType, arrayLength(ref("events"))))
                    .forLoopIntSimple("i", arrayLength(ref("events")))
                    .assignArrayElement("array", ref("i"), cast(underlyingType, exprDotUnderlying(arrayAtIndex(ref("events"), ref("i")))))
                    .blockEnd()
                    .methodReturn(ref("array"));
                rhs = localMethod(conversion, CodegenLegoMayVoid.expressionMayVoid(Object.class, selectContext.getExprForges()[i], methodNode, exprSymbol, codegenClassScope));
            } else if (propertyType == null) {
                methodNode.getBlock().expression(CodegenLegoMayVoid.expressionMayVoid(Object.class, selectContext.getExprForges()[i], methodNode, exprSymbol, codegenClassScope));
            } else {
                throw new UnsupportedOperationException("Unrecognized property ");
            }

            if (rhs != null) {
                if (field.getPropertyType().isPrimitive()) {
                    String tmp = "result_" + i;
                    methodNode.getBlock()
                        .declareVar(fieldClassBoxed, tmp, rhs)
                        .ifRefNotNull(tmp)
                        .assignRef(exprDotName(ref("und"), field.getFieldName()), ref(tmp))
                        .blockEnd();
                }
                else {
                    methodNode.getBlock().assignRef(exprDotName(ref("und"), field.getFieldName()), rhs);
                }
            }
        }
        methodNode.getBlock().methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedJson", ref("und"), resultEventType));
        return methodNode;
    }

    public EventType getResultEventType() {
        return jsonEventType;
    }
}
