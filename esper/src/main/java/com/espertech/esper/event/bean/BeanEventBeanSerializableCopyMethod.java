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
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventBeanCopyMethod;
import com.espertech.esper.util.SerializableObjectCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Copy method for bean events utilizing serializable.
 */
public class BeanEventBeanSerializableCopyMethod implements EventBeanCopyMethod {
    private static final Logger log = LoggerFactory.getLogger(BeanEventBeanSerializableCopyMethod.class);

    private final BeanEventType beanEventType;
    private final EventAdapterService eventAdapterService;

    /**
     * Ctor.
     *
     * @param beanEventType       event type
     * @param eventAdapterService for creating the event object
     */
    public BeanEventBeanSerializableCopyMethod(BeanEventType beanEventType, EventAdapterService eventAdapterService) {
        this.beanEventType = beanEventType;
        this.eventAdapterService = eventAdapterService;
    }

    public EventBean copy(EventBean theEvent) {
        Object underlying = theEvent.getUnderlying();
        Object copied;
        try {
            copied = SerializableObjectCopier.copy(underlying);
        } catch (IOException e) {
            log.error("IOException copying event object for update: " + e.getMessage(), e);
            return null;
        } catch (ClassNotFoundException e) {
            log.error("Exception copying event object for update: " + e.getMessage(), e);
            return null;
        }

        return eventAdapterService.adapterForTypedBean(copied, beanEventType);
    }
}
