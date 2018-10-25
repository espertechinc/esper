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
package com.espertech.esper.regressionlib.suite.resultset.querytype;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.EPStatement;
import org.junit.Assert;

public class ResultSetQueryTypeRowPerGroupReclaimMicrosecondResolution implements RegressionExecution {

    private final long flipTime;

    public ResultSetQueryTypeRowPerGroupReclaimMicrosecondResolution(long flipTime) {
        this.flipTime = flipTime;
    }

    public void run(RegressionEnvironment env) {
        env.advanceTime(0);

        String epl = "@name('s0') @IterableUnbound @Hint('reclaim_group_aged=1,reclaim_group_freq=5') select theString, count(*) from SupportBean group by theString";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 0));
        assertCount(env.statement("s0"), 1);

        env.advanceTime(flipTime - 1);
        env.sendEventBean(new SupportBean("E2", 0));
        assertCount(env.statement("s0"), 2);

        env.advanceTime(flipTime);
        env.sendEventBean(new SupportBean("E3", 0));
        assertCount(env.statement("s0"), 2);

        env.undeployAll();
    }

    private static void assertCount(EPStatement stmt, long count) {
        Assert.assertEquals(count, EPAssertionUtil.iteratorCount(stmt.iterator()));
    }
}
