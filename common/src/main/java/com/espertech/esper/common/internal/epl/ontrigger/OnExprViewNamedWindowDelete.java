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
import com.espertech.esper.common.client.annotation.HintEnum;
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
public class OnExprViewNamedWindowDelete extends OnExprViewNameWindowBase {
    private final boolean silentDelete;

    public OnExprViewNamedWindowDelete(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance rootView, AgentInstanceContext agentInstanceContext) {
        super(lookupStrategy, rootView, agentInstanceContext);
        silentDelete = HintEnum.SILENT_DELETE.getHint(agentInstanceContext.getAnnotations()) != null;
    }

    public void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        agentInstanceContext.getInstrumentationProvider().qInfraOnAction(OnTriggerType.ON_DELETE, triggerEvents, matchingEvents);

        if ((matchingEvents != null) && (matchingEvents.length > 0)) {
            // Events to delete are indicated via old data
            this.rootView.update(null, matchingEvents);

            if (silentDelete) {
                this.rootView.clearDeliveriesRemoveStream(matchingEvents);
            }

            StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
            // The on-delete listeners receive the events deleted, but only if there is interest
            if (statementResultService.isMakeNatural() || statementResultService.isMakeSynthetic()) {
                child.update(matchingEvents, null);
            }
        }

        agentInstanceContext.getInstrumentationProvider().aInfraOnAction();
    }

    public EventType getEventType() {
        return rootView.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return CollectionUtil.NULL_EVENT_ITERATOR;
    }
}
