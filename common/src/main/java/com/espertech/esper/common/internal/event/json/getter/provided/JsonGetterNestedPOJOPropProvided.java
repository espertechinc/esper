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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.getter.BaseNativePropertyGetter;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.json.getter.core.JsonEventPropertyGetter;

import java.lang.reflect.Field;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.event.json.getter.provided.JsonFieldGetterHelperProvided.getJsonProvidedSimpleProp;

public class JsonGetterNestedPOJOPropProvided extends BaseNativePropertyGetter implements JsonEventPropertyGetter {
    private final Field field;
    private final BeanEventPropertyGetter nestedGetter;

    public JsonGetterNestedPOJOPropProvided(EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class returnType, Class genericType, Field field, BeanEventPropertyGetter nestedGetter) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory, returnType, genericType);
        this.field = field;
        this.nestedGetter = nestedGetter;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(field.getDeclaringClass(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getFieldCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(field.getDeclaringClass(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getFieldExistsCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return getJsonExists(eventBean.getUnderlying());
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getJsonProp(eventBean.getUnderlying());
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        Object nested = getJsonProvidedSimpleProp(object, field);
        if (nested == null) {
            return null;
        }
        return nestedGetter.getBeanProp(nested);
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        Object nested = getJsonProvidedSimpleProp(object, field);
        if (nested == null) {
            return false;
        }
        return nestedGetter.isBeanExistsProperty(nested);
    }

    public Object getJsonFragment(Object object) throws PropertyAccessException {
        return null;
    }

    public Class getTargetType() {
        return field.getDeclaringClass();
    }

    public Class getBeanPropType() {
        return Object.class;
    }

    private CodegenMethod getFieldCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(field.getDeclaringClass(), "und").getBlock()
            .declareVar(Object.class, "value", ref("und." + field.getName()))
            .ifRefNullReturnNull("value")
            .methodReturn(nestedGetter.underlyingGetCodegen(castRef(nestedGetter.getTargetType(), "value"), codegenMethodScope, codegenClassScope));
    }

    private CodegenMethod getFieldExistsCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(field.getDeclaringClass(), "und").getBlock()
            .declareVar(Object.class, "value", ref("und." + field.getName()))
            .ifRefNullReturnFalse("value")
            .methodReturn(nestedGetter.underlyingExistsCodegen(castRef(nestedGetter.getTargetType(), "value"), codegenMethodScope, codegenClassScope));
    }
}
