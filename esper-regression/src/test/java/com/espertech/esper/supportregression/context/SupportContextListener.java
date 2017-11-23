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
package com.espertech.esper.supportregression.context;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.context.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SupportContextListener implements ContextStateListener, ContextPartitionStateListener {
    private final EPServiceProvider engine;
    private List<ContextStateEvent> events = new ArrayList<>();

    public SupportContextListener(EPServiceProvider engine) {
        this.engine = engine;
    }

    public void onContextCreated(ContextStateEventContextCreated event) {
        events.add(event);
        engine.getEPAdministrator().getContextPartitionAdmin().addContextPartitionStateListener(event.getContextName(), this);
    }

    public void onContextDestroyed(ContextStateEventContextDestroyed event) {
        events.add(event);
    }

    public void onContextPartitionAllocated(ContextStateEventContextPartitionAllocated event) {
        events.add(event);
        assertNotNull(engine.getEPAdministrator().getContextPartitionAdmin().getContextProperties(event.getContextName(), event.getId()));
    }

    public void onContextPartitionDeallocated(ContextStateEventContextPartitionDeallocated event) {
        events.add(event);
    }

    public void onContextActivated(ContextStateEventContextActivated event) {
        events.add(event);
    }

    public void onContextDeactivated(ContextStateEventContextDeactivated event) {
        events.add(event);
    }

    public void onContextStatementAdded(ContextStateEventContextStatementAdded event) {
        events.add(event);
    }

    public void onContextStatementRemoved(ContextStateEventContextStatementRemoved event) {
        events.add(event);
    }

    public List<ContextStateEvent> getAndReset() {
        List<ContextStateEvent> current = events;
        events = new ArrayList<>();
        return current;
    }

    public void assertAndReset(Consumer<ContextStateEvent>... consumers) {
        List<ContextStateEvent> events = getAndReset();
        assertEquals(consumers.length, events.size());
        int count = 0;
        for (Consumer<ContextStateEvent> consumer : consumers) {
            consumer.accept(events.get(count++));
        }
    }

    public List<ContextStateEventContextPartitionAllocated> getAllocatedEvents() {
        List<ContextStateEventContextPartitionAllocated> allocateds = new ArrayList<>();
        for (ContextStateEvent event : events) {
            if (event instanceof ContextStateEventContextPartitionAllocated) {
                allocateds.add((ContextStateEventContextPartitionAllocated) event);
            }
        }
        return allocateds;
    }

    public void assertNotInvoked() {
        assertTrue(events.isEmpty());
    }
}
