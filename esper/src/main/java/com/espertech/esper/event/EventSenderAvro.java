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
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.EPRuntimeEventSender;
import com.espertech.esper.core.thread.InboundUnitSendWrapped;
import com.espertech.esper.core.thread.ThreadingOption;
import com.espertech.esper.core.thread.ThreadingService;

/**
 * Event sender for avro-backed events.
 * <p>
 * Allows sending only event objects of type GenericData.Record, does not check contents. Any other event object generates an error.
 */
public class EventSenderAvro implements EventSender {
    private final EPRuntimeEventSender runtimeEventSender;
    private final EventAdapterService eventAdapterService;
    private final EventType eventType;
    private final ThreadingService threadingService;

    /**
     * Ctor.
     *
     * @param runtimeEventSender  for processing events
     * @param eventType           the event type
     * @param threadingService    for inbound threading
     * @param eventAdapterService for event bean creation
     */
    public EventSenderAvro(EPRuntimeEventSender runtimeEventSender, EventType eventType, EventAdapterService eventAdapterService, ThreadingService threadingService) {
        this.runtimeEventSender = runtimeEventSender;
        this.eventType = eventType;
        this.threadingService = threadingService;
        this.eventAdapterService = eventAdapterService;
    }

    public void sendEvent(Object theEvent) {
        EventBean eventBean = eventAdapterService.adapterForTypedAvro(theEvent, eventType);

        if ((ThreadingOption.isThreadingEnabled) && (threadingService.isInboundThreading())) {
            threadingService.submitInbound(new InboundUnitSendWrapped(eventBean, runtimeEventSender));
        } else {
            runtimeEventSender.processWrappedEvent(eventBean);
        }
    }

    public void route(Object theEvent) {
        if (!(theEvent.getClass().isArray())) {
            throw new EPException("Unexpected event object of type " + theEvent.getClass().getName() + ", expected Object[]");
        }
        EventBean eventBean = eventAdapterService.adapterForTypedAvro(theEvent, eventType);
        runtimeEventSender.routeEventBean(eventBean);
    }
}
