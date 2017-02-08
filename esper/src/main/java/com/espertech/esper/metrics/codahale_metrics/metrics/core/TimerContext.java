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
/***************************************************************************************
 * Attribution Notice
 *
 * This file is imported from Metrics (https://github.com/codahale/metrics subproject metrics-core).
 * Metrics is Copyright (c) 2010-2012 Coda Hale, Yammer.com
 * Metrics is Published under Apache Software License 2.0, see LICENSE in root folder.
 *
 * Thank you for the Metrics developers efforts in making their library available under an Apache license.
 * EsperTech incorporates Metrics version 0.2.2 in source code form since Metrics depends on SLF4J
 * and this dependency is not possible to introduce for Esper.
 * *************************************************************************************
 */
package com.espertech.esper.metrics.codahale_metrics.metrics.core;

import java.util.concurrent.TimeUnit;

/**
 * A timing context.
 *
 * @see Timer#time()
 */
public class TimerContext {
    private final Timer timer;
    private final Clock clock;
    private final long startTime;

    /**
     * Creates a new {@link TimerContext} with the current time as its starting value and with the
     * given {@link Timer}.
     *
     * @param timer the {@link Timer} to report the elapsed time to
     */
    TimerContext(Timer timer, Clock clock) {
        this.timer = timer;
        this.clock = clock;
        this.startTime = clock.tick();
    }

    /**
     * Stops recording the elapsed time and updates the timer.
     */
    public void stop() {
        timer.update(clock.tick() - startTime, TimeUnit.NANOSECONDS);
    }
}
