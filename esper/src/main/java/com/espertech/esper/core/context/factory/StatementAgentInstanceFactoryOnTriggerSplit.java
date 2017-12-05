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
import com.espertech.esper.core.context.activator.ViewableActivator;
import com.espertech.esper.core.context.subselect.SubSelectStrategyCollection;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.start.EPStatementStartMethodOnTriggerItem;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactoryDesc;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.spec.OnTriggerSplitStreamDesc;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.View;
import com.espertech.esper.view.internal.RouteResultView;

import java.util.List;

public class StatementAgentInstanceFactoryOnTriggerSplit extends StatementAgentInstanceFactoryOnTriggerBase {
    private final EPStatementStartMethodOnTriggerItem[] items;
    private final EventType activatorResultEventType;
    private final ExprEvaluator[] whereClauseEvals;

    public StatementAgentInstanceFactoryOnTriggerSplit(StatementContext statementContext, StatementSpecCompiled statementSpec, EPServicesContext services, ViewableActivator activator, SubSelectStrategyCollection subSelectStrategyCollection, EPStatementStartMethodOnTriggerItem[] items, EventType activatorResultEventType) {
        super(statementContext, statementSpec, services, activator, subSelectStrategyCollection);
        this.items = items;
        this.activatorResultEventType = activatorResultEventType;

        whereClauseEvals = new ExprEvaluator[items.length];
        for (int i = 0; i < items.length; i++) {
            whereClauseEvals[i] = items[i].getWhereClause() == null ? null : ExprNodeCompiler.allocateEvaluator(items[i].getWhereClause().getForge(), statementContext.getEngineImportService(), StatementAgentInstanceFactoryOnTriggerSplit.class, false, statementContext.getStatementName());
        }
    }

    public OnExprViewResult determineOnExprView(AgentInstanceContext agentInstanceContext, List<StopCallback> stopCallbacks, boolean isRecoveringReslient) {
        ResultSetProcessor[] processors = new ResultSetProcessor[items.length];
        for (int i = 0; i < processors.length; i++) {
            ResultSetProcessorFactoryDesc factory = items[i].getFactoryDesc();
            ResultSetProcessor processor = factory.getResultSetProcessorFactory().instantiate(null, null, agentInstanceContext);
            processors[i] = processor;
        }

        TableStateInstance[] tableStateInstances = new TableStateInstance[processors.length];
        for (int i = 0; i < items.length; i++) {
            String tableName = items[i].getInsertIntoTableNames();
            if (tableName != null) {
                tableStateInstances[i] = agentInstanceContext.getStatementContext().getTableService().getState(tableName, agentInstanceContext.getAgentInstanceId());
            }
        }

        OnTriggerSplitStreamDesc desc = (OnTriggerSplitStreamDesc) statementSpec.getOnTriggerDesc();
        View view = new RouteResultView(desc.isFirst(), activatorResultEventType, statementContext.getEpStatementHandle(), services.getInternalEventRouter(), tableStateInstances, items, processors, whereClauseEvals, agentInstanceContext);
        return new OnExprViewResult(view, null);
    }

    public View determineFinalOutputView(AgentInstanceContext agentInstanceContext, View onExprView) {
        return onExprView;
    }
}
