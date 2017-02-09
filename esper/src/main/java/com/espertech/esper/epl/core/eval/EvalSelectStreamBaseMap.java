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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.spec.SelectClauseStreamCompiledSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class EvalSelectStreamBaseMap extends EvalSelectStreamBase implements SelectExprProcessor {

    protected EvalSelectStreamBaseMap(SelectExprContext selectExprContext, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard) {
        super(selectExprContext, resultEventType, namedStreams, usingWildcard);
    }

    public abstract EventBean processSpecific(Map<String, Object> props, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        // Evaluate all expressions and build a map of name-value pairs
        Map<String, Object> props = new HashMap<String, Object>();
        int count = 0;
        for (ExprEvaluator expressionNode : selectExprContext.getExpressionNodes()) {
            Object evalResult = expressionNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            props.put(selectExprContext.getColumnNames()[count], evalResult);
            count++;
        }
        for (SelectClauseStreamCompiledSpec element : namedStreams) {
            EventBean theEvent = eventsPerStream[element.getStreamNumber()];
            if (element.getTableMetadata() != null) {
                if (theEvent != null) {
                    theEvent = element.getTableMetadata().getEventToPublic().convert(theEvent, eventsPerStream, isNewData, exprEvaluatorContext);
                }
            }
            props.put(selectExprContext.getColumnNames()[count], theEvent);
            count++;
        }
        if (isUsingWildcard && eventsPerStream.length > 1) {
            for (EventBean anEventsPerStream : eventsPerStream) {
                props.put(selectExprContext.getColumnNames()[count], anEventsPerStream);
                count++;
            }
        }

        return processSpecific(props, eventsPerStream, isNewData, exprEvaluatorContext);
    }
}