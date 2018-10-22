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
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.metrics.stmtmetrics.StatementMetricHandle;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.Iterator;
import java.util.List;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class OnExprViewNamedWindowMerge extends OnExprViewNameWindowBase {
    private final InfraOnMergeViewFactory parent;

    public OnExprViewNamedWindowMerge(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance rootView, AgentInstanceContext agentInstanceContext, InfraOnMergeViewFactory parent) {
        super(lookupStrategy, rootView, agentInstanceContext);
        this.parent = parent;
    }

    public void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        InstrumentationCommon instrumentationCommon = agentInstanceContext.getInstrumentationProvider();
        instrumentationCommon.qInfraOnAction(OnTriggerType.ON_MERGE, triggerEvents, matchingEvents);

        OneEventCollection newData = new OneEventCollection();
        OneEventCollection oldData = null;
        EventBean[] eventsPerStream = new EventBean[3]; // first:named window, second: trigger, third:before-update (optional)

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
                    action.applyNamedWindow(null, eventsPerStream, newData, oldData, agentInstanceContext);
                    instrumentationCommon.aInfraMergeWhenThenItem(false, true);
                    break;  // apply no other actions
                }

                instrumentationCommon.aInfraMergeWhenThens(false);
            }
        } else {

            // handle update/
            oldData = new OneEventCollection();

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
                        action.applyNamedWindow(matchingEvent, eventsPerStream, newData, oldData, agentInstanceContext);
                        instrumentationCommon.aInfraMergeWhenThenItem(true, true);
                        break;  // apply no other actions
                    }
                }

                instrumentationCommon.aInfraMergeWhenThens(true);
            }
        }

        applyDelta(newData, oldData, parent, rootView, agentInstanceContext, this);

        instrumentationCommon.aInfraOnAction();
    }

    static void applyDelta(OneEventCollection newData, OneEventCollection oldData, InfraOnMergeViewFactory parent, NamedWindowRootViewInstance rootView, AgentInstanceContext agentInstanceContext, ViewSupport viewable) {
        if (!newData.isEmpty() || (oldData != null && !oldData.isEmpty())) {
            StatementMetricHandle metricHandle = rootView.getAgentInstanceContext().getStatementContext().getEpStatementHandle().getMetricsHandle();
            if (metricHandle.isEnabled() && !newData.isEmpty()) {
                agentInstanceContext.getMetricReportingService().accountTime(metricHandle, 0, 0, newData.toArray().length);
            }

            StatementResultService statementResultService = agentInstanceContext.getStatementResultService();

            // Events to delete are indicated via old data
            // The on-merge listeners receive the events deleted, but only if there is interest
            if (statementResultService.isMakeNatural()) {
                EventBean[] eventsPerStreamNaturalNew = newData.isEmpty() ? null : newData.toArray();
                EventBean[] eventsPerStreamNaturalOld = (oldData == null || oldData.isEmpty()) ? null : oldData.toArray();
                rootView.update(EventBeanUtility.denaturalize(eventsPerStreamNaturalNew), EventBeanUtility.denaturalize(eventsPerStreamNaturalOld));
                viewable.getChild().update(eventsPerStreamNaturalNew, eventsPerStreamNaturalOld);
            } else {
                EventBean[] eventsPerStreamNew = newData.isEmpty() ? null : newData.toArray();
                EventBean[] eventsPerStreamOld = (oldData == null || oldData.isEmpty()) ? null : oldData.toArray();
                rootView.update(eventsPerStreamNew, eventsPerStreamOld);
                if (statementResultService.isMakeSynthetic()) {
                    viewable.getChild().update(eventsPerStreamNew, eventsPerStreamOld);
                }
            }
        }
    }

    public EventType getEventType() {
        return rootView.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return CollectionUtil.NULL_EVENT_ITERATOR;
    }
}