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
package com.espertech.esper.common.internal.event.json.getter.fromschema;

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
import com.espertech.esper.common.internal.event.json.getter.core.JsonGetterIndexedBase;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public final class JsonGetterIndexedSchema extends JsonGetterIndexedBase {
    private final JsonUnderlyingField field;

    public JsonGetterIndexedSchema(int index, String underlyingClassName, EventType optionalInnerType, EventBeanTypedEventFactory eventBeanTypedEventFactory, JsonUnderlyingField field) {
        super(index, underlyingClassName, optionalInnerType, eventBeanTypedEventFactory);
        this.field = field;
    }

    public String getFieldName() {
        return field.getFieldName();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (optionalInnerType == null) {
            return constantNull();
        }
        CodegenExpression factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpression eventType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(optionalInnerType, EPStatementInitServices.REF));
        return staticMethod(JsonFieldGetterHelperSchema.class, "handleJsonCreateFragmentIndexed", underlyingExpression, constant(field.getPropertyNumber()), constant(index), eventType, factory);
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        return JsonFieldGetterHelperSchema.getJsonIndexedProp(object, field.getPropertyNumber(), index);
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        return JsonFieldGetterHelperSchema.getJsonIndexedPropExists(object, field, index);
    }

    public Object getJsonFragment(Object object) throws PropertyAccessException {
        if (optionalInnerType == null) {
            return null;
        }
        Object value = JsonFieldGetterHelperSchema.getJsonIndexedProp(object, field.getPropertyNumber(), index);
        if (value == null) {
            return null;
        }
        return eventBeanTypedEventFactory.adapterForTypedJson(value, optionalInnerType);
    }
}
