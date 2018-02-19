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
 * A spin-locking implementation of a latch for use in guaranteeing delivery between
 * a delta stream produced by a named window and consumable by another statement.
 */
public class NamedWindowConsumerLatchSpin extends NamedWindowConsumerLatch {
    private static final Logger log = LoggerFactory.getLogger(NamedWindowConsumerLatchSpin.class);

    // The earlier latch is the latch generated before this latch
    private NamedWindowConsumerLatchFactory factory;
    private NamedWindowConsumerLatchSpin earlier;

    private volatile boolean isCompleted;

    public NamedWindowConsumerLatchSpin(NamedWindowDeltaData deltaData, Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> dispatchTo, NamedWindowConsumerLatchFactory factory, NamedWindowConsumerLatchSpin earlier) {
        super(deltaData, dispatchTo);
        this.factory = factory;
        this.earlier = earlier;
    }

    public NamedWindowConsumerLatchSpin(NamedWindowConsumerLatchFactory factory) {
        super(null, null);
        this.factory = factory;
        isCompleted = true;
        earlier = null;
    }

    public NamedWindowConsumerLatchSpin getEarlier() {
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
     * Blocking call that returns only when the earlier latch completed.
     */
    public void await() {
        if (earlier.isCompleted) {
            return;
        }

        long spinStartTime = factory.getTimeSourceService().getTimeMillis();
        while (!earlier.isCompleted) {
            Thread.yield();
            long spinDelta = factory.getTimeSourceService().getTimeMillis() - spinStartTime;
            if (spinDelta > factory.getMsecWait()) {
                log.info("Spin wait timeout exceeded in named window '" + factory.getName() + "' consumer dispatch at " + factory.getMsecWait() + "ms for " + factory.getName() + ", consider disabling named window consumer dispatch latching for better performance");
                break;
            }
        }
    }

    /**
     * Called to indicate that the latch completed and a later latch can start.
     */
    public void done() {
        isCompleted = true;
        earlier = null;
    }
}
