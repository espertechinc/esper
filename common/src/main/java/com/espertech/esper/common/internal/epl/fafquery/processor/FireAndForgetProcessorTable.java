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
package com.espertech.esper.common.internal.epl.fafquery.processor;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;

public class FireAndForgetProcessorTable extends FireAndForgetProcessor {
    private Table table;

    public void setTable(Table table) {
        this.table = table;
    }

    public EventType getEventTypeResultSetProcessor() {
        return table.getMetaData().getPublicEventType();
    }

    public String getContextName() {
        return table.getMetaData().getOptionalContextName();
    }

    public FireAndForgetInstance getProcessorInstance(AgentInstanceContext agentInstanceContext) {
        TableInstance instance;
        if (agentInstanceContext != null) {
            instance = table.getTableInstance(agentInstanceContext.getAgentInstanceId());
        } else {
            instance = table.getTableInstanceNoContext();
        }
        if (instance != null) {
            return new FireAndForgetInstanceTable(instance);
        }
        return null;
    }

    public String getContextDeploymentId() {
        return table.getStatementContextCreateTable().getContextRuntimeDescriptor().getContextDeploymentId();
    }

    public FireAndForgetInstance getProcessorInstanceContextById(int agentInstanceId) {
        TableInstance instance = table.getTableInstance(agentInstanceId);
        if (instance != null) {
            return new FireAndForgetInstanceTable(instance);
        }
        return null;
    }

    public FireAndForgetInstance getProcessorInstanceNoContext() {
        return getProcessorInstance(null);
    }

    public boolean isVirtualDataWindow() {
        throw new UnsupportedOperationException();
    }

    public EventType getEventTypePublic() {
        return table.getMetaData().getPublicEventType();
    }

    public Table getTable() {
        return table;
    }

    public StatementContext getStatementContext() {
        return table.getStatementContextCreateTable();
    }
}
