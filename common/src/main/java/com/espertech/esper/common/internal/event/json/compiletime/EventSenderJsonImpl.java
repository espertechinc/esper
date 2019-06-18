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
package com.espertech.esper.common.internal.event.json.compiletime;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.json.util.EventSenderJson;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.util.EPRuntimeEventProcessWrapped;
import com.espertech.esper.common.internal.statement.thread.ThreadingCommon;

/**
 * Event sender for json-backed events.
 * <p>
 * Allows sending only event objects of type string, does not check contents. Any other event object generates an error.
 */
public class EventSenderJsonImpl implements EventSenderJson {
    private final EPRuntimeEventProcessWrapped runtimeEventSender;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final JsonEventType eventType;
    private final ThreadingCommon threadingService;

    /**
     * Ctor.
     *
     * @param runtimeEventSender         for processing events
     * @param eventType                  the event type
     * @param threadingService           for inbound threading
     * @param eventBeanTypedEventFactory for event bean creation
     */
    public EventSenderJsonImpl(EPRuntimeEventProcessWrapped runtimeEventSender, JsonEventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, ThreadingCommon threadingService) {
        this.runtimeEventSender = runtimeEventSender;
        this.eventType = eventType;
        this.threadingService = threadingService;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public void sendEvent(Object theEvent) {
        Object underlying = getUnderlying(theEvent);
        EventBean eventBean = eventBeanTypedEventFactory.adapterForTypedJson(underlying, eventType);

        if (threadingService.isInboundThreading()) {
            threadingService.submitInbound(eventBean, runtimeEventSender);
        } else {
            runtimeEventSender.processWrappedEvent(eventBean);
        }
    }

    public void routeEvent(Object theEvent) {
        EventBean eventBean = eventBeanTypedEventFactory.adapterForTypedJson(getUnderlying(theEvent), eventType);
        runtimeEventSender.routeEventBean(eventBean);
    }

    public Object parse(String json) {
        return eventType.parse(json);
    }

    private Object getUnderlying(Object theEvent) {
        if (theEvent instanceof String) {
            return eventType.parse((String) theEvent);
        } else if (theEvent == null || !(theEvent.getClass() == eventType.getUnderlyingType())) {
            throw new EPException("Unexpected event object of type '" + (theEvent == null ? "(null)" : theEvent.getClass().getName()) + "', expected a Json-formatted string-type value");
        }
        return theEvent;
    }
}
