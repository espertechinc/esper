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
 * A getter for use with Map-based events simply returns the value for the key.
 */
public class ObjectArrayEventBeanPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;

    /**
     * Ctor.
     *
     * @param propertyIndex property to get
     */
    public ObjectArrayEventBeanPropertyGetter(int propertyIndex) {
        this.propertyIndex = propertyIndex;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        Object eventBean = array[propertyIndex];
        if (eventBean == null) {
            return null;
        }

        EventBean theEvent = (EventBean) eventBean;
        return theEvent.getUnderlying();
    }

    private String getObjectArrayCodegen(CodegenContext context) {
        return context.addMethod(Object.class, Object[].class, "array", this.getClass())
                .declareVar(Object.class, "eventBean", arrayAtIndex(ref("array"), constant(propertyIndex)))
                .ifRefNullReturnNull("eventBean")
                .methodReturn(exprDotUnderlying(cast(EventBean.class, ref("eventBean"))));
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object get(EventBean obj) {
        return getObjectArray(BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean obj) {
        return BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj)[propertyIndex];
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
        return arrayAtIndex(underlyingExpression, constant(propertyIndex));
    }
}
