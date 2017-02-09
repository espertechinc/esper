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
package com.espertech.esper.dataflow.util;

import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.interfaces.EPDataFlowSignalHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@DataFlowOperator
public class DefaultSupportCaptureOp<T> implements EPDataFlowSignalHandler, Future<Object[]> {
    private List<List<T>> received = new ArrayList<List<T>>();
    private List<T> current = new ArrayList<T>();

    private CountDownLatch numRowLatch;

    public DefaultSupportCaptureOp() {
        this(0);
    }

    public DefaultSupportCaptureOp(int latchedNumRows) {
        this.numRowLatch = new CountDownLatch(latchedNumRows);
    }

    public synchronized void onInput(T event) {
        current.add(event);
        if (numRowLatch != null) {
            numRowLatch.countDown();
        }
    }

    public void onSignal(EPDataFlowSignal signal) {
        received.add(current);
        current = new ArrayList<T>();
    }

    public synchronized List<List<T>> getAndReset() {
        List<List<T>> resultEvents = received;
        received = new ArrayList<List<T>>();
        current.clear();
        return resultEvents;
    }

    public synchronized Object[] getCurrent() {
        return current.toArray();
    }

    public synchronized Object[] getCurrentAndReset() {
        Object[] currentArray = current.toArray();
        current.clear();
        return currentArray;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    public boolean isDone() {
        return numRowLatch.getCount() <= 0;
    }

    public Object[] get() throws InterruptedException, ExecutionException {
        try {
            return get(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (TimeoutException e) {
        }
        return null;
    }

    public Object[] get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean result = numRowLatch.await(timeout, unit);
        if (!result) {
            throw new TimeoutException("latch timed out");
        }
        return getCurrent();
    }

    public Object[] getPunctuated() throws InterruptedException, ExecutionException, TimeoutException {
        boolean result = numRowLatch.await(1, TimeUnit.SECONDS);
        if (!result) {
            throw new TimeoutException("latch timed out");
        }
        return received.get(0).toArray();
    }

    /**
     * Wait for the listener invocation for up to the given number of milliseconds.
     *
     * @param msecWait          to wait
     * @param numberOfNewEvents in any number of separate invocations required before returning
     * @throws RuntimeException when no results or insufficient number of events were received
     */
    public void waitForInvocation(long msecWait, int numberOfNewEvents) {
        long startTime = System.currentTimeMillis();
        while (true) {

            synchronized (this) {
                if ((System.currentTimeMillis() - startTime) > msecWait) {
                    throw new RuntimeException("No events or less then the number of expected events received, expected " + numberOfNewEvents + " received " + current.size());
                }

                if (current.size() >= numberOfNewEvents) {
                    return;
                }
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}

