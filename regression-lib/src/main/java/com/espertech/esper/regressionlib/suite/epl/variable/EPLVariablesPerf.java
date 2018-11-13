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
package com.espertech.esper.regressionlib.suite.epl.variable;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.runtime.client.scopetest.SupportListener;

import static org.junit.Assert.assertTrue;

public class EPLVariablesPerf implements RegressionExecution {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window MyWindow#keepall as SupportBean", path);
        env.compileDeploy("insert into MyWindow select * from SupportBean", path);
        env.compileDeploy("create const variable String MYCONST = 'E331'", path);

        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(new SupportBean("E" + i, i * -1));
        }

        // test join
        env.compileDeploy("@name('s0') select * from SupportBean_S0 s0 unidirectional, MyWindow sb where theString = MYCONST", path);
        env.addListener("s0");

        SupportListener listener = env.listener("s0");
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(new SupportBean_S0(i, "E" + i));
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "sb.theString,sb.intPrimitive".split(","), new Object[]{"E331", -331});
        }
        long delta = System.currentTimeMillis() - start;
        assertTrue("delta=" + delta, delta < 500);
        env.undeployModuleContaining("s0");

        // test subquery
        env.compileDeploy("@name('s0') select * from SupportBean_S0 where exists (select * from MyWindow where theString = MYCONST)", path);
        env.addListener("s0");
        listener = env.listener("s0");

        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(new SupportBean_S0(i, "E" + i));
            assertTrue(listener.getAndClearIsInvoked());
        }
        delta = System.currentTimeMillis() - start;
        assertTrue("delta=" + delta, delta < 500);

        env.undeployModuleContaining("s0");
        env.undeployAll();
    }
}
