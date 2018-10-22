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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.onsplit;

import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.StatementAgentInstanceFactoryOnTriggerBase;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.epl.ontrigger.InfraOnExprBaseViewResult;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.view.core.View;

import java.util.List;

public class StatementAgentInstanceFactoryOnTriggerSplitStream extends StatementAgentInstanceFactoryOnTriggerBase implements StatementReadyCallback {
    private OnSplitItemEval[] items;
    private boolean isFirst;
    private StatementContext statementContext;

    public void setItems(OnSplitItemEval[] items) {
        this.items = items;
    }

    public void setFirst(boolean first) {
        isFirst = first;
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        this.statementContext = statementContext;
    }

    public InfraOnExprBaseViewResult determineOnExprView(AgentInstanceContext agentInstanceContext, List<AgentInstanceStopCallback> stopCallbacks, boolean isRecoveringReslient) {
        ResultSetProcessor[] processors = new ResultSetProcessor[items.length];
        for (int i = 0; i < processors.length; i++) {
            ResultSetProcessorFactoryProvider factory = items[i].getRspFactoryProvider();
            ResultSetProcessor processor = factory.getResultSetProcessorFactory().instantiate(null, null, agentInstanceContext);
            processors[i] = processor;
        }

        TableInstance[] tableStateInstances = new TableInstance[processors.length];
        for (int i = 0; i < items.length; i++) {
            Table table = items[i].getInsertIntoTable();
            if (table != null) {
                tableStateInstances[i] = table.getTableInstance(agentInstanceContext.getAgentInstanceId());
            }
        }

        View view = new RouteResultView(isFirst, getStatementEventType(), statementContext.getEpStatementHandle(), statementContext.getInternalEventRouter(), tableStateInstances, items, processors, agentInstanceContext);
        return new InfraOnExprBaseViewResult(view, null);
    }

    public View determineFinalOutputView(AgentInstanceContext agentInstanceContext, View onExprView) {
        return onExprView;
    }

    public StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId) {
        return AgentInstanceUtil.newLock(statementContext);
    }
}
