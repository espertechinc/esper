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

import java.util.List;

public class EventArrayAndSortKeyArray {
    private final List<EventBean>[] eventsPerLevel;
    private final List<Object>[] sortKeyPerLevel;

    public EventArrayAndSortKeyArray(List<EventBean>[] eventsPerLevel, List<Object>[] sortKeyPerLevel) {
        this.eventsPerLevel = eventsPerLevel;
        this.sortKeyPerLevel = sortKeyPerLevel;
    }

    public List<EventBean>[] getEventsPerLevel() {
        return eventsPerLevel;
    }

    public List<Object>[] getSortKeyPerLevel() {
        return sortKeyPerLevel;
    }

    public void reset() {
        for (List<EventBean> anEventsPerLevel : eventsPerLevel) {
            anEventsPerLevel.clear();
        }
        if (sortKeyPerLevel != null) {
            for (List<Object> anSortKeyPerLevel : sortKeyPerLevel) {
                anSortKeyPerLevel.clear();
            }
        }
    }
}
