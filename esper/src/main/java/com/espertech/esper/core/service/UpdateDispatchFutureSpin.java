/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.core.service;

import com.espertech.esper.dispatch.Dispatchable;
import com.espertech.esper.timer.TimeSourceService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * UpdateDispatchFutureSpin can be added to a dispatch queue that is thread-local. It represents
 * is a stand-in for a future dispatching of a statement result to statement listeners.
 * <p>
 * UpdateDispatchFutureSpin is aware of future and past dispatches:
 * (newest) DF3   <-->   DF2  <-->  DF1  (oldest), and uses a spin lock to block if required
 */
public class UpdateDispatchFutureSpin implements Dispatchable
{
    private static final Log log = LogFactory.getLog(UpdateDispatchFutureSpin.class);
    private UpdateDispatchViewBlockingSpin view;
    private UpdateDispatchFutureSpin earlier;
    private volatile boolean isCompleted;
    private long msecTimeout;
    private TimeSourceService timeSourceService;

    /**
     * Ctor.
     * @param view is the blocking dispatch view through which to execute a dispatch
     * @param earlier is the older future
     * @param msecTimeout is the timeout period to wait for listeners to complete a prior dispatch
     * @param timeSourceService time source provider
     */
    public UpdateDispatchFutureSpin(UpdateDispatchViewBlockingSpin view, UpdateDispatchFutureSpin earlier, long msecTimeout, TimeSourceService timeSourceService)
    {
        this.view = view;
        this.earlier = earlier;
        this.msecTimeout = msecTimeout;
        this.timeSourceService = timeSourceService;
    }

    /**
     * Ctor - use for the first future to indicate completion.
     * @param timeSourceService time source provider
     */
    public UpdateDispatchFutureSpin(TimeSourceService timeSourceService)
    {
        isCompleted = true;
        this.timeSourceService = timeSourceService;
    }

    /**
     * Returns true if the dispatch completed for this future.
     * @return true for completed, false if not
     */
    public boolean isCompleted()
    {
        return isCompleted;
    }

    public void execute()
    {
        if (!earlier.isCompleted)
        {
            long spinStartTime = timeSourceService.getTimeMillis();

            while(!earlier.isCompleted)
            {
                Thread.yield();

                long spinDelta = timeSourceService.getTimeMillis() - spinStartTime;
                if (spinDelta > msecTimeout)
                {
                    log.info("Spin wait timeout exceeded in listener dispatch for statement '" + view.getStatementResultService().getStatementName() + "'");
                    break;
                }
            }
        }

        view.execute();
        isCompleted = true;

        earlier = null;
    }
}
