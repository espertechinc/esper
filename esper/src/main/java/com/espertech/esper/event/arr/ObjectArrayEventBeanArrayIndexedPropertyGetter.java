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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.base.CodegenMethodNode;
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

    private CodegenMethodNode getObjectArrayCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Object[].class, "array").getBlock()
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

    private CodegenMethodNode getFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Object[].class, "array").getBlock()
                .declareVar(EventBean[].class, "wrapper", cast(EventBean[].class, arrayAtIndex(ref("array"), constant(propertyIndex))))
                .methodReturn(staticMethod(BaseNestableEventUtil.class, "getBNArrayPropertyBean", ref("wrapper"), constant(index)));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(Object[].class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(Object[].class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getObjectArrayCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getFragmentCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }
}