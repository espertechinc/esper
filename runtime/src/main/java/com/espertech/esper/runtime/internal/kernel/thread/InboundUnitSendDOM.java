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
 * Inbound unit for DOM events.
 */
public class InboundUnitSendDOM implements InboundUnitRunnable {
    private static final Logger log = LoggerFactory.getLogger(InboundUnitSendDOM.class);

    private final org.w3c.dom.Node theEvent;
    private final String eventTypeName;
    private final EPEventServiceImpl runtime;

    /**
     * Ctor.
     *
     * @param theEvent      document
     * @param runtime       runtime to process
     * @param eventTypeName type name
     */
    public InboundUnitSendDOM(org.w3c.dom.Node theEvent, String eventTypeName, EPEventServiceImpl runtime) {
        this.theEvent = theEvent;
        this.eventTypeName = eventTypeName;
        this.runtime = runtime;
    }

    public void run() {
        try {
            EventBean eventBean = runtime.getServices().getEventTypeResolvingBeanFactory().adapterForXMLDOM(theEvent, eventTypeName);
            runtime.processWrappedEvent(eventBean);
        } catch (RuntimeException e) {
            runtime.getServices().getExceptionHandlingService().handleInboundPoolException(runtime.getRuntimeURI(), e, theEvent);
            log.error("Unexpected error processing DOM event: " + e.getMessage(), e);
        }
    }
}
