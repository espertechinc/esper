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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.espertech.esper.regressionlib.suite.resultset.aggregate.ResultSetAggregationMethodSorted.assertType;
import static org.junit.Assert.assertEquals;

public class ResultSetAggregationMethodWindow {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAggregateWindowNonTable());
        execs.add(new ResultSetAggregateWindowTableAccess());
        execs.add(new ResultSetAggregateWindowTableIdentWCount());
        execs.add(new ResultSetAggregateWindowListReference());
        execs.add(new ResultSetAggregateWindowInvalid());
        return execs;
    }

    private static class ResultSetAggregateWindowListReference implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(windowcol window(*) @type('SupportBean'));\n" +
                "into table MyTable select window(*) as windowcol from SupportBean;\n" +
                "@name('s0') select MyTable.windowcol.listReference() as collref from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            assertType(env, EPTypeClassParameterized.from(List.class, EventBean.EPTYPE), "collref");

            SupportBean sb1 = makeSendBean(env, "E1", 10);
            SupportBean sb2 = makeSendBean(env, "E1", 10);
            env.sendEventBean(new SupportBean_S0(-1));
            env.assertEventNew("s0", event -> {
                List<EventBean> events = (List<EventBean>) event.get("collref");
                assertEquals(2, events.size());
                EPAssertionUtil.assertEqualsExactOrder(new Object[]{events.get(0).getUnderlying(), events.get(1).getUnderlying()}, new SupportBean[]{sb1, sb2});
            });

            env.milestone(0);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateWindowTableIdentWCount implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(windowcol window(*) @type('SupportBean'));\n" +
                "into table MyTable select window(*) as windowcol from SupportBean;\n" +
                "@name('s0') select windowcol.first(intPrimitive) as c0, windowcol.last(intPrimitive) as c1, windowcol.countEvents() as c2 from SupportBean_S0, MyTable";
            env.compileDeploy(epl).addListener("s0");

            assertType(env, EPTypePremade.INTEGERBOXED.getEPType(), "c0,c1,c2");

            makeSendBean(env, "E1", 10);
            makeSendBean(env, "E2", 20);
            makeSendBean(env, "E3", 30);

            env.sendEventBean(new SupportBean_S0(-1));
            env.assertPropsNew("s0", "c0,c1,c2".split(","), new Object[]{10, 30, 3});

            env.milestone(0);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateWindowTableAccess implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(windowcol window(*) @type('SupportBean'));\n" +
                "into table MyTable select window(*) as windowcol from SupportBean#length(2);\n" +
                "@name('s0') select MyTable.windowcol.first() as c0, MyTable.windowcol.last() as c1 from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            assertType(env, new EPTypeClass(SupportBean.class), "c0,c1");

            sendAssert(env, null, null);

            SupportBean sb1 = makeSendBean(env, "E1", 10);
            sendAssert(env, sb1, sb1);

            SupportBean sb2 = makeSendBean(env, "E2", 20);
            sendAssert(env, sb1, sb2);

            SupportBean sb3 = makeSendBean(env, "E3", 0);
            sendAssert(env, sb2, sb3);

            env.milestone(0);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, SupportBean first, SupportBean last) {
            final String[] fields = "c0,c1".split(",");
            env.sendEventBean(new SupportBean_S0(-1));
            env.assertPropsNew("s0", fields, new Object[]{first, last});
        }
    }

    private static class ResultSetAggregateWindowNonTable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString,c0,c1".split(",");
            String epl = "@name('s0') select theString, window(*).first() as c0, window(*).last() as c1 from SupportBean#length(3) as sb group by theString";
            env.compileDeploy(epl).addListener("s0");

            assertType(env, new EPTypeClass(SupportBean.class), "c0,c1");

            SupportBean sb1 = makeSendBean(env, "A", 1);
            env.assertPropsNew("s0", fields, new Object[]{"A", sb1, sb1});

            SupportBean sb2 = makeSendBean(env, "A", 2);
            env.assertPropsNew("s0", fields, new Object[]{"A", sb1, sb2});

            SupportBean sb3 = makeSendBean(env, "A", 3);
            env.assertPropsNew("s0", fields, new Object[]{"A", sb1, sb3});

            SupportBean sb4 = makeSendBean(env, "A", 4);
            env.assertPropsNew("s0", fields, new Object[]{"A", sb2, sb4});

            SupportBean sb5 = makeSendBean(env, "B", 5);
            env.assertPropsPerRowNewOnly("s0", fields, new Object[][]{{"B", sb5, sb5}, {"A", sb3, sb4}});

            SupportBean sb6 = makeSendBean(env, "A", 6);
            env.assertPropsNew("s0", fields, new Object[]{"A", sb4, sb6});

            env.milestone(0);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateWindowInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create table MyTable(windowcol window(*) @type('SupportBean'));\n", path);

            env.tryInvalidCompile(path, "select MyTable.windowcol.first(id) from SupportBean_S0",
                "Failed to validate select-clause expression 'MyTable.windowcol.first(id)': Failed to validate aggregation function parameter expression 'id': Property named 'id' is not valid in any stream");

            env.tryInvalidCompile(path, "select MyTable.windowcol.listReference(intPrimitive) from SupportBean_S0",
                "Failed to validate select-clause expression 'MyTable.windowcol.listReference(int...(45 chars)': Invalid number of parameters");

            env.undeployAll();
        }
    }

    private static SupportBean makeSendBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        env.sendEventBean(sb);
        return sb;
    }
}
