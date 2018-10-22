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
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class OnExprViewTableDelete extends OnExprViewTableBase {

    public OnExprViewTableDelete(SubordWMatchExprLookupStrategy lookupStrategy, TableInstance rootView, AgentInstanceContext agentInstanceContext) {
        super(lookupStrategy, rootView, agentInstanceContext, true);
    }

    public void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        agentInstanceContext.getInstrumentationProvider().qInfraOnAction(OnTriggerType.ON_DELETE, triggerEvents, matchingEvents);

        if ((matchingEvents != null) && (matchingEvents.length > 0)) {
            for (EventBean event : matchingEvents) {
                tableInstance.deleteEvent(event);
            }

            // The on-delete listeners receive the events deleted, but only if there is interest
            StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
            if (statementResultService.isMakeNatural() || statementResultService.isMakeSynthetic()) {
                EventBean[] posted = OnExprViewTableUtil.toPublic(matchingEvents, tableInstance.getTable(), triggerEvents, true, super.getExprEvaluatorContext());
                child.update(posted, null);
            }
        }

        agentInstanceContext.getInstrumentationProvider().aInfraOnAction();
    }

    public EventType getEventType() {
        return tableInstance.getTable().getMetaData().getPublicEventType();
    }
}
