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
package com.espertech.esper.regressionlib.suite.infra.nwtable;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQuery;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InfraNWTableFAFIndexPerfWNoQueryPlanLog implements IndexBackingTableInfo {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();

        execs.add(new InfraFAFKeyBTreePerformance(true));
        execs.add(new InfraFAFKeyBTreePerformance(false));
        execs.add(new InfraFAFKeyAndRangePerformance(true));
        execs.add(new InfraFAFKeyAndRangePerformance(false));
        execs.add(new InfraFAFRangePerformance(true));
        execs.add(new InfraFAFRangePerformance(false));
        execs.add(new InfraFAFKeyPerformance(true));
        execs.add(new InfraFAFKeyPerformance(false));
        execs.add(new InfraFAFInKeywordSingleIndex(true));
        execs.add(new InfraFAFInKeywordSingleIndex(false));

        return execs;
    }

    private static class InfraFAFKeyBTreePerformance implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        private final boolean namedWindow;

        public InfraFAFKeyBTreePerformance(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            // create window one
            String eplCreate = namedWindow ?
                "create window MyInfraFAFKB#keepall as SupportBean" :
                "create table MyInfraFAFKB (theString string primary key, intPrimitive int primary key)";
            env.compileDeploy(eplCreate, path);
            env.compileDeploy("insert into MyInfraFAFKB select theString, intPrimitive from SupportBean", path);
            env.compileDeploy("@name('idx') create index idx1 on MyInfraFAFKB(intPrimitive btree)", path);

            // insert X rows
            int maxRows = 10000;   //for performance testing change to int maxRows = 100000;
            for (int i = 0; i < maxRows; i++) {
                env.sendEventBean(new SupportBean("A", i));
            }
            env.sendEventBean(new SupportBean("B", 100));

            // fire single-key queries
            String eplIdx1One = "select intPrimitive as sumi from MyInfraFAFKB where intPrimitive = 5501";
            runFAFAssertion(env, path, eplIdx1One, 5501);

            String eplIdx1Two = "select sum(intPrimitive) as sumi from MyInfraFAFKB where intPrimitive > 9997";
            runFAFAssertion(env, path, eplIdx1Two, 9998 + 9999);

            // drop index, create multikey btree
            env.undeployModuleContaining("idx");

            env.compileDeploy("create index idx2 on MyInfraFAFKB(intPrimitive btree, theString btree)", path);

            String eplIdx2One = "select intPrimitive as sumi from MyInfraFAFKB where intPrimitive = 5501 and theString = 'A'";
            runFAFAssertion(env, path, eplIdx2One, 5501);

            String eplIdx2Two = "select sum(intPrimitive) as sumi from MyInfraFAFKB where intPrimitive in [5000:5004) and theString = 'A'";
            runFAFAssertion(env, path, eplIdx2Two, 5000 + 5001 + 5003 + 5002);

            String eplIdx2Three = "select sum(intPrimitive) as sumi from MyInfraFAFKB where intPrimitive=5001 and theString between 'A' and 'B'";
            runFAFAssertion(env, path, eplIdx2Three, 5001);

            env.undeployAll();
        }
    }

    private static class InfraFAFKeyAndRangePerformance implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        private final boolean namedWindow;

        public InfraFAFKeyAndRangePerformance(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            // create window one
            String eplCreate = namedWindow ?
                "create window MyInfraFAFKR#keepall as SupportBean" :
                "create table MyInfraFAFKR (theString string primary key, intPrimitive int primary key)";
            env.compileDeploy(eplCreate, path);
            env.compileDeploy("insert into MyInfraFAFKR select theString, intPrimitive from SupportBean", path);
            env.compileDeploy("create index idx1 on MyInfraFAFKR(theString hash, intPrimitive btree)", path);

            // insert X rows
            int maxRows = 10000;   //for performance testing change to int maxRows = 100000;
            for (int i = 0; i < maxRows; i++) {
                env.sendEventBean(new SupportBean("A", i));
            }

            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive not in [3:9997]", 1 + 2 + 9998 + 9999);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive not in [3:9997)", 1 + 2 + 9997 + 9998 + 9999);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive not in (3:9997]", 1 + 2 + 3 + 9998 + 9999);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive not in (3:9997)", 1 + 2 + 3 + 9997 + 9998 + 9999);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'B' and intPrimitive not in (3:9997)", null);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive between 200 and 202", 603);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive between 202 and 199", 199 + 200 + 201 + 202);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive >= 200 and intPrimitive <= 202", 603);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive >= 202 and intPrimitive <= 200", null);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive > 9997", 9998 + 9999);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive >= 9997", 9997 + 9998 + 9999);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive < 5", 4 + 3 + 2 + 1);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive <= 5", 5 + 4 + 3 + 2 + 1);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive in [200:202]", 603);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive in [200:202)", 401);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive in (200:202]", 403);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive in (200:202)", 201);

            // test no value returned
            EPFireAndForgetPreparedQuery query = prepare(env, path, "select * from MyInfraFAFKR where theString = 'A' and intPrimitive < 0");
            EPFireAndForgetQueryResult result = query.execute();
            assertEquals(0, result.getArray().length);

            env.undeployAll();
        }
    }

    private static class InfraFAFRangePerformance implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        private final boolean namedWindow;

        public InfraFAFRangePerformance(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            // create window one
            String eplCreate = namedWindow ?
                "create window MyInfraRP#keepall as SupportBean" :
                "create table MyInfraRP (theString string primary key, intPrimitive int primary key)";
            env.compileDeploy(eplCreate, path);
            env.compileDeploy("insert into MyInfraRP select theString, intPrimitive from SupportBean", path);
            env.compileDeploy("create index idx1 on MyInfraRP(intPrimitive btree)", path);

            // insert X rows
            int maxRows = 10000;   //for performance testing change to int maxRows = 100000;
            for (int i = 0; i < maxRows; i++) {
                env.sendEventBean(new SupportBean("K", i));
            }

            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive between 200 and 202", 603);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive between 202 and 199", 199 + 200 + 201 + 202);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive >= 200 and intPrimitive <= 202", 603);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive >= 202 and intPrimitive <= 200", null);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive > 9997", 9998 + 9999);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive >= 9997", 9997 + 9998 + 9999);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive < 5", 4 + 3 + 2 + 1);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive <= 5", 5 + 4 + 3 + 2 + 1);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive in [200:202]", 603);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive in [200:202)", 401);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive in (200:202]", 403);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive in (200:202)", 201);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive not in [3:9997]", 1 + 2 + 9998 + 9999);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive not in [3:9997)", 1 + 2 + 9997 + 9998 + 9999);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive not in (3:9997]", 1 + 2 + 3 + 9998 + 9999);
            runFAFAssertion(env, path, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive not in (3:9997)", 1 + 2 + 3 + 9997 + 9998 + 9999);

            // test no value returned
            EPFireAndForgetPreparedQuery query = prepare(env, path, "select * from MyInfraRP where intPrimitive < 0");
            EPFireAndForgetQueryResult result = query.execute();
            assertEquals(0, result.getArray().length);

            env.undeployAll();
        }
    }

    private static class InfraFAFKeyPerformance implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        private final boolean namedWindow;

        public InfraFAFKeyPerformance(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            // create window one
            RegressionPath path = new RegressionPath();
            String stmtTextCreateOne = namedWindow ?
                "create window MyInfraOne#keepall as (f1 string, f2 int)" :
                "create table MyInfraOne (f1 string primary key, f2 int primary key)";
            env.compileDeploy(stmtTextCreateOne, path);
            env.compileDeploy("insert into MyInfraOne(f1, f2) select theString, intPrimitive from SupportBean", path);
            env.compileDeploy("create index MyInfraOneIndex on MyInfraOne(f1)", path);

            // insert X rows
            int maxRows = 100;   //for performance testing change to int maxRows = 100000;
            for (int i = 0; i < maxRows; i++) {
                env.sendEventBean(new SupportBean("K" + i, i));
            }
            long start;
            String queryText;
            EPFireAndForgetPreparedQuery query;
            EPFireAndForgetQueryResult result;

            // fire N queries each returning 1 row
            start = System.currentTimeMillis();
            queryText = "select * from MyInfraOne where f1='K10'";
            query = prepare(env, path, queryText);
            int loops = 10000;

            for (int i = 0; i < loops; i++) {
                result = query.execute();
                assertEquals(1, result.getArray().length);
                assertEquals("K10", result.getArray()[0].get("f1"));
            }
            long end = System.currentTimeMillis();
            long delta = end - start;
            assertTrue("delta=" + delta, delta < 500);

            // test no value returned
            queryText = "select * from MyInfraOne where f1='KX'";
            query = prepare(env, path, queryText);
            result = query.execute();
            assertEquals(0, result.getArray().length);

            // test query null
            queryText = "select * from MyInfraOne where f1=null";
            query = prepare(env, path, queryText);
            result = query.execute();
            assertEquals(0, result.getArray().length);

            // insert null and test null
            env.sendEventBean(new SupportBean(null, -2));
            result = query.execute();
            assertEquals(0, result.getArray().length);

            // test two values
            env.sendEventBean(new SupportBean(null, -1));
            query = prepare(env, path, "select * from MyInfraOne where f1 is null order by f2 asc");
            result = query.execute();
            assertEquals(2, result.getArray().length);
            assertEquals(-2, result.getArray()[0].get("f2"));
            assertEquals(-1, result.getArray()[1].get("f2"));

            env.undeployAll();
        }
    }

    private static class InfraFAFInKeywordSingleIndex implements RegressionExecution {
        private final boolean namedWindow;

        public InfraFAFInKeywordSingleIndex(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCreate = namedWindow ?
                "create window MyInfraIKW#keepall as SupportBean" :
                "create table MyInfraIKW (theString string primary key)";
            env.compileDeploy(eplCreate, path);
            env.compileDeploy("create index idx on MyInfraIKW(theString)", path);
            env.compileDeploy("insert into MyInfraIKW select theString from SupportBean", path);

            int eventCount = 10;
            for (int i = 0; i < eventCount; i++) {
                env.sendEventBean(new SupportBean("E" + i, 0));
            }

            InvocationCounter.setCount(0);
            String fafEPL = "select * from MyInfraIKW as mw where justCount(mw) and theString in ('notfound')";
            env.compileExecuteFAF(fafEPL, path);
            assertEquals(0, InvocationCounter.getCount());

            env.undeployAll();
        }
    }

    private static void runFAFAssertion(RegressionEnvironment env, RegressionPath path, String epl, Integer expected) {
        long start = System.currentTimeMillis();
        int loops = 500;

        EPFireAndForgetPreparedQuery query = prepare(env, path, epl);
        for (int i = 0; i < loops; i++) {
            runFAFQuery(query, expected);
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("delta=" + delta, delta < 1500);
    }

    private static void runFAFQuery(EPFireAndForgetPreparedQuery query, Integer expectedValue) {
        EPFireAndForgetQueryResult result = query.execute();
        assertEquals(1, result.getArray().length);
        assertEquals(expectedValue, result.getArray()[0].get("sumi"));
    }

    private static EPFireAndForgetPreparedQuery prepare(RegressionEnvironment env, RegressionPath path, String queryText) {
        EPCompiled compiled = env.compileFAF(queryText, path);
        return env.runtime().getFireAndForgetService().prepareQuery(compiled);
    }

    public static class InvocationCounter {
        private static int count;

        public static void setCount(int count) {
            InvocationCounter.count = count;
        }

        public static int getCount() {
            return count;
        }

        public static boolean justCount(Object o) {
            count++;
            return true;
        }
    }
}
