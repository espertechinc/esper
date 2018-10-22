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
package com.espertech.esper.example.stockticker;

import com.espertech.esper.runtime.client.EPRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendEventRunnable implements Runnable {
    private Object eventToSend;
    private EPRuntime runtime;

    public SendEventRunnable(EPRuntime runtime, Object eventToSend) {
        this.runtime = runtime;
        this.eventToSend = eventToSend;
    }

    public void run() {
        try {
            runtime.getEventService().sendEventBean(eventToSend, eventToSend.getClass().getSimpleName());
        } catch (Exception ex) {
            log.error("Failed to run: {}", ex.getMessage(), ex);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(SendEventRunnable.class);
}
