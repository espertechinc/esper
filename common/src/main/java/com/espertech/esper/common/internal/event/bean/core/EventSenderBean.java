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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.util.EPRuntimeEventProcessWrapped;
import com.espertech.esper.common.internal.statement.thread.ThreadingCommon;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.HashSet;
import java.util.Set;

/**
 * Event sender for POJO Java object events.
 * <p>
 * Allows sending only event objects of the underlying type matching the event type, or
 * implementing the interface or extending the type. Any other event object generates an error.
 */
public class EventSenderBean implements EventSender {
    private final EPRuntimeEventProcessWrapped runtime;
    private final BeanEventType beanEventType;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final Set<Class> compatibleClasses;
    private final ThreadingCommon threadingService;

    /**
     * Ctor.
     *
     * @param runtime                    for processing events
     * @param beanEventType              the event type
     * @param eventBeanTypedEventFactory factory for event beans and event types
     * @param threadingService           for inbound threading
     */
    public EventSenderBean(EPRuntimeEventProcessWrapped runtime, BeanEventType beanEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, ThreadingCommon threadingService) {
        this.runtime = runtime;
        this.beanEventType = beanEventType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        compatibleClasses = new HashSet<Class>();
        this.threadingService = threadingService;
    }

    public void sendEvent(Object theEvent) {
        if (theEvent == null) {
            throw new NullPointerException("No event object provided to sendEvent method");
        }

        EventBean eventBean = getEventBean(theEvent);

        // Process event
        if (threadingService.isInboundThreading()) {
            threadingService.submitInbound(eventBean, runtime);
        } else {
            runtime.processWrappedEvent(eventBean);
        }
    }

    public void routeEvent(Object theEvent) throws EPException {
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

        return eventBeanTypedEventFactory.adapterForTypedBean(theEvent, beanEventType);
    }
}
