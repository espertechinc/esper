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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.client.EventBean;
import junit.framework.TestCase;

public class TestPatternEventProperties extends TestCase
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

    public void testWildcardSimplePattern()
    {
        setupSimplePattern("*");

        Object theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
        EventBean eventBean = updateListener.assertOneGetNewAndReset();
        assertSame(theEvent, eventBean.get("a"));
    }

    public void testWildcardOrPattern()
    {
        setupOrPattern("*");

        Object theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
        EventBean eventBean = updateListener.assertOneGetNewAndReset();
        assertSame(theEvent, eventBean.get("a"));
        assertNull(eventBean.get("b"));

        theEvent = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(theEvent);
        eventBean = updateListener.assertOneGetNewAndReset();
        assertSame(theEvent, eventBean.get("b"));
        assertNull(eventBean.get("a"));
    }

    public void testPropertiesSimplePattern()
    {
        setupSimplePattern("a, a as myEvent, a.intPrimitive as myInt, a.theString");

        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(1);
        theEvent.setTheString("test");
        epService.getEPRuntime().sendEvent(theEvent);

        EventBean eventBean = updateListener.assertOneGetNewAndReset();
        assertSame(theEvent, eventBean.get("a"));
        assertSame(theEvent, eventBean.get("myEvent"));
        assertEquals(1, eventBean.get("myInt"));
        assertEquals("test", eventBean.get("a.theString"));
    }

    public void testPropertiesOrPattern()
    {
        setupOrPattern("a, a as myAEvent, b, b as myBEvent, a.intPrimitive as myInt, " +
                "a.theString, b.simpleProperty as simple, b.indexed[0] as indexed, b.nested.nestedValue as nestedVal");

        Object theEvent = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(theEvent);
        EventBean eventBean = updateListener.assertOneGetNewAndReset();
        assertSame(theEvent, eventBean.get("b"));
        assertEquals("simple", eventBean.get("simple"));
        assertEquals(1, eventBean.get("indexed"));
        assertEquals("nestedValue", eventBean.get("nestedVal"));
        assertNull(eventBean.get("a"));
        assertNull(eventBean.get("myAEvent"));
        assertNull(eventBean.get("myInt"));
        assertNull(eventBean.get("a.theString"));

        SupportBean eventTwo = new SupportBean();
        eventTwo.setIntPrimitive(2);
        eventTwo.setTheString("test2");
        epService.getEPRuntime().sendEvent(eventTwo);
        eventBean = updateListener.assertOneGetNewAndReset();
        assertEquals(2, eventBean.get("myInt"));
        assertEquals("test2", eventBean.get("a.theString"));
        assertNull(eventBean.get("b"));
        assertNull(eventBean.get("myBEvent"));
        assertNull(eventBean.get("simple"));
        assertNull(eventBean.get("indexed"));
        assertNull(eventBean.get("nestedVal"));
    }

    private void setupSimplePattern(String selectCriteria)
    {
        String stmtText = "select " + selectCriteria + " from pattern [a=" + SupportBean.class.getName() + "]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(updateListener);
    }

    private void setupOrPattern(String selectCriteria)
    {
        String stmtText = "select " + selectCriteria + " from pattern [every(a=" + SupportBean.class.getName() +
                " or b=" + SupportBeanComplexProps.class.getName() + ")]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(updateListener);
    }
}
