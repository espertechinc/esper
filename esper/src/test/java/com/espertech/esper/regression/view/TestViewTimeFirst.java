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
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Iterator;

public class TestViewTimeFirst extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        Configuration configuration = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testMonthScoped() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendCurrentTime("2002-02-01T09:00:00.000");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean.win:firsttime(1 month)");

        sendCurrentTime("2002-02-15T09:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        sendCurrentTimeWithMinus("2002-03-01T09:00:00.000", 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        sendCurrentTime("2002-03-01T09:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));

        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), "theString".split(","), new Object[][] {{"E1"}, {"E2"}});
    }

    private void sendCurrentTime(String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendCurrentTimeWithMinus(String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }
}
