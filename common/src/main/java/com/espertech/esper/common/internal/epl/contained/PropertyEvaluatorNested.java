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
package com.espertech.esper.common.internal.epl.contained;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityEvaluate;
import com.espertech.esper.common.internal.filterspec.PropertyEvaluator;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;

/**
 * A property evaluator that considers nested properties and that considers where-clauses
 * but does not consider select-clauses.
 */
public class PropertyEvaluatorNested implements PropertyEvaluator {
    private ContainedEventEval[] containedEventEvals;
    private boolean[] fragmentEventTypeIsIndexed;
    private ExprEvaluator[] whereClauses;
    private EventBean[] eventsPerStream;
    private String[] expressionTexts;
    private EventType resultEventType;

    public EventBean[] getProperty(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        ArrayDeque<EventBean> resultEvents = new ArrayDeque<EventBean>();
        eventsPerStream[0] = theEvent;
        populateEvents(theEvent, 0, resultEvents, exprEvaluatorContext);
        if (resultEvents.isEmpty()) {
            return null;
        }
        return resultEvents.toArray(new EventBean[resultEvents.size()]);
    }

    public EventType getFragmentEventType() {
        return resultEventType;
    }

    public boolean compareTo(PropertyEvaluator otherEval) {
        return false;
    }

    public void setContainedEventEvals(ContainedEventEval[] containedEventEvals) {
        this.containedEventEvals = containedEventEvals;
    }

    public void setFragmentEventTypeIsIndexed(boolean[] fragmentEventTypeIsIndexed) {
        this.fragmentEventTypeIsIndexed = fragmentEventTypeIsIndexed;
        eventsPerStream = new EventBean[fragmentEventTypeIsIndexed.length + 1];
    }

    public void setWhereClauses(ExprEvaluator[] whereClauses) {
        this.whereClauses = whereClauses;
    }

    public void setExpressionTexts(String[] expressionTexts) {
        this.expressionTexts = expressionTexts;
    }

    public void setResultEventType(EventType resultEventType) {
        this.resultEventType = resultEventType;
    }

    private void populateEvents(EventBean branch, int level, Collection<EventBean> events, ExprEvaluatorContext exprEvaluatorContext) {
        try {
            Object result = containedEventEvals[level].getFragment(branch, eventsPerStream, exprEvaluatorContext);
            int lastLevel = fragmentEventTypeIsIndexed.length - 1;

            if (fragmentEventTypeIsIndexed[level]) {
                EventBean[] fragments = (EventBean[]) result;
                if (level == lastLevel) {
                    if (whereClauses[level] != null) {
                        for (EventBean theEvent : fragments) {
                            eventsPerStream[level + 1] = theEvent;
                            if (ExprNodeUtilityEvaluate.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
                                events.add(theEvent);
                            }
                        }
                    } else {
                        events.addAll(Arrays.asList(fragments));
                    }
                } else {
                    if (whereClauses[level] != null) {
                        for (EventBean next : fragments) {
                            eventsPerStream[level + 1] = next;
                            if (ExprNodeUtilityEvaluate.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
                                populateEvents(next, level + 1, events, exprEvaluatorContext);
                            }
                        }
                    } else {
                        for (EventBean next : fragments) {
                            eventsPerStream[level + 1] = next;
                            populateEvents(next, level + 1, events, exprEvaluatorContext);
                        }
                    }
                }
            } else {
                EventBean fragment = (EventBean) result;
                if (level == lastLevel) {
                    if (whereClauses[level] != null) {
                        eventsPerStream[level + 1] = fragment;
                        if (ExprNodeUtilityEvaluate.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
                            events.add(fragment);
                        }
                    } else {
                        events.add(fragment);
                    }
                } else {
                    if (whereClauses[level] != null) {
                        eventsPerStream[level + 1] = fragment;
                        if (ExprNodeUtilityEvaluate.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
                            populateEvents(fragment, level + 1, events, exprEvaluatorContext);
                        }
                    } else {
                        eventsPerStream[level + 1] = fragment;
                        populateEvents(fragment, level + 1, events, exprEvaluatorContext);
                    }
                }
            }
        } catch (RuntimeException ex) {
            String message = "Unexpected error evaluating property expression for event of type '" +
                    branch.getEventType().getName() +
                    "' and property '" +
                    expressionTexts[level + 1] + "': " + ex.getMessage();
            throw new EPException(message, ex);
        }
    }
}
