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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.event.bean.core.DynamicPropertyDescriptorByField;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.util.PropertyUtility;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for a dynamic indexed property (syntax field.indexed[0]?), using vanilla reflection.
 */
public class DynamicIndexedPropertyGetterByField extends DynamicPropertyGetterByFieldBase {
    private final String fieldName;
    private final int index;

    public DynamicIndexedPropertyGetterByField(String fieldName, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory);
        this.fieldName = fieldName;
        this.index = index;
    }

    protected Field determineField(Class clazz) {
        return dynamicIndexPropertyDetermineField(clazz, fieldName);
    }

    protected CodegenExpression determineFieldCodegen(CodegenExpressionRef clazz, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        return staticMethod(DynamicIndexedPropertyGetterByField.class, "dynamicIndexPropertyDetermineField", clazz, constant(fieldName));
    }

    protected Object call(DynamicPropertyDescriptorByField descriptor, Object underlying) {
        return dynamicIndexedPropertyGet(descriptor, underlying, index);
    }

    protected CodegenExpression callCodegen(CodegenExpressionRef desc, CodegenExpressionRef object, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        return staticMethod(DynamicIndexedPropertyGetterByField.class, "dynamicIndexedPropertyGet", desc, object, constant(index));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        DynamicPropertyDescriptorByField desc = getPopulateCache(cache, this, eventBean.getUnderlying(), eventBeanTypedEventFactory);
        if (desc.getField() == null) {
            return false;
        }
        return dynamicIndexedPropertyExists(desc, eventBean.getUnderlying(), index);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenExpression memberCache = codegenClassScope.addOrGetFieldSharable(sharableCode);
        CodegenMethod method = parent.makeChild(boolean.class, DynamicPropertyGetterByMethodBase.class, codegenClassScope).addParam(Object.class, "object");
        method.getBlock()
            .declareVar(DynamicPropertyDescriptorByField.class, "desc", getPopulateCacheCodegen(memberCache, ref("object"), method, codegenClassScope))
            .ifCondition(equalsNull(exprDotMethod(ref("desc"), "getField"))).blockReturn(constantFalse())
            .methodReturn(staticMethod(DynamicIndexedPropertyGetterByField.class, "dynamicIndexedPropertyExists", ref("desc"), ref("object"), constant(index)));
        return localMethod(method, underlyingExpression);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param clazz     class
     * @param fieldName field
     * @return null or field
     */
    public static Field dynamicIndexPropertyDetermineField(Class clazz, String fieldName) {
        try {
            Field field = clazz.getField(fieldName);
            if (!field.getType().isArray()) {
                return null;
            }
            return field;
        } catch (NoSuchFieldException ex1) {
            return null;
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
    public static Object dynamicIndexedPropertyGet(DynamicPropertyDescriptorByField descriptor, Object underlying, int index) {
        try {
            Object array = descriptor.getField().get(underlying);
            if (array == null) {
                return null;
            }
            if (Array.getLength(array) <= index) {
                return null;
            }
            return Array.get(array, index);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(descriptor.getField(), underlying, e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(descriptor.getField(), e);
        } catch (IllegalAccessException e) {
            throw PropertyUtility.getIllegalAccessException(descriptor.getField(), e);
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
    public static boolean dynamicIndexedPropertyExists(DynamicPropertyDescriptorByField descriptor, Object underlying, int index) {
        try {
            Object array = descriptor.getField().get(underlying);
            if (array == null) {
                return false;
            }
            if (Array.getLength(array) <= index) {
                return false;
            }
            return true;
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(descriptor.getField(), underlying, e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(descriptor.getField(), e);
        } catch (IllegalAccessException e) {
            throw PropertyUtility.getIllegalAccessException(descriptor.getField(), e);
        }
    }
}
