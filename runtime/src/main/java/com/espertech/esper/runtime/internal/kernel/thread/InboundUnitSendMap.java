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
import com.espertech.esper.runtime.internal.kernel.service.EPServicesEvaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Inbound work unit processing a map event.
 */
public class InboundUnitSendMap implements InboundUnitRunnable {
    private static final Logger log = LoggerFactory.getLogger(InboundUnitSendMap.class);
    private final Map map;
    private final String eventTypeName;
    private final EPRuntimeEventProcessWrapped runtime;
    private final EPServicesEvaluation services;

    public InboundUnitSendMap(Map map, String eventTypeName, EPRuntimeEventProcessWrapped runtime, EPServicesEvaluation services) {
        this.map = map;
        this.eventTypeName = eventTypeName;
        this.runtime = runtime;
        this.services = services;
    }

    public void run() {
        try {
            EventBean eventBean = services.getEventTypeResolvingBeanFactory().adapterForMap(map, eventTypeName);
            runtime.processWrappedEvent(eventBean);
        } catch (RuntimeException e) {
            services.getExceptionHandlingService().handleInboundPoolException(runtime.getURI(), e, map);
            log.error("Unexpected error processing Map event: " + e.getMessage(), e);
        }
    }
}
