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
package com.espertech.esper.regressionlib.support.bean;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public final class SupportEventWithLongArray implements Serializable {
    private static final long serialVersionUID = -1159235961857399350L;
    private final String id;
    private final long[] coll;

    public SupportEventWithLongArray(String id, long[] coll) {
        this.id = id;
        this.coll = coll;
    }

    public String getId() {
        return id;
    }

    public long[] getColl() {
        return coll;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SupportEventWithLongArray that = (SupportEventWithLongArray) o;

        if (!id.equals(that.id)) return false;
        return Arrays.equals(coll, that.coll);
    }

    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + Arrays.hashCode(coll);
        return result;
    }
}
