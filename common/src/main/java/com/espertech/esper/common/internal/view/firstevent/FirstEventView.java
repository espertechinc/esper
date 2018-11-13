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
package com.espertech.esper.common.internal.view.firstevent;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.SingleEventIterator;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.view.core.DataWindowView;
import com.espertech.esper.common.internal.view.core.ViewDataVisitor;
import com.espertech.esper.common.internal.view.core.ViewFactory;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.Iterator;

/**
 * View retaining the very first event. Any subsequent events received are simply discarded and not
 * entered into either insert or remove stream. Only the very first event received is entered into the remove stream.
 * <p>
 * The view thus never posts a remove stream unless explicitly deleted from when used with a named window.
 */
public class FirstEventView extends ViewSupport implements DataWindowView {
    /**
     * The first new element posted from a parent view.
     */
    private final FirstEventViewFactory viewFactory;
    private final AgentInstanceContext agentInstanceContext;
    protected EventBean firstEvent;

    public FirstEventView(FirstEventViewFactory viewFactory, AgentInstanceContext agentInstanceContext) {
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

        if ((child != null) && ((newDataToPost != null) || (oldDataToPost != null))) {
            agentInstanceContext.getInstrumentationProvider().qViewIndicate(viewFactory, newDataToPost, oldDataToPost);
            child.update(newDataToPost, oldDataToPost);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
        }
        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
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
        viewDataVisitor.visitPrimary(firstEvent, viewFactory.getViewName());
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }
}
