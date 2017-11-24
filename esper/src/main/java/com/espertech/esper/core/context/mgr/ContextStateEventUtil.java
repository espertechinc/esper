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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.context.ContextPartitionStateListener;
import com.espertech.esper.client.context.ContextStateListener;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ContextStateEventUtil {
    public static <T> void dispatchContext(CopyOnWriteArrayList<ContextStateListener> listeners, Supplier<T> supplier, BiConsumer<ContextStateListener, T> consumer) {
        if (listeners.isEmpty()) {
            return;
        }
        T event = supplier.get();
        for (ContextStateListener listener : listeners) {
            consumer.accept(listener, event);
        }
    }

    public static <T> void dispatchPartition(CopyOnWriteArrayList<ContextPartitionStateListener> listeners, Supplier<T> supplier, BiConsumer<ContextPartitionStateListener, T> consumer) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        T event = supplier.get();
        for (ContextPartitionStateListener listener : listeners) {
            consumer.accept(listener, event);
        }
    }
}
