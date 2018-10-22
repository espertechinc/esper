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
package com.espertech.esper.common.internal.event.arr;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.util.EPRuntimeEventProcessWrapped;
import com.espertech.esper.common.internal.statement.thread.ThreadingCommon;

/**
 * Event sender for map-backed events.
 * <p>
 * Allows sending only event objects of type map, does not check map contents. Any other event object generates an error.
 */
public class EventSenderObjectArray implements EventSender {
    private final EPRuntimeEventProcessWrapped runtimeEventSender;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final ObjectArrayEventType objectArrayEventType;
    private final ThreadingCommon threadingService;

    /**
     * Ctor.
     *
     * @param runtimeEventSender         for processing events
     * @param objectArrayEventType       the event type
     * @param threadingService           for inbound threading
     * @param eventBeanTypedEventFactory for event bean creation
     */
    public EventSenderObjectArray(EPRuntimeEventProcessWrapped runtimeEventSender, ObjectArrayEventType objectArrayEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, ThreadingCommon threadingService) {
        this.runtimeEventSender = runtimeEventSender;
        this.objectArrayEventType = objectArrayEventType;
        this.threadingService = threadingService;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public void sendEvent(Object theEvent) {
        if (!(theEvent.getClass().isArray())) {
            throw new EPException("Unexpected event object of type " + theEvent.getClass().getName() + ", expected Object[]");
        }

        Object[] arr = (Object[]) theEvent;
        EventBean objectArrayEvent = eventBeanTypedEventFactory.adapterForTypedObjectArray(arr, objectArrayEventType);

        if (threadingService.isInboundThreading()) {
            threadingService.submitInbound(objectArrayEvent, runtimeEventSender);
        } else {
            runtimeEventSender.processWrappedEvent(objectArrayEvent);
        }
    }

    public void routeEvent(Object theEvent) {
        if (!(theEvent.getClass().isArray())) {
            throw new EPException("Unexpected event object of type " + theEvent.getClass().getName() + ", expected Object[]");
        }
        Object[] arr = (Object[]) theEvent;
        EventBean objectArrayEvent = eventBeanTypedEventFactory.adapterForTypedObjectArray(arr, objectArrayEventType);
        runtimeEventSender.routeEventBean(objectArrayEvent);
    }
}
