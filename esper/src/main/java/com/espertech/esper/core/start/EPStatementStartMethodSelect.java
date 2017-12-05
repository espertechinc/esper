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

import com.espertech.esper.core.context.factory.StatementAgentInstanceFactorySelectResult;
import com.espertech.esper.core.context.factory.StatementAgentInstancePreload;
import com.espertech.esper.core.context.mgr.ContextManagedStatementSelectDesc;
import com.espertech.esper.core.context.stmt.*;
import com.espertech.esper.core.context.subselect.SubSelectStrategyFactoryDesc;
import com.espertech.esper.core.context.subselect.SubSelectStrategyHolder;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.ContextMergeView;
import com.espertech.esper.core.context.util.StatementAgentInstanceUtil;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.resource.StatementResourceHolder;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.prev.ExprPreviousEvalStrategy;
import com.espertech.esper.epl.expression.prev.ExprPreviousMatchRecognizeNode;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategy;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.spec.IntoTableSpec;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.rowregex.EventRowRegexHelper;
import com.espertech.esper.rowregex.EventRowRegexNFAViewService;
import com.espertech.esper.rowregex.RegexExprPreviousEvalStrategy;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.ViewProcessingException;
import com.espertech.esper.view.Viewable;

import java.util.*;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPStatementStartMethodSelect extends EPStatementStartMethodBase {
    public EPStatementStartMethodSelect(StatementSpecCompiled statementSpec) {
        super(statementSpec);
    }

    public EPStatementStartResult startInternal(final EPServicesContext services, final StatementContext statementContext, boolean isNewStatement, boolean isRecoveringStatement, boolean isRecoveringResilient) throws ExprValidationException, ViewProcessingException {

        // validate use of table: may not both read and write
        validateTableAccessUse(statementSpec.getIntoTableSpec(), statementSpec.getTableNodes());

        final String contextName = statementSpec.getOptionalContextName();
        AgentInstanceContext defaultAgentInstanceContext = getDefaultAgentInstanceContext(statementContext);
        EPStatementStartMethodSelectDesc selectDesc = EPStatementStartMethodSelectUtil.prepare(statementSpec, services, statementContext, isRecoveringResilient, defaultAgentInstanceContext, isQueryPlanLogging(services), null, null, null);
        statementContext.setStatementAgentInstanceFactory(selectDesc.getStatementAgentInstanceFactorySelect());

        // allow extension to walk
        statementContext.getStatementExtensionServicesContext().preStartWalk(selectDesc);

        // Determine context
        EPStatementStopMethod stopStatementMethod;
        Viewable finalViewable;
        AggregationService aggregationService;
        Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategyInstances;
        Map<ExprPriorNode, ExprPriorEvalStrategy> priorStrategyInstances;
        Map<ExprPreviousNode, ExprPreviousEvalStrategy> previousStrategyInstances;
        Map<ExprTableAccessNode, ExprTableAccessEvalStrategy> tableAccessStrategyInstances;
        List<StatementAgentInstancePreload> preloadList = Collections.emptyList();
        RegexExprPreviousEvalStrategy matchRecognizePrevEvalStrategy;

        // With context - delegate instantiation to context
        if (statementSpec.getOptionalContextName() != null) {

            // use statement-wide agent-instance-specific aggregation service
            aggregationService = statementContext.getStatementAgentInstanceRegistry().getAgentInstanceAggregationService();

            // use statement-wide agent-instance-specific subselects
            AIRegistryExpr aiRegistryExpr = statementContext.getStatementAgentInstanceRegistry().getAgentInstanceExprService();

            subselectStrategyInstances = new HashMap<ExprSubselectNode, SubSelectStrategyHolder>();
            for (Map.Entry<ExprSubselectNode, SubSelectStrategyFactoryDesc> entry : selectDesc.getSubSelectStrategyCollection().getSubqueries().entrySet()) {
                AIRegistrySubselect specificService = aiRegistryExpr.allocateSubselect(entry.getKey());
                entry.getKey().setStrategy(specificService);

                Map<ExprPriorNode, ExprPriorEvalStrategy> subselectPriorStrategies = new HashMap<ExprPriorNode, ExprPriorEvalStrategy>();
                for (ExprPriorNode subselectPrior : entry.getValue().getPriorNodesList()) {
                    AIRegistryPrior specificSubselectPriorService = aiRegistryExpr.allocatePrior(subselectPrior);
                    subselectPriorStrategies.put(subselectPrior, specificSubselectPriorService);
                }

                Map<ExprPreviousNode, ExprPreviousEvalStrategy> subselectPreviousStrategies = new HashMap<ExprPreviousNode, ExprPreviousEvalStrategy>();
                for (ExprPreviousNode subselectPrevious : entry.getValue().getPrevNodesList()) {
                    AIRegistryPrevious specificSubselectPreviousService = aiRegistryExpr.allocatePrevious(subselectPrevious);
                    subselectPreviousStrategies.put(subselectPrevious, specificSubselectPreviousService);
                }

                AIRegistryAggregation subselectAggregation = aiRegistryExpr.allocateSubselectAggregation(entry.getKey());
                SubSelectStrategyHolder strategyHolder = new SubSelectStrategyHolder(specificService, subselectAggregation, subselectPriorStrategies, subselectPreviousStrategies, null, null, null);
                subselectStrategyInstances.put(entry.getKey(), strategyHolder);
            }

            // use statement-wide agent-instance-specific "prior"
            priorStrategyInstances = new HashMap<ExprPriorNode, ExprPriorEvalStrategy>();
            for (ExprPriorNode priorNode : selectDesc.getViewResourceDelegateUnverified().getPriorRequests()) {
                AIRegistryPrior specificService = aiRegistryExpr.allocatePrior(priorNode);
                priorStrategyInstances.put(priorNode, specificService);
            }

            // use statement-wide agent-instance-specific "previous"
            previousStrategyInstances = new HashMap<ExprPreviousNode, ExprPreviousEvalStrategy>();
            for (ExprPreviousNode previousNode : selectDesc.getViewResourceDelegateUnverified().getPreviousRequests()) {
                AIRegistryPrevious specificService = aiRegistryExpr.allocatePrevious(previousNode);
                previousStrategyInstances.put(previousNode, specificService);
            }

            // use statement-wide agent-instance-specific match-recognize "previous"
            matchRecognizePrevEvalStrategy = aiRegistryExpr.allocateMatchRecognizePrevious();

            // use statement-wide agent-instance-specific tables
            tableAccessStrategyInstances = new HashMap<ExprTableAccessNode, ExprTableAccessEvalStrategy>();
            if (statementSpec.getTableNodes() != null) {
                for (ExprTableAccessNode tableNode : statementSpec.getTableNodes()) {
                    AIRegistryTableAccess specificService = aiRegistryExpr.allocateTableAccess(tableNode);
                    tableAccessStrategyInstances.put(tableNode, specificService);
                }
            }

            ContextMergeView mergeView = new ContextMergeView(selectDesc.getResultSetProcessorPrototypeDesc().getResultEventType());
            finalViewable = mergeView;

            ContextManagedStatementSelectDesc statement = new ContextManagedStatementSelectDesc(statementSpec, statementContext, mergeView, selectDesc.getStatementAgentInstanceFactorySelect(),
                    selectDesc.getResultSetProcessorPrototypeDesc().getAggregationServiceFactoryDesc().getExpressions(),
                    selectDesc.getSubSelectStrategyCollection());
            services.getContextManagementService().addStatement(contextName, statement, isRecoveringResilient);
            final EPStatementStopMethod selectStop = selectDesc.getStopMethod();
            stopStatementMethod = new EPStatementStopMethod() {
                public void stop() {
                    services.getContextManagementService().stoppedStatement(contextName, statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getExpression(), statementContext.getExceptionHandlingService());
                    selectStop.stop();
                }
            };

            selectDesc.getDestroyCallbacks().addCallback(new EPStatementDestroyCallbackContext(services.getContextManagementService(), contextName, statementContext.getStatementName(), statementContext.getStatementId()));
        } else {
            // Without context - start here
            StatementAgentInstanceFactorySelectResult resultOfStart = (StatementAgentInstanceFactorySelectResult) selectDesc.getStatementAgentInstanceFactorySelect().newContext(defaultAgentInstanceContext, isRecoveringResilient);
            finalViewable = resultOfStart.getFinalView();

            final StopCallback startResultStop = services.getEpStatementFactory().makeStopMethod(resultOfStart);
            final EPStatementStopMethod selectStop = selectDesc.getStopMethod();
            stopStatementMethod = new EPStatementStopMethod() {
                public void stop() {
                    StatementAgentInstanceUtil.stopSafe(startResultStop, statementContext);
                    selectStop.stop();
                }
            };

            aggregationService = resultOfStart.getOptionalAggegationService();
            subselectStrategyInstances = resultOfStart.getSubselectStrategies();
            priorStrategyInstances = resultOfStart.getPriorNodeStrategies();
            previousStrategyInstances = resultOfStart.getPreviousNodeStrategies();
            tableAccessStrategyInstances = resultOfStart.getTableAccessEvalStrategies();
            preloadList = resultOfStart.getPreloadList();

            matchRecognizePrevEvalStrategy = null;
            if (resultOfStart.getTopViews().length > 0) {
                EventRowRegexNFAViewService matchRecognize = EventRowRegexHelper.recursiveFindRegexService(resultOfStart.getTopViews()[0]);
                if (matchRecognize != null) {
                    matchRecognizePrevEvalStrategy = matchRecognize.getPreviousEvaluationStrategy();
                }
            }

            if (statementContext.getStatementExtensionServicesContext() != null && statementContext.getStatementExtensionServicesContext().getStmtResources() != null) {
                StatementResourceHolder holder = statementContext.getStatementExtensionServicesContext().extractStatementResourceHolder(resultOfStart);
                statementContext.getStatementExtensionServicesContext().getStmtResources().setUnpartitioned(holder);
                statementContext.getStatementExtensionServicesContext().postProcessStart(resultOfStart, isRecoveringResilient);
            }
        }

        Set<ExprPreviousMatchRecognizeNode> matchRecognizeNodes = selectDesc.getStatementAgentInstanceFactorySelect().getViewResourceDelegate().getPerStream()[0].getMatchRecognizePreviousRequests();

        // assign strategies to expression nodes
        EPStatementStartMethodHelperAssignExpr.assignExpressionStrategies(selectDesc, aggregationService, subselectStrategyInstances, priorStrategyInstances, previousStrategyInstances, matchRecognizeNodes, matchRecognizePrevEvalStrategy, tableAccessStrategyInstances);

        // execute preload if any
        for (StatementAgentInstancePreload preload : preloadList) {
            preload.executePreload(defaultAgentInstanceContext);
        }

        // handle association to table
        if (statementSpec.getIntoTableSpec() != null) {
            services.getStatementVariableRefService().addReferences(statementContext.getStatementName(), statementSpec.getIntoTableSpec().getName());
        }

        return new EPStatementStartResult(finalViewable, stopStatementMethod, selectDesc.getDestroyCallbacks());
    }

    private void validateTableAccessUse(IntoTableSpec bindingSpec, ExprTableAccessNode[] tableNodes)
            throws ExprValidationException {
        if (statementSpec.getIntoTableSpec() != null && statementSpec.getTableNodes() != null && statementSpec.getTableNodes().length > 0) {
            for (ExprTableAccessNode node : statementSpec.getTableNodes()) {
                if (node.getTableName().equals(statementSpec.getIntoTableSpec().getName())) {
                    throw new ExprValidationException("Invalid use of table '" + statementSpec.getIntoTableSpec().getName() + "', aggregate-into requires write-only, the expression '" +
                            ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(statementSpec.getTableNodes()[0]) + "' is not allowed");
                }
            }
        }

    }
}
