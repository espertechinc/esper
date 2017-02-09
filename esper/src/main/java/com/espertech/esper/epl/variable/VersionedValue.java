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
package com.espertech.esper.epl.variable;

import java.util.Date;

/**
 * Holds a version of a value and a timestamp when that version is taken.
 */
public class VersionedValue<T> {
    private int version;
    private T value;
    private long timestamp;

    /**
     * Ctor.
     *
     * @param version   version number
     * @param value     value at that version
     * @param timestamp time when version was taken
     */
    public VersionedValue(int version, T value, long timestamp) {
        this.version = version;
        this.value = value;
        this.timestamp = timestamp;
    }

    /**
     * Returns the version.
     *
     * @return version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Returns the value.
     *
     * @return value
     */
    public T getValue() {
        return value;
    }

    /**
     * Returns the time the version was taken.
     *
     * @return time of version
     */
    public long getTimestamp() {
        return timestamp;
    }

    public String toString() {
        return value + "@" + version + "@" + (new Date(timestamp));
    }
}
