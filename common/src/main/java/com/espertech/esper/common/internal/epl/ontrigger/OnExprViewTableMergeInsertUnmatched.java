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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.StopCallback;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.Collections;
import java.util.Iterator;

public class OnExprViewTableMergeInsertUnmatched extends ViewSupport implements StopCallback {
    private final TableInstance tableInstance;
    private final AgentInstanceContext agentInstanceContext;
    private final InfraOnMergeViewFactory parent;

    public OnExprViewTableMergeInsertUnmatched(TableInstance tableInstance, AgentInstanceContext agentInstanceContext, InfraOnMergeViewFactory parent) {
        this.tableInstance = tableInstance;
        this.agentInstanceContext = agentInstanceContext;
        this.parent = parent;
    }

    public void stop() {
    }

    public EventType getEventType() {
        return tableInstance.getTable().getMetaData().getPublicEventType();
    }

    public Iterator<EventBean> iterator() {
        return Collections.emptyIterator();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getInstrumentationProvider().qInfraOnAction(OnTriggerType.ON_MERGE, newData, CollectionUtil.EVENTBEANARRAY_EMPTY);

        if (newData == null) {
            agentInstanceContext.getInstrumentationProvider().aInfraOnAction();
            return;
        }

        StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
        boolean postResultsToListeners = statementResultService.isMakeNatural() || statementResultService.isMakeSynthetic();
        OnExprViewTableChangeHandler changeHandlerAdded = null;
        if (postResultsToListeners) {
            changeHandlerAdded = new OnExprViewTableChangeHandler(tableInstance.getTable());
        }

        EventBean[] eventsPerStream = new EventBean[3]; // first:named window, second: trigger, third:before-update (optional)
        for (EventBean trigger : newData) {
            eventsPerStream[1] = trigger;
            parent.getOnMergeHelper().getInsertUnmatched().apply(null, eventsPerStream, tableInstance, changeHandlerAdded, null, agentInstanceContext);

            // The on-delete listeners receive the events deleted, but only if there is interest
            if (postResultsToListeners) {
                EventBean[] postedNew = changeHandlerAdded.getEvents();
                if (postedNew != null) {
                    child.update(postedNew, null);
                }
            }
        }

        agentInstanceContext.getInstrumentationProvider().aInfraOnAction();
    }
}
