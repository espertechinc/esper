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
package com.espertech.esper.core.context.subselect;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactoryDesc;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.prev.ExprPreviousEvalStrategy;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategy;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.join.hint.IndexHint;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.lookup.*;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.epl.named.NamedWindowProcessorInstance;
import com.espertech.esper.epl.named.NamedWindowRootView;
import com.espertech.esper.epl.subquery.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.table.mgmt.TableServiceImpl;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.StatementStopCallback;
import com.espertech.esper.view.StatementStopService;
import com.espertech.esper.view.Viewable;
import org.slf4j.Logger;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Entry holding lookup resource references for use by {@link SubSelectActivationCollection}.
 */
public class SubSelectStrategyFactoryIndexShare implements SubSelectStrategyFactory {
    private final NamedWindowProcessor optionalNamedWindowProcessor;
    private final TableMetadata optionalTableMetadata;
    private final ExprEvaluator filterExprEval;
    private final AggregationServiceFactoryDesc aggregationServiceFactory;
    private final ExprEvaluator[] groupByKeys;
    private final TableService tableService;
    private SubordinateQueryPlanDesc queryPlan;

    public SubSelectStrategyFactoryIndexShare(final String statementName, int statementId, int subqueryNum, EventType[] outerEventTypesSelect, final NamedWindowProcessor optionalNamedWindowProcessor, TableMetadata optionalTableMetadata, boolean fullTableScan, IndexHint optionalIndexHint, SubordPropPlan joinedPropPlan, ExprEvaluator filterExprEval, AggregationServiceFactoryDesc aggregationServiceFactory, ExprEvaluator[] groupByKeys, TableService tableService, Annotation[] annotations, StatementStopService statementStopService, EngineImportService engineImportService) throws ExprValidationException {
        this.optionalNamedWindowProcessor = optionalNamedWindowProcessor;
        this.optionalTableMetadata = optionalTableMetadata;
        this.filterExprEval = filterExprEval;
        this.aggregationServiceFactory = aggregationServiceFactory;
        this.groupByKeys = groupByKeys;
        this.tableService = tableService;

        boolean isLogging;
        Logger log;
        if (optionalTableMetadata != null) {
            isLogging = optionalTableMetadata.isQueryPlanLogging();
            log = TableServiceImpl.getQueryPlanLog();
            queryPlan = SubordinateQueryPlanner.planSubquery(outerEventTypesSelect, joinedPropPlan, false, fullTableScan, optionalIndexHint, true, subqueryNum,
                    false, optionalTableMetadata.getEventTableIndexMetadataRepo(), optionalTableMetadata.getUniqueKeyProps(), true, statementName, statementId, annotations);
            if (queryPlan != null) {
                for (int i = 0; i < queryPlan.getIndexDescs().length; i++) {
                    optionalTableMetadata.addIndexReference(queryPlan.getIndexDescs()[i].getIndexName(), statementName);
                }
            }
        } else {
            isLogging = optionalNamedWindowProcessor.getRootView().isQueryPlanLogging();
            log = NamedWindowRootView.getQueryPlanLog();
            queryPlan = SubordinateQueryPlanner.planSubquery(outerEventTypesSelect, joinedPropPlan, false, fullTableScan, optionalIndexHint, true, subqueryNum,
                    optionalNamedWindowProcessor.isVirtualDataWindow(), optionalNamedWindowProcessor.getEventTableIndexMetadataRepo(), optionalNamedWindowProcessor.getOptionalUniqueKeyProps(), false, statementName, statementId, annotations);
            if (queryPlan != null && queryPlan.getIndexDescs() != null) {
                SubordinateQueryPlannerUtil.addIndexMetaAndRef(queryPlan.getIndexDescs(), optionalNamedWindowProcessor.getEventTableIndexMetadataRepo(), statementName);
                statementStopService.addSubscriber(new StatementStopCallback() {
                    public void statementStopped() {
                        for (int i = 0; i < queryPlan.getIndexDescs().length; i++) {
                            boolean last = optionalNamedWindowProcessor.getEventTableIndexMetadataRepo().removeIndexReference(queryPlan.getIndexDescs()[i].getIndexMultiKey(), statementName);
                            if (last) {
                                optionalNamedWindowProcessor.getEventTableIndexMetadataRepo().removeIndex(queryPlan.getIndexDescs()[i].getIndexMultiKey());
                                optionalNamedWindowProcessor.removeAllInstanceIndexes(queryPlan.getIndexDescs()[i].getIndexMultiKey());
                            }
                        }
                    }
                });
            }
        }
        SubordinateQueryPlannerUtil.queryPlanLogOnSubq(isLogging, log, queryPlan, subqueryNum, annotations, engineImportService);
    }

