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
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.BaseNestableEventUtil;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Returns the event bean or the underlying array.
 */
public class ObjectArrayEventBeanArrayPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;
    private final Class underlyingType;

    /**
     * Ctor.
     *
     * @param propertyIndex  property to get
     * @param underlyingType type of property
     */
    public ObjectArrayEventBeanArrayPropertyGetter(int propertyIndex, Class underlyingType) {
        this.propertyIndex = propertyIndex;
        this.underlyingType = underlyingType;
    }

    public Object getObjectArray(Object[] oa) throws PropertyAccessException {
        Object inner = oa[propertyIndex];
        return BaseNestableEventUtil.getArrayPropertyAsUnderlyingsArray(underlyingType, (EventBean[]) inner);
    }

    private CodegenMethodId getObjectArrayCodegen(CodegenContext context) {
        return context.addMethod(Object.class, this.getClass()).add(Object[].class, "oa").begin()
                .declareVar(Object.class, "inner", arrayAtIndex(ref("oa"), constant(propertyIndex)))
                .methodReturn(localMethod(BaseNestableEventUtil.getArrayPropertyAsUnderlyingsArrayCodegen(underlyingType, context), cast(EventBean[].class, ref("inner"))));
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return true; // Property exists as the property is not dynamic (unchecked)
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
        return array[propertyIndex];
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingGetCodegen(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingFragmentCodegen(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getObjectArrayCodegen(context), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return arrayAtIndex(underlyingExpression, constant(propertyIndex));
    }
}
