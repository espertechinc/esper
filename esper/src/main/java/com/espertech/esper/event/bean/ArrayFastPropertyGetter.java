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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterAndIndexed;
import com.espertech.esper.event.vaevent.PropertyUtility;
import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

/**
 * Getter for an array property identified by a given index, using the CGLIB fast method.
 */
public class ArrayFastPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter, EventPropertyGetterAndIndexed {
    private final FastMethod fastMethod;
    private final int index;

    /**
     * Constructor.
     *
     * @param fastMethod          is the method to use to retrieve a value from the object
     * @param index               is tge index within the array to get the property from
     * @param eventAdapterService factory for event beans and event types
     */
    public ArrayFastPropertyGetter(FastMethod fastMethod, int index, EventAdapterService eventAdapterService) {
        super(eventAdapterService, fastMethod.getReturnType().getComponentType(), null);
        this.index = index;
        this.fastMethod = fastMethod;

        if (index < 0) {
            throw new IllegalArgumentException("Invalid negative index value");
        }
    }

    public Object getBeanProp(Object object) throws PropertyAccessException {
        return getBeanPropInternal(object, index);
    }

    private Object getBeanPropInternal(Object object, int index) throws PropertyAccessException {
        try {
            Object value = fastMethod.invoke(object, null);
            if (Array.getLength(value) <= index) {
                return null;
            }
            return Array.get(value, index);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(fastMethod.getJavaMethod(), object, e);
        } catch (InvocationTargetException e) {
            throw PropertyUtility.getInvocationTargetException(fastMethod.getJavaMethod(), e);
        }
    }

    public boolean isBeanExistsProperty(Object object) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public final Object get(EventBean obj) throws PropertyAccessException {
        return getBeanProp(obj.getUnderlying());
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        return getBeanPropInternal(eventBean.getUnderlying(), index);
    }

    public String toString() {
        return "ArrayFastPropertyGetter " +
                " fastMethod=" + fastMethod.toString() +
                " index=" + index;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }
}
