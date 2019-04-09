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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;
import com.espertech.esper.regressionlib.support.bean.SupportTwoKeyEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableOnUpdate {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraTableOnUpdateTwoKey());
        execs.add(new InfraTableOnUpdateMultikeyWArrayOneArray());
        execs.add(new InfraTableOnUpdateMultikeyWArrayTwoArray());
        execs.add(new InfraTableOnUpdateMultikeyWArrayTwoArrayNonGetter());
        return execs;
    }

    private static class InfraTableOnUpdateMultikeyWArrayTwoArrayNonGetter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('tbl') create table MyTable(k1 int[primitive] primary key, k2 int[primitive] primary key, v int);\n" +
                "on SupportBean_S0 update MyTable set v = id where k1 = toIntArray(p00) and k2 = toIntArray(p01);\n" +
                "on SupportEventWithManyArray(id like 'I%') insert into MyTable select intOne as k1, intTwo as k2, value as v;\n";
            env.compileDeploy(epl);

            sendManyArray(env, "I1", new int[] {1}, new int[] {1, 2}, 10);
            sendManyArray(env, "I2", new int[] {1}, new int[] {1, 2, 3}, 20);
            sendManyArray(env, "I3", new int[] {2}, new int[] {1}, 30);
            sendS0(env, "1", "1, 2, 3", 21);

            env.milestone(0);

            sendS0(env, "1", "1,2", 11);
            sendS0(env, "2", "1", 31);
            sendS0(env, "1", "1", 99);
            sendS0(env, "2", "1, 2", 99);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("tbl"), "k1,k2,v".split(","), new Object[][] {
                {new int[] {1}, new int[] {1, 2}, 11},
                {new int[] {1}, new int[] {1, 2, 3}, 21},
                {new int[] {2}, new int[] {1}, 31}});

            env.undeployAll();
        }

        private void sendS0(RegressionEnvironment env, String p00, String p01, int id) {
            env.sendEventBean(new SupportBean_S0(id, p00, p01));
        }

        private void sendManyArray(RegressionEnvironment env, String id, int[] intOne, int[] intTwo, int value) {
            env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(intOne).withIntTwo(intTwo).withValue(value));
        }
    }

    private static class InfraTableOnUpdateMultikeyWArrayTwoArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('tbl') create table MyTable(k1 int[primitive] primary key, k2 int[primitive] primary key, v int);\n" +
                "on SupportEventWithManyArray(id like 'U%') update MyTable set v = value where k1 = intOne and k2 = intTwo;\n" +
                "on SupportEventWithManyArray(id like 'I%') insert into MyTable select intOne as k1, intTwo as k2, value as v;\n";
            env.compileDeploy(epl);

            sendManyArray(env, "I1", new int[] {1}, new int[] {1, 2}, 10);
            sendManyArray(env, "I2", new int[] {1}, new int[] {1, 2, 3}, 20);
            sendManyArray(env, "I3", new int[] {2}, new int[] {1}, 30);
            sendManyArray(env, "U2", new int[] {1}, new int[] {1, 2, 3}, 21);

            env.milestone(0);

            sendManyArray(env, "U1", new int[] {1}, new int[] {1, 2}, 11);
            sendManyArray(env, "U3", new int[] {2}, new int[] {1}, 31);
            sendManyArray(env, "U4", new int[] {1}, new int[] {1}, 99);
            sendManyArray(env, "U5", new int[] {2}, new int[] {1, 2}, 99);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("tbl"), "k1,k2,v".split(","), new Object[][] {
                    {new int[] {1}, new int[] {1, 2}, 11},
                    {new int[] {1}, new int[] {1, 2, 3}, 21},
                    {new int[] {2}, new int[] {1}, 31}});

            env.undeployAll();
        }

        private void sendManyArray(RegressionEnvironment env, String id, int[] intOne, int[] intTwo, int value) {
            env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(intOne).withIntTwo(intTwo).withValue(value));
        }
    }

    private static class InfraTableOnUpdateMultikeyWArrayOneArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('tbl') create table MyTable(k1 int[primitive] primary key, v int);\n" +
                "on SupportEventWithManyArray(id like 'U%') update MyTable set v = value where k1 = intOne;\n" +
                "on SupportEventWithManyArray(id like 'I%') insert into MyTable select intOne as k1, value as v;\n";
            env.compileDeploy(epl);

            sendManyArray(env, "I1", new int[] {1, 2}, 10);
            sendManyArray(env, "I2", new int[] {1, 2, 3}, 20);
            sendManyArray(env, "I3", new int[] {1}, 30);
            sendManyArray(env, "U2", new int[] {1, 2, 3}, 21);

            env.milestone(0);

            sendManyArray(env, "U1", new int[] {1, 2}, 11);
            sendManyArray(env, "U3", new int[] {1}, 31);
            sendManyArray(env, "U4", new int[] {}, 99);
            sendManyArray(env, "U5", new int[] {1, 2, 4}, 99);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("tbl"), "k1,v".split(","),
                new Object[][] {{new int[] {1, 2}, 11}, {new int[] {1, 2, 3}, 21}, {new int[] {1}, 31}});

            env.undeployAll();
        }

        private void sendManyArray(RegressionEnvironment env, String id, int[] ints, int value) {
            env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(ints).withValue(value));
        }
    }

    private static class InfraTableOnUpdateTwoKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "keyOne,keyTwo,p0".split(",");
            RegressionPath path = new RegressionPath();

            env.compileDeploy("create table varagg as (" +
                "keyOne string primary key, keyTwo int primary key, p0 long)", path);
            env.compileDeploy("on SupportBean merge varagg where theString = keyOne and " +
                "intPrimitive = keyTwo when not matched then insert select theString as keyOne, intPrimitive as keyTwo, 1 as p0", path);
            env.compileDeploy("@name('s0') select varagg[p00, id].p0 as value from SupportBean_S0", path).addListener("s0");
            env.compileDeploy("@name('update') on SupportTwoKeyEvent update varagg set p0 = newValue " +
                "where k1 = keyOne and k2 = keyTwo", path).addListener("update");

            Object[][] expectedType = new Object[][]{{"keyOne", String.class}, {"keyTwo", Integer.class}, {"p0", Long.class}};
            EventType updateStmtEventType = env.statement("update").getEventType();
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, updateStmtEventType, SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            env.sendEventBean(new SupportBean("G1", 10));
            assertValues(env, new Object[][]{{"G1", 10}}, new Long[]{1L});

            env.milestone(0);

            env.sendEventBean(new SupportTwoKeyEvent("G1", 10, 2));
            assertValues(env, new Object[][]{{"G1", 10}}, new Long[]{2L});
            EPAssertionUtil.assertProps(env.listener("update").getLastNewData()[0], fields, new Object[]{"G1", 10, 2L});
            EPAssertionUtil.assertProps(env.listener("update").getAndResetLastOldData()[0], fields, new Object[]{"G1", 10, 1L});

            // try property method invocation
            env.compileDeploy("create table MyTableSuppBean as (sb SupportBean)", path);
            env.compileDeploy("on SupportBean_S0 update MyTableSuppBean sb set sb.setLongPrimitive(10)", path);
            env.undeployAll();
        }
    }

    private static void assertValues(RegressionEnvironment env, Object[][] keys, Long[] values) {
        assertEquals(keys.length, values.length);
        for (int i = 0; i < keys.length; i++) {
            env.sendEventBean(new SupportBean_S0((Integer) keys[i][1], (String) keys[i][0]));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("Failed for key '" + Arrays.toString(keys[i]) + "'", values[i], event.get("value"));
        }
    }

    public static int[] toIntArray(String text) {
        String[] split = text.split(",");
        int[] ints = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            ints[i] = Integer.parseInt(split[i].trim());
        }
        return ints;
    }
}
