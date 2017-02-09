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
import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * Getter for a iterable property identified by a given index, using the CGLIB fast method.
 */
public class IterableFastPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter, EventPropertyGetterAndIndexed {
    private final FastMethod fastMethod;
    private final int index;

    /**
     * Constructor.
     *
     * @param method              the underlying method
     * @param fastMethod          is the method to use to retrieve a value from the object
     * @param index               is tge index within the array to get the property from
     * @param eventAdapterService factory for event beans and event types
     */
    public IterableFastPropertyGetter(Method method, FastMethod fastMethod, int index, EventAdapterService eventAdapterService) {
        super(eventAdapterService, JavaClassHelper.getGenericReturnType(method, false), null);
        this.index = index;
        this.fastMethod = fastMethod;

        if (index < 0) {
            throw new IllegalArgumentException("Invalid negative index value");
        }
    }

    public Object getBeanProp(Object object) throws PropertyAccessException {
        try {
            Object value = fastMethod.invoke(object, null);
            return getIterable(value, index);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(fastMethod.getJavaMethod(), object, e);
        } catch (InvocationTargetException e) {
            throw PropertyUtility.getInvocationTargetException(fastMethod.getJavaMethod(), e);
        }
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        return getIterable(eventBean.getUnderlying(), index);
    }

    /**
     * Returns the iterable at a certain index, or null.
     *
     * @param value the iterable
     * @param index index
     * @return value at index
     */
    protected static Object getIterable(Object value, int index) {
        if (!(value instanceof Iterable)) {
            return null;
        }

        Iterator it = ((Iterable) value).iterator();

        if (index == 0) {
            if (it.hasNext()) {
                return it.next();
            }
            return null;
        }

        int count = 0;
        while (true) {
            if (!it.hasNext()) {
                return null;
            }
            if (count < index) {
                it.next();
            } else {
                return it.next();
            }
            count++;
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
        return "ListFastPropertyGetter " +
                " fastMethod=" + fastMethod.toString() +
                " index=" + index;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }
}
