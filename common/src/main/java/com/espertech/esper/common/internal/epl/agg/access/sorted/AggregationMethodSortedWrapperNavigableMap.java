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

import static com.espertech.esper.common.internal.epl.agg.access.sorted.AggregatorAccessSortedImpl.checkedPayloadGetCollEvents;

public class AggregationMethodSortedWrapperNavigableMap implements NavigableMap<Object, Collection<EventBean>> {
    private final NavigableMap<Object, Object> sorted;

    AggregationMethodSortedWrapperNavigableMap(NavigableMap<Object, Object> sorted) {
        this.sorted = sorted;
    }

    public Entry<Object, Collection<EventBean>> lowerEntry(Object key) {
        return toEntry(sorted.lowerEntry(key));
    }

    public Object lowerKey(Object key) {
        return sorted.lowerKey(key);
    }

    public Entry<Object, Collection<EventBean>> floorEntry(Object key) {
        return toEntry(sorted.floorEntry(key));
    }

    public Object floorKey(Object key) {
        return sorted.floorKey(key);
    }

    public Entry<Object, Collection<EventBean>> ceilingEntry(Object key) {
        return toEntry(sorted.ceilingEntry(key));
    }

    public Object ceilingKey(Object key) {
        return sorted.ceilingKey(key);
    }

    public Entry<Object, Collection<EventBean>> higherEntry(Object key) {
        return toEntry(sorted.higherEntry(key));
    }

    public Object higherKey(Object key) {
        return sorted.higherKey(key);
    }

    public Entry<Object, Collection<EventBean>> firstEntry() {
        return toEntry(sorted.firstEntry());
    }

    public Entry<Object, Collection<EventBean>> lastEntry() {
        return toEntry(sorted.lastEntry());
    }

    public NavigableMap<Object, Collection<EventBean>> descendingMap() {
        return new AggregationMethodSortedWrapperNavigableMap(sorted.descendingMap());
    }

    public NavigableSet<Object> navigableKeySet() {
        return new AggregationMethodSortedWrapperNavigableSet(sorted.navigableKeySet());
    }

    public NavigableSet<Object> descendingKeySet() {
        return new AggregationMethodSortedWrapperNavigableSet(sorted.descendingKeySet());
    }

    public NavigableMap<Object, Collection<EventBean>> subMap(Object fromKey, boolean fromInclusive, Object toKey, boolean toInclusive) {
        return new AggregationMethodSortedWrapperNavigableMap(sorted.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    public NavigableMap<Object, Collection<EventBean>> headMap(Object toKey, boolean inclusive) {
        return new AggregationMethodSortedWrapperNavigableMap(sorted.headMap(toKey, inclusive));
    }

    public NavigableMap<Object, Collection<EventBean>> tailMap(Object fromKey, boolean inclusive) {
        return new AggregationMethodSortedWrapperNavigableMap(sorted.tailMap(fromKey, inclusive));
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

    public Entry<Object, Collection<EventBean>> pollFirstEntry() {
        throw immutableException();
    }

    public Entry<Object, Collection<EventBean>> pollLastEntry() {
        throw immutableException();
    }

    public void putAll(Map<?, ? extends Collection<EventBean>> m) {
        throw immutableException();
    }

    public void clear() {
        throw immutableException();
    }

    private Entry<Object, Collection<EventBean>> toEntry(Entry<Object, Object> entry) {
        return new AbstractMap.SimpleEntry<>(entry.getKey(), checkedPayloadGetCollEvents(entry.getValue()));
    }

    static UnsupportedOperationException immutableException() {
        return new UnsupportedOperationException("Mutation operations are not supported");
    }

    static UnsupportedOperationException containsNotSupported() {
        throw new UnsupportedOperationException("Contains-method is not supported");
    }
}
