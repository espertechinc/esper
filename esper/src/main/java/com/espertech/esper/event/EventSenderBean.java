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
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPRuntimeEventSender;
import com.espertech.esper.core.thread.InboundUnitSendWrapped;
import com.espertech.esper.core.thread.ThreadingOption;
import com.espertech.esper.core.thread.ThreadingService;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.util.ExecutionPathDebugLog;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Event sender for POJO Java object events.
 * <p>
 * Allows sending only event objects of the underlying type matching the event type, or
 * implementing the interface or extending the type. Any other event object generates an error.
 */
public class EventSenderBean implements EventSender {
    private static final Logger log = LoggerFactory.getLogger(EventSenderBean.class);
    private final EPRuntimeEventSender runtime;
    private final BeanEventType beanEventType;
    private final EventAdapterService eventAdapterService;
    private final Set<Class> compatibleClasses;
    private final ThreadingService threadingService;

    /**
     * Ctor.
     *
     * @param runtime             for processing events
     * @param beanEventType       the event type
     * @param eventAdapterService factory for event beans and event types
     * @param threadingService    for inbound threading
     */
    public EventSenderBean(EPRuntimeEventSender runtime, BeanEventType beanEventType, EventAdapterService eventAdapterService, ThreadingService threadingService) {
        this.runtime = runtime;
        this.beanEventType = beanEventType;
        this.eventAdapterService = eventAdapterService;
        compatibleClasses = new HashSet<Class>();
        this.threadingService = threadingService;
    }

    public void sendEvent(Object theEvent) {
        if (theEvent == null) {
            throw new NullPointerException("No event object provided to sendEvent method");
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            if ((!(theEvent instanceof CurrentTimeEvent)) || (ExecutionPathDebugLog.isTimerDebugEnabled)) {
                log.debug(".sendEvent Processing event " + theEvent);
            }
        }

        EventBean eventBean = getEventBean(theEvent);

        // Process event
        if ((ThreadingOption.isThreadingEnabled) && (threadingService.isInboundThreading())) {
            threadingService.submitInbound(new InboundUnitSendWrapped(eventBean, runtime));
        } else {
            runtime.processWrappedEvent(eventBean);
        }
    }

    public void route(Object theEvent) throws EPException {
        EventBean eventBean = getEventBean(theEvent);
        runtime.routeEventBean(eventBean);
    }

    private EventBean getEventBean(Object theEvent) {
        // type check
        if (theEvent.getClass() != beanEventType.getUnderlyingType()) {
            synchronized (this) {
                if (!compatibleClasses.contains(theEvent.getClass())) {
                    if (JavaClassHelper.isSubclassOrImplementsInterface(theEvent.getClass(), beanEventType.getUnderlyingType())) {
                        compatibleClasses.add(theEvent.getClass());
                    } else {
                        throw new EPException("Event object of type " + theEvent.getClass().getName() +
                                " does not equal, extend or implement the type " + beanEventType.getUnderlyingType().getName() +
                                " of event type '" + beanEventType.getName() + "'");
                    }
                }
            }
        }

        return eventAdapterService.adapterForTypedBean(theEvent, beanEventType);
    }
}
