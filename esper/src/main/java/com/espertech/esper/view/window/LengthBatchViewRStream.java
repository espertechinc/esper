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
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Same as the {@link LengthBatchView}, this view also supports fast-remove from the batch for remove stream events.
 */
public class LengthBatchViewRStream extends ViewSupport implements DataWindowView {
    // View parameters
    protected final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;
    private final LengthBatchViewFactory lengthBatchViewFactory;
    private final int size;

    // Current running windows
    protected LinkedHashSet<EventBean> lastBatch = null;
    protected LinkedHashSet<EventBean> currentBatch = new LinkedHashSet<EventBean>();

    /**
     * Constructor.
     *
     * @param size                            is the number of events to batch
     * @param lengthBatchViewFactory          for copying this view in a group-by
     * @param agentInstanceViewFactoryContext context
     */
    public LengthBatchViewRStream(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext,
                                  LengthBatchViewFactory lengthBatchViewFactory,
                                  int size) {
        this.lengthBatchViewFactory = lengthBatchViewFactory;
        this.size = size;
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;

        if (size <= 0) {
            throw new IllegalArgumentException("Invalid size parameter, size=" + size);
        }
    }

    /**
     * Returns the number of events to batch (data window size).
     *
     * @return batch size
     */
    public final int getSize() {
        return size;
    }

    public final EventType getEventType() {
        return parent.getEventType();
    }

    public void internalHandleRemoved(EventBean oldData) {
        // no action required
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, lengthBatchViewFactory.getViewName(), newData, oldData);
        }

        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                if (currentBatch.remove(oldData[i])) {
                    internalHandleRemoved(oldData[i]);
                }
            }
        }

        // we don't care about removed data from a prior view
        if ((newData == null) || (newData.length == 0)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewProcessIRStream();
            }
            return;
        }

        // add data points to the current batch
        for (EventBean newEvent : newData) {
            currentBatch.add(newEvent);
        }

        // check if we reached the minimum size
        if (currentBatch.size() < size) {
            // done if no overflow
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewProcessIRStream();
            }
            return;
        }

        sendBatch();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    /**
     * This method updates child views and clears the batch of events.
     */
    protected void sendBatch() {
        // If there are child views and the batch was filled, fireStatementStopped update method
        if (this.hasViews()) {
            // Convert to object arrays
            EventBean[] newData = null;
            EventBean[] oldData = null;
            if (!currentBatch.isEmpty()) {
                newData = currentBatch.toArray(new EventBean[currentBatch.size()]);
            }
            if ((lastBatch != null) && (!lastBatch.isEmpty())) {
                oldData = lastBatch.toArray(new EventBean[lastBatch.size()]);
            }

            // Post new data (current batch) and old data (prior batch)
            if ((newData != null) || (oldData != null)) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewIndicate(this, lengthBatchViewFactory.getViewName(), newData, oldData);
                }
                updateChildren(newData, oldData);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewIndicate();
                }
            }
        }

        lastBatch = currentBatch;
        currentBatch = new LinkedHashSet<EventBean>();
    }

    /**
     * Returns true if the window is empty, or false if not empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        if (lastBatch != null) {
            if (!lastBatch.isEmpty()) {
                return false;
            }
        }
        return currentBatch.isEmpty();
    }

    public final Iterator<EventBean> iterator() {
        return currentBatch.iterator();
    }

    public final String toString() {
        return this.getClass().getName() +
                " size=" + size;
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(currentBatch, true, lengthBatchViewFactory.getViewName(), null);
        viewDataVisitor.visitPrimary(lastBatch, true, lengthBatchViewFactory.getViewName(), null);
    }

    public ViewFactory getViewFactory() {
        return lengthBatchViewFactory;
    }
}
