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
package com.espertech.esper.common.internal.statement.dispatch;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Implements dispatch service using a thread-local linked list of Dispatchable instances.
 */
public class DispatchService {
    private final ThreadLocal<ArrayDeque<Dispatchable>> dispatchStateThreadLocal = new ThreadLocal<ArrayDeque<Dispatchable>>() {
        protected synchronized ArrayDeque<Dispatchable> initialValue() {
            return new ArrayDeque<>();
        }
    };

    public ThreadLocal<ArrayDeque<Dispatchable>> getDispatchStateThreadLocal() {
        return dispatchStateThreadLocal;
    }

    public void dispatch() {
        dispatchFromQueue(dispatchStateThreadLocal.get());
    }

    public void addExternal(Dispatchable dispatchable) {
        ArrayDeque<Dispatchable> dispatchQueue = dispatchStateThreadLocal.get();
        addToQueue(dispatchable, dispatchQueue);
    }

    private static void addToQueue(Dispatchable dispatchable, ArrayDeque<Dispatchable> dispatchQueue) {
        dispatchQueue.add(dispatchable);
    }

    private static void dispatchFromQueue(ArrayDeque<Dispatchable> dispatchQueue) {
        while (true) {
            Dispatchable next = dispatchQueue.poll();
            if (next != null) {
                next.execute();
            } else {
                break;
            }
        }
    }

    public void removeAll(UpdateDispatchView updateDispatchView) {
        ArrayDeque<Dispatchable> dispatchables = dispatchStateThreadLocal.get();
        Iterator<Dispatchable> it = dispatchables.descendingIterator();
        while (it.hasNext()) {
            Dispatchable dispatchable = it.next();
            if (dispatchable.getView() == updateDispatchView) {
                it.remove();
                dispatchable.cancelled();
            }
        }
    }
}
