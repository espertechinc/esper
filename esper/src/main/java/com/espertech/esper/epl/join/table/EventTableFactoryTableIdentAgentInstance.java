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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;

public class EventTableFactoryTableIdentAgentInstance implements EventTableFactoryTableIdent {
    private final AgentInstanceContext agentInstanceContext;

    public EventTableFactoryTableIdentAgentInstance(AgentInstanceContext agentInstanceContext) {
        this.agentInstanceContext = agentInstanceContext;
    }

    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public StatementContext getStatementContext() {
        return agentInstanceContext.getStatementContext();
    }
}
