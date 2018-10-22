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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.context.util.StatementFinalizeCallback;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategy;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategyFactory;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategyRedoCallback;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperNoCopy;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperWCopy;

import java.util.Collections;

public class InfraOnMergeActionUpd extends InfraOnMergeAction implements TableUpdateStrategyRedoCallback, StatementReadyCallback {
    private final EventBeanUpdateHelperWCopy namedWindowUpdate;
    private final EventBeanUpdateHelperNoCopy tableUpdate;
    private Table table;
    private TableUpdateStrategy tableUpdateStrategy;

    public InfraOnMergeActionUpd(ExprEvaluator optionalFilter, EventBeanUpdateHelperWCopy namedWindowUpdate) {
        super(optionalFilter);
        this.namedWindowUpdate = namedWindowUpdate;
        this.tableUpdate = null;
    }

    public InfraOnMergeActionUpd(ExprEvaluator optionalFilter, EventBeanUpdateHelperNoCopy tableUpdate, Table table) {
        super(optionalFilter);
        this.tableUpdate = tableUpdate;
        this.namedWindowUpdate = null;
        this.table = table;
        initTableUpdateStrategy(table);
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        table.addUpdateStrategyCallback(this);
        statementContext.addFinalizeCallback(new StatementFinalizeCallback() {
            public void statementDestroyed(StatementContext context) {
                InfraOnMergeActionUpd.this.table.removeUpdateStrategyCallback(InfraOnMergeActionUpd.this);
            }
        });
    }

    public void apply(EventBean matchingEvent, EventBean[] eventsPerStream, OneEventCollection newData, OneEventCollection oldData, AgentInstanceContext agentInstanceContext) {
        EventBean copy = namedWindowUpdate.updateWCopy(matchingEvent, eventsPerStream, agentInstanceContext);
        newData.add(copy);
        oldData.add(matchingEvent);
    }

    public void apply(EventBean matchingEvent, EventBean[] eventsPerStream, TableInstance tableStateInstance, OnExprViewTableChangeHandler changeHandlerAdded, OnExprViewTableChangeHandler changeHandlerRemoved, AgentInstanceContext agentInstanceContext) {
        if (changeHandlerRemoved != null) {
            changeHandlerRemoved.add(matchingEvent, eventsPerStream, false, agentInstanceContext);
        }
        tableUpdateStrategy.updateTable(Collections.singleton(matchingEvent), tableStateInstance, eventsPerStream, agentInstanceContext);
        if (changeHandlerAdded != null) {
            changeHandlerAdded.add(matchingEvent, eventsPerStream, false, agentInstanceContext);
        }
    }

    public String getName() {
        return "update";
    }

    public boolean isMerge() {
        return true;
    }

    public String[] getTableUpdatedProperties() {
        return tableUpdate.getUpdatedProperties();
    }

    public void initTableUpdateStrategy(Table table) {
        try {
            this.tableUpdateStrategy = TableUpdateStrategyFactory.validateGetTableUpdateStrategy(table.getMetaData(), tableUpdate, true);
        } catch (ExprValidationException e) {
            throw new EPException(e.getMessage(), e);
        }
    }
}
