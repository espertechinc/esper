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
package com.espertech.esper.common.internal.view.lastevent;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.collection.SingleEventIterator;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.view.core.DataWindowView;
import com.espertech.esper.common.internal.view.core.ViewDataVisitor;
import com.espertech.esper.common.internal.view.core.ViewFactory;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.Iterator;

/**
 * This view is a very simple view presenting the last event posted by the parent view to any subviews.
 * Only the very last event object is kept by this view. The update method invoked by the parent view supplies
 * new data in an object array, of which the view keeps the very last instance as the 'last' or newest event.
 * The view always has the same schema as the parent view and attaches to anything, and accepts no parameters.
 * <p>
 * Thus if 5 pieces of new data arrive, the child view receives 5 elements of new data
 * and also 4 pieces of old data which is the first 4 elements of new data.
 * I.e. New data elements immediatly gets to be old data elements.
 * <p>
 * Old data received from parent is not handled, it is ignored.
 * We thus post old data as follows:
 * last event is not null +
 * new data from index zero to N-1, where N is the index of the last element in new data
 */
public class LastEventView extends ViewSupport implements DataWindowView {
    private final LastEventViewFactory viewFactory;
    private final AgentInstanceContext agentInstanceContext;

    /**
     * The last new element posted from a parent view.
     */
    protected EventBean lastEvent;

    public LastEventView(LastEventViewFactory viewFactory, AgentInstanceContext agentInstanceContext) {
        this.viewFactory = viewFactory;
        this.agentInstanceContext = agentInstanceContext;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, viewFactory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(viewFactory, newData, oldData);

        OneEventCollection oldDataToPost = null;

        if (newData != null && newData.length == 1 && (oldData == null || oldData.length == 0)) {
            EventBean currentLast = lastEvent;
            lastEvent = newData[0];
            if (child != null) {
                EventBean[] oldDataToPostHere = currentLast == null ? null : new EventBean[]{currentLast};
                agentInstanceContext.getInstrumentationProvider().qViewIndicate(viewFactory, newData, oldDataToPostHere);
                child.update(newData, oldDataToPostHere);
                agentInstanceContext.getInstrumentationProvider().aViewIndicate();
            }
        } else {
            if ((newData != null) && (newData.length != 0)) {
                if (lastEvent != null) {
                    oldDataToPost = new OneEventCollection();
                    oldDataToPost.add(lastEvent);
                }
                if (newData.length > 1) {
                    for (int i = 0; i < newData.length - 1; i++) {
                        if (oldDataToPost == null) {
                            oldDataToPost = new OneEventCollection();
                        }
                        oldDataToPost.add(newData[i]);
                    }
                }
                lastEvent = newData[newData.length - 1];
            }

            if (oldData != null) {
                for (int i = 0; i < oldData.length; i++) {
                    if (oldData[i] == lastEvent) {
                        if (oldDataToPost == null) {
                            oldDataToPost = new OneEventCollection();
                        }
                        oldDataToPost.add(oldData[i]);
                        lastEvent = null;
                    }
                }
            }

            // If there are child views, fireStatementStopped update method
            if (child != null) {
                if ((oldDataToPost != null) && (!oldDataToPost.isEmpty())) {
                    EventBean[] oldDataArray = oldDataToPost.toArray();
                    agentInstanceContext.getInstrumentationProvider().qViewIndicate(viewFactory, newData, oldDataArray);
                    child.update(newData, oldDataArray);
                    agentInstanceContext.getInstrumentationProvider().aViewIndicate();
                } else {
                    agentInstanceContext.getInstrumentationProvider().qViewIndicate(viewFactory, newData, null);
                    child.update(newData, null);
                    agentInstanceContext.getInstrumentationProvider().aViewIndicate();
                }
            }
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    public final Iterator<EventBean> iterator() {
        return new SingleEventIterator(lastEvent);
    }

    public final String toString() {
        return this.getClass().getName();
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(lastEvent, viewFactory.getViewName());
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }
}
