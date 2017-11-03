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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.OneEventCollection;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategy;
import com.espertech.esper.epl.metric.MetricReportingPath;
import com.espertech.esper.epl.spec.OnTriggerType;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.ViewSupport;

import java.util.Iterator;
import java.util.List;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class NamedWindowOnMergeView extends NamedWindowOnExprBaseView {
    private final NamedWindowOnMergeViewFactory parent;

    public NamedWindowOnMergeView(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance rootView, ExprEvaluatorContext exprEvaluatorContext, NamedWindowOnMergeViewFactory parent) {
        super(lookupStrategy, rootView, exprEvaluatorContext);
        this.parent = parent;
    }

    public void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qInfraOnAction(OnTriggerType.ON_MERGE, triggerEvents, matchingEvents);
        }

        OneEventCollection newData = new OneEventCollection();
        OneEventCollection oldData = null;
        EventBean[] eventsPerStream = new EventBean[3]; // first:named window, second: trigger, third:before-update (optional)

        if ((matchingEvents == null) || (matchingEvents.length == 0)) {

            List<NamedWindowOnMergeMatch> unmatched = parent.getNamedWindowOnMergeHelper().getUnmatched();

            for (EventBean triggerEvent : triggerEvents) {
                eventsPerStream[1] = triggerEvent;

                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qInfraMergeWhenThens(false, triggerEvent, unmatched.size());
                }

                int count = -1;
                for (NamedWindowOnMergeMatch action : unmatched) {
                    count++;

                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().qInfraMergeWhenThenItem(false, count);
                    }
                    if (!action.isApplies(eventsPerStream, super.getExprEvaluatorContext())) {
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().aInfraMergeWhenThenItem(false, false);
                        }
                        continue;
                    }
                    action.apply(null, eventsPerStream, newData, oldData, super.getExprEvaluatorContext());
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aInfraMergeWhenThenItem(false, true);
                    }
                    break;  // apply no other actions
                }

                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aInfraMergeWhenThens(false);
                }
            }
        } else {

            // handle update/
            oldData = new OneEventCollection();

            List<NamedWindowOnMergeMatch> matched = parent.getNamedWindowOnMergeHelper().getMatched();

            for (EventBean triggerEvent : triggerEvents) {
                eventsPerStream[1] = triggerEvent;
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qInfraMergeWhenThens(true, triggerEvent, matched.size());
                }

                for (EventBean matchingEvent : matchingEvents) {
                    eventsPerStream[0] = matchingEvent;

                    int count = -1;
                    for (NamedWindowOnMergeMatch action : matched) {
                        count++;

                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().qInfraMergeWhenThenItem(true, count);
                        }
                        if (!action.isApplies(eventsPerStream, super.getExprEvaluatorContext())) {
                            if (InstrumentationHelper.ENABLED) {
                                InstrumentationHelper.get().aInfraMergeWhenThenItem(true, false);
                            }
                            continue;
                        }
                        action.apply(matchingEvent, eventsPerStream, newData, oldData, super.getExprEvaluatorContext());
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().aInfraMergeWhenThenItem(true, true);
                        }
                        break;  // apply no other actions
                    }
                }

                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aInfraMergeWhenThens(true);
                }
            }
        }

        applyDelta(newData, oldData, parent, rootView, this);

        // Keep the last delete records
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aInfraOnAction();
        }
    }

    static void applyDelta(OneEventCollection newData, OneEventCollection oldData, NamedWindowOnMergeViewFactory parent, NamedWindowRootViewInstance rootView, ViewSupport viewable) {
        if (!newData.isEmpty() || (oldData != null && !oldData.isEmpty())) {
            if ((MetricReportingPath.isMetricsEnabled) && (parent.getCreateNamedWindowMetricHandle().isEnabled()) && !newData.isEmpty()) {
                parent.getMetricReportingService().accountTime(parent.getCreateNamedWindowMetricHandle(), 0, 0, newData.toArray().length);
            }

            // Events to delete are indicated via old data
            // The on-merge listeners receive the events deleted, but only if there is interest
            if (parent.getStatementResultService().isMakeNatural()) {
                EventBean[] eventsPerStreamNaturalNew = newData.isEmpty() ? null : newData.toArray();
                EventBean[] eventsPerStreamNaturalOld = (oldData == null || oldData.isEmpty()) ? null : oldData.toArray();
                rootView.update(EventBeanUtility.denaturalize(eventsPerStreamNaturalNew), EventBeanUtility.denaturalize(eventsPerStreamNaturalOld));
                viewable.updateChildren(eventsPerStreamNaturalNew, eventsPerStreamNaturalOld);
            } else {
                EventBean[] eventsPerStreamNew = newData.isEmpty() ? null : newData.toArray();
                EventBean[] eventsPerStreamOld = (oldData == null || oldData.isEmpty()) ? null : oldData.toArray();
                rootView.update(eventsPerStreamNew, eventsPerStreamOld);
                if (parent.getStatementResultService().isMakeSynthetic()) {
                    viewable.updateChildren(eventsPerStreamNew, eventsPerStreamOld);
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