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
package com.espertech.esper.epl.core.resultset.rowperevent;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator for aggregation results that aggregate all rows.
 */
public class ResultSetProcessorRowPerEventIterator implements Iterator<EventBean> {
    private final Iterator<EventBean> sourceIterator;
    private final ResultSetProcessorRowPerEvent resultSetProcessor;
    private EventBean nextResult;
    private final EventBean[] eventsPerStream;
    private ExprEvaluatorContext exprEvaluatorContext;

    /**
     * Ctor.
     *
     * @param sourceIterator       is the parent iterator
     * @param resultSetProcessor   for getting outgoing rows
     * @param exprEvaluatorContext context for expression evalauation
     */
    public ResultSetProcessorRowPerEventIterator(Iterator<EventBean> sourceIterator, ResultSetProcessorRowPerEvent resultSetProcessor, ExprEvaluatorContext exprEvaluatorContext) {
        this.sourceIterator = sourceIterator;
        this.resultSetProcessor = resultSetProcessor;
        eventsPerStream = new EventBean[1];
        this.exprEvaluatorContext = exprEvaluatorContext;
    }

    public boolean hasNext() {
        if (nextResult != null) {
            return true;
        }
        findNext();
        return nextResult != null;
    }

    public EventBean next() {
        if (nextResult != null) {
            EventBean result = nextResult;
            nextResult = null;
            return result;
        }
        findNext();
        if (nextResult != null) {
            EventBean result = nextResult;
            nextResult = null;
            return result;
        }
        throw new NoSuchElementException();
    }

    private void findNext() {
        nextResult = null;
        if (!resultSetProcessor.hasHavingClause()) {
            if (sourceIterator.hasNext()) {
                EventBean candidate = sourceIterator.next();
                eventsPerStream[0] = candidate;
                nextResult = resultSetProcessor.getSelectExprProcessor().process(eventsPerStream, true, true, exprEvaluatorContext);
            }
        } else {
            while (sourceIterator.hasNext()) {
                EventBean candidate = sourceIterator.next();
                eventsPerStream[0] = candidate;
                if (!resultSetProcessor.evaluateHavingClause(eventsPerStream, true, exprEvaluatorContext)) {
                    continue;
                }
                nextResult = resultSetProcessor.getSelectExprProcessor().process(eventsPerStream, true, true, exprEvaluatorContext);
                break;
            }
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
