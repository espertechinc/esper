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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.espertech.esper.common.internal.epl.agg.access.sorted.AggregationMethodSortedWrapperNavigableMap.containsNotSupported;
import static com.espertech.esper.common.internal.epl.agg.access.sorted.AggregationMethodSortedWrapperNavigableMap.immutableException;
import static com.espertech.esper.common.internal.epl.agg.access.sorted.AggregatorAccessSortedImpl.checkedPayloadGetCollEvents;

public class AggregationMethodSortedWrapperSet implements Set<Map.Entry<Object, Collection<EventBean>>> {
    private final Set<Map.Entry<Object, Object>> entrySet;

    public AggregationMethodSortedWrapperSet(Set<Map.Entry<Object, Object>> entrySet) {
        this.entrySet = entrySet;
    }

    public int size() {
        return entrySet.size();
    }

    public boolean isEmpty() {
        return entrySet.isEmpty();
    }

    public boolean contains(Object o) {
        throw containsNotSupported();
    }

    public Iterator<Map.Entry<Object, Collection<EventBean>>> iterator() {
        return new AggregationMethodSortedWrapperEntryIterator(entrySet.iterator());
    }

    public Object[] toArray() {
        Map.Entry<Object, Collection<EventBean>>[] arr = new Map.Entry[entrySet.size()];
        int index = 0;
        for (Map.Entry<Object, Object> entry : entrySet) {
            arr[index++] = new AbstractMap.SimpleEntry<>(entry.getKey(), checkedPayloadGetCollEvents(entry.getValue()));
        }
        return arr;
    }

    public <T> T[] toArray(T[] a) {
        return (T[]) toArray();
    }

    public boolean add(Map.Entry<Object, Collection<EventBean>> objectCollectionEntry) {
        throw immutableException();
    }

    public boolean remove(Object o) {
        throw immutableException();
    }

    public boolean containsAll(Collection<?> c) {
        throw containsNotSupported();
    }

    public boolean addAll(Collection<? extends Map.Entry<Object, Collection<EventBean>>> c) {
        throw immutableException();
    }

    public boolean retainAll(Collection<?> c) {
        throw immutableException();
    }

    public boolean removeAll(Collection<?> c) {
        throw immutableException();
    }

    public void clear() {
        throw immutableException();
    }

    public Spliterator<Map.Entry<Object, Collection<EventBean>>> spliterator() {
        return Spliterators.spliterator(iterator(), /* initial size*/ 0L, Spliterator.NONNULL);
    }

    public boolean removeIf(Predicate<? super Map.Entry<Object, Collection<EventBean>>> filter) {
        throw immutableException();
    }

    public Stream<Map.Entry<Object, Collection<EventBean>>> stream() {
        return entrySet.stream().map(entry -> toEntry(entry));
    }

    public Stream<Map.Entry<Object, Collection<EventBean>>> parallelStream() {
        return entrySet.parallelStream().map(entry -> toEntry(entry));
    }

    public void forEach(Consumer<? super Map.Entry<Object, Collection<EventBean>>> action) {
        entrySet.forEach(entry -> action.accept(toEntry(entry)));
    }

    private Map.Entry<Object, Collection<EventBean>> toEntry(Map.Entry<Object, Object> entry) {
        return new AbstractMap.SimpleEntry<>(entry.getKey(), checkedPayloadGetCollEvents(entry.getValue()));
    }
}
