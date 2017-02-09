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
package com.espertech.esper.epl.view;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPStatementHandle;
import com.espertech.esper.core.service.InternalEventRouter;
import com.espertech.esper.epl.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;

/**
 * An output strategy that handles routing (insert-into) and stream selection.
 */
public class OutputStrategyPostProcessFactory {
    private final boolean isRoute;
    private final SelectClauseStreamSelectorEnum insertIntoStreamSelector;
    private final SelectClauseStreamSelectorEnum selectStreamDirEnum;
    private final InternalEventRouter internalEventRouter;
    private final EPStatementHandle epStatementHandle;
    private final boolean addToFront;
    private final TableService tableService;
    private final String tableName;

    public OutputStrategyPostProcessFactory(boolean route, SelectClauseStreamSelectorEnum insertIntoStreamSelector, SelectClauseStreamSelectorEnum selectStreamDirEnum, InternalEventRouter internalEventRouter, EPStatementHandle epStatementHandle, boolean addToFront, TableService tableService, String tableName) {
        isRoute = route;
        this.insertIntoStreamSelector = insertIntoStreamSelector;
        this.selectStreamDirEnum = selectStreamDirEnum;
        this.internalEventRouter = internalEventRouter;
        this.epStatementHandle = epStatementHandle;
        this.addToFront = addToFront;
        this.tableService = tableService;
        this.tableName = tableName;
    }

    public OutputStrategyPostProcess make(AgentInstanceContext agentInstanceContext) {
        TableStateInstance tableStateInstance = null;
        if (tableName != null) {
            tableStateInstance = tableService.getState(tableName, agentInstanceContext.getAgentInstanceId());
        }
        return new OutputStrategyPostProcess(this, agentInstanceContext, tableStateInstance);
    }

    public boolean isRoute() {
        return isRoute;
    }

    public SelectClauseStreamSelectorEnum getInsertIntoStreamSelector() {
        return insertIntoStreamSelector;
    }

    public SelectClauseStreamSelectorEnum getSelectStreamDirEnum() {
        return selectStreamDirEnum;
    }

    public InternalEventRouter getInternalEventRouter() {
        return internalEventRouter;
    }

    public EPStatementHandle getEpStatementHandle() {
        return epStatementHandle;
    }

    public boolean isAddToFront() {
        return addToFront;
    }
}
