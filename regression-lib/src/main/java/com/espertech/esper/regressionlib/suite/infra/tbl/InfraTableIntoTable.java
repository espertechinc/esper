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
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportBeanNumeric;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;
import com.espertech.esper.runtime.client.EPStatement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableIntoTable {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraIntoTableUnkeyedSimpleSameModule());
        execs.add(new InfraIntoTableUnkeyedSimpleTwoModule());
        execs.add(new InfraBoundUnbound());
        execs.add(new InfraIntoTableWindowSortedFromJoin());
        execs.add(new InfraTableIntoTableNoKeys());
        execs.add(new InfraTableIntoTableWithKeys());
        execs.add(new InfraTableBigNumberAggregation());
        execs.add(new InfraIntoTableMultikeyWArraySingleArrayKeyed());
        execs.add(new InfraIntoTableMultikeyWArrayTwoKeyed());
        return execs;
    }

    private static class InfraIntoTableMultikeyWArrayTwoKeyed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@name('tbl') create table MyTable(k1 int[primitive] primary key, k2 int[primitive] primary key, thesum sum(int));\n" +
                    "into table MyTable select intOne, intTwo, sum(value) as thesum from SupportEventWithManyArray group by intOne, intTwo;\n";
            env.compileDeploy(epl);

            sendEvent(env, "E1", 100, new int[]{10}, new int[]{1, 2});
            sendEvent(env, "E2", 101, new int[]{10, 20}, new int[]{1, 2});
            sendEvent(env, "E3", 102, new int[]{10}, new int[]{1, 1});
            sendEvent(env, "E4", 103, new int[]{10, 20}, new int[]{1, 2});
            sendEvent(env, "E5", 104, new int[]{10}, new int[]{1, 1});
            sendEvent(env, "E6", 105, new int[]{10, 20}, new int[]{1, 1});

            env.milestone(0);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("tbl"), "k1,k2,thesum".split(","), new Object[][]{
                {new int[]{10}, new int[]{1, 2}, 100}, {new int[]{10}, new int[]{1, 1}, 102 + 104},
                {new int[]{10, 20}, new int[]{1, 2}, 101 + 103}, {new int[]{10, 20}, new int[]{1, 1}, 105},
            });

            env.undeployAll();
        }

        private void sendEvent(RegressionEnvironment env, String id, int value, int[] intOne, int[] intTwo) {
            env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(intOne).withIntTwo(intTwo).withValue(value));
        }
    }

    private static class InfraIntoTableMultikeyWArraySingleArrayKeyed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@name('tbl') create table MyTable(k int[primitive] primary key, thesum sum(int));\n" +
                    "into table MyTable select intOne, sum(value) as thesum from SupportEventWithManyArray group by intOne;\n";
            env.compileDeploy(epl);

            sendEvent(env, "E1", 10, new int[]{1, 2});
            sendEvent(env, "E2", 11, new int[]{0, 2});
            sendEvent(env, "E3", 12, new int[]{1, 1});
            sendEvent(env, "E4", 13, new int[]{0, 2});
            sendEvent(env, "E5", 14, new int[]{1});
            sendEvent(env, "E6", 15, new int[]{1, 1});

            env.milestone(0);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("tbl"), "k,thesum".split(","), new Object[][]{
                {new int[]{1, 2}, 10}, {new int[]{0, 2}, 11 + 13}, {new int[]{1, 1}, 12 + 15}, {new int[]{1}, 14},
            });

            env.undeployAll();
        }

        private void sendEvent(RegressionEnvironment env, String id, int value, int[] array) {
            env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(array).withValue(value));
        }
    }

    private static class InfraIntoTableUnkeyedSimpleSameModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('tbl') create table MyTable(mycnt count(*));\n" +
                "into table MyTable select count(*) as mycnt from SupportBean;\n";
            env.compileDeploy(epl);
            runAssertionIntoTableUnkeyedSimple(env);
            env.undeployAll();
        }
    }

    private static class InfraIntoTableUnkeyedSimpleTwoModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('tbl') create table MyTable(mycnt count(*))", path);
            env.compileDeploy("into table MyTable select count(*) as mycnt from SupportBean;\n", path);
            runAssertionIntoTableUnkeyedSimple(env);
            env.undeployAll();
        }
    }

    private static class InfraIntoTableWindowSortedFromJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table MyTable(" +
                "thewin window(*) @type('SupportBean')," +
                "thesort sorted(intPrimitive desc) @type('SupportBean')" +
                ")", path);

            env.compileDeploy("into table MyTable " +
                "select window(sb.*) as thewin, sorted(sb.*) as thesort " +
                "from SupportBean_S0#lastevent, SupportBean#keepall as sb", path);
            env.sendEventBean(new SupportBean_S0(1));

            SupportBean sb1 = new SupportBean("E1", 1);
            env.sendEventBean(sb1);

            env.milestone(0);

            SupportBean sb2 = new SupportBean("E2", 2);
            env.sendEventBean(sb2);

            EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from MyTable", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), "thewin,thesort".split(","),
                new Object[][]{{new SupportBean[]{sb1, sb2}, new SupportBean[]{sb2, sb1}}});

            env.undeployAll();
        }
    }

    private static class InfraBoundUnbound implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            // Bound: max/min; Unbound: maxever/minever
            tryAssertionMinMax(env, false, milestone);
            tryAssertionMinMax(env, true, milestone);

            // Bound: window; Unbound: lastever/firstever; Disallowed: last, first
            tryAssertionLastFirstWindow(env, false, milestone);
            tryAssertionLastFirstWindow(env, true, milestone);

            // Bound: sorted; Unbound: maxbyever/minbyever; Disallowed: minby, maxby declaration (must use sorted instead)
            // - requires declaring the same sort expression but can be against subtype of declared event type
            tryAssertionSortedMinMaxBy(env, false, milestone);
            tryAssertionSortedMinMaxBy(env, true, milestone);
        }
    }

    private static void tryAssertionLastFirstWindow(RegressionEnvironment env, boolean soda, AtomicInteger milestone) {
        String[] fields = "lasteveru,firsteveru,windowb".split(",");
        RegressionPath path = new RegressionPath();
        String eplDeclare = "create table varagg (" +
            "lasteveru lastever(*) @type('SupportBean'), " +
            "firsteveru firstever(*) @type('SupportBean'), " +
            "windowb window(*) @type('SupportBean'))";
        env.compileDeploy(soda, eplDeclare, path);

        String eplIterate = "@name('iterate') select varagg from SupportBean_S0#lastevent";
        env.compileDeploy(soda, eplIterate, path);
        env.sendEventBean(new SupportBean_S0(0));

        String eplBoundInto = "into table varagg select window(*) as windowb from SupportBean#length(2)";
        env.compileDeploy(soda, eplBoundInto, path);

        String eplUnboundInto = "into table varagg select lastever(*) as lasteveru, firstever(*) as firsteveru from SupportBean";
        env.compileDeploy(soda, eplUnboundInto, path);

        SupportBean b1 = makeSendBean(env, "E1", 20);
        SupportBean b2 = makeSendBean(env, "E2", 15);

        env.milestoneInc(milestone);

        SupportBean b3 = makeSendBean(env, "E3", 10);
        assertResults(env.statement("iterate"), fields, new Object[]{b3, b1, new Object[]{b2, b3}});

        env.milestoneInc(milestone);

        SupportBean b4 = makeSendBean(env, "E4", 5);
        assertResults(env.statement("iterate"), fields, new Object[]{b4, b1, new Object[]{b3, b4}});

        // invalid: bound aggregation into unbound max
        SupportMessageAssertUtil.tryInvalidCompile(env, path, "into table varagg select last(*) as lasteveru from SupportBean#length(2)",
            "Failed to validate select-clause expression 'last(*)': For into-table use 'window(*)' or 'window(stream.*)' instead");
        // invalid: unbound aggregation into bound max
        SupportMessageAssertUtil.tryInvalidCompile(env, path, "into table varagg select lastever(*) as windowb from SupportBean#length(2)",
            "Incompatible aggregation function for table 'varagg' column 'windowb', expecting 'window(*)' and received 'lastever(*)': The table declares 'window(*)' and provided is 'lastever(*)'");

        // valid: bound with unbound variable
        String eplBoundIntoUnbound = "into table varagg select lastever(*) as lasteveru from SupportBean#length(2)";
        env.compileDeploy(soda, eplBoundIntoUnbound, path);

        env.undeployAll();
    }

    private static void tryAssertionSortedMinMaxBy(RegressionEnvironment env, boolean soda, AtomicInteger milestone) {
        String[] fields = "maxbyeveru,minbyeveru,sortedb".split(",");
        RegressionPath path = new RegressionPath();

        String eplDeclare = "create table varagg (" +
            "maxbyeveru maxbyever(intPrimitive) @type('SupportBean'), " +
            "minbyeveru minbyever(intPrimitive) @type('SupportBean'), " +
            "sortedb sorted(intPrimitive) @type('SupportBean'))";
        env.compileDeploy(soda, eplDeclare, path);

        String eplIterate = "@name('iterate') select varagg from SupportBean_S0#lastevent";
        env.compileDeploy(soda, eplIterate, path);
        env.sendEventBean(new SupportBean_S0(0));

        String eplBoundInto = "into table varagg select sorted() as sortedb from SupportBean#length(2)";
        env.compileDeploy(soda, eplBoundInto, path);

        String eplUnboundInto = "into table varagg select maxbyever() as maxbyeveru, minbyever() as minbyeveru from SupportBean";
        env.compileDeploy(soda, eplUnboundInto, path);

        SupportBean b1 = makeSendBean(env, "E1", 20);
        SupportBean b2 = makeSendBean(env, "E2", 15);

        env.milestoneInc(milestone);

        SupportBean b3 = makeSendBean(env, "E3", 10);
        assertResults(env.statement("iterate"), fields, new Object[]{b1, b3, new Object[]{b3, b2}});

        // invalid: bound aggregation into unbound max
        SupportMessageAssertUtil.tryInvalidCompile(env, path, "into table varagg select maxby(intPrimitive) as maxbyeveru from SupportBean#length(2)",
            "Failed to validate select-clause expression 'maxby(intPrimitive)': When specifying into-table a sort expression cannot be provided [");
        // invalid: unbound aggregation into bound max
        SupportMessageAssertUtil.tryInvalidCompile(env, path, "into table varagg select maxbyever() as sortedb from SupportBean#length(2)",
            "Incompatible aggregation function for table 'varagg' column 'sortedb', expecting 'sorted(intPrimitive)' and received 'maxbyever()': The required aggregation function name is 'sorted' and provided is 'maxbyever' [");

        // valid: bound with unbound variable
        String eplBoundIntoUnbound = "into table varagg select " +
            "maxbyever() as maxbyeveru, minbyever() as minbyeveru " +
            "from SupportBean#length(2)";
        env.compileDeploy(soda, eplBoundIntoUnbound, path);

        env.undeployAll();
    }

    public static class InfraTableIntoTableNoKeys implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "sumint".split(",");
            RegressionPath path = new RegressionPath();

            String eplCreateTable = "@Name('Create-Table') create table MyTable(sumint sum(int))";
            env.compileDeploy(eplCreateTable, path);

            String eplIntoTable = "@Name('Into-Table') into table MyTable select sum(intPrimitive) as sumint from SupportBean";
            env.compileDeploy(eplIntoTable, path);

            String eplQueryTable = "@Name('s0') select (select sumint from MyTable) as c0 from SupportBean_S0 as s0";
            env.compileDeploy(eplQueryTable, path).addListener("s0");

            env.milestone(1);

            assertValue(env, null);

            makeSendBean(env, "E1", 10);
            assertValue(env, 10);

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{10}});
            makeSendBean(env, "E2", 200);
            assertValue(env, 210);

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{210}});
            makeSendBean(env, "E1", 11);
            assertValue(env, 221);

            env.milestone(4);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{221}});
            makeSendBean(env, "E3", 3000);
            assertValue(env, 3221);

            env.milestone(5);
            env.milestone(6);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{3221}});
            makeSendBean(env, "E2", 201);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{3422}});
            makeSendBean(env, "E3", 3001);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{6423}});

            makeSendBean(env, "E1", 12);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{6435}});

            env.undeployAll();
        }

        private static void assertValue(RegressionEnvironment env, Integer value) {
            env.sendEventBean(new SupportBean_S0(0));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(value, event.get("c0"));
        }
    }

    public static class InfraTableIntoTableWithKeys implements RegressionExecution {

        public void run(RegressionEnvironment env) {

            String[] fields = "pkey,sumint".split(",");
            String valueList = "E1,E2,E3";
            RegressionPath path = new RegressionPath();

            String eplCreateTable = "@Name('Create-Table') create table MyTable(pkey string primary key, sumint sum(int))";
            env.compileDeploy(eplCreateTable, path);

            String eplIntoTable = "@Name('Into-Table') into table MyTable select sum(intPrimitive) as sumint from SupportBean group by theString";
            env.compileDeploy(eplIntoTable, path);

            String eplQueryTable = "@Name('s0') select (select sumint from MyTable where pkey = s0.p00) as c0 from SupportBean_S0 as s0";
            env.compileDeploy(eplQueryTable, path).addListener("s0");

            env.milestone(1);

            assertValues(env, valueList, new Integer[]{null, null, null});

            makeSendBean(env, "E1", 10);
            assertValues(env, valueList, new Integer[]{10, null, null});

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{"E1", 10}});
            makeSendBean(env, "E2", 200);
            assertValues(env, valueList, new Integer[]{10, 200, null});

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{"E1", 10}, {"E2", 200}});
            makeSendBean(env, "E1", 11);
            assertValues(env, valueList, new Integer[]{21, 200, null});

            env.milestone(4);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{"E1", 21}, {"E2", 200}});
            makeSendBean(env, "E3", 3000);
            assertValues(env, valueList, new Integer[]{21, 200, 3000});

            env.milestone(5);
            env.milestone(6);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{"E1", 21}, {"E2", 200}, {"E3", 3000}});
            makeSendBean(env, "E2", 201);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{"E1", 21}, {"E2", 401}, {"E3", 3000}});
            makeSendBean(env, "E3", 3001);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{"E1", 21}, {"E2", 401}, {"E3", 6001}});

            makeSendBean(env, "E1", 12);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create-Table"), fields, new Object[][]{{"E1", 33}, {"E2", 401}, {"E3", 6001}});

            env.undeployAll();
        }

        private static void assertValues(RegressionEnvironment env, String keys, Integer[] values) {
            String[] keyarr = keys.split(",");
            for (int i = 0; i < keyarr.length; i++) {
                env.sendEventBean(new SupportBean_S0(0, keyarr[i]));
                EventBean event = env.listener("s0").assertOneGetNewAndReset();
                assertEquals("Failed for key '" + keyarr[i] + "'", values[i], event.get("c0"));
            }
        }
    }

    private static class InfraTableBigNumberAggregation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            String epl = "@name('tbl') create table MyTable as (c0 avg(BigInteger), c1 avg(BigDecimal), c2 sum(BigInteger), c3 sum(BigDecimal));\n" +
                "into table MyTable select avg(bigint) as c0, avg(bigdec) as c1, sum(bigint) as c2, sum(bigdec) as c3  from SupportBeanNumeric#lastevent;\n";
            env.compileDeploy(epl);

            env.sendEventBean(new SupportBeanNumeric(new BigInteger("5"), new BigDecimal("100")));
            EventBean result = env.iterator("tbl").next();
            EPAssertionUtil.assertProps(result, fields, new Object[]{new BigDecimal("5"), new BigDecimal("100"), new BigInteger("5"), new BigDecimal("100")});

            env.sendEventBean(new SupportBeanNumeric(new BigInteger("4"), new BigDecimal("200")));
            result = env.iterator("tbl").next();
            EPAssertionUtil.assertProps(result, fields, new Object[]{new BigDecimal("4"), new BigDecimal("200"), new BigInteger("4"), new BigDecimal("200")});

            env.undeployAll();
        }
    }

    private static void tryAssertionMinMax(RegressionEnvironment env, boolean soda, AtomicInteger milestone) {
        String[] fields = "maxb,maxu,minb,minu".split(",");
        RegressionPath path = new RegressionPath();
        String eplDeclare = "create table varagg (" +
            "maxb max(int), maxu maxever(int), minb min(int), minu minever(int))";
        env.compileDeploy(soda, eplDeclare, path);

        String eplIterate = "@name('iterate') select varagg from SupportBean_S0#lastevent";
        env.compileDeploy(soda, eplIterate, path);
        env.sendEventBean(new SupportBean_S0(0));

        String eplBoundInto = "into table varagg select " +
            "max(intPrimitive) as maxb, min(intPrimitive) as minb " +
            "from SupportBean#length(2)";
        env.compileDeploy(soda, eplBoundInto, path);

        String eplUnboundInto = "into table varagg select " +
            "maxever(intPrimitive) as maxu, minever(intPrimitive) as minu " +
            "from SupportBean";
        env.compileDeploy(soda, eplUnboundInto, path);

        env.sendEventBean(new SupportBean("E1", 20));
        env.sendEventBean(new SupportBean("E2", 15));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E3", 10));
        assertResults(env.statement("iterate"), fields, new Object[]{15, 20, 10, 10});

        env.sendEventBean(new SupportBean("E4", 5));
        assertResults(env.statement("iterate"), fields, new Object[]{10, 20, 5, 5});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E5", 25));
        assertResults(env.statement("iterate"), fields, new Object[]{25, 25, 5, 5});

        // invalid: unbound aggregation into bound max
        SupportMessageAssertUtil.tryInvalidCompile(env, path, "into table varagg select max(intPrimitive) as maxb from SupportBean",
            "Incompatible aggregation function for table 'varagg' column 'maxb', expecting 'max(int)' and received 'max(intPrimitive)': The table declares use with data windows and provided is unbound [");

        // valid: bound with unbound variable
        String eplBoundIntoUnbound = "into table varagg select " +
            "maxever(intPrimitive) as maxu, minever(intPrimitive) as minu " +
            "from SupportBean#length(2)";
        env.compileDeploy(soda, eplBoundIntoUnbound, path);

        env.undeployAll();
    }

    private static void assertResults(EPStatement stmt, String[] fields, Object[] values) {
        EventBean event = stmt.iterator().next();
        Map map = (Map) event.get("varagg");
        EPAssertionUtil.assertPropsMap(map, fields, values);
    }

    private static SupportBean makeSendBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        env.sendEventBean(bean);
        return bean;
    }

    private static void runAssertionIntoTableUnkeyedSimple(RegressionEnvironment env) {
        assertFalse(env.iterator("tbl").hasNext());

        env.sendEventBean(new SupportBean());
        assertIteratorUnkeyedSimple(env, 1);

        env.milestone(0);

        assertIteratorUnkeyedSimple(env, 1);

        env.sendEventBean(new SupportBean());
        assertIteratorUnkeyedSimple(env, 2);
    }

    private static void assertIteratorUnkeyedSimple(RegressionEnvironment env, long expected) {
        assertEquals(expected, env.iterator("tbl").next().get("mycnt"));
    }
}
