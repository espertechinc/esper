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
package com.espertech.esper.common.internal.event.bean.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.core.EventBeanCopyMethod;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Copies an event for modification.
 */
public class BeanEventBeanConfiguredCopyMethod implements EventBeanCopyMethod {
    private static final Logger log = LoggerFactory.getLogger(BeanEventBeanConfiguredCopyMethod.class);

    private final BeanEventType beanEventType;
    private final EventBeanTypedEventFactory eventAdapterService;
    private final Method copyMethod;

    /**
     * Ctor.
     *
     * @param beanEventType       type of bean to copy
     * @param eventAdapterService for creating events
     * @param copyMethod          method to copy the event
     */
    public BeanEventBeanConfiguredCopyMethod(BeanEventType beanEventType, EventBeanTypedEventFactory eventAdapterService, Method copyMethod) {
        this.beanEventType = beanEventType;
        this.eventAdapterService = eventAdapterService;
        this.copyMethod = copyMethod;
    }

    public EventBean copy(EventBean theEvent) {
        Object underlying = theEvent.getUnderlying();
        Object copied;
        try {
            copied = copyMethod.invoke(underlying, null);
        } catch (IllegalAccessException e) {
            log.error("IllegalAccessException copying event object for update: " + e.getMessage(), e);
            return null;
        } catch (InvocationTargetException e) {
            log.error("InvocationTargetException copying event object for update: " + e.getMessage(), e);
            return null;
        } catch (RuntimeException e) {
            log.error("RuntimeException copying event object for update: " + e.getMessage(), e);
            return null;
        }

        return eventAdapterService.adapterForTypedBean(copied, beanEventType);
    }
}
