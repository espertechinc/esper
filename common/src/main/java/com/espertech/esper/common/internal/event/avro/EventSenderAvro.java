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
package com.espertech.esper.common.internal.event.avro;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.util.EPRuntimeEventProcessWrapped;
import com.espertech.esper.common.internal.statement.thread.ThreadingCommon;

/**
 * Event sender for avro-backed events.
 * <p>
 * Allows sending only event objects of type GenericData.Record, does not check contents. Any other event object generates an error.
 */
public class EventSenderAvro implements EventSender {
    private final EPRuntimeEventProcessWrapped runtimeEventSender;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final EventType eventType;
    private final ThreadingCommon threadingService;

    /**
     * Ctor.
     *
     * @param runtimeEventSender         for processing events
     * @param eventType                  the event type
     * @param threadingService           for inbound threading
     * @param eventBeanTypedEventFactory for event bean creation
     */
    public EventSenderAvro(EPRuntimeEventProcessWrapped runtimeEventSender, EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, ThreadingCommon threadingService) {
        this.runtimeEventSender = runtimeEventSender;
        this.eventType = eventType;
        this.threadingService = threadingService;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public void sendEvent(Object theEvent) {
        EventBean eventBean = eventBeanTypedEventFactory.adapterForTypedAvro(theEvent, eventType);

        if (threadingService.isInboundThreading()) {
            threadingService.submitInbound(eventBean, runtimeEventSender);
        } else {
            runtimeEventSender.processWrappedEvent(eventBean);
        }
    }

    public void routeEvent(Object theEvent) {
        EventBean eventBean = eventBeanTypedEventFactory.adapterForTypedAvro(theEvent, eventType);
        runtimeEventSender.routeEventBean(eventBean);
    }
}
