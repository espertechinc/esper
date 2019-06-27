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
package com.espertech.esper.common.internal.event.json.getter.provided;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.json.getter.core.JsonGetterIndexedBase;

import java.lang.reflect.Field;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Property getter for Json underlying fields.
 */
public final class JsonGetterIndexedProvided extends JsonGetterIndexedBase {
    private final Field field;

    public JsonGetterIndexedProvided(int index, String underlyingClassName, EventType optionalInnerType, EventBeanTypedEventFactory eventBeanTypedEventFactory, Field field) {
        super(index, underlyingClassName, optionalInnerType, eventBeanTypedEventFactory);
        this.field = field;
    }

    public String getFieldName() {
        return field.getName();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (optionalInnerType == null) {
            return constantNull();
        }
        CodegenExpression factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpression eventType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(optionalInnerType, EPStatementInitServices.REF));
        return staticMethod(JsonFieldGetterHelperProvided.class, "handleJsonProvidedCreateFragmentIndexed", exprDotName(underlyingExpression, field.getName()), constant(index), eventType, factory);
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        return JsonFieldGetterHelperProvided.getJsonProvidedIndexedProp(object, field, index);
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        return JsonFieldGetterHelperProvided.getJsonProvidedIndexedPropExists(object, field, index);
    }

    public Object getJsonFragment(Object object) throws PropertyAccessException {
        if (optionalInnerType == null) {
            return null;
        }
        Object value = JsonFieldGetterHelperProvided.getJsonProvidedIndexedProp(object, field, index);
        if (value == null) {
            return null;
        }
        return eventBeanTypedEventFactory.adapterForTypedJson(value, optionalInnerType);
    }
}
