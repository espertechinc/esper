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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.event.bean.core.DynamicPropertyDescriptorByMethod;
import com.espertech.esper.common.internal.event.bean.core.PropertyHelper;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.util.PropertyUtility;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;

/**
 * Getter for a dynamic indexed property (syntax field.indexed[0]?), using vanilla reflection.
 */
public class DynamicIndexedPropertyGetterByMethod extends DynamicPropertyGetterByMethodBase {
    private final String getterMethodName;
    private final Object[] parameters;
    private final int index;

    public DynamicIndexedPropertyGetterByMethod(String fieldName, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory);
        getterMethodName = PropertyHelper.getGetterMethodName(fieldName);
        this.parameters = new Object[]{index};
        this.index = index;
    }

    protected Method determineMethod(Class clazz) {
        return dynamicIndexPropertyDetermineMethod(clazz, getterMethodName);
    }

    protected CodegenExpression determineMethodCodegen(CodegenExpressionRef clazz, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        return staticMethod(DynamicIndexedPropertyGetterByMethod.class, "dynamicIndexPropertyDetermineMethod", clazz, constant(getterMethodName));
    }

    protected Object call(DynamicPropertyDescriptorByMethod descriptor, Object underlying) {
        return dynamicIndexedPropertyGet(descriptor, underlying, parameters, index);
    }

    protected CodegenExpression callCodegen(CodegenExpressionRef desc, CodegenExpressionRef object, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenExpressionField params = codegenClassScope.addFieldUnshared(true, Object[].class, constant(parameters));
        return staticMethod(DynamicIndexedPropertyGetterByMethod.class, "dynamicIndexedPropertyGet", desc, object, params, constant(index));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        DynamicPropertyDescriptorByMethod desc = getPopulateCache(cache, this, eventBean.getUnderlying(), eventBeanTypedEventFactory);
        if (desc.getMethod() == null) {
            return false;
        }
        return dynamicIndexedPropertyExists(desc, eventBean.getUnderlying(), index);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenExpression memberCache = codegenClassScope.addOrGetFieldSharable(sharableCode);
        CodegenMethod method = parent.makeChild(boolean.class, DynamicPropertyGetterByMethodBase.class, codegenClassScope).addParam(Object.class, "object");
        method.getBlock()
            .declareVar(DynamicPropertyDescriptorByMethod.class, "desc", getPopulateCacheCodegen(memberCache, ref("object"), method, codegenClassScope))
            .ifCondition(equalsNull(exprDotMethod(ref("desc"), "getMethod"))).blockReturn(constantFalse())
            .methodReturn(staticMethod(DynamicIndexedPropertyGetterByMethod.class, "dynamicIndexedPropertyExists", ref("desc"), ref("object"), constant(index)));
        return localMethod(method, underlyingExpression);
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
    public static Object dynamicIndexedPropertyGet(DynamicPropertyDescriptorByMethod descriptor, Object underlying, Object[] parameters, int index) {
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

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param descriptor descriptor
     * @param underlying target
     * @param index      idx
     * @return null or method
     */
    public static boolean dynamicIndexedPropertyExists(DynamicPropertyDescriptorByMethod descriptor, Object underlying, int index) {
        try {
            if (descriptor.isHasParameters()) {
                return true;
            } else {
                Object array = descriptor.getMethod().invoke(underlying, null);
                if (array == null) {
                    return false;
                }
                if (Array.getLength(array) <= index) {
                    return false;
                }
                return true;
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
