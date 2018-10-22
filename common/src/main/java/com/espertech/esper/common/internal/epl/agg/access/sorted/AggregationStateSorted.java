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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.client.EventBean;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import static com.espertech.esper.common.internal.epl.agg.access.sorted.AggregatorAccessSortedImpl.checkedPayloadMayDeque;

public class AggregationStateSorted {
    private TreeMap<Object, Object> sorted;
    private int size;

    public TreeMap<Object, Object> getSorted() {
        return sorted;
    }

    public void setSorted(TreeMap<Object, Object> sorted) {
        this.sorted = sorted;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public EventBean getFirstValue() {
        if (sorted.isEmpty()) {
            return null;
        }
        Map.Entry<Object, Object> max = sorted.firstEntry();
        return checkedPayloadMayDeque(max.getValue());
    }

    public EventBean getLastValue() {
        if (sorted.isEmpty()) {
            return null;
        }
        Map.Entry<Object, Object> min = sorted.lastEntry();
        return checkedPayloadMayDeque(min.getValue());
    }

    public Collection<EventBean> collectionReadOnly() {
        return new AggregationStateSortedWrappingCollection(sorted, size);
    }
}
