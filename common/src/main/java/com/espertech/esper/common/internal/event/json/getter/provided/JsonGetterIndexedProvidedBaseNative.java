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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.bean.getter.BaseNativePropertyGetter;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.json.getter.core.JsonEventPropertyGetter;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.lang.reflect.Field;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Property getter for Json underlying fields.
 */
public final class JsonGetterIndexedProvidedBaseNative extends BaseNativePropertyGetter implements JsonEventPropertyGetter {
    private final Field field;
    private final int index;

    public JsonGetterIndexedProvidedBaseNative(EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class returnType, Field field, int index) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory, returnType, null);
        this.field = field;
        this.index = index;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(field.getDeclaringClass(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return staticMethod(CollectionUtil.class, "arrayValueAtIndex", exprDotName(underlyingExpression, field.getName()), constant(index));
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(field.getDeclaringClass(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return staticMethod(CollectionUtil.class, "arrayExistsAtIndex", exprDotName(underlyingExpression, field.getName()), constant(index));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return getJsonExists(eventBean.getUnderlying());
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getJsonProp(eventBean.getUnderlying());
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        Object value = JsonFieldGetterHelperProvided.getJsonProvidedSimpleProp(object, field);
        return CollectionUtil.arrayValueAtIndex(value, index);
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        return JsonFieldGetterHelperProvided.getJsonProvidedIndexedPropExists(object, field, index);
    }

    public Object getJsonFragment(Object object) throws PropertyAccessException {
        if (!isFragmentable) {
            return null;
        }
        Object value = JsonFieldGetterHelperProvided.getJsonProvidedIndexedProp(object, field, index);
        if (value == null) {
            return null;
        }
        return getFragmentFromValue(value);
    }

    public Class getTargetType() {
        return field.getDeclaringClass();
    }

    public Class getBeanPropType() {
        return Object.class;
    }
}
