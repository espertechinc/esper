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

/**
 * Interface for processors of select-clause items, implementors are computing results based on matching events.
 */
public class SelectExprProcessorWDeliveryCallback implements SelectExprProcessor {
    private final EventType eventType;
    private final BindProcessor bindProcessor;
    private final SelectExprProcessorDeliveryCallback selectExprProcessorCallback;

    public SelectExprProcessorWDeliveryCallback(EventType eventType, BindProcessor bindProcessor, SelectExprProcessorDeliveryCallback selectExprProcessorCallback) {
        this.eventType = eventType;
        this.bindProcessor = bindProcessor;
        this.selectExprProcessorCallback = selectExprProcessorCallback;
    }

    public EventType getResultEventType() {
        return eventType;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] columns = bindProcessor.process(eventsPerStream, isNewData, exprEvaluatorContext);
        return selectExprProcessorCallback.selected(columns);
    }
}
