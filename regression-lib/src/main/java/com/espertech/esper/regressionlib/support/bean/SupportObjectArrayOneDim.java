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
public class SupportObjectArrayOneDim implements Serializable {
    private static final long serialVersionUID = 2486483767936078439L;
    private String id;
    private Object[] arr;

    public SupportObjectArrayOneDim() {
    }

    public SupportObjectArrayOneDim(String id, Object[] arr) {
        this.id = id;
        this.arr = arr;
    }

    public Object[] getArr() {
        return arr;
    }

    public void setArr(Object[] arr) {
        this.arr = arr;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SupportObjectArrayOneDim that = (SupportObjectArrayOneDim) o;

        if (!id.equals(that.id)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.deepEquals(arr, that.arr);
    }

    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + Arrays.deepHashCode(arr);
        return result;
    }

    public String toString() {
        return "SupportObjectArrayOneDim{" +
            "id='" + id + '\'' +
            ", arr=" + Arrays.toString(arr) +
            '}';
    }
}
