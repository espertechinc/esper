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
package com.espertech.esper.epl.core.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;

public class EvalInsertWildcardJoinRevision extends EvalBase implements SelectExprProcessor {

    private final SelectExprProcessor joinWildcardProcessor;
    private final ValueAddEventProcessor vaeProcessor;

    public EvalInsertWildcardJoinRevision(SelectExprContext selectExprContext, EventType resultEventType, SelectExprProcessor joinWildcardProcessor, ValueAddEventProcessor vaeProcessor) {
        super(selectExprContext, resultEventType);
        this.joinWildcardProcessor = joinWildcardProcessor;
        this.vaeProcessor = vaeProcessor;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = joinWildcardProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
        return vaeProcessor.getValueAddEventBean(theEvent);
    }
}