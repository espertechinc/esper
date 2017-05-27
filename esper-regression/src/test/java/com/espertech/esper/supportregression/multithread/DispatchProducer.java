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
package com.espertech.esper.supportregression.multithread;

import java.util.LinkedHashMap;

public class DispatchProducer {
    private final UpdateDispatchViewModel dispatchProcessor;
    private int currentCount;
    private LinkedHashMap<Integer, int[]> payloads = new LinkedHashMap<Integer, int[]>();

    public DispatchProducer(UpdateDispatchViewModel dispatchProcessor) {
        this.dispatchProcessor = dispatchProcessor;
    }

    public synchronized int next() {
        currentCount++;

        int[] payload = new int[]{currentCount, 0};
        payloads.put(currentCount, payload);

        dispatchProcessor.add(payload);

        return currentCount;
    }

    public LinkedHashMap<Integer, int[]> getPayloads() {
        return payloads;
    }
}
