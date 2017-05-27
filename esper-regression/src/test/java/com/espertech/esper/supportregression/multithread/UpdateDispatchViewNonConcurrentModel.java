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

import com.espertech.esper.dispatch.DispatchService;
import com.espertech.esper.dispatch.Dispatchable;

import java.util.LinkedList;

public class UpdateDispatchViewNonConcurrentModel implements UpdateDispatchViewModel, Dispatchable {
    private DispatchService dispatchService;
    private DispatchListener dispatchListener;

    private ThreadLocal<Boolean> isDispatchWaiting = new ThreadLocal<Boolean>() {
        protected synchronized Boolean initialValue() {
            return new Boolean(false);
        }
    };
    private ThreadLocal<LinkedList<int[]>> received = new ThreadLocal<LinkedList<int[]>>() {
        protected synchronized LinkedList<int[]> initialValue() {
            return new LinkedList<int[]>();
        }
    };

    public UpdateDispatchViewNonConcurrentModel(DispatchService dispatchService, DispatchListener dispatchListener) {
        this.dispatchService = dispatchService;
        this.dispatchListener = dispatchListener;
    }

    public void add(int[] payload) {
        received.get().add(payload);
        if (!isDispatchWaiting.get()) {
            dispatchService.addExternal(this);
            isDispatchWaiting.set(true);
        }
    }

    public void execute() {
        // flatten
        LinkedList<int[]> payloads = received.get();
        int[][] result = new int[payloads.size()][];

        int count = 0;
        for (int[] entry : payloads) {
            result[count++] = entry;
        }

        isDispatchWaiting.set(false);
        payloads.clear();
        dispatchListener.dispatched(result);
    }
}
