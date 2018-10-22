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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.event.bean.core.DynamicPropertyDescriptor;
import com.espertech.esper.common.internal.event.bean.core.PropertyHelper;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.util.PropertyUtility;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

/**
 * Getter for a dynamic indexed property (syntax field.indexed[0]?), using vanilla reflection.
 */
public class DynamicIndexedPropertyGetter extends DynamicPropertyGetterBase {
    private final String getterMethodName;
    private final Object[] parameters;
    private final int index;

    public DynamicIndexedPropertyGetter(String fieldName, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory);
        getterMethodName = PropertyHelper.getGetterMethodName(fieldName);
        this.parameters = new Object[]{index};
        this.index = index;
    }

    protected Method determineMethod(Class clazz) {
        return dynamicIndexPropertyDetermineMethod(clazz, getterMethodName);
    }

    protected CodegenExpression determineMethodCodegen(CodegenExpressionRef clazz, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        return staticMethod(DynamicIndexedPropertyGetter.class, "dynamicIndexPropertyDetermineMethod", clazz, constant(getterMethodName));
    }

    protected Object call(DynamicPropertyDescriptor descriptor, Object underlying) {
        return dynamicIndexedPropertyGet(descriptor, underlying, parameters, index);
    }

    protected CodegenExpression callCodegen(CodegenExpressionRef desc, CodegenExpressionRef object, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenExpressionField params = codegenClassScope.addFieldUnshared(true, Object[].class, constant(parameters));
        return staticMethod(DynamicIndexedPropertyGetter.class, "dynamicIndexedPropertyGet", desc, object, params, constant(index));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param clazz            class
     * @param getterMethodName method
     * @return null or method
     */
    public static Method dynamicIndexPropertyDetermineMethod(Class clazz, String getterMethodName) {
        Method method;

        try {
            return clazz.getMethod(getterMethodName, int.class);
        } catch (NoSuchMethodException ex1) {
            try {
                method = clazz.getMethod(getterMethodName);
            } catch (NoSuchMethodException e) {
                return null;
            }
            if (!method.getReturnType().isArray()) {
                return null;
            }
            return method;
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param descriptor descriptor
     * @param underlying target
     * @param parameters params
     * @param index      idx
     * @return null or method
     */
    public static Object dynamicIndexedPropertyGet(DynamicPropertyDescriptor descriptor, Object underlying, Object[] parameters, int index) {
        try {
            if (descriptor.isHasParameters()) {
                return descriptor.getMethod().invoke(underlying, parameters);
            } else {
                Object array = descriptor.getMethod().invoke(underlying, null);
                if (array == null) {
                    return null;
                }
                if (Array.getLength(array) <= index) {
                    return null;
                }
                return Array.get(array, index);
            }
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(descriptor.getMethod(), underlying, e);
        } catch (InvocationTargetException e) {
            throw PropertyUtility.getInvocationTargetException(descriptor.getMethod(), e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(descriptor.getMethod(), e);
        } catch (IllegalAccessException e) {
            throw PropertyUtility.getIllegalAccessException(descriptor.getMethod(), e);
        }
    }

}
