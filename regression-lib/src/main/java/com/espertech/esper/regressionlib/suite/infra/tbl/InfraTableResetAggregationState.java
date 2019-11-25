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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;

public class InfraTableResetAggregationState {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraTableResetRowSum());
        execs.add(new InfraTableResetRowSumWTableAlias());
        execs.add(new InfraTableResetSelective());
        execs.add(new InfraTableResetVariousAggs());
        execs.add(new InfraTableResetInvalid());
        execs.add(new InfraTableResetDocSample());
        return execs;
    }

    private static class InfraTableResetDocSample implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table IntrusionCountTable (\n" +
                "  fromAddress string primary key,\n" +
                "  toAddress string primary key,\n" +
                "  countIntrusion10Sec count(*),\n" +
                "  countIntrusion60Sec count(*)\n" +
                ");\n" +
                "create schema IntrusionReset(fromAddress string, toAddress string);\n" +
                "on IntrusionReset as resetEvent merge IntrusionCountTable as tableRow\n" +
                "where resetEvent.fromAddress = tableRow.fromAddress and resetEvent.toAddress = tableRow.toAddress\n" +
                "when matched then update set countIntrusion10Sec.reset(), countIntrusion60Sec.reset();\n" +
                "" +
                "on IntrusionReset as resetEvent merge IntrusionCountTable as tableRow\n" +
                "where resetEvent.fromAddress = tableRow.fromAddress and resetEvent.toAddress = tableRow.toAddress\n" +
                "when matched then update set tableRow.reset();\n";
            env.compile(epl);
        }
    }

    private static class InfraTableResetInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String prefix = "@name('table') create table MyTable(asum sum(int));\n";

            String invalidSelectAggReset = prefix + "on SupportBean_S0 merge MyTable when matched then insert into MyStream select asum.reset()";
            tryInvalidCompile(env, invalidSelectAggReset, "Failed to validate select-clause expression 'asum.reset()': The table aggregation'reset' method is only available for the on-merge update action");

            String invalidSelectRowReset = prefix + "on SupportBean_S0 merge MyTable as mt when matched then insert into MyStream select mt.reset()";
            tryInvalidCompile(env, invalidSelectRowReset, "Failed to validate select-clause expression 'mt.reset()'");

            String invalidAggResetWParams = prefix + "on SupportBean_S0 merge MyTable as mt when matched then update set asum.reset(1)";
            tryInvalidCompile(env, invalidAggResetWParams,
                "Failed to validate update assignment expression 'asum.reset(1)': The table aggregation 'reset' method does not allow parameters");

            String invalidRowResetWParams = prefix + "on SupportBean_S0 merge MyTable as mt when matched then update set mt.reset(1)";
            tryInvalidCompile(env, invalidRowResetWParams,
                "Failed to validate update assignment expression 'mt.reset(1)': The table aggregation 'reset' method does not allow parameters");
        }
    }

    private static class InfraTableResetVariousAggs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('table') create table MyTable(" +
                "  myAvedev avedev(int),\n" +
                "  myCount count(*),\n" +
                "  myCountDistinct count(distinct int),\n" +
                "  myMax max(int),\n" +
                "  myMedian median(int),\n" +
                "  myStddev stddev(int),\n" +
                "  myFirstEver firstever(string),\n" +
                "  myCountEver countever(*)," +
                "  myMaxByEver maxbyever(intPrimitive) @type(SupportBean)," +
                "  myPluginAggSingle myaggsingle(*)," +
                "  myPluginAggAccess referenceCountedMap(string)," +
                "  myWordcms countMinSketch()" +
                ");\n" +
                "into table MyTable select" +
                "  avedev(intPrimitive) as myAvedev," +
                "  count(*) as myCount," +
                "  count(distinct intPrimitive) as myCountDistinct," +
                "  max(intPrimitive) as myMax," +
                "  median(intPrimitive) as myMedian," +
                "  stddev(intPrimitive) as myStddev," +
                "  firstever(theString) as myFirstEver," +
                "  countever(*) as myCountEver," +
                "  maxbyever(*) as myMaxByEver," +
                "  myaggsingle(*) as myPluginAggSingle," +
                "  referenceCountedMap(theString) as myPluginAggAccess," +
                "  countMinSketchAdd(theString) as myWordcms" +
                "   " +
                "from SupportBean#keepall;\n" +
                "on SupportBean_S0 merge MyTable mt when matched then update set mt.reset();\n" +
                "@name('s0') select MyTable.myWordcms.countMinSketchFrequency(p10) as c0 from SupportBean_S1;\n";
            env.compileDeploy(epl).addListener("s0");
            String[] fieldSetOne = "myAvedev,myCount,myCountDistinct,myMax,myMedian,myStddev,myFirstEver,myCountEver,myMaxByEver".split(",");

            sendEventSetAssert(env, fieldSetOne);

            env.milestone(0);

            sendResetAssert(env, fieldSetOne);

            env.milestone(1);

            sendEventSetAssert(env, fieldSetOne);

            env.undeployAll();
        }

        private void sendEventSetAssert(RegressionEnvironment env, String[] fieldSetOne) {
            sendBean(env, "E1", 10);
            sendBean(env, "E2", 10);
            SupportBean e3 = sendBean(env, "E3", 30);

            EventBean row = env.iterator("table").next();
            EPAssertionUtil.assertProps(row, fieldSetOne,
                new Object[] {8.88888888888889d, 3L, 2L, 30, 10.0, 11.547005383792515d, "E1", 3L, e3});
            assertEquals(-3, row.get("myPluginAggSingle"));
            assertEquals(3, ((Map) row.get("myPluginAggAccess")).size());

            assertCountMinSketch(env, "E1", 1);
        }

        private void assertCountMinSketch(RegressionEnvironment env, String theString, long expected) {
            env.sendEventBean(new SupportBean_S1(0, theString));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }

        private void sendResetAssert(RegressionEnvironment env, String[] fieldSetOne) {
            env.sendEventBean(new SupportBean_S0(0));
            EventBean row = env.iterator("table").next();
            EPAssertionUtil.assertProps(row, fieldSetOne,
                new Object[] {null, 0L, 0L, null, null, null, null, 0L, null});
            assertEquals(0, row.get("myPluginAggSingle"));
            assertEquals(0, ((Map) row.get("myPluginAggAccess")).size());

            assertCountMinSketch(env, "E1", 0);
        }
    }

    private static class InfraTableResetSelective implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('table') create table MyTable(k string primary key, " +
                "  avgone avg(int), avgtwo avg(int)," +
                "  winone window(*) @type(SupportBean), wintwo window(*) @type(SupportBean)" +
                ");\n" +
                "into table MyTable select theString, " +
                "  avg(intPrimitive) as avgone, avg(intPrimitive) as avgtwo," +
                "  window(*) as winone, window(*) as wintwo " +
                "from SupportBean#keepall group by theString;\n" +
                "on SupportBean_S0 merge MyTable where p00 = k  when matched then update set avgone.reset(), winone.reset();\n" +
                "on SupportBean_S1 merge MyTable where p10 = k  when matched then update set avgtwo.reset(), wintwo.reset();\n";
            env.compileDeploy(epl);
            String[] propertyNames = "k,avgone,avgtwo,winone,wintwo".split(",");

            SupportBean s0 = sendBean(env, "G1", 1);
            SupportBean s1 = sendBean(env, "G2", 10);
            SupportBean s2 = sendBean(env, "G2", 2);
            SupportBean s3 = sendBean(env, "G1", 20);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("table"), propertyNames, new Object[][] {
                {"G1", 10.5d, 10.5d, new SupportBean[] {s0, s3}, new SupportBean[] {s0, s3}},
                {"G2", 6d, 6d, new SupportBean[] {s1, s2}, new SupportBean[] {s1, s2}}
                });

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(0, "G2"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("table"), propertyNames, new Object[][] {
                {"G1", 10.5d, 10.5d, new SupportBean[] {s0, s3}, new SupportBean[] {s0, s3}},
                {"G2", null, 6d, null, new SupportBean[] {s1, s2}}
            });

            env.milestone(1);

            env.sendEventBean(new SupportBean_S1(0, "G1"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("table"), propertyNames, new Object[][] {
                {"G1", 10.5d, null, new SupportBean[] {s0, s3}, null},
                {"G2", null, 6d, null, new SupportBean[] {s1, s2}}
            });

            env.undeployAll();
        }
    }

    private static class InfraTableResetRowSumWTableAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionReset(env, "on SupportBean_S0 merge MyTable as mt when matched then update set mt.reset();\n");
        }
    }

    private static class InfraTableResetRowSum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionReset(env, "on SupportBean_S0 merge MyTable when matched then update set asum.reset();\n");
        }
    }

    private static void runAssertionReset(RegressionEnvironment env, String onMerge) {
        String epl = "@name('table') create table MyTable(asum sum(int));\n" +
            "into table MyTable select sum(intPrimitive) as asum from SupportBean;\n" +
            onMerge;
        env.compileDeploy(epl);

        sendBeanAssertSum(env, 10, 10);
        sendBeanAssertSum(env, 11, 21);
        sendResetAssertSum(env);

        env.milestone(0);

        assertTableSum(env, null);
        sendBeanAssertSum(env, 20, 20);
        sendBeanAssertSum(env, 21, 41);
        sendResetAssertSum(env);

        env.milestone(1);

        sendBeanAssertSum(env, 30, 30);

        env.undeployAll();
    }

    private static SupportBean sendBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        env.sendEventBean(sb);
        return sb;
    }

    private static void sendBeanAssertSum(RegressionEnvironment env, int intPrimitive, int expected) {
        env.sendEventBean(new SupportBean("E1", intPrimitive));
        assertTableSum(env, expected);
    }

    private static void sendResetAssertSum(RegressionEnvironment env) {
        env.sendEventBean(new SupportBean_S0(0));
        assertTableSum(env, null);
    }

    private static void assertTableSum(RegressionEnvironment env, Integer expected) {
        assertEquals(expected, env.iterator("table").next().get("asum"));
    }
}
