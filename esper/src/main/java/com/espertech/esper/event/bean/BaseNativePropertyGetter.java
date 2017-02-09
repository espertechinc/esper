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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Base getter for native fragments.
 */
public abstract class BaseNativePropertyGetter implements EventPropertyGetter {
    private final EventAdapterService eventAdapterService;
    private volatile BeanEventType fragmentEventType;
    private final Class fragmentClassType;
    private boolean isFragmentable;
    private final boolean isArray;
    private final boolean isIterable;

    /**
     * Constructor.
     *
     * @param eventAdapterService factory for event beans and event types
     * @param returnType          type of the entry returned
     * @param genericType         type generic parameter, if any
     */
    public BaseNativePropertyGetter(EventAdapterService eventAdapterService, Class returnType, Class genericType) {
        this.eventAdapterService = eventAdapterService;
        if (returnType.isArray()) {
            this.fragmentClassType = returnType.getComponentType();
            isArray = true;
            isIterable = false;
        } else if (JavaClassHelper.isImplementsInterface(returnType, Iterable.class)) {
            this.fragmentClassType = genericType;
            isArray = false;
            isIterable = true;
        } else {
            this.fragmentClassType = returnType;
            isArray = false;
            isIterable = false;
        }
        isFragmentable = true;
    }

    /**
     * Returns the fragment for dynamic properties.
     *
     * @param object              to inspect
     * @param eventAdapterService factory for event beans and event types
     * @return fragment
     */
    public static Object getFragmentDynamic(Object object, EventAdapterService eventAdapterService) {
        if (object == null) {
            return null;
        }

        BeanEventType fragmentEventType = null;
        boolean isArray = false;
        if (object.getClass().isArray()) {
            if (JavaClassHelper.isFragmentableType(object.getClass().getComponentType())) {
                isArray = true;
                fragmentEventType = eventAdapterService.getBeanEventTypeFactory().createBeanTypeDefaultName(object.getClass().getComponentType());
            }
        } else {
            if (JavaClassHelper.isFragmentableType(object.getClass())) {
                fragmentEventType = eventAdapterService.getBeanEventTypeFactory().createBeanTypeDefaultName(object.getClass());
            }
        }

        if (fragmentEventType == null) {
            return null;
        }

        if (isArray) {
            int len = Array.getLength(object);
            EventBean[] events = new EventBean[len];
            int countFilled = 0;

            for (int i = 0; i < len; i++) {
                Object element = Array.get(object, i);
                if (element == null) {
                    continue;
                }

                events[countFilled] = eventAdapterService.adapterForTypedBean(element, fragmentEventType);
                countFilled++;
            }

            if (countFilled == len) {
                return events;
            }

            if (countFilled == 0) {
                return new EventBean[0];
            }

            EventBean[] returnVal = new EventBean[countFilled];
            System.arraycopy(events, 0, returnVal, 0, countFilled);
            return returnVal;
        } else {
            return eventAdapterService.adapterForTypedBean(object, fragmentEventType);
        }
    }

    public Object getFragment(EventBean eventBean) {
        Object object = get(eventBean);
        if (object == null) {
            return null;
        }

        if (!isFragmentable) {
            return null;
        }

        if (fragmentEventType == null) {
            if (JavaClassHelper.isFragmentableType(fragmentClassType)) {
                fragmentEventType = eventAdapterService.getBeanEventTypeFactory().createBeanTypeDefaultName(fragmentClassType);
            } else {
                isFragmentable = false;
                return null;
            }
        }

        if (isArray) {
            int len = Array.getLength(object);
            EventBean[] events = new EventBean[len];
            int countFilled = 0;

            for (int i = 0; i < len; i++) {
                Object element = Array.get(object, i);
                if (element == null) {
                    continue;
                }

                events[countFilled] = eventAdapterService.adapterForTypedBean(element, fragmentEventType);
                countFilled++;
            }

            if (countFilled == len) {
                return events;
            }

            if (countFilled == 0) {
                return new EventBean[0];
            }

            EventBean[] returnVal = new EventBean[countFilled];
            System.arraycopy(events, 0, returnVal, 0, countFilled);
            return returnVal;
        } else if (isIterable) {
            if (!(object instanceof Iterable)) {
                return null;
            }
            Iterator iterator = ((Iterable) object).iterator();
            if (!iterator.hasNext()) {
                return new EventBean[0];
            }
            ArrayDeque<EventBean> events = new ArrayDeque<EventBean>();
            while (iterator.hasNext()) {
                Object next = iterator.next();
                if (next == null) {
                    continue;
                }

                events.add(eventAdapterService.adapterForTypedBean(next, fragmentEventType));
            }
            return events.toArray(new EventBean[events.size()]);
        } else {
            return eventAdapterService.adapterForTypedBean(object, fragmentEventType);
        }
    }
}
