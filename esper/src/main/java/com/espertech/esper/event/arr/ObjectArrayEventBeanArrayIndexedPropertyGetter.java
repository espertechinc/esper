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
 * Getter for array events.
 */
public class ObjectArrayEventBeanArrayIndexedPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;
    private final int index;

    /**
     * Ctor.
     *
     * @param propertyIndex property index
     * @param index         array index
     */
    public ObjectArrayEventBeanArrayIndexedPropertyGetter(int propertyIndex, int index) {
        this.propertyIndex = propertyIndex;
        this.index = index;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        EventBean[] wrapper = (EventBean[]) array[propertyIndex];
        return BaseNestableEventUtil.getBNArrayPropertyUnderlying(wrapper, index);
    }

    private String getObjectArrayCodegen(CodegenContext context) {
        return context.addMethod(Object.class, Object[].class, "array", this.getClass())
                .declareVar(EventBean[].class, "wrapper", cast(EventBean[].class, arrayAtIndex(ref("array"), constant(propertyIndex))))
                .methodReturn(staticMethod(BaseNestableEventUtil.class, "getBNArrayPropertyUnderlying", ref("wrapper"), constant(index)));
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return true;
    }

    public Object get(EventBean obj) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj);
        return getObjectArray(array);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean obj) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj);
        EventBean[] wrapper = (EventBean[]) array[propertyIndex];
        return BaseNestableEventUtil.getBNArrayPropertyBean(wrapper, index);
    }

    private String getFragmentCodegen(CodegenContext context) {
        return context.addMethod(Object.class, Object[].class, "array", this.getClass())
                .declareVar(EventBean[].class, "wrapper", cast(EventBean[].class, arrayAtIndex(ref("array"), constant(propertyIndex))))
                .methodReturn(staticMethod(BaseNestableEventUtil.class, "getBNArrayPropertyBean", ref("wrapper"), constant(index)));
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingFragment(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getObjectArrayCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getFragmentCodegen(context), underlyingExpression);
    }
}