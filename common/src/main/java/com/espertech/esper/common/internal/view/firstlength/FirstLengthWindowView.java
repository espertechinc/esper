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
package com.espertech.esper.common.internal.view.firstlength;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.view.core.*;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * A length-first view takes the first N arriving events. Further arriving insert stream events are disregarded until
 * events are deleted.
 * <p>
 * Remove stream events delete from the data window.
 */
public class FirstLengthWindowView extends ViewSupport implements DataWindowView {
    protected final AgentInstanceContext agentInstanceContext;
    private final FirstLengthWindowViewFactory lengthFirstFactory;
    private final int size;
    protected LinkedHashSet<EventBean> indexedEvents;

    public FirstLengthWindowView(AgentInstanceViewFactoryChainContext agentInstanceContext, FirstLengthWindowViewFactory lengthFirstWindowViewFactory, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Illegal argument for size of length window");
        }

        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
        this.lengthFirstFactory = lengthFirstWindowViewFactory;
        this.size = size;
        indexedEvents = new LinkedHashSet<>();
    }

    /**
     * Returns true if the window is empty, or false if not empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return indexedEvents.isEmpty();
    }

    /**
     * Returns the size of the length window.
     *
     * @return size of length window
     */
    public final int getSize() {
        return size;
    }

    public final EventType getEventType() {
        // The event type is the parent view's event type
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, lengthFirstFactory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(lengthFirstFactory, newData, oldData);

        OneEventCollection newDataToPost = null;
        OneEventCollection oldDataToPost = null;

        // add data points to the window as long as its not full, ignoring later events
        if (newData != null) {
            for (EventBean aNewData : newData) {
                if (indexedEvents.size() < size) {
                    if (newDataToPost == null) {
                        newDataToPost = new OneEventCollection();
                    }
                    newDataToPost.add(aNewData);
                    indexedEvents.add(aNewData);
                }
            }
        }

        if (oldData != null) {
            for (EventBean anOldData : oldData) {
                boolean removed = indexedEvents.remove(anOldData);
                if (removed) {
                    if (oldDataToPost == null) {
                        oldDataToPost = new OneEventCollection();
                    }
                    oldDataToPost.add(anOldData);
                }
            }
        }

        // If there are child views, call update method
        if ((child != null) && ((newDataToPost != null) || (oldDataToPost != null))) {
            EventBean[] nd = (newDataToPost != null) ? newDataToPost.toArray() : null;
            EventBean[] od = (oldDataToPost != null) ? oldDataToPost.toArray() : null;
            agentInstanceContext.getInstrumentationProvider().qViewIndicate(lengthFirstFactory, nd, od);
            child.update(nd, od);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
        }
        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    public final Iterator<EventBean> iterator() {
        return indexedEvents.iterator();
    }

    public final String toString() {
        return this.getClass().getName() + " size=" + size;
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(indexedEvents, true, lengthFirstFactory.getViewName(), null);
    }

    public LinkedHashSet<EventBean> getIndexedEvents() {
        return indexedEvents;
    }

    public ViewFactory getViewFactory() {
        return lengthFirstFactory;
    }
}
