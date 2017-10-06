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
import com.espertech.esper.collection.ArrayWrap;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

public class AIRegistryAggregationMultiPerm implements AIRegistryAggregation {
    private final ArrayWrap<AggregationService> services;
    private int count;

    public AIRegistryAggregationMultiPerm() {
        this.services = new ArrayWrap<AggregationService>(AggregationService.class, 2);
    }

    public void assignService(int serviceId, AggregationService aggregationService) {
        AIRegistryUtil.checkExpand(serviceId, services);
        services.getArray()[serviceId] = aggregationService;
        count++;
    }

    public void deassignService(int serviceId) {
        if (serviceId >= services.getArray().length) {
            // possible since it may not have been assigned as there was nothing to assign
            return;
        }
        services.getArray()[serviceId] = null;
        count--;
    }

    public int getInstanceCount() {
        return count;
    }

    public void applyEnter(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext) {
        services.getArray()[exprEvaluatorContext.getAgentInstanceId()].applyEnter(eventsPerStream, optionalGroupKeyPerRow, exprEvaluatorContext);
    }

    public void applyLeave(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext) {
        services.getArray()[exprEvaluatorContext.getAgentInstanceId()].applyLeave(eventsPerStream, optionalGroupKeyPerRow, exprEvaluatorContext);
    }

    public void setCurrentAccess(Object groupKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        services.getArray()[agentInstanceId].setCurrentAccess(groupKey, agentInstanceId, null);
    }

    public AggregationService getContextPartitionAggregationService(int agentInstanceId) {
        return services.getArray()[agentInstanceId];
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        services.getArray()[exprEvaluatorContext.getAgentInstanceId()].clearResults(exprEvaluatorContext);
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return services.getArray()[agentInstanceId].getValue(column, agentInstanceId, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return services.getArray()[context.getAgentInstanceId()].getCollectionOfEvents(column, eventsPerStream, isNewData, context);
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return services.getArray()[context.getAgentInstanceId()].getCollectionScalar(column, eventsPerStream, isNewData, context);
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return services.getArray()[context.getAgentInstanceId()].getEventBean(column, eventsPerStream, isNewData, context);
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
        return services.getArray()[agentInstanceId].getGroupKey(agentInstanceId);
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        return services.getArray()[exprEvaluatorContext.getAgentInstanceId()].getGroupKeys(exprEvaluatorContext);
    }

    public void stop() {
    }
}
