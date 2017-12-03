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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.client.soda.StreamSelector;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPStatementHandle;
import com.espertech.esper.core.service.InternalEventRouteDest;
import com.espertech.esper.core.service.InternalEventRouter;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategy;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.event.EventBeanReader;

/**
 * View for the on-select statement that handles selecting events from a named window.
 */
public class NamedWindowOnSelectViewFactory extends NamedWindowOnExprBaseViewFactory {
    private final InternalEventRouter internalEventRouter;
    private final boolean addToFront;
    private final EPStatementHandle statementHandle;
    private final EventBeanReader eventBeanReader;
    private final boolean isDistinct;
    private final StatementResultService statementResultService;
    private final InternalEventRouteDest internalEventRouteDest;
    private final boolean deleteAndSelect;
    private final StreamSelector optionalStreamSelector;
    private final String optionalInsertIntoTableName;

    public NamedWindowOnSelectViewFactory(EventType namedWindowEventType, InternalEventRouter internalEventRouter, boolean addToFront, EPStatementHandle statementHandle, EventBeanReader eventBeanReader, boolean distinct, StatementResultService statementResultService, InternalEventRouteDest internalEventRouteDest, boolean deleteAndSelect, StreamSelector optionalStreamSelector, String optionalInsertIntoTableName) {
        super(namedWindowEventType);
        this.internalEventRouter = internalEventRouter;
        this.addToFront = addToFront;
        this.statementHandle = statementHandle;
        this.eventBeanReader = eventBeanReader;
        isDistinct = distinct;
        this.statementResultService = statementResultService;
        this.internalEventRouteDest = internalEventRouteDest;
        this.deleteAndSelect = deleteAndSelect;
        this.optionalStreamSelector = optionalStreamSelector;
        this.optionalInsertIntoTableName = optionalInsertIntoTableName;
    }

    public NamedWindowOnExprBaseView make(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance namedWindowRootViewInstance, AgentInstanceContext agentInstanceContext, ResultSetProcessor resultSetProcessor) {
        boolean audit = AuditEnum.INSERT.getAudit(agentInstanceContext.getAnnotations()) != null;
        TableStateInstance tableStateInstance = null;
        if (optionalInsertIntoTableName != null) {
            tableStateInstance = agentInstanceContext.getStatementContext().getTableService().getState(optionalInsertIntoTableName, agentInstanceContext.getAgentInstanceId());
        }
        return new NamedWindowOnSelectView(lookupStrategy, namedWindowRootViewInstance, agentInstanceContext, this, resultSetProcessor, audit, deleteAndSelect, tableStateInstance);
    }

    public InternalEventRouter getInternalEventRouter() {
        return internalEventRouter;
    }

    public boolean isAddToFront() {
        return addToFront;
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

    public StreamSelector getOptionalStreamSelector() {
        return optionalStreamSelector;
    }
}
