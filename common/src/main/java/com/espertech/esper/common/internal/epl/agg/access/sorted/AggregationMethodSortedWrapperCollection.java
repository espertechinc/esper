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
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.espertech.esper.common.internal.epl.agg.access.sorted.AggregationMethodSortedWrapperNavigableMap.containsNotSupported;
import static com.espertech.esper.common.internal.epl.agg.access.sorted.AggregationMethodSortedWrapperNavigableMap.immutableException;

public class AggregationMethodSortedWrapperCollection implements Collection<Collection<EventBean>> {
    private final Collection<Object> values;

    public AggregationMethodSortedWrapperCollection(Collection<Object> values) {
        this.values = values;
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean contains(Object o) {
        throw containsNotSupported();
    }

    public Iterator<Collection<EventBean>> iterator() {
        return new AggregationMethodSortedWrapperValueIterator(values.iterator());
    }

    public Object[] toArray() {
        Collection<EventBean>[] collections = new Collection[values.size()];
        int index = 0;
        for (Object value : values) {
            collections[index++] = AggregatorAccessSortedImpl.checkedPayloadGetCollEvents(value);
        }
        return collections;
    }

    public <T> T[] toArray(T[] a) {
        return (T[]) toArray();
    }

    public boolean add(Collection<EventBean> eventBeans) {
        throw immutableException();
    }

    public boolean remove(Object o) {
        throw immutableException();
    }

    public boolean containsAll(Collection<?> c) {
        throw containsNotSupported();
    }

    public boolean addAll(Collection<? extends Collection<EventBean>> c) {
        throw immutableException();
    }

    public boolean removeAll(Collection<?> c) {
        throw immutableException();
    }

    public boolean retainAll(Collection<?> c) {
        throw immutableException();
    }

    public void clear() {
        throw immutableException();
    }

    public boolean removeIf(Predicate<? super Collection<EventBean>> filter) {
        throw immutableException();
    }

    public Spliterator<Collection<EventBean>> spliterator() {
        return Spliterators.spliterator(iterator(), /* initial size*/ 0L, Spliterator.NONNULL);
    }

    public Stream<Collection<EventBean>> stream() {
        return values.stream().map(o -> AggregatorAccessSortedImpl.checkedPayloadGetCollEvents(o));
    }

    public Stream<Collection<EventBean>> parallelStream() {
        return values.parallelStream().map(o -> AggregatorAccessSortedImpl.checkedPayloadGetCollEvents(o));
    }

    public void forEach(Consumer<? super Collection<EventBean>> action) {
        for (Object value : values) {
            action.accept(AggregatorAccessSortedImpl.checkedPayloadGetCollEvents(value));
        }
    }
}
