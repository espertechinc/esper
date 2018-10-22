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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.context.util.StatementFinalizeCallback;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowRootViewInstance;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategy;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategyFactory;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategyRedoCallback;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperNoCopy;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperWCopy;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class InfraOnUpdateViewFactory extends InfraOnExprBaseViewFactory implements TableUpdateStrategyRedoCallback {
    private final EventBeanUpdateHelperWCopy updateHelperNamedWindow;
    private final EventBeanUpdateHelperNoCopy updateHelperTable;
    private final Table table;
    private TableUpdateStrategy tableUpdateStrategy;

    public InfraOnUpdateViewFactory(EventType infraEventType, EventBeanUpdateHelperWCopy updateHelperNamedWindow, EventBeanUpdateHelperNoCopy updateHelperTable, Table table, StatementContext statementContext) {
        super(infraEventType);
        this.updateHelperNamedWindow = updateHelperNamedWindow;
        this.updateHelperTable = updateHelperTable;
        this.table = table;

        if (table != null) {
            initTableUpdateStrategy(table);
            table.addUpdateStrategyCallback(this);
            statementContext.addFinalizeCallback(new StatementFinalizeCallback() {
                public void statementDestroyed(StatementContext context) {
                    InfraOnUpdateViewFactory.this.table.removeUpdateStrategyCallback(InfraOnUpdateViewFactory.this);
                }
            });
        }
    }

    public InfraOnExprBaseViewResult makeNamedWindow(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance namedWindowRootViewInstance, AgentInstanceContext agentInstanceContext) {
        OnExprViewNamedWindowUpdate view = new OnExprViewNamedWindowUpdate(lookupStrategy, namedWindowRootViewInstance, agentInstanceContext, this);
        return new InfraOnExprBaseViewResult(view, null);
    }

    public InfraOnExprBaseViewResult makeTable(SubordWMatchExprLookupStrategy lookupStrategy, TableInstance tableInstance, AgentInstanceContext agentInstanceContext) {
        OnExprViewTableUpdate view = new OnExprViewTableUpdate(lookupStrategy, tableInstance, agentInstanceContext, this);
        return new InfraOnExprBaseViewResult(view, null);
    }

    public EventBeanUpdateHelperWCopy getUpdateHelperNamedWindow() {
        return updateHelperNamedWindow;
    }

    public EventBeanUpdateHelperNoCopy getUpdateHelperTable() {
        return updateHelperTable;
    }

    public TableUpdateStrategy getTableUpdateStrategy() {
        return tableUpdateStrategy;
    }

    public boolean isMerge() {
        return false;
    }

    public String[] getTableUpdatedProperties() {
        return updateHelperTable.getUpdatedProperties();
    }

    public void initTableUpdateStrategy(Table table) {
        try {
            tableUpdateStrategy = TableUpdateStrategyFactory.validateGetTableUpdateStrategy(table.getMetaData(), updateHelperTable, false);
        } catch (ExprValidationException ex) {
            throw new EPException(ex.getMessage(), ex);
        }
    }
}
