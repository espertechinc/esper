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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Iterator;

public class TestSelectClauseJoin extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        updateListener = null;
    }

    public void testJoinSelect()
    {
        String eventA = SupportBean.class.getName();
        String eventB = SupportBean.class.getName();

        String joinStatement = "select s0.doubleBoxed, s1.intPrimitive*s1.intBoxed/2.0 as div from " +
            eventA + "(theString='s0').win:length(3) as s0," +
            eventB + "(theString='s1').win:length(3) as s1" +
            " where s0.doubleBoxed = s1.doubleBoxed";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        EventType result = joinView.getEventType();
        assertEquals(Double.class, result.getPropertyType("s0.doubleBoxed"));
        assertEquals(Double.class, result.getPropertyType("div"));
        assertEquals(2, joinView.getEventType().getPropertyNames().length);

        assertNull(updateListener.getLastNewData());

        sendEvent("s0", 1, 4, 5);
        sendEvent("s1", 1, 3, 2);

        EventBean[] newEvents = updateListener.getLastNewData();
        assertEquals(1d, newEvents[0].get("s0.doubleBoxed"));
        assertEquals(3d, newEvents[0].get("div"));

        Iterator<EventBean> iterator = joinView.iterator();
        EventBean theEvent = iterator.next();
        assertEquals(1d, theEvent.get("s0.doubleBoxed"));
        assertEquals(3d, theEvent.get("div"));
    }

    private void sendEvent(String s, double doubleBoxed, int intPrimitive, int intBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }
}
