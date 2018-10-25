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
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;

public class EPLSubselectAggregatedSingleValue {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectUngroupedUncorrelatedInSelect());
        execs.add(new EPLSubselectUngroupedUncorrelatedTwoAggStopStart());
        execs.add(new EPLSubselectUngroupedUncorrelatedNoDataWindow());
        execs.add(new EPLSubselectUngroupedUncorrelatedWHaving());
        execs.add(new EPLSubselectUngroupedUncorrelatedInWhereClause());
        execs.add(new EPLSubselectUngroupedUncorrelatedInSelectClause());
        execs.add(new EPLSubselectUngroupedUncorrelatedFiltered());
        execs.add(new EPLSubselectUngroupedUncorrelatedWWhereClause());
        execs.add(new EPLSubselectUngroupedCorrelated());
        execs.add(new EPLSubselectUngroupedCorrelatedSceneTwo());
        execs.add(new EPLSubselectUngroupedCorrelatedInWhereClause());
        execs.add(new EPLSubselectUngroupedCorrelatedWHaving());
        execs.add(new EPLSubselectUngroupedJoin3StreamKeyRangeCoercion());
        execs.add(new EPLSubselectUngroupedJoin2StreamRangeCoercion());
        execs.add(new EPLSubselectGroupedUncorrelatedWHaving());
        execs.add(new EPLSubselectGroupedCorrelatedWHaving());
        execs.add(new EPLSubselectGroupedCorrelationInsideHaving());
        execs.add(new EPLSubselectAggregatedInvalid());
        execs.add(new EPLSubselectUngroupedCorrelationInsideHaving());
        execs.add(new EPLSubselectUngroupedTableWHaving());
        execs.add(new EPLSubselectGroupedTableWHaving());
        return execs;
    }

    public static class EPLSubselectAggregatedInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // invalid tests
            String stmtText;

            stmtText = "select (select sum(s0.id) from SupportBean_S1#length(3) as s1) as value from SupportBean_S0 as s0";
            SupportMessageAssertUtil.tryInvalidCompile(env, stmtText, "Failed to plan subquery number 1 querying SupportBean_S1: Subselect aggregation functions cannot aggregate across correlated properties");

            stmtText = "select (select s1.id + sum(s1.id) from SupportBean_S1#length(3) as s1) as value from SupportBean_S0 as s0";
            SupportMessageAssertUtil.tryInvalidCompile(env, stmtText, "Failed to plan subquery number 1 querying SupportBean_S1: Subselect properties must all be within aggregation functions");

            stmtText = "select (select sum(s0.id + s1.id) from SupportBean_S1#length(3) as s1) as value from SupportBean_S0 as s0";
            SupportMessageAssertUtil.tryInvalidCompile(env, stmtText, "Failed to plan subquery number 1 querying SupportBean_S1: Subselect aggregation functions cannot aggregate across correlated properties");

            // having-clause cannot aggregate over properties from other streams
            stmtText = "select (select theString from SupportBean#keepall having sum(s0.p00) = 1) as c0 from SupportBean_S0 as s0";
            SupportMessageAssertUtil.tryInvalidCompile(env, stmtText, "Failed to plan subquery number 1 querying SupportBean: Failed to validate having-clause expression '(sum(s0.p00))=1': Implicit conversion from datatype 'String' to numeric is not allowed for aggregation function 'sum' [");

            // having-clause properties must be aggregated
            stmtText = "select (select theString from SupportBean#keepall having sum(intPrimitive) = intPrimitive) as c0 from SupportBean_S0 as s0";
            SupportMessageAssertUtil.tryInvalidCompile(env, stmtText, "Failed to plan subquery number 1 querying SupportBean: Subselect having-clause requires that all properties are under aggregation, consider using the 'first' aggregation function instead");

            // having-clause not returning boolean
            stmtText = "select (select theString from SupportBean#keepall having sum(intPrimitive)) as c0 from SupportBean_S0";
            SupportMessageAssertUtil.tryInvalidCompile(env, stmtText, "Failed to plan subquery number 1 querying SupportBean: Subselect having-clause expression must return a boolean value ");
        }
    }

    private static class EPLSubselectGroupedCorrelationInsideHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') @name('s0')select (select theString from SupportBean#keepall group by theString having sum(intPrimitive) = s0.id) as c0 from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendSB(env, "E1", 100);
            sendSB(env, "E2", 5);
            sendSB(env, "E3", 20);
            sendEventS0Assert(env, 1, null);
            sendEventS0Assert(env, 5, "E2");

            sendSB(env, "E2", 3);
            sendEventS0Assert(env, 5, null);
            sendEventS0Assert(env, 8, "E2");
            sendEventS0Assert(env, 20, "E3");

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedCorrelationInsideHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') @name('s0')select (select last(theString) from SupportBean#keepall having sum(intPrimitive) = s0.id) as c0 from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendSB(env, "E1", 100);
            sendEventS0Assert(env, 1, null);
            sendEventS0Assert(env, 100, "E1");

            sendSB(env, "E2", 5);
            sendEventS0Assert(env, 100, null);
            sendEventS0Assert(env, 105, "E2");

            env.undeployAll();
        }
    }

    private static class EPLSubselectGroupedTableWHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTableWith2Keys(k1 string primary key, k2 string primary key, total sum(int));\n" +
                "into table MyTableWith2Keys select p10 as k1, p11 as k2, sum(id) as total from SupportBean_S1 group by p10, p11;\n" +
                "@name('s0') @name('s0')select (select sum(total) from MyTableWith2Keys group by k1 having sum(total) > 100) as c0 from SupportBean_S0;\n";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEventS1(env, 50, "G1", "S1");
            sendEventS1(env, 50, "G1", "S2");
            sendEventS1(env, 50, "G2", "S1");
            sendEventS1(env, 50, "G2", "S2");
            sendEventS0Assert(env, null);

            sendEventS1(env, 1, "G2", "S3");
            sendEventS0Assert(env, 101);

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedTableWHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table MyTable(total sum(int))", path);
            env.compileDeploy("into table MyTable select sum(intPrimitive) as total from SupportBean", path);
            env.compileDeploy("@name('s0') select (select sum(total) from MyTable having sum(total) > 100) as c0 from SupportBean_S0", path);
            env.addListener("s0");

            sendEventS0Assert(env, null);

            sendSB(env, "E1", 50);
            sendEventS0Assert(env, null);

            sendSB(env, "E2", 55);
            sendEventS0Assert(env, 105);

            sendSB(env, "E3", -5);
            sendEventS0Assert(env, null);

            env.undeployAll();
        }
    }

    private static class EPLSubselectGroupedCorrelatedWHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') @name('s0')select (select sum(intPrimitive) from SupportBean#keepall where s0.id = intPrimitive group by theString having sum(intPrimitive) > 10) as c0 from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEventS0Assert(env, 10, null);

            sendSB(env, "G1", 10);
            sendSB(env, "G2", 10);
            sendSB(env, "G2", 2);
            sendSB(env, "G1", 9);
            sendEventS0Assert(env, null);

            sendSB(env, "G2", 10);
            sendEventS0Assert(env, 10, 20);

            sendSB(env, "G1", 10);
            sendEventS0Assert(env, 10, null);

            env.undeployAll();
        }
    }

    private static class EPLSubselectGroupedUncorrelatedWHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') @name('s0')select (select sum(intPrimitive) from SupportBean#keepall group by theString having sum(intPrimitive) > 10) as c0 from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEventS0Assert(env, null);

            sendSB(env, "G1", 10);
            sendSB(env, "G2", 9);
            sendEventS0Assert(env, null);

            sendSB(env, "G2", 2);
            sendEventS0Assert(env, 11);

            sendSB(env, "G1", 3);
            sendEventS0Assert(env, null);

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedCorrelatedWHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') @name('s0')select (select sum(intPrimitive) from SupportBean#keepall where theString = s0.p00 having sum(intPrimitive) > 10) as c0 from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEventS0Assert(env, "G1", null);

            sendSB(env, "G1", 10);
            sendEventS0Assert(env, "G1", null);

            sendSB(env, "G2", 11);
            sendEventS0Assert(env, "G1", null);
            sendEventS0Assert(env, "G2", 11);

            sendSB(env, "G1", 12);
            sendEventS0Assert(env, "G1", 22);

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedUncorrelatedFiltered implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select (select sum(id) from SupportBean_S1(id < 0)#length(3)) as value from SupportBean_S0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            runAssertionSumFilter(env);

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedUncorrelatedWWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select (select sum(id) from SupportBean_S1#length(3) where id < 0) as value from SupportBean_S0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            runAssertionSumFilter(env);

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedUncorrelatedNoDataWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select p00 as c0, (select sum(intPrimitive) from SupportBean) as c1 from SupportBean_S0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            String[] fields = "c0,c1".split(",");

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", null});

            env.sendEventBean(new SupportBean("", 10));
            env.sendEventBean(new SupportBean_S0(2, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 10});

            env.sendEventBean(new SupportBean("", 20));
            env.sendEventBean(new SupportBean_S0(3, "E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 30});

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedUncorrelatedWHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') @name('s0')select *, " +
                "(select sum(intPrimitive) from SupportBean#keepall having sum(intPrimitive) > 100) as c0," +
                "exists (select sum(intPrimitive) from SupportBean#keepall having sum(intPrimitive) > 100) as c1 " +
                "from SupportBean_S0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEventS0Assert(env, fields, new Object[]{null, false});
            sendSB(env, "E1", 10);
            sendEventS0Assert(env, fields, new Object[]{null, false});
            sendSB(env, "E1", 91);
            sendEventS0Assert(env, fields, new Object[]{101, true});
            sendSB(env, "E1", 2);
            sendEventS0Assert(env, fields, new Object[]{103, true});

            env.undeployAll();
        }
    }

    public static class EPLSubselectUngroupedCorrelatedSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"id", "mycount"};
            String text = "@name('s0') select id, (select count(*) from SupportBean_S1#length(3) s1 where s1.p10 = s0.p00) as mycount from SupportBean_S0 s0";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "G1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, 0L});

            env.sendEventBean(new SupportBean_S1(200, "G2"));
            env.sendEventBean(new SupportBean_S0(2, "G2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, 1L});

            env.sendEventBean(new SupportBean_S1(201, "G2"));
            env.sendEventBean(new SupportBean_S0(3, "G2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, 2L});

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(4, "G1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{4, 0L});

            env.sendEventBean(new SupportBean_S0(5, "G2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5, 2L});

            env.sendEventBean(new SupportBean_S0(6, "G3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{6, 0L});

            env.sendEventBean(new SupportBean_S1(202, "G2"));
            env.sendEventBean(new SupportBean_S0(7, "G2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{7, 3L});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(8, "G2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{8, 3L});

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedCorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;
            AtomicInteger milestone = new AtomicInteger();

            epl = "@name('s0') select p00, " +
                "(select sum(intPrimitive) from SupportBean#keepall where theString = s0.p00) as sump00 " +
                "from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            String[] fields = "p00,sump00".split(",");

            env.sendEventBean(new SupportBean_S0(1, "T1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"T1", null});

            env.sendEventBean(new SupportBean("T1", 10));
            env.sendEventBean(new SupportBean_S0(2, "T1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"T1", 10});

            env.sendEventBean(new SupportBean("T1", 11));
            env.sendEventBean(new SupportBean_S0(3, "T1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"T1", 21});

            env.sendEventBean(new SupportBean_S0(4, "T2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"T2", null});

            env.sendEventBean(new SupportBean("T2", -2));
            env.sendEventBean(new SupportBean("T2", -7));
            env.sendEventBean(new SupportBean_S0(5, "T2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"T2", -9});
            env.undeployAll();

            // test distinct
            fields = "theString,c0,c1,c2,c3".split(",");
            epl = "@name('s0') @name('s0')select theString, " +
                "(select count(sb.intPrimitive) from SupportBean()#keepall as sb where bean.theString = sb.theString) as c0, " +
                "(select count(distinct sb.intPrimitive) from SupportBean()#keepall as sb where bean.theString = sb.theString) as c1, " +
                "(select count(sb.intPrimitive, true) from SupportBean()#keepall as sb where bean.theString = sb.theString) as c2, " +
                "(select count(distinct sb.intPrimitive, true) from SupportBean()#keepall as sb where bean.theString = sb.theString) as c3 " +
                "from SupportBean as bean";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 1L, 1L, 1L});

            env.sendEventBean(new SupportBean("E2", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 1L, 1L, 1L, 1L});

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L, 2L, 2L, 2L});

            env.sendEventBean(new SupportBean("E2", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 3L, 2L, 3L, 2L});

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedJoin3StreamKeyRangeCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String epl = "@name('s0') @name('s0')select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where theString = st2.key2 and intPrimitive between s0.p01Long and s1.p11Long) " +
                "from SupportBean_ST2#lastevent st2, SupportBean_ST0#lastevent s0, SupportBean_ST1#lastevent s1";
            tryAssertion3StreamKeyRangeCoercion(env, milestone, epl, true);

            epl = "@name('s0') select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where theString = st2.key2 and s1.p11Long >= intPrimitive and s0.p01Long <= intPrimitive) " +
                "from SupportBean_ST2#lastevent st2, SupportBean_ST0#lastevent s0, SupportBean_ST1#lastevent s1";
            tryAssertion3StreamKeyRangeCoercion(env, milestone, epl, false);

            epl = "@name('s0') select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where theString = st2.key2 and s1.p11Long > intPrimitive) " +
                "from SupportBean_ST2#lastevent st2, SupportBean_ST0#lastevent s0, SupportBean_ST1#lastevent s1";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

            env.sendEventBean(new SupportBean("G", 21));
            env.sendEventBean(new SupportBean("G", 13));
            env.sendEventBean(new SupportBean_ST2("ST2", "G", 0));
            env.sendEventBean(new SupportBean_ST0("ST0", -1L));
            env.sendEventBean(new SupportBean_ST1("ST1", 20L));
            TestCase.assertEquals(13, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

            env.undeployAll();
            epl = "@name('s0') select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where theString = st2.key2 and s1.p11Long < intPrimitive) " +
                "from SupportBean_ST2#lastevent st2, SupportBean_ST0#lastevent s0, SupportBean_ST1#lastevent s1";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

            env.sendEventBean(new SupportBean("G", 21));
            env.sendEventBean(new SupportBean("G", 13));
            env.sendEventBean(new SupportBean_ST2("ST2", "G", 0));
            env.sendEventBean(new SupportBean_ST0("ST0", -1L));
            env.sendEventBean(new SupportBean_ST1("ST1", 20L));
            TestCase.assertEquals(21, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedJoin2StreamRangeCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            // between and 'in' automatically revert the range (20 to 10 is the same as 10 to 20)
            String epl = "@name('s0') select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where intPrimitive between s0.p01Long and s1.p11Long) " +
                "from SupportBean_ST0#lastevent s0, SupportBean_ST1#lastevent s1";
            tryAssertion2StreamRangeCoercion(env, milestone, epl, true);

            epl = "@name('s0') select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where intPrimitive between s1.p11Long and s0.p01Long) " +
                "from SupportBean_ST1#lastevent s1, SupportBean_ST0#lastevent s0";
            tryAssertion2StreamRangeCoercion(env, milestone, epl, true);

            // >= and <= should not automatically revert the range
            epl = "@name('s0') select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where intPrimitive >= s0.p01Long and intPrimitive <= s1.p11Long) " +
                "from SupportBean_ST0#lastevent s0, SupportBean_ST1#lastevent s1";
            tryAssertion2StreamRangeCoercion(env, milestone, epl, false);
        }
    }

    private static class EPLSubselectUngroupedCorrelatedInWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select p00 from SupportBean_S0 as s0 where id > " +
                "(select sum(intPrimitive) from SupportBean#keepall where theString = s0.p00)";
            env.compileDeployAddListenerMile(epl, "s0", 0);

            runAssertionCorrAggWhereGreater(env);
            env.undeployAll();

            epl = "@name('s0') select p00 from SupportBean_S0 as s0 where id > " +
                "(select sum(intPrimitive) from SupportBean#keepall where theString||'X' = s0.p00||'X')";
            env.compileDeployAddListenerMile(epl, "s0", 1);

            runAssertionCorrAggWhereGreater(env);
            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedUncorrelatedInWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportMarketDataBean " +
                "where price > (select max(price) from SupportMarketDataBean(symbol='GOOG')#lastevent) ";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEventMD(env, "GOOG", 1);
            assertFalse(env.listener("s0").isInvoked());

            sendEventMD(env, "GOOG", 2);
            assertFalse(env.listener("s0").isInvoked());

            Object theEvent = sendEventMD(env, "IBM", 3);
            Assert.assertEquals(theEvent, env.listener("s0").assertOneGetNewAndReset().getUnderlying());

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedUncorrelatedInSelectClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select (select s0.id + max(s1.id) from SupportBean_S1#length(3) as s1) as value from SupportBean_S0 as s0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEventS0(env, 1);
            Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("value"));

            sendEventS1(env, 100);
            sendEventS0(env, 2);
            Assert.assertEquals(102, env.listener("s0").assertOneGetNewAndReset().get("value"));

            sendEventS1(env, 30);
            sendEventS0(env, 3);
            Assert.assertEquals(103, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedUncorrelatedInSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select (select max(id) from SupportBean_S1#length(3)) as value from SupportBean_S0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEventS0(env, 1);
            Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("value"));

            sendEventS1(env, 100);
            sendEventS0(env, 2);
            Assert.assertEquals(100, env.listener("s0").assertOneGetNewAndReset().get("value"));

            sendEventS1(env, 200);
            sendEventS0(env, 3);
            Assert.assertEquals(200, env.listener("s0").assertOneGetNewAndReset().get("value"));

            sendEventS1(env, 190);
            sendEventS0(env, 4);
            Assert.assertEquals(200, env.listener("s0").assertOneGetNewAndReset().get("value"));

            sendEventS1(env, 180);
            sendEventS0(env, 5);
            Assert.assertEquals(200, env.listener("s0").assertOneGetNewAndReset().get("value"));

            sendEventS1(env, 170);   // note event leaving window
            sendEventS0(env, 6);
            Assert.assertEquals(190, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectUngroupedUncorrelatedTwoAggStopStart implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select (select avg(id) + max(id) from SupportBean_S1#length(3)) as value from SupportBean_S0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEventS0(env, 1);
            Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("value"));

            sendEventS1(env, 100);
            sendEventS0(env, 2);
            Assert.assertEquals(200.0, env.listener("s0").assertOneGetNewAndReset().get("value"));

            sendEventS1(env, 200);
            sendEventS0(env, 3);
            Assert.assertEquals(350.0, env.listener("s0").assertOneGetNewAndReset().get("value"));

            SupportListener listener = env.listener("s0");
            env.undeployAll();
            sendEventS1(env, 10000);
            sendEventS0(env, 4);
            assertFalse(listener.isInvoked());
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEventS1(env, 10);
            sendEventS0(env, 5);
            Assert.assertEquals(20.0, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.undeployAll();
        }
    }

    private static void runAssertionSumFilter(RegressionEnvironment env) {
        sendEventS0(env, 1);
        Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("value"));

        sendEventS1(env, 1);
        sendEventS0(env, 2);
        Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("value"));

        sendEventS1(env, 0);
        sendEventS0(env, 3);
        Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("value"));

        sendEventS1(env, -1);
        sendEventS0(env, 4);
        Assert.assertEquals(-1, env.listener("s0").assertOneGetNewAndReset().get("value"));

        sendEventS1(env, -3);
        sendEventS0(env, 5);
        Assert.assertEquals(-4, env.listener("s0").assertOneGetNewAndReset().get("value"));

        sendEventS1(env, -5);
        sendEventS0(env, 6);
        Assert.assertEquals(-9, env.listener("s0").assertOneGetNewAndReset().get("value"));

        sendEventS1(env, -2);   // note event leaving window
        sendEventS0(env, 6);
        Assert.assertEquals(-10, env.listener("s0").assertOneGetNewAndReset().get("value"));
    }

    private static void tryAssertion2StreamRangeCoercion(RegressionEnvironment env, AtomicInteger milestone, String epl, boolean isHasRangeReversal) {
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        env.sendEventBean(new SupportBean_ST0("ST01", 10L));
        env.sendEventBean(new SupportBean_ST1("ST11", 20L));
        env.sendEventBean(new SupportBean("E1", 9));
        env.sendEventBean(new SupportBean("E1", 21));
        TestCase.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("sumi")); // range 10 to 20

        env.sendEventBean(new SupportBean("E1", 13));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_ST0("ST0_1", 10L));  // range 10 to 20
        TestCase.assertEquals(13, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST1("ST1_1", 13L));  // range 10 to 13
        TestCase.assertEquals(13, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST0("ST0_2", 13L));  // range 13 to 13
        TestCase.assertEquals(13, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E2", 14));
        env.sendEventBean(new SupportBean("E3", 12));
        env.sendEventBean(new SupportBean_ST1("ST1_3", 13L));  // range 13 to 13
        TestCase.assertEquals(13, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST1("ST1_4", 20L));  // range 13 to 20
        TestCase.assertEquals(27, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_ST0("ST0_3", 11L));  // range 11 to 20
        TestCase.assertEquals(39, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST0("ST0_4", null));  // range null to 16
        TestCase.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_ST1("ST1_5", null));  // range null to null
        TestCase.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST0("ST0_5", 20L));  // range 20 to null
        TestCase.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST1("ST1_6", 13L));  // range 20 to 13
        if (isHasRangeReversal) {
            TestCase.assertEquals(27, env.listener("s0").assertOneGetNewAndReset().get("sumi"));
        } else {
            TestCase.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("sumi"));
        }

        env.undeployAll();
    }

    private static void tryAssertion3StreamKeyRangeCoercion(RegressionEnvironment env, AtomicInteger milestone, String epl, boolean isHasRangeReversal) {
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        env.sendEventBean(new SupportBean("G", -1));
        env.sendEventBean(new SupportBean("G", 9));
        env.sendEventBean(new SupportBean("G", 21));
        env.sendEventBean(new SupportBean("G", 13));
        env.sendEventBean(new SupportBean("G", 17));
        env.sendEventBean(new SupportBean_ST2("ST21", "X", 0));
        env.sendEventBean(new SupportBean_ST0("ST01", 10L));
        env.sendEventBean(new SupportBean_ST1("ST11", 20L));
        TestCase.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("sumi")); // range 10 to 20

        env.sendEventBean(new SupportBean_ST2("ST22", "G", 0));
        TestCase.assertEquals(30, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST0("ST01", 0L));    // range 0 to 20
        TestCase.assertEquals(39, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST2("ST21", null, 0));
        TestCase.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST2("ST21", "G", 0));
        TestCase.assertEquals(39, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST1("ST11", 100L));   // range 0 to 100
        TestCase.assertEquals(60, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST1("ST11", null));   // range 0 to null
        TestCase.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST0("ST01", null));    // range null to null
        TestCase.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST1("ST11", -1L));   // range null to -1
        TestCase.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("sumi"));

        env.sendEventBean(new SupportBean_ST0("ST01", 10L));    // range 10 to -1
        if (isHasRangeReversal) {
            TestCase.assertEquals(8, env.listener("s0").assertOneGetNewAndReset().get("sumi"));
        } else {
            TestCase.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("sumi"));
        }

        env.undeployAll();
    }

    private static void runAssertionCorrAggWhereGreater(RegressionEnvironment env) {
        String[] fields = "p00".split(",");

        env.sendEventBean(new SupportBean_S0(1, "T1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean("T1", 10));

        env.sendEventBean(new SupportBean_S0(10, "T1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(11, "T1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"T1"});

        env.sendEventBean(new SupportBean("T1", 11));
        env.sendEventBean(new SupportBean_S0(21, "T1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(22, "T1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"T1"});
    }

    private static void sendEventS0(RegressionEnvironment env, int id) {
        env.sendEventBean(new SupportBean_S0(id));
    }

    private static void sendEventS0(RegressionEnvironment env, int id, String p00) {
        env.sendEventBean(new SupportBean_S0(id, p00));
    }

    private static void sendEventS1(RegressionEnvironment env, int id, String p10, String p11) {
        env.sendEventBean(new SupportBean_S1(id, p10, p11));
    }

    private static void sendEventS1(RegressionEnvironment env, int id) {
        env.sendEventBean(new SupportBean_S1(id));
    }

    private static Object sendEventMD(RegressionEnvironment env, String symbol, double price) {
        Object theEvent = new SupportMarketDataBean(symbol, price, 0L, "");
        env.sendEventBean(theEvent);
        return theEvent;
    }

    private static void sendSB(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }

    private static void sendEventS0Assert(RegressionEnvironment env, Object expected) {
        sendEventS0Assert(env, 0, expected);
    }

    private static void sendEventS0Assert(RegressionEnvironment env, int id, Object expected) {
        sendEventS0(env, id, null);
        Assert.assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
    }

    private static void sendEventS0Assert(RegressionEnvironment env, String p00, Object expected) {
        sendEventS0(env, 0, p00);
        Assert.assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
    }

    private static void sendEventS0Assert(RegressionEnvironment env, String[] fields, Object[] expected) {
        env.sendEventBean(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);
    }
}
