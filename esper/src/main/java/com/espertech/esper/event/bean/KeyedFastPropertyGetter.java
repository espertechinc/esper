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
import com.espertech.esper.event.EventPropertyGetterAndMapped;
import com.espertech.esper.event.vaevent.PropertyUtility;
import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.InvocationTargetException;

/**
 * Getter for a key property identified by a given key value, using the CGLIB fast method.
 */
public class KeyedFastPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter, EventPropertyGetterAndMapped, EventPropertyGetterAndIndexed {
    private final FastMethod fastMethod;
    private final Object key;

    /**
     * Constructor.
     *
     * @param fastMethod          is the method to use to retrieve a value from the object.
     * @param key                 is the key to supply as parameter to the mapped property getter
     * @param eventAdapterService factory for event beans and event types
     */
    public KeyedFastPropertyGetter(FastMethod fastMethod, Object key, EventAdapterService eventAdapterService) {
        super(eventAdapterService, fastMethod.getReturnType(), null);
        this.key = key;
        this.fastMethod = fastMethod;
    }

    public boolean isBeanExistsProperty(Object object) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public final Object get(EventBean obj) throws PropertyAccessException {
        return getBeanProp(obj.getUnderlying());
    }

    public Object getBeanProp(Object object) throws PropertyAccessException {
        return getBeanPropInternal(object, key);
    }

    public Object get(EventBean eventBean, String mapKey) throws PropertyAccessException {
        return getBeanPropInternal(eventBean.getUnderlying(), mapKey);
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        return getBeanPropInternal(eventBean.getUnderlying(), index);
    }

    public Object getBeanPropInternal(Object object, Object key) throws PropertyAccessException {
        try {
            return fastMethod.invoke(object, new Object[]{key});
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(fastMethod.getJavaMethod(), object, e);
        } catch (InvocationTargetException e) {
            throw PropertyUtility.getInvocationTargetException(fastMethod.getJavaMethod(), e);
        }
    }

    public String toString() {
        return "KeyedFastPropertyGetter " +
                " fastMethod=" + fastMethod.toString() +
                " key=" + key;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }
}
