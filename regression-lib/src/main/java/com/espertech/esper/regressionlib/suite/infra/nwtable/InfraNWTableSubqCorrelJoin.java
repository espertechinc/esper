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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

public class InfraNWTableSubqCorrelJoin {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        // named window
        execs.add(new InfraNWTableSubqCorrelJoinAssertion(true, false)); // disable index-share
        execs.add(new InfraNWTableSubqCorrelJoinAssertion(true, true)); // enable-index-share

        // table
        execs.add(new InfraNWTableSubqCorrelJoinAssertion(false, false));
        return execs;
    }

    private static class InfraNWTableSubqCorrelJoinAssertion implements RegressionExecution {
        private final boolean namedWindow;
        private final boolean enableIndexShareCreate;

        public InfraNWTableSubqCorrelJoinAssertion(boolean namedWindow, boolean enableIndexShareCreate) {
            this.namedWindow = namedWindow;
            this.enableIndexShareCreate = enableIndexShareCreate;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String createEpl = namedWindow ?
                "create window MyInfra#unique(theString) as select * from SupportBean" :
                "create table MyInfra(theString string primary key, intPrimitive int primary key)";
            if (enableIndexShareCreate) {
                createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
            }
            env.compileDeploy(createEpl, path);
            env.compileDeploy("insert into MyInfra select theString, intPrimitive from SupportBean", path);

            String consumeEpl = "@name('s0') select (select intPrimitive from MyInfra where theString = s1.p10) as val from SupportBean_S0#lastevent as s0, SupportBean_S1#lastevent as s1";
            env.compileDeploy(consumeEpl, path).addListener("s0");

            String[] fields = "val".split(",");

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 20));
            env.sendEventBean(new SupportBean("E3", 30));

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(1, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20});

            env.sendEventBean(new SupportBean_S0(1, "E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20});

            env.sendEventBean(new SupportBean_S1(1, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10});

            env.sendEventBean(new SupportBean_S1(1, "E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{30});

            env.undeployModuleContaining("s0");
            env.undeployAll();
        }
    }
}
