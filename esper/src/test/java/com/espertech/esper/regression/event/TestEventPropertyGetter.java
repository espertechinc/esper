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

package com.espertech.esper.regression.event;

import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;

public class TestEventPropertyGetter extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testGetter() throws Exception
    {
        String stmtText = "select * from " + SupportMarketDataBean.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        MyGetterUpdateListener listener = new MyGetterUpdateListener(stmt.getEventType());
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("sym", 100, 1000L, "feed"));
        assertEquals("sym", listener.getLastSymbol());
        assertEquals(1000L, (long) listener.getLastVolume());
        assertEquals(stmt, listener.getEpStatement());
        assertEquals(epService, listener.getEpServiceProvider());
    }
}
