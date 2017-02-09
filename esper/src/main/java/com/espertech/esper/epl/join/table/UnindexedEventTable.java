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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;

import java.util.Set;

/**
 * Simple table of events without an index.
 */
public abstract class UnindexedEventTable implements EventTable {
    private final int streamNum;

    public abstract Set<EventBean> getEventSet();

    /**
     * Ctor.
     *
     * @param streamNum is the indexed stream's number
     */
    public UnindexedEventTable(int streamNum) {
        this.streamNum = streamNum;
    }

    public String toString() {
        return toQueryPlan();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " streamNum=" + streamNum;
    }

    public int getNumKeys() {
        return 0;
    }

    public EventTableOrganization getOrganization() {
        return new EventTableOrganization(null, false, false, streamNum, null, EventTableOrganizationType.UNORGANIZED);
    }
}
