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
package com.espertech.esper.common.internal.epl.updatehelper;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.core.EventBeanCopyMethod;
import com.espertech.esper.common.internal.event.core.EventBeanCopyMethodForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;
import static com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode.instblock;

public class EventBeanUpdateHelperForge {
    private final EventType eventType;
    private final EventBeanCopyMethodForge copyMethod;
    private final EventBeanUpdateItemForge[] updateItems;

    public EventBeanUpdateHelperForge(EventType eventType, EventBeanCopyMethodForge copyMethod, EventBeanUpdateItemForge[] updateItems) {
        this.eventType = eventType;
        this.copyMethod = copyMethod;
        this.updateItems = updateItems;
    }

    public CodegenExpression makeWCopy(CodegenMethodScope scope, CodegenClassScope classScope) {
        CodegenExpressionField copyMethodField = classScope.addFieldUnshared(true, EventBeanCopyMethod.class, copyMethod.makeCopyMethodClassScoped(classScope));

        CodegenMethod method = scope.makeChild(EventBeanUpdateHelperWCopy.class, this.getClass(), classScope);
        CodegenMethod updateInternal = makeUpdateInternal(method, classScope);

        CodegenExpressionNewAnonymousClass clazz = newAnonymousClass(method.getBlock(), EventBeanUpdateHelperWCopy.class);
        CodegenMethod updateWCopy = CodegenMethod.makeParentNode(EventBean.class, this.getClass(), classScope)
                .addParam(EventBean.class, "matchingEvent")
                .addParam(EventBean[].class, NAME_EPS)
                .addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        clazz.addMethod("updateWCopy", updateWCopy);

        updateWCopy.getBlock()
                .apply(instblock(classScope, "qInfraUpdate", ref("matchingEvent"), REF_EPS, constant(updateItems.length), constantTrue()))
                .declareVar(EventBean.class, "copy", exprDotMethod(copyMethodField, "copy", ref("matchingEvent")))
                .assignArrayElement(REF_EPS, constant(0), ref("copy"))
                .assignArrayElement(REF_EPS, constant(2), ref("matchingEvent"))
                .localMethod(updateInternal, REF_EPS, REF_EXPREVALCONTEXT, ref("copy"))
                .apply(instblock(classScope, "aInfraUpdate", ref("copy")))
                .methodReturn(ref("copy"));

        method.getBlock().methodReturn(clazz);

        return localMethod(method);
    }

    public CodegenExpression makeNoCopy(CodegenMethodScope scope, CodegenClassScope classScope) {
        CodegenMethod method = scope.makeChild(EventBeanUpdateHelperNoCopy.class, this.getClass(), classScope);
        CodegenMethod updateInternal = makeUpdateInternal(method, classScope);

        CodegenExpressionNewAnonymousClass clazz = newAnonymousClass(method.getBlock(), EventBeanUpdateHelperNoCopy.class);

        CodegenMethod updateNoCopy = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope)
                .addParam(EventBean.class, "matchingEvent")
                .addParam(EventBean[].class, NAME_EPS)
                .addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        clazz.addMethod("updateNoCopy", updateNoCopy);
        updateNoCopy.getBlock()
                .apply(instblock(classScope, "qInfraUpdate", ref("matchingEvent"), REF_EPS, constant(updateItems.length), constantFalse()))
                .localMethod(updateInternal, REF_EPS, REF_EXPREVALCONTEXT, ref("matchingEvent"))
                .apply(instblock(classScope, "aInfraUpdate", ref("matchingEvent")));

        CodegenMethod getUpdatedProperties = CodegenMethod.makeParentNode(String[].class, this.getClass(), classScope);
        clazz.addMethod("getUpdatedProperties", getUpdatedProperties);
        getUpdatedProperties.getBlock().methodReturn(constant(getUpdateItemsPropertyNames()));

