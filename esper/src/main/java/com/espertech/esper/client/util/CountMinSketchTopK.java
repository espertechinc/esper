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
package com.espertech.esper.client.util;

/**
 * Value object for count-min-sketch top-k.
 */
public class CountMinSketchTopK {
    private final long frequency;
    private final Object value;

    /**
     * Ctor.
     *
     * @param frequency the value frequency
     * @param value     the value object
     */
    public CountMinSketchTopK(long frequency, Object value) {
        this.frequency = frequency;
        this.value = value;
    }

    /**
     * Returns the frequency
     *
     * @return frequency
     */
    public long getFrequency() {
        return frequency;
    }

    /**
     * Returns the value object
     *
     * @return value
     */
    public Object getValue() {
        return value;
    }

    public String toString() {
        return "CountMinSketchFrequency{" +
                "frequency=" + frequency +
                ", value=" + value +
                '}';
    }
}
