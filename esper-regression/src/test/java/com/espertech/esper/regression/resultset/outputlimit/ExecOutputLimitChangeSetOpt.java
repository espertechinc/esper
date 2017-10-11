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
package com.espertech.esper.regression.resultset.outputlimit;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.resource.StatementResourceHolder;
import com.espertech.esper.epl.view.OutputProcessViewBase;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.epl.SupportOutputLimitOpt;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.util.concurrent.atomic.AtomicLong;

import static com.espertech.esper.supportregression.epl.SupportOutputLimitOpt.*;
import static org.junit.Assert.assertEquals;

public class ExecOutputLimitChangeSetOpt implements RegressionExecution {
    private final boolean enableOutputLimitOpt;

    public ExecOutputLimitChangeSetOpt(boolean enableOutputLimitOpt) {
        this.enableOutputLimitOpt = enableOutputLimitOpt;
    }

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setOutputLimitOpt(enableOutputLimitOpt);
    }

    public void run(EPServiceProvider epService) throws Exception {

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        AtomicLong currentTime = new AtomicLong(0);
        sendTime(epService, currentTime.get());

        // unaggregated and ungrouped
        //
        tryAssertion(epService, currentTime, DEFAULT, 0, "intPrimitive", null, null, "last", null);
        tryAssertion(epService, currentTime, DEFAULT, 0, "intPrimitive", null, null, "last", "order by intPrimitive");

        tryAssertion(epService, currentTime, DEFAULT, enableOutputLimitOpt ? 0 : 5, "intPrimitive", null, null, "all", null);
        tryAssertion(epService, currentTime, ENABLED, 0, "intPrimitive", null, null, "all", null);
        tryAssertion(epService, currentTime, DISABLED, 5, "intPrimitive", null, null, "all", null);

        tryAssertion(epService, currentTime, DEFAULT, 0, "intPrimitive", null, null, "first", null);

        // fully-aggregated and ungrouped
        tryAssertion(epService, currentTime, DEFAULT, enableOutputLimitOpt ? 0 : 5, "count(*)", null, null, "last", null);
        tryAssertion(epService, currentTime, ENABLED, 0, "count(*)", null, null, "last", null);
        tryAssertion(epService, currentTime, DISABLED, 5, "count(*)", null, null, "last", null);

        tryAssertion(epService, currentTime, DEFAULT, enableOutputLimitOpt ? 0 : 5, "count(*)", null, null, "all", null);
        tryAssertion(epService, currentTime, ENABLED, 0, "count(*)", null, null, "all", null);
        tryAssertion(epService, currentTime, DISABLED, 5, "count(*)", null, null, "all", null);

        tryAssertion(epService, currentTime, DEFAULT, 0, "count(*)", null, null, "first", null);
        tryAssertion(epService, currentTime, DEFAULT, 0, "count(*)", null, "having count(*) > 0", "first", null);

        // aggregated and ungrouped
        tryAssertion(epService, currentTime, DEFAULT, enableOutputLimitOpt ? 0 : 5, "theString, count(*)", null, null, "last", null);
        tryAssertion(epService, currentTime, ENABLED, 0, "theString, count(*)", null, null, "last", null);
        tryAssertion(epService, currentTime, DISABLED, 5, "theString, count(*)", null, null, "last", null);

        tryAssertion(epService, currentTime, DEFAULT, enableOutputLimitOpt ? 0 : 5, "theString, count(*)", null, null, "all", null);
        tryAssertion(epService, currentTime, ENABLED, 0, "theString, count(*)", null, null, "all", null);
        tryAssertion(epService, currentTime, DISABLED, 5, "theString, count(*)", null, null, "all", null);

        tryAssertion(epService, currentTime, DEFAULT, 0, "theString, count(*)", null, null, "first", null);
        tryAssertion(epService, currentTime, DEFAULT, 0, "theString, count(*)", null, "having count(*) > 0", "first", null);

        // fully-aggregated and grouped
        tryAssertion(epService, currentTime, DEFAULT, enableOutputLimitOpt ? 0 : 5, "theString, count(*)", "group by theString", null, "last", null);
        tryAssertion(epService, currentTime, ENABLED, 0, "theString, count(*)", "group by theString", null, "last", null);
        tryAssertion(epService, currentTime, DISABLED, 5, "theString, count(*)", "group by theString", null, "last", null);

        tryAssertion(epService, currentTime, DEFAULT, enableOutputLimitOpt ? 0 : 5, "theString, count(*)", "group by theString", null, "all", null);
        tryAssertion(epService, currentTime, ENABLED, 0, "theString, count(*)", "group by theString", null, "all", null);
        tryAssertion(epService, currentTime, DISABLED, 5, "theString, count(*)", "group by theString", null, "all", null);

        tryAssertion(epService, currentTime, DEFAULT, 0, "theString, count(*)", "group by theString", null, "first", null);

        // aggregated and grouped
        tryAssertion(epService, currentTime, DEFAULT, enableOutputLimitOpt ? 0 : 5, "theString, intPrimitive, count(*)", "group by theString", null, "last", null);
        tryAssertion(epService, currentTime, ENABLED, 0, "theString, intPrimitive, count(*)", "group by theString", null, "last", null);
        tryAssertion(epService, currentTime, DISABLED, 5, "theString, intPrimitive, count(*)", "group by theString", null, "last", null);

        tryAssertion(epService, currentTime, DEFAULT, enableOutputLimitOpt ? 0 : 5, "theString, intPrimitive, count(*)", "group by theString", null, "all", null);
        tryAssertion(epService, currentTime, ENABLED, 0, "theString, intPrimitive, count(*)", "group by theString", null, "all", null);
        tryAssertion(epService, currentTime, DISABLED, 5, "theString, intPrimitive, count(*)", "group by theString", null, "all", null);

        tryAssertion(epService, currentTime, DEFAULT, 0, "theString, intPrimitive, count(*)", "group by theString", null, "first", null);

        SupportMessageAssertUtil.tryInvalid(epService, SupportOutputLimitOpt.ENABLED.getHint() + " select sum(intPrimitive) " +
                        "from SupportBean output last every 4 events order by theString",
                "Error starting statement: The ENABLE_OUTPUTLIMIT_OPT hint is not supported with order-by");
    }

    private void tryAssertion(EPServiceProvider epService,
                              AtomicLong currentTime,
                              SupportOutputLimitOpt hint, int expected,
                              String selectClause,
                              String groupBy,
                              String having,
                              String outputKeyword,
                              String orderBy) {
        String epl = hint.getHint() +
                "select irstream " + selectClause + " " +
                "from SupportBean#length(2) " +
                (groupBy == null ? "" : groupBy + " ") +
                (having == null ? "" : having + " ") +
                "output " + outputKeyword + " every 1 seconds " +
                (orderBy == null ? "" : orderBy);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        for (int i = 0; i < 5; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
        }

        assertResourcesOutputRate(stmt, expected);

        sendTime(epService, currentTime.addAndGet(1000));

        assertResourcesOutputRate(stmt, 0);
        stmt.destroy();
        listener.reset();
    }

    private void assertResourcesOutputRate(EPStatement stmt, int numExpectedChangeset) {
        EPStatementSPI spi = (EPStatementSPI) stmt;
        StatementResourceHolder resources = spi.getStatementContext().getStatementExtensionServicesContext().getStmtResources().getResourcesUnpartitioned();
        OutputProcessViewBase outputProcessViewBase = (OutputProcessViewBase) resources.getEventStreamViewables()[0].getViews()[0].getViews()[0];
        try {
            assertEquals("enableOutputLimitOpt=" + enableOutputLimitOpt, numExpectedChangeset, outputProcessViewBase.getNumChangesetRows());
        } catch (UnsupportedOperationException ex) {
            // allowed
        }
    }

    private void sendTime(EPServiceProvider epService, long currentTime) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(currentTime));
    }
}
