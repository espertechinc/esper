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
package com.espertech.esper.epl.expression.subquery;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventBeanUtility;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;

/**
 * Represents a subselect in an expression tree.
 */
public class SubselectEvalStrategyRowUnfilteredSelectedGroupedWHaving extends SubselectEvalStrategyRowUnfilteredSelected
        implements SubselectEvalStrategyRow {

    private final ExprEvaluator havingClause;

    public SubselectEvalStrategyRowUnfilteredSelectedGroupedWHaving(ExprEvaluator havingClause) {
        this.havingClause = havingClause;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, ExprSubselectRowNode parent) {

        AggregationService aggregationService = parent.getSubselectAggregationService().getContextPartitionAggregationService(exprEvaluatorContext.getAgentInstanceId());
        Collection<Object> groupKeys = aggregationService.getGroupKeys(exprEvaluatorContext);
        if (groupKeys.isEmpty()) {
            return null;
        }

        EventBean[] events = EventBeanUtility.allocatePerStreamShift(eventsPerStream);
        boolean haveResult = false;
        Object result = null;
        for (Object groupKey : groupKeys) {
            aggregationService.setCurrentAccess(groupKey, exprEvaluatorContext.getAgentInstanceId(), null);
            Boolean pass = (Boolean) havingClause.evaluate(events, newData, exprEvaluatorContext);
            if (pass != null && pass) {
                if (haveResult) {
                    return null;
                }

                result = super.evaluate(eventsPerStream, newData, matchingEvents, exprEvaluatorContext, parent);
                haveResult = true;
            }
        }

        return haveResult ? result : null;
    }

    public Collection evaluateGetCollScalar(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext context, ExprSubselectRowNode parent) {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext context, ExprSubselectRowNode parent) {
        return null;
    }

    public Collection<EventBean> evaluateGetCollEvents(EventBean[] eventsPerStream, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext context, ExprSubselectRowNode parent) {
        AggregationService aggregationService = parent.getSubselectAggregationService().getContextPartitionAggregationService(context.getAgentInstanceId());
        Collection<Object> groupKeys = aggregationService.getGroupKeys(context);
        if (groupKeys.isEmpty()) {
            return null;
        }
        EventBean[] events = EventBeanUtility.allocatePerStreamShift(eventsPerStream);
        Collection<EventBean> result = new ArrayDeque<EventBean>(groupKeys.size());
        for (Object groupKey : groupKeys) {
            aggregationService.setCurrentAccess(groupKey, context.getAgentInstanceId(), null);

            Boolean pass = (Boolean) havingClause.evaluate(events, newData, context);
            if (pass != null && pass) {
                Map<String, Object> row = parent.evaluateRow(events, true, context);
                EventBean event = parent.subselectMultirowType.getEventAdapterService().adapterForTypedMap(row, parent.subselectMultirowType.getEventType());
                result.add(event);
            }
        }
        return result;
    }
}
