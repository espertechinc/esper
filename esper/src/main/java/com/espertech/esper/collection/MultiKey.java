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

import java.util.Arrays;

/**
 * Functions as a key value for Maps where keys need to be composite values.
 * The class allows a Map that uses MultiKeyUntyped entries for key values to use multiple objects as keys.
 * It calculates the hashCode from the key objects on construction and caches the hashCode.
 */
public final class MultiKey<T> {
    private final T[] keys;
    private final int hashCode;

    /**
     * Constructor for multiple keys supplied in an object array.
     *
     * @param keys is an array of key objects
     */
    public MultiKey(T[] keys) {
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
    public final T get(int index) {
        return keys[index];
    }

    public final boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof MultiKey) {
            MultiKey otherKeys = (MultiKey) other;
            return Arrays.equals(keys, otherKeys.keys);
        }
        return false;
    }

    public final int hashCode() {
        return hashCode;
    }

    public final String toString() {
        return "MultiKey" + Arrays.asList(keys).toString();
    }

    /**
     * Returns the key value array.
     *
     * @return key value array
     */
    public final T[] getArray() {
        return keys;
    }
}

