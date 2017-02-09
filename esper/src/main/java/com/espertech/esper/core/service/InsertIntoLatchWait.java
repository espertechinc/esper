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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EventBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A suspend-and-notify implementation of a latch for use in guaranteeing delivery between
 * a single event produced by a single statement and consumable by another statement.
 */
public class InsertIntoLatchWait {
    private static final Logger log = LoggerFactory.getLogger(InsertIntoLatchWait.class);

    // The earlier latch is the latch generated before this latch
    private InsertIntoLatchWait earlier;
    private long msecTimeout;
    private EventBean payload;

    // The later latch is the latch generated after this latch
    private InsertIntoLatchWait later;
    private volatile boolean isCompleted;

    /**
     * Ctor.
     *
     * @param earlier     the latch before this latch that this latch should be waiting for
     * @param msecTimeout the timeout after which delivery occurs
     * @param payload     the payload is an event to deliver
     */
    public InsertIntoLatchWait(InsertIntoLatchWait earlier, long msecTimeout, EventBean payload) {
        this.earlier = earlier;
        this.msecTimeout = msecTimeout;
        this.payload = payload;
    }

    /**
     * Ctor - use for the first and unused latch to indicate completion.
     *
     * @param factory the latch factory
     */
    public InsertIntoLatchWait(InsertIntoLatchFactory factory) {
        isCompleted = true;
        earlier = null;
        msecTimeout = 0;
    }

    /**
     * Returns true if the dispatch completed for this future.
     *
     * @return true for completed, false if not
     */
    public boolean isCompleted() {
        return isCompleted;
    }

    /**
     * Hand a later latch to use for indicating completion via notify.
     *
     * @param later is the later latch
     */
    public void setLater(InsertIntoLatchWait later) {
        this.later = later;
    }

    /**
     * Blcking call that returns only when the earlier latch completed.
     *
     * @return payload of the latch
     */
    public EventBean await() {
        if (!earlier.isCompleted) {
            synchronized (this) {
                if (!earlier.isCompleted) {
                    try {
                        this.wait(msecTimeout);
                    } catch (InterruptedException e) {
                        log.error("Interrupted: " + e.getMessage(), e);
                    }
                }
            }
        }

        if (!earlier.isCompleted) {
            log.info("Wait timeout exceeded for insert-into dispatch with notify");
        }

        return payload;
    }

    /**
     * Called to indicate that the latch completed and a later latch can start.
     */
    public void done() {
        isCompleted = true;
        if (later != null) {
            synchronized (later) {
                later.notify();
            }
        }
        earlier = null;
        later = null;
    }
}
