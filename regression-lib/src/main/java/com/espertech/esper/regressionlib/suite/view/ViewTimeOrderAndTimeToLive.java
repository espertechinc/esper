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
package com.espertech.esper.regressionlib.suite.view;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanTimestamp;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class ViewTimeOrderAndTimeToLive {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewTimeOrderSceneOne());
        execs.add(new ViewTimeOrderSceneTwo());
        execs.add(new ViewTimeOrderTTLTimeToLive());
        execs.add(new ViewTimeOrderTTLMonthScoped());
        execs.add(new ViewTimeOrderTTLTimeOrderRemoveStream());
        execs.add(new ViewTimeOrderTTLTimeOrder());
        execs.add(new ViewTimeOrderTTLGroupedWindow());
        execs.add(new ViewTimeOrderTTLInvalid());
        execs.add(new ViewTimeOrderTTLPreviousAndPriorSceneOne());
        execs.add(new ViewTimeOrderTTLPreviousAndPriorSceneTwo());
        return execs;
    }

    public static class ViewTimeOrderSceneOne implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            env.advanceTime(1000);

            String text = "@name('s0') select irstream * from SupportBeanTimestamp#time_order(timestamp, 10 sec)";
            env.compileDeploy(text).addListener("s0").milestone(0);

            // 1st event
            env.advanceTime(1000);
            sendEvent(env, "E1", 3000);
            assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.milestone(1);

            // 2nd event
            env.advanceTime(2000);
            sendEvent(env, "E2", 2000);
            assertEquals("E2", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), new String[]{"id"}, new Object[][]{{"E2"}, {"E1"}});

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), new String[]{"id"}, new Object[][]{{"E2"}, {"E1"}});

            // 3rd event
            env.advanceTime(3000);
            sendEvent(env, "E3", 3000);
            assertEquals("E3", env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.milestone(3);

            // 4th event
            env.advanceTime(4000);
            sendEvent(env, "E4", 2500);
            assertEquals("E4", env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.milestone(4);

            // Window pushes out event E2
            env.advanceTime(11999);
            assertFalse(env.listener("s0").isInvoked());
            env.advanceTime(12000);
            assertNull(env.listener("s0").getLastNewData());
            EventBean[] oldData = env.listener("s0").getLastOldData();
            EPAssertionUtil.assertPropsPerRow(oldData, new String[]{"id"}, new Object[][]{{"E2"}});
            env.listener("s0").reset();

            env.milestone(5);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), new String[]{"id"}, new Object[][]{{"E4"}, {"E1"}, {"E3"}});

            // Window pushes out event E4
            env.advanceTime(12499);
            assertFalse(env.listener("s0").isInvoked());
            env.advanceTime(12500);
            assertNull(env.listener("s0").getLastNewData());
            oldData = env.listener("s0").getLastOldData();
            EPAssertionUtil.assertPropsPerRow(oldData, new String[]{"id"}, new Object[][]{{"E4"}});
            env.listener("s0").reset();

            env.milestone(6);

            // Window pushes out event E1 and E3
            env.advanceTime(13000);
            assertNull(env.listener("s0").getLastNewData());
            oldData = env.listener("s0").getLastOldData();
            EPAssertionUtil.assertPropsPerRow(oldData, new String[]{"id"}, new Object[][]{{"E1"}, {"E3"}});
            env.listener("s0").reset();

            env.milestone(7);

            // E5
            env.advanceTime(14000);
            sendEvent(env, "E5", 14200);
            assertEquals("E5", env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.milestone(8);

            // E6
            env.advanceTime(14000);
            sendEvent(env, "E6", 14100);
            assertEquals("E6", env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.milestone(9);

            // E7
            env.advanceTime(15000);
            sendEvent(env, "E7", 15000);
            assertEquals("E7", env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.milestone(10);

            // E8
            env.advanceTime(15000);
            sendEvent(env, "E8", 14150);
            assertEquals("E8", env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.milestone(11);

            // Window pushes out events
            env.advanceTime(24500);
            assertNull(env.listener("s0").getLastNewData());
            oldData = env.listener("s0").getLastOldData();
            EPAssertionUtil.assertPropsPerRow(oldData, new String[]{"id"}, new Object[][]{{"E6"}, {"E8"}, {"E5"}});
            env.listener("s0").reset();

            env.milestone(12);

            // Window pushes out events
            env.advanceTime(25000);
            assertNull(env.listener("s0").getLastNewData());
            oldData = env.listener("s0").getLastOldData();
            EPAssertionUtil.assertPropsPerRow(oldData, new String[]{"id"}, new Object[][]{{"E7"}});
            env.listener("s0").reset();

            env.milestone(13);

            // E9 is very old
            env.advanceTime(25000);
            sendEvent(env, "E9", 15000);
            EventBean[] newData = env.listener("s0").getLastNewData();
            EPAssertionUtil.assertPropsPerRow(newData, new String[]{"id"}, new Object[][]{{"E9"}});
            oldData = env.listener("s0").getLastOldData();
            EPAssertionUtil.assertPropsPerRow(oldData, new String[]{"id"}, new Object[][]{{"E9"}});
            env.listener("s0").reset();

            env.milestone(14);

            // E10 at 26 sec
            env.advanceTime(26000);
            sendEvent(env, "E10", 26000);
            assertEquals("E10", env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.milestone(15);

            // E11 at 27 sec
            env.advanceTime(27000);
            sendEvent(env, "E11", 27000);
            assertEquals("E11", env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.milestone(16);

            // E12 and E13 at 25 sec
            env.advanceTime(28000);
            sendEvent(env, "E12", 25000);
            assertEquals("E12", env.listener("s0").assertOneGetNewAndReset().get("id"));
            sendEvent(env, "E13", 25000);
            assertEquals("E13", env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.milestone(17);

            // Window pushes out events
            env.advanceTime(35000);
            assertNull(env.listener("s0").getLastNewData());
            oldData = env.listener("s0").getLastOldData();
            EPAssertionUtil.assertPropsPerRow(oldData, new String[]{"id"}, new Object[][]{{"E12"}, {"E13"}});
            env.listener("s0").reset();

            env.milestone(18);

            // E10 at 26 sec
            env.advanceTime(35000);
            sendEvent(env, "E14", 26500);
            assertEquals("E14", env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.milestone(19);

            // Window pushes out events
            env.advanceTime(36000);
            assertNull(env.listener("s0").getLastNewData());
            oldData = env.listener("s0").getLastOldData();
            EPAssertionUtil.assertPropsPerRow(oldData, new String[]{"id"}, new Object[][]{{"E10"}});
            env.listener("s0").reset();

            env.milestone(20);
            // leaving 1 event in the window

            env.undeployAll();
        }
    }

    public static class ViewTimeOrderSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString,longPrimitive".split(",");

            env.advanceTime(0);
            String epl = "@Name('s0') select irstream * from SupportBean.ext:time_order(longPrimitive, 10 sec)";
            env.compileDeploy(epl).addListener("s0");

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, null);

            env.advanceTime(1000);
            sendSupportBeanWLong(env, "E1", 5000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 5000L});

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1", 5000L}});
            env.advanceTime(2000);
            sendSupportBeanWLong(env, "E2", 4000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 4000L});

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E2", 4000L}, {"E1", 5000L}});
            env.advanceTime(13999);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            env.advanceTime(14000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E2", 4000L});

            env.milestone(4);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1", 5000L}});
            sendSupportBeanWLong(env, "E3", 5000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 5000L});

            env.milestone(5);

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1", 5000L}, {"E3", 5000L}});
            env.advanceTime(14999);
            assertFalse(env.listener("s0").isInvoked());
            env.advanceTime(15000);
            assertNull(env.listener("s0").getLastNewData());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastOldData(), fields, new Object[][]{{"E1", 5000L}, {"E3", 5000L}});

            env.milestone(6);

            sendSupportBeanWLong(env, "E4", 2500);
            EPAssertionUtil.assertProps(env.listener("s0").assertGetAndResetIRPair(), fields, new Object[]{"E4", 2500L}, new Object[]{"E4", 2500L});

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[0][]);
            env.advanceTime(99999);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ViewTimeOrderTTLTimeToLive implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String[] fields = "id".split(",");
            String epl = "@name('s0') select irstream * from SupportBeanTimestamp#timetolive(timestamp)";
            env.compileDeploy(epl).addListener("s0").milestone(0);

            sendEvent(env, "E1", 1000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});

            env.milestone(1);

            sendEvent(env, "E2", 500);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E2"}, {"E1"}});

            env.milestone(2);

            env.advanceTime(499);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.advanceTime(500);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E2"});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});

            env.milestone(3);

            sendEvent(env, "E3", 200);
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E3"}, new Object[]{"E3"});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});

            env.milestone(4);

            sendEvent(env, "E4", 1200);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4"});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}, {"E4"}});

            env.milestone(5);

            sendEvent(env, "E5", 1000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5"});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}, {"E4"}, {"E5"}});

            env.advanceTime(999);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.advanceTime(1000);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetDataListsFlattened(), fields, null, new Object[][]{{"E1"}, {"E5"}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E4"}});

            env.milestone(6);

            env.advanceTime(1199);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.advanceTime(1200);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E4"});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, null);

            sendEvent(env, "E6", 1200);
            EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E6"}, new Object[]{"E6"});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, null);

            env.milestone(7);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            env.undeployAll();
        }
    }

    private static class ViewTimeOrderTTLMonthScoped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendCurrentTime(env, "2002-02-01T09:00:00.000");
            env.compileDeploy("@name('s0') select rstream * from SupportBeanTimestamp#time_order(timestamp, 1 month)").addListener("s0");

            sendEvent(env, "E1", DateTime.parseDefaultMSec("2002-02-01T09:00:00.000"));
            sendCurrentTimeWithMinus(env, "2002-03-01T09:00:00.000", 1);
            assertFalse(env.listener("s0").isInvoked());

            sendCurrentTime(env, "2002-03-01T09:00:00.000");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "id".split(","), new Object[][]{{"E1"}});

            env.undeployAll();
        }
    }

    private static class ViewTimeOrderTTLTimeOrderRemoveStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 1000);
            String epl = "insert rstream into OrderedStream select rstream id from SupportBeanTimestamp#time_order(timestamp, 10 sec);\n" +
                "@name('s0') select * from OrderedStream";
            env.compileDeploy(epl).addListener("s0");

            // 1st event at 21 sec
            sendTimer(env, 21000);
            sendEvent(env, "E1", 21000);

            // 2nd event at 22 sec
            sendTimer(env, 22000);
            sendEvent(env, "E2", 22000);

            env.milestone(0);

            // 3nd event at 28 sec
            sendTimer(env, 28000);
            sendEvent(env, "E3", 28000);

            // 4th event at 30 sec, however is 27 sec (old 3 sec)
            sendTimer(env, 30000);
            sendEvent(env, "E4", 27000);

            env.milestone(1);

            // 5th event at 30 sec, however is 22 sec (old 8 sec)
            sendEvent(env, "E5", 22000);

            // flush one
            sendTimer(env, 30999);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, 31000);
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            Assert.assertEquals("E1", env.listener("s0").getLastNewData()[0].get("id"));
            env.listener("s0").reset();

            // 6th event at 31 sec, however is 21 sec (old 10 sec)
            sendEvent(env, "E6", 21000);
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            Assert.assertEquals("E6", env.listener("s0").getLastNewData()[0].get("id"));
            env.listener("s0").reset();

            // 7th event at 31 sec, however is 21.3 sec (old 9.7 sec)
            sendEvent(env, "E7", 21300);

            // flush one
            sendTimer(env, 31299);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(env, 31300);
            Assert.assertEquals(1, env.listener("s0").getNewDataList().size());
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            Assert.assertEquals("E7", env.listener("s0").getLastNewData()[0].get("id"));
            env.listener("s0").reset();

            // flush two
            sendTimer(env, 31999);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(env, 32000);

            EventBean[] result = env.listener("s0").getNewDataListFlattened();
            assertEquals(2, result.length);
            Assert.assertEquals("E2", result[0].get("id"));
            Assert.assertEquals("E5", result[1].get("id"));
            env.listener("s0").reset();

            // flush one
            sendTimer(env, 36999);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(env, 37000);
            Assert.assertEquals(1, env.listener("s0").getNewDataList().size());
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            Assert.assertEquals("E4", env.listener("s0").getLastNewData()[0].get("id"));
            env.listener("s0").reset();

            // rather old event
            sendEvent(env, "E8", 21000);
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            Assert.assertEquals("E8", env.listener("s0").getLastNewData()[0].get("id"));
            env.listener("s0").reset();

            // 9-second old event for posting at 38 sec
            sendEvent(env, "E9", 28000);

            // flush two
            sendTimer(env, 37999);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(env, 38000);
            result = env.listener("s0").getNewDataListFlattened();
            assertEquals(2, result.length);
            Assert.assertEquals("E3", result[0].get("id"));
            Assert.assertEquals("E9", result[1].get("id"));
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ViewTimeOrderTTLTimeOrder implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 1000);

            String epl = "@name('s0') select irstream * from SupportBeanTimestamp#time_order(timestamp, 10 sec)";
            env.compileDeploy(epl).addListener("s0");
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, null);

            sendTimer(env, 21000);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, null);

            env.milestone(0);

            // 1st event at 21 sec
            sendEvent(env, "E1", 21000);
            Assert.assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E1"}});

            // 2nd event at 22 sec
            sendTimer(env, 22000);
            sendEvent(env, "E2", 22000);
            Assert.assertEquals("E2", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E1"}, {"E2"}});

            // 3nd event at 28 sec
            sendTimer(env, 28000);
            sendEvent(env, "E3", 28000);
            Assert.assertEquals("E3", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

            env.milestone(1);

            // 4th event at 30 sec, however is 27 sec (old 3 sec)
            sendTimer(env, 30000);
            sendEvent(env, "E4", 27000);
            Assert.assertEquals("E4", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E1"}, {"E2"}, {"E4"}, {"E3"}});

            // 5th event at 30 sec, however is 22 sec (old 8 sec)
            sendEvent(env, "E5", 22000);
            Assert.assertEquals("E5", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E1"}, {"E2"}, {"E5"}, {"E4"}, {"E3"}});

            // flush one
            sendTimer(env, 30999);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(env, 31000);
            assertNull(env.listener("s0").getLastNewData());
            Assert.assertEquals(1, env.listener("s0").getOldDataList().size());
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            Assert.assertEquals("E1", env.listener("s0").getLastOldData()[0].get("id"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E2"}, {"E5"}, {"E4"}, {"E3"}});

            // 6th event at 31 sec, however is 21 sec (old 10 sec)
            sendEvent(env, "E6", 21000);
            Assert.assertEquals(1, env.listener("s0").getNewDataList().size());
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            Assert.assertEquals("E6", env.listener("s0").getLastNewData()[0].get("id"));
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            Assert.assertEquals("E6", env.listener("s0").getLastOldData()[0].get("id"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E2"}, {"E5"}, {"E4"}, {"E3"}});

            // 7th event at 31 sec, however is 21.3 sec (old 9.7 sec)
            sendEvent(env, "E7", 21300);
            Assert.assertEquals("E7", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E7"}, {"E2"}, {"E5"}, {"E4"}, {"E3"}});

            // flush one
            sendTimer(env, 31299);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(env, 31300);
            assertNull(env.listener("s0").getLastNewData());
            Assert.assertEquals(1, env.listener("s0").getOldDataList().size());
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            Assert.assertEquals("E7", env.listener("s0").getLastOldData()[0].get("id"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E2"}, {"E5"}, {"E4"}, {"E3"}});

            // flush two
            sendTimer(env, 31999);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(env, 32000);
            assertNull(env.listener("s0").getLastNewData());
            Assert.assertEquals(1, env.listener("s0").getOldDataList().size());
            Assert.assertEquals(2, env.listener("s0").getLastOldData().length);
            Assert.assertEquals("E2", env.listener("s0").getLastOldData()[0].get("id"));
            Assert.assertEquals("E5", env.listener("s0").getLastOldData()[1].get("id"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E4"}, {"E3"}});

            // flush one
            sendTimer(env, 36999);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(env, 37000);
            assertNull(env.listener("s0").getLastNewData());
            Assert.assertEquals(1, env.listener("s0").getOldDataList().size());
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            Assert.assertEquals("E4", env.listener("s0").getLastOldData()[0].get("id"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E3"}});

            // rather old event
            sendEvent(env, "E8", 21000);
            Assert.assertEquals(1, env.listener("s0").getNewDataList().size());
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            Assert.assertEquals("E8", env.listener("s0").getLastNewData()[0].get("id"));
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            Assert.assertEquals("E8", env.listener("s0").getLastOldData()[0].get("id"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E3"}});

            // 9-second old event for posting at 38 sec
            sendEvent(env, "E9", 28000);
            Assert.assertEquals("E9", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E3"}, {"E9"}});

            // flush two
            sendTimer(env, 37999);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(env, 38000);
            assertNull(env.listener("s0").getLastNewData());
            Assert.assertEquals(1, env.listener("s0").getOldDataList().size());
            Assert.assertEquals(2, env.listener("s0").getLastOldData().length);
            Assert.assertEquals("E3", env.listener("s0").getLastOldData()[0].get("id"));
            Assert.assertEquals("E9", env.listener("s0").getLastOldData()[1].get("id"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, null);

            // new event
            sendEvent(env, "E10", 38000);
            Assert.assertEquals("E10", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E10"}});

            // flush last
            sendTimer(env, 47999);
            assertFalse(env.listener("s0").isInvoked());
            sendTimer(env, 48000);
            assertNull(env.listener("s0").getLastNewData());
            Assert.assertEquals(1, env.listener("s0").getOldDataList().size());
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            Assert.assertEquals("E10", env.listener("s0").getLastOldData()[0].get("id"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, null);

            // last, in the future
            sendEvent(env, "E11", 70000);
            Assert.assertEquals("E11", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E11"}});

            sendTimer(env, 80000);
            assertNull(env.listener("s0").getLastNewData());
            Assert.assertEquals(1, env.listener("s0").getOldDataList().size());
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            Assert.assertEquals("E11", env.listener("s0").getLastOldData()[0].get("id"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, null);

            sendTimer(env, 100000);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, null);

            env.undeployAll();
        }
    }

    private static class ViewTimeOrderTTLGroupedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 20000);
            String epl = "@name('s0') select irstream * from SupportBeanTimestamp#groupwin(groupId)#time_order(timestamp, 10 sec)";
            env.compileDeploy(epl).addListener("s0");

            // 1st event is old
            sendEvent(env, "E1", "G1", 10000);
            Assert.assertEquals(1, env.listener("s0").getNewDataList().size());
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            Assert.assertEquals("E1", env.listener("s0").getLastNewData()[0].get("id"));
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            Assert.assertEquals("E1", env.listener("s0").getLastOldData()[0].get("id"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, null);

            env.milestone(0);

            // 2nd just fits
            sendEvent(env, "E2", "G2", 10001);
            Assert.assertEquals("E2", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E2"}});

            sendEvent(env, "E3", "G3", 20000);
            Assert.assertEquals("E3", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E2"}, {"E3"}});

            sendEvent(env, "E4", "G2", 20000);
            Assert.assertEquals("E4", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E2"}, {"E4"}, {"E3"}});

            sendTimer(env, 20001);
            assertNull(env.listener("s0").getLastNewData());
            Assert.assertEquals(1, env.listener("s0").getOldDataList().size());
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            Assert.assertEquals("E2", env.listener("s0").getLastOldData()[0].get("id"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E4"}, {"E3"}});

            env.milestone(1);

            sendTimer(env, 22000);
            sendEvent(env, "E5", "G2", 19000);
            Assert.assertEquals("E5", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E5"}, {"E4"}, {"E3"}});

            sendTimer(env, 29000);
            assertNull(env.listener("s0").getLastNewData());
            Assert.assertEquals(1, env.listener("s0").getOldDataList().size());
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            Assert.assertEquals("E5", env.listener("s0").getLastOldData()[0].get("id"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E4"}, {"E3"}});

            sendTimer(env, 30000);
            assertNull(env.listener("s0").getLastNewData());
            Assert.assertEquals(1, env.listener("s0").getOldDataList().size());
            Assert.assertEquals(2, env.listener("s0").getLastOldData().length);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getLastOldData(), "id".split(","), new Object[][]{{"E4"}, {"E3"}});
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, null);

            sendTimer(env, 100000);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ViewTimeOrderTTLInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBeanTimestamp#time_order(bump, 10 sec)",
                "Failed to validate data window declaration: Invalid parameter expression 0 for Time-Order view: Failed to validate view parameter expression 'bump': Property named 'bump' is not valid in any stream [");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBeanTimestamp#time_order(10 sec)",
                "Failed to validate data window declaration: Time-Order view requires the expression supplying timestamp values, and a numeric or time period parameter for interval size [");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBeanTimestamp#time_order(timestamp, abc)",
                "Failed to validate data window declaration: Invalid parameter expression 1 for Time-Order view: Failed to validate view parameter expression 'abc': Property named 'abc' is not valid in any stream (did you mean 'id'?) [");
        }
    }

    private static class ViewTimeOrderTTLPreviousAndPriorSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 1000);

            String epl = "@name('s0') select irstream id, " +
                " prev(0, id) as prevIdZero, " +
                " prev(1, id) as prevIdOne, " +
                " prior(1, id) as priorIdOne," +
                " prevtail(0, id) as prevTailIdZero, " +
                " prevtail(1, id) as prevTailIdOne, " +
                " prevcount(id) as prevCountId, " +
                " prevwindow(id) as prevWindowId " +
                " from SupportBeanTimestamp#time_order(timestamp, 10 sec)";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = new String[]{"id", "prevIdZero", "prevIdOne", "priorIdOne", "prevTailIdZero", "prevTailIdOne", "prevCountId"};

            sendTimer(env, 20000);
            sendEvent(env, "E1", 25000);
            Assert.assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("id"));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), new String[]{"id"}, new Object[][]{{"E1"}});

            env.milestone(0);

            sendEvent(env, "E2", 21000);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals("E2", theEvent.get("id"));
            Assert.assertEquals("E2", theEvent.get("prevIdZero"));
            Assert.assertEquals("E1", theEvent.get("prevIdOne"));
            Assert.assertEquals("E1", theEvent.get("priorIdOne"));
            Assert.assertEquals("E1", theEvent.get("prevTailIdZero"));
            Assert.assertEquals("E2", theEvent.get("prevTailIdOne"));
            Assert.assertEquals(2L, theEvent.get("prevCountId"));
            EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("prevWindowId"), new Object[]{"E2", "E1"});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E2", "E1", "E1", "E1", "E2", 2L}, {"E1", "E2", "E1", null, "E1", "E2", 2L}});

            sendEvent(env, "E3", 22000);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals("E3", theEvent.get("id"));
            Assert.assertEquals("E2", theEvent.get("prevIdZero"));
            Assert.assertEquals("E3", theEvent.get("prevIdOne"));
            Assert.assertEquals("E2", theEvent.get("priorIdOne"));
            Assert.assertEquals("E1", theEvent.get("prevTailIdZero"));
            Assert.assertEquals("E3", theEvent.get("prevTailIdOne"));
            Assert.assertEquals(3L, theEvent.get("prevCountId"));
            EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("prevWindowId"), new Object[]{"E2", "E3", "E1"});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E2", "E3", "E1", "E1", "E3", 3L}, {"E3", "E2", "E3", "E2", "E1", "E3", 3L}, {"E1", "E2", "E3", null, "E1", "E3", 3L}});

            sendTimer(env, 31000);
            assertNull(env.listener("s0").getLastNewData());
            Assert.assertEquals(1, env.listener("s0").getOldDataList().size());
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            theEvent = env.listener("s0").getLastOldData()[0];
            Assert.assertEquals("E2", theEvent.get("id"));
            Assert.assertNull(theEvent.get("prevIdZero"));
            Assert.assertNull(theEvent.get("prevIdOne"));
            Assert.assertEquals("E1", theEvent.get("priorIdOne"));
            Assert.assertNull(theEvent.get("prevTailIdZero"));
            Assert.assertNull(theEvent.get("prevTailIdOne"));
            Assert.assertNull(theEvent.get("prevCountId"));
            Assert.assertNull(theEvent.get("prevWindowId"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E3", "E3", "E1", "E2", "E1", "E3", 2L}, {"E1", "E3", "E1", null, "E1", "E3", 2L}});

            env.undeployAll();
        }
    }

    public static class ViewTimeOrderTTLPreviousAndPriorSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(1000);

            String text = "@name('s0') select irstream id, " +
                "prev(1, id) as prevId, " +
                "prior(1, id) as priorId, " +
                "prevtail(0, id) as prevtail, " +
                "prevcount(id) as prevCountId, " +
                "prevwindow(id) as prevWindowId " +
                "from SupportBeanTimestamp#time_order(timestamp, 10 sec)";
            env.compileDeploy(text).addListener("s0").milestone(0);

            // event
            env.advanceTime(1000);
            sendEvent(env, "E1", 1000);
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertData(event, "E1", null, null, "E1", 1L, new Object[]{"E1"});

            env.milestone(1);

            // event
            env.advanceTime(10000);
            sendEvent(env, "E2", 10000);
            event = env.listener("s0").assertOneGetNewAndReset();
            assertData(event, "E2", "E2", "E1", "E2", 2L, new Object[]{"E1", "E2"});

            env.milestone(2);

            // event
            env.advanceTime(10500);
            sendEvent(env, "E3", 8000);
            event = env.listener("s0").assertOneGetNewAndReset();
            assertData(event, "E3", "E3", "E2", "E2", 3L, new Object[]{"E1", "E3", "E2"});

            env.milestone(3);

            env.advanceTime(11000);
            assertNull(env.listener("s0").getLastNewData());
            EventBean[] oldData = env.listener("s0").getLastOldData();
            assertData(oldData[0], "E1", null, null, null, null, null);
            env.listener("s0").reset();

            env.milestone(4);

            // event
            env.advanceTime(12000);
            sendEvent(env, "E4", 7000);
            event = env.listener("s0").assertOneGetNewAndReset();
            assertData(event, "E4", "E3", "E3", "E2", 3L, new Object[]{"E4", "E3", "E2"});

            env.milestone(5);

            env.advanceTime(16999);
            assertFalse(env.listener("s0").isInvoked());
            env.advanceTime(17000);
            assertNull(env.listener("s0").getLastNewData());
            oldData = env.listener("s0").getLastOldData();
            assertData(oldData[0], "E4", null, "E3", null, null, null);
            env.listener("s0").reset();

            env.milestone(6);

            env.advanceTime(17999);
            assertFalse(env.listener("s0").isInvoked());
            env.advanceTime(18000);
            assertNull(env.listener("s0").getLastNewData());
            oldData = env.listener("s0").getLastOldData();
            assertData(oldData[0], "E3", null, "E2", null, null, null);
            env.listener("s0").reset();

            env.milestone(7);

            env.undeployAll();
        }
    }

    private static SupportBeanTimestamp sendEvent(RegressionEnvironment env, String id, String groupId, long timestamp) {
        SupportBeanTimestamp theEvent = new SupportBeanTimestamp(id, groupId, timestamp);
        env.sendEventBean(theEvent);
        return theEvent;
    }

    private static SupportBeanTimestamp sendEvent(RegressionEnvironment env, String id, long timestamp) {
        SupportBeanTimestamp theEvent = new SupportBeanTimestamp(id, timestamp);
        env.sendEventBean(theEvent);
        return theEvent;
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void sendCurrentTime(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void sendCurrentTimeWithMinus(RegressionEnvironment env, String time, long minus) {
        env.advanceTime(DateTime.parseDefaultMSec(time) - minus);
    }

    private static void assertData(EventBean event, String id, String prevId, String priorId, String prevTailId, Long prevCountId, Object[] prevWindowId) {
        assertEquals(id, event.get("id"));
        assertEquals(prevId, event.get("prevId"));
        assertEquals(priorId, event.get("priorId"));
        assertEquals(prevTailId, event.get("prevtail"));
        assertEquals(prevCountId, event.get("prevCountId"));
        EPAssertionUtil.assertEqualsExactOrder(prevWindowId, (Object[]) event.get("prevWindowId"));
    }

    private static void sendSupportBeanWLong(RegressionEnvironment env, String string, long longPrimitive) {
        SupportBean sb = new SupportBean(string, 0);
        sb.setLongPrimitive(longPrimitive);
        env.sendEventBean(sb);
    }
}
