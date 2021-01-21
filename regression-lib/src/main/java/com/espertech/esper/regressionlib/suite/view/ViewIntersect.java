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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.bean.SupportSensorEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ViewIntersect {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewIntersectUniqueAndFirstLength());
        execs.add(new ViewIntersectFirstUniqueAndFirstLength());
        execs.add(new ViewIntersectBatchWindow());
        execs.add(new ViewIntersectAndDerivedValue());
        execs.add(new ViewIntersectGroupBy());
        execs.add(new ViewIntersectThreeUnique());
        execs.add(new ViewIntersectPattern());
        execs.add(new ViewIntersectTwoUnique());
        execs.add(new ViewIntersectSorted());
        execs.add(new ViewIntersectTimeWin());
        execs.add(new ViewIntersectTimeWinReversed());
        execs.add(new ViewIntersectTimeWinSODA());
        execs.add(new ViewIntersectLengthOneUnique());
        execs.add(new ViewIntersectTimeUniqueMultikey());
        execs.add(new ViewIntersectGroupTimeUnique());
        execs.add(new ViewIntersectSubselect());
        execs.add(new ViewIntersectFirstUniqueAndLengthOnDelete());
        execs.add(new ViewIntersectTimeWinNamedWindow());
        execs.add(new ViewIntersectTimeWinNamedWindowDelete());
        execs.add(new ViewIntersectGroupTimeLength());
        return execs;
    }

    private static class ViewIntersectGroupTimeLength implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select sum(intPrimitive) as c0 from SupportBean#groupwin(theString)#time(1 second)#length(2)";
            env.advanceTime(0);
            env.compileDeploy(epl).addListener("s0");

            sendAssert(env, "G1", 10, 10);

            env.advanceTime(250);
            sendAssert(env, "G2", 100, 110);

            env.milestone(0);

            env.advanceTime(500);
            sendAssert(env, "G1", 11, 10 + 100 + 11);

            env.advanceTime(750);
            sendAssert(env, "G2", 101, 10 + 100 + 11 + 101);

            env.milestone(1);

            env.advanceTime(800);
            sendAssert(env, "G3", 1000, 10 + 100 + 11 + 101 + 1000);

            env.advanceTime(1000); // expires: {"G1", 10}
            assertReceived(env, 100 + 11 + 101 + 1000);

            env.milestone(2);

            sendAssert(env, "G2", 102, 11 + 101 + 1000 + 102); // expires: {"G2", 100}

            env.advanceTime(1499); // expires: {"G1", 10}
            env.assertListenerNotInvoked("s0");

            env.milestone(3);

            env.advanceTime(1500); // expires: {"G1", 11}
            assertReceived(env, 101 + 1000 + 102);

            env.advanceTime(1750); // expires: {"G2", 101}
            assertReceived(env, 1000 + 102);

            env.milestone(4);

            env.advanceTime(1800); // expires: {"G3", 1000}
            assertReceived(env, 102);

            env.advanceTime(2000); // expires: {"G2", 102}
            assertReceived(env, null);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String theString, int intPrimitive, Object expected) {
            env.sendEventBean(new SupportBean(theString, intPrimitive));
            assertReceived(env, expected);
        }

        private void assertReceived(RegressionEnvironment env, Object expected) {
            env.assertEqualsNew("s0", "c0", expected);
        }
    }

    private static class ViewIntersectUniqueAndFirstLength implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger(1);

            String epl = "@name('s0') select irstream theString, intPrimitive from SupportBean#firstlength(3)#unique(theString)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertionUniqueAndFirstLength(env, milestone);

            env.undeployAll();

            epl = "@name('s0') select irstream theString, intPrimitive from SupportBean#unique(theString)#firstlength(3)";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

            tryAssertionUniqueAndFirstLength(env, milestone);

            env.undeployAll();
        }
    }

    private static class ViewIntersectFirstUniqueAndFirstLength implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select irstream theString, intPrimitive from SupportBean#firstunique(theString)#firstlength(3)";
            env.compileDeployAddListenerMile(epl, "s0", milestone.incrementAndGet());

            tryAssertionFirstUniqueAndLength(env);

            env.undeployAll();

            epl = "@name('s0') select irstream theString, intPrimitive from SupportBean#firstlength(3)#firstunique(theString)";
            env.compileDeployAddListenerMile(epl, "s0", milestone.incrementAndGet());

            tryAssertionFirstUniqueAndLength(env);

            env.undeployAll();
        }
    }

    private static class ViewIntersectFirstUniqueAndLengthOnDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create window MyWindowOne#firstunique(theString)#firstlength(3) as SupportBean;\n" +
                    "insert into MyWindowOne select * from SupportBean;\n" +
                    "on SupportBean_S0 delete from MyWindowOne where theString = p00;\n" +
                    "@name('s0') select irstream * from MyWindowOne";
            env.compileDeployAddListenerMileZero(epl, "s0");

            String[] fields = new String[]{"theString", "intPrimitive"};

            sendEvent(env, "E1", 1);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 1});

            sendEvent(env, "E1", 99);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}});
            env.assertListenerNotInvoked("s0");

            sendEvent(env, "E2", 2);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E2", 2}});
            env.assertPropsNew("s0", fields, new Object[]{"E2", 2});

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E2", 2}});
            env.assertPropsOld("s0", fields, new Object[]{"E1", 1});

            sendEvent(env, "E1", 3);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 3}, {"E2", 2}});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 3});

            sendEvent(env, "E1", 99);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 3}, {"E2", 2}});
            env.assertListenerNotInvoked("s0");

            sendEvent(env, "E3", 3);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 3}, {"E2", 2}, {"E3", 3}});
            env.assertPropsNew("s0", fields, new Object[]{"E3", 3});

            sendEvent(env, "E3", 98);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 3}, {"E2", 2}, {"E3", 3}});
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ViewIntersectBatchWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl;

            // test window
            epl = "@name('s0') select irstream theString from SupportBean#length_batch(3)#unique(intPrimitive) order by theString asc";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            tryAssertionUniqueAndBatch(env, milestone);
            env.undeployAll();

            epl = "@name('s0') select irstream theString from SupportBean#unique(intPrimitive)#length_batch(3) order by theString asc";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            tryAssertionUniqueAndBatch(env, milestone);
            env.undeployAll();

            // test aggregation with window
            epl = "@name('s0') select count(*) as c0, sum(intPrimitive) as c1 from SupportBean#unique(theString)#length_batch(3)";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            tryAssertionUniqueBatchAggreation(env, milestone);
            env.undeployAll();

            epl = "@name('s0') select count(*) as c0, sum(intPrimitive) as c1 from SupportBean#length_batch(3)#unique(theString)";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            tryAssertionUniqueBatchAggreation(env, milestone);
            env.undeployAll();

            // test first-unique
            epl = "@name('s0') select irstream * from SupportBean#firstunique(theString)#length_batch(3)";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            tryAssertionLengthBatchAndFirstUnique(env, milestone);
            env.undeployAll();

            epl = "@name('s0') select irstream * from SupportBean#length_batch(3)#firstunique(theString)";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            tryAssertionLengthBatchAndFirstUnique(env, milestone);
            env.undeployAll();

            // test time-based expiry
            env.advanceTime(0);
            epl = "@name('s0') select * from SupportBean#unique(theString)#time_batch(1)";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            tryAssertionTimeBatchAndUnique(env, 0, milestone);
            env.undeployAll();

            env.advanceTime(0);
            epl = "@name('s0') select * from SupportBean#time_batch(1)#unique(theString)";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            tryAssertionTimeBatchAndUnique(env, 100000, milestone);
            env.undeployAll();

            env.tryInvalidCompile("select * from SupportBean#time_batch(1)#length_batch(10)",
                    "Failed to validate data window declaration: Cannot combined multiple batch data windows into an intersection [");
        }
    }

    private static class ViewIntersectAndDerivedValue implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"total"};

            String epl = "@name('s0') select * from SupportBean#unique(intPrimitive)#unique(intBoxed)#uni(doublePrimitive)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "E1", 1, 10, 100d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr(100d));
            env.assertPropsNew("s0", fields, new Object[]{100d});

            sendEvent(env, "E2", 2, 20, 50d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr(150d));
            env.assertPropsNew("s0", fields, new Object[]{150d});

            sendEvent(env, "E3", 1, 20, 20d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr(20d));
            env.assertPropsNew("s0", fields, new Object[]{20d});

            env.undeployAll();
        }
    }

    private static class ViewIntersectGroupBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String text = "@name('s0') select irstream theString from SupportBean#groupwin(intPrimitive)#length(2)#unique(intBoxed) retain-intersection";
            env.compileDeployAddListenerMileZero(text, "s0");

            sendEvent(env, "E1", 1, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1"));
            env.assertPropsNew("s0", fields, new Object[]{"E1"});

            env.milestone(1);

            sendEvent(env, "E2", 2, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2"));
            env.assertPropsNew("s0", fields, new Object[]{"E2"});

            env.milestone(2);

            sendEvent(env, "E3", 1, 20);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3"));
            env.assertPropsNew("s0", fields, new Object[]{"E3"});

            env.milestone(3);

            sendEvent(env, "E4", 1, 30);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E3", "E4"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E4"}, new Object[]{"E1"});

            env.milestone(4);

            sendEvent(env, "E5", 2, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E3", "E4", "E5"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E5"}, new Object[]{"E2"});

            env.milestone(5);

            sendEvent(env, "E6", 1, 20);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E4", "E5", "E6"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E6"}, new Object[]{"E3"});

            env.milestone(6);

            sendEvent(env, "E7", 1, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E5", "E6", "E7"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E7"}, new Object[]{"E4"});

            env.milestone(7);

            sendEvent(env, "E8", 2, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E6", "E7", "E8"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E8"}, new Object[]{"E5"});

            env.undeployAll();

            // another combination
            env.compileDeployAddListenerMile("@name('s0') select * from SupportBean#groupwin(theString)#time(.0083 sec)#firstevent", "s0", 8);
            env.undeployAll();
        }
    }

    private static class ViewIntersectSubselect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select * from SupportBean_S0 where p00 in (select theString from SupportBean#length(2)#unique(intPrimitive) retain-intersection)";
            env.compileDeployAddListenerMileZero(text, "s0");

            sendEvent(env, "E1", 1);
            sendEvent(env, "E2", 2);

            env.milestone(1);

            sendEvent(env, "E3", 3); // throws out E1
            sendEvent(env, "E4", 2); // throws out E2
            sendEvent(env, "E5", 1); // throws out E3

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            env.assertListenerNotInvoked("s0");

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(1, "E2"));
            env.assertListenerNotInvoked("s0");

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(1, "E3"));
            env.assertListenerNotInvoked("s0");

            env.milestone(4);

            env.sendEventBean(new SupportBean_S0(1, "E4"));
            env.assertListenerInvoked("s0");

            env.sendEventBean(new SupportBean_S0(1, "E5"));
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ViewIntersectThreeUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String epl = "@name('s0') select irstream theString from SupportBean#unique(intPrimitive)#unique(intBoxed)#unique(doublePrimitive) retain-intersection";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "E1", 1, 10, 100d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1"));
            env.assertPropsNew("s0", fields, new Object[]{"E1"});

            env.milestone(0);

            sendEvent(env, "E2", 2, 10, 200d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E2"}, new Object[]{"E1"});

            env.milestone(1);

            sendEvent(env, "E3", 2, 20, 100d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E3"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E3"}, new Object[]{"E2"});

            env.milestone(2);

            sendEvent(env, "E4", 1, 30, 300d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E3", "E4"));
            env.assertPropsNew("s0", fields, new Object[]{"E4"});

            env.milestone(3);

            sendEvent(env, "E5", 3, 40, 400d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E3", "E4", "E5"));
            env.assertPropsNew("s0", fields, new Object[]{"E5"});

            env.milestone(4);

            sendEvent(env, "E6", 3, 40, 300d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E3", "E6"));
            env.assertListener("s0", listener -> {
                Object[] result = {listener.getLastOldData()[0].get("theString"), listener.getLastOldData()[1].get("theString")};
                EPAssertionUtil.assertEqualsAnyOrder(result, new String[]{"E4", "E5"});
                EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E6"});
                listener.reset();
            });

            env.undeployAll();
        }
    }

    private static class ViewIntersectPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String text = "@name('s0') select irstream a.p00||b.p10 as theString from pattern [every a=SupportBean_S0 -> b=SupportBean_S1]#unique(a.id)#unique(b.id) retain-intersection";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            env.sendEventBean(new SupportBean_S1(2, "E2"));
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1E2"));
            env.assertPropsNew("s0", fields, new Object[]{"E1E2"});

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(10, "E3"));
            env.sendEventBean(new SupportBean_S1(20, "E4"));
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1E2", "E3E4"));
            env.assertPropsNew("s0", fields, new Object[]{"E3E4"});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(1, "E5"));
            env.sendEventBean(new SupportBean_S1(2, "E6"));
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E3E4", "E5E6"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E5E6"}, new Object[]{"E1E2"});

            env.undeployAll();
        }
    }

    private static class ViewIntersectTwoUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String epl = "@name('s0') select irstream theString from SupportBean#unique(intPrimitive)#unique(intBoxed) retain-intersection";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "E1", 1, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1"));
            env.assertPropsNew("s0", fields, new Object[]{"E1"});

            sendEvent(env, "E2", 2, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E2"}, new Object[]{"E1"});

            env.milestone(1);

            sendEvent(env, "E3", 1, 20);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E3"));
            env.assertPropsNew("s0", fields, new Object[]{"E3"});

            sendEvent(env, "E4", 3, 20);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E4"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E4"}, new Object[]{"E3"});

            env.milestone(2);

            sendEvent(env, "E5", 2, 30);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E4", "E5"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E5"}, new Object[]{"E2"});

            env.milestone(3);

            sendEvent(env, "E6", 3, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E5", "E6"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E6"}, new Object[]{"E4"});

            env.milestone(4);

            sendEvent(env, "E7", 3, 30);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E7"));
            env.assertListener("s0", listener -> {
                assertEquals(2, listener.getLastOldData().length);
                Object[] result = {listener.getLastOldData()[0].get("theString"), listener.getLastOldData()[1].get("theString")};
                EPAssertionUtil.assertEqualsAnyOrder(result, new String[]{"E5", "E6"});
                EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E7"});
                listener.reset();
            });

            env.milestone(5);

            sendEvent(env, "E8", 4, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E7", "E8"));
            env.assertPropsNew("s0", fields, new Object[]{"E8"});

            sendEvent(env, "E9", 3, 50);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E8", "E9"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E9"}, new Object[]{"E7"});

            env.milestone(6);

            sendEvent(env, "E10", 2, 50);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E8", "E10"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E10"}, new Object[]{"E9"});

            env.undeployAll();
        }
    }

    private static class ViewIntersectSorted implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String epl = "@name('s0') select irstream theString from SupportBean#sort(2, intPrimitive)#sort(2, intBoxed) retain-intersection";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "E1", 1, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1"));
            env.assertPropsNew("s0", fields, new Object[]{"E1"});

            env.milestone(0);

            sendEvent(env, "E2", 2, 9);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2"));
            env.assertPropsNew("s0", fields, new Object[]{"E2"});

            env.milestone(1);

            sendEvent(env, "E3", 0, 0);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E3"));
            env.assertListener("s0", listener -> {
                Object[] result = {listener.getLastOldData()[0].get("theString"), listener.getLastOldData()[1].get("theString")};
                EPAssertionUtil.assertEqualsAnyOrder(result, new String[]{"E1", "E2"});
                EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E3"});
                listener.reset();
            });

            env.milestone(2);

            sendEvent(env, "E4", -1, -1);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E3", "E4"));
            env.assertPropsNew("s0", fields, new Object[]{"E4"});

            env.milestone(3);

            sendEvent(env, "E5", 1, 1);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E3", "E4"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E5"}, new Object[]{"E5"});

            env.milestone(4);

            sendEvent(env, "E6", 0, 0);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E4", "E6"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E6"}, new Object[]{"E3"});

            env.undeployAll();
        }
    }

    private static class ViewIntersectTimeWin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@name('s0') select irstream theString from SupportBean#unique(intPrimitive)#time(10 sec) retain-intersection";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertionTimeWinUnique(env);

            env.undeployAll();
        }
    }

    private static class ViewIntersectTimeWinReversed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@name('s0') select irstream theString from SupportBean#time(10 sec)#unique(intPrimitive) retain-intersection";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertionTimeWinUnique(env);

            env.undeployAll();
        }
    }

    private static class ViewIntersectTimeWinSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@name('s0') select irstream theString from SupportBean#time(10 seconds)#unique(intPrimitive) retain-intersection";
            env.eplToModelCompileDeploy(epl).addListener("s0").milestone(0);

            tryAssertionTimeWinUnique(env);

            env.undeployAll();
        }
    }

    private static class ViewIntersectTimeWinNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@name('s0') create window MyWindowTwo#time(10 sec)#unique(intPrimitive) retain-intersection as select * from SupportBean;\n" +
                    "insert into MyWindowTwo select * from SupportBean;\n" +
                    "on SupportBean_S0 delete from MyWindowTwo where intBoxed = id;\n";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertionTimeWinUnique(env);

            env.undeployAll();
        }
    }

    private static class ViewIntersectTimeWinNamedWindowDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@name('s0') create window MyWindowThree#time(10 sec)#unique(intPrimitive) retain-intersection as select * from SupportBean;\n" +
                    "insert into MyWindowThree select * from SupportBean\n;" +
                    "on SupportBean_S0 delete from MyWindowThree where intBoxed = id;\n";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = new String[]{"theString"};

            env.advanceTime(1000);
            sendEvent(env, "E1", 1, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1"));
            env.assertPropsNew("s0", fields, new Object[]{"E1"});

            env.milestone(0);

            env.advanceTime(2000);
            sendEvent(env, "E2", 2, 20);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2"));
            env.assertPropsNew("s0", fields, new Object[]{"E2"});

            env.sendEventBean(new SupportBean_S0(20));
            env.assertPropsOld("s0", fields, new Object[]{"E2"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1"));

            env.milestone(1);

            env.advanceTime(3000);
            sendEvent(env, "E3", 3, 30);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E3"));
            env.assertPropsNew("s0", fields, new Object[]{"E3"});
            sendEvent(env, "E4", 3, 40);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E4"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E4"}, new Object[]{"E3"});

            env.milestone(2);

            env.advanceTime(4000);
            sendEvent(env, "E5", 4, 50);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E4", "E5"));
            env.assertPropsNew("s0", fields, new Object[]{"E5"});
            sendEvent(env, "E6", 4, 50);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E4", "E6"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E6"}, new Object[]{"E5"});

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(20));
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E4", "E6"));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBean_S0(50));
            env.assertPropsOld("s0", fields, new Object[]{"E6"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E4"));

            env.milestone(4);

            env.advanceTime(10999);
            env.assertListenerNotInvoked("s0");
            env.advanceTime(11000);
            env.assertPropsOld("s0", fields, new Object[]{"E1"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E4"));

            env.milestone(5);

            env.advanceTime(12999);
            env.assertListenerNotInvoked("s0");
            env.advanceTime(13000);
            env.assertPropsOld("s0", fields, new Object[]{"E4"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr());

            env.milestone(6);

            env.advanceTime(10000000);
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    public static class ViewIntersectLengthOneUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream symbol, price from SupportMarketDataBean#length(1)#unique(symbol)";
            env.compileDeployAddListenerMileZero(text, "s0");
            env.sendEventBean(makeMarketDataEvent("S1", 100));
            env.assertPropsNV("s0", new Object[][]{{"symbol", "S1"}, {"price", 100.0}}, null);

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("S1", 5));
            env.assertPropsNV("s0", new Object[][]{{"symbol", "S1"}, {"price", 5.0}},
                    new Object[][]{{"symbol", "S1"}, {"price", 100.0}});

            env.undeployAll();
        }
    }

    private static class ViewIntersectUniqueSort implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final String symbolCsco = "CSCO.O";
            final String symbolIbm = "IBM.N";
            final String symbolMsft = "MSFT.O";
            final String symbolC = "C.N";

            String epl = "@name('s0') select * from SupportMarketDataBean#unique(symbol)#sort(3, price desc)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            Object[] beans = new Object[10];

            beans[0] = makeEvent(symbolCsco, 50);
            env.sendEventBean(beans[0]);

            env.assertIterator("s0", iterator -> {
                Object[] result = toObjectArray(iterator);
                EPAssertionUtil.assertEqualsExactOrder(new Object[]{beans[0]}, result);
            });
            env.assertListener("s0", listener -> {
                assertTrue(listener.isInvoked());
                EPAssertionUtil.assertEqualsExactOrder((Object[]) null, listener.getLastOldData());
                EPAssertionUtil.assertEqualsExactOrder(new Object[]{beans[0]}, new Object[]{listener.getLastNewData()[0].getUnderlying()});
                listener.reset();
            });


            beans[1] = makeEvent(symbolCsco, 20);
            beans[2] = makeEvent(symbolIbm, 50);
            beans[3] = makeEvent(symbolMsft, 40);
            beans[4] = makeEvent(symbolC, 100);
            beans[5] = makeEvent(symbolIbm, 10);

            env.sendEventBean(beans[1]);
            env.sendEventBean(beans[2]);
            env.sendEventBean(beans[3]);
            env.sendEventBean(beans[4]);
            env.sendEventBean(beans[5]);

            env.assertIterator("s0", iterator -> {
                Object[] result = toObjectArray(iterator);
                EPAssertionUtil.assertEqualsExactOrder(new Object[]{beans[3], beans[4]}, result);
            });

            beans[6] = makeEvent(symbolCsco, 110);
            beans[7] = makeEvent(symbolC, 30);
            beans[8] = makeEvent(symbolCsco, 30);

            env.sendEventBean(beans[6]);
            env.sendEventBean(beans[7]);
            env.sendEventBean(beans[8]);

            env.assertIterator("s0", iterator -> {
                Object[] result = toObjectArray(iterator);
                EPAssertionUtil.assertEqualsExactOrder(new Object[]{beans[3], beans[8]}, result);
            });

            env.undeployAll();
        }
    }

    private static class ViewIntersectGroupTimeUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') SELECT irstream * FROM SupportSensorEvent#groupwin(type)#time(1 hour)#unique(device)#sort(1, measurement desc) as high order by measurement asc";
            env.compileDeployAddListenerMileZero(epl, "s0");

            SupportSensorEvent eventOne = new SupportSensorEvent(1, "Temperature", "Device1", 5.0, 96.5);
            env.sendEventBean(eventOne);
            assertUnderlying(env, new Object[]{eventOne}, null);

            SupportSensorEvent eventTwo = new SupportSensorEvent(2, "Temperature", "Device2", 7.0, 98.5);
            env.sendEventBean(eventTwo);
            assertUnderlying(env, new Object[]{eventTwo}, new Object[]{eventOne});

            SupportSensorEvent eventThree = new SupportSensorEvent(3, "Temperature", "Device2", 4.0, 99.5);
            env.sendEventBean(eventThree);
            assertUnderlying(env, new Object[]{eventThree}, new Object[]{eventThree, eventTwo});

            env.assertIterator("s0", iterator -> assertFalse(iterator.hasNext()));

            env.undeployAll();
        }
    }

    private static class ViewIntersectTimeUniqueMultikey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String epl = "@name('s0') select irstream * from SupportMarketDataBean#time(3.0)#unique(symbol, price)";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = new String[]{"symbol", "price", "volume"};

            sendMDEvent(env, "IBM", 10, 1L);
            env.assertPropsNew("s0", fields, new Object[]{"IBM", 10.0, 1L});

            sendMDEvent(env, "IBM", 11, 2L);
            env.assertPropsNew("s0", fields, new Object[]{"IBM", 11.0, 2L});

            sendMDEvent(env, "IBM", 10, 3L);
            env.assertPropsIRPair("s0", fields, new Object[]{"IBM", 10.0, 3L}, new Object[]{"IBM", 10.0, 1L});

            sendMDEvent(env, "IBM", 11, 4L);
            env.assertPropsIRPair("s0", fields, new Object[]{"IBM", 11.0, 4L}, new Object[]{"IBM", 11.0, 2L});

            env.advanceTime(2000);
            sendMDEvent(env, null, 11, 5L);
            env.assertPropsNew("s0", fields, new Object[]{null, 11.0, 5L});

            env.advanceTime(3000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][] {{"IBM", 10.0, 3L}, new Object[]{"IBM", 11.0, 4L}});

            sendMDEvent(env, null, 11, 6L);
            env.assertPropsIRPair("s0", fields, new Object[]{null, 11.0, 6L}, new Object[]{null, 11.0, 5L});

            env.advanceTime(6000);
            env.assertPropsOld("s0", fields, new Object[]{null, 11.0, 6L});

            env.undeployAll();
        }
    }

    private static void tryAssertionTimeWinUnique(RegressionEnvironment env) {
        String[] fields = new String[]{"theString"};

        env.advanceTime(1000);
        sendEvent(env, "E1", 1);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1"));
        env.assertPropsNew("s0", fields, new Object[]{"E1"});

        env.milestone(1);

        env.advanceTime(2000);
        sendEvent(env, "E2", 2);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2"));
        env.assertPropsNew("s0", fields, new Object[]{"E2"});

        env.milestone(2);

        env.advanceTime(3000);
        sendEvent(env, "E3", 1);
        env.assertPropsIRPair("s0", fields, new Object[]{"E3"}, new Object[]{"E1"});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E3"));

        env.milestone(3);

        env.advanceTime(4000);
        sendEvent(env, "E4", 3);
        env.assertPropsNew("s0", fields, new Object[]{"E4"});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E3", "E4"));
        sendEvent(env, "E5", 3);
        env.assertPropsIRPair("s0", fields, new Object[]{"E5"}, new Object[]{"E4"});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E3", "E5"));

        env.milestone(4);

        env.advanceTime(11999);
        env.assertListenerNotInvoked("s0");
        env.advanceTime(12000);
        env.assertPropsOld("s0", fields, new Object[]{"E2"});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E3", "E5"));

        env.milestone(5);

        env.advanceTime(12999);
        env.assertListenerNotInvoked("s0");
        env.advanceTime(13000);
        env.assertPropsOld("s0", fields, new Object[]{"E3"});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E5"));

        env.milestone(6);

        env.advanceTime(13999);
        env.assertListenerNotInvoked("s0");
        env.advanceTime(14000);
        env.assertPropsOld("s0", fields, new Object[]{"E5"});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr());
    }

    private static void tryAssertionUniqueBatchAggreation(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "c0,c1".split(",");

        env.sendEventBean(new SupportBean("A1", 10));
        env.sendEventBean(new SupportBean("A2", 11));
        env.assertListenerNotInvoked("s0");

        env.sendEventBean(new SupportBean("A3", 12));
        env.assertPropsNew("s0", fields, new Object[]{3L, 10 + 11 + 12});

        env.sendEventBean(new SupportBean("A1", 13));
        env.sendEventBean(new SupportBean("A2", 14));
        env.assertListenerNotInvoked("s0");

        env.sendEventBean(new SupportBean("A3", 15));
        env.assertPropsNew("s0", fields, new Object[]{3L, 13 + 14 + 15});

        env.sendEventBean(new SupportBean("A1", 16));
        env.sendEventBean(new SupportBean("A2", 17));
        env.assertListenerNotInvoked("s0");

        env.sendEventBean(new SupportBean("A3", 18));
        env.assertPropsNew("s0", fields, new Object[]{3L, 16 + 17 + 18});

        env.sendEventBean(new SupportBean("A1", 19));
        env.sendEventBean(new SupportBean("A1", 20));
        env.sendEventBean(new SupportBean("A2", 21));
        env.sendEventBean(new SupportBean("A2", 22));
        env.assertListenerNotInvoked("s0");

        env.sendEventBean(new SupportBean("A3", 23));
        env.assertPropsNew("s0", fields, new Object[]{3L, 20 + 22 + 23});
    }

    private static void tryAssertionUniqueAndBatch(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = new String[]{"theString"};

        sendEvent(env, "E1", 1);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1"));
        env.assertListenerNotInvoked("s0");

        sendEvent(env, "E2", 2);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2"));
        env.assertListenerNotInvoked("s0");

        env.milestoneInc(milestone);

        sendEvent(env, "E3", 3);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, null);
        env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}}, null);

        sendEvent(env, "E4", 4);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E4"));
        env.assertListenerNotInvoked("s0");

        sendEvent(env, "E5", 4); // throws out E5
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E5"));
        env.assertListenerNotInvoked("s0");

        sendEvent(env, "E6", 4); // throws out E6
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E6"));
        env.assertListenerNotInvoked("s0");

        sendEvent(env, "E7", 5);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E6", "E7"));
        env.assertListenerNotInvoked("s0");

        env.milestoneInc(milestone);

        sendEvent(env, "E8", 6);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, null);
        env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E6"}, {"E7"}, {"E8"}}, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

        sendEvent(env, "E8", 7);
        sendEvent(env, "E9", 9);
        sendEvent(env, "E9", 9);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E8", "E9"));
        env.assertListenerNotInvoked("s0");

        env.milestoneInc(milestone);

        sendEvent(env, "E10", 11);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, null);
        env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E10"}, {"E8"}, {"E9"}}, new Object[][]{{"E6"}, {"E7"}, {"E8"}});
    }

    private static void tryAssertionUniqueAndFirstLength(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = new String[]{"theString", "intPrimitive"};

        sendEvent(env, "E1", 1);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}});
        env.assertPropsNew("s0", fields, new Object[]{"E1", 1});

        sendEvent(env, "E2", 2);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        env.assertPropsNew("s0", fields, new Object[]{"E2", 2});

        env.milestoneInc(milestone);

        sendEvent(env, "E1", 3);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 3}, {"E2", 2}});
        env.assertPropsIRPair("s0", fields, new Object[]{"E1", 3}, new Object[]{"E1", 1});

        sendEvent(env, "E3", 30);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 3}, {"E2", 2}, {"E3", 30}});
        env.assertPropsNew("s0", fields, new Object[]{"E3", 30});

        env.milestoneInc(milestone);

        sendEvent(env, "E4", 40);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 3}, {"E2", 2}, {"E3", 30}});
        env.assertListenerNotInvoked("s0");
    }

    private static void tryAssertionFirstUniqueAndLength(RegressionEnvironment env) {

        String[] fields = new String[]{"theString", "intPrimitive"};

        sendEvent(env, "E1", 1);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}});
        env.assertPropsNew("s0", fields, new Object[]{"E1", 1});

        sendEvent(env, "E2", 2);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        env.assertPropsNew("s0", fields, new Object[]{"E2", 2});

        sendEvent(env, "E2", 10);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        env.assertListenerNotInvoked("s0");

        sendEvent(env, "E3", 3);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});
        env.assertPropsNew("s0", fields, new Object[]{"E3", 3});

        sendEvent(env, "E4", 4);
        sendEvent(env, "E4", 5);
        sendEvent(env, "E5", 5);
        sendEvent(env, "E1", 1);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});
        env.assertListenerNotInvoked("s0");
    }

    private static void tryAssertionTimeBatchAndUnique(RegressionEnvironment env, long startTime, AtomicInteger milestone) {
        String[] fields = "theString,intPrimitive".split(",");

        sendEvent(env, "E1", 1);
        sendEvent(env, "E2", 2);
        sendEvent(env, "E1", 3);
        env.assertListenerNotInvoked("s0");

        env.advanceTime(startTime + 1000);
        env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E2", 2}, {"E1", 3}}, null);

        sendEvent(env, "E3", 3);
        sendEvent(env, "E3", 4);
        sendEvent(env, "E3", 5);
        sendEvent(env, "E4", 6);
        sendEvent(env, "E3", 7);
        env.assertListenerNotInvoked("s0");

        env.advanceTime(startTime + 2000);
        env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E4", 6}, {"E3", 7}}, null);
    }

    private static void tryAssertionLengthBatchAndFirstUnique(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "theString,intPrimitive".split(",");

        sendEvent(env, "E1", 1);
        sendEvent(env, "E2", 2);
        sendEvent(env, "E1", 3);
        env.assertListenerNotInvoked("s0");

        sendEvent(env, "E3", 4);
        env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 4}}, null);

        sendEvent(env, "E1", 5);
        sendEvent(env, "E4", 7);
        sendEvent(env, "E1", 6);
        env.assertListenerNotInvoked("s0");

        sendEvent(env, "E5", 9);
        env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E1", 5}, {"E4", 7}, {"E5", 9}}, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 4}});
    }

    private static void sendEvent(RegressionEnvironment env, String theString, int intPrimitive, int intBoxed, double doublePrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        bean.setDoublePrimitive(doublePrimitive);
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env, String theString, int intPrimitive, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static Object[][] toArr(Object... values) {
        Object[][] arr = new Object[values.length][];
        for (int i = 0; i < values.length; i++) {
            arr[i] = new Object[]{values[i]};
        }
        return arr;
    }

    private static void sendMDEvent(RegressionEnvironment env, String symbol, double price, Long volume) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, price, volume, "");
        env.sendEventBean(theEvent);
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol, double price) {
        return new SupportMarketDataBean(symbol, price, 0L, "");
    }

    private static Object makeEvent(String symbol, double price) {
        return new SupportMarketDataBean(symbol, price, 0L, "");
    }

    private static Object[] toObjectArray(Iterator<EventBean> it) {
        List<Object> result = new LinkedList<>();
        for (; it.hasNext(); ) {
            EventBean theEvent = it.next();
            result.add(theEvent.getUnderlying());
        }
        return result.toArray();
    }

    private static void assertUnderlying(RegressionEnvironment env, Object[] newUnd, Object[] oldUnd) {
        env.assertListener("s0", listener -> {
            EPAssertionUtil.assertUnderlyingPerRow(listener.assertInvokedAndReset(), newUnd, oldUnd);
        });
    }
}
