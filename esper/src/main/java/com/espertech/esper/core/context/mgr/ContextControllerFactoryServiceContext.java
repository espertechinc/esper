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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.epl.spec.ContextDetail;

public class ContextControllerFactoryServiceContext {
    private final String contextName;
    private final EPServicesContext servicesContext;
    private final ContextDetail detail;
    private final AgentInstanceContext agentInstanceContextCreate;
    private final boolean isRecoveringResilient;
    private final EventType statementResultEventType;

    public ContextControllerFactoryServiceContext(String contextName, EPServicesContext servicesContext, ContextDetail detail, AgentInstanceContext agentInstanceContextCreate, boolean isRecoveringResilient, EventType statementResultEventType) {
        this.contextName = contextName;
        this.servicesContext = servicesContext;
        this.detail = detail;
        this.agentInstanceContextCreate = agentInstanceContextCreate;
        this.isRecoveringResilient = isRecoveringResilient;
        this.statementResultEventType = statementResultEventType;
    }

    public String getContextName() {
        return contextName;
    }

    public EPServicesContext getServicesContext() {
        return servicesContext;
    }

    public ContextDetail getDetail() {
        return detail;
    }

    public AgentInstanceContext getAgentInstanceContextCreate() {
        return agentInstanceContextCreate;
    }

    public boolean isRecoveringResilient() {
        return isRecoveringResilient;
    }

    public EventType getStatementResultEventType() {
        return statementResultEventType;
    }
}
