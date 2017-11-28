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
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.OneEventCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * A length-first view takes the first N arriving events. Further arriving insert stream events are disregarded until
 * events are deleted.
 * <p>
 * Remove stream events delete from the data window.
 */
public class FirstLengthWindowView extends ViewSupport implements DataWindowView {
    protected final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;
    private final FirstLengthWindowViewFactory lengthFirstFactory;
    private final int size;
    protected LinkedHashSet<EventBean> indexedEvents;

    public FirstLengthWindowView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext, FirstLengthWindowViewFactory lengthFirstWindowViewFactory, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Illegal argument for size of length window");
        }

        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
        this.lengthFirstFactory = lengthFirstWindowViewFactory;
        this.size = size;
        indexedEvents = new LinkedHashSet<EventBean>();
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
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, lengthFirstFactory.getViewName(), newData, oldData);
        }

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
                    internalHandleAdded(aNewData);
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
                    internalHandleRemoved(anOldData);
                }
            }
        }

        // If there are child views, call update method
        if ((this.hasViews()) && ((newDataToPost != null) || (oldDataToPost != null))) {
            EventBean[] nd = (newDataToPost != null) ? newDataToPost.toArray() : null;
            EventBean[] od = (oldDataToPost != null) ? oldDataToPost.toArray() : null;
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, lengthFirstFactory.getViewName(), nd, od);
            }
            updateChildren(nd, od);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public void internalHandleRemoved(EventBean anOldData) {
        // no action required
    }

    public void internalHandleAdded(EventBean aNewData) {
        // no action required
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
