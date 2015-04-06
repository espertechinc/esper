/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.core.context.factory;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.activator.ViewableActivator;
import com.espertech.esper.core.context.subselect.SubSelectStrategyCollection;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.ResultSetProcessor;
import com.espertech.esper.epl.core.ResultSetProcessorFactoryDesc;
import com.espertech.esper.epl.spec.OnTriggerSplitStreamDesc;
import com.espertech.esper.epl.spec.StatementSpecCompiled;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.View;
import com.espertech.esper.view.internal.RouteResultView;

import java.util.List;

public class StatementAgentInstanceFactoryOnTriggerSplit extends StatementAgentInstanceFactoryOnTriggerBase {
    private final StatementAgentInstanceFactoryOnTriggerSplitDesc splitDesc;
    private final EventType activatorResultEventType;
    private final String[] insertIntoTableNames;

    public StatementAgentInstanceFactoryOnTriggerSplit(StatementContext statementContext, StatementSpecCompiled statementSpec, EPServicesContext services, ViewableActivator activator, SubSelectStrategyCollection subSelectStrategyCollection, StatementAgentInstanceFactoryOnTriggerSplitDesc splitDesc, EventType activatorResultEventType, String[] insertIntoTableNames) {
        super(statementContext, statementSpec, services, activator, subSelectStrategyCollection);
        this.splitDesc = splitDesc;
        this.activatorResultEventType = activatorResultEventType;
        this.insertIntoTableNames = insertIntoTableNames;
    }

    public OnExprViewResult determineOnExprView(AgentInstanceContext agentInstanceContext, List<StopCallback> stopCallbacks) {
        ResultSetProcessor[] processors = new ResultSetProcessor[splitDesc.getProcessorFactories().length];
        for (int i = 0; i < processors.length; i++) {
            ResultSetProcessorFactoryDesc factory = splitDesc.getProcessorFactories()[i];
            ResultSetProcessor processor = factory.getResultSetProcessorFactory().instantiate(null, null, agentInstanceContext);
            processors[i] = processor;
        }

        TableStateInstance[] tableStateInstances = new TableStateInstance[processors.length];
        for (int i = 0; i < insertIntoTableNames.length; i++) {
            String tableName = insertIntoTableNames[i];
            if (tableName != null) {
                tableStateInstances[i] = agentInstanceContext.getStatementContext().getTableService().getState(tableName, agentInstanceContext.getAgentInstanceId());
            }
        }
        OnTriggerSplitStreamDesc desc = (OnTriggerSplitStreamDesc) statementSpec.getOnTriggerDesc();
        View view = new RouteResultView(desc.isFirst(), activatorResultEventType, statementContext.getEpStatementHandle(), services.getInternalEventRouter(), tableStateInstances, splitDesc.getNamedWindowInsert(), processors, splitDesc.getWhereClauses(), agentInstanceContext);
        return new OnExprViewResult(view, null);
    }

    public View determineFinalOutputView(AgentInstanceContext agentInstanceContext, View onExprView) {
        return onExprView;
    }
}
