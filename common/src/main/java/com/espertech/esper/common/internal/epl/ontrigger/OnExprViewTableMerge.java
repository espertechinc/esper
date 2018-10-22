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
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.util.List;

public class OnExprViewTableMerge extends OnExprViewTableBase {
    private final InfraOnMergeViewFactory parent;

    public OnExprViewTableMerge(SubordWMatchExprLookupStrategy lookupStrategy, TableInstance tableInstance, AgentInstanceContext agentInstanceContext, InfraOnMergeViewFactory parent) {
        super(lookupStrategy, tableInstance, agentInstanceContext, parent.getOnMergeHelper().isRequiresTableWriteLock());
        this.parent = parent;
    }

    public void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        InstrumentationCommon instrumentationCommon = agentInstanceContext.getInstrumentationProvider();
        instrumentationCommon.qInfraOnAction(OnTriggerType.ON_MERGE, triggerEvents, matchingEvents);

        EventBean[] eventsPerStream = new EventBean[3]; // first:table, second: trigger, third:before-update (optional)

        StatementResultService statementResultService = agentInstanceContext.getStatementResultService();
        boolean postResultsToListeners = statementResultService.isMakeNatural() || statementResultService.isMakeSynthetic();
        OnExprViewTableChangeHandler changeHandlerRemoved = null;
        OnExprViewTableChangeHandler changeHandlerAdded = null;
        if (postResultsToListeners) {
            changeHandlerRemoved = new OnExprViewTableChangeHandler(tableInstance.getTable());
            changeHandlerAdded = new OnExprViewTableChangeHandler(tableInstance.getTable());
        }

        if ((matchingEvents == null) || (matchingEvents.length == 0)) {

            List<InfraOnMergeMatch> unmatched = parent.getOnMergeHelper().getUnmatched();

            for (EventBean triggerEvent : triggerEvents) {
                eventsPerStream[1] = triggerEvent;
                instrumentationCommon.qInfraMergeWhenThens(false, triggerEvent, unmatched.size());

                int count = -1;
                for (InfraOnMergeMatch action : unmatched) {
                    count++;
                    instrumentationCommon.qInfraMergeWhenThenItem(false, count);

                    if (!action.isApplies(eventsPerStream, super.getExprEvaluatorContext())) {
                        instrumentationCommon.aInfraMergeWhenThenItem(false, false);
                        continue;
                    }
                    action.applyTable(null, eventsPerStream, tableInstance, changeHandlerAdded, changeHandlerRemoved, agentInstanceContext);
                    instrumentationCommon.aInfraMergeWhenThenItem(false, true);
                    break;  // apply no other actions
                }

                instrumentationCommon.aInfraMergeWhenThens(false);
            }
        } else {

            List<InfraOnMergeMatch> matched = parent.getOnMergeHelper().getMatched();

            for (EventBean triggerEvent : triggerEvents) {
                eventsPerStream[1] = triggerEvent;
                instrumentationCommon.qInfraMergeWhenThens(true, triggerEvent, matched.size());

                for (EventBean matchingEvent : matchingEvents) {
                    eventsPerStream[0] = matchingEvent;

                    int count = -1;
                    for (InfraOnMergeMatch action : matched) {
                        count++;
                        instrumentationCommon.qInfraMergeWhenThenItem(true, count);

                        if (!action.isApplies(eventsPerStream, super.getExprEvaluatorContext())) {
                            instrumentationCommon.aInfraMergeWhenThenItem(true, false);
                            continue;
                        }
                        action.applyTable(matchingEvent, eventsPerStream, tableInstance, changeHandlerAdded, changeHandlerRemoved, agentInstanceContext);
                        instrumentationCommon.aInfraMergeWhenThenItem(true, true);
                        break;  // apply no other actions
                    }
                }

                instrumentationCommon.aInfraMergeWhenThens(true);
            }
        }

        // The on-delete listeners receive the events deleted, but only if there is interest
        if (postResultsToListeners) {
            EventBean[] postedNew = changeHandlerAdded.getEvents();
            EventBean[] postedOld = changeHandlerRemoved.getEvents();
            if (postedNew != null || postedOld != null) {
                child.update(postedNew, postedOld);
            }
        }

        instrumentationCommon.aInfraOnAction();
    }
}
