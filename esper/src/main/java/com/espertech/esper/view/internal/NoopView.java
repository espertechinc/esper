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
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.*;

import java.util.Iterator;

public class NoopView extends ViewSupport implements DataWindowView, CloneableView {

    private final NoopViewFactory viewFactory;

    public NoopView(NoopViewFactory viewFactory) {
        this.viewFactory = viewFactory;
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }

    public View cloneView() {
        return viewFactory.makeView(null);
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
    }

    public EventType getEventType() {
        return viewFactory.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return CollectionUtil.NULL_EVENT_ITERATOR;
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
    }
}
