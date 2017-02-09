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
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Getter for an iterable property identified by a given index, using vanilla reflection.
 */
public class IterableMethodPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter, EventPropertyGetterAndIndexed {
    private final Method method;
    private final int index;

    /**
     * Constructor.
     *
     * @param method              is the method to use to retrieve a value from the object
     * @param index               is tge index within the array to get the property from
     * @param eventAdapterService factory for event beans and event types
     */
    public IterableMethodPropertyGetter(Method method, int index, EventAdapterService eventAdapterService) {
        super(eventAdapterService, JavaClassHelper.getGenericReturnType(method, false), null);
        this.index = index;
        this.method = method;

        if (index < 0) {
            throw new IllegalArgumentException("Invalid negative index value");
        }
    }

    public Object getBeanProp(Object object) throws PropertyAccessException {
        return getBeanPropInternal(object, index);
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        return getBeanPropInternal(eventBean.getUnderlying(), index);
    }

    private Object getBeanPropInternal(Object object, int index) throws PropertyAccessException {
        try {
            Object value = method.invoke(object, (Object[]) null);
            return IterableFastPropertyGetter.getIterable(value, index);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(method, object, e);
        } catch (InvocationTargetException e) {
            throw PropertyUtility.getInvocationTargetException(method, e);
        } catch (IllegalAccessException e) {
            throw PropertyUtility.getIllegalAccessException(method, e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(method, e);
        }
    }

    public boolean isBeanExistsProperty(Object object) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public final Object get(EventBean obj) throws PropertyAccessException {
        Object underlying = obj.getUnderlying();
        return getBeanProp(underlying);
    }

    public String toString() {
        return "IterableMethodPropertyGetter " +
                " method=" + method.toString() +
                " index=" + index;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }
}
