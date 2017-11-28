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
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * A data view that aggregates events in a stream and releases them in one batch when a maximum number of events has
 * been collected.
 * <p>
 * The view works similar to a length_window but is not continuous, and similar to a time_batch however is not time-based
 * but reacts to the number of events.
 * <p>
 * The view releases the batched events, when a certain number of batched events has been reached or exceeded,
 * as new data to child views. The prior batch if
 * not empty is released as old data to any child views. The view doesn't release intervals with no old or new data.
 * It also does not collect old data published by a parent view.
 * <p>
 * If there are no events in the current and prior batch, the view will not invoke the update method of child views.
 */
public class LengthBatchView extends ViewSupport implements DataWindowView {
    // View parameters
    protected final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;
    private final LengthBatchViewFactory lengthBatchViewFactory;
    private final int size;
    private final ViewUpdatedCollection viewUpdatedCollection;

    // Current running windows
    protected ArrayDeque<EventBean> lastBatch = null;
    protected ArrayDeque<EventBean> currentBatch = new ArrayDeque<EventBean>();

    /**
     * Constructor.
     *
     * @param size                            is the number of events to batch
     * @param viewUpdatedCollection           is a collection that the view must update when receiving events
     * @param lengthBatchViewFactory          for copying this view in a group-by
     * @param agentInstanceViewFactoryContext context
     */
    public LengthBatchView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext,
                           LengthBatchViewFactory lengthBatchViewFactory,
                           int size,
                           ViewUpdatedCollection viewUpdatedCollection) {
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
        this.lengthBatchViewFactory = lengthBatchViewFactory;
        this.size = size;
        this.viewUpdatedCollection = viewUpdatedCollection;

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

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, lengthBatchViewFactory.getViewName(), newData, oldData);
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

            // update view buffer to serve expressions require access to events held
            if (viewUpdatedCollection != null) {
                viewUpdatedCollection.update(newData, oldData);
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
        currentBatch = new ArrayDeque<EventBean>();
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

    private static final Logger log = LoggerFactory.getLogger(LengthBatchView.class);
}
