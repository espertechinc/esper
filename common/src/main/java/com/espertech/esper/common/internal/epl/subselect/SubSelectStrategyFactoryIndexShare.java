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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.lookup.*;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordinateQueryPlanDesc;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowInstance;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.List;
import java.util.concurrent.locks.Lock;

public class SubSelectStrategyFactoryIndexShare implements SubSelectStrategyFactory {

    private NamedWindow namedWindow;
    private Table table;
    private SubordinateQueryPlanDesc queryPlan;
    private AggregationServiceFactory aggregationServiceFactory;
    private ExprEvaluator groupKeyEval;
    private ExprEvaluator filterExprEval;

    public void setNamedWindow(NamedWindow namedWindow) {
        this.namedWindow = namedWindow;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public void setQueryPlan(SubordinateQueryPlanDesc queryPlan) {
        this.queryPlan = queryPlan;
    }

    public void setAggregationServiceFactory(AggregationServiceFactory aggregationServiceFactory) {
        this.aggregationServiceFactory = aggregationServiceFactory;
    }

    public void setGroupKeyEval(ExprEvaluator groupKeyEval) {
        this.groupKeyEval = groupKeyEval;
    }

    public void setFilterExprEval(ExprEvaluator filterExprEval) {
        this.filterExprEval = filterExprEval;
    }

    public void ready(SubSelectStrategyFactoryContext subselectFactoryContext, EventType eventType) {
        // no action
    }

    public SubSelectStrategyRealization instantiate(Viewable viewableRoot, AgentInstanceContext agentInstanceContext, List<AgentInstanceStopCallback> stopCallbackList, int subqueryNumber, boolean isRecoveringResilient) {
        SubselectAggregationPreprocessorBase subselectAggregationPreprocessor = null;

        AggregationService aggregationService = null;
        if (aggregationServiceFactory != null) {
            aggregationService = aggregationServiceFactory.makeService(agentInstanceContext, agentInstanceContext.getClasspathImportServiceRuntime(), true, subqueryNumber, null);

            final AggregationService aggregationServiceStoppable = aggregationService;
            stopCallbackList.add(new AgentInstanceStopCallback() {
                public void stop(AgentInstanceStopServices services) {
                    aggregationServiceStoppable.stop();
                }
            });

            if (groupKeyEval == null) {
                if (filterExprEval == null) {
                    subselectAggregationPreprocessor = new SubselectAggregationPreprocessorUnfilteredUngrouped(aggregationService, filterExprEval, null);
                } else {
                    subselectAggregationPreprocessor = new SubselectAggregationPreprocessorFilteredUngrouped(aggregationService, filterExprEval, null);
                }
            } else {
                if (filterExprEval == null) {
                    subselectAggregationPreprocessor = new SubselectAggregationPreprocessorUnfilteredGrouped(aggregationService, filterExprEval, groupKeyEval);
                } else {
                    subselectAggregationPreprocessor = new SubselectAggregationPreprocessorFilteredGrouped(aggregationService, filterExprEval, groupKeyEval);
                }
            }
        }

        SubordTableLookupStrategy subqueryLookup;
        if (namedWindow != null) {
            NamedWindowInstance instance = namedWindow.getNamedWindowInstance(agentInstanceContext);
            if (queryPlan == null) {
                subqueryLookup = new SubordFullTableScanLookupStrategyLocking(instance.getRootViewInstance().getDataWindowContents(), agentInstanceContext.getEpStatementAgentInstanceHandle().getStatementAgentInstanceLock());
            } else {
                EventTable[] indexes = new EventTable[queryPlan.getIndexDescs().length];
                for (int i = 0; i < indexes.length; i++) {
                    indexes[i] = instance.getRootViewInstance().getIndexRepository().getIndexByDesc(queryPlan.getIndexDescs()[i].getIndexMultiKey());
                }
                subqueryLookup = queryPlan.getLookupStrategyFactory().makeStrategy(indexes, agentInstanceContext, instance.getRootViewInstance().getVirtualDataWindow());
                subqueryLookup = new SubordIndexedTableLookupStrategyLocking(subqueryLookup, instance.getTailViewInstance().getAgentInstanceContext().getAgentInstanceLock());
            }
        } else {
            TableInstance instance = table.getTableInstance(agentInstanceContext.getAgentInstanceId());
            Lock lock = agentInstanceContext.getStatementContext().getStatementInformationals().isWritesToTables() ?
                    instance.getTableLevelRWLock().writeLock() : instance.getTableLevelRWLock().readLock();
            if (queryPlan == null) {
                subqueryLookup = new SubordFullTableScanTableLookupStrategy(lock, instance.getIterableTableScan());
            } else {
                EventTable[] indexes = new EventTable[queryPlan.getIndexDescs().length];
                for (int i = 0; i < indexes.length; i++) {
                    indexes[i] = instance.getIndexRepository().getIndexByDesc(queryPlan.getIndexDescs()[i].getIndexMultiKey());
                }
                subqueryLookup = queryPlan.getLookupStrategyFactory().makeStrategy(indexes, agentInstanceContext, null);
                subqueryLookup = new SubordIndexedTableLookupTableStrategy(subqueryLookup, lock);
            }
        }

        return new SubSelectStrategyRealization(subqueryLookup, subselectAggregationPreprocessor, aggregationService, null, null, null, null);
    }

    public LookupStrategyDesc getLookupStrategyDesc() {
        return queryPlan == null ? LookupStrategyDesc.SCAN : queryPlan.getLookupStrategyFactory().getLookupStrategyDesc();
    }
}
