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
import com.espertech.esper.collection.IterablesListIterator;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.view.View;
import com.espertech.esper.view.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The merge view works together with a group view that splits the data in a stream to multiple subviews, based on
 * a key index. Every group view requires a merge view to merge the many subviews back into a single view.
 * Typically the last view in a chain containing a group view is a merge view.
 * The merge view has no other responsibility then becoming the single last instance in the chain
 * to which external listeners for updates can be attached to receive updates for the many subviews
 * that have this merge view as common child views.
 * The parent view of this view is generally the AddPropertyValueView that adds the grouped-by information
 * back into the data.
 */
public final class MergeView extends ViewSupport implements MergeViewMarker {
    private final AgentInstanceViewFactoryChainContext agentInstanceContext;
    private final Collection<View> parentViews;
    private final ExprNode[] groupFieldNames;
    private final EventType eventType;
    private final boolean removable;

    public MergeView(AgentInstanceViewFactoryChainContext agentInstanceContext,
                     ExprNode[] groupCriteria,
                     EventType resultEventType,
                     boolean removable) {
        this.removable = removable;
        if (!removable) {
            parentViews = new ArrayDeque<View>();
        } else {
            parentViews = new HashSet<View>();
        }
        this.agentInstanceContext = agentInstanceContext;
        this.groupFieldNames = groupCriteria;
        this.eventType = resultEventType;
    }

    /**
     * Returns the field name that contains the values to group by.
     *
     * @return field name providing group key value
     */
    public final ExprNode[] getGroupFieldNames() {
        return groupFieldNames;
    }

    /**
     * Add a parent data merge view.
     *
     * @param parentView is the parent data merge view to add
     */
    public final void addParentView(View parentView) {
        parentViews.add(parentView);
    }

    public final EventType getEventType() {
        // The schema is the parent view's type, or the type plus the added field(s)
        return eventType;
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        updateChildren(newData, oldData);
    }

    public final Iterator<EventBean> iterator() {
        // The merge data view has multiple parent views which are AddPropertyValueView
        ArrayDeque<Iterable<EventBean>> iterables = new ArrayDeque<Iterable<EventBean>>();

        for (View dataView : parentViews) {
            iterables.add(dataView);
        }

        return new IterablesListIterator(iterables.iterator());
    }

    public final String toString() {
        return this.getClass().getName() + " groupFieldName=" + Arrays.toString(groupFieldNames);
    }

    public void removeParentView(View view) {
        parentViews.remove(view);
    }

    private static final Logger log = LoggerFactory.getLogger(MergeView.class);
}
