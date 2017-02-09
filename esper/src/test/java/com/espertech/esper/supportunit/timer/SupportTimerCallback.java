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
package com.espertech.esper.supportunit.timer;

import com.espertech.esper.timer.TimerCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class SupportTimerCallback implements TimerCallback {
    private AtomicInteger numInvoked = new AtomicInteger();

    public void timerCallback() {
        int current = numInvoked.incrementAndGet();
        log.debug(".timerCallback numInvoked=" + current + " thread=" + Thread.currentThread());
    }

    public int getCount() {
        return numInvoked.get();
    }

    public int getAndResetCount() {
        int count = numInvoked.getAndSet(0);
        return count;
    }

    private static final Logger log = LoggerFactory.getLogger(SupportTimerCallback.class);
}
