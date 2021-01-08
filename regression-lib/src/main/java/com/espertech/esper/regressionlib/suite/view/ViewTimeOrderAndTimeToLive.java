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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanTimestamp;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
            String[] fields = "id".split(",");
            env.advanceTime(1000);

            String text = "@name('s0') select irstream * from SupportBeanTimestamp#time_order(timestamp, 10 sec)";
            env.compileDeploy(text).addListener("s0").milestone(0);

            // 1st event
            env.advanceTime(1000);
            sendEvent(env, "E1", 3000);
            assertIdReceived(env, "E1");

            env.milestone(1);

            // 2nd event
            env.advanceTime(2000);
            sendEvent(env, "E2", 2000);
            assertIdReceived(env, "E2");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E2"}, {"E1"}});

            env.milestone(2);

            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E2"}, {"E1"}});

            // 3rd event
            env.advanceTime(3000);
            sendEvent(env, "E3", 3000);
            assertIdReceived(env, "E3");

            env.milestone(3);

            // 4th event
            env.advanceTime(4000);
            sendEvent(env, "E4", 2500);
            assertIdReceived(env, "E4");

            env.milestone(4);

            // Window pushes out event E2
            env.advanceTime(11999);
            env.assertListenerNotInvoked("s0");
            env.advanceTime(12000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E2"}});

            env.milestone(5);

            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E4"}, {"E1"}, {"E3"}});

            // Window pushes out event E4
            env.advanceTime(12499);
            env.assertListenerNotInvoked("s0");
            env.advanceTime(12500);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E4"}});

            env.milestone(6);

            // Window pushes out event E1 and E3
            env.advanceTime(13000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E1"}, {"E3"}});

            env.milestone(7);

            // E5
            env.advanceTime(14000);
            sendEvent(env, "E5", 14200);
            assertIdReceived(env, "E5");

            env.milestone(8);

            // E6
            env.advanceTime(14000);
            sendEvent(env, "E6", 14100);
            assertIdReceived(env, "E6");

            env.milestone(9);

            // E7
            env.advanceTime(15000);
            sendEvent(env, "E7", 15000);
            assertIdReceived(env, "E7");

            env.milestone(10);

            // E8
            env.advanceTime(15000);
            sendEvent(env, "E8", 14150);
            assertIdReceived(env, "E8");

            env.milestone(11);

            // Window pushes out events
            env.advanceTime(24500);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E6"}, {"E8"}, {"E5"}});

            env.milestone(12);

            // Window pushes out events
            env.advanceTime(25000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E7"}});

            env.milestone(13);

            // E9 is very old
            env.advanceTime(25000);
            sendEvent(env, "E9", 15000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E9"}}, new Object[][]{{"E9"}});

            env.milestone(14);

            // E10 at 26 sec
            env.advanceTime(26000);
            sendEvent(env, "E10", 26000);
            assertIdReceived(env, "E10");

            env.milestone(15);

            // E11 at 27 sec
            env.advanceTime(27000);
            sendEvent(env, "E11", 27000);
            assertIdReceived(env, "E11");

            env.milestone(16);

            // E12 and E13 at 25 sec
            env.advanceTime(28000);
            sendEvent(env, "E12", 25000);
            assertIdReceived(env, "E12");
            sendEvent(env, "E13", 25000);
            assertIdReceived(env, "E13");

            env.milestone(17);

            // Window pushes out events
            env.advanceTime(35000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E12"}, {"E13"}});

            env.milestone(18);

            // E10 at 26 sec
            env.advanceTime(35000);
            sendEvent(env, "E14", 26500);
            assertIdReceived(env, "E14");

            env.milestone(19);

            // Window pushes out events
            env.advanceTime(36000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E10"}});

            env.milestone(20);
            // leaving 1 event in the window

            env.undeployAll();
        }

        private void assertIdReceived(RegressionEnvironment env, String expected) {
            env.assertEqualsNew("s0", "id", expected);
        }
    }

    public static class ViewTimeOrderSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString,longPrimitive".split(",");

            env.advanceTime(0);
            String epl = "@Name('s0') select irstream * from SupportBean.ext:time_order(longPrimitive, 10 sec)";
            env.compileDeploy(epl).addListener("s0");

            env.assertPropsPerRowIterator("s0", fields, null);

            env.advanceTime(1000);
            sendSupportBeanWLong(env, "E1", 5000);
            env.assertPropsNew("s0", fields, new Object[]{"E1", 5000L});

            env.milestone(1);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1", 5000L}});
            env.advanceTime(2000);
            sendSupportBeanWLong(env, "E2", 4000);
            env.assertPropsNew("s0", fields, new Object[]{"E2", 4000L});

            env.milestone(2);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E2", 4000L}, {"E1", 5000L}});
            env.advanceTime(13999);
            env.assertListenerNotInvoked("s0");

            env.milestone(3);

            env.advanceTime(14000);
            env.assertPropsOld("s0", fields, new Object[]{"E2", 4000L});

            env.milestone(4);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1", 5000L}});
            sendSupportBeanWLong(env, "E3", 5000);
            env.assertPropsNew("s0", fields, new Object[]{"E3", 5000L});

            env.milestone(5);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1", 5000L}, {"E3", 5000L}});
            env.advanceTime(14999);
            env.assertListenerNotInvoked("s0");
            env.advanceTime(15000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E1", 5000L}, {"E3", 5000L}});

            env.milestone(6);

            sendSupportBeanWLong(env, "E4", 2500);
            env.assertPropsIRPair("s0", fields, new Object[]{"E4", 2500L}, new Object[]{"E4", 2500L});

            env.assertPropsPerRowIterator("s0", fields, new Object[0][]);
            env.advanceTime(99999);
            env.assertListenerNotInvoked("s0");

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
            env.assertPropsNew("s0", fields, new Object[]{"E1"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1"}});

            env.milestone(1);

            sendEvent(env, "E2", 500);
            env.assertPropsNew("s0", fields, new Object[]{"E2"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E2"}, {"E1"}});

            env.milestone(2);

            env.advanceTime(499);
            env.assertListenerNotInvoked("s0");

            env.advanceTime(500);
            env.assertPropsOld("s0", fields, new Object[]{"E2"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1"}});

            env.milestone(3);

            sendEvent(env, "E3", 200);
            env.assertPropsIRPair("s0", fields, new Object[]{"E3"}, new Object[]{"E3"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1"}});

            env.milestone(4);

            sendEvent(env, "E4", 1200);
            env.assertPropsNew("s0", fields, new Object[]{"E4"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1"}, {"E4"}});

            env.milestone(5);

            sendEvent(env, "E5", 1000);
            env.assertPropsNew("s0", fields, new Object[]{"E5"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1"}, {"E4"}, {"E5"}});

            env.advanceTime(999);
            env.assertListenerNotInvoked("s0");

            env.advanceTime(1000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E1"}, {"E5"}});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E4"}});

            env.milestone(6);

            env.advanceTime(1199);
            env.assertListenerNotInvoked("s0");

            env.advanceTime(1200);
            env.assertPropsOld("s0", fields, new Object[]{"E4"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, null);

            sendEvent(env, "E6", 1200);
            env.assertPropsIRPair("s0", fields, new Object[]{"E6"}, new Object[]{"E6"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, null);

            env.milestone(7);

            env.assertPropsPerRowIteratorAnyOrder("s0", fields, null);

            env.undeployAll();
        }
    }

    private static class ViewTimeOrderTTLMonthScoped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendCurrentTime(env, "2002-02-01T09:00:00.000");
            env.compileDeploy("@name('s0') select rstream * from SupportBeanTimestamp#time_order(timestamp, 1 month)").addListener("s0");

            sendEvent(env, "E1", DateTime.parseDefaultMSec("2002-02-01T09:00:00.000"));
            sendCurrentTimeWithMinus(env, "2002-03-01T09:00:00.000", 1);
            env.assertListenerNotInvoked("s0");

            sendCurrentTime(env, "2002-03-01T09:00:00.000");
            env.assertPropsPerRowLastNew("s0", "id".split(","), new Object[][]{{"E1"}});

            env.undeployAll();
        }
    }

    private static class ViewTimeOrderTTLTimeOrderRemoveStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "id".split(",");
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
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 31000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E1"}}, null);

            // 6th event at 31 sec, however is 21 sec (old 10 sec)
            sendEvent(env, "E6", 21000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E6"}}, null);

            // 7th event at 31 sec, however is 21.3 sec (old 9.7 sec)
            sendEvent(env, "E7", 21300);

            // flush one
            sendTimer(env, 31299);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 31300);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E7"}}, null);

            // flush two
            sendTimer(env, 31999);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 32000);
            env.assertPropsPerRowIRPairFlattened("s0", fields, new Object[][]{{"E2"}, {"E5"}}, null);

            // flush one
            sendTimer(env, 36999);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 37000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E4"}}, null);

            // rather old event
            sendEvent(env, "E8", 21000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E8"}}, null);

            // 9-second old event for posting at 38 sec
            sendEvent(env, "E9", 28000);

            // flush two
            sendTimer(env, 37999);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 38000);
            env.assertPropsPerRowIRPairFlattened("s0", fields, new Object[][]{{"E3"}, {"E9"}}, null);

            env.undeployAll();
        }
    }

    private static class ViewTimeOrderTTLTimeOrder implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "id".split(",");
            sendTimer(env, 1000);

            String epl = "@name('s0') select irstream * from SupportBeanTimestamp#time_order(timestamp, 10 sec)";
            env.compileDeploy(epl).addListener("s0");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, null);

            sendTimer(env, 21000);
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, null);

            env.milestone(0);

            // 1st event at 21 sec
            sendEvent(env, "E1", 21000);
            assertId(env, "E1");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E1"}});

            // 2nd event at 22 sec
            sendTimer(env, 22000);
            sendEvent(env, "E2", 22000);
            assertId(env, "E2");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E1"}, {"E2"}});

            // 3nd event at 28 sec
            sendTimer(env, 28000);
            sendEvent(env, "E3", 28000);
            assertId(env, "E3");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

            env.milestone(1);

            // 4th event at 30 sec, however is 27 sec (old 3 sec)
            sendTimer(env, 30000);
            sendEvent(env, "E4", 27000);
            assertId(env, "E4");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E1"}, {"E2"}, {"E4"}, {"E3"}});

            // 5th event at 30 sec, however is 22 sec (old 8 sec)
            sendEvent(env, "E5", 22000);
            assertId(env, "E5");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E1"}, {"E2"}, {"E5"}, {"E4"}, {"E3"}});

            // flush one
            sendTimer(env, 30999);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 31000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E1"}});
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E2"}, {"E5"}, {"E4"}, {"E3"}});

            // 6th event at 31 sec, however is 21 sec (old 10 sec)
            sendEvent(env, "E6", 21000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E6"}}, new Object[][]{{"E6"}});
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E2"}, {"E5"}, {"E4"}, {"E3"}});

            // 7th event at 31 sec, however is 21.3 sec (old 9.7 sec)
            sendEvent(env, "E7", 21300);
            assertId(env, "E7");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E7"}, {"E2"}, {"E5"}, {"E4"}, {"E3"}});

            // flush one
            sendTimer(env, 31299);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 31300);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E7"}});
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E2"}, {"E5"}, {"E4"}, {"E3"}});

            // flush two
            sendTimer(env, 31999);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 32000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E2"}, {"E5"}});
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E4"}, {"E3"}});

            // flush one
            sendTimer(env, 36999);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 37000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E4"}});
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E3"}});

            // rather old event
            sendEvent(env, "E8", 21000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E8"}}, new Object[][]{{"E8"}});
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E3"}});

            // 9-second old event for posting at 38 sec
            sendEvent(env, "E9", 28000);
            assertId(env, "E9");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E3"}, {"E9"}});

            // flush two
            sendTimer(env, 37999);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 38000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E3"}, {"E9"}});
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, null);

            // new event
            sendEvent(env, "E10", 38000);
            assertId(env, "E10");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E10"}});

            // flush last
            sendTimer(env, 47999);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 48000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E10"}});
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, null);

            // last, in the future
            sendEvent(env, "E11", 70000);
            assertId(env, "E11");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E11"}});

            sendTimer(env, 80000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E11"}});
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, null);

            sendTimer(env, 100000);
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, null);

            env.undeployAll();
        }
    }

    private static class ViewTimeOrderTTLGroupedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "id".split(",");
            sendTimer(env, 20000);
            String epl = "@name('s0') select irstream * from SupportBeanTimestamp#groupwin(groupId)#time_order(timestamp, 10 sec)";
            env.compileDeploy(epl).addListener("s0");

            // 1st event is old
            sendEvent(env, "E1", "G1", 10000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E1"}}, new Object[][]{{"E1"}});
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, null);

            env.milestone(0);

            // 2nd just fits
            sendEvent(env, "E2", "G2", 10001);
            assertId(env, "E2");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E2"}});

            sendEvent(env, "E3", "G3", 20000);
            assertId(env, "E3");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E2"}, {"E3"}});

            sendEvent(env, "E4", "G2", 20000);
            assertId(env, "E4");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E2"}, {"E4"}, {"E3"}});

            sendTimer(env, 20001);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E2"}});
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E4"}, {"E3"}});

            env.milestone(1);

            sendTimer(env, 22000);
            sendEvent(env, "E5", "G2", 19000);
            assertId(env, "E5");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E5"}, {"E4"}, {"E3"}});

            sendTimer(env, 29000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E5"}});
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E4"}, {"E3"}});

            sendTimer(env, 30000);
            env.assertListener("s0", listener -> {
                assertNull(listener.getLastNewData());
                Assert.assertEquals(1, listener.getOldDataList().size());
                Assert.assertEquals(2, listener.getLastOldData().length);
                EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getLastOldData(), "id".split(","), new Object[][]{{"E4"}, {"E3"}});
                listener.reset();
            });
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, null);

            sendTimer(env, 100000);
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ViewTimeOrderTTLInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.tryInvalidCompile("select * from SupportBeanTimestamp#time_order(bump, 10 sec)",
                "Failed to validate data window declaration: Invalid parameter expression 0 for Time-Order view: Failed to validate view parameter expression 'bump': Property named 'bump' is not valid in any stream [");

            env.tryInvalidCompile("select * from SupportBeanTimestamp#time_order(10 sec)",
                "Failed to validate data window declaration: Time-Order view requires the expression supplying timestamp values, and a numeric or time period parameter for interval size [");

            env.tryInvalidCompile("select * from SupportBeanTimestamp#time_order(timestamp, abc)",
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
            assertId(env, "E1");
            env.assertPropsPerRowIterator("s0", new String[]{"id"}, new Object[][]{{"E1"}});

            env.milestone(0);

            sendEvent(env, "E2", 21000);
            env.assertEventNew("s0", theEvent -> {
                Assert.assertEquals("E2", theEvent.get("id"));
                Assert.assertEquals("E2", theEvent.get("prevIdZero"));
                Assert.assertEquals("E1", theEvent.get("prevIdOne"));
                Assert.assertEquals("E1", theEvent.get("priorIdOne"));
                Assert.assertEquals("E1", theEvent.get("prevTailIdZero"));
                Assert.assertEquals("E2", theEvent.get("prevTailIdOne"));
                Assert.assertEquals(2L, theEvent.get("prevCountId"));
                EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("prevWindowId"), new Object[]{"E2", "E1"});
            });
            env.assertPropsPerRowIterator("s0", fields,
                new Object[][]{{"E2", "E2", "E1", "E1", "E1", "E2", 2L}, {"E1", "E2", "E1", null, "E1", "E2", 2L}});

            sendEvent(env, "E3", 22000);
            env.assertEventNew("s0", theEvent -> {
                Assert.assertEquals("E3", theEvent.get("id"));
                Assert.assertEquals("E2", theEvent.get("prevIdZero"));
                Assert.assertEquals("E3", theEvent.get("prevIdOne"));
                Assert.assertEquals("E2", theEvent.get("priorIdOne"));
                Assert.assertEquals("E1", theEvent.get("prevTailIdZero"));
                Assert.assertEquals("E3", theEvent.get("prevTailIdOne"));
                Assert.assertEquals(3L, theEvent.get("prevCountId"));
                EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("prevWindowId"), new Object[]{"E2", "E3", "E1"});
            });
            env.assertPropsPerRowIterator("s0", fields,
                new Object[][]{{"E2", "E2", "E3", "E1", "E1", "E3", 3L}, {"E3", "E2", "E3", "E2", "E1", "E3", 3L}, {"E1", "E2", "E3", null, "E1", "E3", 3L}});

            sendTimer(env, 31000);
            env.assertListener("s0", listener -> {
                assertNull(listener.getLastNewData());
                Assert.assertEquals(1, listener.getOldDataList().size());
                Assert.assertEquals(1, listener.getLastOldData().length);
                EventBean theEvent = env.listener("s0").getLastOldData()[0];
                Assert.assertEquals("E2", theEvent.get("id"));
                Assert.assertNull(theEvent.get("prevIdZero"));
                Assert.assertNull(theEvent.get("prevIdOne"));
                Assert.assertEquals("E1", theEvent.get("priorIdOne"));
                Assert.assertNull(theEvent.get("prevTailIdZero"));
                Assert.assertNull(theEvent.get("prevTailIdOne"));
                Assert.assertNull(theEvent.get("prevCountId"));
                Assert.assertNull(theEvent.get("prevWindowId"));
                env.listener("s0").reset();
            });
            env.assertPropsPerRowIterator("s0", fields,
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
            env.assertEventNew("s0", event -> {
                assertData(event, "E1", null, null, "E1", 1L, new Object[]{"E1"});
            });

            env.milestone(1);

            // event
            env.advanceTime(10000);
            sendEvent(env, "E2", 10000);
            env.assertEventNew("s0", event -> {
                assertData(event, "E2", "E2", "E1", "E2", 2L, new Object[]{"E1", "E2"});
            });

            env.milestone(2);

            // event
            env.advanceTime(10500);
            sendEvent(env, "E3", 8000);
            env.assertEventNew("s0", event -> {
                assertData(event, "E3", "E3", "E2", "E2", 3L, new Object[]{"E1", "E3", "E2"});
            });

            env.milestone(3);

            env.advanceTime(11000);
            env.assertListener("s0", listener -> {
                assertNull(listener.getLastNewData());
                EventBean[] oldData = listener.getLastOldData();
                assertData(oldData[0], "E1", null, null, null, null, null);
                listener.reset();
            });

            env.milestone(4);

            // event
            env.advanceTime(12000);
            sendEvent(env, "E4", 7000);
            env.assertEventNew("s0", event -> {
                assertData(event, "E4", "E3", "E3", "E2", 3L, new Object[]{"E4", "E3", "E2"});
            });

            env.milestone(5);

            env.advanceTime(16999);
            env.assertListenerNotInvoked("s0");
            env.advanceTime(17000);
            env.assertListener("s0", listener -> {
                assertNull(listener.getLastNewData());
                EventBean[] oldData = listener.getLastOldData();
                assertData(oldData[0], "E4", null, "E3", null, null, null);
                listener.reset();
            });

            env.milestone(6);

            env.advanceTime(17999);
            env.assertListenerNotInvoked("s0");
            env.advanceTime(18000);
            env.assertListener("s0", listener -> {
                assertNull(listener.getLastNewData());
                EventBean[] oldData = listener.getLastOldData();
                assertData(oldData[0], "E3", null, "E2", null, null, null);
                listener.reset();
            });

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

    private static void assertId(RegressionEnvironment env, String expected) {
        env.assertEqualsNew("s0", "id", expected);
    }
}
