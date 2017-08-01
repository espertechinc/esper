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
package com.espertech.esper.event.arr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.BaseNestableEventUtil;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for a dynamic mappeds property for maps.
 */
public class ObjectArrayMappedPropertyGetter implements ObjectArrayEventPropertyGetterAndMapped {
    private final int propertyIndex;
    private final String key;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param objectArray   data
     * @param propertyIndex prop index
     * @param providedKey   key
     * @return value
     * @throws PropertyAccessException exception
     */
    public static Object getOAMapValue(Object[] objectArray, int propertyIndex, String providedKey) throws PropertyAccessException {
        Object value = objectArray[propertyIndex];
        return BaseNestableEventUtil.getMappedPropertyValue(value, providedKey);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param objectArray   data
     * @param propertyIndex prop index
     * @param providedKey   key
     * @return value
     * @throws PropertyAccessException exception
     */
    public static boolean getOAMapExists(Object[] objectArray, int propertyIndex, String providedKey) throws PropertyAccessException {
        Object value = objectArray[propertyIndex];
        return BaseNestableEventUtil.getMappedPropertyExists(value, providedKey);
    }

    /**
     * Ctor.
     *
     * @param propertyIndex property index
     * @param key           get the element at
     */
    public ObjectArrayMappedPropertyGetter(int propertyIndex, String key) {
        this.propertyIndex = propertyIndex;
        this.key = key;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        return getOAMapValue(array, propertyIndex, key);
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return getOAMapExists(array, propertyIndex, key);
    }

    public Object get(EventBean eventBean, String mapKey) throws PropertyAccessException {
        Object[] data = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return getOAMapValue(data, propertyIndex, mapKey);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object[] data = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return getOAMapValue(data, propertyIndex, key);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Object[] data = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return getOAMapExists(data, propertyIndex, key);
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingGetCodegen(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingExistsCodegen(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return staticMethod(this.getClass(), "getOAMapValue", underlyingExpression, constant(propertyIndex), constant(key));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return staticMethod(this.getClass(), "getOAMapExists", underlyingExpression, constant(propertyIndex), constant(key));
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression eventBeanGetMappedCodegen(CodegenContext context, CodegenExpression beanExpression, CodegenExpression key) {
        return staticMethod(this.getClass(), "getOAMapValue", castUnderlying(Object[].class, beanExpression), constant(propertyIndex), key);
    }
}
