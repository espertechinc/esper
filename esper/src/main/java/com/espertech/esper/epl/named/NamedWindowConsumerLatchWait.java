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
package com.espertech.esper.epl.named;

import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * A suspend-and-notify implementation of a latch for use in guaranteeing delivery between
 * a named window delta result and consumable by another statement.
 */
public class NamedWindowConsumerLatchWait extends NamedWindowConsumerLatch {
    private static final Logger log = LoggerFactory.getLogger(NamedWindowConsumerLatchWait.class);

    // The earlier latch is the latch generated before this latch
    private final NamedWindowConsumerLatchFactory factory;
    private NamedWindowConsumerLatchWait earlier;

    // The later latch is the latch generated after this latch
    private NamedWindowConsumerLatchWait later;
    private volatile boolean isCompleted;

    public NamedWindowConsumerLatchWait(NamedWindowDeltaData deltaData, Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> dispatchTo, NamedWindowConsumerLatchFactory factory, NamedWindowConsumerLatchWait earlier) {
        super(deltaData, dispatchTo);
        this.factory = factory;
        this.earlier = earlier;
    }

    public NamedWindowConsumerLatchWait(NamedWindowConsumerLatchFactory factory) {
        super(null, null);
        this.factory = factory;
        isCompleted = true;
        earlier = null;
    }

    public NamedWindowConsumerLatchWait getEarlier() {
        return earlier;
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
    public void setLater(NamedWindowConsumerLatchWait later) {
        this.later = later;
    }

    /**
     * Blcking call that returns only when the earlier latch completed.
     */
    public void await() {

        if (earlier.isCompleted) {
            return;
        }

        synchronized (this) {
            if (!earlier.isCompleted) {
                try {
                    this.wait(factory.getMsecWait());
                } catch (InterruptedException e) {
                    log.error("Interrupted: " + e.getMessage(), e);
                }
            }
        }

        if (!earlier.isCompleted) {
            log.info("Wait timeout exceeded for named window '" + "' consumer dispatch with notify");
        }
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
