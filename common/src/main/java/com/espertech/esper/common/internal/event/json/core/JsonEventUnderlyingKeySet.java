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
import java.util.Set;
import java.util.function.Predicate;

public class JsonEventUnderlyingKeySet implements Set<String> {
    private final JsonEventObjectBase jeu;
    private final Set<String> keySet;

    public JsonEventUnderlyingKeySet(JsonEventObjectBase jsonEventUnderlyingBase) {
        this.jeu = jsonEventUnderlyingBase;
        this.keySet = jeu.getJsonValues().keySet();
    }

    public int size() {
        return jeu.size();
    }

    public boolean isEmpty() {
        return jeu.isEmpty();
    }

    public Iterator<String> iterator() {
        return new JsonEventUnderlyingKeySetIterator(jeu, keySet);
    }

    public boolean contains(Object value) {
        return jeu.containsKey(value);
    }

    public Object[] toArray() {
        if (jeu.getNativeSize() == 0) {
            return keySet.toArray();
        }
        String[] result = new String[size()];
        fillArray(result);
        return result;
    }

    public boolean containsAll(Collection<?> c) {
        if (jeu.getNativeSize() == 0) {
            return keySet.containsAll(c);
        }
        for (Object key : c) {
            if (!contains(key)) {
                return false;
            }
        }
        return true;
    }

    public <T> T[] toArray(T[] a) {
        int nativeSize = jeu.getNativeSize();
        if (nativeSize == 0) {
            return keySet.toArray(a);
        }
        String[] array = (String[]) a;
        int size = size();
        if (a.length >= size) {
            fillArray(array);
            return a;
        }
        String[] result = new String[size()];
        fillArray(result);
        return (T[]) result;
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean add(String s) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends String> c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeIf(Predicate<? super String> filter) {
        throw new UnsupportedOperationException();
    }

    private void fillArray(String[] array) {
        int size = jeu.getNativeSize();
        for (int i = 0; i < size; i++) {
            array[i] = jeu.getNativeKey(i);
        }
        Iterator<String> it = keySet.iterator();
        while (it.hasNext()) {
            array[size++] = it.next();
        }
    }
}
