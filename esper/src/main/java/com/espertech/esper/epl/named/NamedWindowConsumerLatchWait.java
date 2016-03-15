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
 * A suspend-and-notify implementation of a latch for use in guaranteeing delivery between
 * a named window delta result and consumable by another statement.
 */
public class NamedWindowConsumerLatchWait extends NamedWindowConsumerLatch
{
    private static final Log log = LogFactory.getLog(NamedWindowConsumerLatchWait.class);

    // The earlier latch is the latch generated before this latch
    private final NamedWindowConsumerLatchFactory factory;
    private NamedWindowConsumerLatchWait earlier;

    // The later latch is the latch generated after this latch
    private NamedWindowConsumerLatchWait later;
    private volatile boolean isCompleted;
    private Thread currentThread;

    /**
     * Ctor.
     * @param earlier the latch before this latch that this latch should be waiting for
     */
    public NamedWindowConsumerLatchWait(NamedWindowDeltaData deltaData, Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> dispatchTo, NamedWindowConsumerLatchFactory factory, NamedWindowConsumerLatchWait earlier)
    {
        super(deltaData, dispatchTo);
        this.factory = factory;
        this.earlier = earlier;
    }

    /**
     * Ctor - use for the first and unused latch to indicate completion.
     */
    public NamedWindowConsumerLatchWait(NamedWindowConsumerLatchFactory factory)
    {
        super(null, null);
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
     * Hand a later latch to use for indicating completion via notify.
     * @param later is the later latch
     */
    public void setLater(NamedWindowConsumerLatchWait later)
    {
        this.later = later;
    }

    /**
     * Blcking call that returns only when the earlier latch completed.
     * @return payload of the latch
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

        synchronized(this) {
            if (!earlier.isCompleted) {
                try {
                    this.wait(factory.getMsecWait());
                }
                catch (InterruptedException e) {
                    log.error(e);
                }
            }
        }

        if (!earlier.isCompleted) {
            log.info("Wait timeout exceeded for named window '" + "' consumer dispatch with notify");
        }
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
        if (later != null)
        {
            synchronized(later)
            {
                later.notify();
            }
        }
        earlier = null;
        later = null;
    }
}
