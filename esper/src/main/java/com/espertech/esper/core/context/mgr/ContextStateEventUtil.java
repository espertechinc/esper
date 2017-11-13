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
import org.codehaus.janino.util.Producer;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public class ContextStateEventUtil {
    public static <T> void dispatchContext(CopyOnWriteArrayList<ContextStateListener> listeners, Producer<T> producer, BiConsumer<ContextStateListener, T> consumer) {
        if (listeners.isEmpty()) {
            return;
        }
        T event = producer.produce();
        for (ContextStateListener listener : listeners) {
            consumer.accept(listener, event);
        }
    }

    public static <T> void dispatchPartition(CopyOnWriteArrayList<ContextPartitionStateListener> listeners, Producer<T> producer, BiConsumer<ContextPartitionStateListener, T> consumer) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        T event = producer.produce();
        for (ContextPartitionStateListener listener : listeners) {
            consumer.accept(listener, event);
        }
    }
}
