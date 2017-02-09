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
package com.espertech.esper.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RefCountedSetAtomicInteger<K> {
    private Map<K, Object> refs;

    public RefCountedSetAtomicInteger() {
        refs = new HashMap<K, Object>();
    }

    /**
     * Clear out the collection.
     */
    public void clear() {
        refs.clear();
    }

    public boolean add(K key) {
        Object count = refs.get(key);
        if (count == null) {
            refs.put(key, 1);
            return true;
        } else if (count instanceof AtomicInteger) {
            ((AtomicInteger) count).incrementAndGet();
            return false;
        } else {
            refs.put(key, new AtomicInteger(2));
            return false;
        }
    }

    public boolean remove(K key) {
        Object count = refs.get(key);
        if (count == null) {
            return false;
        } else if (count instanceof AtomicInteger) {
            int val = ((AtomicInteger) count).decrementAndGet();
            if (val == 0) {
                refs.remove(key);
                return true;
            }
            return false;
        } else {
            refs.remove(key);
            return true;
        }
    }

    public void removeAll(K key) {
        refs.remove(key);
    }

    public boolean isEmpty() {
        return refs.isEmpty();
    }

    public Map<K, Object> getRefs() {
        return refs;
    }
}
