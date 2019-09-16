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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.event.bean.core.DynamicPropertyDescriptorByField;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.util.PropertyUtility;

import java.lang.reflect.Field;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.util.CollectionUtil.getMapKeyExistsChecked;
import static com.espertech.esper.common.internal.util.CollectionUtil.getMapValueChecked;

/**
 * Getter for a dynamic mapped property (syntax field.mapped('key')?), using vanilla reflection.
 */
public class DynamicMappedPropertyGetterByField extends DynamicPropertyGetterByFieldBase {
    private final String fieldName;
    private final String key;

    public DynamicMappedPropertyGetterByField(String fieldName, String key, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory);
        this.fieldName = fieldName;
        this.key = key;
    }

    protected Field determineField(Class clazz) {
        return dynamicMapperPropertyDetermineField(clazz, fieldName);
    }

    protected CodegenExpression determineFieldCodegen(CodegenExpressionRef clazz, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        return staticMethod(DynamicMappedPropertyGetterByField.class, "dynamicMapperPropertyDetermineField", clazz, constant(fieldName));
    }

    protected Object call(DynamicPropertyDescriptorByField descriptor, Object underlying) {
        return dynamicMappedPropertyGet(descriptor, underlying, key);
    }

    protected CodegenExpression callCodegen(CodegenExpressionRef desc, CodegenExpressionRef object, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        return staticMethod(DynamicMappedPropertyGetterByField.class, "dynamicMappedPropertyGet", desc, object, constant(key));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenExpression memberCache = codegenClassScope.addOrGetFieldSharable(sharableCode);
        CodegenMethod method = parent.makeChild(boolean.class, DynamicPropertyGetterByMethodBase.class, codegenClassScope).addParam(Object.class, "object");
        method.getBlock()
            .declareVar(DynamicPropertyDescriptorByField.class, "desc", getPopulateCacheCodegen(memberCache, ref("object"), method, codegenClassScope))
            .ifCondition(equalsNull(exprDotMethod(ref("desc"), "getField"))).blockReturn(constantFalse())
            .methodReturn(staticMethod(DynamicMappedPropertyGetterByField.class, "dynamicMappedPropertyExists", ref("desc"), ref("object"), constant(key)));
        return localMethod(method, underlyingExpression);

    }

    public boolean isExistsProperty(EventBean eventBean) {
        DynamicPropertyDescriptorByField desc = getPopulateCache(cache, this, eventBean.getUnderlying(), eventBeanTypedEventFactory);
        if (desc.getField() == null) {
            return false;
        }
        return dynamicMappedPropertyExists(desc, eventBean.getUnderlying(), key);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param clazz     class
     * @param fieldName method
     * @return value
     * @throws PropertyAccessException for access ex
     */
    public static Field dynamicMapperPropertyDetermineField(Class clazz, String fieldName) throws PropertyAccessException {
        try {
            Field field = clazz.getField(fieldName);
            if (field.getType() != Map.class) {
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
     * @param key key
     * @return value
     */
    public static Object dynamicMappedPropertyGet(DynamicPropertyDescriptorByField descriptor, Object underlying, String key) {
        try {
            Object result = descriptor.getField().get(underlying);
            return getMapValueChecked(result, key);
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
     * @param key        key
     * @return value
     */
    public static boolean dynamicMappedPropertyExists(DynamicPropertyDescriptorByField descriptor, Object underlying, String key) {
        try {
            Object result = descriptor.getField().get(underlying);
            return getMapKeyExistsChecked(result, key);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(descriptor.getField(), underlying, e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(descriptor.getField(), e);
        } catch (IllegalAccessException e) {
            throw PropertyUtility.getIllegalAccessException(descriptor.getField(), e);
        }
    }
}