    public SubSelectStrategyRealization instantiate(EPServicesContext services,
                                                    Viewable viewableRoot,
                                                    AgentInstanceContext agentInstanceContext,
                                                    List<StopCallback> stopCallbackList,
                                                    int subqueryNumber, boolean isRecoveringResilient) {

        SubselectAggregationPreprocessorBase subselectAggregationPreprocessor = null;

        AggregationService aggregationService = null;
        if (aggregationServiceFactory != null) {
            aggregationService = aggregationServiceFactory.getAggregationServiceFactory().makeService(agentInstanceContext, agentInstanceContext.getEngineImportService(), true, subqueryNumber);
            if (groupByKeys == null) {
                if (filterExprEval == null) {
                    subselectAggregationPreprocessor = new SubselectAggregationPreprocessorUnfilteredUngrouped(aggregationService, filterExprEval, null);
                } else {
                    subselectAggregationPreprocessor = new SubselectAggregationPreprocessorFilteredUngrouped(aggregationService, filterExprEval, null);
                }
            } else {
                if (filterExprEval == null) {
                    subselectAggregationPreprocessor = new SubselectAggregationPreprocessorUnfilteredGrouped(aggregationService, filterExprEval, groupByKeys);
                } else {
                    subselectAggregationPreprocessor = new SubselectAggregationPreprocessorFilteredGrouped(aggregationService, filterExprEval, groupByKeys);
                }
            }
        }

        SubordTableLookupStrategy subqueryLookup;
        if (optionalNamedWindowProcessor != null) {
            NamedWindowProcessorInstance instance = optionalNamedWindowProcessor.getProcessorInstance(agentInstanceContext);
            if (queryPlan == null) {
                if (instance.getRootViewInstance().isQueryPlanLogging() && NamedWindowRootView.getQueryPlanLog().isInfoEnabled()) {
                    NamedWindowRootView.getQueryPlanLog().info("shared, full table scan");
                }
                subqueryLookup = new SubordFullTableScanLookupStrategyLocking(instance.getRootViewInstance().getDataWindowContents(), agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock());
            } else {
                EventTable[] tables = null;
                if (!optionalNamedWindowProcessor.isVirtualDataWindow()) {
                    tables = SubordinateQueryPlannerUtil.realizeTables(queryPlan.getIndexDescs(), instance.getRootViewInstance().getEventType(), instance.getRootViewInstance().getIndexRepository(), instance.getRootViewInstance().getDataWindowContents(), agentInstanceContext, isRecoveringResilient);
                }
                SubordTableLookupStrategy strategy = queryPlan.getLookupStrategyFactory().makeStrategy(tables, instance.getRootViewInstance().getVirtualDataWindow());
                subqueryLookup = new SubordIndexedTableLookupStrategyLocking(strategy, instance.getTailViewInstance().getAgentInstanceContext().getAgentInstanceLock());
            }
        } else {
            TableStateInstance state = tableService.getState(optionalTableMetadata.getTableName(), agentInstanceContext.getAgentInstanceId());
            Lock lock = agentInstanceContext.getStatementContext().isWritesToTables() ?
                    state.getTableLevelRWLock().writeLock() : state.getTableLevelRWLock().readLock();
            if (queryPlan == null) {
                subqueryLookup = new SubordFullTableScanTableLookupStrategy(lock, state.getIterableTableScan());
            } else {
                EventTable[] indexes = new EventTable[queryPlan.getIndexDescs().length];
                for (int i = 0; i < indexes.length; i++) {
                    indexes[i] = state.getIndexRepository().getIndexByDesc(queryPlan.getIndexDescs()[i].getIndexMultiKey());
                }
                subqueryLookup = queryPlan.getLookupStrategyFactory().makeStrategy(indexes, null);
                subqueryLookup = new SubordIndexedTableLookupTableStrategy(subqueryLookup, lock);
            }
        }

        return new SubSelectStrategyRealization(subqueryLookup, subselectAggregationPreprocessor, aggregationService, Collections.<ExprPriorNode, ExprPriorEvalStrategy>emptyMap(), Collections.<ExprPreviousNode, ExprPreviousEvalStrategy>emptyMap(), null, null);
    }
}
