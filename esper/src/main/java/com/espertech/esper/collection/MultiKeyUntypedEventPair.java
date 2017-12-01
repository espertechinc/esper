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

import com.espertech.esper.client.EventBean;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Functions as a key value for Maps where keys need to be composite values, and includes an {@link com.espertech.esper.client.EventBean} handle
 * The class allows a Map that uses MultiKeyUntyped entries for key values to use multiple objects as keys.
 * It calculates the hashCode from the key objects on construction and caches the hashCode.
 */
public final class MultiKeyUntypedEventPair implements Serializable {
    private transient final Object[] keys;
    private transient EventBean eventBean = null;
    private final int hashCode;
    private static final long serialVersionUID = -3890626073105861216L;

    /**
     * Constructor for multiple keys supplied in an object array.
     *
     * @param keys      is an array of key objects
     * @param eventBean for pair
     */
    public MultiKeyUntypedEventPair(Object[] keys, EventBean eventBean) {
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
        this.eventBean = eventBean;
    }

    /**
     * Returns the event.
     *
     * @return event
     */
    public EventBean getEventBean() {
        return eventBean;
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
        if (other instanceof MultiKeyUntypedEventPair) {
            MultiKeyUntypedEventPair otherKeys = (MultiKeyUntypedEventPair) other;
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