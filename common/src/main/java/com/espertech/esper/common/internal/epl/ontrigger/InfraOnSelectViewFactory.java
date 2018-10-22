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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.AuditEnum;
import com.espertech.esper.common.client.soda.StreamSelector;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryUtil;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowRootViewInstance;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.event.core.EventBeanReader;

/**
 * View for the on-select statement that handles selecting events from a named window.
 */
public class InfraOnSelectViewFactory extends InfraOnExprBaseViewFactory {
    private final boolean addToFront;
    private final EventBeanReader eventBeanReader;
    private final boolean isDistinct;
    private final boolean selectAndDelete;
    private final StreamSelector optionalStreamSelector;
    private final Table optionalInsertIntoTable;
    private final boolean insertInto;
    private final ResultSetProcessorFactoryProvider resultSetProcessorPrototype;

    public InfraOnSelectViewFactory(EventType infraEventType, boolean addToFront, EventBeanReader eventBeanReader, boolean isDistinct, boolean selectAndDelete, StreamSelector optionalStreamSelector, Table optionalInsertIntoTable, boolean insertInto, ResultSetProcessorFactoryProvider resultSetProcessorPrototype) {
        super(infraEventType);
        this.addToFront = addToFront;
        this.eventBeanReader = eventBeanReader;
        this.isDistinct = isDistinct;
        this.selectAndDelete = selectAndDelete;
        this.optionalStreamSelector = optionalStreamSelector;
        this.optionalInsertIntoTable = optionalInsertIntoTable;
        this.insertInto = insertInto;
        this.resultSetProcessorPrototype = resultSetProcessorPrototype;
    }

    public InfraOnExprBaseViewResult makeNamedWindow(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance namedWindowRootViewInstance, AgentInstanceContext agentInstanceContext) {
        Pair<ResultSetProcessor, AggregationService> pair = StatementAgentInstanceFactoryUtil.startResultSetAndAggregation(resultSetProcessorPrototype, agentInstanceContext, false, null);

        boolean audit = AuditEnum.INSERT.getAudit(agentInstanceContext.getAnnotations()) != null;
        TableInstance tableInstanceInsertInto = null;
        if (optionalInsertIntoTable != null) {
            tableInstanceInsertInto = optionalInsertIntoTable.getTableInstance(agentInstanceContext.getAgentInstanceId());
        }

        OnExprViewNamedWindowSelect selectView = new OnExprViewNamedWindowSelect(lookupStrategy, namedWindowRootViewInstance, agentInstanceContext, this, pair.getFirst(), audit, selectAndDelete, tableInstanceInsertInto);
        return new InfraOnExprBaseViewResult(selectView, pair.getSecond());
    }

    public InfraOnExprBaseViewResult makeTable(SubordWMatchExprLookupStrategy lookupStrategy, TableInstance tableInstance, AgentInstanceContext agentInstanceContext) {
        Pair<ResultSetProcessor, AggregationService> pair = StatementAgentInstanceFactoryUtil.startResultSetAndAggregation(resultSetProcessorPrototype, agentInstanceContext, false, null);

        boolean audit = AuditEnum.INSERT.getAudit(agentInstanceContext.getAnnotations()) != null;
        TableInstance tableInstanceInsertInto = null;
        if (optionalInsertIntoTable != null) {
            tableInstanceInsertInto = optionalInsertIntoTable.getTableInstance(agentInstanceContext.getAgentInstanceId());
        }

        OnExprViewTableSelect selectView = new OnExprViewTableSelect(lookupStrategy, tableInstance, agentInstanceContext, pair.getFirst(), this, audit, selectAndDelete, tableInstanceInsertInto);
        return new InfraOnExprBaseViewResult(selectView, pair.getSecond());
    }

    public boolean isAddToFront() {
        return addToFront;
    }

    public EventBeanReader getEventBeanReader() {
        return eventBeanReader;
    }

    public boolean isDistinct() {
        return isDistinct;
    }

    public StreamSelector getOptionalStreamSelector() {
        return optionalStreamSelector;
    }

    public boolean isSelectAndDelete() {
        return selectAndDelete;
    }

    public boolean isInsertInto() {
        return insertInto;
    }
}
