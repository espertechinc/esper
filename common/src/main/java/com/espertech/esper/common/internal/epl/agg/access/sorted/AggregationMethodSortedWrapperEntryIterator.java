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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class AggregationMethodSortedWrapperEntryIterator implements Iterator<Map.Entry<Object, Collection<EventBean>>> {
    private final Iterator<Map.Entry<Object, Object>> iterator;

    public AggregationMethodSortedWrapperEntryIterator(Iterator<Map.Entry<Object, Object>> iterator) {
        this.iterator = iterator;
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public Map.Entry<Object, Collection<EventBean>> next() {
        Map.Entry<Object, Object> next = iterator.next();
        return new AbstractMap.SimpleEntry<>(next.getKey(), AggregatorAccessSortedImpl.checkedPayloadGetCollEvents(next.getValue()));
    }
}
