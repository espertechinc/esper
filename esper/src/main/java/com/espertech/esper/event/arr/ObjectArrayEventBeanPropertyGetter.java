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
 * A getter for use with Map-based events simply returns the value for the key.
 */
public class ObjectArrayEventBeanPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;
    private final Class underlyingType;

    public ObjectArrayEventBeanPropertyGetter(int propertyIndex, Class underlyingType) {
        this.propertyIndex = propertyIndex;
        this.underlyingType = underlyingType;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        Object eventBean = array[propertyIndex];
        if (eventBean == null) {
            return null;
        }

        EventBean theEvent = (EventBean) eventBean;
        return theEvent.getUnderlying();
    }

    private CodegenMethodNode getObjectArrayCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(underlyingType, this.getClass(), codegenClassScope).addParam(Object[].class, "array").getBlock()
                .declareVar(Object.class, "eventBean", arrayAtIndex(ref("array"), constant(propertyIndex)))
                .ifRefNullReturnNull("eventBean")
                .methodReturn(cast(underlyingType, exprDotUnderlying(cast(EventBean.class, ref("eventBean")))));
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
        return arrayAtIndex(underlyingExpression, constant(propertyIndex));
    }
}
