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
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class OnExprViewTableUpdate extends OnExprViewTableBase {

    private final InfraOnUpdateViewFactory parent;

    public OnExprViewTableUpdate(SubordWMatchExprLookupStrategy lookupStrategy, TableInstance tableInstance, AgentInstanceContext agentInstanceContext, InfraOnUpdateViewFactory parent) {
        super(lookupStrategy, tableInstance, agentInstanceContext, true);
        this.parent = parent;
    }

    public void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        agentInstanceContext.getInstrumentationProvider().qInfraOnAction(OnTriggerType.ON_UPDATE, triggerEvents, matchingEvents);

        EventBean[] eventsPerStream = new EventBean[3];

        StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
        boolean postUpdates = statementResultService.isMakeNatural() || statementResultService.isMakeSynthetic();
        EventBean[] postedOld = null;
        if (postUpdates) {
            postedOld = OnExprViewTableUtil.toPublic(matchingEvents, tableInstance.getTable(), triggerEvents, false, super.getExprEvaluatorContext());
        }

        TableUpdateStrategy tableUpdateStrategy = parent.getTableUpdateStrategy();

        for (EventBean triggerEvent : triggerEvents) {
            eventsPerStream[1] = triggerEvent;
            Collection<EventBean> matching = matchingEvents == null ? Collections.emptyList() : Arrays.asList(matchingEvents);
            tableUpdateStrategy.updateTable(matching, tableInstance, eventsPerStream, agentInstanceContext);
        }

        // The on-delete listeners receive the events deleted, but only if there is interest
        if (postUpdates) {
            EventBean[] postedNew = OnExprViewTableUtil.toPublic(matchingEvents, tableInstance.getTable(), triggerEvents, true, super.getExprEvaluatorContext());
            child.update(postedNew, postedOld);
        }

        agentInstanceContext.getInstrumentationProvider().aInfraOnAction();
    }
}
