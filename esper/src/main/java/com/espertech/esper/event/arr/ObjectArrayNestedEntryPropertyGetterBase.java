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
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventAdapterService;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public abstract class ObjectArrayNestedEntryPropertyGetterBase implements ObjectArrayEventPropertyGetter {

    protected final int propertyIndex;
    protected final EventType fragmentType;
    protected final EventAdapterService eventAdapterService;

    /**
     * Ctor.
     *
     * @param propertyIndex       the property to look at
     * @param eventAdapterService factory for event beans and event types
     * @param fragmentType        type of the entry returned
     */
    public ObjectArrayNestedEntryPropertyGetterBase(int propertyIndex, EventType fragmentType, EventAdapterService eventAdapterService) {
        this.propertyIndex = propertyIndex;
        this.fragmentType = fragmentType;
        this.eventAdapterService = eventAdapterService;
    }

    public abstract Object handleNestedValue(Object value);
    public abstract boolean handleNestedValueExists(Object value);
    public abstract Object handleNestedValueFragment(Object value);
    public abstract CodegenExpression handleNestedValueCodegen(CodegenExpression refName, CodegenContext context);
    public abstract CodegenExpression handleNestedValueExistsCodegen(CodegenExpression refName, CodegenContext context);
    public abstract CodegenExpression handleNestedValueFragmentCodegen(CodegenExpression refName, CodegenContext context);

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        Object value = array[propertyIndex];
        if (value == null) {
            return null;
        }
        return handleNestedValue(value);
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object get(EventBean obj) {
        return getObjectArray(BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj));
    }

    private String getCodegen(CodegenContext context) {
        return context.addMethod(Object.class, Object[].class, "array", this.getClass())
                .declareVar(Object.class, "value", arrayAtIndex(ref("array"), constant(propertyIndex)))
                .ifRefNullReturnNull("value")
                .methodReturn(handleNestedValueCodegen(ref("value"), context));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        Object value = array[propertyIndex];
        if (value == null) {
            return false;
        }
        return handleNestedValueExists(value);
    }

    private String isExistsPropertyCodegen(CodegenContext context) {
        return context.addMethod(boolean.class, Object[].class, "array", this.getClass())
                .declareVar(Object.class, "value", arrayAtIndex(ref("array"), constant(propertyIndex)))
                .ifRefNullReturnFalse("value")
                .methodReturn(handleNestedValueExistsCodegen(ref("value"), context));
    }

    public Object getFragment(EventBean obj) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj);
        Object value = array[propertyIndex];
        if (value == null) {
            return null;
        }
        return handleNestedValueFragment(value);
    }

    private String getFragmentCodegen(CodegenContext context) {
        return context.addMethod(Object.class, Object[].class, "array", this.getClass())
                .declareVar(Object.class, "value", arrayAtIndex(ref("array"), constant(propertyIndex)))
                .ifRefNullReturnFalse("value")
                .methodReturn(handleNestedValueFragmentCodegen(ref("value"), context));
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingExists(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingFragment(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(isExistsPropertyCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getFragmentCodegen(context), underlyingExpression);
    }

}
