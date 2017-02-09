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

public class RollingTwoValueBuffer<A, B> {
    private final A[] bufferA;
    private final B[] bufferB;
    private int nextFreeIndex;

    public RollingTwoValueBuffer(A[] bufferA, B[] bufferB) {
        if (bufferA.length != bufferB.length || bufferA.length == 0) {
            throw new IllegalArgumentException("Minimum buffer size is 1, buffer sizes must be identical");
        }
        this.bufferA = bufferA;
        this.bufferB = bufferB;
        nextFreeIndex = 0;
    }

    public void add(A valueA, B valueB) {
        bufferA[nextFreeIndex] = valueA;
        bufferB[nextFreeIndex] = valueB;
        nextFreeIndex++;

        if (nextFreeIndex == bufferA.length) {
            nextFreeIndex = 0;
        }
    }

    public A[] getBufferA() {
        return bufferA;
    }

    public B[] getBufferB() {
        return bufferB;
    }
}
