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
package com.espertech.esper.epl.table.onaction;

import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPStatementHandle;
import com.espertech.esper.core.service.InternalEventRouteDest;
import com.espertech.esper.core.service.InternalEventRouter;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategy;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.event.EventBeanReader;

public class TableOnSelectViewFactory implements TableOnViewFactory {
    private final TableMetadata tableMetadata;
    private final InternalEventRouter internalEventRouter;
    private final EPStatementHandle statementHandle;
    private final EventBeanReader eventBeanReader;
    private final boolean isDistinct;
    private final StatementResultService statementResultService;
    private final InternalEventRouteDest internalEventRouteDest;
    private final boolean deleteAndSelect;

    public TableOnSelectViewFactory(TableMetadata tableMetadata, InternalEventRouter internalEventRouter, EPStatementHandle statementHandle, EventBeanReader eventBeanReader, boolean distinct, StatementResultService statementResultService, InternalEventRouteDest internalEventRouteDest, boolean deleteAndSelect) {
        this.tableMetadata = tableMetadata;
        this.internalEventRouter = internalEventRouter;
        this.statementHandle = statementHandle;
        this.eventBeanReader = eventBeanReader;
        isDistinct = distinct;
        this.statementResultService = statementResultService;
        this.internalEventRouteDest = internalEventRouteDest;
        this.deleteAndSelect = deleteAndSelect;
    }

    public TableOnViewBase make(SubordWMatchExprLookupStrategy lookupStrategy, TableStateInstance tableState, AgentInstanceContext agentInstanceContext, ResultSetProcessor resultSetProcessor) {
        boolean audit = AuditEnum.INSERT.getAudit(agentInstanceContext.getAnnotations()) != null;
        return new TableOnSelectView(lookupStrategy, tableState, agentInstanceContext, tableMetadata, this, resultSetProcessor, audit, deleteAndSelect);
    }

    public InternalEventRouter getInternalEventRouter() {
        return internalEventRouter;
    }

    public EPStatementHandle getStatementHandle() {
        return statementHandle;
    }

    public EventBeanReader getEventBeanReader() {
        return eventBeanReader;
    }

    public boolean isDistinct() {
        return isDistinct;
    }

    public StatementResultService getStatementResultService() {
        return statementResultService;
    }

    public InternalEventRouteDest getInternalEventRouteDest() {
        return internalEventRouteDest;
    }
}
