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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanWithEnum;
import com.espertech.esper.supportregression.bean.SupportEnum;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.TestCase.assertEquals;

public class ExecExprCaseExpr implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionCaseSyntax1Sum(epService);
        runAssertionCaseSyntax1Sum_OM(epService);
        runAssertionCaseSyntax1Sum_Compile(epService);
        runAssertionCaseSyntax1WithElse(epService);
        runAssertionCaseSyntax1WithElse_OM(epService);
        runAssertionCaseSyntax1WithElse_Compile(epService);
        runAssertionCaseSyntax1Branches3(epService);
        runAssertionCaseSyntax2(epService);
        runAssertionCaseSyntax2StringsNBranches(epService);
        runAssertionCaseSyntax2NoElseWithNull(epService);
        runAssertionCaseSyntax1WithNull(epService);
        runAssertionCaseSyntax2WithNull_OM(epService);
        runAssertionCaseSyntax2WithNull_compile(epService);
        runAssertionCaseSyntax2WithNull(epService);
        runAssertionCaseSyntax2WithNullBool(epService);
        runAssertionCaseSyntax2WithCoercion(epService);
        runAssertionCaseSyntax2WithinExpression(epService);
        runAssertionCaseSyntax2Sum(epService);
        runAssertionCaseSyntax2EnumChecks(epService);
        runAssertionCaseSyntax2EnumResult(epService);
        runAssertionCaseSyntax2NoAsName(epService);
        runAssertionCaseWithArrayResult(epService);
    }

    private void runAssertionCaseWithArrayResult(EPServiceProvider epService) {
        String epl = "select case when intPrimitive = 1 then { 1, 2 } else { 1, 2 } end as c1 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertEqualsExactOrder((Integer[]) listener.assertOneGetNewAndReset().get("c1"), new Integer[] {1,2});

        stmt.destroy();
    }

    private void runAssertionCaseSyntax1Sum(EPServiceProvider epService) {
        // Testing the two forms of the case expression
        // Furthermore the test checks the different when clauses and actions related.
        String caseExpr = "select case " +
                " when symbol='GE' then volume " +
                " when symbol='DELL' then sum(price) " +
                "end as p1 from " + SupportMarketDataBean.class.getName() + "#length(10)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Double.class, stmt.getEventType().getPropertyType("p1"));

        runCaseSyntax1Sum(epService, listener);

        stmt.destroy();
    }

    private void runAssertionCaseSyntax1Sum_OM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.caseWhenThen()
                .add(Expressions.eq("symbol", "GE"), Expressions.property("volume"))
                .add(Expressions.eq("symbol", "DELL"), Expressions.sum("price")), "p1"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName()).addView("win", "length", Expressions.constant(10))));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String caseExpr = "select case" +
                " when symbol=\"GE\" then volume" +
                " when symbol=\"DELL\" then sum(price) " +
                "end as p1 from " + SupportMarketDataBean.class.getName() + ".win:length(10)";

        assertEquals(caseExpr, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Double.class, stmt.getEventType().getPropertyType("p1"));

        runCaseSyntax1Sum(epService, listener);

        stmt.destroy();
    }

    private void runAssertionCaseSyntax1Sum_Compile(EPServiceProvider epService) {
        String caseExpr = "select case" +
                " when symbol=\"GE\" then volume" +
                " when symbol=\"DELL\" then sum(price) " +
                "end as p1 from " + SupportMarketDataBean.class.getName() + "#length(10)";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(caseExpr);

        assertEquals(caseExpr, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Double.class, stmt.getEventType().getPropertyType("p1"));

        runCaseSyntax1Sum(epService, listener);

        stmt.destroy();
    }

    private void runCaseSyntax1Sum(EPServiceProvider epService, SupportUpdateListener listener) {
        sendMarketDataEvent(epService, "DELL", 10000, 50);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(50.0, theEvent.get("p1"));

        sendMarketDataEvent(epService, "DELL", 10000, 50);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(100.0, theEvent.get("p1"));

        sendMarketDataEvent(epService, "CSCO", 4000, 5);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(null, theEvent.get("p1"));

        sendMarketDataEvent(epService, "GE", 20, 30);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(20.0, theEvent.get("p1"));
    }

    private void runAssertionCaseSyntax1WithElse(EPServiceProvider epService) {
        // Adding to the EPL statement an else expression
        // when a CSCO ticker is sent the property for the else expression is selected
        String caseExpr = "select case " +
                " when symbol='DELL' then 3 * volume " +
                " else volume " +
                "end as p1 from " + SupportMarketDataBean.class.getName() + "#length(3)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Long.class, stmt.getEventType().getPropertyType("p1"));

        runCaseSyntax1WithElse(epService, listener);

        stmt.destroy();
    }

    private void runAssertionCaseSyntax1WithElse_OM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.caseWhenThen()
                .setElse(Expressions.property("volume"))
                .add(Expressions.eq("symbol", "DELL"), Expressions.multiply(Expressions.property("volume"), Expressions.constant(3))), "p1"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName()).addView("length", Expressions.constant(10))));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String caseExpr = "select case " +
                "when symbol=\"DELL\" then volume*3 " +
                "else volume " +
                "end as p1 from " + SupportMarketDataBean.class.getName() + "#length(10)";
        assertEquals(caseExpr, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Long.class, stmt.getEventType().getPropertyType("p1"));

        runCaseSyntax1WithElse(epService, listener);

        stmt.destroy();
    }

    private void runAssertionCaseSyntax1WithElse_Compile(EPServiceProvider epService) {
        String caseExpr = "select case " +
                "when symbol=\"DELL\" then volume*3 " +
                "else volume " +
                "end as p1 from " + SupportMarketDataBean.class.getName() + "#length(10)";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(caseExpr);
        assertEquals(caseExpr, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Long.class, stmt.getEventType().getPropertyType("p1"));

        runCaseSyntax1WithElse(epService, listener);

        stmt.destroy();
    }

    private void runCaseSyntax1WithElse(EPServiceProvider epService, SupportUpdateListener listener) {
        sendMarketDataEvent(epService, "CSCO", 4000, 0);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(4000L, theEvent.get("p1"));

        sendMarketDataEvent(epService, "DELL", 20, 0);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(3 * 20L, theEvent.get("p1"));
    }

    private void runAssertionCaseSyntax1Branches3(EPServiceProvider epService) {
        // Same test but the where clause doesn't match any of the condition of the case expresssion
        String caseExpr = "select case " +
                " when (symbol='GE') then volume " +
                " when (symbol='DELL') then volume / 2.0 " +
                " when (symbol='MSFT') then volume / 3.0 " +
                " end as p1 from " + SupportMarketDataBean.class.getName();

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Double.class, stmt.getEventType().getPropertyType("p1"));

        sendMarketDataEvent(epService, "DELL", 10000, 0);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(10000 / 2.0, theEvent.get("p1"));

        sendMarketDataEvent(epService, "MSFT", 10000, 0);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(10000 / 3.0, theEvent.get("p1"));

        sendMarketDataEvent(epService, "GE", 10000, 0);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(10000.0, theEvent.get("p1"));

        stmt.destroy();
    }

    private void runAssertionCaseSyntax2(EPServiceProvider epService) {
        String caseExpr = "select case intPrimitive " +
                " when longPrimitive then (intPrimitive + longPrimitive) " +
                " when doublePrimitive then intPrimitive * doublePrimitive" +
                " when floatPrimitive then floatPrimitive / doublePrimitive " +
                " else (intPrimitive + longPrimitive + floatPrimitive + doublePrimitive) end as p1 " +
                " from " + SupportBean.class.getName() + "#length(10)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Double.class, stmt.getEventType().getPropertyType("p1"));

        // intPrimitive = longPrimitive
        // case result is intPrimitive + longPrimitive
        sendSupportBeanEvent(epService, 2, 2L, 1.0f, 1.0);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(4.0, theEvent.get("p1"));
        // intPrimitive = doublePrimitive
        // case result is intPrimitive * doublePrimitive
        sendSupportBeanEvent(epService, 5, 1L, 1.0f, 5.0);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(25.0, theEvent.get("p1"));
        // intPrimitive = floatPrimitive
        // case result is floatPrimitive / doublePrimitive
        sendSupportBeanEvent(epService, 12, 1L, 12.0f, 4.0);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(3.0, theEvent.get("p1"));
        // all the properties of the event are different
        // The else part is computed: 1+2+3+4 = 10
        sendSupportBeanEvent(epService, 1, 2L, 3.0f, 4.0);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(10.0, theEvent.get("p1"));

        stmt.destroy();
    }

    private void runAssertionCaseSyntax2StringsNBranches(EPServiceProvider epService) {
        // Test of the various coercion user cases.
        String caseExpr = "select case intPrimitive" +
                " when 1 then Boolean.toString(boolPrimitive) " +
                " when 2 then Boolean.toString(boolBoxed) " +
                " when 3 then Integer.toString(intPrimitive) " +
                " when 4 then Integer.toString(intBoxed)" +
                " when 5 then Long.toString(longPrimitive) " +
                " when 6 then Long.toString(longBoxed) " +
                " when 7 then Character.toString(charPrimitive) " +
                " when 8 then Character.toString(charBoxed) " +
                " when 9 then Short.toString(shortPrimitive) " +
                " when 10 then Short.toString(shortBoxed) " +
                " when 11 then Byte.toString(bytePrimitive) " +
                " when 12 then Byte.toString(byteBoxed) " +
                " when 13 then Float.toString(floatPrimitive) " +
                " when 14 then Float.toString(floatBoxed) " +
                " when 15 then Double.toString(doublePrimitive) " +
                " when 16 then Double.toString(doubleBoxed) " +
                " when 17 then theString " +
                " else 'x' end as p1 " +
                " from " + SupportBean.class.getName() + "#length(1)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(String.class, stmt.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 1, new Integer(0), 0L, new Long(0L), '0', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
        EventBean theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("true", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 2, new Integer(0), 0L, new Long(0L), '0', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("false", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 3, new Integer(0), 0L, new Long(0L), '0', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("3", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 4, new Integer(4), 0L, new Long(0L), '0', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("4", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 5, new Integer(0), 5L, new Long(0L), '0', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("5", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 6, new Integer(0), 0L, new Long(6L), '0', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("6", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 7, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("A", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 8, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("a", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 9, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("9", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 10, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("10", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 11, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("11", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 12, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("12", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 13, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("13.0", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 14, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("14.0", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 15, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("15.0", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 16, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("16.0", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), 17, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("testCoercion", theEvent.get("p1"));

        sendSupportBeanEvent(epService, true, new Boolean(false), -1, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("x", theEvent.get("p1"));

        stmt.destroy();
    }

    private void runAssertionCaseSyntax2NoElseWithNull(EPServiceProvider epService) {
        String caseExpr = "select case theString " +
                " when null then true " +
                " when '' then false end as p1" +
                " from " + SupportBean.class.getName() + "#length(100)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Boolean.class, stmt.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(epService, "x");
        assertEquals(null, listener.assertOneGetNewAndReset().get("p1"));

        sendSupportBeanEvent(epService, "null");
        assertEquals(null, listener.assertOneGetNewAndReset().get("p1"));

        sendSupportBeanEvent(epService, null);
        assertEquals(true, listener.assertOneGetNewAndReset().get("p1"));

        sendSupportBeanEvent(epService, "");
        assertEquals(false, listener.assertOneGetNewAndReset().get("p1"));

        stmt.destroy();
    }

    private void runAssertionCaseSyntax1WithNull(EPServiceProvider epService) {
        String caseExpr = "select case " +
                " when theString is null then true " +
                " when theString = '' then false end as p1" +
                " from " + SupportBean.class.getName() + "#length(100)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Boolean.class, stmt.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(epService, "x");
        assertEquals(null, listener.assertOneGetNewAndReset().get("p1"));

        sendSupportBeanEvent(epService, "null");
        assertEquals(null, listener.assertOneGetNewAndReset().get("p1"));

        sendSupportBeanEvent(epService, null);
        assertEquals(true, listener.assertOneGetNewAndReset().get("p1"));

        sendSupportBeanEvent(epService, "");
        assertEquals(false, listener.assertOneGetNewAndReset().get("p1"));

        stmt.destroy();
    }

    private void runAssertionCaseSyntax2WithNull_OM(EPServiceProvider epService) throws Exception {
        String caseExpr = "select case intPrimitive " +
                "when 1 then null " +
                "when 2 then 1.0d " +
                "when 3 then null " +
                "else 2 " +
                "end as p1 from " + SupportBean.class.getName() + "#length(100)";

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.caseSwitch("intPrimitive")
                .setElse(Expressions.constant(2))
                .add(Expressions.constant(1), Expressions.constant(null))
                .add(Expressions.constant(2), Expressions.constant(1.0))
                .add(Expressions.constant(3), Expressions.constant(null)), "p1"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName()).addView("length", Expressions.constant(100))));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        assertEquals(caseExpr, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Double.class, stmt.getEventType().getPropertyType("p1"));

        runCaseSyntax2WithNull(epService, listener);

        stmt.destroy();
    }

    private void runAssertionCaseSyntax2WithNull_compile(EPServiceProvider epService) throws Exception {
        String caseExpr = "select case intPrimitive " +
                "when 1 then null " +
                "when 2 then 1.0d " +
                "when 3 then null " +
                "else 2 " +
                "end as p1 from " + SupportBean.class.getName() + "#length(100)";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(caseExpr);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(caseExpr, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Double.class, stmt.getEventType().getPropertyType("p1"));

        runCaseSyntax2WithNull(epService, listener);

        stmt.destroy();
    }

    private void runAssertionCaseSyntax2WithNull(EPServiceProvider epService) {
        String caseExpr = "select case intPrimitive " +
                " when 1 then null " +
                " when 2 then 1.0" +
                " when 3 then null " +
                " else 2 " +
                " end as p1 from " + SupportBean.class.getName() + "#length(100)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Double.class, stmt.getEventType().getPropertyType("p1"));

        runCaseSyntax2WithNull(epService, listener);

        stmt.destroy();
    }

    public void runCaseSyntax2WithNull(EPServiceProvider epService, SupportUpdateListener listener) {
        sendSupportBeanEvent(epService, 4);
        assertEquals(2.0, listener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(epService, 1);
        assertEquals(null, listener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(epService, 2);
        assertEquals(1.0, listener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(epService, 3);
        assertEquals(null, listener.assertOneGetNewAndReset().get("p1"));
    }

    private void runAssertionCaseSyntax2WithNullBool(EPServiceProvider epService) {
        String caseExpr = "select case boolBoxed " +
                " when null then 1 " +
                " when true then 2l" +
                " when false then 3 " +
                " end as p1 from " + SupportBean.class.getName() + "#length(100)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Long.class, stmt.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(epService, null);
        assertEquals(1L, listener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(epService, false);
        assertEquals(3L, listener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(epService, true);
        assertEquals(2L, listener.assertOneGetNewAndReset().get("p1"));

        stmt.destroy();
    }

    private void runAssertionCaseSyntax2WithCoercion(EPServiceProvider epService) {
        String caseExpr = "select case intPrimitive " +
                " when 1.0 then null " +
                " when 4/2.0 then 'x'" +
                " end as p1 from " + SupportBean.class.getName() + "#length(100)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(String.class, stmt.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(epService, 1);
        assertEquals(null, listener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(epService, 2);
        assertEquals("x", listener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(epService, 3);
        assertEquals(null, listener.assertOneGetNewAndReset().get("p1"));

        stmt.destroy();
    }

    private void runAssertionCaseSyntax2WithinExpression(EPServiceProvider epService) {
        String caseExpr = "select 2 * (case " +
                " intPrimitive when 1 then 2 " +
                " when 2 then 3 " +
                " else 10 end) as p1 " +
                " from " + SupportBean.class.getName() + "#length(1)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(epService, 1);
        EventBean theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(4, theEvent.get("p1"));

        sendSupportBeanEvent(epService, 2);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(6, theEvent.get("p1"));

        sendSupportBeanEvent(epService, 3);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(20, theEvent.get("p1"));

        stmt.destroy();
    }

    private void runAssertionCaseSyntax2Sum(EPServiceProvider epService) {
        String caseExpr = "select case intPrimitive when 1 then sum(longPrimitive) " +
                " when 2 then sum(floatPrimitive) " +
                " else sum(intPrimitive) end as p1 " +
                " from " + SupportBean.class.getName() + "#length(10)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Double.class, stmt.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(epService, 1, 10L, 3.0f, 4.0);
        EventBean theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(10d, theEvent.get("p1"));

        sendSupportBeanEvent(epService, 1, 15L, 3.0f, 4.0);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(25d, theEvent.get("p1"));

        sendSupportBeanEvent(epService, 2, 1L, 3.0f, 4.0);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(9d, theEvent.get("p1"));

        sendSupportBeanEvent(epService, 2, 1L, 3.0f, 4.0);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(12.0d, theEvent.get("p1"));

        sendSupportBeanEvent(epService, 5, 1L, 1.0f, 1.0);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(11.0d, theEvent.get("p1"));

        sendSupportBeanEvent(epService, 5, 1L, 1.0f, 1.0);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(16d, theEvent.get("p1"));

        stmt.destroy();
    }

    private void runAssertionCaseSyntax2EnumChecks(EPServiceProvider epService) {
        String caseExpr = "select case supportEnum " +
                " when " + SupportEnum.class.getName() + ".getValueForEnum(0) then 1 " +
                " when " + SupportEnum.class.getName() + ".getValueForEnum(1) then 2 " +
                " end as p1 " +
                " from " + SupportBeanWithEnum.class.getName() + "#length(10)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(epService, "a", SupportEnum.ENUM_VALUE_1);
        EventBean theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(1, theEvent.get("p1"));

        sendSupportBeanEvent(epService, "b", SupportEnum.ENUM_VALUE_2);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(2, theEvent.get("p1"));

        sendSupportBeanEvent(epService, "c", SupportEnum.ENUM_VALUE_3);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(null, theEvent.get("p1"));

        stmt.destroy();
    }

    private void runAssertionCaseSyntax2EnumResult(EPServiceProvider epService) {
        String caseExpr = "select case intPrimitive * 2 " +
                " when 2 then " + SupportEnum.class.getName() + ".getValueForEnum(0) " +
                " when 4 then " + SupportEnum.class.getName() + ".getValueForEnum(1) " +
                " else " + SupportEnum.class.getName() + ".getValueForEnum(2) " +
                " end as p1 " +
                " from " + SupportBean.class.getName() + "#length(10)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(SupportEnum.class, stmt.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(epService, 1);
        EventBean theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(SupportEnum.ENUM_VALUE_1, theEvent.get("p1"));

        sendSupportBeanEvent(epService, 2);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(SupportEnum.ENUM_VALUE_2, theEvent.get("p1"));

        sendSupportBeanEvent(epService, 3);
        theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(SupportEnum.ENUM_VALUE_3, theEvent.get("p1"));

        stmt.destroy();
    }

    private void runAssertionCaseSyntax2NoAsName(EPServiceProvider epService) {
        String caseSubExpr = "case intPrimitive when 1 then 0 end";
        String caseExpr = "select " + caseSubExpr +
                " from " + SupportBean.class.getName() + "#length(10)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType(caseSubExpr));

        sendSupportBeanEvent(epService, 1);
        EventBean theEvent = listener.getAndResetLastNewData()[0];
        assertEquals(0, theEvent.get(caseSubExpr));

        stmt.destroy();
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, boolean b, Boolean boolBoxed, int i, Integer intBoxed, long l, Long longBoxed,
                                      char c, Character charBoxed, short s, Short shortBoxed, byte by, Byte byteBoxed,
                                      float f, Float floatBoxed, double d, Double doubleBoxed, String str, SupportEnum enumval) {
        SupportBean theEvent = new SupportBean();
        theEvent.setBoolPrimitive(b);
        theEvent.setBoolBoxed(boolBoxed);
        theEvent.setIntPrimitive(i);
        theEvent.setIntBoxed(intBoxed);
        theEvent.setLongPrimitive(l);
        theEvent.setLongBoxed(longBoxed);
        theEvent.setCharPrimitive(c);
        theEvent.setCharBoxed(charBoxed);
        theEvent.setShortPrimitive(s);
        theEvent.setShortBoxed(shortBoxed);
        theEvent.setBytePrimitive(by);
        theEvent.setByteBoxed(byteBoxed);
        theEvent.setFloatPrimitive(f);
        theEvent.setFloatBoxed(floatBoxed);
        theEvent.setDoublePrimitive(d);
        theEvent.setDoubleBoxed(doubleBoxed);
        theEvent.setTheString(str);
        theEvent.setEnumValue(enumval);
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

    private void sendSupportBeanEvent(EPServiceProvider epService, int intPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, String theString) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(theString);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, boolean boolBoxed) {
        SupportBean theEvent = new SupportBean();
        theEvent.setBoolBoxed(boolBoxed);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, String theString, SupportEnum supportEnum) {
        SupportBeanWithEnum theEvent = new SupportBeanWithEnum(theString, supportEnum);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendMarketDataEvent(EPServiceProvider epService, String symbol, long volume, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecExprCaseExpr.class);
}
