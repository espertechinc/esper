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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.ByteArrayProvidingClassLoader;
import com.espertech.esper.common.internal.context.util.ParentClassLoader;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class CompilableItemPostCompileLatchJson implements CompilableItemPostCompileLatch {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final Collection<EventType> eventTypes;
    private final ClassLoader parentClassLoader;
    private Map<String, byte[]> moduleBytes;

    public CompilableItemPostCompileLatchJson(Collection<EventType> eventTypes, ClassLoader parentClassLoader) {
        this.eventTypes = eventTypes;
        this.parentClassLoader = parentClassLoader;
    }

    public void awaitAndRun() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // load underlying class of Json types
        for (EventType eventType : eventTypes) {
            if (!(eventType instanceof JsonEventType)) {
                continue;
            }
            JsonEventType jsonEventType = (JsonEventType) eventType;
            ByteArrayProvidingClassLoader classLoader = new ByteArrayProvidingClassLoader(moduleBytes, parentClassLoader);
            jsonEventType.initialize(classLoader);
        }
    }

    public void completed(Map<String, byte[]> moduleBytes) {
        this.moduleBytes = moduleBytes;
        latch.countDown();
    }
}
