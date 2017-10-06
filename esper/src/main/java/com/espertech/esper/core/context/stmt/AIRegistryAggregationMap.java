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
import java.util.HashMap;
import java.util.Map;

public class AIRegistryAggregationMap implements AIRegistryAggregation {
    private final Map<Integer, AggregationService> services;

    public AIRegistryAggregationMap() {
        this.services = new HashMap<Integer, AggregationService>();
    }

    public void assignService(int serviceId, AggregationService aggregationService) {
        services.put(serviceId, aggregationService);
    }

    public void deassignService(int serviceId) {
        services.remove(serviceId);
    }

    public int getInstanceCount() {
        return services.size();
    }

    public void applyEnter(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext) {
        services.get(exprEvaluatorContext.getAgentInstanceId()).applyEnter(eventsPerStream, optionalGroupKeyPerRow, exprEvaluatorContext);
    }

    public void applyLeave(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext) {
        services.get(exprEvaluatorContext.getAgentInstanceId()).applyLeave(eventsPerStream, optionalGroupKeyPerRow, exprEvaluatorContext);
    }

    public void setCurrentAccess(Object groupKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        services.get(agentInstanceId).setCurrentAccess(groupKey, agentInstanceId, null);
    }

    public AggregationService getContextPartitionAggregationService(int agentInstanceId) {
        return services.get(agentInstanceId);
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        services.get(exprEvaluatorContext.getAgentInstanceId()).clearResults(exprEvaluatorContext);
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return services.get(agentInstanceId).getValue(column, agentInstanceId, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return services.get(context.getAgentInstanceId()).getCollectionOfEvents(column, eventsPerStream, isNewData, context);
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return services.get(context.getAgentInstanceId()).getCollectionScalar(column, eventsPerStream, isNewData, context);
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return services.get(context.getAgentInstanceId()).getEventBean(column, eventsPerStream, isNewData, context);
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
        return services.get(agentInstanceId).getGroupKey(agentInstanceId);
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        return services.get(exprEvaluatorContext.getAgentInstanceId()).getGroupKeys(exprEvaluatorContext);
    }

    public void stop() {
    }
}
