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
import com.espertech.esper.common.internal.event.util.EPRuntimeEventProcessWrapped;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inbound unit for wrapped events.
 */
public class InboundUnitSendWrapped implements InboundUnitRunnable {
    private static final Logger log = LoggerFactory.getLogger(InboundUnitSendWrapped.class);
    private final EventBean eventBean;
    private final EPServicesContext services;
    private final EPRuntimeEventProcessWrapped runtime;

    /**
     * Ctor.
     *
     * @param theEvent inbound event, wrapped
     * @param runtime  to process
     * @param services services
     */
    public InboundUnitSendWrapped(EventBean theEvent, EPServicesContext services, EPRuntimeEventProcessWrapped runtime) {
        this.eventBean = theEvent;
        this.services = services;
        this.runtime = runtime;
    }

    public void run() {
        try {
            runtime.processWrappedEvent(eventBean);
        } catch (RuntimeException e) {
            services.getExceptionHandlingService().handleInboundPoolException(services.getRuntimeURI(), e, eventBean);
            log.error("Unexpected error processing wrapped event: " + e.getMessage(), e);
        }
    }
}
