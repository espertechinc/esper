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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategy;
import com.espertech.esper.epl.spec.OnTriggerType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Iterator;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class NamedWindowOnDeleteView extends NamedWindowOnExprBaseView {
    private final NamedWindowOnDeleteViewFactory parent;
    private EventBean[] lastResult;

    public NamedWindowOnDeleteView(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance rootView, ExprEvaluatorContext exprEvaluatorContext, NamedWindowOnDeleteViewFactory parent) {
        super(lookupStrategy, rootView, exprEvaluatorContext);
        this.parent = parent;
    }

    public void handleMatching(EventBean[] triggerEvents, EventBean[] matchingEvents) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qInfraOnAction(OnTriggerType.ON_DELETE, triggerEvents, matchingEvents);
        }

        if ((matchingEvents != null) && (matchingEvents.length > 0)) {
            // Events to delete are indicated via old data
            this.rootView.update(null, matchingEvents);

            // The on-delete listeners receive the events deleted, but only if there is interest
            if (parent.getStatementResultService().isMakeNatural() || parent.getStatementResultService().isMakeSynthetic()) {
                updateChildren(matchingEvents, null);
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
