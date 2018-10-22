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
package com.espertech.esper.common.internal.view.groupwin;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.IterablesListIterator;
import com.espertech.esper.common.internal.view.core.View;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class MergeView extends ViewSupport {
    private final GroupByView groupByView;
    private final EventType eventType;
    private final Collection<View> parentViews;

    public MergeView(GroupByView groupByView, EventType eventType) {
        this.groupByView = groupByView;
        this.eventType = eventType;
        this.parentViews = new ArrayList<>(4);
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        groupByView.getChild().update(newData, oldData);
    }

    public EventType getEventType() {
        return eventType;
    }

    public Iterator<EventBean> iterator() {
        // The merge data view has multiple parent views which are AddPropertyValueView
        ArrayDeque<Iterable<EventBean>> iterables = new ArrayDeque<Iterable<EventBean>>();

        for (View dataView : parentViews) {
            iterables.add(dataView);
        }

        return new IterablesListIterator(iterables.iterator());
    }

    public void removeParentView(View parentView) {
        parentViews.remove(parentView);
    }

    public void addParentView(View parentView) {
        parentViews.add(parentView);
    }
}
