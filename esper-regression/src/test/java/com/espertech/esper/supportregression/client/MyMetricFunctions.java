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
package com.espertech.esper.supportregression.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class MyMetricFunctions {
    private static final Logger log = LoggerFactory.getLogger(MyMetricFunctions.class);

    public static boolean takeCPUTime(long nanoSecTarget) {
        if (nanoSecTarget < 100) {
            throw new RuntimeException("CPU time wait nsec less then zero, was " + nanoSecTarget);
        }

        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        if (!mbean.isThreadCpuTimeEnabled()) {
            throw new RuntimeException("ThreadMXBean CPU time reporting not enabled");
        }

        long before = mbean.getCurrentThreadCpuTime();

        while (true) {
            long after = mbean.getCurrentThreadCpuTime();
            long spent = after - before;
            if (spent > nanoSecTarget) {
                break;
            }
        }

        return true;
    }

    public static boolean takeWallTime(long msecTarget) {
        try {
            Thread.sleep(msecTarget);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }
}
