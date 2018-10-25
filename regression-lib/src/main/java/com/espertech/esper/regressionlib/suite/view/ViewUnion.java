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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            env.milestone(0);

            sendEvent(env, "E1", 99);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 99}});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 99});

            sendEvent(env, "E2", 2);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 99}, {"E2", 2}});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E2", 2}});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], "theString".split(","), new Object[]{"E1"});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[1], "theString".split(","), new Object[]{"E1"});
            env.listener("s0").reset();

            env.milestone(1);

            sendEvent(env, "E1", 3);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 3}, {"E2", 2}});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 3});

            env.undeployAll();
        }
    }

    private static class ViewUnionFirstUniqueAndFirstLength implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream theString, intPrimitive from SupportBean#firstlength(3)#firstunique(theString) retain-union";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertionFirstUniqueAndFirstLength(env);

            env.undeployAll();

            epl = "@name('s0') select irstream theString, intPrimitive from SupportBean#firstunique(theString)#firstlength(3) retain-union";
            env.compileDeployAddListenerMileZero(epl, "s0");

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
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            sendEvent(env, "E2", 2);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            sendEvent(env, "E3", 3);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});

            sendEvent(env, "E4", 4);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3", "E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4"});

            sendEvent(env, "E5", 4);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5"});

            sendEvent(env, "E6", 4);     // remove stream is E1, E2, E3
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5", "E6"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E6"});

            sendEvent(env, "E7", 5);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5", "E6", "E7"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E7"});

            sendEvent(env, "E8", 6);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3", "E5", "E4", "E6", "E7", "E8"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E8"});

            sendEvent(env, "E9", 7);     // remove stream is E4, E5, E6; E4 and E5 get removed as their
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3", "E6", "E7", "E8", "E9"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields, new Object[][]{{"E4"}, {"E5"}});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E9"});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ViewUnionAndDerivedValue implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"total"};

            String epl = "@name('s0') select * from SupportBean#unique(intPrimitive)#unique(intBoxed)#uni(doublePrimitive) retain-union";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "E1", 1, 10, 100d);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr(100d));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{100d});

            sendEvent(env, "E2", 2, 20, 50d);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr(150d));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{150d});

            sendEvent(env, "E3", 1, 20, 20d);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr(170d));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{170d});

            env.undeployAll();
        }
    }

    private static class ViewUnionGroupBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String text = "@name('s0') select irstream theString from SupportBean#groupwin(intPrimitive)#length(2)#unique(intBoxed) retain-union";
            env.compileDeployAddListenerMileZero(text, "s0");

            sendEvent(env, "E1", 1, 10);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(1);

            sendEvent(env, "E2", 2, 10);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(2);

            sendEvent(env, "E3", 1, 20);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});

            env.milestone(3);

            sendEvent(env, "E4", 1, 30);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3", "E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4"});

            env.milestone(4);

            sendEvent(env, "E5", 2, 10);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5"});

            env.milestone(5);

            sendEvent(env, "E6", 1, 20);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E4", "E5", "E6"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{"E3"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E6"});
            env.listener("s0").reset();

            sendEvent(env, "E7", 1, 10);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E2", "E4", "E5", "E6", "E7"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{"E1"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E7"});
            env.listener("s0").reset();

            env.milestone(6);

            sendEvent(env, "E8", 2, 10);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E4", "E5", "E6", "E7", "E8"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{"E2"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E8"});
            env.listener("s0").reset();

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
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(1, "E2"));
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean_S0(1, "E3"));
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(1, "E4"));
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.milestone(4);

            env.sendEventBean(new SupportBean_S0(1, "E5"));
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class ViewUnionThreeUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String epl = "@name('s0') select irstream theString from SupportBean#unique(intPrimitive)#unique(intBoxed)#unique(doublePrimitive) retain-union";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "E1", 1, 10, 100d);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(0);

            sendEvent(env, "E2", 2, 10, 200d);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(1);

            sendEvent(env, "E3", 2, 20, 100d);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});

            env.milestone(2);

            sendEvent(env, "E4", 1, 30, 300d);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E2", "E3", "E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{"E1"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E4"});
            env.listener("s0").reset();

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
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1E2"});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(10, "E3"));
            env.sendEventBean(new SupportBean_S1(20, "E4"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1E2", "E3E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3E4"});

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(1, "E5"));
            env.sendEventBean(new SupportBean_S1(2, "E6"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E3E4", "E5E6"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{"E1E2"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E5E6"});

            env.undeployAll();
        }
    }

    private static class ViewUnionTwoUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String epl = "@name('s0') select irstream theString from SupportBean#unique(intPrimitive)#unique(intBoxed) retain-union";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "E1", 1, 10);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(1);

            sendEvent(env, "E2", 2, 10);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(2);

            sendEvent(env, "E3", 1, 20);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E2", "E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{"E1"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E3"});
            env.listener("s0").reset();

            env.milestone(3);

            sendEvent(env, "E4", 1, 20);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E2", "E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{"E3"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E4"});
            env.listener("s0").reset();

            env.milestone(4);

            sendEvent(env, "E5", 2, 30);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E2", "E4", "E5"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5"});

            env.milestone(5);

            sendEvent(env, "E6", 3, 10);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E4", "E5", "E6"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{"E2"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E6"});
            env.listener("s0").reset();

            env.milestone(6);

            sendEvent(env, "E7", 3, 30);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E4", "E5", "E6", "E7"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E7"});

            env.milestone(7);

            sendEvent(env, "E8", 4, 10);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E4", "E5", "E7", "E8"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{"E6"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E8"});
            env.listener("s0").reset();

            env.milestone(8);

            sendEvent(env, "E9", 3, 50);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E4", "E5", "E7", "E8", "E9"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E9"});

            env.milestone(9);

            sendEvent(env, "E10", 2, 30);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E4", "E8", "E9", "E10"));
            Assert.assertEquals(2, env.listener("s0").getLastOldData().length);
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"E5"});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[1], fields, new Object[]{"E7"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E10"});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ViewUnionSorted implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};

            String epl = "@name('s0') select irstream theString from SupportBean#sort(2, intPrimitive)#sort(2, intBoxed) retain-union";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "E1", 1, 10);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(1);

            sendEvent(env, "E2", 2, 9);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(2);

            sendEvent(env, "E3", 0, 0);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});

            env.milestone(3);

            sendEvent(env, "E4", -1, -1);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E3", "E4"));
            Assert.assertEquals(2, env.listener("s0").getLastOldData().length);
            Object[] result = {env.listener("s0").getLastOldData()[0].get("theString"), env.listener("s0").getLastOldData()[1].get("theString")};
            EPAssertionUtil.assertEqualsAnyOrder(result, new String[]{"E1", "E2"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E4"});
            env.listener("s0").reset();

            env.milestone(4);

            sendEvent(env, "E5", 1, 1);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E3", "E4"));
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{"E5"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E5"});
            env.listener("s0").reset();

            env.milestone(5);

            sendEvent(env, "E6", 0, 0);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E4", "E6"));
            Assert.assertEquals(1, env.listener("s0").getLastOldData().length);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOld(), fields, new Object[]{"E3"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), fields, new Object[]{"E6"});
            env.listener("s0").reset();

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
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.milestone(0);

            env.advanceTime(2000);
            sendEvent(env, "E2", 2, 20);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E2"});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1"));

            env.milestone(2);

            env.advanceTime(3000);
            sendEvent(env, "E3", 3, 30);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});
            sendEvent(env, "E4", 3, 40);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E3", "E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4"});

            env.milestone(3);

            env.advanceTime(4000);
            sendEvent(env, "E5", 4, 50);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E3", "E4", "E5"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5"});
            sendEvent(env, "E6", 4, 50);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E3", "E4", "E5", "E6"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E6"});

            env.milestone(4);

            env.sendEventBean(new SupportBean_S0(20));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E3", "E4", "E5", "E6"));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(5);

            env.sendEventBean(new SupportBean_S0(50));
            Assert.assertEquals(2, env.listener("s0").getLastOldData().length);
            Object[] result = {env.listener("s0").getLastOldData()[0].get("theString"), env.listener("s0").getLastOldData()[1].get("theString")};
            EPAssertionUtil.assertEqualsAnyOrder(result, new String[]{"E5", "E6"});
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E3", "E4"));

            env.milestone(6);

            env.advanceTime(12999);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(7);

            env.advanceTime(13000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E3"});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E4"));

            env.advanceTime(10000000);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static void tryAssertionTimeWinUnique(RegressionEnvironment env) {
        String[] fields = new String[]{"theString"};

        env.advanceTime(1000);
        sendEvent(env, "E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        env.milestone(1);

        env.advanceTime(2000);
        sendEvent(env, "E2", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

        env.advanceTime(3000);
        sendEvent(env, "E3", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3"});

        env.milestone(2);

        env.advanceTime(4000);
        sendEvent(env, "E4", 3);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4"});
        sendEvent(env, "E5", 1);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5"));
        sendEvent(env, "E6", 3);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E6"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5", "E6"));

        env.milestone(3);

        env.advanceTime(5000);
        sendEvent(env, "E7", 4);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E7"});
        sendEvent(env, "E8", 4);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E8"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8"));

        env.milestone(4);

        env.advanceTime(6000);
        sendEvent(env, "E9", 4);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E9"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9"));

        env.advanceTime(10999);
        assertFalse(env.listener("s0").isInvoked());

        env.milestone(5);

        env.advanceTime(11000);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9"));

        env.advanceTime(12999);
        assertFalse(env.listener("s0").isInvoked());
        env.advanceTime(13000);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E2", "E4", "E5", "E6", "E7", "E8", "E9"));

        env.milestone(6);

        env.advanceTime(14000);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E4"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E2", "E5", "E6", "E7", "E8", "E9"));

        env.milestone(7);

        env.advanceTime(15000);
        EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"E7"});
        EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[1], fields, new Object[]{"E8"});
        env.listener("s0").reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E2", "E5", "E6", "E9"));

        env.advanceTime(1000000);
        assertFalse(env.listener("s0").isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, toArr("E2", "E5", "E6", "E9"));
    }

    private static class ViewUnionInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = null;

            text = "select theString from SupportBean#groupwin(theString)#unique(theString)#merge(intPrimitive) retain-union";
            SupportMessageAssertUtil.tryInvalidCompile(env, text, "Failed to validate data window declaration: Mismatching parameters between 'group' and 'merge'");

            text = "select theString from SupportBean#groupwin(theString)#groupwin(intPrimitive)#unique(theString)#unique(intPrimitive) retain-union";
            SupportMessageAssertUtil.tryInvalidCompile(env, text, "Failed to validate data window declaration: Multiple groupwin-declarations are not supported [select theString from SupportBean#groupwin(theString)#groupwin(intPrimitive)#unique(theString)#unique(intPrimitive) retain-union]");
        }
    }

    private static void tryAssertionFirstUniqueAndFirstLength(RegressionEnvironment env) {
        String[] fields = new String[]{"theString", "intPrimitive"};

        sendEvent(env, "E1", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        sendEvent(env, "E1", 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 2}});
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 2});

        sendEvent(env, "E2", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 1}});
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 1});

        sendEvent(env, "E2", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 1}});
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        sendEvent(env, "E3", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 1}, {"E3", 3}});
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});

        sendEvent(env, "E3", 4);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 1}, {"E3", 3}});
        assertFalse(env.listener("s0").getAndClearIsInvoked());
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
