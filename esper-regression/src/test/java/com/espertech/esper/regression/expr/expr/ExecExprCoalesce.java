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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.regression.epl.other.ExecEPLSelectExpr;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class ExecExprCoalesce implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionCoalesceBeans(epService);
        runAssertionCoalesceLong(epService);
        runAssertionCoalesceLong_OM(epService);
        runAssertionCoalesceLong_Compile(epService);
        runAssertionCoalesceDouble(epService);
        runAssertionCoalesceInvalid(epService);
    }

    private void runAssertionCoalesceBeans(EPServiceProvider epService) {
        tryCoalesceBeans(epService, "select coalesce(a.theString, b.theString) as myString, coalesce(a, b) as myBean" +
                " from pattern [every (a=" + SupportBean.class.getName() + "(theString='s0') or b=" + SupportBean.class.getName() + "(theString='s1'))]");

        tryCoalesceBeans(epService, "SELECT COALESCE(a.theString, b.theString) AS myString, COALESCE(a, b) AS myBean" +
                " FROM PATTERN [EVERY (a=" + SupportBean.class.getName() + "(theString='s0') OR b=" + SupportBean.class.getName() + "(theString='s1'))]");
    }

    private void runAssertionCoalesceLong(EPServiceProvider epService) {
        EPStatement stmt = setupCoalesce(epService, "coalesce(longBoxed, intBoxed, shortBoxed)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(Long.class, stmt.getEventType().getPropertyType("result"));

        sendEvent(epService, 1L, 2, (short) 3);
        assertEquals(1L, listener.assertOneGetNewAndReset().get("result"));

        sendBoxedEvent(epService, null, 2, null);
        assertEquals(2L, listener.assertOneGetNewAndReset().get("result"));

        sendBoxedEvent(epService, null, null, Short.parseShort("3"));
        assertEquals(3L, listener.assertOneGetNewAndReset().get("result"));

        sendBoxedEvent(epService, null, null, null);
        assertEquals(null, listener.assertOneGetNewAndReset().get("result"));

        stmt.destroy();
    }

    private void runAssertionCoalesceLong_OM(EPServiceProvider epService) throws Exception {
        String epl = "select coalesce(longBoxed,intBoxed,shortBoxed) as result" +
                " from " + SupportBean.class.getName() + "#length(1000)";

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.coalesce(
                "longBoxed", "intBoxed", "shortBoxed"), "result"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName()).addView("length", Expressions.constant(1000))));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(epl, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Long.class, stmt.getEventType().getPropertyType("result"));

        tryCoalesceLong(epService, listener);

        stmt.destroy();
    }

    private void runAssertionCoalesceLong_Compile(EPServiceProvider epService) {
        String epl = "select coalesce(longBoxed,intBoxed,shortBoxed) as result" +
                " from " + SupportBean.class.getName() + "#length(1000)";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Long.class, stmt.getEventType().getPropertyType("result"));

        tryCoalesceLong(epService, listener);

        stmt.destroy();
    }

    private void runAssertionCoalesceDouble(EPServiceProvider epService) {
        EPStatement stmt = setupCoalesce(epService, "coalesce(null, byteBoxed, shortBoxed, intBoxed, longBoxed, floatBoxed, doubleBoxed)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Double.class, stmt.getEventType().getPropertyType("result"));

        sendEventWithDouble(epService, null, null, null, null, null, null);
        assertEquals(null, listener.assertOneGetNewAndReset().get("result"));

        sendEventWithDouble(epService, null, Short.parseShort("2"), null, null, null, 1d);
        assertEquals(2d, listener.assertOneGetNewAndReset().get("result"));

        sendEventWithDouble(epService, null, null, null, null, null, 100d);
        assertEquals(100d, listener.assertOneGetNewAndReset().get("result"));

        sendEventWithDouble(epService, null, null, null, null, 10f, 100d);
        assertEquals(10d, listener.assertOneGetNewAndReset().get("result"));

        sendEventWithDouble(epService, null, null, 1, 5L, 10f, 100d);
        assertEquals(1d, listener.assertOneGetNewAndReset().get("result"));

        sendEventWithDouble(epService, Byte.parseByte("3"), null, null, null, null, null);
        assertEquals(3d, listener.assertOneGetNewAndReset().get("result"));

        sendEventWithDouble(epService, null, null, null, 5L, 10f, 100d);
        assertEquals(5d, listener.assertOneGetNewAndReset().get("result"));

        stmt.destroy();
    }

    private void runAssertionCoalesceInvalid(EPServiceProvider epService) {
        String epl = "select coalesce(null, null) as result" +
                " from " + SupportBean.class.getName() + "#length(3) ";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        assertEquals(null, stmt.getEventType().getPropertyType("result"));

        tryCoalesceInvalid(epService, "coalesce(intPrimitive)");
        tryCoalesceInvalid(epService, "coalesce(intPrimitive, string)");
        tryCoalesceInvalid(epService, "coalesce(intPrimitive, xxx)");
        tryCoalesceInvalid(epService, "coalesce(intPrimitive, booleanBoxed)");
        tryCoalesceInvalid(epService, "coalesce(charPrimitive, longBoxed)");
        tryCoalesceInvalid(epService, "coalesce(charPrimitive, string, string)");
        tryCoalesceInvalid(epService, "coalesce(string, longBoxed)");
        tryCoalesceInvalid(epService, "coalesce(null, longBoxed, string)");
        tryCoalesceInvalid(epService, "coalesce(null, null, boolBoxed, 1l)");
    }

    private EPStatement setupCoalesce(EPServiceProvider epService, String coalesceExpr) {
        String epl = "select " + coalesceExpr + " as result" +
                " from " + SupportBean.class.getName() + "#length(1000) ";
        return epService.getEPAdministrator().createEPL(epl);
    }

    private void tryCoalesceInvalid(EPServiceProvider epService, String coalesceExpr) {
        String epl = "select " + coalesceExpr + " as result" +
                " from " + SupportBean.class.getName() + "#length(3) ";

        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        } catch (EPStatementException ex) {
            // expected
        }
    }

    private void tryCoalesceBeans(EPServiceProvider epService, String epl) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean theEvent = sendEvent(epService, "s0");
        EventBean eventReceived = listener.assertOneGetNewAndReset();
        assertEquals("s0", eventReceived.get("myString"));
        assertSame(theEvent, eventReceived.get("myBean"));

        theEvent = sendEvent(epService, "s1");
        eventReceived = listener.assertOneGetNewAndReset();
        assertEquals("s1", eventReceived.get("myString"));
        assertSame(theEvent, eventReceived.get("myBean"));

        stmt.destroy();
    }

    private void tryCoalesceLong(EPServiceProvider epService, SupportUpdateListener listener) {
        sendEvent(epService, 1L, 2, (short) 3);
        assertEquals(1L, listener.assertOneGetNewAndReset().get("result"));

        sendBoxedEvent(epService, null, 2, null);
        assertEquals(2L, listener.assertOneGetNewAndReset().get("result"));

        sendBoxedEvent(epService, null, null, Short.parseShort("3"));
        assertEquals(3L, listener.assertOneGetNewAndReset().get("result"));

        sendBoxedEvent(epService, null, null, null);
        assertEquals(null, listener.assertOneGetNewAndReset().get("result"));
    }

    private SupportBean sendEvent(EPServiceProvider epService, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void sendEvent(EPServiceProvider epService, long longBoxed, int intBoxed, short shortBoxed) {
        sendBoxedEvent(epService, longBoxed, intBoxed, shortBoxed);
    }

    private void sendBoxedEvent(EPServiceProvider epService, Long longBoxed, Integer intBoxed, Short shortBoxed) {
        SupportBean bean = new SupportBean();
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEventWithDouble(EPServiceProvider epService, Byte byteBoxed, Short shortBoxed, Integer intBoxed, Long longBoxed, Float floatBoxed, Double doubleBoxed) {
        SupportBean bean = new SupportBean();
        bean.setByteBoxed(byteBoxed);
        bean.setShortBoxed(shortBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setLongBoxed(longBoxed);
        bean.setFloatBoxed(floatBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecEPLSelectExpr.class);
}
