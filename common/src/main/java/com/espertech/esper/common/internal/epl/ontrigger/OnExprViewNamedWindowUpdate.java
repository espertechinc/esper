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
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowRootViewInstance;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.Iterator;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class OnExprViewNamedWindowUpdate extends OnExprViewNameWindowBase {
    private InfraOnUpdateViewFactory parent;

    public OnExprViewNamedWindowUpdate(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance rootView, AgentInstanceContext agentInstanceContext, InfraOnUpdateViewFactory parent) {
        super(lookupStrategy, rootView, agentInstanceContext);
        this.parent = parent;
    }

    public void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        agentInstanceContext.getInstrumentationProvider().qInfraOnAction(OnTriggerType.ON_UPDATE, triggerEvents, matchingEvents);

        if ((matchingEvents == null) || (matchingEvents.length == 0)) {
            agentInstanceContext.getInstrumentationProvider().aInfraOnAction();
            return;
        }

        EventBean[] eventsPerStream = new EventBean[3];

        OneEventCollection newData = new OneEventCollection();
        OneEventCollection oldData = new OneEventCollection();

        for (EventBean triggerEvent : triggerEvents) {
            eventsPerStream[1] = triggerEvent;
            for (EventBean matchingEvent : matchingEvents) {
                EventBean copy = parent.getUpdateHelperNamedWindow().updateWCopy(matchingEvent, eventsPerStream, super.getExprEvaluatorContext());
                newData.add(copy);
                oldData.add(matchingEvent);
            }
        }

        if (!newData.isEmpty()) {
            // Events to delete are indicated via old data
            this.rootView.update(newData.toArray(), oldData.toArray());

            // The on-delete listeners receive the events deleted, but only if there is interest
            StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
            if (statementResultService.isMakeNatural() || statementResultService.isMakeSynthetic()) {
                if (child != null) {
                    child.update(newData.toArray(), oldData.toArray());
                }
            }
        }

        // Keep the last delete records
        agentInstanceContext.getInstrumentationProvider().aInfraOnAction();
    }

    public EventType getEventType() {
        return rootView.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return CollectionUtil.NULL_EVENT_ITERATOR;
    }
}