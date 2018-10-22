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
import com.espertech.esper.common.internal.filtersvc.FilterHandleCallback;

import java.util.Collection;

public class SupportFilterHandle implements FilterHandleCallback, FilterHandle {
    private int countInvoked;
    private EventBean lastEvent;

    public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
        countInvoked++;
        lastEvent = theEvent;
    }

    public boolean isSubSelect() {
        return false;
    }

    public int getCountInvoked() {
        return countInvoked;
    }

    public EventBean getLastEvent() {
        return lastEvent;
    }

    public void setCountInvoked(int countInvoked) {
        this.countInvoked = countInvoked;
    }

    public void setLastEvent(EventBean lastEvent) {
        this.lastEvent = lastEvent;
    }

    public int getAndResetCountInvoked() {
        int count = countInvoked;
        countInvoked = 0;
        return count;
    }

    public int getStatementId() {
        return 1;
    }

    public int getAgentInstanceId() {
        return -1;
    }
}
