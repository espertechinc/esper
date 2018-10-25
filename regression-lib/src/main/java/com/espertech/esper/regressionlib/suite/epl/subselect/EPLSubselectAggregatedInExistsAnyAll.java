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
package com.espertech.esper.regressionlib.suite.epl.subselect;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class EPLSubselectAggregatedInExistsAnyAll {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectInSimple());
        execs.add(new EPLSubselectExistsSimple());
        execs.add(new EPLSubselectUngroupedWOHavingWRelOpAllAnySome());
        execs.add(new EPLSubselectUngroupedWOHavingWEqualsAllAnySome());
        execs.add(new EPLSubselectUngroupedWOHavingWIn());
        execs.add(new EPLSubselectUngroupedWOHavingWExists());
        execs.add(new EPLSubselectUngroupedWHavingWExists());
        execs.add(new EPLSubselectGroupedWOHavingWRelOpAllAnySome());
        execs.add(new EPLSubselectGroupedWOHavingWEqualsAllAnySome());
        execs.add(new EPLSubselectGroupedWOHavingWIn());
        execs.add(new EPLSubselectGroupedWHavingWIn());
        execs.add(new EPLSubselectGroupedWHavingWEqualsAllAnySome());
        execs.add(new EPLSubselectGroupedWHavingWRelOpAllAnySome());
        execs.add(new EPLSubselectUngroupedWHavingWIn());
        execs.add(new EPLSubselectUngroupedWHavingWRelOpAllAnySome());
        execs.add(new EPLSubselectUngroupedWHavingWEqualsAllAnySome());
        execs.add(new EPLSubselectGroupedWOHavingWExists());
        execs.add(new EPLSubselectGroupedWHavingWExists());
        return execs;
    }

    private static class EPLSubselectUngroupedWHavingWIn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') select value in (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c0," +
                "value not in (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c1 " +
                "from SupportValueEvent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendVEAndAssert(env, fields, 10, new Object[]{null, null});

            env.sendEventBean(new SupportBean("E1", 10));
            sendVEAndAssert(env, fields, 10, new Object[]{null, null});

            env.sendEventBean(new SupportBean("E2", 0));
            sendVEAndAssert(env, fields, 10, new Object[]{true, false});

            env.sendEventBean(new SupportBean("E3", 1));
            sendVEAndAssert(env, fields, 10, new Object[]{false, true});

            env.sendEventBean(new SupportBean("E4", -1));
            sendVEAndAssert(env, fields, 10, new Object[]{true, false});

            env.undeployAll();
        }
    }

    private static class EPLSubselectGroupedWHavingWIn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') select value in (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) != 'E1') as c0," +
                "value not in (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) != 'E1') as c1 " +
                "from SupportValueEvent";
            env.compileDeploy(epl).addListener("s0");

            sendVEAndAssert(env, fields, 10, new Object[]{false, true});

            env.sendEventBean(new SupportBean("E1", 10));
            sendVEAndAssert(env, fields, 10, new Object[]{false, true});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 10));
            sendVEAndAssert(env, fields, 10, new Object[]{true, false});

            env.undeployAll();
        }
    }

    private static class EPLSubselectGroupedWOHavingWIn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') select value in (select sum(intPrimitive) from SupportBean#keepall group by theString) as c0," +
                "value not in (select sum(intPrimitive) from SupportBean#keepall group by theString) as c1 " +
                "from SupportValueEvent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendVEAndAssert(env, fields, 10, new Object[]{false, true});

            env.sendEventBean(new SupportBean("E1", 19));
            env.sendEventBean(new SupportBean("E2", 11));
            sendVEAndAssert(env, fields, 10, new Object[]{false, true});
            sendVEAndAssert(env, fields, 11, new Object[]{true, false});

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedWOHavingWIn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') select value in (select sum(intPrimitive) from SupportBean#keepall) as c0," +
                "value not in (select sum(intPrimitive) from SupportBean#keepall) as c1 " +
                "from SupportValueEvent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendVEAndAssert(env, fields, 10, new Object[]{null, null});

            env.sendEventBean(new SupportBean("E1", 10));
            sendVEAndAssert(env, fields, 10, new Object[]{true, false});

            env.sendEventBean(new SupportBean("E2", 1));
            sendVEAndAssert(env, fields, 10, new Object[]{false, true});

            env.sendEventBean(new SupportBean("E3", -1));
            sendVEAndAssert(env, fields, 10, new Object[]{true, false});

            env.undeployAll();
        }
    }

    private static class EPLSubselectGroupedWOHavingWRelOpAllAnySome implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "@name('s0') select " +
                "value < all (select sum(intPrimitive) from SupportBean#keepall group by theString) as c0, " +
                "value < any (select sum(intPrimitive) from SupportBean#keepall group by theString) as c1, " +
                "value < some (select sum(intPrimitive) from SupportBean#keepall group by theString) as c2 " +
                "from SupportValueEvent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendVEAndAssert(env, fields, 10, new Object[]{true, false, false});

            env.sendEventBean(new SupportBean("E1", 19));
            env.sendEventBean(new SupportBean("E2", 11));
            sendVEAndAssert(env, fields, 10, new Object[]{true, true, true});

            env.sendEventBean(new SupportBean("E3", 9));
            sendVEAndAssert(env, fields, 10, new Object[]{false, true, true});

            env.undeployAll();
        }
    }

    private static class EPLSubselectGroupedWHavingWRelOpAllAnySome implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "@name('s0') select " +
                "value < all (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) not in ('E1', 'E3')) as c0, " +
                "value < any (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) not in ('E1', 'E3')) as c1, " +
                "value < some (select sum(intPrimitive) from SupportBean#keepall group by theString having last(theString) not in ('E1', 'E3')) as c2 " +
                "from SupportValueEvent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendVEAndAssert(env, fields, 10, new Object[]{true, false, false});

            env.sendEventBean(new SupportBean("E1", 19));
            sendVEAndAssert(env, fields, 10, new Object[]{true, false, false});

            env.sendEventBean(new SupportBean("E2", 11));
            sendVEAndAssert(env, fields, 10, new Object[]{true, true, true});

            env.sendEventBean(new SupportBean("E3", 9));
            sendVEAndAssert(env, fields, 10, new Object[]{true, true, true});

            env.sendEventBean(new SupportBean("E4", 9));
            sendVEAndAssert(env, fields, 10, new Object[]{false, true, true});

            env.undeployAll();
        }
    }

    private static class EPLSubselectGroupedWOHavingWEqualsAllAnySome implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "@name('s0') select " +
                "value = all (select sum(intPrimitive) from SupportBean#keepall group by theString) as c0, " +
                "value = any (select sum(intPrimitive) from SupportBean#keepall group by theString) as c1, " +
                "value = some (select sum(intPrimitive) from SupportBean#keepall group by theString) as c2 " +
                "from SupportValueEvent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendVEAndAssert(env, fields, 10, new Object[]{true, false, false});

            env.sendEventBean(new SupportBean("E1", 10));
            sendVEAndAssert(env, fields, 10, new Object[]{true, true, true});

            env.sendEventBean(new SupportBean("E2", 11));
            sendVEAndAssert(env, fields, 10, new Object[]{false, true, true});

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedWOHavingWEqualsAllAnySome implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "@name('s0') select " +
                "value = all (select sum(intPrimitive) from SupportBean#keepall) as c0, " +
                "value = any (select sum(intPrimitive) from SupportBean#keepall) as c1, " +
                "value = some (select sum(intPrimitive) from SupportBean#keepall) as c2 " +
                "from SupportValueEvent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendVEAndAssert(env, fields, 10, new Object[]{null, null, null});

            env.sendEventBean(new SupportBean("E1", 10));
            sendVEAndAssert(env, fields, 10, new Object[]{true, true, true});

            env.sendEventBean(new SupportBean("E2", 11));
            sendVEAndAssert(env, fields, 10, new Object[]{false, false, false});

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedWHavingWEqualsAllAnySome implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "@name('s0') select " +
                "value = all (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c0, " +
                "value = any (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c1, " +
                "value = some (select sum(intPrimitive) from SupportBean#keepall having last(theString) != 'E1') as c2 " +
                "from SupportValueEvent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendVEAndAssert(env, fields, 10, new Object[]{null, null, null});

            env.sendEventBean(new SupportBean("E1", 10));
            sendVEAndAssert(env, fields, 10, new Object[]{null, null, null});

            env.sendEventBean(new SupportBean("E2", 0));
            sendVEAndAssert(env, fields, 10, new Object[]{true, true, true});

            env.sendEventBean(new SupportBean("E3", 1));
            sendVEAndAssert(env, fields, 10, new Object[]{false, false, false});

            env.sendEventBean(new SupportBean("E1", -1));
            sendVEAndAssert(env, fields, 10, new Object[]{null, null, null});

            env.undeployAll();
        }
    }

    private static class EPLSubselectGroupedWHavingWEqualsAllAnySome implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "@name('s0') select " +
                "value = all (select sum(intPrimitive) from SupportBean#keepall group by theString having first(theString) != 'E1') as c0, " +
                "value = any (select sum(intPrimitive) from SupportBean#keepall group by theString having first(theString) != 'E1') as c1, " +
                "value = some (select sum(intPrimitive) from SupportBean#keepall group by theString having first(theString) != 'E1') as c2 " +
                "from SupportValueEvent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendVEAndAssert(env, fields, 10, new Object[]{true, false, false});

            env.sendEventBean(new SupportBean("E1", 10));
            sendVEAndAssert(env, fields, 10, new Object[]{true, false, false});

            env.sendEventBean(new SupportBean("E2", 10));
            sendVEAndAssert(env, fields, 10, new Object[]{true, true, true});

            env.sendEventBean(new SupportBean("E3", 11));
            sendVEAndAssert(env, fields, 10, new Object[]{false, true, true});

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedWHavingWExists implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') select exists (select sum(intPrimitive) from SupportBean having sum(intPrimitive) < 15) as c0," +
                "not exists (select sum(intPrimitive) from SupportBean  having sum(intPrimitive) < 15) as c1 from SupportValueEvent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendVEAndAssert(env, fields, new Object[]{false, true});

            env.sendEventBean(new SupportBean("E1", 1));
            sendVEAndAssert(env, fields, new Object[]{true, false});

            env.sendEventBean(new SupportBean("E1", 100));
            sendVEAndAssert(env, fields, new Object[]{false, true});

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedWOHavingWExists implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') select exists (select sum(intPrimitive) from SupportBean) as c0," +
                "not exists (select sum(intPrimitive) from SupportBean) as c1 from SupportValueEvent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendVEAndAssert(env, fields, new Object[]{true, false});

            env.sendEventBean(new SupportBean("E1", 1));
            sendVEAndAssert(env, fields, new Object[]{true, false});

            env.undeployAll();
        }
    }

    private static class EPLSubselectGroupedWOHavingWExists implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window MyWindow#keepall as (key string, anint int);\n" +
                "insert into MyWindow(key, anint) select id, value from SupportIdAndValueEvent;\n" +
                "@name('s0') select exists (select sum(anint) from MyWindow group by key) as c0," +
                "not exists (select sum(anint) from MyWindow group by key) as c1 from SupportValueEvent;\n";
            env.compileDeploy(epl, path).addListener("s0");
            String[] fields = "c0,c1".split(",");

            sendVEAndAssert(env, fields, new Object[]{false, true});

            env.sendEventBean(new SupportIdAndValueEvent("E1", 19));
            sendVEAndAssert(env, fields, new Object[]{true, false});

            env.compileExecuteFAF("delete from MyWindow", path);

            sendVEAndAssert(env, fields, new Object[]{false, true});

            env.undeployAll();
        }
    }

    private static class EPLSubselectGroupedWHavingWExists implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window MyWindow#keepall as (key string, anint int);\n" +
                "insert into MyWindow(key, anint) select id, value from SupportIdAndValueEvent;\n" +
                "@name('s0') select exists (select sum(anint) from MyWindow group by key having sum(anint) < 15) as c0," +
                "not exists (select sum(anint) from MyWindow group by key having sum(anint) < 15) as c1 from SupportValueEvent";
            String[] fields = "c0,c1".split(",");
            env.compileDeploy(epl, path).addListener("s0");

            sendVEAndAssert(env, fields, new Object[]{false, true});

            env.sendEventBean(new SupportIdAndValueEvent("E1", 19));
            sendVEAndAssert(env, fields, new Object[]{false, true});

            env.sendEventBean(new SupportIdAndValueEvent("E2", 12));
            sendVEAndAssert(env, fields, new Object[]{true, false});

            env.compileExecuteFAF("delete from MyWindow", path);

            sendVEAndAssert(env, fields, new Object[]{false, true});

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedWHavingWRelOpAllAnySome implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "@name('s0') select " +
                "value < all (select sum(intPrimitive) from SupportBean#keepall having last(theString) not in ('E1', 'E3')) as c0, " +
                "value < any (select sum(intPrimitive) from SupportBean#keepall having last(theString) not in ('E1', 'E3')) as c1, " +
                "value < some (select sum(intPrimitive) from SupportBean#keepall having last(theString) not in ('E1', 'E3')) as c2 " +
                "from SupportValueEvent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendVEAndAssert(env, fields, 10, new Object[]{null, null, null});

            env.sendEventBean(new SupportBean("E1", 19));
            sendVEAndAssert(env, fields, 10, new Object[]{null, null, null});

            env.sendEventBean(new SupportBean("E2", 11));
            sendVEAndAssert(env, fields, 10, new Object[]{true, true, true});

            env.sendEventBean(new SupportBean("E3", 9));
            sendVEAndAssert(env, fields, 10, new Object[]{null, null, null});

            env.sendEventBean(new SupportBean("E4", -1000));
            sendVEAndAssert(env, fields, 10, new Object[]{false, false, false});

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedWOHavingWRelOpAllAnySome implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "@name('s0') select " +
                "value < all (select sum(intPrimitive) from SupportBean#keepall) as c0, " +
                "value < any (select sum(intPrimitive) from SupportBean#keepall) as c1, " +
                "value < some (select sum(intPrimitive) from SupportBean#keepall) as c2 " +
                "from SupportValueEvent";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendVEAndAssert(env, fields, 10, new Object[]{null, null, null});

            env.sendEventBean(new SupportBean("E1", 11));
            sendVEAndAssert(env, fields, 10, new Object[]{true, true, true});

            env.sendEventBean(new SupportBean("E2", -1000));
            sendVEAndAssert(env, fields, 10, new Object[]{false, false, false});

            env.undeployAll();
        }
    }

    private static class EPLSubselectExistsSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select id from SupportBean_S0 where exists (select max(id) from SupportBean_S1#length(3))";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEventS0(env, 1);
            Assert.assertEquals(1, env.listener("s0").assertOneGetNewAndReset().get("id"));

            sendEventS1(env, 100);
            sendEventS0(env, 2);
            Assert.assertEquals(2, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectInSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select id from SupportBean_S0 where id in (select max(id) from SupportBean_S1#length(2))";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEventS0(env, 1);
            assertFalse(env.listener("s0").isInvoked());

            sendEventS1(env, 100);
            sendEventS0(env, 2);
            assertFalse(env.listener("s0").isInvoked());

            sendEventS0(env, 100);
            Assert.assertEquals(100, env.listener("s0").assertOneGetNewAndReset().get("id"));

            sendEventS0(env, 200);
            assertFalse(env.listener("s0").isInvoked());

            sendEventS1(env, -1);
            sendEventS1(env, -1);
            sendEventS0(env, -1);
            Assert.assertEquals(-1, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.undeployAll();
        }
    }

    private static void sendVEAndAssert(RegressionEnvironment env, String[] fields, int value, Object[] expected) {
        env.sendEventBean(new SupportValueEvent(value));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);
    }

    private static void sendVEAndAssert(RegressionEnvironment env, String[] fields, Object[] expected) {
        env.sendEventBean(new SupportValueEvent(-1));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);
    }

    private static void sendEventS0(RegressionEnvironment env, int id) {
        env.sendEventBean(new SupportBean_S0(id));
    }

    private static void sendEventS1(RegressionEnvironment env, int id) {
        env.sendEventBean(new SupportBean_S1(id));
    }
}
