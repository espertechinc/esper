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
package com.espertech.esper.common.internal.metrics.audit;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowState;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.pattern.core.EvalFactoryNode;
import com.espertech.esper.common.internal.filterspec.MatchedEventMapMinimal;
import com.espertech.esper.common.internal.schedule.ScheduleHandle;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.view.core.ViewFactory;

public class AuditProviderDefault implements AuditProvider {

    public final static AuditProviderDefault INSTANCE = new AuditProviderDefault();

    private AuditProviderDefault() {
    }

    public boolean activated() {
        return false;
    }

    public void view(EventBean[] newData, EventBean[] oldData, AgentInstanceContext agentInstanceContext, ViewFactory viewFactory) {
    }

    public void stream(EventBean event, ExprEvaluatorContext context, String filterSpecText) {
    }

    public void stream(EventBean[] newData, EventBean[] oldData, ExprEvaluatorContext context, String filterSpecText) {
    }

    public void scheduleAdd(long nextScheduledTime, AgentInstanceContext agentInstanceContext, ScheduleHandle scheduleHandle, ScheduleObjectType type, String name) {
    }

    public void scheduleRemove(AgentInstanceContext agentInstanceContext, ScheduleHandle scheduleHandle, ScheduleObjectType type, String name) {
    }

    public void scheduleFire(AgentInstanceContext agentInstanceContext, ScheduleObjectType type, String name) {
    }

    public void property(String name, Object value, ExprEvaluatorContext exprEvaluatorContext) {
    }

    public void insert(EventBean event, ExprEvaluatorContext exprEvaluatorContext) {
    }

    public void expression(String text, Object value, ExprEvaluatorContext exprEvaluatorContext) {
    }

    public void patternTrue(EvalFactoryNode factoryNode, Object from, MatchedEventMapMinimal matchEvent, boolean isQuitted, AgentInstanceContext agentInstanceContext) {
    }

    public void patternFalse(EvalFactoryNode factoryNode, Object from, AgentInstanceContext agentInstanceContext) {
    }

    public void patternInstance(boolean increase, EvalFactoryNode factoryNode, AgentInstanceContext agentInstanceContext) {
    }

    public void exprdef(String name, Object value, ExprEvaluatorContext exprEvaluatorContext) {
    }

    public void dataflowTransition(String dataflowName, String dataFlowInstanceId, EPDataFlowState state, EPDataFlowState newState, AgentInstanceContext agentInstanceContext) {
    }

    public void dataflowSource(String dataFlowName, String dataFlowInstanceId, String operatorName, int operatorNumber, AgentInstanceContext agentInstanceContext) {
    }

    public void dataflowOp(String dataFlowName, String instanceId, String operatorName, int operatorNumber, Object[] parameters, AgentInstanceContext agentInstanceContext) {
    }

    public void contextPartition(boolean allocate, AgentInstanceContext agentInstanceContext) {
    }
}
