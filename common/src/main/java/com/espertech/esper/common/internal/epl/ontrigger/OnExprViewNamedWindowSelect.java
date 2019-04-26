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
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowRootViewInstance;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * View for the on-select statement that handles selecting events from a named window.
 */
public class OnExprViewNamedWindowSelect extends OnExprViewNameWindowBase {
    private final InfraOnSelectViewFactory parent;
    private final ResultSetProcessor resultSetProcessor;
    private final Set<MultiKeyArrayOfKeys<EventBean>> oldEvents = new HashSet<>();
    private final boolean audit;
    private final boolean isDelete;
    private final TableInstance tableInstanceInsertInto;

    public OnExprViewNamedWindowSelect(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance rootView, AgentInstanceContext agentInstanceContext, InfraOnSelectViewFactory parent, ResultSetProcessor resultSetProcessor, boolean audit, boolean isDelete, TableInstance tableInstanceInsertInto) {
        super(lookupStrategy, rootView, agentInstanceContext);
        this.parent = parent;
        this.resultSetProcessor = resultSetProcessor;
        this.audit = audit;
        this.isDelete = isDelete;
        this.tableInstanceInsertInto = tableInstanceInsertInto;
    }

    public void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        agentInstanceContext.getInstrumentationProvider().qInfraOnAction(OnTriggerType.ON_SELECT, triggerEvents, matchingEvents);

        // clear state from prior results
        resultSetProcessor.clear();

        // build join result
        // use linked hash set to retain order of join results for last/first/window to work most intuitively
        Set<MultiKeyArrayOfKeys<EventBean>> newEvents = buildJoinResult(triggerEvents, matchingEvents);

        // process matches
        UniformPair<EventBean[]> pair = resultSetProcessor.processJoinResult(newEvents, oldEvents, false);
        EventBean[] newData = pair != null ? pair.getFirst() : null;

        // handle distinct and insert
        newData = InfraOnSelectUtil.handleDistintAndInsert(newData, parent, agentInstanceContext, tableInstanceInsertInto, audit);

        // The on-select listeners receive the events selected
        if ((newData != null) && (newData.length > 0)) {
            if (child != null) {
                // And post only if we have listeners/subscribers that need the data
                StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
                if (statementResultService.isMakeNatural() || statementResultService.isMakeSynthetic()) {
                    child.update(newData, null);
                }
            }
        }

        // clear state from prior results
        resultSetProcessor.clear();

        // Events to delete are indicated via old data
        if (isDelete) {
            this.rootView.update(null, matchingEvents);
        }

        agentInstanceContext.getInstrumentationProvider().aInfraOnAction();
    }

    public static Set<MultiKeyArrayOfKeys<EventBean>> buildJoinResult(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        LinkedHashSet events = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>();
        for (int i = 0; i < triggerEvents.length; i++) {
            EventBean triggerEvent = triggerEvents[0];
            if (matchingEvents != null) {
                for (int j = 0; j < matchingEvents.length; j++) {
                    EventBean[] eventsPerStream = new EventBean[2];
                    eventsPerStream[0] = matchingEvents[j];
                    eventsPerStream[1] = triggerEvent;
                    events.add(new MultiKeyArrayOfKeys<EventBean>(eventsPerStream));
                }
            }
        }
        return events;
    }

    public EventType getEventType() {
        if (resultSetProcessor != null) {
            return resultSetProcessor.getResultEventType();
        } else {
            return rootView.getEventType();
        }
    }

    public Iterator<EventBean> iterator() {
        return CollectionUtil.NULL_EVENT_ITERATOR;
    }
}
