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

import java.util.*;

import static com.espertech.esper.common.internal.epl.agg.access.sorted.AggregationMethodSortedWrapperNavigableMap.containsNotSupported;
import static com.espertech.esper.common.internal.epl.agg.access.sorted.AggregationMethodSortedWrapperNavigableMap.immutableException;
import static com.espertech.esper.common.internal.epl.agg.access.sorted.AggregatorAccessSortedImpl.checkedPayloadGetCollEvents;

public class AggregationMethodSortedWrapperSortedMap implements SortedMap<Object, Collection<EventBean>> {
    private final SortedMap<Object, Object> sorted;

    AggregationMethodSortedWrapperSortedMap(SortedMap<Object, Object> sorted) {
        this.sorted = sorted;
    }

    public SortedMap<Object, Collection<EventBean>> subMap(Object fromKey, Object toKey) {
        return new AggregationMethodSortedWrapperSortedMap(sorted.subMap(fromKey, toKey));
    }

    public SortedMap<Object, Collection<EventBean>> headMap(Object toKey) {
        return new AggregationMethodSortedWrapperSortedMap(sorted.headMap(toKey));
    }

    public SortedMap<Object, Collection<EventBean>> tailMap(Object fromKey) {
        return new AggregationMethodSortedWrapperSortedMap(sorted.tailMap(fromKey));
    }

    public Comparator<? super Object> comparator() {
        return sorted.comparator();
    }

    public Object firstKey() {
        return sorted.firstKey();
    }

    public Object lastKey() {
        return sorted.lastKey();
    }

    public Set<Object> keySet() {
        return Collections.unmodifiableSet(sorted.keySet());
    }

    public Collection<Collection<EventBean>> values() {
        return new AggregationMethodSortedWrapperCollection(sorted.values());
    }

    public Set<Entry<Object, Collection<EventBean>>> entrySet() {
        return new AggregationMethodSortedWrapperSet(sorted.entrySet());
    }

    public int size() {
        return sorted.size();
    }

    public boolean isEmpty() {
        return sorted.isEmpty();
    }

    public boolean containsKey(Object key) {
        return sorted.containsKey(key);
    }

    public boolean containsValue(Object value) {
        throw containsNotSupported();
    }

    public Collection<EventBean> get(Object key) {
        Object value = sorted.get(key);
        return value == null ? null : checkedPayloadGetCollEvents(value);
    }

    public Collection<EventBean> put(Object key, Collection<EventBean> value) {
        throw immutableException();
    }

    public Collection<EventBean> remove(Object key) {
        throw immutableException();
    }

    public void putAll(Map<?, ? extends Collection<EventBean>> m) {
        throw immutableException();
    }

    public void clear() {
        throw immutableException();
    }
}
