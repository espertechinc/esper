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
import com.espertech.esper.collection.ArrayEventIterator;
import com.espertech.esper.collection.OneEventCollection;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategy;
import com.espertech.esper.epl.spec.OnTriggerType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Iterator;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class NamedWindowOnUpdateView extends NamedWindowOnExprBaseView {
    private NamedWindowOnUpdateViewFactory parent;
    private EventBean[] lastResult;

    public NamedWindowOnUpdateView(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance rootView, ExprEvaluatorContext exprEvaluatorContext, NamedWindowOnUpdateViewFactory parent) {
        super(lookupStrategy, rootView, exprEvaluatorContext);
        this.parent = parent;
    }

    public void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qInfraOnAction(OnTriggerType.ON_UPDATE, triggerEvents, matchingEvents);
        }

        if ((matchingEvents == null) || (matchingEvents.length == 0)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aInfraOnAction();
            }
            return;
        }

        EventBean[] eventsPerStream = new EventBean[3];

        OneEventCollection newData = new OneEventCollection();
        OneEventCollection oldData = new OneEventCollection();

        for (EventBean triggerEvent : triggerEvents) {
            eventsPerStream[1] = triggerEvent;
            for (EventBean matchingEvent : matchingEvents) {
                EventBean copy = parent.getUpdateHelper().updateWCopy(matchingEvent, eventsPerStream, super.getExprEvaluatorContext());
                newData.add(copy);
                oldData.add(matchingEvent);
            }
        }

        if (!newData.isEmpty()) {
            // Events to delete are indicated via old data
            this.rootView.update(newData.toArray(), oldData.toArray());

            // The on-delete listeners receive the events deleted, but only if there is interest
            if (parent.getStatementResultService().isMakeNatural() || parent.getStatementResultService().isMakeSynthetic()) {
                updateChildren(newData.toArray(), oldData.toArray());
            }
        }

        // Keep the last delete records
        lastResult = matchingEvents;
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aInfraOnAction();
        }
    }

    public EventType getEventType() {
        return rootView.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return new ArrayEventIterator(lastResult);
    }
}