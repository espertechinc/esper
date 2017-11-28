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
import com.espertech.esper.collection.SingleEventIterator;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;

import java.util.Iterator;

/**
 * View retaining the very first event. Any subsequent events received are simply discarded and not
 * entered into either insert or remove stream. Only the very first event received is entered into the remove stream.
 * <p>
 * The view thus never posts a remove stream unless explicitly deleted from when used with a named window.
 */
public class FirstElementView extends ViewSupport implements DataWindowView {
    /**
     * The first new element posted from a parent view.
     */
    private final FirstElementViewFactory viewFactory;
    protected EventBean firstEvent;

    public FirstElementView(FirstElementViewFactory viewFactory) {
        this.viewFactory = viewFactory;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, FirstElementViewFactory.NAME, newData, oldData);
        }

        EventBean[] newDataToPost = null;
        EventBean[] oldDataToPost = null;

        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                if (oldData[i] == firstEvent) {
                    oldDataToPost = new EventBean[]{firstEvent};
                    firstEvent = null;
                }
            }
        }

        if ((newData != null) && (newData.length != 0)) {
            if (firstEvent == null) {
                firstEvent = newData[0];
                newDataToPost = new EventBean[]{firstEvent};
            }
        }

        if ((this.hasViews()) && ((newDataToPost != null) || (oldDataToPost != null))) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, FirstElementViewFactory.NAME, newDataToPost, oldDataToPost);
            }
            updateChildren(newDataToPost, oldDataToPost);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public final Iterator<EventBean> iterator() {
        return new SingleEventIterator(firstEvent);
    }

    public final String toString() {
        return this.getClass().getName();
    }

    public void setFirstEvent(EventBean firstEvent) {
        this.firstEvent = firstEvent;
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(firstEvent, FirstElementViewFactory.NAME);
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }
}
