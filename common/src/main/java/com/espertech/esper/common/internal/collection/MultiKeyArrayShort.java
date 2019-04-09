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
package com.espertech.esper.common.internal.collection;

import java.util.Arrays;

public final class MultiKeyArrayShort implements MultiKeyArrayWrap {
    private final short[] keys;

    public MultiKeyArrayShort(short[] keys) {
        this.keys = keys;
    }

    public short[] getKeys() {
        return keys;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MultiKeyArrayShort that = (MultiKeyArrayShort) o;

        if (!Arrays.equals(keys, that.keys)) return false;

        return true;
    }

    public int hashCode() {
        return Arrays.hashCode(keys);
    }

    public String toString() {
        return "MultiKeyShort{" +
            "keys=" + Arrays.toString(keys) +
            '}';
    }

    public Object getArray() {
        return keys;
    }
}

