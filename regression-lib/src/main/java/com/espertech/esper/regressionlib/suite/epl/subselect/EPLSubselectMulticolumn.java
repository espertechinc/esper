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
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class EPLSubselectMulticolumn {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectMulticolumnAgg());
        execs.add(new EPLSubselectInvalid());
        execs.add(new EPLSubselectColumnsUncorrelated());
        execs.add(new EPLSubselectCorrelatedAggregation());
        return execs;
    }

    public static class EPLSubselectMulticolumnAgg implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"id", "s1totals.v1", "s1totals.v2"};
            String text = "@name('s0') select id, (select count(*) as v1, sum(id) as v2 from SupportBean_S1#length(3)) as s1totals " +
                "from SupportBean_S0 s0";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "G1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, 0L, null});

            env.sendEventBean(new SupportBean_S1(200, "G2"));
            env.sendEventBean(new SupportBean_S0(2, "G2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, 1L, 200});

            env.milestone(0);

            env.sendEventBean(new SupportBean_S1(210, "G2"));
            env.sendEventBean(new SupportBean_S0(3, "G2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, 2L, 410});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S1(220, "G2"));
            env.sendEventBean(new SupportBean_S0(4, "G2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{4, 3L, 630});

            env.undeployAll();
        }
    }

    private static class EPLSubselectInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "select (select theString, sum(intPrimitive) from SupportBean#lastevent as sb) from SupportBean_S0";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to plan subquery number 1 querying SupportBean: Subquery with multi-column select requires that either all or none of the selected columns are under aggregation, unless a group-by clause is also specified [select (select theString, sum(intPrimitive) from SupportBean#lastevent as sb) from SupportBean_S0]");

            epl = "select (select theString, theString from SupportBean#lastevent as sb) from SupportBean_S0";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Column 1 in subquery does not have a unique column name assigned [select (select theString, theString from SupportBean#lastevent as sb) from SupportBean_S0]");

            epl = "select * from SupportBean_S0(p00 = (select theString, theString from SupportBean#lastevent as sb))";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate subquery number 1 querying SupportBean: Subquery multi-column select is not allowed in this context. [select * from SupportBean_S0(p00 = (select theString, theString from SupportBean#lastevent as sb))]");

            epl = "select exists(select sb.* as v1, intPrimitive*2 as v3 from SupportBean#lastevent as sb) as subrow from SupportBean_S0 as s0";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to plan subquery number 1 querying SupportBean: Subquery multi-column select does not allow wildcard or stream wildcard when selecting multiple columns. [select exists(select sb.* as v1, intPrimitive*2 as v3 from SupportBean#lastevent as sb) as subrow from SupportBean_S0 as s0]");

            epl = "select (select sb.* as v1, intPrimitive*2 as v3 from SupportBean#lastevent as sb) as subrow from SupportBean_S0 as s0";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to plan subquery number 1 querying SupportBean: Subquery multi-column select does not allow wildcard or stream wildcard when selecting multiple columns. [select (select sb.* as v1, intPrimitive*2 as v3 from SupportBean#lastevent as sb) as subrow from SupportBean_S0 as s0]");

            epl = "select (select *, intPrimitive from SupportBean#lastevent as sb) as subrow from SupportBean_S0 as s0";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to plan subquery number 1 querying SupportBean: Subquery multi-column select does not allow wildcard or stream wildcard when selecting multiple columns. [select (select *, intPrimitive from SupportBean#lastevent as sb) as subrow from SupportBean_S0 as s0]");

            epl = "select * from SupportBean_S0(p00 in (select theString, theString from SupportBean#lastevent as sb))";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate subquery number 1 querying SupportBean: Subquery multi-column select is not allowed in this context. [select * from SupportBean_S0(p00 in (select theString, theString from SupportBean#lastevent as sb))]");
        }
    }

    private static class EPLSubselectColumnsUncorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String stmtText = "@name('s0') select " +
                "(select theString as v1, intPrimitive as v2 from SupportBean#lastevent) as subrow " +
                "from SupportBean_S0 as s0";
            env.compileDeployAddListenerMile(stmtText, "s0", milestone.getAndIncrement());

            tryAssertion(env);

            env.undeployAll();

            env.eplToModelCompileDeploy(stmtText).addListener("s0").milestone(milestone.getAndIncrement());

            tryAssertion(env);

            env.undeployAll();
        }
    }

    private static class EPLSubselectCorrelatedAggregation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select p00, " +
                "(select " +
                "  sum(intPrimitive) as v1, " +
                "  sum(intPrimitive + 1) as v2, " +
                "  window(intPrimitive) as v3, " +
                "  window(sb.*) as v4 " +
                "  from SupportBean#keepall sb " +
                "  where theString = s0.p00) as subrow " +
                "from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            Object[][] rows = new Object[][]{
                {"p00", String.class, false},
                {"subrow", Map.class, true}
            };
            for (int i = 0; i < rows.length; i++) {
                String message = "Failed assertion for " + rows[i][0];
                EventPropertyDescriptor prop = env.statement("s0").getEventType().getPropertyDescriptors()[i];
                Assert.assertEquals(message, rows[i][0], prop.getPropertyName());
                Assert.assertEquals(message, rows[i][1], prop.getPropertyType());
                Assert.assertEquals(message, rows[i][2], prop.isFragment());
            }

            FragmentEventType fragmentType = env.statement("s0").getEventType().getFragmentType("subrow");
            assertFalse(fragmentType.isIndexed());
            assertFalse(fragmentType.isNative());
            rows = new Object[][]{
                {"v1", Integer.class},
                {"v2", Integer.class},
                {"v3", Integer[].class},
                {"v4", SupportBean[].class},
            };
            for (int i = 0; i < rows.length; i++) {
                String message = "Failed assertion for " + rows[i][0];
                EventPropertyDescriptor prop = fragmentType.getFragmentType().getPropertyDescriptors()[i];
                Assert.assertEquals(message, rows[i][0], prop.getPropertyName());
                Assert.assertEquals(message, rows[i][1], prop.getPropertyType());
            }

            String[] fields = "p00,subrow.v1,subrow.v2".split(",");

            env.sendEventBean(new SupportBean_S0(1, "T1"));
            EventBean row = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(row, fields, new Object[]{"T1", null, null});
            assertNull(row.get("subrow.v3"));
            assertNull(row.get("subrow.v4"));

            SupportBean sb1 = new SupportBean("T1", 10);
            env.sendEventBean(sb1);
            env.sendEventBean(new SupportBean_S0(2, "T1"));
            row = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(row, fields, new Object[]{"T1", 10, 11});
            EPAssertionUtil.assertEqualsAnyOrder((Integer[]) row.get("subrow.v3"), new Integer[]{10});
            EPAssertionUtil.assertEqualsAnyOrder((Object[]) row.get("subrow.v4"), new Object[]{sb1});

            SupportBean sb2 = new SupportBean("T1", 20);
            env.sendEventBean(sb2);
            env.sendEventBean(new SupportBean_S0(3, "T1"));
            row = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(row, fields, new Object[]{"T1", 30, 32});
            EPAssertionUtil.assertEqualsAnyOrder((Integer[]) row.get("subrow.v3"), new Integer[]{10, 20});
            EPAssertionUtil.assertEqualsAnyOrder((Object[]) row.get("subrow.v4"), new Object[]{sb1, sb2});

            env.undeployAll();
        }
    }

    private static void tryAssertion(RegressionEnvironment env) {

        FragmentEventType fragmentType = env.statement("s0").getEventType().getFragmentType("subrow");
        assertFalse(fragmentType.isIndexed());
        assertFalse(fragmentType.isNative());
        Object[][] rows = new Object[][]{
            {"v1", String.class},
            {"v2", Integer.class},
        };
        for (int i = 0; i < rows.length; i++) {
            String message = "Failed assertion for " + rows[i][0];
            EventPropertyDescriptor prop = fragmentType.getFragmentType().getPropertyDescriptors()[i];
            Assert.assertEquals(message, rows[i][0], prop.getPropertyName());
            Assert.assertEquals(message, rows[i][1], prop.getPropertyType());
        }

        String[] fields = "subrow.v1,subrow.v2".split(",");

        env.sendEventBean(new SupportBean_S0(1));
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, fields, new Object[]{null, null});

        env.sendEventBean(new SupportBean("E1", 10));
        env.sendEventBean(new SupportBean_S0(2));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10});

        env.sendEventBean(new SupportBean("E2", 20));
        env.sendEventBean(new SupportBean_S0(3));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 20});
    }
}
