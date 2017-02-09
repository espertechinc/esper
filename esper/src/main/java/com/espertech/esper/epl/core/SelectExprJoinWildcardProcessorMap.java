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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventAdapterService;

import java.util.HashMap;
import java.util.Map;

/**
 * Processor for select-clause expressions that handles wildcards. Computes results based on matching events.
 */
public class SelectExprJoinWildcardProcessorMap implements SelectExprProcessor {
    private final String[] streamNames;
    private final EventType resultEventType;
    private final EventAdapterService eventAdapterService;

    public SelectExprJoinWildcardProcessorMap(String[] streamNames, EventType resultEventType, EventAdapterService eventAdapterService) {
        this.streamNames = streamNames;
        this.resultEventType = resultEventType;
        this.eventAdapterService = eventAdapterService;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        Map<String, Object> tuple = new HashMap<String, Object>();
        for (int i = 0; i < streamNames.length; i++) {
            if (streamNames[i] == null) {
                throw new IllegalStateException("Event name for stream " + i + " is null");
            }
            tuple.put(streamNames[i], eventsPerStream[i]);
        }

        return eventAdapterService.adapterForTypedMap(tuple, resultEventType);
    }

    public EventType getResultEventType() {
        return resultEventType;
    }
}
