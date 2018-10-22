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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SupportEventEvaluator implements EventEvaluator {
    private int countInvoked;
    private EventBean lastEvent;
    private Collection<FilterHandle> lastMatches;

    public void matchEvent(EventBean theEvent, Collection<FilterHandle> matches) {
        countInvoked++;
        lastEvent = theEvent;
        lastMatches = matches;
    }

    public EventBean getLastEvent() {
        return lastEvent;
    }

    public Collection<FilterHandle> getLastMatches() {
        return lastMatches;
    }

    public void setCountInvoked(int countInvoked) {
        this.countInvoked = countInvoked;
    }

    public void setLastEvent(EventBean lastEvent) {
        this.lastEvent = lastEvent;
    }

    public void setLastMatches(List<FilterHandle> lastMatches) {
        this.lastMatches = lastMatches;
    }

    public int getAndResetCountInvoked() {
        int count = countInvoked;
        countInvoked = 0;
        return count;
    }

    public void getTraverseStatement(EventTypeIndexTraverse traverse, Set<Integer> statementIds, ArrayDeque<FilterItem> evaluatorStack) {
        throw new UnsupportedOperationException();
    }
}
