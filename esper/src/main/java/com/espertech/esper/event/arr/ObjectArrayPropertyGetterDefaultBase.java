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

/**
 * Getter for map entry.
 */
public abstract class ObjectArrayPropertyGetterDefaultBase implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;
    protected final EventType fragmentEventType;
    protected final EventAdapterService eventAdapterService;

    /**
     * Ctor.
     *
     * @param propertyIndex       property index
     * @param fragmentEventType   fragment type
     * @param eventAdapterService factory for event beans and event types
     */
    public ObjectArrayPropertyGetterDefaultBase(int propertyIndex, EventType fragmentEventType, EventAdapterService eventAdapterService) {
        this.propertyIndex = propertyIndex;
        this.fragmentEventType = fragmentEventType;
        this.eventAdapterService = eventAdapterService;
    }

    protected abstract Object handleCreateFragment(Object value);
    protected abstract CodegenExpression handleCreateFragmentCodegen(CodegenExpression value, CodegenContext context);

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        return array[propertyIndex];
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return array.length > propertyIndex;
    }

    public Object get(EventBean obj) throws PropertyAccessException {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj);
        return getObjectArray(array);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        Object value = get(eventBean);
        return handleCreateFragment(value);
    }

    private String getFragmentCodegen(CodegenExpression value, CodegenContext context) {
        return context.addMethod(Object.class, this.getClass()).add(Object[].class, "oa").begin()
                .declareVar(Object.class, "value", underlyingGetCodegen(ref("oa"), context))
                .methodReturn(handleCreateFragmentCodegen(ref("value"), context));
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
        return arrayAtIndex(underlyingExpression, constant(propertyIndex));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        if (fragmentEventType == null) {
            return constantNull();
        }
        return localMethod(getFragmentCodegen(underlyingExpression, context), underlyingExpression);
    }
}
