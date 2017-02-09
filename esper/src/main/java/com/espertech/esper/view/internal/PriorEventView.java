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
package com.espertech.esper.view.internal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.view.ViewDataVisitable;
import com.espertech.esper.view.ViewDataVisitor;
import com.espertech.esper.view.ViewSupport;
import com.espertech.esper.view.Viewable;

import java.util.Iterator;

/**
 * View that provides access to prior events posted by the parent view for use by 'prior' expression nodes.
 */
public class PriorEventView extends ViewSupport implements ViewDataVisitable {
    private Viewable parent;
    protected ViewUpdatedCollection buffer;

    /**
     * Ctor.
     *
     * @param buffer is handling the actual storage of events for use in the 'prior' expression
     */
    public PriorEventView(ViewUpdatedCollection buffer) {
        this.buffer = buffer;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        buffer.update(newData, oldData);
        this.updateChildren(newData, oldData);
    }

    public void setParent(Viewable parent) {
        this.parent = parent;
    }

    /**
     * Returns the underlying buffer used for access to prior events.
     *
     * @return buffer
     */
    public ViewUpdatedCollection getBuffer() {
        return buffer;
    }

    public EventType getEventType() {
        return parent.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return parent.iterator();
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(buffer, "Prior");
    }
}
