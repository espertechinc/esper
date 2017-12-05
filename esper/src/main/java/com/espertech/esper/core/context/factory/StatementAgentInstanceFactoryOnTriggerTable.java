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
package com.espertech.esper.core.context.factory;

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.activator.ViewableActivator;
import com.espertech.esper.core.context.subselect.SubSelectStrategyCollection;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.start.EPStatementStartMethodHelperUtil;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactoryDesc;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.epl.join.hint.IndexHint;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategy;
import com.espertech.esper.epl.lookup.SubordinateQueryPlanner;
import com.espertech.esper.epl.lookup.SubordinateQueryPlannerUtil;
import com.espertech.esper.epl.lookup.SubordinateWMatchExprQueryPlanResult;
import com.espertech.esper.epl.spec.OnTriggerType;
import com.espertech.esper.epl.spec.OnTriggerWindowDesc;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableServiceImpl;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.epl.table.onaction.TableOnView;
import com.espertech.esper.epl.table.onaction.TableOnViewFactory;
import com.espertech.esper.epl.view.OutputProcessViewFactory;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.View;

import java.util.List;

public class StatementAgentInstanceFactoryOnTriggerTable extends StatementAgentInstanceFactoryOnTriggerBase {

    private final ResultSetProcessorFactoryDesc resultSetProcessorPrototype;
    private final ResultSetProcessorFactoryDesc outputResultSetProcessorPrototype;
    private final OutputProcessViewFactory outputProcessViewFactory;
    private final TableOnViewFactory onExprFactory;
    private final SubordinateWMatchExprQueryPlanResult queryPlanResult;

    public StatementAgentInstanceFactoryOnTriggerTable(StatementContext statementContext, StatementSpecCompiled statementSpec, EPServicesContext services, ViewableActivator activator, SubSelectStrategyCollection subSelectStrategyCollection, ResultSetProcessorFactoryDesc resultSetProcessorPrototype, ExprNode validatedJoin, TableOnViewFactory onExprFactory, EventType activatorResultEventType, TableMetadata tableMetadata, ResultSetProcessorFactoryDesc outputResultSetProcessorPrototype, OutputProcessViewFactory outputProcessViewFactory)
            throws ExprValidationException {
        super(statementContext, statementSpec, services, activator, subSelectStrategyCollection);
        this.resultSetProcessorPrototype = resultSetProcessorPrototype;
        this.onExprFactory = onExprFactory;
        this.outputResultSetProcessorPrototype = outputResultSetProcessorPrototype;
        this.outputProcessViewFactory = outputProcessViewFactory;

        StatementAgentInstanceFactoryOnTriggerNamedWindow.IndexHintPair pair = StatementAgentInstanceFactoryOnTriggerNamedWindow.getIndexHintPair(statementContext, statementSpec);
        IndexHint indexHint = pair.getIndexHint();
        ExcludePlanHint excludePlanHint = pair.getExcludePlanHint();

        queryPlanResult = SubordinateQueryPlanner.planOnExpression(
                validatedJoin, activatorResultEventType, indexHint, true, -1, excludePlanHint,
                false, tableMetadata.getEventTableIndexMetadataRepo(), tableMetadata.getInternalEventType(),
                tableMetadata.getUniqueKeyProps(), true, statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getEngineImportService());
        if (queryPlanResult.getIndexDescs() != null) {
            for (int i = 0; i < queryPlanResult.getIndexDescs().length; i++) {
                tableMetadata.addIndexReference(queryPlanResult.getIndexDescs()[i].getIndexName(), statementContext.getStatementName());
            }
        }
        SubordinateQueryPlannerUtil.queryPlanLogOnExpr(tableMetadata.isQueryPlanLogging(), TableServiceImpl.getQueryPlanLog(),
                queryPlanResult, statementContext.getAnnotations(), statementContext.getEngineImportService());
    }

    public OnExprViewResult determineOnExprView(AgentInstanceContext agentInstanceContext, List<StopCallback> stopCallbacks, boolean isRecoveringReslient) {
        OnTriggerWindowDesc onTriggerWindowDesc = (OnTriggerWindowDesc) statementSpec.getOnTriggerDesc();

        // get result set processor and aggregation services
        Pair<ResultSetProcessor, AggregationService> pair = EPStatementStartMethodHelperUtil.startResultSetAndAggregation(resultSetProcessorPrototype, agentInstanceContext, false, null);

        TableStateInstance state = services.getTableService().getState(onTriggerWindowDesc.getWindowName(), agentInstanceContext.getAgentInstanceId());
        EventTable[] indexes;
        if (queryPlanResult.getIndexDescs() == null) {
            indexes = null;
        } else {
            indexes = new EventTable[queryPlanResult.getIndexDescs().length];
            for (int i = 0; i < indexes.length; i++) {
                indexes[i] = state.getIndexRepository().getIndexByDesc(queryPlanResult.getIndexDescs()[i].getIndexMultiKey());
            }
        }
        SubordWMatchExprLookupStrategy strategy = queryPlanResult.getFactory().realize(indexes, agentInstanceContext, state.getIterableTableScan(), null);
        TableOnView onExprBaseView = onExprFactory.make(strategy, state, agentInstanceContext, pair.getFirst());

        return new OnExprViewResult(onExprBaseView, pair.getSecond());
    }

    public View determineFinalOutputView(AgentInstanceContext agentInstanceContext, View onExprView) {
        if ((statementSpec.getOnTriggerDesc().getOnTriggerType() == OnTriggerType.ON_DELETE) ||
                (statementSpec.getOnTriggerDesc().getOnTriggerType() == OnTriggerType.ON_UPDATE) ||
                (statementSpec.getOnTriggerDesc().getOnTriggerType() == OnTriggerType.ON_MERGE)) {

            ResultSetProcessor outputResultSetProcessor = outputResultSetProcessorPrototype.getResultSetProcessorFactory().instantiate(null, null, agentInstanceContext);
            View outputView = outputProcessViewFactory.makeView(outputResultSetProcessor, agentInstanceContext);
            onExprView.addView(outputView);
            return outputView;
        }

        return onExprView;
    }
}
