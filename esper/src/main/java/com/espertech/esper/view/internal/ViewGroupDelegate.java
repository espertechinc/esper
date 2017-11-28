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
import com.espertech.esper.view.GroupableView;
import com.espertech.esper.view.ViewFactory;
import com.espertech.esper.view.ViewSupport;

import java.util.Iterator;

public class ViewGroupDelegate extends ViewSupport implements GroupableView {

    private final ViewFactory viewFactory;

    public ViewGroupDelegate(ViewFactory viewFactory) {
        this.viewFactory = viewFactory;
    }

    public EventType getEventType() {
        return viewFactory.getEventType();
    }

    public Iterator<EventBean> iterator() {
        if (parent != null) {
            return parent.iterator();
        }
        return CollectionUtil.NULL_EVENT_ITERATOR;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        // no action
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }
}
