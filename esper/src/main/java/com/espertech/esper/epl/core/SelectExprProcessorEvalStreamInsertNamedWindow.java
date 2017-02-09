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

public class SelectExprProcessorEvalStreamInsertNamedWindow implements ExprEvaluator {
    private final int streamNum;
    private final EventType namedWindowAsType;
    private final Class returnType;
    private final EventAdapterService eventAdapterService;

    public SelectExprProcessorEvalStreamInsertNamedWindow(int streamNum, EventType namedWindowAsType, Class returnType, EventAdapterService eventAdapterService) {
        this.streamNum = streamNum;
        this.namedWindowAsType = namedWindowAsType;
        this.returnType = returnType;
        this.eventAdapterService = eventAdapterService;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean event = eventsPerStream[streamNum];
        if (event == null) {
            return null;
        }
        return eventAdapterService.adapterForType(event.getUnderlying(), namedWindowAsType);
    }

    public Class getType() {
        return returnType;
    }

    public int getStreamNum() {
        return streamNum;
    }
}
