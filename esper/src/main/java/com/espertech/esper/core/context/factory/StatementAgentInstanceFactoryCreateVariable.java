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
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.ViewableDefaultImpl;

public class StatementAgentInstanceFactoryCreateVariable extends StatementAgentInstanceFactoryBase {
    private final StatementContext statementContext;
    private final EPServicesContext services;
    private final VariableMetaData variableMetaData;
    private final EventType eventType;

    public StatementAgentInstanceFactoryCreateVariable(StatementContext statementContext, EPServicesContext services, VariableMetaData variableMetaData, EventType eventType) {
        super(statementContext.getAnnotations());
        this.statementContext = statementContext;
        this.services = services;
        this.variableMetaData = variableMetaData;
        this.eventType = eventType;
    }

    public StatementAgentInstanceFactoryCreateVariableResult newContextInternal(final AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient)
    {
        StopCallback stopCallback = new StopCallback() {
            public void stop() {
                services.getVariableService().deallocateVariableState(variableMetaData.getVariableName(), agentInstanceContext.getAgentInstanceId());
            }
        };
        services.getVariableService().allocateVariableState(variableMetaData.getVariableName(), agentInstanceContext.getAgentInstanceId(), statementContext.getStatementExtensionServicesContext());
        return new StatementAgentInstanceFactoryCreateVariableResult(new ViewableDefaultImpl(eventType), stopCallback, agentInstanceContext);
    }
}
