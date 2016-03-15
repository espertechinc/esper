/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.named;

import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;

/**
 * A spin-locking implementation of a latch for use in guaranteeing delivery between
 * a delta stream produced by a named window and consumable by another statement.
 */
public class NamedWindowConsumerLatchSpin extends NamedWindowConsumerLatch
{
    private static final Log log = LogFactory.getLog(NamedWindowConsumerLatchSpin.class);

    // The earlier latch is the latch generated before this latch
    private NamedWindowConsumerLatchFactory factory;
    private NamedWindowConsumerLatchSpin earlier;
    private NamedWindowDeltaData deltaData;
    private Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> dispatchTo;
    private Thread currentThread;

    private volatile boolean isCompleted;

    /**
     * Ctor.
     * @param earlier the latch before this latch that this latch should be waiting for
     */
    public NamedWindowConsumerLatchSpin(NamedWindowConsumerLatchFactory factory, NamedWindowConsumerLatchSpin earlier,
                                        NamedWindowDeltaData deltaData, Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> dispatchTo)
    {
        this.factory = factory;
        this.earlier = earlier;
        this.deltaData = deltaData;
        this.dispatchTo = dispatchTo;
    }

    /**
     * Ctor - use for the first and unused latch to indicate completion.
     */
    public NamedWindowConsumerLatchSpin(NamedWindowConsumerLatchFactory factory)
    {
        this.factory = factory;
        isCompleted = true;
        earlier = null;
    }

    /**
     * Returns true if the dispatch completed for this future.
     * @return true for completed, false if not
     */
    public boolean isCompleted()
    {
        return isCompleted;
    }

    /**
     * Blocking call that returns only when the earlier latch completed.
     * @return unit of the latch
     */
    public void await()
    {
        if (earlier.isCompleted) {
            currentThread = Thread.currentThread();
            return;
        }

        if (earlier.getCurrentThread() == Thread.currentThread()) {
            currentThread = Thread.currentThread();
            return;
        }

        long spinStartTime = factory.getTimeSourceService().getTimeMillis();
        while(!earlier.isCompleted) {
            Thread.yield();
            long spinDelta = factory.getTimeSourceService().getTimeMillis() - spinStartTime;
            if (spinDelta > factory.getMsecWait()) {
                log.info("Spin wait timeout exceeded in named window '" + factory.getName() + "' consumer dispatch at " + factory.getMsecWait() + "ms for " + factory.getName() + ", consider disabling named window consumer dispatch latching for better performance");
                break;
            }
        }
    }

    public NamedWindowDeltaData getDeltaData() {
        return deltaData;
    }

    public Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> getDispatchTo() {
        return dispatchTo;
    }

    public Thread getCurrentThread() {
        return currentThread;
    }

    /**
     * Called to indicate that the latch completed and a later latch can start.
     */
    public void done()
    {
        isCompleted = true;
        earlier = null;
    }
}
