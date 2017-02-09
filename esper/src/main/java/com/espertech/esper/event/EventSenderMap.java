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
package com.espertech.esper.event;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventSender;
import com.espertech.esper.core.service.EPRuntimeEventSender;
import com.espertech.esper.core.thread.InboundUnitSendWrapped;
import com.espertech.esper.core.thread.ThreadingOption;
import com.espertech.esper.core.thread.ThreadingService;
import com.espertech.esper.event.map.MapEventType;

import java.util.Map;

/**
 * Event sender for map-backed events.
 * <p>
 * Allows sending only event objects of type map, does not check map contents. Any other event object generates an error.
 */
public class EventSenderMap implements EventSender {
    private final EPRuntimeEventSender runtimeEventSender;
    private final EventAdapterService eventAdapterService;
    private final MapEventType mapEventType;
    private final ThreadingService threadingService;

    /**
     * Ctor.
     *
     * @param runtimeEventSender  for processing events
     * @param mapEventType        the event type
     * @param threadingService    for inbound threading
     * @param eventAdapterService for event bean creation
     */
    public EventSenderMap(EPRuntimeEventSender runtimeEventSender, MapEventType mapEventType, EventAdapterService eventAdapterService, ThreadingService threadingService) {
        this.runtimeEventSender = runtimeEventSender;
        this.mapEventType = mapEventType;
        this.threadingService = threadingService;
        this.eventAdapterService = eventAdapterService;
    }

    public void sendEvent(Object theEvent) {
        if (!(theEvent instanceof Map)) {
            throw new EPException("Unexpected event object of type " + theEvent.getClass().getName() + ", expected " + Map.class.getName());
        }

        Map<String, Object> map = (Map<String, Object>) theEvent;
        EventBean mapEvent = eventAdapterService.adapterForTypedMap(map, mapEventType);

        if ((ThreadingOption.isThreadingEnabled) && (threadingService.isInboundThreading())) {
            threadingService.submitInbound(new InboundUnitSendWrapped(mapEvent, runtimeEventSender));
        } else {
            runtimeEventSender.processWrappedEvent(mapEvent);
        }
    }

    public void route(Object theEvent) {
        if (!(theEvent instanceof Map)) {
            throw new EPException("Unexpected event object of type " + theEvent.getClass().getName() + ", expected " + Map.class.getName());
        }
        Map<String, Object> map = (Map<String, Object>) theEvent;
        EventBean mapEvent = eventAdapterService.adapterForTypedMap(map, mapEventType);
        runtimeEventSender.routeEventBean(mapEvent);
    }
}
