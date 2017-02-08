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
package com.espertech.esper.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Timer task to simply invoke the callback when triggered.
 */
final class EPLTimerTask implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger(EPLTimerTask.class);
    private final TimerCallback callback;
    private ScheduledFuture<?> future;
    private boolean isCancelled;

    protected boolean _enableStats;
    protected long _lastDrift;
    protected long _maxDrift;
    protected long _totalDrift;
    protected long _invocationCount;

    public EPLTimerTask(TimerCallback callback)
    {
        this.callback = callback;
        _enableStats = false;
        _lastDrift = 0;
        _maxDrift = 0;
        _totalDrift = 0;
        _invocationCount = 0;
    }

    public final void run()
    {
        if (!isCancelled)
        {
            if (_enableStats) {
                // If we are called early, then delay will be positive. If we are called late, then the delay will be negative.
                // NOTE: don't allow _enableStats to be set until future has been set
                if (future != null)
                {
                    _lastDrift = Math.abs(future.getDelay(TimeUnit.MILLISECONDS));
                }
                _totalDrift += _lastDrift;
                _invocationCount++;
                if (_lastDrift > _maxDrift) _maxDrift = _lastDrift;
            }

            try
            {
                callback.timerCallback();
            }
            catch (Throwable t)
            {
                log.error("Timer thread caught unhandled exception: " + t.getMessage(), t);
            }
        }
    }

    protected void resetStats()
    {
        _invocationCount = 0;
        _lastDrift = 0;
        _totalDrift = 0;
        _maxDrift = 0;
    }

    public void setCancelled(boolean cancelled)
    {
        isCancelled = cancelled;
    }

	public void setFuture(ScheduledFuture<?> future) {
		this.future = future;
	}    
}
