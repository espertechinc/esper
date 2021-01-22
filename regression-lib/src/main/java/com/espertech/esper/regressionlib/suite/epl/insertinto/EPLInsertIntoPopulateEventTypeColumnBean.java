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
package com.espertech.esper.regressionlib.suite.epl.insertinto;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class EPLInsertIntoPopulateEventTypeColumnBean {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoColBeanFromSubquerySingle("objectarray", false));
        execs.add(new EPLInsertIntoColBeanFromSubquerySingle("objectarray", true));
        execs.add(new EPLInsertIntoColBeanFromSubquerySingle("map", false));
        execs.add(new EPLInsertIntoColBeanFromSubquerySingle("map", true));
        execs.add(new EPLInsertIntoColBeanFromSubqueryMulti("objectarray", false));
        execs.add(new EPLInsertIntoColBeanFromSubqueryMulti("objectarray", true));
        execs.add(new EPLInsertIntoColBeanFromSubqueryMulti("map", false));
        execs.add(new EPLInsertIntoColBeanFromSubqueryMulti("map", true));
        execs.add(new EPLInsertIntoColBeanSingleToMulti());
        execs.add(new EPLInsertIntoColBeanMultiToSingle());
        execs.add(new EPLInsertIntoColBeanInvalid());
        return execs;
    }

    private static class EPLInsertIntoColBeanMultiToSingle implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create schema EventOne(sb SupportBean)", path);
            env.compileDeploy("insert into EventOne select (select * from SupportBean#keepall) as sb from SupportBean_S0", path);
            env.compileDeploy("@name('s0') select * from EventOne#keepall", path).addListener("s0");

            SupportBean bean = new SupportBean("E1", 1);
            env.sendEventBean(bean);
            env.sendEventBean(new SupportBean_S0(1));
            env.assertEventNew("s0", event -> {
                assertSame(bean, event.get("sb"));
            });

            env.milestone(0);

            env.assertPropsPerRowIterator("s0", new String[] {"sb.theString"}, new Object[][] {{"E1"}});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoColBeanSingleToMulti implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create schema EventOne(sbarr SupportBean[])", path);
            env.compileDeploy("insert into EventOne select maxby(intPrimitive) as sbarr from SupportBean as sb", path);
            env.compileDeploy("@name('s0') select * from EventOne#keepall", path).addListener("s0");

            SupportBean bean = new SupportBean("E1", 1);
            env.sendEventBean(bean);
            env.assertEventNew("s0", event -> {
                SupportBean[] events = (SupportBean[]) event.get("sbarr");
                assertEquals(1, events.length);
                assertSame(bean, events[0]);
            });

            env.milestone(0);

            env.assertPropsPerRowIterator("s0", new String[] {"sbarr[0].theString"}, new Object[][] {{"E1"}});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoColBeanFromSubqueryMulti implements RegressionExecution {
        private final String typeType;
        private final boolean filter;

        public EPLInsertIntoColBeanFromSubqueryMulti(String typeType, boolean filter) {
            this.typeType = typeType;
            this.filter = filter;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create " + typeType + " schema EventOne(sbarr SupportBean_S0[])", path);

            env.compileDeploy("@name('s0') @public insert into EventOne select " +
                "(select * from SupportBean_S0#keepall " +
                (filter ? "where 1=1" : "") + ") as sbarr " +
                "from SupportBean", path).addListener("s0");
            env.compileDeploy("@name('s1') select * from EventOne#keepall", path);

            SupportBean_S0 s0One = new SupportBean_S0(1, "x1");
            env.sendEventBean(s0One);
            env.sendEventBean(new SupportBean("E1", 1));
            env.assertEventNew("s0", event -> assertS0(event, s0One));

            env.milestone(0);

            SupportBean_S0 s0Two = new SupportBean_S0(2, "x2", "y2");
            env.sendEventBean(s0Two);
            env.sendEventBean(new SupportBean("E2", 2));
            env.assertEventNew("s0", event -> assertS0(event, s0One, s0Two));
            env.assertPropsPerRowIterator("s1", "sbarr[0].id,sbarr[1].id".split(","), new Object[][]{{1, null}, {1, 2}});

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "typeType='" + typeType + '\'' +
                ", filter=" + filter +
                '}';
        }
    }

    private static class EPLInsertIntoColBeanFromSubquerySingle implements RegressionExecution {
        private final String typeType;
        private final boolean filter;

        public EPLInsertIntoColBeanFromSubquerySingle(String typeType, boolean filter) {
            this.typeType = typeType;
            this.filter = filter;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create " + typeType + " schema EventOne(sb SupportBean_S0)", path);

            String[] fields = "sb.p00".split(",");
            String epl = "@name('s0') insert into EventOne select " +
                "(select * from SupportBean_S0#length(2) " +
                (filter ? "where id >= 100" : "") + ") as sb " +
                "from SupportBean;\n " +
                "@name('s1') select * from EventOne#keepall";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "x1"));
            env.sendEventBean(new SupportBean("E1", 1));
            Object[] expected = filter ? new Object[]{null} : new Object[]{"x1"};
            env.assertPropsNew("s0", fields, expected);

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(100, "x2"));
            env.sendEventBean(new SupportBean("E2", 2));
            env.assertEqualsNew("s0", fields[0], filter ? "x2" : null);
            if (!filter) {
                env.assertPropsPerRowIterator("s1", "sb.id".split(","), new Object[][]{{1}, {null}});
            }

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "typeType='" + typeType + '\'' +
                ", filter=" + filter +
                '}';
        }
    }

    private static class EPLInsertIntoColBeanInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            // enumeration type is incompatible
            env.compileDeploy("@public create schema TypeOne(sbs SupportBean[])", path);
            env.tryInvalidCompile(path, "insert into TypeOne select (select * from SupportBean_S0#keepall) as sbs from SupportBean_S1",
                "Incompatible type detected attempting to insert into column 'sbs' type '" + SupportBean.class.getName() + "' compared to selected type 'SupportBean_S0'");

            env.compileDeploy("@public create schema TypeTwo(sbs SupportBean)", path);
            env.tryInvalidCompile(path, "insert into TypeTwo select (select * from SupportBean_S0#keepall) as sbs from SupportBean_S1",
                "Incompatible type detected attempting to insert into column 'sbs' type '" + SupportBean.class.getName() + "' compared to selected type 'SupportBean_S0'");

            env.undeployAll();
        }
    }

    private static void assertS0(EventBean event, SupportBean_S0... expected) {
        SupportBean_S0[] inner = (SupportBean_S0[]) event.get("sbarr");
        EPAssertionUtil.assertEqualsExactOrder(expected, inner);
    }
}
