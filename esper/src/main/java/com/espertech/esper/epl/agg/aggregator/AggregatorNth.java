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
package com.espertech.esper.epl.agg.aggregator;

/**
 * Aggregator to return the Nth oldest element to enter, with N=1 the most recent
 * value is returned. If N is larger than the enter minus leave size, null is returned.
 * A maximum N historical values are stored, so it can be safely used to compare
 * recent values in large views without incurring excessive overhead.
 */
public class AggregatorNth implements AggregationMethod {

    protected final int sizeBuf;

    protected Object[] circularBuffer;
    protected int currentBufferElementPointer;
    protected long numDataPoints;

    /**
     * Ctor.
     *
     * @param sizeBuf size
     */
    public AggregatorNth(int sizeBuf) {
        this.sizeBuf = sizeBuf;
    }

    public void enter(Object value) {
        Object[] arr = (Object[]) value;
        enterValues(arr);
    }

    public void leave(Object value) {
        if (sizeBuf > numDataPoints) {
            final int diff = sizeBuf - (int) numDataPoints;
            circularBuffer[(currentBufferElementPointer + diff - 1) % sizeBuf] = null;
        }
        numDataPoints--;
    }

    public Object getValue() {
        if (circularBuffer == null) {
            return null;
        }
        return circularBuffer[(currentBufferElementPointer + sizeBuf) % sizeBuf];
    }

    public void clear() {
        circularBuffer = new Object[sizeBuf];
        numDataPoints = 0;
        currentBufferElementPointer = 0;
    }

    protected void enterValues(Object[] arr) {
        numDataPoints++;
        if (circularBuffer == null) {
            clear();
        }
        circularBuffer[currentBufferElementPointer] = arr[0];
        currentBufferElementPointer = (currentBufferElementPointer + 1) % sizeBuf;
    }

    public int getSizeBuf() {
        return sizeBuf;
    }

    public Object[] getCircularBuffer() {
        return circularBuffer;
    }

    public void setCircularBuffer(Object[] circularBuffer) {
        this.circularBuffer = circularBuffer;
    }

    public int getCurrentBufferElementPointer() {
        return currentBufferElementPointer;
    }

    public void setCurrentBufferElementPointer(int currentBufferElementPointer) {
        this.currentBufferElementPointer = currentBufferElementPointer;
    }

    public long getNumDataPoints() {
        return numDataPoints;
    }

    public void setNumDataPoints(long numDataPoints) {
        this.numDataPoints = numDataPoints;
    }
}