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

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensure that TimeSourceMills and TimeSourceMills
 * agree on wall clock time.
 *
 * @author Jerry Shea
 */
public class TestTimeSource extends TestCase {
    public void tearDown() {
        TimeSourceServiceImpl.isSystemCurrentTime = true;
    }

    public void testWallClock() throws InterruptedException {
        // allow a tolerance as TimeSourceMillis resolution may be around 16ms
        final long TOLERANCE_MILLISECS = 50, DELAY_MILLISECS = 100;

        // This is a smoke test
        TimeSourceService nanos = new TimeSourceServiceImpl();
        TimeSourceService millis = new TimeSourceServiceImpl();

        assertTimeWithinTolerance(TOLERANCE_MILLISECS, nanos, millis);
        Thread.sleep(DELAY_MILLISECS);
        assertTimeWithinTolerance(TOLERANCE_MILLISECS, nanos, millis);
        Thread.sleep(DELAY_MILLISECS);
        assertTimeWithinTolerance(TOLERANCE_MILLISECS, nanos, millis);
        Thread.sleep(DELAY_MILLISECS);
        assertTimeWithinTolerance(TOLERANCE_MILLISECS, nanos, millis);
    }

    private void assertTimeWithinTolerance(final long TOLERANCE_MILLISECS,
                                           TimeSourceService nanos, TimeSourceService millis) {

        TimeSourceServiceImpl.isSystemCurrentTime = true;
        long nanosWallClockTime = nanos.getTimeMillis();

        TimeSourceServiceImpl.isSystemCurrentTime = false;
        long millisWallClockTime = millis.getTimeMillis();

        long diff = nanosWallClockTime - millisWallClockTime;
        log.info("diff=" + diff + " between " + nanos + " and " + millis);
        assertTrue("Diff " + diff + " >= " + TOLERANCE_MILLISECS, Math.abs(diff) < TOLERANCE_MILLISECS);
    }

    private static final Logger log = LoggerFactory.getLogger(TestTimeSource.class);
}
