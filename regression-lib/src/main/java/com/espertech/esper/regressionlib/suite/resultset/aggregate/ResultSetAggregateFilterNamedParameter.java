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
package com.espertech.esper.regressionlib.suite.resultset.aggregate;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ResultSetAggregateFilterNamedParameter {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAggregateFirstAggSODA(false));
        execs.add(new ResultSetAggregateFirstAggSODA(true));
        execs.add(new ResultSetAggregateMethodAggSQLAll());
        execs.add(new ResultSetAggregateMethodAggSQLMixedFilter());
        execs.add(new ResultSetAggregateMethodAggLeaving());
        execs.add(new ResultSetAggregateMethodAggNth());
        execs.add(new ResultSetAggregateMethodAggRateUnbound());
        execs.add(new ResultSetAggregateMethodAggRateBound());
        execs.add(new ResultSetAggregateAccessAggLinearBound(false));
        execs.add(new ResultSetAggregateAccessAggLinearBound(true));
        execs.add(new ResultSetAggregateAccessAggLinearUnbound(false));
        execs.add(new ResultSetAggregateAccessAggLinearUnbound(true));
        execs.add(new ResultSetAggregateAccessAggLinearWIndex());
        execs.add(new ResultSetAggregateAccessAggLinearBoundMixedFilter());
        execs.add(new ResultSetAggregateAccessAggSortedBound(false));
        execs.add(new ResultSetAggregateAccessAggSortedBound(true));
        execs.add(new ResultSetAggregateAccessAggSortedUnbound(false));
        execs.add(new ResultSetAggregateAccessAggSortedUnbound(true));
        execs.add(new ResultSetAggregateAccessAggSortedMulticriteria());
        execs.add(new ResultSetAggregateAuditAndReuse());
        execs.add(new ResultSetAggregateFilterNamedParamInvalid());
        execs.add(new ResultSetAggregateMethodPlugIn());
        execs.add(new ResultSetAggregateAccessAggPlugIn());
        execs.add(new ResultSetAggregateIntoTable(false));
        execs.add(new ResultSetAggregateIntoTable(true));
        execs.add(new ResultSetAggregateIntoTableCountMinSketch());
        return execs;
    }

    private static class ResultSetAggregateAccessAggPlugIn implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') select eventsAsList(theString, filter:theString like 'A%') as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            sendEventAssertEventsAsList(env, "X1", "[]");
            sendEventAssertEventsAsList(env, "A1", "[SupportBean(A1, 0)]");
            sendEventAssertEventsAsList(env, "A2", "[SupportBean(A1, 0), SupportBean(A2, 0)]");
            sendEventAssertEventsAsList(env, "X2", "[SupportBean(A1, 0), SupportBean(A2, 0)]");

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMethodPlugIn implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0".split(",");
            String epl = "@name('s0') select concatMethodAgg(theString, filter:theString like 'A%') as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            sendEventAssert(env, "X1", 0, fields, new Object[]{""});
            sendEventAssert(env, "A1", 0, fields, new Object[]{"A1"});
            sendEventAssert(env, "A2", 0, fields, new Object[]{"A1 A2"});
            sendEventAssert(env, "X2", 0, fields, new Object[]{"A1 A2"});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateIntoTableCountMinSketch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table WordCountTable(wordcms countMinSketch());\n" +
                "into table WordCountTable select countMinSketchAdd(theString, filter:intPrimitive > 0) as wordcms from SupportBean;\n" +
                "@name('s0') select WordCountTable.wordcms.countMinSketchFrequency(p00) as c0 from SupportBean_S0;\n";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "hello", 0);
            sendEventAssertCount(env, "hello", 0L);

            sendEvent(env, "name", 1);
            sendEventAssertCount(env, "name", 1L);

            sendEvent(env, "name", 0);
            sendEventAssertCount(env, "name", 1L);

            sendEvent(env, "name", 1);
            sendEventAssertCount(env, "name", 2L);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMethodAggRateBound implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "myrate,myqtyrate".split(",");
            String epl = "@name('s0') select " +
                "rate(longPrimitive, filter:theString like 'A%') as myrate, " +
                "rate(longPrimitive, intPrimitive, filter:theString like 'A%') as myqtyrate " +
                "from SupportBean#length(3)";
            env.compileDeploy(epl).addListener("s0");

            sendEventWLong(env, "X1", 1000, 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            sendEventWLong(env, "X2", 1200, 0);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.milestone(0);

            sendEventWLong(env, "X2", 1300, 0);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            sendEventWLong(env, "A1", 1000, 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.milestone(1);

            sendEventWLong(env, "A2", 1200, 0);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            sendEventWLong(env, "A3", 1300, 0);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            sendEventWLong(env, "A4", 1500, 14);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3 * 1000 / 500d, 14 * 1000 / 500d});

            env.milestone(2);

            sendEventWLong(env, "A5", 2000, 11);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3 * 1000 / 800d, 25 * 1000 / 800d});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMethodAggRateUnbound implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.advanceTime(0);

            String[] fields = "c0".split(",");
            String epl = "@name('s0') select rate(1, filter:theString like 'A%') as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            sendEventAssertIsolated(env, "X1", 0, fields, new Object[]{null});
            sendEventAssertIsolated(env, "A1", 1, fields, new Object[]{null});

            env.milestone(0);

            env.advanceTime(1000);
            sendEventAssertIsolated(env, "X2", 2, fields, new Object[]{null});
            sendEventAssertIsolated(env, "A2", 2, fields, new Object[]{1.0});

            env.milestone(1);

            sendEventAssertIsolated(env, "A3", 3, fields, new Object[]{2.0});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMethodAggNth implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            String epl = "@name('s0') select nth(intPrimitive, 1, filter:theString like 'A%') as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            sendEventAssert(env, "X1", 0, fields, new Object[]{null});
            sendEventAssert(env, "X2", 0, fields, new Object[]{null});

            env.milestone(0);

            sendEventAssert(env, "A3", 1, fields, new Object[]{null});
            sendEventAssert(env, "A4", 2, fields, new Object[]{1});

            env.milestone(1);

            sendEventAssert(env, "X3", 0, fields, new Object[]{1});
            sendEventAssert(env, "A5", 3, fields, new Object[]{2});

            env.milestone(2);

            sendEventAssert(env, "X4", 0, fields, new Object[]{2});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMethodAggLeaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') select " +
                "leaving(filter:intPrimitive=1) as c0," +
                "leaving(filter:intPrimitive=2) as c1" +
                " from SupportBean#length(2)";
            env.compileDeploy(epl).addListener("s0");

            sendEventAssert(env, "E1", 2, fields, new Object[]{false, false});

            env.milestone(0);

            sendEventAssert(env, "E2", 1, fields, new Object[]{false, false});

            env.milestone(1);

            sendEventAssert(env, "E3", 3, fields, new Object[]{false, true});

            env.milestone(2);

            sendEventAssert(env, "E4", 4, fields, new Object[]{true, true});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateAuditAndReuse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "sum(intPrimitive, filter:intPrimitive=1) as c0, sum(intPrimitive, filter:intPrimitive=1) as c1, " +
                "window(*, filter:intPrimitive=1) as c2, window(*, filter:intPrimitive=1) as c3 " +
                " from SupportBean#length(3)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));

            env.undeployAll();
        }

    }

    private static class ResultSetAggregateFilterNamedParamInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // invalid filter expression name parameter: multiple values
            SupportMessageAssertUtil.tryInvalidCompile(env, "select sum(intPrimitive, filter:(intPrimitive, doublePrimitive)) from SupportBean",
                "Failed to validate select-clause expression 'sum(intPrimitive,filter:(intPrimiti...(55 chars)': Filter named parameter requires a single expression returning a boolean-typed value");

            // multiple filter expressions
            SupportMessageAssertUtil.tryInvalidCompile(env, "select sum(intPrimitive, intPrimitive > 0, filter:intPrimitive < 0) from SupportBean",
                "Failed to validate select-clause expression 'sum(intPrimitive,intPrimitive>0,fil...(54 chars)': Only a single filter expression can be provided");

            // invalid filter expression name parameter: not returning boolean
            SupportMessageAssertUtil.tryInvalidCompile(env, "select sum(intPrimitive, filter:intPrimitive) from SupportBean",
                "Failed to validate select-clause expression 'sum(intPrimitive,filter:intPrimitive)': Filter named parameter requires a single expression returning a boolean-typed value");

            // create-table does not allow filters
            SupportMessageAssertUtil.tryInvalidCompile(env, "create table MyTable(totals sum(int, filter:true))",
                "Failed to validate table-column expression 'sum(int,filter:true)': The 'group_by' and 'filter' parameter is not allowed in create-table statements");

            // invalid correlated subquery
            SupportMessageAssertUtil.tryInvalidCompile(env, "select (select sum(intPrimitive, filter:s0.p00='a') from SupportBean) from SupportBean_S0 as s0",
                "Failed to plan subquery number 1 querying SupportBean: Subselect aggregation functions cannot aggregate across correlated properties");
        }
    }

    private static class ResultSetAggregateIntoTable implements RegressionExecution {
        private final boolean join;

        public ResultSetAggregateIntoTable(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplTable =
                "create table MyTable(\n" +
                    "totalA sum(int, true),\n" +
                    "totalB sum(int, true),\n" +
                    "winA window(*) @type(SupportBean),\n" +
                    "winB window(*) @type(SupportBean),\n" +
                    "sortedA sorted(intPrimitive) @type(SupportBean),\n" +
                    "sortedB sorted(intPrimitive) @type(SupportBean)" +
                    ")";
            env.compileDeploy(eplTable, path);

            String eplInto = "into table MyTable select\n" +
                "sum(intPrimitive, filter: theString like 'A%') as totalA,\n" +
                "sum(intPrimitive, filter: theString like 'B%') as totalB,\n" +
                "window(sb, filter: theString like 'A%') as winA,\n" +
                "window(sb, filter: theString like 'B%') as winB,\n" +
                "sorted(sb, filter: theString like 'A%') as sortedA,\n" +
                "sorted(sb, filter: theString like 'B%') as sortedB\n" +
                "from " + (join ? "SupportBean_S1#lastevent, SupportBean#keepall as sb;\n" : "SupportBean as sb");
            env.compileDeploy(eplInto, path);

            String eplSelect = "@name('s0') select MyTable.totalA as ta , MyTable.totalB as tb, MyTable.winA as wa, MyTable.winB as wb, MyTable.sortedA as sa, MyTable.sortedB as sb from SupportBean_S0";
            env.compileDeploy(eplSelect, path).addListener("s0");

            env.sendEventBean(new SupportBean_S1(0));

            sendEvent(env, "X1", 1);
            sendEventAssertInfoTable(env, null, null, null, null, null, null);

            env.milestone(0);

            SupportBean a1 = sendEvent(env, "A1", 1);
            sendEventAssertInfoTable(env, 1, null, new SupportBean[]{a1}, null, new SupportBean[]{a1}, null);

            env.milestone(1);

            SupportBean b2 = sendEvent(env, "B2", 20);
            sendEventAssertInfoTable(env, 1, 20, new SupportBean[]{a1}, new SupportBean[]{b2}, new SupportBean[]{a1}, new SupportBean[]{b2});

            SupportBean a3 = sendEvent(env, "A3", 10);
            sendEventAssertInfoTable(env, 11, 20, new SupportBean[]{a1, a3}, new SupportBean[]{b2}, new SupportBean[]{a1, a3}, new SupportBean[]{b2});

            env.milestone(2);

            SupportBean b4 = sendEvent(env, "B4", 2);
            sendEventAssertInfoTable(env, 11, 22, new SupportBean[]{a1, a3}, new SupportBean[]{b2, b4}, new SupportBean[]{a1, a3}, new SupportBean[]{b4, b2});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateAccessAggLinearWIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            String epl = "@name('s0') select " +
                "first(intPrimitive, 0, filter:theString like 'A%') as c0," +
                "first(intPrimitive, 1, filter:theString like 'A%') as c1," +
                "last(intPrimitive, 0, filter:theString like 'A%') as c2," +
                "last(intPrimitive, 1, filter:theString like 'A%') as c3" +
                " from SupportBean#length(3)";
            env.compileDeploy(epl).addListener("s0");

            sendEventAssert(env, "B1", 1, fields, new Object[]{null, null, null, null});
            sendEventAssert(env, "A2", 2, fields, new Object[]{2, null, 2, null});
            sendEventAssert(env, "A3", 3, fields, new Object[]{2, 3, 3, 2});

            env.milestone(0);

            sendEventAssert(env, "A4", 4, fields, new Object[]{2, 3, 4, 3});
            sendEventAssert(env, "B2", 2, fields, new Object[]{3, 4, 4, 3});

            env.milestone(1);

            sendEventAssert(env, "B3", 3, fields, new Object[]{4, null, 4, null});
            sendEventAssert(env, "B4", 4, fields, new Object[]{null, null, null, null});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateAccessAggSortedBound implements RegressionExecution {
        private final boolean join;

        public ResultSetAggregateAccessAggSortedBound(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "aMaxby,aMinby,aSorted,bMaxby,bMinby,bSorted".split(",");
            String epl = "@name('s0') select " +
                "maxby(intPrimitive, filter:theString like 'A%').theString as aMaxby," +
                "minby(intPrimitive, filter:theString like 'A%').theString as aMinby," +
                "sorted(intPrimitive, filter:theString like 'A%') as aSorted," +
                "maxby(intPrimitive, filter:theString like 'B%').theString as bMaxby," +
                "minby(intPrimitive, filter:theString like 'B%').theString as bMinby," +
                "sorted(intPrimitive, filter:theString like 'B%') as bSorted" +
                " from " + (join ? "SupportBean_S1#lastevent, SupportBean#length(4)" : "SupportBean#length(4)");
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S1(0));

            env.milestone(0);

            SupportBean b1 = sendEvent(env, "B1", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, "B1", "B1", new SupportBean[]{b1}});

            SupportBean a10 = sendEvent(env, "A10", 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A10", "A10", new SupportBean[]{a10}, "B1", "B1", new SupportBean[]{b1}});

            env.milestone(1);

            SupportBean b2 = sendEvent(env, "B2", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A10", "A10", new SupportBean[]{a10}, "B2", "B1", new SupportBean[]{b1, b2}});

            SupportBean a5 = sendEvent(env, "A5", 5);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A10", "A5", new SupportBean[]{a5, a10}, "B2", "B1", new SupportBean[]{b1, b2}});

            SupportBean a15 = sendEvent(env, "A15", 15);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A15", "A5", new SupportBean[]{a5, a10, a15}, "B2", "B2", new SupportBean[]{b2}});

            sendEvent(env, "X3", 3);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A15", "A5", new SupportBean[]{a5, a15}, "B2", "B2", new SupportBean[]{b2}});

            env.milestone(2);

            sendEvent(env, "X4", 4);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A15", "A5", new SupportBean[]{a5, a15}, null, null, null});

            sendEvent(env, "X5", 5);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A15", "A15", new SupportBean[]{a15}, null, null, null});

            sendEvent(env, "X6", 6);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null, null});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateAccessAggSortedMulticriteria implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "aSorted,bSorted".split(",");
            String epl = "@name('s0') select " +
                "sorted(intPrimitive, doublePrimitive, filter:theString like 'A%') as aSorted," +
                "sorted(intPrimitive, doublePrimitive, filter:theString like 'B%') as bSorted" +
                " from SupportBean#keepall";
            env.compileDeploy(epl).addListener("s0");

            SupportBean b1 = sendEvent(env, "B1", 1, 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, new SupportBean[]{b1}});

            SupportBean a1 = sendEvent(env, "A1", 100, 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{new SupportBean[]{a1}, new SupportBean[]{b1}});

            env.milestone(0);

            SupportBean b2 = sendEvent(env, "B2", 1, 4);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{new SupportBean[]{a1}, new SupportBean[]{b2, b1}});

            SupportBean a2 = sendEvent(env, "A2", 100, 3);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{new SupportBean[]{a1, a2}, new SupportBean[]{b2, b1}});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateAccessAggSortedUnbound implements RegressionExecution {
        private final boolean join;

        public ResultSetAggregateAccessAggSortedUnbound(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "aMaxby,aMaxbyever,aMinby,aMinbyever".split(",");
            String epl = "@name('s0') select " +
                "maxby(intPrimitive, filter:theString like 'A%').theString as aMaxby," +
                "maxbyever(intPrimitive, filter:theString like 'A%').theString as aMaxbyever," +
                "minby(intPrimitive, filter:theString like 'A%').theString as aMinby," +
                "minbyever(intPrimitive, filter:theString like 'A%').theString as aMinbyever" +
                " from " + (join ? "SupportBean_S1#lastevent, SupportBean#keepall" : "SupportBean");
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S1(0));

            sendEventAssert(env, "B1", 1, fields, new Object[]{null, null, null, null});

            env.milestone(0);

            sendEventAssert(env, "A10", 10, fields, new Object[]{"A10", "A10", "A10", "A10"});
            sendEventAssert(env, "A5", 5, fields, new Object[]{"A10", "A10", "A5", "A5"});

            env.milestone(1);

            sendEventAssert(env, "A15", 15, fields, new Object[]{"A15", "A15", "A5", "A5"});

            env.milestone(2);

            sendEventAssert(env, "B1000", 1000, fields, new Object[]{"A15", "A15", "A5", "A5"});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateAccessAggLinearBound implements RegressionExecution {
        private final boolean join;

        public ResultSetAggregateAccessAggLinearBound(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "aFirst,aLast,aWindow,bFirst,bLast,bWindow".split(",");
            String epl = "@name('s0') select " +
                "first(intPrimitive, filter:theString like 'A%') as aFirst," +
                "last(intPrimitive, filter:theString like 'A%') as aLast," +
                "window(intPrimitive, filter:theString like 'A%') as aWindow," +
                "first(intPrimitive, filter:theString like 'B%') as bFirst," +
                "last(intPrimitive, filter:theString like 'B%') as bLast," +
                "window(intPrimitive, filter:theString like 'B%') as bWindow" +
                " from " + (join ? "SupportBean_S1#lastevent, SupportBean#length(5)" : "SupportBean#length(5)");
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S1(0));

            sendEventAssert(env, "X1", 1, fields, new Object[]{null, null, null, null, null, null});
            sendEventAssert(env, "B2", 2, fields, new Object[]{null, null, null, 2, 2, new int[]{2}});

            env.milestone(0);

            sendEventAssert(env, "B3", 3, fields, new Object[]{null, null, null, 2, 3, new int[]{2, 3}});
            sendEventAssert(env, "A4", 4, fields, new Object[]{4, 4, new int[]{4}, 2, 3, new int[]{2, 3}});
            sendEventAssert(env, "B5", 5, fields, new Object[]{4, 4, new int[]{4}, 2, 5, new int[]{2, 3, 5}});

            env.milestone(1);

            sendEventAssert(env, "A6", 6, fields, new Object[]{4, 6, new int[]{4, 6}, 2, 5, new int[]{2, 3, 5}});
            sendEventAssert(env, "X2", 7, fields, new Object[]{4, 6, new int[]{4, 6}, 3, 5, new int[]{3, 5}});
            sendEventAssert(env, "X3", 8, fields, new Object[]{4, 6, new int[]{4, 6}, 5, 5, new int[]{5}});

            env.milestone(2);

            sendEventAssert(env, "X4", 9, fields, new Object[]{6, 6, new int[]{6}, 5, 5, new int[]{5}});
            sendEventAssert(env, "X5", 10, fields, new Object[]{6, 6, new int[]{6}, null, null, null});
            sendEventAssert(env, "X6", 11, fields, new Object[]{null, null, null, null, null, null});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateAccessAggLinearUnbound implements RegressionExecution {
        private final boolean join;

        public ResultSetAggregateAccessAggLinearUnbound(boolean join) {
            this.join = join;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "aFirst,aFirstever,aLast,aLastever,aCountever".split(",");
            String epl = "@name('s0') select " +
                "first(intPrimitive, filter:theString like 'A%') as aFirst," +
                "firstever(intPrimitive, filter:theString like 'A%') as aFirstever," +
                "last(intPrimitive, filter:theString like 'A%') as aLast," +
                "lastever(intPrimitive, filter:theString like 'A%') as aLastever," +
                "countever(intPrimitive, filter:theString like 'A%') as aCountever" +
                " from " + (join ? "SupportBean_S1#lastevent, SupportBean#keepall" : "SupportBean");
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean_S1(0));

            env.milestone(1);

            sendEventAssert(env, "X0", 0, fields, new Object[]{null, null, null, null, 0L});
            sendEventAssert(env, "A1", 1, fields, new Object[]{1, 1, 1, 1, 1L});

            env.milestone(2);

            sendEventAssert(env, "X2", 2, fields, new Object[]{1, 1, 1, 1, 1L});
            sendEventAssert(env, "A3", 3, fields, new Object[]{1, 1, 3, 3, 2L});

            env.milestone(3);

            sendEventAssert(env, "X4", 4, fields, new Object[]{1, 1, 3, 3, 2L});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateAccessAggLinearBoundMixedFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "@name('s0') select " +
                "window(sb, filter:theString like 'A%') as c0," +
                "window(sb) as c1," +
                "window(filter:theString like 'B%', sb) as c2" +
                " from SupportBean#keepall as sb";
            env.compileDeploy(epl).addListener("s0");

            SupportBean x1 = sendEvent(env, "X1", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, new SupportBean[]{x1}, null});

            SupportBean a2 = sendEvent(env, "A2", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{new SupportBean[]{a2}, new SupportBean[]{x1, a2}, null});

            env.milestone(0);

            SupportBean b3 = sendEvent(env, "B3", 3);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{new SupportBean[]{a2}, new SupportBean[]{x1, a2, b3}, new SupportBean[]{b3}});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMethodAggSQLMixedFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            String epl = "@name('s0') select " +
                "sum(intPrimitive, filter:theString like 'A%') as c0," +
                "sum(intPrimitive) as c1," +
                "sum(filter:theString like 'B%', intPrimitive) as c2" +
                " from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            sendEventAssert(env, "X1", 1, fields, new Object[]{null, 1, null});
            sendEventAssert(env, "B2", 20, fields, new Object[]{null, 1 + 20, 20});

            env.milestone(0);

            sendEventAssert(env, "A3", 300, fields, new Object[]{300, 1 + 20 + 300, 20});
            sendEventAssert(env, "X1", 2, fields, new Object[]{300, 1 + 20 + 300 + 2, 20});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMethodAggSQLAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "avedev(doublePrimitive, filter:intPrimitive between 1 and 3) as cAvedev," +
                "avg(doublePrimitive, filter:intPrimitive between 1 and 3) as cAvg, " +
                "count(*, filter:intPrimitive between 1 and 3) as cCount, " +
                "max(doublePrimitive, filter:intPrimitive between 1 and 3) as cMax, " +
                "fmax(doublePrimitive, filter:intPrimitive between 1 and 3) as cFmax, " +
                "maxever(doublePrimitive, filter:intPrimitive between 1 and 3) as cMaxever, " +
                "fmaxever(doublePrimitive, filter:intPrimitive between 1 and 3) as cFmaxever, " +
                "median(doublePrimitive, filter:intPrimitive between 1 and 3) as cMedian, " +
                "min(doublePrimitive, filter:intPrimitive between 1 and 3) as cMin, " +
                "fmin(doublePrimitive, filter:intPrimitive between 1 and 3) as cFmin, " +
                "minever(doublePrimitive, filter:intPrimitive between 1 and 3) as cMinever, " +
                "fminever(doublePrimitive, filter:intPrimitive between 1 and 3) as cFminever, " +
                "stddev(doublePrimitive, filter:intPrimitive between 1 and 3) as cStddev, " +
                "sum(doublePrimitive, filter:intPrimitive between 1 and 3) as cSum " +
                "from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            sendEventAssertSQLFuncs(env, "E1", 0, 50, null, null, 0L, null, null, null, null, null, null, null, null, null, null, null);
            env.milestone(0);

            sendEventAssertSQLFuncs(env, "E2", 2, 10, 0.0, 10d, 1L, 10d, 10d, 10d, 10d, 10.0, 10d, 10d, 10d, 10d, null, 10d);

            env.milestone(1);

            sendEventAssertSQLFuncs(env, "E3", 100, 10, 0.0, 10d, 1L, 10d, 10d, 10d, 10d, 10.0, 10d, 10d, 10d, 10d, null, 10d);

            env.milestone(2);

            sendEventAssertSQLFuncs(env, "E4", 1, 20, 5.0, 15d, 2L, 20d, 20d, 20d, 20d, 15.0, 10d, 10d, 10d, 10d, 7.0710678118654755, 30d);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateFirstAggSODA implements RegressionExecution {
        private final boolean soda;

        public ResultSetAggregateFirstAggSODA(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') select " +
                "first(*,filter:intPrimitive=1).theString as c0, " +
                "first(*,filter:intPrimitive=2).theString as c1" +
                " from SupportBean#length(3)";
            env.compileDeploy(soda, epl).addListener("s0");

            sendEventAssert(env, "E1", 3, fields, new Object[]{null, null});
            sendEventAssert(env, "E2", 2, fields, new Object[]{null, "E2"});
            sendEventAssert(env, "E3", 1, fields, new Object[]{"E3", "E2"});

            env.milestone(0);

            sendEventAssert(env, "E4", 2, fields, new Object[]{"E3", "E2"});
            sendEventAssert(env, "E5", -1, fields, new Object[]{"E3", "E4"});
            sendEventAssert(env, "E6", -1, fields, new Object[]{null, "E4"});

            env.milestone(1);

            sendEventAssert(env, "E7", -1, fields, new Object[]{null, null});

            env.undeployAll();
        }
    }

    private static void sendEventAssertSQLFuncs(RegressionEnvironment env, String theString, int intPrimitive, double doublePrimitive,
                                                Object cAvedev, Object cAvg, Object cCount,
                                                Object cMax, Object cFmax, Object cMaxever, Object cFmaxever,
                                                Object cMedian,
                                                Object cMin, Object cFmin, Object cMinever, Object cFminever,
                                                Object cStddev, Object cSum) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        env.sendEventBean(sb);
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(cAvedev, event.get("cAvedev"));
        assertEquals(cAvg, event.get("cAvg"));
        assertEquals(cCount, event.get("cCount"));
        assertEquals(cMax, event.get("cMax"));
        assertEquals(cFmax, event.get("cFmax"));
        assertEquals(cMaxever, event.get("cMaxever"));
        assertEquals(cFmaxever, event.get("cFmaxever"));
        assertEquals(cMedian, event.get("cMedian"));
        assertEquals(cMin, event.get("cMin"));
        assertEquals(cFmin, event.get("cFmin"));
        assertEquals(cMinever, event.get("cMinever"));
        assertEquals(cFminever, event.get("cFminever"));
        assertEquals(cStddev, event.get("cStddev"));
        assertEquals(cSum, event.get("cSum"));
    }

    private static void sendEventAssertEventsAsList(RegressionEnvironment env, String theString, String expected) {
        sendEvent(env, theString, 0);
        List value = (List) env.listener("s0").assertOneGetNewAndReset().get("c0");
        assertEquals(expected, value.toString());
    }

    private static void sendEventAssert(RegressionEnvironment env, String theString, int intPrimitive, String[] fields, Object[] expected) {
        sendEvent(env, theString, intPrimitive);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);
    }

    private static void sendEventAssertIsolated(RegressionEnvironment env, String theString, int intPrimitive, String[] fields, Object[] expected) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);
    }

    private static SupportBean sendEvent(RegressionEnvironment env, String theString, int intPrimitive) {
        return sendEvent(env, theString, intPrimitive, -1);
    }

    private static SupportBean sendEvent(RegressionEnvironment env, String theString, int intPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        env.sendEventBean(sb);
        return sb;
    }

    private static void sendEventAssertInfoTable(RegressionEnvironment env, Object ta, Object tb, Object wa, Object wb, Object sa, Object sb) {
        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "ta,tb,wa,wb,sa,sb".split(","), new Object[]{ta, tb, wa, wb, sa, sb});
    }

    private static void sendEventAssertCount(RegressionEnvironment env, String p00, Object expected) {
        env.sendEventBean(new SupportBean_S0(0, p00));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{expected});
    }

    private static void sendEventWLong(RegressionEnvironment env, String theString, long longPrimitive, int intPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
    }
}
