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
 * Inbound work unit processing a map event.
 */
public class InboundUnitSendObjectArray implements InboundUnitRunnable {
    private static final Logger log = LoggerFactory.getLogger(InboundUnitSendObjectArray.class);
    private final Object[] properties;
    private final String eventTypeName;
    private final EPEventServiceImpl runtime;

    /**
     * Ctor.
     *
     * @param properties    to send
     * @param eventTypeName type name
     * @param runtime       to process
     */
    public InboundUnitSendObjectArray(Object[] properties, String eventTypeName, EPEventServiceImpl runtime) {
        this.eventTypeName = eventTypeName;
        this.properties = properties;
        this.runtime = runtime;
    }

    public void run() {
        try {
            EventBean eventBean = runtime.getServices().getEventTypeResolvingBeanFactory().adapterForObjectArray(properties, eventTypeName);
            runtime.processWrappedEvent(eventBean);
        } catch (RuntimeException e) {
            runtime.getServices().getExceptionHandlingService().handleInboundPoolException(runtime.getRuntimeURI(), e, properties);
            log.error("Unexpected error processing Object-array event: " + e.getMessage(), e);
        }
    }
}
