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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityEvaluate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Collection;

/**
 * A property evaluator that returns a full row of events for each stream, i.e. flattened inner-join results for
 * property-upon-property.
 */
public class PropertyEvaluatorAccumulative {
    private static final Logger log = LoggerFactory.getLogger(PropertyEvaluatorAccumulative.class);

    private ContainedEventEval[] containedEventEvals;
    private boolean[] fragmentEventTypeIsIndexed;
    private ExprEvaluator[] whereClauses;
    private String[] propertyNames;

    public void setPropertyNames(String[] propertyNames) {
        this.propertyNames = propertyNames;
    }

    public void setContainedEventEvals(ContainedEventEval[] containedEventEvals) {
        this.containedEventEvals = containedEventEvals;
    }

    public void setFragmentEventTypeIsIndexed(boolean[] fragmentEventTypeIsIndexed) {
        this.fragmentEventTypeIsIndexed = fragmentEventTypeIsIndexed;
    }

    public void setWhereClauses(ExprEvaluator[] whereClauses) {
        this.whereClauses = whereClauses;
    }

    /**
     * Returns the accumulative events for the input event.
     *
     * @param theEvent             is the input event
     * @param exprEvaluatorContext expression evaluation context
     * @return events per stream for each row
     */
    public ArrayDeque<EventBean[]> getAccumulative(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        ArrayDeque<EventBean[]> resultEvents = new ArrayDeque<>();
        EventBean[] eventsPerStream = new EventBean[fragmentEventTypeIsIndexed.length + 1];
        eventsPerStream[0] = theEvent;
        populateEvents(eventsPerStream, theEvent, 0, resultEvents, exprEvaluatorContext);
        if (resultEvents.isEmpty()) {
            return null;
        }
        return resultEvents;
    }

    private void populateEvents(EventBean[] eventsPerStream, EventBean branch, int level, Collection<EventBean[]> events, ExprEvaluatorContext exprEvaluatorContext) {
        try {
            Object result = containedEventEvals[level].getFragment(branch, eventsPerStream, exprEvaluatorContext);
            int lastLevel = fragmentEventTypeIsIndexed.length - 1;
            int levels = fragmentEventTypeIsIndexed.length + 1;

            if (fragmentEventTypeIsIndexed[level]) {
                EventBean[] fragments = (EventBean[]) result;
                if (level == lastLevel) {
                    if (whereClauses[level] != null) {
                        for (EventBean theEvent : fragments) {
                            eventsPerStream[level + 1] = theEvent;
                            if (ExprNodeUtilityEvaluate.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
                                EventBean[] eventsPerRow = new EventBean[levels];
                                System.arraycopy(eventsPerStream, 0, eventsPerRow, 0, levels);
                                events.add(eventsPerRow);
                            }
                        }
                    } else {
                        for (EventBean theEvent : fragments) {
                            eventsPerStream[level + 1] = theEvent;
                            EventBean[] eventsPerRow = new EventBean[levels];
                            System.arraycopy(eventsPerStream, 0, eventsPerRow, 0, levels);
                            events.add(eventsPerRow);
                        }
                    }
                } else {
                    if (whereClauses[level] != null) {
                        for (EventBean next : fragments) {
                            eventsPerStream[level + 1] = next;
                            if (ExprNodeUtilityEvaluate.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
                                populateEvents(eventsPerStream, next, level + 1, events, exprEvaluatorContext);
                            }
                        }
                    } else {
                        for (EventBean next : fragments) {
                            eventsPerStream[level + 1] = next;
                            populateEvents(eventsPerStream, next, level + 1, events, exprEvaluatorContext);
                        }
                    }
                }
            } else {
                EventBean fragment = (EventBean) result;
                if (level == lastLevel) {
                    if (whereClauses[level] != null) {
                        eventsPerStream[level + 1] = fragment;
                        if (ExprNodeUtilityEvaluate.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
                            EventBean[] eventsPerRow = new EventBean[levels];
                            System.arraycopy(eventsPerStream, 0, eventsPerRow, 0, levels);
                            events.add(eventsPerRow);
                        }
                    } else {
                        eventsPerStream[level + 1] = fragment;
                        EventBean[] eventsPerRow = new EventBean[levels];
                        System.arraycopy(eventsPerStream, 0, eventsPerRow, 0, levels);
                        events.add(eventsPerRow);
                    }
                } else {
                    if (whereClauses[level] != null) {
                        eventsPerStream[level + 1] = fragment;
                        if (ExprNodeUtilityEvaluate.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
                            populateEvents(eventsPerStream, fragment, level + 1, events, exprEvaluatorContext);
                        }
                    } else {
                        eventsPerStream[level + 1] = fragment;
                        populateEvents(eventsPerStream, fragment, level + 1, events, exprEvaluatorContext);
                    }
                }
            }
        } catch (RuntimeException ex) {
            log.error("Unexpected error evaluating property expression for event of type '" +
                    branch.getEventType().getName() +
                    "' and property '" +
                    propertyNames[level + 1] + "': " + ex.getMessage(), ex);
        }
    }
}
