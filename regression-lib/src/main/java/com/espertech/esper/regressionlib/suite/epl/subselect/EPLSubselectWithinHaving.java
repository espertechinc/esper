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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportMaxAmountEvent;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class EPLSubselectWithinHaving {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectHavingSubselectWithGroupBy(true));
        execs.add(new EPLSubselectHavingSubselectWithGroupBy(false));
        return execs;
    }

    private static class EPLSubselectHavingSubselectWithGroupBy implements RegressionExecution {
        private final boolean namedWindow;

        public EPLSubselectHavingSubselectWithGroupBy(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCreate = namedWindow ?
                "create window MyInfra#unique(key) as SupportMaxAmountEvent" :
                "create table MyInfra(key string primary key, maxAmount double)";
            env.compileDeploy(eplCreate, path);
            env.compileDeploy("insert into MyInfra select * from SupportMaxAmountEvent", path);

            String stmtText = "@name('s0') select theString as c0, sum(intPrimitive) as c1 " +
                "from SupportBean#groupwin(theString)#length(2) as sb " +
                "group by theString " +
                "having sum(intPrimitive) > (select maxAmount from MyInfra as mw where sb.theString = mw.key)";
            env.compileDeploy(stmtText, path).addListener("s0");

            String[] fields = "c0,c1".split(",");

            // set some amounts
            env.sendEventBean(new SupportMaxAmountEvent("G1", 10));
            env.sendEventBean(new SupportMaxAmountEvent("G2", 20));
            env.sendEventBean(new SupportMaxAmountEvent("G3", 30));

            // send some events
            env.sendEventBean(new SupportBean("G1", 5));
            env.sendEventBean(new SupportBean("G2", 19));
            env.sendEventBean(new SupportBean("G3", 28));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("G2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 21});

            env.sendEventBean(new SupportBean("G2", 18));
            env.sendEventBean(new SupportBean("G1", 4));
            env.sendEventBean(new SupportBean("G3", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("G3", 29));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G3", 31});

            env.sendEventBean(new SupportBean("G3", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G3", 33});

            env.sendEventBean(new SupportBean("G1", 6));
            env.sendEventBean(new SupportBean("G2", 2));
            env.sendEventBean(new SupportBean("G3", 26));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("G1", 99));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 105});

            env.sendEventBean(new SupportBean("G1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 100});

            env.undeployAll();
        }
    }

}
