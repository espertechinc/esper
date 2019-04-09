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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EPLSubselectAggregatedMultirowAndColumn {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectMultirowGroupedNoDataWindowUncorrelated());
        execs.add(new EPLSubselectMultirowGroupedCorrelatedWithEnumMethod());
        execs.add(new EPLSubselectMultirowGroupedUncorrelatedWithEnumerationMethod());
        execs.add(new EPLSubselectMultirowGroupedCorrelatedWHaving());
        execs.add(new EPLSubselectMultirowGroupedNamedWindowSubqueryIndexShared());
        execs.add(new EPLSubselectMulticolumnGroupedUncorrelatedUnfiltered());
        execs.add(new EPLSubselectMultirowGroupedUncorrelatedIteratorAndExpressionDef());
        execs.add(new EPLSubselectMulticolumnGroupedContextPartitioned());
        execs.add(new EPLSubselectMulticolumnGroupedWHaving());
        execs.add(new EPLSubselectMulticolumnInvalid());
        execs.add(new EPLSubselectMulticolumnGroupBy());
        execs.add(new EPLSubselectMultirowGroupedMultikeyWArray());
        execs.add(new EPLSubselectMultirowGroupedIndexSharedMultikeyWArray());
        return execs;
    }

    private static class EPLSubselectMultirowGroupedIndexSharedMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test uncorrelated
            RegressionPath path = new RegressionPath();
            String epl = "@Hint('enable_window_subquery_indexshare') create window MyWindow#keepall as SupportEventWithManyArray;\n" +
                "insert into MyWindow select * from SupportEventWithManyArray;\n";
            env.compileDeploy(epl, path);

            sendManyArray(env, "E1", new int[]{1, 2}, 10);
            sendManyArray(env, "E2", new int[]{1}, 20);
            sendManyArray(env, "E3", new int[]{1}, 21);
            sendManyArray(env, "E4", new int[]{1, 2}, 11);

            epl = "@name('s0') select " +
                "(select intOne as c0, sum(value) as c1 from MyWindow group by intOne).take(10) as e1 from SupportBean_S0";
            env.compileDeploy(epl, path).addListener("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(1));
            Map[] maps = getSortMapMultiRow("e1", env.listener("s0").assertOneGetNewAndReset(), "c1");
            assertTrue(Arrays.equals(new int[]{1, 2}, (int[]) maps[0].get("c0")));
            assertEquals(21, maps[0].get("c1"));
            assertTrue(Arrays.equals(new int[]{1}, (int[]) maps[1].get("c0")));
            assertEquals(41, maps[1].get("c1"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectMultirowGroupedMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select (select sum(value) as c0 from SupportEventWithIntArray#keepall group by array) as subq from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportEventWithIntArray("E1", new int[]{1, 2}, 10));
            env.sendEventBean(new SupportEventWithIntArray("E2", new int[]{1, 2}, 11));

            env.milestone(0);

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "subq".split(","), new Object[]{21});

            env.sendEventBean(new SupportEventWithIntArray("E3", new int[]{1, 2}, 12));
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "subq".split(","), new Object[]{33});

            env.milestone(1);

            env.sendEventBean(new SupportEventWithIntArray("E4", new int[]{1}, 13));
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "subq".split(","), new Object[]{null});

            env.undeployAll();
        }
    }

    public static class EPLSubselectMulticolumnInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Invalid tests
            String epl;
            // not fully aggregated
            epl = "select (select theString, sum(longPrimitive) from SupportBean#keepall group by intPrimitive) from SupportBean_S0";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to plan subquery number 1 querying SupportBean: Subselect with group-by requires non-aggregated properties in the select-clause to also appear in the group-by clause [select (select theString, sum(longPrimitive) from SupportBean#keepall group by intPrimitive) from SupportBean_S0]");

            // correlated group-by not allowed
            epl = "select (select theString, sum(longPrimitive) from SupportBean#keepall group by theString, s0.id) from SupportBean_S0 as s0";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to plan subquery number 1 querying SupportBean: Subselect with group-by requires that group-by properties are provided by the subselect stream only (property 'id' is not) [select (select theString, sum(longPrimitive) from SupportBean#keepall group by theString, s0.id) from SupportBean_S0 as s0]");
            epl = "select (select theString, sum(longPrimitive) from SupportBean#keepall group by theString, s0.getP00()) from SupportBean_S0 as s0";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to plan subquery number 1 querying SupportBean: Subselect with group-by requires that group-by properties are provided by the subselect stream only (expression 's0.getP00()' against stream 1 is not)");

            // aggregations not allowed in group-by
            epl = "select (select intPrimitive, sum(longPrimitive) from SupportBean#keepall group by sum(intPrimitive)) from SupportBean_S0 as s0";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to plan subquery number 1 querying SupportBean: Group-by expressions in a subselect may not have an aggregation function [select (select intPrimitive, sum(longPrimitive) from SupportBean#keepall group by sum(intPrimitive)) from SupportBean_S0 as s0]");

            // "prev" not allowed in group-by
            epl = "select (select intPrimitive, sum(longPrimitive) from SupportBean#keepall group by prev(1, intPrimitive)) from SupportBean_S0 as s0";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to plan subquery number 1 querying SupportBean: Group-by expressions in a subselect may not have a function that requires view resources (prior, prev) [select (select intPrimitive, sum(longPrimitive) from SupportBean#keepall group by prev(1, intPrimitive)) from SupportBean_S0 as s0]");
        }
    }

    private static class EPLSubselectMulticolumnGroupedWHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') @name('s0')select (select theString as c0, sum(intPrimitive) as c1 from SupportBean#keepall group by theString having sum(intPrimitive) > 10) as subq from SupportBean_S0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendSBEventAndTrigger(env, "E1", 10);
            assertMapFieldAndReset(env, "subq", fields, null);

            sendSBEventAndTrigger(env, "E2", 5);
            assertMapFieldAndReset(env, "subq", fields, null);

            sendSBEventAndTrigger(env, "E2", 6);
            assertMapFieldAndReset(env, "subq", fields, new Object[]{"E2", 11});

            sendSBEventAndTrigger(env, "E1", 1);
            assertMapFieldAndReset(env, "subq", fields, null);

            env.undeployAll();
        }
    }

    private static class EPLSubselectMulticolumnGroupedContextPartitioned implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String fieldName = "subq";
            String[] fields = "c0,c1".split(",");

            String epl =
                "create context MyCtx partition by theString from SupportBean, p00 from SupportBean_S0;\n" +
                    "@name('s0') context MyCtx select " +
                    "(select theString as c0, sum(intPrimitive) as c1 " +
                    " from SupportBean#keepall " +
                    " group by theString) as subq " +
                    "from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBean("P1", 100));
            env.sendEventBean(new SupportBean_S0(1, "P1"));
            assertMapFieldAndReset(env, fieldName, fields, new Object[]{"P1", 100});

            env.sendEventBean(new SupportBean_S0(2, "P2"));
            assertMapFieldAndReset(env, fieldName, fields, null);

            env.sendEventBean(new SupportBean("P2", 200));
            env.sendEventBean(new SupportBean_S0(3, "P2"));
            assertMapFieldAndReset(env, fieldName, fields, new Object[]{"P2", 200});

            env.sendEventBean(new SupportBean("P2", 205));
            env.sendEventBean(new SupportBean_S0(4, "P2"));
            assertMapFieldAndReset(env, fieldName, fields, new Object[]{"P2", 405});

            env.undeployAll();
        }
    }

    private static class EPLSubselectMulticolumnGroupedUncorrelatedUnfiltered implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String fieldName = "subq";
            String[] fields = "c0,c1".split(",");
            RegressionPath path = new RegressionPath();

            String eplNoDelete = "@name('s0') select " +
                "(select theString as c0, sum(intPrimitive) as c1 " +
                "from SupportBean#keepall " +
                "group by theString) as subq " +
                "from SupportBean_S0 as s0";
            env.compileDeploy(eplNoDelete, path).addListener("s0").milestoneInc(milestone);
            runAssertionNoDelete(env, fieldName, fields);
            env.undeployAll();

            // try SODA
            EPStatementObjectModel model = env.eplToModel(eplNoDelete);
            assertEquals(eplNoDelete, model.toEPL());
            env.compileDeploy(model, path).addListener("s0").milestoneInc(milestone);
            runAssertionNoDelete(env, fieldName, fields);
            env.undeployAll();

            // test named window with delete/remove
            String epl = "create window MyWindow#keepall as SupportBean;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "on SupportBean_S1 delete from MyWindow where id = intPrimitive;\n" +
                "@name('s0') @Hint('disable_reclaim_group') select (select theString as c0, sum(intPrimitive) as c1 " +
                " from MyWindow group by theString) as subq from SupportBean_S0 as s0";
            env.compileDeploy(epl, path).addListener("s0").milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(1));
            assertMapFieldAndReset(env, fieldName, fields, null);

            sendSBEventAndTrigger(env, "E1", 10);
            assertMapFieldAndReset(env, fieldName, fields, new Object[]{"E1", 10});

            sendS1EventAndTrigger(env, 10);     // delete 10
            assertMapFieldAndReset(env, fieldName, fields, null);

            sendSBEventAndTrigger(env, "E2", 20);
            assertMapFieldAndReset(env, fieldName, fields, new Object[]{"E2", 20});

            sendSBEventAndTrigger(env, "E2", 21);
            assertMapFieldAndReset(env, fieldName, fields, new Object[]{"E2", 41});

            sendSBEventAndTrigger(env, "E1", 30);
            assertMapFieldAndReset(env, fieldName, fields, null);

            sendS1EventAndTrigger(env, 30);     // delete 30
            assertMapFieldAndReset(env, fieldName, fields, new Object[]{"E2", 41});

            sendS1EventAndTrigger(env, 20);     // delete 20
            assertMapFieldAndReset(env, fieldName, fields, new Object[]{"E2", 21});

            sendSBEventAndTrigger(env, "E1", 31);    // two groups
            assertMapFieldAndReset(env, fieldName, fields, null);

            sendS1EventAndTrigger(env, 21);     // delete 21
            assertMapFieldAndReset(env, fieldName, fields, new Object[]{"E1", 31});
            env.undeployAll();

            // test multiple group-by criteria
            String[] fieldsMultiGroup = "c0,c1,c2,c3,c4".split(",");
            String eplMultiGroup = "@name('s0') select " +
                "(select theString as c0, intPrimitive as c1, theString||'x' as c2, " +
                "    intPrimitive * 1000 as c3, sum(longPrimitive) as c4 " +
                " from SupportBean#keepall " +
                " group by theString, intPrimitive) as subq " +
                "from SupportBean_S0 as s0";
            env.compileDeploy(eplMultiGroup, path).addListener("s0");

            sendSBEventAndTrigger(env, "G1", 1, 100L);
            assertMapFieldAndReset(env, fieldName, fieldsMultiGroup, new Object[]{"G1", 1, "G1x", 1000, 100L});

            env.milestoneInc(milestone);
            sendSBEventAndTrigger(env, "G1", 1, 101L);
            assertMapFieldAndReset(env, fieldName, fieldsMultiGroup, new Object[]{"G1", 1, "G1x", 1000, 201L});

            sendSBEventAndTrigger(env, "G2", 1, 200L);
            assertMapFieldAndReset(env, fieldName, fieldsMultiGroup, null);

            env.undeployAll();
        }
    }

    private static class EPLSubselectMultirowGroupedCorrelatedWHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String fieldName = "subq";
            String[] fields = "c0,c1".split(",");

            String eplEnumCorrelated = "@name('s0') select " +
                "(select theString as c0, sum(intPrimitive) as c1 " +
                " from SupportBean#keepall " +
                " where intPrimitive = s0.id " +
                " group by theString" +
                " having sum(intPrimitive) > 10).take(100) as subq " +
                "from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(eplEnumCorrelated, "s0");

            env.sendEventBean(new SupportBean_S0(1));
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, null);

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 10));
            env.sendEventBean(new SupportBean("E3", 10));
            env.sendEventBean(new SupportBean_S0(10));
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, null);

            env.sendEventBean(new SupportBean("E2", 10));
            env.sendEventBean(new SupportBean_S0(10));
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E2", 20}});

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean_S0(10));
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E1", 20}, {"E2", 20}});

            env.sendEventBean(new SupportBean("E3", 55));
            env.sendEventBean(new SupportBean_S0(10));
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E1", 20}, {"E2", 20}});

            env.undeployAll();
        }
    }

    private static class EPLSubselectMultirowGroupedCorrelatedWithEnumMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String fieldName = "subq";
            String[] fields = "c0,c1".split(",");

            String eplEnumCorrelated = "@name('s0') select " +
                "(select theString as c0, sum(intPrimitive) as c1 " +
                " from SupportBean#keepall " +
                " where intPrimitive = s0.id " +
                " group by theString).take(100) as subq " +
                "from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(eplEnumCorrelated, "s0");

            env.sendEventBean(new SupportBean_S0(1));
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, null);

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean_S0(10));
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E1", 10}});

            env.sendEventBean(new SupportBean_S0(11));
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, null);

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean_S0(10));
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E1", 20}});

            env.sendEventBean(new SupportBean("E2", 100));
            env.sendEventBean(new SupportBean_S0(100));
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E2", 100}});

            env.undeployAll();
        }
    }

    private static class EPLSubselectMultirowGroupedNamedWindowSubqueryIndexShared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test uncorrelated
            RegressionPath path = new RegressionPath();
            String epl = "@Hint('enable_window_subquery_indexshare')" +
                "create window SBWindow#keepall as SupportBean;\n" +
                "insert into SBWindow select * from SupportBean;\n";
            env.compileDeploy(epl, path);

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E1", 20));

            String stmtUncorrelated = "@name('s0') select " +
                "(select theString as c0, sum(intPrimitive) as c1 from SBWindow group by theString).take(10) as e1 from SupportBean_S0";
            env.compileDeploy(stmtUncorrelated, path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1));
            assertMapMultiRow("e1", env.listener("s0").assertOneGetNewAndReset(), "c0", "c0,c1".split(","), new Object[][]{{"E1", 30}});

            env.sendEventBean(new SupportBean("E2", 200));
            env.sendEventBean(new SupportBean_S0(2));
            assertMapMultiRow("e1", env.listener("s0").assertOneGetNewAndReset(), "c0", "c0,c1".split(","), new Object[][]{{"E1", 30}, {"E2", 200}});
            env.undeployModuleContaining("s0");

            // test correlated
            String eplTwo = "@name('s0') select " +
                "(select theString as c0, sum(intPrimitive) as c1 from SBWindow where theString = s0.p00 group by theString).take(10) as e1 from SupportBean_S0 as s0";
            env.compileDeploy(eplTwo, path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            assertMapMultiRow("e1", env.listener("s0").assertOneGetNewAndReset(), "c0", "c0,c1".split(","), new Object[][]{{"E1", 30}});

            env.undeployAll();
        }
    }

    private static class EPLSubselectMultirowGroupedNoDataWindowUncorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select (select theString as c0, sum(intPrimitive) as c1 from SupportBean group by theString).take(10) as subq from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "c0,c1".split(",");

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            EPLSubselectAggregatedMultirowAndColumn.assertMapMultiRow("subq", env.listener("s0").assertOneGetNewAndReset(), "c0", fields, null);

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean_S0(2, "E2"));
            EPLSubselectAggregatedMultirowAndColumn.assertMapMultiRow("subq", env.listener("s0").assertOneGetNewAndReset(), "c0", fields, new Object[][]{{"G1", 10}});

            env.sendEventBean(new SupportBean("G2", 20));
            env.sendEventBean(new SupportBean_S0(3, "E3"));
            EPLSubselectAggregatedMultirowAndColumn.assertMapMultiRow("subq", env.listener("s0").assertOneGetNewAndReset(), "c0", fields, new Object[][]{{"G1", 10}, {"G2", 20}});

            env.undeployAll();
        }
    }

    private static class EPLSubselectMultirowGroupedUncorrelatedIteratorAndExpressionDef implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') expression getGroups {" +
                "(select theString as c0, sum(intPrimitive) as c1 " +
                "  from SupportBean#keepall group by theString)" +
                "}" +
                "select getGroups() as e1, getGroups().take(10) as e2 from SupportBean_S0#lastevent()";
            env.compileDeploy(epl).addListener("s0");

            sendSBEventAndTrigger(env, "E1", 20);
            for (EventBean event : new EventBean[]{env.listener("s0").assertOneGetNew(), env.statement("s0").iterator().next()}) {
                assertMapField("e1", event, fields, new Object[]{"E1", 20});
                assertMapMultiRow("e2", event, "c0", fields, new Object[][]{{"E1", 20}});
            }
            env.listener("s0").reset();

            sendSBEventAndTrigger(env, "E2", 30);
            for (EventBean event : new EventBean[]{env.listener("s0").assertOneGetNew(), env.statement("s0").iterator().next()}) {
                assertMapField("e1", event, fields, null);
                assertMapMultiRow("e2", event, "c0", fields, new Object[][]{{"E1", 20}, {"E2", 30}});
            }
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class EPLSubselectMultirowGroupedUncorrelatedWithEnumerationMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String fieldName = "subq";
            String[] fields = "c0,c1".split(",");

            // test unfiltered
            String eplEnumUnfiltered = "@name('s0') select " +
                "(select theString as c0, sum(intPrimitive) as c1 " +
                " from SupportBean#keepall " +
                " group by theString).take(100) as subq " +
                "from SupportBean_S0 as s0";
            env.compileDeploy(eplEnumUnfiltered).addListener("s0").milestone(0);

            env.sendEventBean(new SupportBean_S0(1));
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, null);

            sendSBEventAndTrigger(env, "E1", 10);
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E1", 10}});

            sendSBEventAndTrigger(env, "E1", 20);
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E1", 30}});

            sendSBEventAndTrigger(env, "E2", 100);
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E1", 30}, {"E2", 100}});

            sendSBEventAndTrigger(env, "E3", 2000);
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E1", 30}, {"E2", 100}, {"E3", 2000}});
            env.undeployAll();

            // test filtered
            String eplEnumFiltered = "@name('s0') select " +
                "(select theString as c0, sum(intPrimitive) as c1 " +
                " from SupportBean#keepall " +
                " where intPrimitive > 100 " +
                " group by theString).take(100) as subq " +
                "from SupportBean_S0 as s0";
            env.compileDeployAddListenerMile(eplEnumFiltered, "s0", 1);

            env.sendEventBean(new SupportBean_S0(1));
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, null);

            sendSBEventAndTrigger(env, "E1", 10);
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, null);

            sendSBEventAndTrigger(env, "E1", 200);
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E1", 200}});

            sendSBEventAndTrigger(env, "E1", 11);
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E1", 200}});

            sendSBEventAndTrigger(env, "E1", 201);
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E1", 401}});

            sendSBEventAndTrigger(env, "E2", 300);
            assertMapMultiRowAndReset(env, fieldName, "c0", fields, new Object[][]{{"E1", 401}, {"E2", 300}});

            env.undeployAll();
        }
    }

    public static class EPLSubselectMulticolumnGroupBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select (select theString as c0, sum(intPrimitive) as c1 " +
                "from SupportBean#keepall() group by theString).take(10) as e1 from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1));
            assertMapMultiRow("e1", env.listener("s0").assertOneGetNewAndReset(), "c0", "c0,c1".split(","),
                null);

            env.milestone(1);

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean_S0(2));
            assertMapMultiRow("e1", env.listener("s0").assertOneGetNewAndReset(), "c0", "c0,c1".split(","),
                new Object[][]{{"E1", 10}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E2", 200));
            env.sendEventBean(new SupportBean_S0(3));
            assertMapMultiRow("e1", env.listener("s0").assertOneGetNewAndReset(), "c0", "c0,c1".split(","),
                new Object[][]{{"E1", 10}, {"E2", 200}});

            env.milestone(3);

            env.sendEventBean(new SupportBean("E1", 20));
            env.sendEventBean(new SupportBean_S0(4));
            assertMapMultiRow("e1", env.listener("s0").assertOneGetNewAndReset(), "c0", "c0,c1".split(","),
                new Object[][]{{"E1", 30}, {"E2", 200}});

            env.undeployAll();
        }
    }

    private static void runAssertionNoDelete(RegressionEnvironment env, String fieldName, String[] fields) {
        env.sendEventBean(new SupportBean_S0(1));
        assertMapFieldAndReset(env, fieldName, fields, null);

        sendSBEventAndTrigger(env, "E1", 10);
        assertMapFieldAndReset(env, fieldName, fields, new Object[]{"E1", 10});

        sendSBEventAndTrigger(env, "E1", 20);
        assertMapFieldAndReset(env, fieldName, fields, new Object[]{"E1", 30});

        // second group - this returns null as subquerys cannot return multiple rows (unless enumerated) (sql standard)
        sendSBEventAndTrigger(env, "E2", 5);
        assertMapFieldAndReset(env, fieldName, fields, null);
    }

    private static void sendSBEventAndTrigger(RegressionEnvironment env, String theString, int intPrimitive) {
        sendSBEventAndTrigger(env, theString, intPrimitive, 0);
    }

    private static void sendSBEventAndTrigger(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
        env.sendEventBean(new SupportBean_S0(0));
    }

    private static void sendS1EventAndTrigger(RegressionEnvironment env, int id) {
        env.sendEventBean(new SupportBean_S1(id, "x"));
        env.sendEventBean(new SupportBean_S0(0));
    }

    private static void assertMapFieldAndReset(RegressionEnvironment env, String fieldName, String[] names, Object[] values) {
        assertMapField(fieldName, env.listener("s0").assertOneGetNew(), names, values);
        env.listener("s0").reset();
    }

    private static void assertMapMultiRowAndReset(RegressionEnvironment env, String fieldName, final String sortKey, String[] names, Object[][] values) {
        assertMapMultiRow(fieldName, env.listener("s0").assertOneGetNew(), sortKey, names, values);
        env.listener("s0").reset();
    }

    private static void assertMapField(String fieldName, EventBean event, String[] names, Object[] values) {
        Map<String, Object> subq = (Map<String, Object>) event.get(fieldName);
        if (values == null && subq == null) {
            return;
        }
        EPAssertionUtil.assertPropsMap(subq, names, values);
    }

    protected static void assertMapMultiRow(String fieldName, EventBean event, final String sortKey, String[] names, Object[][] values) {
        Map[] maps = getSortMapMultiRow(fieldName, event, sortKey);
        if (values == null && maps == null) {
            return;
        }
        EPAssertionUtil.assertPropsPerRow(maps, names, values);
    }

    protected static Map[] getSortMapMultiRow(String fieldName, EventBean event, final String sortKey) {
        Collection<Map> subq = (Collection<Map>) event.get(fieldName);
        if (subq == null) {
            return null;
        }
        Map[] maps = subq.toArray(new Map[subq.size()]);
        Arrays.sort(maps, new Comparator<Map>() {
            public int compare(Map o1, Map o2) {
                return ((Comparable) o1.get(sortKey)).compareTo(o2.get(sortKey));
            }
        });
        return maps;
    }

    private static void sendManyArray(RegressionEnvironment env, String id, int[] ints, int value) {
        env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(ints).withValue(value));
    }
}
