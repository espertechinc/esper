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
package com.espertech.esper.core.context.stmt;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

public class AIRegistryAggregationSingle implements AIRegistryAggregation, AggregationService {
    private AggregationService service;

    public AIRegistryAggregationSingle() {
    }

    public void assignService(int serviceId, AggregationService aggregationService) {
        service = aggregationService;
    }

    public void deassignService(int serviceId) {
        service = null;
    }

    public int getInstanceCount() {
        return service == null ? 0 : 1;
    }

    public void applyEnter(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext) {
        service.applyEnter(eventsPerStream, optionalGroupKeyPerRow, exprEvaluatorContext);
    }

    public void applyLeave(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext) {
        service.applyLeave(eventsPerStream, optionalGroupKeyPerRow, exprEvaluatorContext);
    }

    public void setCurrentAccess(Object groupKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        service.setCurrentAccess(groupKey, agentInstanceId, null);
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        service.clearResults(exprEvaluatorContext);
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return service.getValue(column, agentInstanceId, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return service.getCollectionOfEvents(column, eventsPerStream, isNewData, context);
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return service.getCollectionScalar(column, eventsPerStream, isNewData, context);
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return service.getEventBean(column, eventsPerStream, isNewData, context);
    }

    public void setRemovedCallback(AggregationRowRemovedCallback callback) {
        // not applicable
    }

    public void accept(AggregationServiceVisitor visitor) {
        throw new UnsupportedOperationException("Not applicable");
    }

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor) {
        throw new UnsupportedOperationException("Not applicable");
    }

    public boolean isGrouped() {
        throw new UnsupportedOperationException("Not applicable");
    }

    public Object getGroupKey(int agentInstanceId) {
        return service.getGroupKey(agentInstanceId);
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        return service.getGroupKeys(exprEvaluatorContext);
    }

    public AggregationService getContextPartitionAggregationService(int agentInstanceId) {
        return service;
    }

    public void stop() {
    }
}
