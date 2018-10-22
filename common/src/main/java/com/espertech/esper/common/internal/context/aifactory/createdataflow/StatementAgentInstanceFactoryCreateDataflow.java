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
package com.espertech.esper.common.internal.context.aifactory.createdataflow;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactory;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpFactoryInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.common.internal.view.core.ViewableDefaultImpl;

import java.util.Map;

public class StatementAgentInstanceFactoryCreateDataflow implements StatementAgentInstanceFactory, StatementReadyCallback {

    private Viewable viewable;
    private DataflowDesc dataflow;

    public void setEventType(EventType eventType) {
        this.viewable = new ViewableDefaultImpl(eventType);
    }

    public void setDataflow(DataflowDesc dataflow) {
        this.dataflow = dataflow;
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        for (Map.Entry<Integer, DataFlowOperatorFactory> entry : dataflow.getOperatorFactories().entrySet()) {
            entry.getValue().initializeFactory(new DataFlowOpFactoryInitializeContext(dataflow.getDataflowName(), entry.getKey(), statementContext));
        }

        dataflow.setStatementContext(statementContext);
        statementContext.getStatementContextRuntimeServices().getDataflowService().addDataflow(statementContext.getDeploymentId(), dataflow);
    }

    public void statementCreate(StatementContext statementContext) {
    }

    public void statementDestroy(StatementContext statementContext) {
        statementContext.getStatementContextRuntimeServices().getDataflowService().removeDataflow(statementContext.getDeploymentId(), dataflow);
    }

    public StatementAgentInstanceFactoryResult newContext(AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        return new StatementAgentInstanceFactoryCreateDataflowResult(viewable, AgentInstanceStopCallbackNoAction.INSTANCE, agentInstanceContext, dataflow);
    }

    public AIRegistryRequirements getRegistryRequirements() {
        return AIRegistryRequirements.noRequirements();
    }

    public EventType getStatementEventType() {
        return viewable.getEventType();
    }

    public StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId) {
        return AgentInstanceUtil.newLock(statementContext);
    }
}
