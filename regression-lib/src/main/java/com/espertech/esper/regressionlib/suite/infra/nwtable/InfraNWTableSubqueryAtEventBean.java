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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.runtime.client.scopetest.SupportListener;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertNull;

public class InfraNWTableSubqueryAtEventBean {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraSubSelStar(true));
        execs.add(new InfraSubSelStar(false));
        return execs;
    }

    private static class InfraSubSelStar implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSubSelStar(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCreate = namedWindow ?
                "create window MyInfra#keepall as (c0 string, c1 int)" :
                "create table MyInfra(c0 string primary key, c1 int)";
            env.compileDeploy(eplCreate, path);

            // create insert into
            String eplInsert = "insert into MyInfra select theString as c0, intPrimitive as c1 from SupportBean";
            env.compileDeploy(eplInsert, path);

            // create subquery
            String eplSubquery = "@name('s0') select p00, (select * from MyInfra) @eventbean as detail from SupportBean_S0";
            env.compileDeploy(eplSubquery, path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(0));
            assertReceived(env.listener("s0"), null);

            env.sendEventBean(new SupportBean("E1", 1));

            env.sendEventBean(new SupportBean_S0(0));
            assertReceived(env.listener("s0"), new Object[][]{{"E1", 1}});

            env.sendEventBean(new SupportBean("E2", 2));

            env.sendEventBean(new SupportBean_S0(0));
            assertReceived(env.listener("s0"), new Object[][]{{"E1", 1}, {"E2", 2}});

            env.undeployAll();
        }
    }

    private static void assertReceived(SupportListener listener, Object[][] values) {
        EventBean event = listener.assertOneGetNewAndReset();
        EventBean[] events = (EventBean[]) event.getFragment("detail");
        if (values == null) {
            assertNull(events);
            return;
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(events, "c0,c1".split(","), values);
    }
}
