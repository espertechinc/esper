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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertTrue;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableOutputRateLimiting implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        AtomicLong currentTime = new AtomicLong(0);
        env.advanceTime(currentTime.get());
        RegressionPath path = new RegressionPath();

        env.compileDeploy("@name('create') create table MyTable as (\n" +
            "key string primary key, thesum sum(int))", path);
        env.compileDeploy("@name('intotable') into table MyTable " +
            "select sum(intPrimitive) as thesum from SupportBean group by theString", path);

        env.sendEventBean(new SupportBean("E1", 10));
        env.sendEventBean(new SupportBean("E2", 20));

        env.milestone(0);

        env.sendEventBean(new SupportBean("E1", 30));
        env.undeployModuleContaining("intotable");

        env.compileDeploy("@name('s0') select key, thesum from MyTable output snapshot every 1 seconds", path).addListener("s0");

        currentTime.set(currentTime.get() + 1000L);
        env.advanceTime(currentTime.get());
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), "key,thesum".split(","),
            new Object[][]{{"E1", 40}, {"E2", 20}});

        env.milestone(1);

        currentTime.set(currentTime.get() + 1000L);
        env.advanceTime(currentTime.get());
        assertTrue(env.listener("s0").isInvoked());

        env.undeployAll();
    }
}
