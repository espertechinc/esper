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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.filterspec.PropertyEvaluator;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A property evaluator that considers nested properties and that considers where-clauses
 * but does not consider select-clauses.
 */
public class PropertyEvaluatorNested implements PropertyEvaluator {
    private final ContainedEventEval[] containedEventEvals;
    private final FragmentEventType[] fragmentEventType;
    private final ExprEvaluator[] whereClauses;
    private final EventBean[] eventsPerStream;
    private final int lastLevel;
    private final List<String> expressionTexts;

    /**
     * Ctor.
     *
     * @param containedEventEvals property getters or other evaluators
     * @param fragmentEventType   the fragments
     * @param whereClauses        the where clauses
     * @param expressionTexts     the property names that are staggered
     */
    public PropertyEvaluatorNested(ContainedEventEval[] containedEventEvals, FragmentEventType[] fragmentEventType, ExprEvaluator[] whereClauses, List<String> expressionTexts) {
        this.fragmentEventType = fragmentEventType;
        this.containedEventEvals = containedEventEvals;
        this.whereClauses = whereClauses;
        lastLevel = fragmentEventType.length - 1;
        eventsPerStream = new EventBean[fragmentEventType.length + 1];
        this.expressionTexts = expressionTexts;
    }

    public EventBean[] getProperty(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        ArrayDeque<EventBean> resultEvents = new ArrayDeque<EventBean>();
        eventsPerStream[0] = theEvent;
        populateEvents(theEvent, 0, resultEvents, exprEvaluatorContext);
        if (resultEvents.isEmpty()) {
            return null;
        }
        return resultEvents.toArray(new EventBean[resultEvents.size()]);
    }

    private void populateEvents(EventBean branch, int level, Collection<EventBean> events, ExprEvaluatorContext exprEvaluatorContext) {
        try {
            Object result = containedEventEvals[level].getFragment(branch, eventsPerStream, exprEvaluatorContext);

            if (fragmentEventType[level].isIndexed()) {
                EventBean[] fragments = (EventBean[]) result;
                if (level == lastLevel) {
                    if (whereClauses[level] != null) {
                        for (EventBean theEvent : fragments) {
                            eventsPerStream[level + 1] = theEvent;
                            if (ExprNodeUtilityCore.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
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
                            if (ExprNodeUtilityCore.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
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
                        if (ExprNodeUtilityCore.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
                            events.add(fragment);
                        }
                    } else {
                        events.add(fragment);
                    }
                } else {
                    if (whereClauses[level] != null) {
                        eventsPerStream[level + 1] = fragment;
                        if (ExprNodeUtilityCore.applyFilterExpression(whereClauses[level], eventsPerStream, exprEvaluatorContext)) {
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
                    expressionTexts.get(level + 1) + "': " + ex.getMessage();
            throw new EPException(message, ex);
        }
    }

    public EventType getFragmentEventType() {
        return fragmentEventType[lastLevel].getFragmentType();
    }

    public boolean compareTo(PropertyEvaluator otherEval) {
        return false;
    }
}
