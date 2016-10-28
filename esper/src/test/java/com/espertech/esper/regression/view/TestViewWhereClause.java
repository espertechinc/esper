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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestViewWhereClause extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }
    
    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testWhere()
    {
        String viewExpr = "select * from " + SupportMarketDataBean.class.getName() + "#length(3) where symbol='CSCO'";
        EPStatement stmt = epService.getEPAdministrator().createEPL(viewExpr);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendMarketDataEvent("IBM");
        assertFalse(listener.getAndClearIsInvoked());

        sendMarketDataEvent("CSCO");
        assertTrue(listener.getAndClearIsInvoked());

        sendMarketDataEvent("IBM");
        assertFalse(listener.getAndClearIsInvoked());

        sendMarketDataEvent("CSCO");
        assertTrue(listener.getAndClearIsInvoked());
        
        // invalid return type for filter during compilation time
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        try {
            epService.getEPAdministrator().createEPL("Select theString From SupportBean#time(30 seconds) where intPrimitive group by theString");
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals("Error validating expression: The where-clause filter expression must return a boolean value [Select theString From SupportBean#time(30 seconds) where intPrimitive group by theString]", ex.getMessage());
        }

        // invalid return type for filter at runtime
        Map<String, Object> dict = new HashMap<String, Object>();
        dict.put("criteria", Boolean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MapEvent", dict);
        stmt = epService.getEPAdministrator().createEPL("Select * From MapEvent#time(30 seconds) where criteria");

        try {
            epService.getEPRuntime().sendEvent(Collections.singletonMap("criteria", 15), "MapEvent");
            fail(); // ensure exception handler rethrows
        }
        catch (EPException ex) {
            // fine
        }
        stmt.destroy();
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
    }

    public void testWhereNumericType()
    {
        String viewExpr = "select " +
                " intPrimitive + longPrimitive as p1," +
                " intPrimitive * doublePrimitive as p2," +
                " floatPrimitive / doublePrimitive as p3" +
                " from " + SupportBean.class.getName() + "#length(3) where " +
                "intPrimitive=longPrimitive and intPrimitive=doublePrimitive and floatPrimitive=doublePrimitive";

        EPStatement stmt = epService.getEPAdministrator().createEPL(viewExpr);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSupportBeanEvent(1,2,3,4);
        assertFalse(listener.getAndClearIsInvoked());

        sendSupportBeanEvent(2, 2, 2, 2);
        EventBean theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(Long.class, theEvent.getEventType().getPropertyType("p1"));
        assertEquals(4l, theEvent.get("p1"));
        assertEquals(Double.class, theEvent.getEventType().getPropertyType("p2"));
        assertEquals(4d, theEvent.get("p2"));
        assertEquals(Double.class, theEvent.getEventType().getPropertyType("p3"));
        assertEquals(1d, theEvent.get("p3"));
    }

    private void sendMarketDataEvent(String symbol)
    {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, 0, 0L, "");
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendSupportBeanEvent(int intPrimitive, long longPrimitive, float floatPrimitive, double doublePrimitive)
    {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        theEvent.setLongPrimitive(longPrimitive);
        theEvent.setFloatPrimitive(floatPrimitive);
        theEvent.setDoublePrimitive(doublePrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
