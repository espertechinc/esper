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
package com.espertech.esper.epl.core.resultset.rowpergrouprollup;

import com.espertech.esper.client.EventBean;

public class EventsAndSortKeysPair {
    private final EventBean[] events;
    private final Object[] sortKeys;

    public EventsAndSortKeysPair(EventBean[] events, Object[] sortKeys) {
        this.events = events;
        this.sortKeys = sortKeys;
    }

    public EventBean[] getEvents() {
        return events;
    }

    public Object[] getSortKeys() {
        return sortKeys;
    }
}
