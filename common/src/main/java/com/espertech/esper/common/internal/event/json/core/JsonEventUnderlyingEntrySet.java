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
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class JsonEventUnderlyingEntrySet implements Set<Map.Entry<String, Object>> {
    private final JsonEventObjectBase jeu;
    private final Set<Map.Entry<String, Object>> entrySet;

    public JsonEventUnderlyingEntrySet(JsonEventObjectBase jeu) {
        this.jeu = jeu;
        this.entrySet = jeu.getJsonValues().entrySet();
    }

    public int size() {
        return jeu.size();
    }

    public Iterator<Map.Entry<String, Object>> iterator() {
        return new JsonEventUnderlyingEntrySetIterator(jeu, entrySet);
    }

    public boolean isEmpty() {
        return jeu.getNativeSize() == 0 && jeu.getJsonValues().isEmpty();
    }

    public Object[] toArray() {
        Map.Entry[] result = new Map.Entry[size()];
        fillArray(result);
        return result;
    }

    public boolean contains(Object o) {
        Iterator<Map.Entry<String, Object>> it = iterator();
        if (o == null) {
            while (it.hasNext()) {
                if (it.next() == null) {
                    return true;
                }
            }
        } else {
            while (it.hasNext()) {
                if (o.equals(it.next())) {
                    return true;
                }
            }
        }
        return false;
    }

    public <T> T[] toArray(T[] a) {
        int nativeSize = jeu.getNativeSize();
        if (nativeSize == 0) {
            return entrySet.toArray(a);
        }
        Map.Entry[] array = (Map.Entry[]) a;
        int size = size();
        if (a.length >= size) {
            fillArray(array);
            return a;
        }
        Map.Entry[] result = new Map.Entry[size()];
        fillArray(result);
        return (T[]) result;
    }

    public boolean containsAll(Collection<?> c) {
        if (jeu.getNativeSize() == 0) {
            return entrySet.containsAll(c);
        }
        for (Object key : c) {
            if (!contains(key)) {
                return false;
            }
        }
        return true;
    }

    public boolean add(Map.Entry<String, Object> stringObjectEntry) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends Map.Entry<String, Object>> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean removeIf(Predicate<? super Map.Entry<String, Object>> filter) {
        throw new UnsupportedOperationException();
    }

    private void fillArray(Map.Entry[] result) {
        int size = jeu.getNativeSize();
        for (int i = 0; i < size; i++) {
            result[i] = jeu.getNativeEntry(i);
        }
        Iterator<Map.Entry<String, Object>> it = entrySet.iterator();
        while (it.hasNext()) {
            result[size++] = it.next();
        }
    }
}
