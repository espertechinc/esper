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
package com.espertech.esperio.kafka;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.Assert.fail;

public class SupportAwaitUtil {
    public static <T> T awaitOrFail(long waitTime, TimeUnit timeUnit, String message, Supplier<T> supplier) {
        long start = System.currentTimeMillis();
        long waitTimeMSec = TimeUnit.MILLISECONDS.convert(waitTime, timeUnit);

        while (true) {
            T result = supplier.get();
            if (result != null) {
                return result;
            }

            long delta = System.currentTimeMillis() - start;
            if (delta > waitTimeMSec) {
                fail("Failed after waiting for " + waitTime + " " + timeUnit.name() + ": " + message);
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        return null;
    }
}
