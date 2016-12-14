/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.agg.service;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;

/**
 * Implementation for handling aggregation without any grouping (no group-by).
 */
public class AggSvcGroupAllNoAccessImpl extends AggregationServiceBaseUngrouped
{
    /**
     * Ctor.
     * @param evaluators - evaluate the sub-expression within the aggregate function (ie. sum(4*myNum))
     * @param aggregators - collect the aggregation state that evaluators evaluate to
     * @param aggregatorFactories - factories
     */
    public AggSvcGroupAllNoAccessImpl(ExprEvaluator evaluators[], AggregationMethod aggregators[], AggregationMethodFactory aggregatorFactories[])
    {
        super(evaluators, aggregators, aggregatorFactories, new AggregationStateFactory[0]);
    }

    public void applyEnter(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggregationUngroupedApplyEnterLeave(true, evaluators.length, 0);}
        for (int j = 0; j < evaluators.length; j++)
        {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggNoAccessEnterLeave(true, j, aggregators[j], aggregatorFactories[j].getAggregationExpression());}
            Object columnResult = evaluators[j].evaluate(eventsPerStream, true, exprEvaluatorContext);
            aggregators[j].enter(columnResult);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggNoAccessEnterLeave(true, j, aggregators[j]);}
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggregationUngroupedApplyEnterLeave(true);}
    }

    public void applyLeave(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggregationUngroupedApplyEnterLeave(false, evaluators.length, 0);}
        for (int j = 0; j < evaluators.length; j++)
        {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qAggNoAccessEnterLeave(false, j, aggregators[j], aggregatorFactories[j].getAggregationExpression());}
            Object columnResult = evaluators[j].evaluate(eventsPerStream, false, exprEvaluatorContext);
            aggregators[j].leave(columnResult);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggNoAccessEnterLeave(false, j, aggregators[j]);}
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aAggregationUngroupedApplyEnterLeave(false);}
    }

    public void setCurrentAccess(Object groupKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel)
    {
        // no action needed - this implementation does not group and the current row is the single group
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext)
    {
        return aggregators[column].getValue();
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext)
    {
        for (AggregationMethod aggregator : aggregators)
        {
            aggregator.clear();
        }
    }

    public void setRemovedCallback(AggregationRowRemovedCallback callback) {
        // not applicable
    }

    public void accept(AggregationServiceVisitor visitor) {
        visitor.visitAggregations(1, aggregators);
    }

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor) {
    }

    public boolean isGrouped() {
        return false;
    }

    public Object getGroupKey(int agentInstanceId) {
        return null;
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }
}
