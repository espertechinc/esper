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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactory;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.aifactory.createcontext.StatementAgentInstanceFactoryCreateContextResult;
import com.espertech.esper.common.internal.context.airegistry.StatementAIResourceRegistry;
import com.espertech.esper.common.internal.context.mgr.ContextControllerStatementDesc;
import com.espertech.esper.common.internal.context.mgr.ContextDeployTimeResolver;
import com.espertech.esper.common.internal.context.module.*;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolder;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementFactoryArgs;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import static com.espertech.esper.runtime.internal.kernel.service.DeployerHelperResolver.reverseDeployment;

public class DeployerHelperStatement {
    private final static Logger log = LoggerFactory.getLogger(DeployerHelperStatement.class);

    static EPStatement[] deployStatements(int rolloutItemNumber, List<StatementLightweight> lightweights, boolean recovery, DeployerModulePaths modulePaths, ModuleProviderCLPair provider, String deploymentId, EPRuntimeSPI epRuntime) throws EPDeployException {
        EPStatement[] statements = new EPStatement[lightweights.size()];
        int count = 0;
        for (StatementLightweight lightweight : lightweights) {

            EPStatementSPI stmt;
            try {
                stmt = DeployerHelperStatement.deployStatement(recovery, lightweight, epRuntime);
            } catch (Throwable t) {
                try {
                    reverseDeployment(deploymentId, modulePaths.getDeploymentTypes(), lightweights, statements, provider, epRuntime.getServicesContext());
                } catch (Throwable udex) {
                    log.warn(udex.getMessage(), udex);
                }
                throw new EPDeployException("Failed to deploy: " + t.getMessage(), t, rolloutItemNumber);
            }

            statements[count++] = stmt;

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qaRuntimeManagementStmtStarted(epRuntime.getURI(), deploymentId, lightweight.getStatementContext().getStatementId(), stmt.getName(),
                    (String) stmt.getProperty(StatementProperty.EPL), epRuntime.getEventService().getCurrentTime());
            }
        }
        return statements;
    }

    private static EPStatementSPI deployStatement(boolean recovery, StatementLightweight lightweight, EPRuntimeSPI epRuntime) {
        // statement-create: safe operation for registering things
        StatementAgentInstanceFactory statementAgentInstanceFactory = lightweight.getStatementContext().getStatementAIFactoryProvider().getFactory();
        statementAgentInstanceFactory.statementCreate(lightweight.getStatementContext());

        // add statement
        EPStatementSPI stmt = makeStatement(lightweight.getStatementContext().getUpdateDispatchView(), lightweight.getStatementContext(), (StatementResultServiceImpl) lightweight.getStatementResultService(), epRuntime);

        // add statement to globals
        epRuntime.getServicesContext().getStatementLifecycleService().addStatement(stmt); // it is now available for lookup

        Viewable finalView;
        StatementDestroyCallback statementDestroyCallback;
        Collection<StatementAgentInstancePreload> preloads = null;
        String contextName = lightweight.getStatementInformationals().getOptionalContextName();

        if (contextName == null) {
            StatementAgentInstanceFactoryResult result = startStatementNoContext(lightweight, recovery, epRuntime.getServicesContext());
            finalView = result.getFinalView();
            preloads = result.getPreloadList();
            boolean createContextStmt = result instanceof StatementAgentInstanceFactoryCreateContextResult;
            statementDestroyCallback = new StatementDestroyCallback() {
                public void destroy(StatementDestroyServices destroyServices, StatementContext statementContext) {
                    // All statements other that create-context: get the agent-instance-context and stop
                    // Create-context statements already got destroyed when the last statement associated to context was removed.
                    if (!createContextStmt) {
                        StatementResourceHolder holder = statementContext.getStatementCPCacheService().makeOrGetEntryCanNull(-1, statementContext);
                        holder.getAgentInstanceStopCallback().stop(new AgentInstanceStopServices(holder.getAgentInstanceContext()));
                    }

                    // Invoke statement-destroy
                    statementAgentInstanceFactory.statementDestroy(lightweight.getStatementContext());
                }
            };

            // assign
            StatementAIFactoryAssignments assignments = new StatementAIFactoryAssignmentsImpl(result.getOptionalAggegationService(), result.getPriorStrategies(),
                result.getPreviousGetterStrategies(), result.getSubselectStrategies(), result.getTableAccessStrategies(), result.getRowRecogPreviousStrategy());
            lightweight.getStatementContext().getStatementAIFactoryProvider().assign(assignments);
        } else {
            String contextModuleName = lightweight.getStatementInformationals().getOptionalContextModuleName();
            StatementAIResourceRegistry statementAIResourceRegistry = lightweight.getStatementContext().getStatementAIResourceRegistry();

            NameAccessModifier contextVisibility = lightweight.getStatementInformationals().getOptionalContextVisibility();
            String contextDeploymentId = ContextDeployTimeResolver.resolveContextDeploymentId(contextModuleName, contextVisibility, contextName, lightweight.getStatementContext().getDeploymentId(), lightweight.getStatementContext().getPathContextRegistry());

            ContextMergeView contextMergeView = lightweight.getStatementInformationals().getStatementType().isOnTriggerInfra() ? new ContextMergeViewForwarding(null) : new ContextMergeView(null);
            finalView = contextMergeView;
            ContextControllerStatementDesc statement = new ContextControllerStatementDesc(lightweight, contextMergeView);

            // assignments before add-statement, since add-statement creates context partitions which may preload
            lightweight.getStatementContext().getStatementAIFactoryProvider().assign(new StatementAIFactoryAssignmentContext(statementAIResourceRegistry));

            // add statement
            epRuntime.getServicesContext().getContextManagementService().addStatement(contextDeploymentId, contextName, statement, recovery);
            statementDestroyCallback = new StatementDestroyCallback() {
                public void destroy(StatementDestroyServices destroyServices, StatementContext statementContext) {
                    epRuntime.getServicesContext().getContextManagementService().stoppedStatement(contextDeploymentId, contextName, statement);
                    statementAgentInstanceFactory.statementDestroy(lightweight.getStatementContext());
                }
            };
        }

        // make dispatch view
        finalView.setChild(lightweight.getStatementContext().getUpdateDispatchView());

        // assign parent view
        stmt.getStatementContext().setDestroyCallback(statementDestroyCallback);
        stmt.setParentView(finalView);

        // execute preloads
        if (preloads != null) {
            for (StatementAgentInstancePreload preload : preloads) {
                preload.executePreload();
            }
        }

        return stmt;
    }

    private static EPStatementSPI makeStatement(UpdateDispatchView dispatchChildView, StatementContext statementContext, StatementResultServiceImpl statementResultService, EPRuntimeSPI epRuntime) {
        EPStatementSPI epStatement = epRuntime.getServicesContext().getEpStatementFactory().statement(new EPStatementFactoryArgs(statementContext, dispatchChildView, statementResultService));
        StatementInformationalsRuntime info = statementContext.getStatementInformationals();
        statementResultService.setSelectClause(info.getSelectClauseTypes(), info.getSelectClauseColumnNames(), info.isForClauseDelivery(), info.getGroupDeliveryEval());
        statementResultService.setContext(epStatement, epRuntime);
        return epStatement;
    }

    private static StatementAgentInstanceFactoryResult startStatementNoContext(StatementLightweight lightweight, boolean recovery, EPServicesContext services) {

        StatementContext statementContext = lightweight.getStatementContext();
        AgentInstanceContext agentInstanceContext = statementContext.makeAgentInstanceContextUnpartitioned();

        // start
        StatementAgentInstanceFactoryResult result = lightweight.getStatementProvider().getStatementAIFactoryProvider().getFactory().newContext(agentInstanceContext, recovery);

        // keep
        StatementResourceHolder holder = services.getStatementResourceHolderBuilder().build(agentInstanceContext, result);
        statementContext.getStatementCPCacheService().getStatementResourceService().setUnpartitioned(holder);

        return result;
    }
}
