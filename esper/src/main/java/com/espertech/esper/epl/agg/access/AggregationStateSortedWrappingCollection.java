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
package com.espertech.esper.epl.agg.access;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

public class AggregationStateSortedWrappingCollection implements Collection {

    private final TreeMap<Object, Object> sorted;
    private final int size;

    public AggregationStateSortedWrappingCollection(TreeMap<Object, Object> sorted, int size) {
        this.sorted = sorted;
        this.size = size;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Iterator iterator() {
        return new AggregationStateSortedIterator(sorted, false);
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException("Partial implementation");
    }

    public Object[] toArray(Object[] a) {
        throw new UnsupportedOperationException("Partial implementation");
    }

    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Partial implementation");
    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException("Read-only implementation");
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Read-only implementation");
    }

    public boolean containsAll(Collection c) {
        throw new UnsupportedOperationException("Read-only implementation");
    }

    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException("Read-only implementation");
    }

    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException("Read-only implementation");
    }

    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException("Read-only implementation");
    }

    public void clear() {
        throw new UnsupportedOperationException("Read-only implementation");
    }
}
