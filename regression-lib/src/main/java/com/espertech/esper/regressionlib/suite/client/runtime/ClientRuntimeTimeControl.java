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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.runtime.internal.kernel.service.EPEventServiceSPI;

import java.util.*;

import static org.junit.Assert.*;

public class ClientRuntimeTimeControl {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientRuntimeSendTimeSpan());
        execs.add(new ClientRuntimeNextScheduledTime());
        return execs;
    }

    private static class ClientRuntimeSendTimeSpan implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            env.compileDeploy("@name('s0') select current_timestamp() as ct from pattern[every timer:interval(1.5 sec)]");
            env.addListener("s0");

            env.advanceTimeSpan(3500);
            env.assertListener("s0", listener -> {
                assertEquals(2, listener.getNewDataList().size());
                assertEquals(1500L, listener.getNewDataList().get(0)[0].get("ct"));
                assertEquals(3000L, listener.getNewDataList().get(1)[0].get("ct"));
                listener.reset();
            });

            env.advanceTimeSpan(4500);
            env.assertListener("s0", listener -> {
                assertEquals(1, listener.getNewDataList().size());
                assertEquals(4500L, listener.getNewDataList().get(0)[0].get("ct"));
                listener.reset();
            });

            env.advanceTimeSpan(9000);
            env.assertListener("s0", listener -> {
                assertEquals(3, listener.getNewDataList().size());
                assertEquals(6000L, listener.getNewDataList().get(0)[0].get("ct"));
                assertEquals(7500L, listener.getNewDataList().get(1)[0].get("ct"));
                assertEquals(9000L, listener.getNewDataList().get(2)[0].get("ct"));
                listener.reset();
            });

            env.advanceTimeSpan(10499);
            env.assertListener("s0", listener -> assertEquals(0, listener.getNewDataList().size()));

            env.advanceTimeSpan(10499);
            env.assertListener("s0", listener -> assertEquals(0, listener.getNewDataList().size()));

            env.advanceTimeSpan(10500);
            env.assertListener("s0", listener -> {
                assertEquals(1, listener.getNewDataList().size());
                assertEquals(10500L, listener.getNewDataList().get(0)[0].get("ct"));
                listener.reset();
            });

            env.advanceTimeSpan(10500);
            env.assertListener("s0", listener -> assertEquals(0, listener.getNewDataList().size()));

            env.advanceTimeSpan(14000, 200);
            env.assertListener("s0", listener -> {
                assertEquals(14000, env.eventService().getCurrentTime());
                assertEquals(2, listener.getNewDataList().size());
                assertEquals(12100L, listener.getNewDataList().get(0)[0].get("ct"));
                assertEquals(13700L, listener.getNewDataList().get(1)[0].get("ct"));
            });

            env.undeployAll();
        }
    }

    private static class ClientRuntimeNextScheduledTime implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            EPEventServiceSPI runtimeSPI = (EPEventServiceSPI) env.eventService();

            env.advanceTime(0);
            assertNull(env.eventService().getNextScheduledTime());
            assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[0][]);

            env.compileDeploy("@name('s0') select * from pattern[timer:interval(2 sec)]");
            assertEquals(2000L, (long) env.eventService().getNextScheduledTime());
            assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[][]{{"s0", 2000L}});

            env.compileDeploy("@Name('s2') select * from pattern[timer:interval(150 msec)]");
            assertEquals(150L, (long) env.eventService().getNextScheduledTime());
            assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[][]{{"s2", 150L}, {"s0", 2000L}});

            env.undeployModuleContaining("s2");
            assertEquals(2000L, (long) env.eventService().getNextScheduledTime());
            assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[][]{{"s0", 2000L}});

            env.compileDeploy("@name('s3') select * from pattern[timer:interval(3 sec) and timer:interval(4 sec)]");
            assertEquals(2000L, (long) env.eventService().getNextScheduledTime());
            assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[][]{{"s0", 2000L}, {"s3", 3000L}});

            env.advanceTime(2500);
            assertEquals(3000L, (long) env.eventService().getNextScheduledTime());
            assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[][]{{"s3", 3000L}});

            env.advanceTime(3500);
            assertEquals(4000L, (long) env.eventService().getNextScheduledTime());
            assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[][]{{"s3", 4000L}});

            env.advanceTime(4500);
            assertEquals(null, env.eventService().getNextScheduledTime());
            assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[0][]);

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.STATICHOOK);
        }
    }

    private static void assertSchedules(Map<DeploymentIdNamePair, Long> schedules, Object[][] expected) {
        assertEquals(expected.length, schedules.size());

        Set<Integer> matchNumber = new HashSet<Integer>();
        for (Map.Entry<DeploymentIdNamePair, Long> entry : schedules.entrySet()) {
            boolean matchFound = false;
            for (int i = 0; i < expected.length; i++) {
                if (matchNumber.contains(i)) {
                    continue;
                }
                if (expected[i][0].equals(entry.getKey().getName())) {
                    matchFound = true;
                    matchNumber.add(i);
                    if (expected[i][1] == null && entry.getValue() == null) {
                        continue;
                    }
                    if (!expected[i][1].equals(entry.getValue())) {
                        fail("Failed to match value for key '" + entry.getKey() + "' expected '" + expected[i][i] + "' received '" + entry.getValue() + "'");
                    }
                }
            }
            if (!matchFound) {
                fail("Failed to find key '" + entry.getKey() + "'");
            }
        }
    }
}
