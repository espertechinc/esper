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

import com.espertech.esper.client.EPServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendEventRunnable implements Runnable {
    private Object eventToSend;
    private EPServiceProvider epService;

    public SendEventRunnable(EPServiceProvider epService, Object eventToSend) {
        this.epService = epService;
        this.eventToSend = eventToSend;
    }

    public void run() {
        try {
            epService.getEPRuntime().sendEvent(eventToSend);
        } catch (Exception ex) {
            log.error("Failed to run: {}", ex.getMessage(), ex);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(SendEventRunnable.class);
}
