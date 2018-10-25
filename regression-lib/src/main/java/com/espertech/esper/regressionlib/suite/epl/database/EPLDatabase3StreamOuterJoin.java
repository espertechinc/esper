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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanTwo;

import java.util.ArrayList;
import java.util.List;

public class EPLDatabase3StreamOuterJoin {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDatabaseInnerJoinLeftS0());
        execs.add(new EPLDatabaseOuterJoinLeftS0());
        return execs;
    }

    private static class EPLDatabaseInnerJoinLeftS0 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select * from SupportBean#lastevent sb" +
                " inner join " +
                " SupportBeanTwo#lastevent sbt" +
                " on sb.theString = sbt.stringTwo " +
                " inner join " +
                " sql:MyDBWithRetain ['select myint from mytesttable'] as s1 " +
                "  on s1.myint = sbt.intPrimitiveTwo";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(new SupportBeanTwo("T1", 2));
            env.sendEventBean(new SupportBean("T1", -1));

            env.sendEventBean(new SupportBeanTwo("T2", 30));
            env.sendEventBean(new SupportBean("T2", -1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T2", "T2", 30});

            env.sendEventBean(new SupportBean("T3", -1));
            env.sendEventBean(new SupportBeanTwo("T3", 40));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T3", "T3", 40});

            env.undeployAll();
        }
    }

    private static class EPLDatabaseOuterJoinLeftS0 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select * from SupportBean#lastevent sb" +
                " left outer join " +
                " SupportBeanTwo#lastevent sbt" +
                " on sb.theString = sbt.stringTwo " +
                " left outer join " +
                " sql:MyDBWithRetain ['select myint from mytesttable'] as s1 " +
                "  on s1.myint = sbt.intPrimitiveTwo";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(new SupportBeanTwo("T1", 2));
            env.sendEventBean(new SupportBean("T1", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T1", "T1", null});

            env.sendEventBean(new SupportBeanTwo("T2", 30));
            env.sendEventBean(new SupportBean("T2", -2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T2", "T2", 30});

            env.sendEventBean(new SupportBean("T3", -1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T3", null, null});

            env.sendEventBean(new SupportBeanTwo("T3", 40));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T3", "T3", 40});

            env.undeployAll();
        }
    }
}
