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
package com.espertech.esper.core.start;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.factory.StatementAgentInstanceFactoryUpdate;
import com.espertech.esper.core.context.factory.StatementAgentInstanceFactoryUpdateResult;
import com.espertech.esper.core.context.mgr.ContextManagedStatementOnTriggerDesc;
import com.espertech.esper.core.context.stmt.AIRegistryExpr;
import com.espertech.esper.core.context.stmt.AIRegistrySubselect;
import com.espertech.esper.core.context.subselect.SubSelectActivationCollection;
import com.espertech.esper.core.context.subselect.SubSelectStrategyCollection;
import com.espertech.esper.core.context.subselect.SubSelectStrategyHolder;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.ContextMergeView;
import com.espertech.esper.core.service.*;
import com.espertech.esper.core.service.resource.StatementResourceHolder;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.ViewProcessingException;
import com.espertech.esper.view.Viewable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPStatementStartMethodUpdate extends EPStatementStartMethodBase {
    public EPStatementStartMethodUpdate(StatementSpecCompiled statementSpec) {
        super(statementSpec);
    }

    public EPStatementStartResult startInternal(final EPServicesContext services, final StatementContext statementContext, boolean isNewStatement, boolean isRecoveringStatement, boolean isRecoveringResilient) throws ExprValidationException, ViewProcessingException {
        // define stop and destroy
        final List<StopCallback> stopCallbacks = new LinkedList<StopCallback>();
        EPStatementDestroyCallbackList destroyCallbacks = new EPStatementDestroyCallbackList();

        // determine context
        final String contextName = statementSpec.getOptionalContextName();
        if (contextName != null) {
            throw new ExprValidationException("Update IStream is not supported in conjunction with a context");
        }

        // First we create streams for subselects, if there are any
        SubSelectActivationCollection subSelectStreamDesc = EPStatementStartMethodHelperSubselect.createSubSelectActivation(services, statementSpec, statementContext, destroyCallbacks);

        final StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[0];
        final UpdateDesc updateSpec = statementSpec.getUpdateSpec();
        String triggereventTypeName;

        if (streamSpec instanceof FilterStreamSpecCompiled) {
            FilterStreamSpecCompiled filterStreamSpec = (FilterStreamSpecCompiled) streamSpec;
            triggereventTypeName = filterStreamSpec.getFilterSpec().getFilterForEventTypeName();
        } else if (streamSpec instanceof NamedWindowConsumerStreamSpec) {
            NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) streamSpec;
            triggereventTypeName = namedSpec.getWindowName();
        } else if (streamSpec instanceof TableQueryStreamSpec) {
            throw new ExprValidationException("Tables cannot be used in an update-istream statement");
        } else {
            throw new ExprValidationException("Unknown stream specification streamEventType: " + streamSpec);
        }

        // determine a stream name
        String streamName = triggereventTypeName;
        if (updateSpec.getOptionalStreamName() != null) {
            streamName = updateSpec.getOptionalStreamName();
        }

        final EventType streamEventType = services.getEventAdapterService().getExistsTypeByName(triggereventTypeName);
        StreamTypeService typeService = new StreamTypeServiceImpl(new EventType[]{streamEventType}, new String[]{streamName}, new boolean[]{true}, services.getEngineURI(), false, false);

        // determine subscriber result types
        ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, false);
        statementContext.getStatementResultService().setSelectClause(new Class[]{streamEventType.getUnderlyingType()}, new String[]{"*"}, false, null, evaluatorContextStmt);

        // Materialize sub-select views
        SubSelectStrategyCollection subSelectStrategyCollection = EPStatementStartMethodHelperSubselect.planSubSelect(services, statementContext, isQueryPlanLogging(services), subSelectStreamDesc, new String[]{streamName}, new EventType[]{streamEventType}, new String[]{triggereventTypeName}, statementSpec.getDeclaredExpressions(), null);

        ExprValidationContext validationContext = new ExprValidationContext(typeService, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, false, false, null, false);
        for (OnTriggerSetAssignment assignment : updateSpec.getAssignments()) {
            ExprNode validated = ExprNodeUtilityRich.getValidatedAssignment(assignment, validationContext);
            assignment.setExpression(validated);
            EPStatementStartMethodHelperValidate.validateNoAggregations(validated, "Aggregation functions may not be used within an update-clause");
        }
        if (updateSpec.getOptionalWhereClause() != null) {
            ExprNode validated = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.WHERE, updateSpec.getOptionalWhereClause(), validationContext);
            updateSpec.setOptionalWhereClause(validated);
            EPStatementStartMethodHelperValidate.validateNoAggregations(validated, "Aggregation functions may not be used within an update-clause");
        }

        // preprocessing view
        InternalRoutePreprocessView onExprView = new InternalRoutePreprocessView(streamEventType, statementContext.getStatementResultService());

        // validation
        InternalEventRouterDesc routerDesc = services.getInternalEventRouter().getValidatePreprocessing(onExprView.getEventType(), updateSpec, statementContext.getAnnotations(), statementContext.getStatementName());

        // create context factory
        StatementAgentInstanceFactoryUpdate contextFactory = new StatementAgentInstanceFactoryUpdate(statementContext, services, streamEventType, updateSpec, onExprView, routerDesc, subSelectStrategyCollection);
        statementContext.setStatementAgentInstanceFactory(contextFactory);

        // perform start of hook-up to start
        Viewable finalViewable;
        EPStatementStopMethod stopStatementMethod;
        Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategyInstances;

        // With context - delegate instantiation to context
        final EPStatementStopMethod stopMethod = new EPStatementStopMethodImpl(statementContext, stopCallbacks);
        if (statementSpec.getOptionalContextName() != null) {

            // use statement-wide agent-instance-specific subselects
            AIRegistryExpr aiRegistryExpr = statementContext.getStatementAgentInstanceRegistry().getAgentInstanceExprService();
            subselectStrategyInstances = new HashMap<ExprSubselectNode, SubSelectStrategyHolder>();
            for (ExprSubselectNode node : subSelectStrategyCollection.getSubqueries().keySet()) {
                AIRegistrySubselect specificService = aiRegistryExpr.allocateSubselect(node);
                node.setStrategy(specificService);
                subselectStrategyInstances.put(node, new SubSelectStrategyHolder(null, null, null, null, null, null, null));
            }

            ContextMergeView mergeView = new ContextMergeView(onExprView.getEventType());
            finalViewable = mergeView;

            ContextManagedStatementOnTriggerDesc statement = new ContextManagedStatementOnTriggerDesc(statementSpec, statementContext, mergeView, contextFactory);
            services.getContextManagementService().addStatement(statementSpec.getOptionalContextName(), statement, isRecoveringResilient);
            stopStatementMethod = new EPStatementStopMethod() {
                public void stop() {
                    services.getContextManagementService().stoppedStatement(contextName, statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getExpression(), statementContext.getExceptionHandlingService());
                    stopMethod.stop();
                }
            };

            destroyCallbacks.addCallback(new EPStatementDestroyCallbackContext(services.getContextManagementService(), statementSpec.getOptionalContextName(), statementContext.getStatementName(), statementContext.getStatementId()));
        } else {
            // Without context - start here
            AgentInstanceContext agentInstanceContext = getDefaultAgentInstanceContext(statementContext);
            final StatementAgentInstanceFactoryUpdateResult resultOfStart = (StatementAgentInstanceFactoryUpdateResult) contextFactory.newContext(agentInstanceContext, isRecoveringResilient);
            finalViewable = resultOfStart.getFinalView();
            final StopCallback stopCallback = services.getEpStatementFactory().makeStopMethod(resultOfStart);
            stopStatementMethod = new EPStatementStopMethod() {
                public void stop() {
                    stopCallback.stop();
                    stopMethod.stop();
                }
            };
            subselectStrategyInstances = resultOfStart.getSubselectStrategies();

            if (statementContext.getStatementExtensionServicesContext() != null && statementContext.getStatementExtensionServicesContext().getStmtResources() != null) {
                StatementResourceHolder holder = statementContext.getStatementExtensionServicesContext().extractStatementResourceHolder(resultOfStart);
                statementContext.getStatementExtensionServicesContext().getStmtResources().setUnpartitioned(holder);
                statementContext.getStatementExtensionServicesContext().postProcessStart(resultOfStart, isRecoveringResilient);
            }
        }

        // assign subquery nodes
        EPStatementStartMethodHelperAssignExpr.assignSubqueryStrategies(subSelectStrategyCollection, subselectStrategyInstances);

        return new EPStatementStartResult(finalViewable, stopStatementMethod, destroyCallbacks);
    }
}
