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

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.timer.TimeSourceService;

import java.util.List;
import java.util.Map;

/**
 * Class to hold a current latch per named window.
 */
public class NamedWindowConsumerLatchFactory {
    protected final String name;
    protected final boolean useSpin;
    protected final TimeSourceService timeSourceService;
    protected final long msecWait;
    protected final boolean enabled;

    private NamedWindowConsumerLatchSpin currentLatchSpin;
    private NamedWindowConsumerLatchWait currentLatchWait;

    /**
     * Ctor.
     *
     * @param name              the factory name
     * @param msecWait          the number of milliseconds latches will await maximually
     * @param locking           the blocking strategy to employ
     * @param timeSourceService time source provider
     * @param initializeNow     for initializing
     * @param enabled           for active indicator
     */
    public NamedWindowConsumerLatchFactory(String name, boolean enabled, long msecWait, ConfigurationEngineDefaults.Threading.Locking locking,
                                           TimeSourceService timeSourceService, boolean initializeNow) {
        this.name = name;
        this.enabled = enabled;
        this.msecWait = msecWait;
        this.timeSourceService = timeSourceService;

        useSpin = enabled && (locking == ConfigurationEngineDefaults.Threading.Locking.SPIN);

        // construct a completed latch as an initial root latch
        if (initializeNow && useSpin) {
            currentLatchSpin = new NamedWindowConsumerLatchSpin(this);
        } else if (initializeNow && enabled) {
            currentLatchWait = new NamedWindowConsumerLatchWait(this);
        }
    }

    /**
     * Returns a new latch.
     * <p>
     * Need not be synchronized as there is one per statement and execution is during statement lock.
     *
     * @param delta     the delta
     * @param consumers consumers
     * @return latch
     */
    public NamedWindowConsumerLatch newLatch(NamedWindowDeltaData delta, Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> consumers) {
        if (useSpin) {
            NamedWindowConsumerLatchSpin nextLatch = new NamedWindowConsumerLatchSpin(delta, consumers, this, currentLatchSpin);
            currentLatchSpin = nextLatch;
            return nextLatch;
        } else {
            if (enabled) {
                NamedWindowConsumerLatchWait nextLatch = new NamedWindowConsumerLatchWait(delta, consumers, this, currentLatchWait);
                currentLatchWait.setLater(nextLatch);
                currentLatchWait = nextLatch;
                return nextLatch;
            }
            return new NamedWindowConsumerLatchNone(delta, consumers);
        }
    }

    public TimeSourceService getTimeSourceService() {
        return timeSourceService;
    }

    public String getName() {
        return name;
    }

    public long getMsecWait() {
        return msecWait;
    }
}
