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
package com.espertech.esper.common.internal.event.map;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.util.EPRuntimeEventProcessWrapped;
import com.espertech.esper.common.internal.statement.thread.ThreadingCommon;

import java.util.Map;

/**
 * Event sender for map-backed events.
 * <p>
 * Allows sending only event objects of type map, does not check map contents. Any other event object generates an error.
 */
public class EventSenderMap implements EventSender {
    private final EPRuntimeEventProcessWrapped runtimeEventSender;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final MapEventType mapEventType;
    private final ThreadingCommon threadingService;

    /**
     * Ctor.
     *
     * @param runtimeEventSender         for processing events
     * @param mapEventType               the event type
     * @param threadingService           for inbound threading
     * @param eventBeanTypedEventFactory for event bean creation
     */
    public EventSenderMap(EPRuntimeEventProcessWrapped runtimeEventSender, MapEventType mapEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, ThreadingCommon threadingService) {
        this.runtimeEventSender = runtimeEventSender;
        this.mapEventType = mapEventType;
        this.threadingService = threadingService;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public void sendEvent(Object theEvent) {
        if (!(theEvent instanceof Map)) {
            throw new EPException("Unexpected event object of type " + theEvent.getClass().getName() + ", expected " + Map.class.getName());
        }

        Map<String, Object> map = (Map<String, Object>) theEvent;
        EventBean mapEvent = eventBeanTypedEventFactory.adapterForTypedMap(map, mapEventType);

        if (threadingService.isInboundThreading()) {
            threadingService.submitInbound(mapEvent, runtimeEventSender);
        } else {
            runtimeEventSender.processWrappedEvent(mapEvent);
        }
    }

    public void routeEvent(Object theEvent) {
        if (!(theEvent instanceof Map)) {
            throw new EPException("Unexpected event object of type " + theEvent.getClass().getName() + ", expected " + Map.class.getName());
        }
        Map<String, Object> map = (Map<String, Object>) theEvent;
        EventBean mapEvent = eventBeanTypedEventFactory.adapterForTypedMap(map, mapEventType);
        runtimeEventSender.routeEventBean(mapEvent);
    }
}
