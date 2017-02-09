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
package com.espertech.esper.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Utility for CPU and wall time metrics.
 */
public class MetricUtil {
    private static final Logger log = LoggerFactory.getLogger(MetricUtil.class);

    private static ThreadMXBean threadMXBean;
    private static boolean isCPUEnabled;

    /**
     * Initialize metrics mgmt.
     */
    public static void initialize() {
        threadMXBean = ManagementFactory.getThreadMXBean();
        isCPUEnabled = threadMXBean.isCurrentThreadCpuTimeSupported();

        if (!isCPUEnabled) {
            log.warn("CPU metrics reporting is not enabled by Java VM");
        }
    }

    /**
     * Returns CPU time for the current thread.
     *
     * @return cpu current thread
     */
    public static long getCPUCurrentThread() {
        if (isCPUEnabled) {
            return threadMXBean.getCurrentThreadCpuTime();
        }
        return 0;
    }

    /**
     * Returns wall time using System#nanoTime.
     *
     * @return wall time
     */
    public static long getWall() {
        return System.nanoTime();
    }
}
