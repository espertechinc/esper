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

import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.runtime.internal.kernel.service.EPEventServiceImpl;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Timer unit for a single callback for a statement.
 */
public class TimerUnitSingle implements TimerUnit {
    private static final Logger log = LoggerFactory.getLogger(TimerUnitSingle.class);

    private final EPServicesContext services;
    private final EPEventServiceImpl runtime;
    private final EPStatementHandleCallbackSchedule handleCallback;

    /**
     * Ctor.
     *
     * @param services       runtime services
     * @param runtime        runtime to process
     * @param handleCallback callback
     */
    public TimerUnitSingle(EPServicesContext services, EPEventServiceImpl runtime, EPStatementHandleCallbackSchedule handleCallback) {
        this.services = services;
        this.runtime = runtime;
        this.handleCallback = handleCallback;
    }

    public void run() {
        try {
            EPEventServiceImpl.processStatementScheduleSingle(handleCallback, services);

            runtime.dispatch();

            runtime.processThreadWorkQueue();
        } catch (RuntimeException e) {
            log.error("Unexpected error processing timer execution: " + e.getMessage(), e);
        }
    }
}
