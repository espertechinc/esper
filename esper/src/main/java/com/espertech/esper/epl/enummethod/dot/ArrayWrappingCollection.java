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
package com.espertech.esper.epl.enummethod.dot;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

public class ArrayWrappingCollection implements Collection {

    private Object array;

    public ArrayWrappingCollection(Object array) {
        if (array == null) {
            throw new IllegalArgumentException("Null array provided");
        }
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Non-array value provided to collection, expected array type but received type " + array.getClass().getName());
        }
        this.array = array;
    }

    public int size() {
        return Array.getLength(array);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Iterator iterator() {
        return new ArrayWrappingIterator(array);
    }

    public Object getArray() {
        return array;
    }

    public Object[] toArray() {
        return (Object[]) array;
    }

    public Object[] toArray(Object[] a) {
        return (Object[]) array;
    }

    public boolean contains(Object o) {
        if (array == null) {
            return false;
        }
        int len = Array.getLength(array);
        for (int i = 0; i < len; i++) {
            Object other = Array.get(array, i);
            if (other != null && other.equals(o)) {
                return true;
            }
        }
        return false;
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
