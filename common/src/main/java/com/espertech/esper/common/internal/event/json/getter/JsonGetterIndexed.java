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
package com.espertech.esper.common.internal.event.json.getter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.util.CollectionUtil;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Property getter for Json underlying fields.
 */
public final class JsonGetterIndexed implements JsonEventPropertyGetter {
    private final JsonUnderlyingField field;
    private final int index;
    private final String underlyingClassName;
    private final EventType optionalInnerType;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

    public JsonGetterIndexed(JsonUnderlyingField field, int index, String underlyingClassName, EventType optionalInnerType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.field = field;
        this.index = index;
        this.underlyingClassName = underlyingClassName;
        this.optionalInnerType = optionalInnerType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return staticMethod(CollectionUtil.class, "arrayValueAtIndex", exprDotName(underlyingExpression, field.getFieldName()), constant(index));
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return staticMethod(CollectionUtil.class, "arrayExistsAtIndex", exprDotName(underlyingExpression, field.getFieldName()), constant(index));
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (optionalInnerType == null) {
            return constantNull();
        }
        CodegenExpression factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpression eventType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(optionalInnerType, EPStatementInitServices.REF));
        return staticMethod(JsonFieldGetterHelper.class, "handleJsonCreateFragmentIndexed", underlyingExpression, constant(field.getPropertyNumber()), constant(index), eventType, factory);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return getJsonExists(eventBean.getUnderlying());
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return getJsonFragment(eventBean.getUnderlying());
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getJsonProp(eventBean.getUnderlying());
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        return JsonFieldGetterHelper.getJsonIndexedProp(object, field.getPropertyNumber(), index);
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        return JsonFieldGetterHelper.getJsonIndexedPropExists(object, field, index);
    }

    public Object getJsonFragment(Object object) throws PropertyAccessException {
        if (optionalInnerType == null) {
            return null;
        }
        Object value = JsonFieldGetterHelper.getJsonIndexedProp(object, field.getPropertyNumber(), index);
        if (value == null) {
            return null;
        }
        return eventBeanTypedEventFactory.adapterForTypedJson(value, optionalInnerType);
    }
}
