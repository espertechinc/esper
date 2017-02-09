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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class IntersectAsymetricViewLocalState {
    private final EventBean[][] oldEventsPerView;
    private final Set<EventBean> removalEvents = new HashSet<EventBean>();
    private final ArrayDeque<EventBean> newEvents = new ArrayDeque<EventBean>();

    private EventBean[] newDataChildView;
    private boolean hasRemovestreamData;
    private boolean retainObserverEvents;
    private boolean discardObserverEvents;
    private Set<EventBean> oldEvents = new HashSet<EventBean>();

    public IntersectAsymetricViewLocalState(EventBean[][] oldEventsPerView) {
        this.oldEventsPerView = oldEventsPerView;
    }

    public EventBean[][] getOldEventsPerView() {
        return oldEventsPerView;
    }

    public Set<EventBean> getRemovalEvents() {
        return removalEvents;
    }

    public ArrayDeque<EventBean> getNewEvents() {
        return newEvents;
    }

    public EventBean[] getNewDataChildView() {
        return newDataChildView;
    }

    public void setNewDataChildView(EventBean[] newDataChildView) {
        this.newDataChildView = newDataChildView;
    }

    public boolean hasRemovestreamData() {
        return hasRemovestreamData;
    }

    public void setHasRemovestreamData(boolean isHasRemovestreamData) {
        this.hasRemovestreamData = isHasRemovestreamData;
    }

    public boolean isRetainObserverEvents() {
        return retainObserverEvents;
    }

    public void setIsRetainObserverEvents(boolean isRetainObserverEvents) {
        this.retainObserverEvents = isRetainObserverEvents;
    }

    public boolean isDiscardObserverEvents() {
        return discardObserverEvents;
    }

    public void setIsDiscardObserverEvents(boolean isDiscardObserverEvents) {
        this.discardObserverEvents = isDiscardObserverEvents;
    }

    public Set<EventBean> getOldEvents() {
        return oldEvents;
    }

    public void setOldEvents(Set<EventBean> oldEvents) {
        this.oldEvents = oldEvents;
    }
}