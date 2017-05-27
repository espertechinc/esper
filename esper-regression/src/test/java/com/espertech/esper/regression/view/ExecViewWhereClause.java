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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecViewWhereClause implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionWhere(epService);
        runAssertionWhereNumericType(epService);
    }

    private void runAssertionWhere(EPServiceProvider epService) {
        String epl = "select * from " + SupportMarketDataBean.class.getName() + "#length(3) where symbol='CSCO'";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendMarketDataEvent(epService, "IBM");
        assertFalse(listener.getAndClearIsInvoked());

        sendMarketDataEvent(epService, "CSCO");
        assertTrue(listener.getAndClearIsInvoked());

        sendMarketDataEvent(epService, "IBM");
        assertFalse(listener.getAndClearIsInvoked());

        sendMarketDataEvent(epService, "CSCO");
        assertTrue(listener.getAndClearIsInvoked());

        // invalid return type for filter during compilation time
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        try {
            epService.getEPAdministrator().createEPL("Select theString From SupportBean#time(30 seconds) where intPrimitive group by theString");
            fail();
        } catch (EPStatementException ex) {
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
        } catch (EPException ex) {
            // fine
        }
        stmt.destroy();
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionWhereNumericType(EPServiceProvider epService) {
        String epl = "select " +
                " intPrimitive + longPrimitive as p1," +
                " intPrimitive * doublePrimitive as p2," +
                " floatPrimitive / doublePrimitive as p3" +
                " from " + SupportBean.class.getName() + "#length(3) where " +
                "intPrimitive=longPrimitive and intPrimitive=doublePrimitive and floatPrimitive=doublePrimitive";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSupportBeanEvent(epService, 1, 2, 3, 4);
        assertFalse(listener.getAndClearIsInvoked());

        sendSupportBeanEvent(epService, 2, 2, 2, 2);
        EventBean theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(Long.class, theEvent.getEventType().getPropertyType("p1"));
        assertEquals(4L, theEvent.get("p1"));
        assertEquals(Double.class, theEvent.getEventType().getPropertyType("p2"));
        assertEquals(4d, theEvent.get("p2"));
        assertEquals(Double.class, theEvent.getEventType().getPropertyType("p3"));
        assertEquals(1d, theEvent.get("p3"));

        stmt.destroy();
    }

    private void sendMarketDataEvent(EPServiceProvider epService, String symbol) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, 0, 0L, "");
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, int intPrimitive, long longPrimitive, float floatPrimitive, double doublePrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        theEvent.setLongPrimitive(longPrimitive);
        theEvent.setFloatPrimitive(floatPrimitive);
        theEvent.setDoublePrimitive(doublePrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
