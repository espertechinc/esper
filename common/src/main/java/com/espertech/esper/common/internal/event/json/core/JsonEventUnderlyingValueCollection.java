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
package com.espertech.esper.common.internal.event.json.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

public class JsonEventUnderlyingValueCollection implements Collection<Object> {

    private final JsonEventObjectBase jeu;
    private final Collection<Object> values;

    public JsonEventUnderlyingValueCollection(JsonEventObjectBase jeu, Collection<Object> values) {
        this.jeu = jeu;
        this.values = values;
    }

    public int size() {
        return jeu.size();
    }

    public boolean isEmpty() {
        return jeu.isEmpty();
    }

    public Object[] toArray() {
        Object[] result = new Object[size()];
        fillArray(result);
        return result;
    }

    public Iterator<Object> iterator() {
        return new JsonEventUnderlyingValueIterator(jeu, values.iterator());
    }

    public boolean contains(Object value) {
        if (value == null) {
            for (int i = 0; i < jeu.getNativeSize(); i++) {
                if (jeu.getNativeValue(i) == null) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < jeu.getNativeSize(); i++) {
                if (value.equals(jeu.getNativeValue(i))) {
                    return true;
                }
            }
        }
        return values.contains(value);
    }

    public boolean containsAll(Collection<?> c) {
        for (Object e : c) {
            if (!contains(e)) {
                return false;
            }
        }
        return true;
    }

    public <T> T[] toArray(T[] array) {
        int nativeSize = jeu.getNativeSize();
        if (nativeSize == 0) {
            return values.toArray(array);
        }
        int size = size();
        if (array.length >= size) {
            fillArray(array);
            return array;
        }
        Object[] result = new Object[size()];
        fillArray(result);
        return (T[]) result;
    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean removeIf(Predicate<? super Object> filter) {
        throw new UnsupportedOperationException();
    }

    private void fillArray(Object[] array) {
        int size = jeu.getNativeSize();
        for (int i = 0; i < size; i++) {
            array[i] = jeu.getNativeValue(i);
        }
        Iterator<Object> it = values.iterator();
        while (it.hasNext()) {
            array[size++] = it.next();
        }
    }
}