        CodegenMethod isRequiresStream2InitialValueEvent = CodegenMethod.makeParentNode(boolean.class, this.getClass(), classScope);
        clazz.addMethod("isRequiresStream2InitialValueEvent", isRequiresStream2InitialValueEvent);
        isRequiresStream2InitialValueEvent.getBlock().methodReturn(constant(isRequiresStream2InitialValueEvent()));

        method.getBlock().methodReturn(clazz);

        return localMethod(method);
    }

    public EventBeanUpdateItemForge[] getUpdateItems() {
        return updateItems;
    }

    private CodegenMethod makeUpdateInternal(CodegenMethodScope scope, CodegenClassScope classScope) {
        CodegenMethod method = scope.makeChildWithScope(void.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EventBean[].class, NAME_EPS)
                .addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT)
                .addParam(EventBean.class, "target");

        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, true);
        CodegenMethod exprMethod = method.makeChildWithScope(void.class, CodegenLegoMethodExpression.class, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);
        CodegenExpression[] expressions = new CodegenExpression[updateItems.length];
        Class[] types = new Class[updateItems.length];
        for (int i = 0; i < updateItems.length; i++) {
            types[i] = updateItems[i].getExpression().getEvaluationType();
            expressions[i] = updateItems[i].getExpression().evaluateCodegen(types[i], exprMethod, exprSymbol, classScope);
        }

        exprSymbol.derivedSymbolsCodegen(method, method.getBlock(), classScope);

        method.getBlock().declareVar(eventType.getUnderlyingType(), "und", cast(eventType.getUnderlyingType(), exprDotUnderlying(ref("target"))));

        for (int i = 0; i < updateItems.length; i++) {
            EventBeanUpdateItemForge updateItem = updateItems[i];
            method.getBlock().apply(instblock(classScope, "qInfraUpdateRHSExpr", constant(i)));

            if (types[i] == null && updateItem.getOptionalWriter() != null) {
                method.getBlock().expression(updateItem.getOptionalWriter().writeCodegen(constantNull(), ref("und"), ref("target"), method, classScope));
                continue;
            }

            if (types[i] == void.class || updateItem.getOptionalWriter() == null) {
                method.getBlock()
                        .expression(expressions[i])
                        .apply(instblock(classScope, "aInfraUpdateRHSExpr", constantNull()));
                continue;
            }

            CodegenExpressionRef ref = ref("r" + i);
            method.getBlock().declareVar(types[i], ref.getRef(), expressions[i]);

            CodegenExpression assigned = ref;
            if (updateItem.getOptionalWidener() != null) {
                assigned = updateItem.getOptionalWidener().widenCodegen(ref, method, classScope);
            }

            if (!types[i].isPrimitive() && updateItem.isNotNullableField()) {
                method.getBlock()
                        .ifNull(ref)
                        .staticMethod(EventBeanUpdateHelperForge.class, "logWarnWhenNullAndNotNullable", constant(updateItem.getOptionalPropertyName()))
                        .ifElse()
                        .expression(updateItem.getOptionalWriter().writeCodegen(assigned, ref("und"), ref("target"), method, classScope))
                        .blockEnd();
            } else {
                method.getBlock().expression(updateItem.getOptionalWriter().writeCodegen(assigned, ref("und"), ref("target"), method, classScope));
            }

            method.getBlock().apply(instblock(classScope, "aInfraUpdateRHSExpr", assigned));
        }

        return method;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param propertyName name
     */
    public static void logWarnWhenNullAndNotNullable(String propertyName) {
        log.warn("Null value returned by expression for assignment to property '" + propertyName + " is ignored as the property type is not nullable for expression");
    }

    private static final Logger log = LoggerFactory.getLogger(EventBeanUpdateHelperForge.class);

    public boolean isRequiresStream2InitialValueEvent() {
        return copyMethod != null;
    }

    public String[] getUpdateItemsPropertyNames() {
        List<String> properties = new ArrayList<>();
        for (EventBeanUpdateItemForge item : updateItems) {
            if (item.getOptionalPropertyName() != null) {
                properties.add(item.getOptionalPropertyName());
            }
        }
        return properties.toArray(new String[properties.size()]);
    }
}
