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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ViewUnion {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewUnionFirstUniqueAndFirstLength());
        execs.add(new ViewUnionBatchWindow());
        execs.add(new ViewUnionAndDerivedValue());
        execs.add(new ViewUnionGroupBy());
        execs.add(new ViewUnionThreeUnique());
        execs.add(new ViewUnionPattern());
        execs.add(new ViewUnionTwoUnique());
        execs.add(new ViewUnionSorted());
        execs.add(new ViewUnionTimeWin());
        execs.add(new ViewUnionTimeWinSODA());
        execs.add(new ViewUnionInvalid());
        execs.add(new ViewUnionSubselect());
        execs.add(new ViewUnionFirstUniqueAndLengthOnDelete());
        execs.add(new ViewUnionTimeWinNamedWindow());
        execs.add(new ViewUnionTimeWinNamedWindowDelete());
        return execs;
    }

    private static class ViewUnionFirstUniqueAndLengthOnDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create window MyWindowOne#firstunique(theString)#firstlength(3) retain-union as SupportBean;\n" +
                "insert into MyWindowOne select * from SupportBean;\n" +
                "on SupportBean_S0 delete from MyWindowOne where theString = p00;\n" +
                "@name('s0') select irstream * from MyWindowOne;\n";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = new String[]{"theString", "intPrimitive"};

            sendEvent(env, "E1", 1);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 1});

            env.milestone(0);

            sendEvent(env, "E1", 99);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E1", 99}});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 99});

            sendEvent(env, "E2", 2);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E1", 99}, {"E2", 2}});
            env.assertPropsNew("s0", fields, new Object[]{"E2", 2});

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E2", 2}});
            env.assertPropsPerRowIRPair("s0", "theString".split(","), null, new Object[][]{{"E1"}, {"E1"}});

            env.milestone(1);

            sendEvent(env, "E1", 3);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 3}, {"E2", 2}});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 3});

            env.undeployAll();
        }
    }

    private static class ViewUnionFirstUniqueAndFirstLength implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select irstream theString, intPrimitive from SupportBean#firstlength(3)#firstunique(theString) retain-union";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

            tryAssertionFirstUniqueAndFirstLength(env);

            env.undeployAll();

            epl = "@name('s0') select irstream theString, intPrimitive from SupportBean#firstunique(theString)#firstlength(3) retain-union";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

            tryAssertionFirstUniqueAndFirstLength(env);

            env.undeployAll();
        }
    }

    private static class ViewUnionBatchWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String epl = "@name('s0') select irstream theString from SupportBean#length_batch(3)#unique(intPrimitive) retain-union";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "E1", 1);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1"));
            env.assertPropsNew("s0", fields, new Object[]{"E1"});

            sendEvent(env, "E2", 2);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2"));
            env.assertPropsNew("s0", fields, new Object[]{"E2"});

            sendEvent(env, "E3", 3);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3"));
            env.assertPropsNew("s0", fields, new Object[]{"E3"});

            sendEvent(env, "E4", 4);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3", "E4"));
            env.assertPropsNew("s0", fields, new Object[]{"E4"});

            sendEvent(env, "E5", 4);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3", "E4", "E5"));
            env.assertPropsNew("s0", fields, new Object[]{"E5"});

            sendEvent(env, "E6", 4);     // remove stream is E1, E2, E3
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3", "E4", "E5", "E6"));
            env.assertPropsNew("s0", fields, new Object[]{"E6"});

            sendEvent(env, "E7", 5);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3", "E4", "E5", "E6", "E7"));
            env.assertPropsNew("s0", fields, new Object[]{"E7"});

            sendEvent(env, "E8", 6);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3", "E5", "E4", "E6", "E7", "E8"));
            env.assertPropsNew("s0", fields, new Object[]{"E8"});

            sendEvent(env, "E9", 7);     // remove stream is E4, E5, E6; E4 and E5 get removed as their
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3", "E6", "E7", "E8", "E9"));
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E9"}}, new Object[][]{{"E4"}, {"E5"}});

            env.undeployAll();
        }
    }

    private static class ViewUnionAndDerivedValue implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"total"};

            String epl = "@name('s0') select * from SupportBean#unique(intPrimitive)#unique(intBoxed)#uni(doublePrimitive) retain-union";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "E1", 1, 10, 100d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr(100d));
            env.assertPropsNew("s0", fields, new Object[]{100d});

            sendEvent(env, "E2", 2, 20, 50d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr(150d));
            env.assertPropsNew("s0", fields, new Object[]{150d});

            sendEvent(env, "E3", 1, 20, 20d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr(170d));
            env.assertPropsNew("s0", fields, new Object[]{170d});

            env.undeployAll();
        }
    }

    private static class ViewUnionGroupBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String text = "@name('s0') select irstream theString from SupportBean#groupwin(intPrimitive)#length(2)#unique(intBoxed) retain-union";
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
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3", "E4"));
            env.assertPropsNew("s0", fields, new Object[]{"E4"});

            env.milestone(4);

            sendEvent(env, "E5", 2, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3", "E4", "E5"));
            env.assertPropsNew("s0", fields, new Object[]{"E5"});

            env.milestone(5);

            sendEvent(env, "E6", 1, 20);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E4", "E5", "E6"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E6"}, new Object[]{"E3"});

            sendEvent(env, "E7", 1, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E4", "E5", "E6", "E7"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E7"}, new Object[]{"E1"});

            env.milestone(6);

            sendEvent(env, "E8", 2, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E4", "E5", "E6", "E7", "E8"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E8"}, new Object[]{"E2"});

            env.undeployAll();
        }
    }

    private static class ViewUnionSubselect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select * from SupportBean_S0 where p00 in (select theString from SupportBean#length(2)#unique(intPrimitive) retain-union)";
            env.compileDeployAddListenerMileZero(text, "s0");

            sendEvent(env, "E1", 1);
            sendEvent(env, "E2", 2);

            env.milestone(1);

            sendEvent(env, "E3", 3);
            sendEvent(env, "E4", 2); // throws out E1
            sendEvent(env, "E5", 1); // retains E3

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            env.assertListenerNotInvoked("s0");

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(1, "E2"));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBean_S0(1, "E3"));
            env.assertListenerInvoked("s0");

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(1, "E4"));
            env.assertListenerInvoked("s0");

            env.milestone(4);

            env.sendEventBean(new SupportBean_S0(1, "E5"));
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ViewUnionThreeUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String epl = "@name('s0') select irstream theString from SupportBean#unique(intPrimitive)#unique(intBoxed)#unique(doublePrimitive) retain-union";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "E1", 1, 10, 100d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1"));
            env.assertPropsNew("s0", fields, new Object[]{"E1"});

            env.milestone(0);

            sendEvent(env, "E2", 2, 10, 200d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2"));
            env.assertPropsNew("s0", fields, new Object[]{"E2"});

            env.milestone(1);

            sendEvent(env, "E3", 2, 20, 100d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3"));
            env.assertPropsNew("s0", fields, new Object[]{"E3"});

            env.milestone(2);

            sendEvent(env, "E4", 1, 30, 300d);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E3", "E4"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E4"}, new Object[]{"E1"});

            env.undeployAll();
        }
    }

    private static class ViewUnionPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"string"};

            String text = "@name('s0') select irstream a.p00||b.p10 as string from pattern [every a=SupportBean_S0 -> b=SupportBean_S1]#unique(a.id)#unique(b.id) retain-union";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            env.sendEventBean(new SupportBean_S1(2, "E2"));
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1E2"));
            env.assertPropsNew("s0", fields, new Object[]{"E1E2"});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(10, "E3"));
            env.sendEventBean(new SupportBean_S1(20, "E4"));
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1E2", "E3E4"));
            env.assertPropsNew("s0", fields, new Object[]{"E3E4"});

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(1, "E5"));
            env.sendEventBean(new SupportBean_S1(2, "E6"));
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E3E4", "E5E6"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E5E6"}, new Object[]{"E1E2"});

            env.undeployAll();
        }
    }

    private static class ViewUnionTwoUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String epl = "@name('s0') select irstream theString from SupportBean#unique(intPrimitive)#unique(intBoxed) retain-union";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "E1", 1, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1"));
            env.assertPropsNew("s0", fields, new Object[]{"E1"});

            env.milestone(1);

            sendEvent(env, "E2", 2, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2"));
            env.assertPropsNew("s0", fields, new Object[]{"E2"});

            env.milestone(2);

            sendEvent(env, "E3", 1, 20);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E3"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E3"}, new Object[]{"E1"});

            env.milestone(3);

            sendEvent(env, "E4", 1, 20);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E4"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E4"}, new Object[]{"E3"});

            env.milestone(4);

            sendEvent(env, "E5", 2, 30);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E4", "E5"));
            env.assertPropsNew("s0", fields, new Object[]{"E5"});

            env.milestone(5);

            sendEvent(env, "E6", 3, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E4", "E5", "E6"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E6"}, new Object[]{"E2"});

            env.milestone(6);

            sendEvent(env, "E7", 3, 30);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E4", "E5", "E6", "E7"));
            env.assertPropsNew("s0", fields, new Object[]{"E7"});

            env.milestone(7);

            sendEvent(env, "E8", 4, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E4", "E5", "E7", "E8"));
            env.assertPropsIRPair("s0", fields, new Object[]{"E8"}, new Object[]{"E6"});

            env.milestone(8);

            sendEvent(env, "E9", 3, 50);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E4", "E5", "E7", "E8", "E9"));
            env.assertPropsNew("s0", fields, new Object[]{"E9"});

            env.milestone(9);

            sendEvent(env, "E10", 2, 30);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E4", "E8", "E9", "E10"));
            env.assertPropsPerRowIRPair("s0", fields, new Object[][] {{"E10"}}, new Object[][] {{"E5"}, {"E7"}});

            env.undeployAll();
        }
    }

    private static class ViewUnionSorted implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String epl = "@name('s0') select irstream theString from SupportBean#sort(2, intPrimitive)#sort(2, intBoxed) retain-union";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "E1", 1, 10);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1"));
            env.assertPropsNew("s0", fields, new Object[]{"E1"});

            env.milestone(1);

            sendEvent(env, "E2", 2, 9);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2"));
            env.assertPropsNew("s0", fields, new Object[]{"E2"});

            env.milestone(2);

            sendEvent(env, "E3", 0, 0);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3"));
            env.assertPropsNew("s0", fields, new Object[]{"E3"});

            env.milestone(3);

            sendEvent(env, "E4", -1, -1);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E3", "E4"));
            env.assertListener("s0", listener -> {
                assertEquals(2, listener.getLastOldData().length);
                Object[] result = {listener.getLastOldData()[0].get("theString"), listener.getLastOldData()[1].get("theString")};
                EPAssertionUtil.assertEqualsAnyOrder(result, new String[]{"E1", "E2"});
                EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E4"});
                listener.reset();
            });

            env.milestone(4);

            sendEvent(env, "E5", 1, 1);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E3", "E4"));
            env.assertListener("s0", listener -> {
                assertEquals(1, listener.getLastOldData().length);
                EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E5"});
                EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E5"});
                listener.reset();
            });

            env.milestone(5);

            sendEvent(env, "E6", 0, 0);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E4", "E6"));
            env.assertListener("s0", listener -> {
                assertEquals(1, listener.getLastOldData().length);
                EPAssertionUtil.assertProps(listener.assertOneGetOld(), fields, new Object[]{"E3"});
                EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E6"});
                listener.reset();
            });

            env.undeployAll();
        }
    }

    private static class ViewUnionTimeWin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@name('s0') select irstream theString from SupportBean#unique(intPrimitive)#time(10 sec) retain-union";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertionTimeWinUnique(env);

            env.undeployAll();
        }
    }

    private static class ViewUnionTimeWinSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@name('s0') select irstream theString from SupportBean#time(10 seconds)#unique(intPrimitive) retain-union";
            env.eplToModelCompileDeploy(epl).addListener("s0");

            tryAssertionTimeWinUnique(env);

            env.undeployAll();
        }
    }

    private static class ViewUnionTimeWinNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@name('s0') create window MyWindowTwo#time(10 sec)#unique(intPrimitive) retain-union as select * from SupportBean;\n" +
                "insert into MyWindowTwo select * from SupportBean;\n" +
                "on SupportBean_S0 delete from MyWindowTwo where intBoxed = id;\n";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertionTimeWinUnique(env);

            env.undeployAll();
        }
    }

    private static class ViewUnionTimeWinNamedWindowDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@name('s0') create window MyWindowThree#time(10 sec)#unique(intPrimitive) retain-union as select * from SupportBean;\n" +
                "insert into MyWindowThree select * from SupportBean;\n" +
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

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(20));
            env.assertPropsOld("s0", fields, new Object[]{"E2"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1"));

            env.milestone(2);

            env.advanceTime(3000);
            sendEvent(env, "E3", 3, 30);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E3"));
            env.assertPropsNew("s0", fields, new Object[]{"E3"});
            sendEvent(env, "E4", 3, 40);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E3", "E4"));
            env.assertPropsNew("s0", fields, new Object[]{"E4"});

            env.milestone(3);

            env.advanceTime(4000);
            sendEvent(env, "E5", 4, 50);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E3", "E4", "E5"));
            env.assertPropsNew("s0", fields, new Object[]{"E5"});
            sendEvent(env, "E6", 4, 50);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E3", "E4", "E5", "E6"));
            env.assertPropsNew("s0", fields, new Object[]{"E6"});

            env.milestone(4);

            env.sendEventBean(new SupportBean_S0(20));
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E3", "E4", "E5", "E6"));
            env.assertListenerNotInvoked("s0");

            env.milestone(5);

            env.sendEventBean(new SupportBean_S0(50));
            env.assertListener("s0", listener -> {
                assertEquals(2, listener.getLastOldData().length);
                Object[] result = {listener.getLastOldData()[0].get("theString"), listener.getLastOldData()[1].get("theString")};
                EPAssertionUtil.assertEqualsAnyOrder(result, new String[]{"E5", "E6"});
                listener.reset();
            });
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E3", "E4"));

            env.milestone(6);

            env.advanceTime(12999);
            env.assertListenerNotInvoked("s0");

            env.milestone(7);

            env.advanceTime(13000);
            env.assertPropsOld("s0", fields, new Object[]{"E3"});
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E4"));

            env.advanceTime(10000000);
            env.assertListenerNotInvoked("s0");

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

        env.advanceTime(3000);
        sendEvent(env, "E3", 1);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3"));
        env.assertPropsNew("s0", fields, new Object[]{"E3"});

        env.milestone(2);

        env.advanceTime(4000);
        sendEvent(env, "E4", 3);
        env.assertPropsNew("s0", fields, new Object[]{"E4"});
        sendEvent(env, "E5", 1);
        env.assertPropsNew("s0", fields, new Object[]{"E5"});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3", "E4", "E5"));
        sendEvent(env, "E6", 3);
        env.assertPropsNew("s0", fields, new Object[]{"E6"});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3", "E4", "E5", "E6"));

        env.milestone(3);

        env.advanceTime(5000);
        sendEvent(env, "E7", 4);
        env.assertPropsNew("s0", fields, new Object[]{"E7"});
        sendEvent(env, "E8", 4);
        env.assertPropsNew("s0", fields, new Object[]{"E8"});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8"));

        env.milestone(4);

        env.advanceTime(6000);
        sendEvent(env, "E9", 4);
        env.assertPropsNew("s0", fields, new Object[]{"E9"});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9"));

        env.advanceTime(10999);
        env.assertListenerNotInvoked("s0");

        env.milestone(5);

        env.advanceTime(11000);
        env.assertPropsOld("s0", fields, new Object[]{"E1"});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9"));

        env.advanceTime(12999);
        env.assertListenerNotInvoked("s0");
        env.advanceTime(13000);
        env.assertPropsOld("s0", fields, new Object[]{"E3"});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E4", "E5", "E6", "E7", "E8", "E9"));

        env.milestone(6);

        env.advanceTime(14000);
        env.assertPropsOld("s0", fields, new Object[]{"E4"});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E5", "E6", "E7", "E8", "E9"));

        env.milestone(7);

        env.advanceTime(15000);
        env.assertPropsPerRowIRPair("s0", fields, null, new Object[][] {{"E7"}, {"E8"}});
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E5", "E6", "E9"));

        env.advanceTime(1000000);
        env.assertListenerNotInvoked("s0");
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, toArr("E2", "E5", "E6", "E9"));
    }

    private static class ViewUnionInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = null;

            text = "select theString from SupportBean#groupwin(theString)#unique(theString)#merge(intPrimitive) retain-union";
            env.tryInvalidCompile(text, "Failed to validate data window declaration: Mismatching parameters between 'group' and 'merge'");

            text = "select theString from SupportBean#groupwin(theString)#groupwin(intPrimitive)#unique(theString)#unique(intPrimitive) retain-union";
            env.tryInvalidCompile(text, "Failed to validate data window declaration: Multiple groupwin-declarations are not supported [select theString from SupportBean#groupwin(theString)#groupwin(intPrimitive)#unique(theString)#unique(intPrimitive) retain-union]");
        }
    }

    private static void tryAssertionFirstUniqueAndFirstLength(RegressionEnvironment env) {
        String[] fields = new String[]{"theString", "intPrimitive"};

        sendEvent(env, "E1", 1);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}});
        env.assertPropsNew("s0", fields, new Object[]{"E1", 1});

        sendEvent(env, "E1", 2);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E1", 2}});
        env.assertPropsNew("s0", fields, new Object[]{"E1", 2});

        sendEvent(env, "E2", 1);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 1}});
        env.assertPropsNew("s0", fields, new Object[]{"E2", 1});

        sendEvent(env, "E2", 3);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 1}});
        env.assertListenerNotInvoked("s0");

        sendEvent(env, "E3", 3);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 1}, {"E3", 3}});
        env.assertPropsNew("s0", fields, new Object[]{"E3", 3});

        sendEvent(env, "E3", 4);
        env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 1}, {"E3", 3}});
        env.assertListenerNotInvoked("s0");
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
}
