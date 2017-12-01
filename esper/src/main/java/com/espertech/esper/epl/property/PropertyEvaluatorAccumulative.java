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
package com.espertech.esper.epl.property;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;

/**
 * A property evaluator that returns a full row of events for each stream, i.e. flattened inner-join results for
 * property-upon-property.
 */
public class PropertyEvaluatorAccumulative {
    private static final Logger log = LoggerFactory.getLogger(PropertyEvaluatorAccumulative.class);

    private final ContainedEventEval[] containedEventEvals;
    private final FragmentEventType[] fragmentEventType;
    private final ExprEvaluator[] whereClauses;
    private final int lastLevel;
    private final int levels;
    private final List<String> propertyNames;

    /**
     * Ctor.
     *
     * @param containedEventEvals property getters or other evaluators
     * @param fragmentEventType   property fragment types
     * @param whereClauses        filters, if any
     * @param propertyNames       the property names that are staggered
     */
    public PropertyEvaluatorAccumulative(ContainedEventEval[] containedEventEvals, FragmentEventType[] fragmentEventType, ExprEvaluator[] whereClauses, List<String> propertyNames) {
        this.fragmentEventType = fragmentEventType;
        this.containedEventEvals = containedEventEvals;
        this.whereClauses = whereClauses;
        lastLevel = fragmentEventType.length - 1;
        levels = fragmentEventType.length + 1;
        this.propertyNames = propertyNames;
    }

    /**
     * Returns the accumulative events for the input event.
     *
     * @param theEvent             is the input event
     * @param exprEvaluatorContext expression evaluation context
     * @return events per stream for each row
     */
    public ArrayDeque<EventBean[]> getAccumulative(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        ArrayDeque<EventBean[]> resultEvents = new ArrayDeque<EventBean[]>();
        EventBean[] eventsPerStream = new EventBean[levels];
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

            if (fragmentEventType[level].isIndexed()) {
                EventBean[] fragments = (EventBean[]) result;
                if (level == lastLevel) {
                    if (whereClauses[level] != null) {
                        for (EventBean theEvent : fragments) {
                            eventsPerStream[level + 1] = theEvent;
                            if (ExprNodeUtilityCore.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
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
                            if (ExprNodeUtilityCore.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
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
                        if (ExprNodeUtilityCore.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
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
                        if (ExprNodeUtilityCore.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
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
                    propertyNames.get(level + 1) + "': " + ex.getMessage(), ex);
        }
    }
}
