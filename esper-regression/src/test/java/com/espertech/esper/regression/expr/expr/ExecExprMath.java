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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class ExecExprMath implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionIntWNull(epService);
        runAssertionBigInt(epService);
        runAssertionBigDec(epService);
        runAssertionFloat(epService);
        runAssertionDouble(epService);
        runAssertionLong(epService);
        runAssertionBigDecConv(epService);
        runAssertionBigIntConv(epService);
    }

    private void runAssertionBigDecConv(EPServiceProvider epService) {
        String epl = "select " +
                "10+BigDecimal.valueOf(5,0) as c0," +
                "10-BigDecimal.valueOf(5,0) as c1," +
                "10*BigDecimal.valueOf(5,0) as c2," +
                "10/BigDecimal.valueOf(5,0) as c3" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "c0,c1,c2,c3".split(",");
        assertTypes(stmt, fields, BigDecimal.class, BigDecimal.class, BigDecimal.class, BigDecimal.class);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {BigDecimal.valueOf(15,0), BigDecimal.valueOf(5,0), BigDecimal.valueOf(50,0), BigDecimal.valueOf(2,0)});

        stmt.destroy();
    }

    private void runAssertionBigIntConv(EPServiceProvider epService) {
        String epl = "select " +
                "10+BigInteger.valueOf(5) as c0," +
                "10-BigInteger.valueOf(5) as c1," +
                "10*BigInteger.valueOf(5) as c2," +
                "10/BigInteger.valueOf(5) as c3" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "c0,c1,c2,c3".split(",");
        assertTypes(stmt, fields, BigInteger.class, BigInteger.class, BigInteger.class, Double.class);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {BigInteger.valueOf(15), BigInteger.valueOf(5), BigInteger.valueOf(50), 2d});

        stmt.destroy();
    }

    private void runAssertionLong(EPServiceProvider epService) {
        String epl = "select " +
                "10L+5L as c0," +
                "10L-5L as c1," +
                "10L*5L as c2," +
                "10L/5L as c3" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "c0,c1,c2,c3".split(",");
        assertTypes(stmt, fields, Long.class, Long.class, Long.class, Double.class);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {15L, 5L, 50L, 2d});

        stmt.destroy();
    }

    private void runAssertionDouble(EPServiceProvider epService) {
        String epl = "select " +
                "10d+5d as c0," +
                "10d-5d as c1," +
                "10d*5d as c2," +
                "10d/5d as c3," +
                "10d%4d as c4" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "c0,c1,c2,c3,c4".split(",");
        assertTypes(stmt, fields, Double.class, Double.class, Double.class, Double.class, Double.class);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {15d, 5d, 50d, 2d, 2d});

        stmt.destroy();
    }

    private void runAssertionFloat(EPServiceProvider epService) {
        String epl = "select " +
                "10f+5f as c0," +
                "10f-5f as c1," +
                "10f*5f as c2," +
                "10f/5f as c3," +
                "10f%4f as c4" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "c0,c1,c2,c3,c4".split(",");
        assertTypes(stmt, fields, Float.class, Float.class, Float.class, Double.class, Float.class);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {15f, 5f, 50f, 2d, 2f});

        stmt.destroy();
    }

    private void runAssertionBigInt(EPServiceProvider epService) {
        String epl = "select " +
                "BigInteger.valueOf(10)+BigInteger.valueOf(5) as c0," +
                "BigInteger.valueOf(10)-BigInteger.valueOf(5) as c1," +
                "BigInteger.valueOf(10)*BigInteger.valueOf(5) as c2," +
                "BigInteger.valueOf(10)/BigInteger.valueOf(5) as c3" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "c0,c1,c2,c3".split(",");
        assertTypes(stmt, fields, BigInteger.class, BigInteger.class, BigInteger.class, Double.class);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {BigInteger.valueOf(15), BigInteger.valueOf(5), BigInteger.valueOf(50), 2d});

        stmt.destroy();
    }

    private void runAssertionBigDec(EPServiceProvider epService) {
        String epl = "select " +
                "BigDecimal.valueOf(10,0)+BigDecimal.valueOf(5,0) as c0," +
                "BigDecimal.valueOf(10,0)-BigDecimal.valueOf(5,0) as c1," +
                "BigDecimal.valueOf(10,0)*BigDecimal.valueOf(5,0) as c2," +
                "BigDecimal.valueOf(10,0)/BigDecimal.valueOf(5,0) as c3" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "c0,c1,c2,c3".split(",");
        assertTypes(stmt, fields, BigDecimal.class, BigDecimal.class, BigDecimal.class, BigDecimal.class);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {BigDecimal.valueOf(15,0), BigDecimal.valueOf(5,0), BigDecimal.valueOf(50,0), BigDecimal.valueOf(2,0)});

        stmt.destroy();
    }

    private void runAssertionIntWNull(EPServiceProvider epService)
    {
        String epl = "select intPrimitive/intBoxed as result from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Double.class, stmt.getEventType().getPropertyType("result"));

        sendEvent(epService, 100, 3);
        assertEquals(100 / 3d, listener.assertOneGetNewAndReset().get("result"));

        sendEvent(epService, 100, null);
        assertEquals(null, listener.assertOneGetNewAndReset().get("result"));

        sendEvent(epService, 100, 0);
        assertEquals(Double.POSITIVE_INFINITY, listener.assertOneGetNewAndReset().get("result"));

        sendEvent(epService, -5, 0);
        assertEquals(Double.NEGATIVE_INFINITY, listener.assertOneGetNewAndReset().get("result"));

        stmt.destroy();
    }

    private void sendEvent(EPServiceProvider epService, Integer intPrimitive, Integer intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void assertTypes(EPStatement stmt, String[] fields, Class... types) {
        for (int i = 0; i < fields.length; i++) {
            assertEquals("failed for " + i, types[i], stmt.getEventType().getPropertyType(fields[i]));
        }
    }
}
