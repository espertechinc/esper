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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableOnDelete {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraDeleteFlow());
        execs.add(new InfraDeleteSecondaryIndexUpd());
        return execs;
    }

    private static class InfraDeleteSecondaryIndexUpd implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table MyTable as (pkey0 string primary key, " +
                "pkey1 int primary key, thesum sum(long))", path);
            env.compileDeploy("into table MyTable select sum(longPrimitive) as thesum from SupportBean group by theString, intPrimitive", path);

            makeSendSupportBean(env, "E1", 10, 2L);
            makeSendSupportBean(env, "E2", 20, 3L);

            env.milestone(0);

            makeSendSupportBean(env, "E1", 11, 4L);
            makeSendSupportBean(env, "E2", 21, 5L);

            env.compileDeploy("create index MyIdx on MyTable(pkey0)", path);
            env.compileDeploy("@name('s0') on SupportBean_S0 select sum(thesum) as c0 from MyTable where pkey0=p00", path).addListener("s0");

            assertSum(env, "E1,E2,E3", new Long[]{6L, 8L, null});

            makeSendSupportBean(env, "E3", 30, 77L);
            makeSendSupportBean(env, "E2", 21, 2L);

            assertSum(env, "E1,E2,E3", new Long[]{6L, 10L, 77L});

            env.compileDeploy("@name('on-delete') on SupportBean_S1 delete from MyTable where pkey0=p10 and pkey1=id", path);

            env.sendEventBean(new SupportBean_S1(11, "E1"));   // deletes {"E1", 11, 4L}
            assertSum(env, "E1,E2,E3", new Long[]{2L, 10L, 77L});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S1(20, "E2"));   // deletes {"E2", 20, 3L}
            assertSum(env, "E1,E2,E3", new Long[]{2L, 7L, 77L});

            env.undeployAll();
        }

        private static void assertSum(RegressionEnvironment env, String listOfP00, Long[] sums) {
            String[] p00s = listOfP00.split(",");
            assertEquals(p00s.length, sums.length);
            for (int i = 0; i < p00s.length; i++) {
                env.sendEventBean(new SupportBean_S0(0, p00s[i]));
                assertEquals(sums[i], env.listener("s0").assertOneGetNewAndReset().get("c0"));
            }
        }
    }

    private static class InfraDeleteFlow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String[] fields = "key,thesum".split(",");
            env.compileDeploy("create table varagg as (key string primary key, thesum sum(int))", path);
            env.compileDeploy("into table varagg select sum(intPrimitive) as thesum from SupportBean group by theString", path);
            env.compileDeploy("@name('s0') select varagg[p00].thesum as value from SupportBean_S0", path).addListener("s0");
            env.compileDeploy("@name('sdf') on SupportBean_S1(id = 1) delete from varagg where key = p10", path).addListener("sdf");
            env.compileDeploy("@name('sda') on SupportBean_S1(id = 2) delete from varagg", path).addListener("sda");

            Object[][] expectedType = new Object[][]{{"key", String.class}, {"thesum", Integer.class}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, env.statement("sda").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            env.sendEventBean(new SupportBean("G1", 10));
            assertValues(env, "G1,G2", new Integer[]{10, null});

            env.sendEventBean(new SupportBean("G2", 20));
            assertValues(env, "G1,G2", new Integer[]{10, 20});

            env.sendEventBean(new SupportBean_S1(1, "G1"));
            assertValues(env, "G1,G2", new Integer[]{null, 20});
            EPAssertionUtil.assertProps(env.listener("sdf").assertOneGetNewAndReset(), fields, new Object[]{"G1", 10});

            env.milestone(0);

            env.sendEventBean(new SupportBean_S1(2, null));
            assertValues(env, "G1,G2", new Integer[]{null, null});
            EPAssertionUtil.assertProps(env.listener("sda").assertOneGetNewAndReset(), fields, new Object[]{"G2", 20});

            env.undeployAll();
        }
    }

    private static void assertValues(RegressionEnvironment env, String keys, Integer[] values) {
        String[] keyarr = keys.split(",");
        assertEquals(keyarr.length, values.length);
        for (int i = 0; i < keyarr.length; i++) {
            env.sendEventBean(new SupportBean_S0(0, keyarr[i]));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("Failed for key '" + keyarr[i] + "'", values[i], event.get("value"));
        }
    }

    private static void makeSendSupportBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        b.setLongPrimitive(longPrimitive);
        env.sendEventBean(b);
    }
}
