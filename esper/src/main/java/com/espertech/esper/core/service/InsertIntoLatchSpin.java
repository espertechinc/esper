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
 * A spin-locking implementation of a latch for use in guaranteeing delivery between
 * a single event produced by a single statement and consumable by another statement.
 */
public class InsertIntoLatchSpin {
    private static final Logger log = LoggerFactory.getLogger(InsertIntoLatchSpin.class);

    // The earlier latch is the latch generated before this latch
    private InsertIntoLatchFactory factory;
    private InsertIntoLatchSpin earlier;
    private long msecTimeout;
    private EventBean payload;

    private volatile boolean isCompleted;

    /**
     * Ctor.
     *
     * @param factory     the latch factory
     * @param earlier     the latch before this latch that this latch should be waiting for
     * @param msecTimeout the timeout after which delivery occurs
     * @param payload     the payload is an event to deliver
     */
    public InsertIntoLatchSpin(InsertIntoLatchFactory factory, InsertIntoLatchSpin earlier, long msecTimeout, EventBean payload) {
        this.factory = factory;
        this.earlier = earlier;
        this.msecTimeout = msecTimeout;
        this.payload = payload;
    }

    /**
     * Ctor - use for the first and unused latch to indicate completion.
     *
     * @param factory the latch factory
     */
    public InsertIntoLatchSpin(InsertIntoLatchFactory factory) {
        this.factory = factory;
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
     * Blocking call that returns only when the earlier latch completed.
     *
     * @return payload of the latch
     */
    public EventBean await() {
        if (!earlier.isCompleted) {
            long spinStartTime = factory.getTimeSourceService().getTimeMillis();

            while (!earlier.isCompleted) {
                Thread.yield();

                long spinDelta = factory.getTimeSourceService().getTimeMillis() - spinStartTime;
                if (spinDelta > msecTimeout) {
                    log.info("Spin wait timeout exceeded in insert-into dispatch at " + msecTimeout + "ms for " + factory.getName() + ", consider disabling insert-into between-statement latching for better performance");
                    break;
                }
            }
        }

        return payload;
    }

    /**
     * Called to indicate that the latch completed and a later latch can start.
     */
    public void done() {
        isCompleted = true;
        earlier = null;
    }
}
