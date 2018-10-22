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
package com.espertech.esper.common.internal.view.exttimedbatch;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodDeltaResult;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodProvide;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.view.core.*;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Batch window based on timestamp of arriving events.
 */
public class ExternallyTimedBatchView extends ViewSupport implements DataWindowView {
    private final ExternallyTimedBatchViewFactory factory;

    private final EventBean[] eventsPerStream = new EventBean[1];
    protected EventBean[] lastBatch;

    private Long oldestTimestamp;
    protected final Set<EventBean> window = new LinkedHashSet<>();
    protected Long referenceTimestamp;

    protected ViewUpdatedCollection viewUpdatedCollection;
    protected AgentInstanceContext agentInstanceContext;
    private final TimePeriodProvide timePeriodProvide;

    public ExternallyTimedBatchView(ExternallyTimedBatchViewFactory factory,
                                    ViewUpdatedCollection viewUpdatedCollection,
                                    AgentInstanceViewFactoryChainContext agentInstanceContext,
                                    TimePeriodProvide timePeriodProvide) {
        this.factory = factory;
        this.viewUpdatedCollection = viewUpdatedCollection;
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
        this.referenceTimestamp = factory.optionalReferencePoint;
        this.timePeriodProvide = timePeriodProvide;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, factory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(factory, newData, oldData);

        // remove points from data window
        if (oldData != null && oldData.length != 0) {
            for (EventBean anOldData : oldData) {
                window.remove(anOldData);
                handleInternalRemovedEvent(anOldData);
            }
            determineOldestTimestamp();
        }

        // add data points to the window
        EventBean[] batchNewData = null;
        if (newData != null) {
            for (EventBean newEvent : newData) {

                long timestamp = getLongValue(newEvent);
                if (referenceTimestamp == null) {
                    referenceTimestamp = timestamp;
                }

                if (oldestTimestamp == null) {
                    oldestTimestamp = timestamp;
                } else {
                    TimePeriodDeltaResult delta = timePeriodProvide.deltaAddWReference(oldestTimestamp, referenceTimestamp, null, true, agentInstanceContext);
                    this.referenceTimestamp = delta.getLastReference();
                    if (timestamp - oldestTimestamp >= delta.getDelta()) {
                        if (batchNewData == null) {
                            batchNewData = window.toArray(new EventBean[window.size()]);
                        } else {
                            batchNewData = EventBeanUtility.addToArray(batchNewData, window);
                        }
                        window.clear();
                        oldestTimestamp = null;
                    }
                }

                window.add(newEvent);
                handleInternalAddEvent(newEvent, batchNewData != null);
            }
        }

        if (batchNewData != null) {
            handleInternalPostBatch(window, batchNewData);
            if (viewUpdatedCollection != null) {
                viewUpdatedCollection.update(batchNewData, lastBatch);
            }

            agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, batchNewData, lastBatch);
            child.update(batchNewData, lastBatch);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();

            lastBatch = batchNewData;
            determineOldestTimestamp();
        }
        if (oldData != null && oldData.length > 0) {
            if (viewUpdatedCollection != null) {
                viewUpdatedCollection.update(null, oldData);
            }

            agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, null, oldData);
            child.update(null, oldData);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    public final Iterator<EventBean> iterator() {
        return window.iterator();
    }

    public final String toString() {
        return this.getClass().getName();
    }

    public boolean isEmpty() {
        return window.isEmpty();
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(window, true, factory.getViewName(), null);
    }

    public ViewFactory getViewFactory() {
        return factory;
    }

    protected void determineOldestTimestamp() {
        if (window.isEmpty()) {
            oldestTimestamp = null;
        } else {
            oldestTimestamp = getLongValue(window.iterator().next());
        }
    }

    protected void handleInternalPostBatch(Set<EventBean> window, EventBean[] batchNewData) {
        // no action require
    }

    protected void handleInternalRemovedEvent(EventBean anOldData) {
        // no action require
    }

    protected void handleInternalAddEvent(EventBean anNewData, boolean isNextBatch) {
        // no action require
    }

    private long getLongValue(EventBean obj) {
        eventsPerStream[0] = obj;
        Number num = (Number) factory.timestampEval.evaluate(eventsPerStream, true, agentInstanceContext);
        return num.longValue();
    }
}
