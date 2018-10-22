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
import com.espertech.esper.runtime.internal.kernel.service.EPEventServiceSPI;
import org.junit.Assert;

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
            Assert.assertEquals(2, env.listener("s0").getNewDataList().size());
            Assert.assertEquals(1500L, env.listener("s0").getNewDataList().get(0)[0].get("ct"));
            Assert.assertEquals(3000L, env.listener("s0").getNewDataList().get(1)[0].get("ct"));
            env.listener("s0").reset();

            env.advanceTimeSpan(4500);
            Assert.assertEquals(1, env.listener("s0").getNewDataList().size());
            Assert.assertEquals(4500L, env.listener("s0").getNewDataList().get(0)[0].get("ct"));
            env.listener("s0").reset();

            env.advanceTimeSpan(9000);
            Assert.assertEquals(3, env.listener("s0").getNewDataList().size());
            Assert.assertEquals(6000L, env.listener("s0").getNewDataList().get(0)[0].get("ct"));
            Assert.assertEquals(7500L, env.listener("s0").getNewDataList().get(1)[0].get("ct"));
            Assert.assertEquals(9000L, env.listener("s0").getNewDataList().get(2)[0].get("ct"));
            env.listener("s0").reset();

            env.advanceTimeSpan(10499);
            Assert.assertEquals(0, env.listener("s0").getNewDataList().size());

            env.advanceTimeSpan(10499);
            Assert.assertEquals(0, env.listener("s0").getNewDataList().size());

            env.advanceTimeSpan(10500);
            Assert.assertEquals(1, env.listener("s0").getNewDataList().size());
            Assert.assertEquals(10500L, env.listener("s0").getNewDataList().get(0)[0].get("ct"));
            env.listener("s0").reset();

            env.advanceTimeSpan(10500);
            Assert.assertEquals(0, env.listener("s0").getNewDataList().size());

            env.eventService().advanceTimeSpan(14000, 200);
            Assert.assertEquals(14000, env.eventService().getCurrentTime());
            Assert.assertEquals(2, env.listener("s0").getNewDataList().size());
            Assert.assertEquals(12100L, env.listener("s0").getNewDataList().get(0)[0].get("ct"));
            Assert.assertEquals(13700L, env.listener("s0").getNewDataList().get(1)[0].get("ct"));

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
            Assert.assertEquals(null, env.eventService().getNextScheduledTime());
            assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[0][]);

            env.undeployAll();
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
