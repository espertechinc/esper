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
import com.espertech.esper.common.internal.event.bean.core.DynamicPropertyDescriptorByMethod;
import com.espertech.esper.common.internal.event.bean.core.PropertyHelper;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

import java.lang.reflect.Method;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

/**
 * Getter for a dynamic property (syntax field.inner?), using vanilla reflection.
 */
public class DynamicSimplePropertyGetterByMethod extends DynamicPropertyGetterByMethodBase {
    private final String getterMethodName;
    private final String isMethodName;

    public DynamicSimplePropertyGetterByMethod(String fieldName, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory);
        getterMethodName = PropertyHelper.getGetterMethodName(fieldName);
        isMethodName = PropertyHelper.getIsMethodName(fieldName);
    }

    protected Object call(DynamicPropertyDescriptorByMethod descriptor, Object underlying) {
        return dynamicSimplePropertyCall(descriptor, underlying);
    }

    protected CodegenExpression callCodegen(CodegenExpressionRef desc, CodegenExpressionRef object, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        return staticMethod(this.getClass(), "dynamicSimplePropertyCall", desc, object);
    }

    protected Method determineMethod(Class clazz) {
        return dynamicSimplePropertyDetermineMethod(getterMethodName, isMethodName, clazz);
    }

    protected CodegenExpression determineMethodCodegen(CodegenExpressionRef clazz, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        return staticMethod(this.getClass(), "dynamicSimplePropertyDetermineMethod", constant(getterMethodName), constant(isMethodName), clazz);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return cacheAndExists(cache, this, eventBean.getUnderlying(), eventBeanTypedEventFactory);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return cacheAndExistsCodegen(underlyingExpression, codegenMethodScope, codegenClassScope);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param descriptor desc
     * @param underlying underlying
     * @return value
     */
    public static Object dynamicSimplePropertyCall(DynamicPropertyDescriptorByMethod descriptor, Object underlying) {
        try {
            return descriptor.getMethod().invoke(underlying, null);
        } catch (Throwable t) {
            throw DynamicPropertyGetterByMethodBase.handleException(descriptor, underlying, t);
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param getterMethodName getter
     * @param isMethodName     is-method
     * @param clazz            class
     * @return method or null
     */
    public static Method dynamicSimplePropertyDetermineMethod(String getterMethodName, String isMethodName, Class clazz) {
        try {
            return clazz.getMethod(getterMethodName);
        } catch (NoSuchMethodException ex1) {
            try {
                return clazz.getMethod(isMethodName);
            } catch (NoSuchMethodException ex2) {
                return null;
            }
        }
    }
}
