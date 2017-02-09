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
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Iterator for the group-by case with a row per group.
 */
public class ResultSetRowPerGroupIterator implements Iterator<EventBean> {
    private final Iterator<EventBean> sourceIterator;
    private final ResultSetProcessorRowPerGroup resultSetProcessor;
    private final AggregationService aggregationService;
    private EventBean nextResult;
    private final EventBean[] eventsPerStream;
    private final Set<Object> priorSeenGroups;
    private final ExprEvaluatorContext exprEvaluatorContext;

    /**
     * Ctor.
     *
     * @param sourceIterator       is the parent view iterator
     * @param resultSetProcessor   for providing results
     * @param aggregationService   for pointing to the right aggregation row
     * @param exprEvaluatorContext context for expression evalauation
     */
    public ResultSetRowPerGroupIterator(Iterator<EventBean> sourceIterator, ResultSetProcessorRowPerGroup resultSetProcessor, AggregationService aggregationService, ExprEvaluatorContext exprEvaluatorContext) {
        this.sourceIterator = sourceIterator;
        this.resultSetProcessor = resultSetProcessor;
        this.aggregationService = aggregationService;
        eventsPerStream = new EventBean[1];
        priorSeenGroups = new HashSet<Object>();
        this.exprEvaluatorContext = exprEvaluatorContext;
    }

    public boolean hasNext() {
        if (nextResult != null) {
            return true;
        }
        findNext();
        if (nextResult != null) {
            return true;
        }
        return false;
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
        while (sourceIterator.hasNext()) {
            EventBean candidate = sourceIterator.next();
            eventsPerStream[0] = candidate;

            Object groupKey = resultSetProcessor.generateGroupKey(eventsPerStream, true);
            aggregationService.setCurrentAccess(groupKey, exprEvaluatorContext.getAgentInstanceId(), null);

            if (resultSetProcessor.getOptionalHavingNode() != null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qHavingClauseNonJoin(candidate);
                }
                Boolean pass = (Boolean) resultSetProcessor.getOptionalHavingNode().evaluate(eventsPerStream, true, exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aHavingClauseJoin(pass);
                }
                if (!pass) {
                    continue;
                }
            }
            if (priorSeenGroups.contains(groupKey)) {
                continue;
            }
            priorSeenGroups.add(groupKey);

            nextResult = resultSetProcessor.getSelectExprProcessor().process(eventsPerStream, true, true, exprEvaluatorContext);

            break;
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
