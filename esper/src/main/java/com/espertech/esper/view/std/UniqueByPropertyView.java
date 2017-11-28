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
package com.espertech.esper.view.std;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.collection.OneEventCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This view includes only the most recent among events having the same value for the specified field or fields.
 * The view accepts the field name as parameter from which the unique values are obtained.
 * For example, a trade's symbol could be used as a unique value.
 * In this example, the first trade for symbol IBM would be posted as new data to child views.
 * When the second trade for symbol IBM arrives the second trade is posted as new data to child views,
 * and the first trade is posted as old data.
 * Should more than one trades for symbol IBM arrive at the same time (like when batched)
 * then the child view will get all new events in newData and all new events in oldData minus the most recent event.
 * When the current new event arrives as old data, the the current unique event gets thrown away and
 * posted as old data to child views.
 * Iteration through the views data shows only the most recent events received for the unique value in the order
 * that events arrived in.
 * The type of the field returning the unique value can be any type but should override equals and hashCode()
 * as the type plays the role of a key in a map storing unique values.
 */
public class UniqueByPropertyView extends ViewSupport implements DataWindowView {
    private final UniqueByPropertyViewFactory viewFactory;
    protected final Map<Object, EventBean> mostRecentEvents = new HashMap<Object, EventBean>();
    private final EventBean[] eventsPerStream = new EventBean[1];
    protected final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;

    public UniqueByPropertyView(UniqueByPropertyViewFactory viewFactory, AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        this.viewFactory = viewFactory;
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
    }

    /**
     * Returns the name of the field supplying the unique value to keep the most recent record for.
     *
     * @return expressions for unique value
     */
    public final ExprNode[] getCriteriaExpressions() {
        return viewFactory.criteriaExpressions;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, UniqueByPropertyViewFactory.NAME, newData, oldData);
        }
        OneEventCollection postOldData = null;

        if (this.hasViews()) {
            postOldData = new OneEventCollection();
        }

        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                // Obtain unique value
                Object key = getUniqueKey(newData[i]);

                // If there are no child views, just update the own collection
                if (!this.hasViews()) {
                    mostRecentEvents.put(key, newData[i]);
                    continue;
                }

                // Post the last value as old data
                EventBean lastValue = mostRecentEvents.get(key);
                if (lastValue != null) {
                    postOldData.add(lastValue);
                }

                // Override with recent event
                mostRecentEvents.put(key, newData[i]);
            }
        }

        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                // Obtain unique value
                Object key = getUniqueKey(oldData[i]);

                // If the old event is the current unique event, remove and post as old data
                EventBean lastValue = mostRecentEvents.get(key);
                if (lastValue == null || !lastValue.equals(oldData[i])) {
                    continue;
                }

                postOldData.add(lastValue);
                mostRecentEvents.remove(key);
            }
        }


        // If there are child views, fireStatementStopped update method
        if (this.hasViews()) {
            if (postOldData.isEmpty()) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewIndicate(this, UniqueByPropertyViewFactory.NAME, newData, null);
                }
                updateChildren(newData, null);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewIndicate();
                }
            } else {
                EventBean[] postOldDataArray = postOldData.toArray();
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewIndicate(this, UniqueByPropertyViewFactory.NAME, newData, postOldDataArray);
                }
                updateChildren(newData, postOldDataArray);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewIndicate();
                }
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    /**
     * Returns true if the view is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return mostRecentEvents.isEmpty();
    }

    public final Iterator<EventBean> iterator() {
        return mostRecentEvents.values().iterator();
    }

    public final String toString() {
        return this.getClass().getName() + " uniqueFieldNames=" + Arrays.toString(viewFactory.criteriaExpressions);
    }

    protected Object getUniqueKey(EventBean theEvent) {
        eventsPerStream[0] = theEvent;
        ExprEvaluator[] criteriaExpressionsEvals = viewFactory.criteriaExpressionsEvals;
        if (criteriaExpressionsEvals.length == 1) {
            return criteriaExpressionsEvals[0].evaluate(eventsPerStream, true, agentInstanceViewFactoryContext);
        }

        Object[] values = new Object[criteriaExpressionsEvals.length];
        for (int i = 0; i < criteriaExpressionsEvals.length; i++) {
            values[i] = criteriaExpressionsEvals[i].evaluate(eventsPerStream, true, agentInstanceViewFactoryContext);
        }
        return new MultiKeyUntyped(values);
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(mostRecentEvents, true, UniqueByPropertyViewFactory.NAME, mostRecentEvents.size(), mostRecentEvents.size());
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }
}
