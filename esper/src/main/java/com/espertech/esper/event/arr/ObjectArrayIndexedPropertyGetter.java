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

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.castUnderlying;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethodTakingExprAndConst;

/**
 * Getter for a dynamic indexed property for maps.
 */
public class ObjectArrayIndexedPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;
    private final int index;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param array array
     * @param propertyIndex prop index
     * @param index index
     * @return value
     * @throws PropertyAccessException exception
     */
    public static Object getObjectArrayIndexValue(Object[] array, int propertyIndex, int index) throws PropertyAccessException {
        Object value = array[propertyIndex];
        return BaseNestableEventUtil.getBNArrayValueAtIndexWithNullCheck(value, index);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param array array
     * @param propertyIndex prop index
     * @param index index
     * @return value
     * @throws PropertyAccessException exception
     */
    public static boolean isObjectArrayExistsProperty(Object[] array, int propertyIndex, int index) throws PropertyAccessException {
        Object value = array[propertyIndex];
        return BaseNestableEventUtil.isExistsIndexedValue(value, index);
    }

    /**
     * Ctor.
     *
     * @param propertyIndex property index
     * @param index         index to get the element at
     */
    public ObjectArrayIndexedPropertyGetter(int propertyIndex, int index) {
        this.propertyIndex = propertyIndex;
        this.index = index;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        return getObjectArrayIndexValue(array, propertyIndex, index);
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return isObjectArrayExistsProperty(array, propertyIndex, index);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getObjectArray(BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return isObjectArrayExistsProperty(BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean));
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingExists(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return staticMethodTakingExprAndConst(this.getClass(), "getObjectArrayIndexValue", underlyingExpression, propertyIndex, index);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return staticMethodTakingExprAndConst(this.getClass(), "isObjectArrayExistsProperty", underlyingExpression, propertyIndex, index);
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantNull();
    }
}
