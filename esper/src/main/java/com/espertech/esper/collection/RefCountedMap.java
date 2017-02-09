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

/**
 * Reference-counting map based on a HashMap implementation that stores as a value a pair of value and reference counter.
 * The class provides a reference method that takes a key
 * and increments the reference count for the key. It also provides a de-reference method that takes a key and
 * decrements the reference count for the key, and removes the key if the reference count reached zero.
 * Null values are not allowed as keys.
 */
public class RefCountedMap<K, V> {
    private Map<K, Pair<V, Integer>> refMap;

    /**
     * Constructor.
     */
    public RefCountedMap() {
        refMap = new HashMap<K, Pair<V, Integer>>();
    }

    /**
     * Add and key and value with a reference count as one. If the key already exists, throw an exception.
     * Clients should use the "get" method first to check if the key exists.
     *
     * @param key   to add
     * @param value to add
     * @return value added
     */
    public V put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("Collection does not allow null key values");
        }
        if (refMap.containsKey(key)) {
            throw new IllegalStateException("Key value already in collection");
        }

        Pair<V, Integer> refValue = new Pair<V, Integer>(value, 1);
        refMap.put(key, refValue);

        return value;
    }

    /**
     * Get the value for a given key, returning null if the key was not found.
     *
     * @param key is the key to look up and return the value for
     * @return value for key, or null if key was not found
     */
    public V get(K key) {
        Pair<V, Integer> refValue = refMap.get(key);
        if (refValue == null) {
            return null;
        }
        return refValue.getFirst();
    }

    /**
     * Increase the reference count for a given key by one.
     * Throws an IllegalArgumentException if the key was not found.
     *
     * @param key is the key to increase the ref count for
     */
    public void reference(K key) {
        Pair<V, Integer> refValue = refMap.get(key);
        if (refValue == null) {
            throw new IllegalStateException("Key value not found in collection");
        }
        refValue.setSecond(refValue.getSecond() + 1);
    }

    /**
     * Decreases the reference count for a given key by one. Returns true if the reference count reaches zero.
     * Removes the key from the collection when the reference count reaches zero.
     * Throw an IllegalArgumentException if the key is not found.
     *
     * @param key to de-reference
     * @return true to indicate the reference count reached zero, false to indicate more references to the key exist.
     */
    public boolean dereference(K key) {
        Pair<V, Integer> refValue = refMap.get(key);
        if (refValue == null) {
            throw new IllegalStateException("Key value not found in collection");
        }

        int refCounter = refValue.getSecond();
        if (refCounter < 1) {
            throw new IllegalStateException("Unexpected reference counter value " + refValue.getSecond() +
                    " encountered for key " + key);
        }

        // Remove key on dereference of last reference
        if (refCounter == 1) {
            refMap.remove(key);
            return true;
        }

        refValue.setSecond(refCounter - 1);
        return false;
    }

    /**
     * Clear out the collection.
     */
    public void clear() {
        refMap.clear();
    }
}
