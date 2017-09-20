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
package com.espertech.esper.epl.core.resultset.rowpergroup;

import com.espertech.esper.client.EventBean;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResultSetProcessorRowPerGroupUnboundHelperImpl implements ResultSetProcessorRowPerGroupUnboundHelper {

    private final Map<Object, EventBean> groupReps = new LinkedHashMap<Object, EventBean>();

    public void put(Object key, EventBean event) {
        groupReps.put(key, event);
    }

    public Iterator<EventBean> valueIterator() {
        return groupReps.values().iterator();
    }

    public void removedAggregationGroupKey(Object key) {
        groupReps.remove(key);
    }

    public void destroy() {
        // no action required
    }
}
