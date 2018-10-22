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
package com.espertech.esper.common.internal.context.aifactory.createvariable;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactory;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryUtil;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewSimpleWProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;
import com.espertech.esper.common.internal.epl.variable.core.VariableReader;
import com.espertech.esper.common.internal.util.NullableObject;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.context.util.StatementCPCacheService.DEFAULT_AGENT_INSTANCE_ID;

public class StatementAgentInstanceFactoryCreateVariable implements StatementAgentInstanceFactory, StatementReadyCallback {

    private String variableName;
    private ExprEvaluator variableInitialValueExpr;
    private ResultSetProcessorFactoryProvider resultSetProcessorFactoryProvider;

    public void setResultSetProcessorFactoryProvider(ResultSetProcessorFactoryProvider resultSetProcessorFactoryProvider) {
        this.resultSetProcessorFactoryProvider = resultSetProcessorFactoryProvider;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setVariableInitialValueExpr(ExprEvaluator variableInitialValueExpr) {
        this.variableInitialValueExpr = variableInitialValueExpr;
    }

    public ResultSetProcessorFactoryProvider getResultSetProcessorFactoryProvider() {
        return resultSetProcessorFactoryProvider;
    }

    public EventType getStatementEventType() {
        return resultSetProcessorFactoryProvider.getResultEventType();
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        VariableMetaData meta = moduleIncidentals.getVariables().get(variableName);
        if (meta == null) {
            throw new UnsupportedOperationException("Missing variable information '" + variableName + "'");
        }

        // evaluate initial value
        if (meta.getValueWhenAvailable() == null && variableInitialValueExpr != null && meta.getOptionalContextName() == null && !recovery) {
            Object initialValue = variableInitialValueExpr.evaluate(null, true, new ExprEvaluatorContextStatement(statementContext, false));
            VariableManagementService svc = statementContext.getVariableManagementService();
            svc.checkAndWrite(statementContext.getDeploymentId(), variableName, DEFAULT_AGENT_INSTANCE_ID, initialValue);
            svc.commit();
            svc.setLocalVersion();
        }
    }

    public void statementCreate(StatementContext statementContext) {
    }

    public void statementDestroy(StatementContext statementContext) {
        statementContext.getVariableManagementService().removeVariableIfFound(statementContext.getDeploymentId(), variableName);
    }

    public StatementAgentInstanceFactoryResult newContext(AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        VariableManagementService variableService = agentInstanceContext.getVariableManagementService();
        String deploymentId = agentInstanceContext.getDeploymentId();
        int agentInstanceId = agentInstanceContext.getAgentInstanceId();
        List<AgentInstanceStopCallback> stopCallbacks = new ArrayList<>(2);

        // allocate state
        // for create-variable with contexts we allocate on new-context
        if (agentInstanceContext.getContextProperties() != null) {
            NullableObject<Object> initialValue = null;
            if (variableInitialValueExpr != null) {
                initialValue = new NullableObject<>(variableInitialValueExpr.evaluate(null, true, agentInstanceContext));
            }
            agentInstanceContext.getVariableManagementService().allocateVariableState(agentInstanceContext.getDeploymentId(), variableName, agentInstanceContext.getAgentInstanceId(), isRecoveringResilient, initialValue, agentInstanceContext.getEventBeanTypedEventFactory());
        }
        stopCallbacks.add(new AgentInstanceStopCallback() {
            public void stop(AgentInstanceStopServices services) {
                services.getAgentInstanceContext().getVariableManagementService().deallocateVariableState(services.getAgentInstanceContext().getDeploymentId(), variableName, agentInstanceContext.getAgentInstanceId());
            }
        });

        // register callback for listener-updates
        VariableReader reader = variableService.getReader(deploymentId, variableName, agentInstanceContext.getAgentInstanceId());
        CreateVariableView createVariableView = new CreateVariableView(this, agentInstanceContext, reader);
        variableService.registerCallback(deploymentId, variableName, agentInstanceContext.getAgentInstanceId(), createVariableView);
        stopCallbacks.add(new AgentInstanceStopCallback() {
            public void stop(AgentInstanceStopServices services) {
                services.getAgentInstanceContext().getVariableManagementService().unregisterCallback(deploymentId, variableName, agentInstanceId, createVariableView);
            }
        });

        // create result-processing
        Pair<ResultSetProcessor, AggregationService> pair = StatementAgentInstanceFactoryUtil.startResultSetAndAggregation(resultSetProcessorFactoryProvider, agentInstanceContext, false, null);
        OutputProcessViewSimpleWProcessor out = new OutputProcessViewSimpleWProcessor(agentInstanceContext, pair.getFirst());
        out.setParent(createVariableView);
        createVariableView.setChild(out);

        AgentInstanceStopCallback stopCallback = AgentInstanceUtil.finalizeSafeStopCallbacks(stopCallbacks);
        return new StatementAgentInstanceFactoryCreateVariableResult(out, stopCallback, agentInstanceContext);
    }

    public AIRegistryRequirements getRegistryRequirements() {
        return AIRegistryRequirements.noRequirements();
    }

    public StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId) {
        return AgentInstanceUtil.newLock(statementContext);
    }
}
