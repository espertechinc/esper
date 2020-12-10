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
package com.espertech.esper.common.internal.epl.agg.method.nth;

import com.espertech.esper.common.client.type.EPTypeClass;

public class AggregationNthState {
    public final static EPTypeClass EPTYPE = new EPTypeClass(AggregationNthState.class);

    private Object[] circularBuffer;
    private int currentBufferElementPointer;
    private long numDataPoints;

    public Object[] getCircularBuffer() {
        return circularBuffer;
    }

    public int getCurrentBufferElementPointer() {
        return currentBufferElementPointer;
    }

    public long getNumDataPoints() {
        return numDataPoints;
    }

    public void setCircularBuffer(Object[] circularBuffer) {
        this.circularBuffer = circularBuffer;
    }

    public void setCurrentBufferElementPointer(int currentBufferElementPointer) {
        this.currentBufferElementPointer = currentBufferElementPointer;
    }

    public void setNumDataPoints(long numDataPoints) {
        this.numDataPoints = numDataPoints;
    }
}
