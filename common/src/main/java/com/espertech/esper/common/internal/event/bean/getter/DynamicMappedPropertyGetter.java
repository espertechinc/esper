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
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.event.bean.core.DynamicPropertyDescriptor;
import com.espertech.esper.common.internal.event.bean.core.PropertyHelper;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.util.PropertyUtility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for a dynamic mapped property (syntax field.mapped('key')?), using vanilla reflection.
 */
public class DynamicMappedPropertyGetter extends DynamicPropertyGetterBase {
    private final String getterMethodName;
    private final Object[] parameters;

    public DynamicMappedPropertyGetter(String fieldName, String key, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory);
        getterMethodName = PropertyHelper.getGetterMethodName(fieldName);
        this.parameters = new Object[]{key};
    }

    public Method determineMethod(Class clazz) throws PropertyAccessException {
        return dynamicMapperPropertyDetermineMethod(clazz, getterMethodName);
    }

    protected CodegenExpression determineMethodCodegen(CodegenExpressionRef clazz, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        return staticMethod(DynamicMappedPropertyGetter.class, "dynamicMapperPropertyDetermineMethod", clazz, constant(getterMethodName));
    }

    protected Object call(DynamicPropertyDescriptor descriptor, Object underlying) {
        return dynamicMappedPropertyGet(descriptor, underlying, parameters);
    }

    protected CodegenExpression callCodegen(CodegenExpressionRef desc, CodegenExpressionRef object, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenExpressionField params = codegenClassScope.addFieldUnshared(true, Object[].class, constant(parameters));
        return staticMethod(DynamicMappedPropertyGetter.class, "dynamicMappedPropertyGet", desc, object, params);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenExpression memberCache = codegenClassScope.addOrGetFieldSharable(sharableCode);
        CodegenMethod method = parent.makeChild(boolean.class, DynamicPropertyGetterBase.class, codegenClassScope).addParam(Object.class, "object");
        method.getBlock()
            .declareVar(DynamicPropertyDescriptor.class, "desc", getPopulateCacheCodegen(memberCache, ref("object"), method, codegenClassScope))
            .ifCondition(equalsNull(exprDotMethod(ref("desc"), "getMethod"))).blockReturn(constantFalse())
            .methodReturn(staticMethod(DynamicMappedPropertyGetter.class, "dynamicMappedPropertyExists", ref("desc"), ref("object"), constant(parameters[0])));
        return localMethod(method, underlyingExpression);

    }

    public boolean isExistsProperty(EventBean eventBean) {
        DynamicPropertyDescriptor desc = getPopulateCache(cache, this, eventBean.getUnderlying(), eventBeanTypedEventFactory);
        if (desc.getMethod() == null) {
            return false;
        }
        return dynamicMappedPropertyExists(desc, eventBean.getUnderlying(), (String) parameters[0]);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param clazz            class
     * @param getterMethodName method
     * @return value
     * @throws PropertyAccessException for access ex
     */
    public static Method dynamicMapperPropertyDetermineMethod(Class clazz, String getterMethodName) throws PropertyAccessException {
        try {
            return clazz.getMethod(getterMethodName, String.class);
        } catch (NoSuchMethodException ex1) {
            Method method;
            try {
                method = clazz.getMethod(getterMethodName);
            } catch (NoSuchMethodException e) {
                return null;
            }

            if (method.getReturnType() != Map.class) {
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
     * @return value
     */
    public static Object dynamicMappedPropertyGet(DynamicPropertyDescriptor descriptor, Object underlying, Object[] parameters) {
        try {
            if (descriptor.isHasParameters()) {
                return descriptor.getMethod().invoke(underlying, parameters);
            } else {
                Object result = descriptor.getMethod().invoke(underlying, null);
                if ((result instanceof Map) && (result != null)) {
                    Map map = (Map) result;
                    return map.get(parameters[0]);
                }
                return null;
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

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param descriptor descriptor
     * @param underlying target
     * @param key key
     * @return value
     */
    public static boolean dynamicMappedPropertyExists(DynamicPropertyDescriptor descriptor, Object underlying, String key) {
        try {
            if (descriptor.isHasParameters()) {
                return true;
            } else {
                Object result = descriptor.getMethod().invoke(underlying, null);
                if ((result instanceof Map) && (result != null)) {
                    Map map = (Map) result;
                    return map.containsKey(key);
                }
                return false;
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
