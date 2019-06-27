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
package com.espertech.esper.common.internal.event.bean.getter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.event.bean.core.DynamicPropertyDescriptorByField;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

import java.lang.reflect.Field;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

/**
 * Getter for a dynamic property (syntax field.inner?), using vanilla reflection.
 */
public class DynamicSimplePropertyGetterByField extends DynamicPropertyGetterByFieldBase {
    private final String fieldName;

    public DynamicSimplePropertyGetterByField(String fieldName, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory);
        this.fieldName = fieldName;
    }

    protected Field determineField(Class clazz) {
        return dynamicSimplePropertyDetermineField(fieldName, clazz);
    }

    protected CodegenExpression determineFieldCodegen(CodegenExpressionRef clazz, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        return staticMethod(this.getClass(), "dynamicSimplePropertyDetermineField", constant(fieldName), clazz);
    }

    protected Object call(DynamicPropertyDescriptorByField descriptor, Object underlying) {
        return dynamicSimplePropertyCall(descriptor, underlying);
    }

    protected CodegenExpression callCodegen(CodegenExpressionRef desc, CodegenExpressionRef object, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        return staticMethod(this.getClass(), "dynamicSimplePropertyCall", desc, object);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return cacheAndExistsCodegen(underlyingExpression, codegenMethodScope, codegenClassScope);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return cacheAndExists(cache, this, eventBean.getUnderlying(), eventBeanTypedEventFactory);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param fieldName name
     * @param clazz            class
     * @return method or null
     */
    public static Field dynamicSimplePropertyDetermineField(String fieldName, Class clazz) {
        try {
            return clazz.getField(fieldName);
        } catch (NoSuchFieldException ex1) {
            return null;
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param descriptor desc
     * @param underlying underlying
     * @return value
     */
    public static Object dynamicSimplePropertyCall(DynamicPropertyDescriptorByField descriptor, Object underlying) {
        try {
            return descriptor.getField().get(underlying);
        } catch (Throwable t) {
            throw DynamicPropertyGetterByFieldBase.handleException(descriptor, underlying, t);
        }
    }
}
