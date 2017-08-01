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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class ExecExprMathDivisionRules implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getExpression().setIntegerDivision(true);
        configuration.getEngineDefaults().getExpression().setDivisionByZeroReturnsNull(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionInt(epService);
        runAssertionDouble(epService);
        runAssertionFloat(epService);
        runAssertionLong(epService);
        runAssertionBigInt(epService);
    }

    private void runAssertionBigInt(EPServiceProvider epService) {
        String epl = "select " +
                "BigInteger.valueOf(4)/BigInteger.valueOf(2) as c0" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(BigInteger.class, stmt.getEventType().getPropertyType("c0"));

        String[] fields = "c0".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {BigInteger.valueOf(4).divide(BigInteger.valueOf(2))});

        stmt.destroy();
    }

    private void runAssertionLong(EPServiceProvider epService) {
        String epl = "select " +
                "10L/2L as c0" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(Long.class, stmt.getEventType().getPropertyType("c0"));

        String[] fields = "c0".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {5L});

        stmt.destroy();
    }

    private void runAssertionFloat(EPServiceProvider epService) {
        String epl = "select " +
                "10f/2f as c0" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(Float.class, stmt.getEventType().getPropertyType("c0"));

        String[] fields = "c0".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {5f});

        stmt.destroy();
    }

    private void runAssertionDouble(EPServiceProvider epService) {
        String epl = "select " +
                "10d/0d as c0" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "c0".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {null});

        stmt.destroy();
    }

    private void runAssertionInt(EPServiceProvider epService) throws Exception {
        String epl = "select intPrimitive/intBoxed as result from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("result"));

        sendEvent(epService, 100, 3);
        assertEquals(33, listener.assertOneGetNewAndReset().get("result"));

        sendEvent(epService, 100, null);
        assertEquals(null, listener.assertOneGetNewAndReset().get("result"));

        sendEvent(epService, 100, 0);
        assertEquals(null, listener.assertOneGetNewAndReset().get("result"));
    }

    private void sendEvent(EPServiceProvider epService, Integer intPrimitive, Integer intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
