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
package com.espertech.esper.regressionlib.suite.resultset.outputlimit;

import com.espertech.esper.common.internal.epl.output.core.OutputProcessView;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolder;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.epl.SupportOutputLimitOpt;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;
import org.junit.Assert;

import java.util.concurrent.atomic.AtomicLong;

public class ResultSetOutputLimitChangeSetOpt implements RegressionExecution {
    private final boolean enableOutputLimitOpt;

    public ResultSetOutputLimitChangeSetOpt(boolean enableOutputLimitOpt) {
        this.enableOutputLimitOpt = enableOutputLimitOpt;
    }

    public void run(RegressionEnvironment env) {
        AtomicLong currentTime = new AtomicLong(0);
        sendTime(env, currentTime.get());

        // unaggregated and ungrouped
        //
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, 0, "intPrimitive", null, null, "last", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, 0, "intPrimitive", null, null, "last", "order by intPrimitive");

        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, enableOutputLimitOpt ? 0 : 5, "intPrimitive", null, null, "all", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.ENABLED, 0, "intPrimitive", null, null, "all", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DISABLED, 5, "intPrimitive", null, null, "all", null);

        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, 0, "intPrimitive", null, null, "first", null);

        // fully-aggregated and ungrouped
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, enableOutputLimitOpt ? 0 : 5, "count(*)", null, null, "last", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.ENABLED, 0, "count(*)", null, null, "last", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DISABLED, 5, "count(*)", null, null, "last", null);

        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, enableOutputLimitOpt ? 0 : 5, "count(*)", null, null, "all", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.ENABLED, 0, "count(*)", null, null, "all", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DISABLED, 5, "count(*)", null, null, "all", null);

        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, 0, "count(*)", null, null, "first", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, 0, "count(*)", null, "having count(*) > 0", "first", null);

        // aggregated and ungrouped
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, enableOutputLimitOpt ? 0 : 5, "theString, count(*)", null, null, "last", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.ENABLED, 0, "theString, count(*)", null, null, "last", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DISABLED, 5, "theString, count(*)", null, null, "last", null);

        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, enableOutputLimitOpt ? 0 : 5, "theString, count(*)", null, null, "all", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.ENABLED, 0, "theString, count(*)", null, null, "all", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DISABLED, 5, "theString, count(*)", null, null, "all", null);

        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, 0, "theString, count(*)", null, null, "first", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, 0, "theString, count(*)", null, "having count(*) > 0", "first", null);

        // fully-aggregated and grouped
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, enableOutputLimitOpt ? 0 : 5, "theString, count(*)", "group by theString", null, "last", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.ENABLED, 0, "theString, count(*)", "group by theString", null, "last", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DISABLED, 5, "theString, count(*)", "group by theString", null, "last", null);

        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, enableOutputLimitOpt ? 0 : 5, "theString, count(*)", "group by theString", null, "all", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.ENABLED, 0, "theString, count(*)", "group by theString", null, "all", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DISABLED, 5, "theString, count(*)", "group by theString", null, "all", null);

        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, 0, "theString, count(*)", "group by theString", null, "first", null);

        // aggregated and grouped
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, enableOutputLimitOpt ? 0 : 5, "theString, intPrimitive, count(*)", "group by theString", null, "last", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.ENABLED, 0, "theString, intPrimitive, count(*)", "group by theString", null, "last", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DISABLED, 5, "theString, intPrimitive, count(*)", "group by theString", null, "last", null);

        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, enableOutputLimitOpt ? 0 : 5, "theString, intPrimitive, count(*)", "group by theString", null, "all", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.ENABLED, 0, "theString, intPrimitive, count(*)", "group by theString", null, "all", null);
        tryAssertion(env, currentTime, SupportOutputLimitOpt.DISABLED, 5, "theString, intPrimitive, count(*)", "group by theString", null, "all", null);

        tryAssertion(env, currentTime, SupportOutputLimitOpt.DEFAULT, 0, "theString, intPrimitive, count(*)", "group by theString", null, "first", null);

        SupportMessageAssertUtil.tryInvalidCompile(env, SupportOutputLimitOpt.ENABLED.getHint() + " select sum(intPrimitive) " +
                "from SupportBean output last every 4 events order by theString",
            "The ENABLE_OUTPUTLIMIT_OPT hint is not supported with order-by");
    }

    private void tryAssertion(RegressionEnvironment env,
                              AtomicLong currentTime,
                              SupportOutputLimitOpt hint, int expected,
                              String selectClause,
                              String groupBy,
                              String having,
                              String outputKeyword,
                              String orderBy) {
        String epl = hint.getHint() +
            "@name('s0') select irstream " + selectClause + " " +
            "from SupportBean#length(2) " +
            (groupBy == null ? "" : groupBy + " ") +
            (having == null ? "" : having + " ") +
            "output " + outputKeyword + " every 1 seconds " +
            (orderBy == null ? "" : orderBy);
        env.compileDeploy(epl).addListener("s0");

        for (int i = 0; i < 5; i++) {
            env.sendEventBean(new SupportBean("E" + i, i));
        }

        assertResourcesOutputRate(env, expected);

        sendTime(env, currentTime.addAndGet(1000));

        assertResourcesOutputRate(env, 0);
        env.undeployAll();
    }

    private void assertResourcesOutputRate(RegressionEnvironment env, int numExpectedChangeset) {
        EPStatementSPI spi = (EPStatementSPI) env.statement("s0");
        StatementResourceHolder resources = spi.getStatementContext().getStatementCPCacheService().getStatementResourceService().getResourcesUnpartitioned();
        OutputProcessView outputProcessView = (OutputProcessView) resources.getFinalView();
        try {
            Assert.assertEquals("enableOutputLimitOpt=" + enableOutputLimitOpt, numExpectedChangeset, outputProcessView.getNumChangesetRows());
        } catch (UnsupportedOperationException ex) {
            // allowed
        }
    }

    private static void sendTime(RegressionEnvironment env, long currentTime) {
        env.advanceTime(currentTime);
    }
}
