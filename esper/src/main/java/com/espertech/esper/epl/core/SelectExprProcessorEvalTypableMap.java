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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventAdapterService;

import java.util.Collections;
import java.util.Map;

public class SelectExprProcessorEvalTypableMap implements ExprEvaluator {
    private final EventType mapType;
    private final ExprEvaluator innerEvaluator;
    private final EventAdapterService eventAdapterService;

    public SelectExprProcessorEvalTypableMap(EventType mapType, ExprEvaluator innerEvaluator, EventAdapterService eventAdapterService) {
        this.mapType = mapType;
        this.innerEvaluator = innerEvaluator;
        this.eventAdapterService = eventAdapterService;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Map<String, Object> values = (Map<String, Object>) innerEvaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (values == null) {
            return eventAdapterService.adapterForTypedMap(Collections.<String, Object>emptyMap(), mapType);
        }
        return eventAdapterService.adapterForTypedMap(values, mapType);
    }

    public Class getType() {
        return Map.class;
    }

    public ExprEvaluator getInnerEvaluator() {
        return innerEvaluator;
    }
}
