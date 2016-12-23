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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.bean.SupportBeanRange;
import junit.framework.TestCase;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;

public class TestJoinMultiKeyAndRange extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    private final int[][] eventData = {{1, 100},
                                       {2, 100},
                                       {1, 200},
                                       {2, 200}};
    private SupportBean eventsA[] = new SupportBean[eventData.length];
    private SupportBean eventsB[] = new SupportBean[eventData.length];

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testRangeNullAndDupAndInvalid() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanRange", SupportBeanRange.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanComplexProps", SupportBeanComplexProps.class);

        String eplOne = "select sb.* from SupportBean#keepall sb, SupportBeanRange#lastevent where intBoxed between rangeStart and rangeEnd";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(eplOne);
        stmtOne.addListener(listener);

        String eplTwo = "select sb.* from SupportBean#keepall sb, SupportBeanRange#lastevent where theString = key and intBoxed in [rangeStart: rangeEnd]";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(eplTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        // null join lookups
        sendEvent(new SupportBeanRange("R1", "G", (Integer) null, null));
        sendEvent(new SupportBeanRange("R2", "G", null, 10));
        sendEvent(new SupportBeanRange("R3", "G", 10, null));
        sendSupportBean("G", -1, null);

        // range invalid
        sendEvent(new SupportBeanRange("R4", "G", 10, 0));
        assertFalse(listener.isInvoked());
        assertFalse(listenerTwo.isInvoked());

        // duplicates
        Object eventOne = sendSupportBean("G", 100, 5);
        Object eventTwo = sendSupportBean("G", 101, 5);
        sendEvent(new SupportBeanRange("R4", "G", 0, 10));
        EventBean[] events = listener.getAndResetLastNewData();
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{eventOne, eventTwo}, EPAssertionUtil.getUnderlying(events));
        events = listenerTwo.getAndResetLastNewData();
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{eventOne, eventTwo}, EPAssertionUtil.getUnderlying(events));

        // test string compare
        String eplThree = "select sb.* from SupportBeanRange#keepall sb, SupportBean#lastevent where theString in [rangeStartStr:rangeEndStr]";
        epService.getEPAdministrator().createEPL(eplThree);

        sendSupportBean("P", 1, 1);
        sendEvent(new SupportBeanRange("R5", "R5", "O", "Q"));
        assertTrue(listener.isInvoked());

    }

    public void testMultiKeyed() {

        String eventClass = SupportBean.class.getName();

        String joinStatement = "select * from " +
            eventClass + "(theString='A')#length(3) as streamA," +
            eventClass + "(theString='B')#length(3) as streamB" +
            " where streamA.intPrimitive = streamB.intPrimitive " +
               "and streamA.intBoxed = streamB.intBoxed";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);

        assertEquals(SupportBean.class, stmt.getEventType().getPropertyType("streamA"));
        assertEquals(SupportBean.class, stmt.getEventType().getPropertyType("streamB"));
        assertEquals(2, stmt.getEventType().getPropertyNames().length);

        for (int i = 0; i < eventData.length; i++)
        {
            eventsA[i] = new SupportBean();
            eventsA[i].setTheString("A");
            eventsA[i].setIntPrimitive(eventData[i][0]);
            eventsA[i].setIntBoxed(eventData[i][1]);

            eventsB[i] = new SupportBean();
            eventsB[i].setTheString("B");
            eventsB[i].setIntPrimitive(eventData[i][0]);
            eventsB[i].setIntBoxed(eventData[i][1]);
        }

        sendEvent(eventsA[0]);
        sendEvent(eventsB[1]);
        sendEvent(eventsB[2]);
        sendEvent(eventsB[3]);
        assertNull(listener.getLastNewData());    // No events expected
    }

    private void sendEvent(Object theEvent)
    {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private SupportBean sendSupportBean(String theString, int intPrimitive, Integer intBoxed) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }
}
