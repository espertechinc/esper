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
package com.espertech.esper.core.start;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;

import java.util.Collection;

public class FireAndForgetProcessorTable extends FireAndForgetProcessor {
    private final TableService tableService;
    private final TableMetadata tableMetadata;

    public FireAndForgetProcessorTable(TableService tableService, TableMetadata tableMetadata) {
        this.tableService = tableService;
        this.tableMetadata = tableMetadata;
    }

    public TableMetadata getTableMetadata() {
        return tableMetadata;
    }

    public EventType getEventTypeResultSetProcessor() {
        return tableMetadata.getInternalEventType();
    }

    public EventType getEventTypePublic() {
        return tableMetadata.getPublicEventType();
    }

    public String getContextName() {
        return tableMetadata.getContextName();
    }

    public FireAndForgetInstance getProcessorInstanceContextById(int agentInstanceId) {
        TableStateInstance instance = tableService.getState(tableMetadata.getTableName(), agentInstanceId);
        if (instance == null) {
            return null;
        }
        return new FireAndForgetInstanceTable(instance);
    }

    public FireAndForgetInstance getProcessorInstanceNoContext() {
        return getProcessorInstanceContextById(-1);
    }

    public FireAndForgetInstance getProcessorInstance(AgentInstanceContext agentInstanceContext) {
        return getProcessorInstanceContextById(agentInstanceContext.getAgentInstanceId());
    }

    public Collection<Integer> getProcessorInstancesAll() {
        return tableService.getAgentInstanceIds(tableMetadata.getTableName());
    }

    public String getNamedWindowOrTableName() {
        return tableMetadata.getTableName();
    }

    public boolean isVirtualDataWindow() {
        return false;
    }

    public String[][] getUniqueIndexes(FireAndForgetInstance processorInstance) {
        return tableMetadata.getUniqueIndexes();
    }
}
