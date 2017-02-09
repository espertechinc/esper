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
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventBeanCopyMethod;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

/**
 * Copy method for Object array-underlying events.
 */
public class ObjectArrayEventBeanCopyMethod implements EventBeanCopyMethod {
    private final ObjectArrayEventType objectArrayEventType;
    private final EventAdapterService eventAdapterService;

    /**
     * Ctor.
     *
     * @param objectArrayEventType map event type
     * @param eventAdapterService  for copying events
     */
    public ObjectArrayEventBeanCopyMethod(ObjectArrayEventType objectArrayEventType, EventAdapterService eventAdapterService) {
        this.objectArrayEventType = objectArrayEventType;
        this.eventAdapterService = eventAdapterService;
    }

    public EventBean copy(EventBean theEvent) {
        Object[] array = ((ObjectArrayBackedEventBean) theEvent).getProperties();
        Object[] copy = new Object[array.length];
        System.arraycopy(array, 0, copy, 0, copy.length);
        return eventAdapterService.adapterForTypedObjectArray(copy, objectArrayEventType);
    }
}
