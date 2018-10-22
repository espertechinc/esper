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
package com.espertech.esper.common.internal.type;

import java.util.Arrays;

public class IntArrayKey {
    private final int[] keys;
    private final int hashCode;

    public IntArrayKey(int[] keys) {
        if (keys == null) {
            throw new IllegalArgumentException("The array of keys must not be null");
        }
        this.keys = keys;
        this.hashCode = Arrays.hashCode(keys);
    }

    public final int size() {
        return keys.length;
    }

    public final boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof IntArrayKey) {
            IntArrayKey otherKeys = (IntArrayKey) other;
            return Arrays.equals(keys, otherKeys.keys);
        }
        return false;
    }

    public final int hashCode() {
        return hashCode;
    }

    public final String toString() {
        return "IntArrayKey" + Arrays.asList(keys).toString();
    }

    public int[] getKeys() {
        return keys;
    }
}
