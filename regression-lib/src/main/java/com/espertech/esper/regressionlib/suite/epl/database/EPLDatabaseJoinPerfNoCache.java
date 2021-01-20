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
package com.espertech.esper.regressionlib.suite.epl.database;

import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;

import java.util.EnumSet;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertTrue;

public class EPLDatabaseJoinPerfNoCache implements RegressionExecution {
    @Override
    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(RegressionFlag.EXCLUDEWHENINSTRUMENTED);
    }

    public void run(RegressionEnvironment env) {
        runAssertion100EventsRetained(env, "MyDBWithRetain");
        runAssertion100EventsPooled(env, "MyDBPooled");
        runAssertionSelectRStream(env, "MyDBWithRetain");
        runAssertionSelectIStream(env, "MyDBWithRetain");
        runAssertionWhereClauseNoIndexNoCache(env, "MyDBWithRetain");
    }

    private static void runAssertion100EventsRetained(RegressionEnvironment env, String dbname) {
        long startTime = System.currentTimeMillis();
        try100Events(env, dbname);
        long endTime = System.currentTimeMillis();
        // log.info(".test100EventsRetained delta=" + (endTime - startTime));
        assertTrue(endTime - startTime < 5000);
    }

    private static void runAssertion100EventsPooled(RegressionEnvironment env, String dbname) {
        long startTime = System.currentTimeMillis();
        try100Events(env, dbname);
        long endTime = System.currentTimeMillis();
        // log.info(".test100EventsPooled delta=" + (endTime - startTime));
        assertTrue(endTime - startTime < 10000);
    }

    private static void runAssertionSelectRStream(RegressionEnvironment env, String dbname) {
        String stmtText = "@name('s0') select rstream myvarchar from " +
            "SupportBean_S0#length(1000) as s0," +
            " sql:" + dbname + "['select myvarchar from mytesttable where ${id} = mytesttable.mybigint'] as s1";
        env.compileDeploy(stmtText).addListener("s0");

        // 1000 events should enter the window fast, no joins
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            SupportBean_S0 bean = new SupportBean_S0(10);
            env.sendEventBean(bean);
            env.assertListenerNotInvoked("s0");
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        assertTrue("delta=" + delta, endTime - startTime < 1000);

        // 1001st event should finally join and produce a result
        SupportBean_S0 bean = new SupportBean_S0(10);
        env.sendEventBean(bean);
        env.assertEqualsNew("s0", "myvarchar", "J");

        env.undeployAll();
    }

    private static void runAssertionSelectIStream(RegressionEnvironment env, String dbname) {
        // set time to zero
        env.advanceTime(0);

        String stmtText = "@name('s0') select istream myvarchar from " +
            "SupportBean_S0#time(1 sec) as s0," +
            " sql:" + dbname + " ['select myvarchar from mytesttable where ${id} = mytesttable.mybigint'] as s1";
        env.compileDeploy(stmtText).addListener("s0");

        // Send 100 events which all fireStatementStopped a join
        for (int i = 0; i < 100; i++) {
            SupportBean_S0 bean = new SupportBean_S0(5);
            env.sendEventBean(bean);
            env.assertEqualsNew("s0", "myvarchar", "E");
        }

        // now advance the time, this should not produce events or join
        long startTime = System.currentTimeMillis();
        env.advanceTime(2000);
        long endTime = System.currentTimeMillis();

        // log.info(".testSelectIStream delta=" + (endTime - startTime));
        assertTrue(endTime - startTime < 200);
        env.assertListenerNotInvoked("s0");

        env.undeployAll();
    }

    private static void runAssertionWhereClauseNoIndexNoCache(RegressionEnvironment env, String dbname) {
        String stmtText = "@name('s0') select id, mycol3, mycol2 from " +
            "SupportBean_S0#keepall as s0," +
            " sql:" + dbname + "['select mycol3, mycol2 from mytesttable_large'] as s1 where s0.id = s1.mycol3";
        env.compileDeploy(stmtText).addListener("s0");

        for (int i = 0; i < 20; i++) {
            int num = i + 1;
            String col2 = Integer.toString(Math.round((float) num / 10));
            SupportBean_S0 bean = new SupportBean_S0(num);
            env.sendEventBean(bean);
            env.assertPropsNew("s0", new String[]{"id", "mycol3", "mycol2"}, new Object[]{num, num, col2});
        }

        env.undeployAll();
    }

    private static void try100Events(RegressionEnvironment env, String dbname) {
        String stmtText = "@name('s0') select myint from " +
            "SupportBean_S0 as s0," +
            " sql:" + dbname + " ['select myint from mytesttable where ${id} = mytesttable.mybigint'] as s1";
        env.compileDeploy(stmtText).addListener("s0");

        for (int i = 0; i < 100; i++) {
            int id = i % 10 + 1;

            SupportBean_S0 bean = new SupportBean_S0(id);
            env.sendEventBean(bean);

            env.assertEqualsNew("s0", "myint", id * 10);
        }

        env.undeployAll();
    }
}
