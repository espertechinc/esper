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

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableSubquery {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraTableSubqueryAgainstKeyed());
        execs.add(new InfraTableSubqueryAgainstUnkeyed());
        execs.add(new InfraTableSubquerySecondaryIndex());
        execs.add(new InfraTableSubqueryInFilter());
        return execs;
    }

    private static class InfraTableSubqueryInFilter implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(tablecol string primary key);\n" +
                "insert into MyTable select p00 as tablecol from SupportBean_S0;\n" +
                "@name('s0') select * from SupportBean(theString=(select tablecol from MyTable).orderBy().firstOf())";
            env.compileDeploy(epl).addListener("s0");

            sendAssert(env, "E", false);
            sendS0(env, "E");
            sendAssert(env, "E", true);
            sendS0(env, "C");
            sendAssert(env, "E", false);
            sendAssert(env, "C", true);

            env.milestone(0);

            sendAssert(env, "A", false);
            sendAssert(env, "C", true);
            sendS0(env, "A");
            sendAssert(env, "A", true);
            sendAssert(env, "C", false);

            env.undeployAll();
        }

        private void sendS0(RegressionEnvironment env, String p00) {
            env.sendEventBean(new SupportBean_S0(0, p00));
        }

        private void sendAssert(RegressionEnvironment env, String theString, boolean expected) {
            env.sendEventBean(new SupportBean(theString, 0));
            env.assertListenerInvokedFlag("s0", expected);
        }
    }

    private static class InfraTableSubqueryAgainstKeyed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            env.compileDeploy("@public create table varagg as (" +
                "key string primary key, total sum(int))", path);
            env.compileDeploy("into table varagg " +
                "select sum(intPrimitive) as total from SupportBean group by theString", path);
            env.compileDeploy("@name('s0') select (select total from varagg where key = s0.p00) as value " +
                "from SupportBean_S0 as s0", path).addListener("s0");

            env.sendEventBean(new SupportBean("G2", 200));
            assertValues(env, "G1,G2", new Integer[]{null, 200});

            env.milestone(0);

            env.sendEventBean(new SupportBean("G1", 100));
            assertValues(env, "G1,G2", new Integer[]{100, 200});

            env.undeployAll();
        }
    }

    private static class InfraTableSubqueryAgainstUnkeyed implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            env.compileDeploy("@public create table InfraOne (string string, intPrimitive int)", path);
            env.compileDeploy("@name('s0') select (select intPrimitive from InfraOne where string = s0.p00) as c0 from SupportBean_S0 as s0", path).addListener("s0");
            env.compileDeploy("insert into InfraOne select theString as string, intPrimitive from SupportBean", path);

            env.sendEventBean(new SupportBean("E1", 10));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(0, "E1"));
            env.assertPropsNew("s0", "c0".split(","), new Object[]{10});

            env.undeployAll();
        }
    }

    private static class InfraTableSubquerySecondaryIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            String eplTable = "@public create table MyTable(k0 string primary key, k1 string primary key, p2 string, value int)";
            env.compileDeploy(eplTable, path);

            String eplIndex = "create index MyIndex on MyTable(p2)";
            env.compileDeploy(eplIndex, path);

            String eplInto = "on SupportBean_S0 merge MyTable " +
                "where p00 = k0 and p01 = k1 " +
                "when not matched then insert select p00 as k0, p01 as k1, p02 as p2, id as value " +
                "when matched then update set p2 = p02, value = id ";
            env.compileDeploy(eplInto, path);

            String eplSubselect = "@Name('s0') select (select value from MyTable as tbl where sb.theString = tbl.p2) as c0 from SupportBean as sb";
            env.compileDeploy(eplSubselect, path).addListener("s0");

            sendInsertUpdate(env, "G1", "SG1", "P2_1", 10);
            assertSubselect(env, "P2_1", 10);

            env.milestone(0);

            sendInsertUpdate(env, "G1", "SG1", "P2_2", 11);

            env.milestone(1);

            assertSubselect(env, "P2_1", null);
            assertSubselect(env, "P2_2", 11);

            env.undeployAll();
        }
    }

    private static void assertValues(RegressionEnvironment env, String keys, Integer[] values) {
        String[] keyarr = keys.split(",");
        for (int i = 0; i < keyarr.length; i++) {
            env.sendEventBean(new SupportBean_S0(0, keyarr[i]));
            final int index = i;
            env.assertEventNew("s0", event -> assertEquals("Failed for key '" + keyarr[index] + "'", values[index], event.get("value")));
        }
    }

    private static void sendInsertUpdate(RegressionEnvironment env, String p00, String p01, String p02, int value) {
        env.sendEventBean(new SupportBean_S0(value, p00, p01, p02));
    }

    private static void assertSubselect(RegressionEnvironment env, String string, Integer expectedSum) {
        String[] fields = "c0".split(",");
        env.sendEventBean(new SupportBean(string, -1));
        env.assertPropsNew("s0", fields, new Object[]{expectedSum});
    }
}
