/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.view;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.resource.StatementResourceHolder;
import com.espertech.esper.epl.view.OutputProcessViewBase;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.regression.support.ResultAssertExecution;
import com.espertech.esper.regression.support.ResultAssertTestResult;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanString;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.concurrent.atomic.AtomicLong;

public class TestOutputLimitChangeSetOpt extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testCases() {
        AtomicLong currentTime = new AtomicLong(0);
        sendTime(currentTime.get());

        // unaggregated and ungrouped
        //
        runAssertion(currentTime, 0, false, "intPrimitive", null, null, "last", null);
        runAssertion(currentTime, 0, false, "intPrimitive", null, null, "last", "order by intPrimitive");

        runAssertion(currentTime, 5, false, "intPrimitive", null, null, "all", null);

        runAssertion(currentTime, 4, false, "intPrimitive", null, null, "first", null);

        // fully-aggregated and ungrouped
        runAssertion(currentTime, 5, false, "count(*)", null, null, "last", null);
        runAssertion(currentTime, 0, true, "count(*)", null, null, "last", null);

        runAssertion(currentTime, 5, false, "count(*)", null, null, "all", null);

        runAssertion(currentTime, 4, false, "count(*)", null, null, "first", null);
        runAssertion(currentTime, 0, false, "count(*)", null, "having count(*) > 0", "first", null);

        // aggregated and ungrouped
        runAssertion(currentTime, 5, false, "theString, count(*)", null, null, "last", null);
        runAssertion(currentTime, 0, true, "theString, count(*)", null, null, "last", null);

        runAssertion(currentTime, 5, true, "theString, count(*)", null, null, "all", null);

        runAssertion(currentTime, 4, true, "theString, count(*)", null, null, "first", null);
        runAssertion(currentTime, 0, true, "theString, count(*)", null, "having count(*) > 0", "first", null);

        // fully-aggregated and grouped
        runAssertion(currentTime, 5, false, "theString, count(*)", "group by theString", null, "last", null);
        runAssertion(currentTime, 0, true, "theString, count(*)", "group by theString", null, "last", null);

        runAssertion(currentTime, 5, false, "theString, count(*)", "group by theString", null, "all", null);

        runAssertion(currentTime, 0, false, "theString, count(*)", "group by theString", null, "first", null);

        // aggregated and grouped
        runAssertion(currentTime, 5, false, "theString, intPrimitive, count(*)", "group by theString", null, "last", null);
        runAssertion(currentTime, 0, true, "theString, intPrimitive, count(*)", "group by theString", null, "last", null);

        runAssertion(currentTime, 5, false, "theString, intPrimitive, count(*)", "group by theString", null, "all", null);

        runAssertion(currentTime, 0, false, "theString, intPrimitive, count(*)", "group by theString", null, "first", null);
    }

    private void runAssertion(AtomicLong currentTime,
                              int expected,
                              boolean withHint,
                              String selectClause,
                              String groupBy,
                              String having,
                              String outputKeyword,
                              String orderBy) {
        String epl = (withHint ? "@Hint('enable_outputlimit_opt') " : "") +
                     "select irstream " + selectClause + " " +
                     "from SupportBean.win:length(2) " +
                     (groupBy == null ? "" : groupBy + " ") +
                     (having == null ? "" : having + " ") +
                     "output " + outputKeyword + " every 1 seconds " +
                    (orderBy == null ? "" : orderBy);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        for (int i = 0; i < 5; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
        }

        assertResourcesOutputRate(stmt, expected);

        sendTime(currentTime.addAndGet(1000));

        assertResourcesOutputRate(stmt, 0);
        stmt.destroy();
        listener.reset();
    }

    private void assertResourcesOutputRate(EPStatement stmt, int numExpectedChangeset) {
        EPStatementSPI spi = (EPStatementSPI) stmt;
        StatementResourceHolder resources = spi.getStatementContext().getStatementExtensionServicesContext().getStmtResources().getResourcesUnpartitioned();
        OutputProcessViewBase outputProcessViewBase = (OutputProcessViewBase) resources.getEventStreamViewables()[0].getViews()[0].getViews()[0];
        assertEquals(numExpectedChangeset, outputProcessViewBase.getNumChangesetRows());
    }

    private void sendTime(long currentTime) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(currentTime));
    }
}
