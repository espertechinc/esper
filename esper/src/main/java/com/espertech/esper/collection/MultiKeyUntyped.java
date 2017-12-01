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

import java.io.Serializable;
import java.util.Arrays;

/**
 * Functions as a key value for Maps where keys need to be composite values.
 * The class allows a Map that uses MultiKeyUntyped entries for key values to use multiple objects as keys.
 * It calculates the hashCode from the key objects on construction and caches the hashCode.
 */
public final class MultiKeyUntyped implements Serializable {
    private final Object[] keys;
    private final int hashCode;
    private static final long serialVersionUID = -3890626073105861216L;

    /**
     * Constructor for multiple keys supplied in an object array.
     *
     * @param keys is an array of key objects
     */
    public MultiKeyUntyped(Object[] keys) {
        if (keys == null) {
            throw new IllegalArgumentException("The array of keys must not be null");
        }

        int total = 0;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                total *= 31;
                total ^= keys[i].hashCode();
            }
        }

        this.hashCode = total;
        this.keys = keys;
    }

    /**
     * Constructor for a single key object.
     *
     * @param key is the single key object
     */
    public MultiKeyUntyped(Object key) {
        this(new Object[]{key});
    }

    /**
     * Constructor for a pair of key objects.
     *
     * @param key1 is the first key object
     * @param key2 is the second key object
     */
    public MultiKeyUntyped(Object key1, Object key2) {
        this(new Object[]{key1, key2});
    }

    /**
     * Constructor for three key objects.
     *
     * @param key1 is the first key object
     * @param key2 is the second key object
     * @param key3 is the third key object
     */
    public MultiKeyUntyped(Object key1, Object key2, Object key3) {
        this(new Object[]{key1, key2, key3});
    }

    /**
     * Constructor for four key objects.
     *
     * @param key1 is the first key object
     * @param key2 is the second key object
     * @param key3 is the third key object
     * @param key4 is the fourth key object
     */
    public MultiKeyUntyped(Object key1, Object key2, Object key3, Object key4) {
        this(new Object[]{key1, key2, key3, key4});
    }

    /**
     * Returns the number of key objects.
     *
     * @return size of key object array
     */
    public final int size() {
        return keys.length;
    }

    /**
     * Returns the key object at the specified position.
     *
     * @param index is the array position
     * @return key object at position
     */
    public final Object get(int index) {
        return keys[index];
    }

    public final boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof MultiKeyUntyped) {
            MultiKeyUntyped otherKeys = (MultiKeyUntyped) other;
            return Arrays.equals(keys, otherKeys.keys);
        }
        return false;
    }

    /**
     * Returns keys.
     *
     * @return keys object array
     */
    public Object[] getKeys() {
        return keys;
    }

    public final int hashCode() {
        return hashCode;
    }

    public final String toString() {
        return "MultiKeyUntyped" + Arrays.asList(keys).toString();
    }
}

