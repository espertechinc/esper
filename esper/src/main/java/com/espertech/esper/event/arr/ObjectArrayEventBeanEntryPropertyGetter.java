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
import com.espertech.esper.event.EventPropertyGetterSPI;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * A getter that works on EventBean events residing within a Map as an event property.
 */
public class ObjectArrayEventBeanEntryPropertyGetter implements ObjectArrayEventPropertyGetter {

    private final int propertyIndex;
    private final EventPropertyGetterSPI eventBeanEntryGetter;

    /**
     * Ctor.
     *
     * @param propertyIndex        the property to look at
     * @param eventBeanEntryGetter the getter for the map entry
     */
    public ObjectArrayEventBeanEntryPropertyGetter(int propertyIndex, EventPropertyGetterSPI eventBeanEntryGetter) {
        this.propertyIndex = propertyIndex;
        this.eventBeanEntryGetter = eventBeanEntryGetter;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        Object value = array[propertyIndex];

        if (value == null) {
            return null;
        }

        // Object within the map
        EventBean theEvent = (EventBean) value;
        return eventBeanEntryGetter.get(theEvent);
    }

    private String getObjectArrayCodegen(CodegenContext context)  {
        return context.addMethod(Object.class, Object[].class, "array", this.getClass())
                .declareVar(Object.class, "value", arrayAtIndex(ref("array"), constant(propertyIndex)))
                .ifRefNullReturnNull("value")
                .declareVarWCast(EventBean.class, "theEvent", "value")
                .methodReturn(eventBeanEntryGetter.codegenEventBeanGet(ref("theEvent"), context));
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
        // If the map does not contain the key, this is allowed and represented as null
        Object value = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj)[propertyIndex];

        if (value == null) {
            return null;
        }

        // Object within the map
        EventBean theEvent = (EventBean) value;
        return eventBeanEntryGetter.getFragment(theEvent);
    }

    private String getFragmentCodegen(CodegenContext context)  {
        return context.addMethod(Object.class, Object[].class, "array", this.getClass())
                .declareVar(Object.class, "value", arrayAtIndex(ref("array"), constant(propertyIndex)))
                .ifRefNullReturnNull("value")
                .declareVarWCast(EventBean.class, "theEvent", "value")
                .methodReturn(eventBeanEntryGetter.codegenEventBeanFragment(ref("theEvent"), context));
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
