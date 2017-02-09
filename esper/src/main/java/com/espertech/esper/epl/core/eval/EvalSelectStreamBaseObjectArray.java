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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class EvalSelectStreamBaseObjectArray extends EvalSelectStreamBase implements SelectExprProcessor {

    private static final Logger log = LoggerFactory.getLogger(EvalSelectStreamBaseObjectArray.class);

    public EvalSelectStreamBaseObjectArray(SelectExprContext selectExprContext, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard) {
        super(selectExprContext, resultEventType, namedStreams, usingWildcard);
    }

    public abstract EventBean processSpecific(Object[] props, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext);

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        // Evaluate all expressions and build a map of name-value pairs
        int size = (isUsingWildcard && eventsPerStream.length > 1) ? eventsPerStream.length : 0;
        size += selectExprContext.getExpressionNodes().length + namedStreams.size();
        Object[] props = new Object[size];
        int count = 0;
        for (ExprEvaluator expressionNode : selectExprContext.getExpressionNodes()) {
            Object evalResult = expressionNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            props[count] = evalResult;
            count++;
        }
        for (SelectClauseStreamCompiledSpec element : namedStreams) {
            EventBean theEvent = eventsPerStream[element.getStreamNumber()];
            props[count] = theEvent;
            count++;
        }
        if (isUsingWildcard && eventsPerStream.length > 1) {
            for (EventBean anEventsPerStream : eventsPerStream) {
                props[count] = anEventsPerStream;
                count++;
            }
        }

        return processSpecific(props, eventsPerStream, exprEvaluatorContext);
    }
}
