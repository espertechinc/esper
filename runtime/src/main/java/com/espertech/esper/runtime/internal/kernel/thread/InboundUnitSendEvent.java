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
package com.espertech.esper.runtime.internal.kernel.thread;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.runtime.internal.kernel.service.EPEventServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inbound unit for unwrapped events.
 */
public class InboundUnitSendEvent implements InboundUnitRunnable {
    private static final Logger log = LoggerFactory.getLogger(InboundUnitSendEvent.class);
    private final Object theEvent;
    private final String eventTypeName;
    private final EPEventServiceImpl runtime;

    /**
     * Ctor.
     *
     * @param theEvent      to process
     * @param runtime       to process event
     * @param eventTypeName type name
     */
    public InboundUnitSendEvent(Object theEvent, String eventTypeName, EPEventServiceImpl runtime) {
        this.theEvent = theEvent;
        this.runtime = runtime;
        this.eventTypeName = eventTypeName;
    }

    public void run() {
        try {
            EventBean eventBean = runtime.getServices().getEventTypeResolvingBeanFactory().adapterForBean(theEvent, eventTypeName);
            runtime.processWrappedEvent(eventBean);
        } catch (Throwable t) {
            runtime.getServices().getExceptionHandlingService().handleInboundPoolException(runtime.getRuntimeURI(), t, theEvent);
            log.error("Unexpected error processing unwrapped event: " + t.getMessage(), t);
        }
    }
}
