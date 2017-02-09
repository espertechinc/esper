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
package com.espertech.esper.core.thread;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.service.EPRuntimeImpl;
import com.espertech.esper.core.service.EPServicesContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inbound unit for DOM events.
 */
public class InboundUnitSendDOM implements InboundUnitRunnable {
    private static final Logger log = LoggerFactory.getLogger(InboundUnitSendDOM.class);

    private final org.w3c.dom.Node theEvent;
    private final EPServicesContext services;
    private final EPRuntimeImpl runtime;

    /**
     * Ctor.
     *
     * @param theEvent document
     * @param services for wrapping event
     * @param runtime  runtime to process
     */
    public InboundUnitSendDOM(org.w3c.dom.Node theEvent, EPServicesContext services, EPRuntimeImpl runtime) {
        this.theEvent = theEvent;
        this.services = services;
        this.runtime = runtime;
    }

    public void run() {
        try {
            EventBean eventBean = services.getEventAdapterService().adapterForDOM(theEvent);
            runtime.processEvent(eventBean);
        } catch (RuntimeException e) {
            log.error("Unexpected error processing DOM event: " + e.getMessage(), e);
        }
    }
}
