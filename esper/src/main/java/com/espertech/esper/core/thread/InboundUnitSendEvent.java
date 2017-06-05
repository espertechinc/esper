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

import com.espertech.esper.core.service.EPRuntimeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inbound unit for unwrapped events.
 */
public class InboundUnitSendEvent implements InboundUnitRunnable {
    private static final Logger log = LoggerFactory.getLogger(InboundUnitSendEvent.class);
    private final Object theEvent;
    private final EPRuntimeImpl runtime;

    /**
     * Ctor.
     *
     * @param theEvent to process
     * @param runtime  to process event
     */
    public InboundUnitSendEvent(Object theEvent, EPRuntimeImpl runtime) {
        this.theEvent = theEvent;
        this.runtime = runtime;
    }

    public void run() {
        try {
            runtime.processEvent(theEvent);
        } catch (Throwable t) {
            runtime.getExceptionHandlingService().handleInboundPoolException(runtime.getEngineURI(), t, theEvent);
            log.error("Unexpected error processing unwrapped event: " + t.getMessage(), t);
        }
    }
}
