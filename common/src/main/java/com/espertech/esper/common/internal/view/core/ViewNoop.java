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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.Iterator;

public class ViewNoop implements View {

    public final static ViewNoop INSTANCE = new ViewNoop();

    private ViewNoop() {
    }

    public Viewable getParent() {
        return null;
    }

    public void setParent(Viewable parent) {
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
    }

    public void setChild(View view) {
    }

    public View getChild() {
        return null;
    }

    public EventType getEventType() {
        throw new IllegalStateException("Information not available");
    }

    public Iterator<EventBean> iterator() {
        return CollectionUtil.NULL_EVENT_ITERATOR;
    }
}
