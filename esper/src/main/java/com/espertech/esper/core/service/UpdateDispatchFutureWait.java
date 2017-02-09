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

import com.espertech.esper.dispatch.Dispatchable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UpdateDispatchFutureWait can be added to a dispatch queue that is thread-local. It represents
 * is a stand-in for a future dispatching of a statement result to statement listeners.
 * <p>
 * UpdateDispatchFutureWait is aware of future and past dispatches:
 * (newest) DF3   &lt;--&gt;   DF2  &lt;--&gt;  DF1  (oldest)
 */
public class UpdateDispatchFutureWait implements Dispatchable {
    private static final Logger log = LoggerFactory.getLogger(UpdateDispatchFutureWait.class);
    private UpdateDispatchViewBlockingWait view;
    private UpdateDispatchFutureWait earlier;
    private UpdateDispatchFutureWait later;
    private volatile boolean isCompleted;
    private long msecTimeout;

    /**
     * Ctor.
     *
     * @param view        is the blocking dispatch view through which to execute a dispatch
     * @param earlier     is the older future
     * @param msecTimeout is the timeout period to wait for listeners to complete a prior dispatch
     */
    public UpdateDispatchFutureWait(UpdateDispatchViewBlockingWait view, UpdateDispatchFutureWait earlier, long msecTimeout) {
        this.view = view;
        this.earlier = earlier;
        this.msecTimeout = msecTimeout;
    }

    /**
     * Ctor - use for the first future to indicate completion.
     */
    public UpdateDispatchFutureWait() {
        isCompleted = true;
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
     * Hand a later future to the dispatch to use for indicating completion via notify.
     *
     * @param later is the later dispatch
     */
    public void setLater(UpdateDispatchFutureWait later) {
        this.later = later;
    }

    public void execute() {
        if (!earlier.isCompleted) {
            synchronized (this) {
                if (!earlier.isCompleted) {
                    try {
                        this.wait(msecTimeout);
                    } catch (InterruptedException e) {
                        log.error("Interupted: " + e.getMessage());
                    }
                }
            }
        }

        view.execute();
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
