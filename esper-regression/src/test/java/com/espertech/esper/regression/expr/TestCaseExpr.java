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
package com.espertech.esper.regression.expr;

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;

import com.espertech.esper.supportregression.bean.SupportEnum;
import com.espertech.esper.supportregression.bean.SupportBeanWithEnum;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.SerializableObjectCopier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCaseExpr extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener testListener;

    public void setUp()
    {
        testListener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
    }

    public void testCaseSyntax1Sum()
    {
        // Testing the two forms of the case expression
        // Furthermore the test checks the different when clauses and actions related.
        String caseExpr = "select case " +
              " when symbol='GE' then volume " +
              " when symbol='DELL' then sum(price) " +
              "end as p1 from " +   SupportMarketDataBean.class.getName() + "#length(10)";

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(Double.class, selectTestCase.getEventType().getPropertyType("p1"));

        runCaseSyntax1Sum();
    }

    public void testCaseSyntax1Sum_OM() throws Exception
    {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.caseWhenThen()
                .add(Expressions.eq("symbol", "GE"), Expressions.property("volume"))
                .add(Expressions.eq("symbol", "DELL"), Expressions.sum("price")), "p1"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName()).addView("win", "length", Expressions.constant(10))));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String caseExpr = "select case" +
              " when symbol=\"GE\" then volume" +
              " when symbol=\"DELL\" then sum(price) " +
              "end as p1 from " +   SupportMarketDataBean.class.getName() + ".win:length(10)";

        assertEquals(caseExpr, model.toEPL());
        EPStatement selectTestCase = epService.getEPAdministrator().create(model);
        selectTestCase.addListener(testListener);
        assertEquals(Double.class, selectTestCase.getEventType().getPropertyType("p1"));

        runCaseSyntax1Sum();
    }

    public void testCaseSyntax1Sum_Compile()
    {
        String caseExpr = "select case" +
              " when symbol=\"GE\" then volume" +
              " when symbol=\"DELL\" then sum(price) " +
              "end as p1 from " +   SupportMarketDataBean.class.getName() + "#length(10)";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(caseExpr);

        assertEquals(caseExpr, model.toEPL());
        EPStatement selectTestCase = epService.getEPAdministrator().create(model);
        selectTestCase.addListener(testListener);
        assertEquals(Double.class, selectTestCase.getEventType().getPropertyType("p1"));

        runCaseSyntax1Sum();
    }

    private void runCaseSyntax1Sum()
    {
        sendMarketDataEvent("DELL", 10000, 50);
        EventBean theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(50.0, theEvent.get("p1"));

        sendMarketDataEvent("DELL", 10000, 50);
        theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(100.0, theEvent.get("p1"));

        sendMarketDataEvent("CSCO", 4000, 5);
        theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(null,theEvent.get("p1"));

        sendMarketDataEvent("GE", 20, 30);
        theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(20.0, theEvent.get("p1"));
    }

    public void testCaseSyntax1WithElse()
    {
        // Adding to the EPL statement an else expression
        // when a CSCO ticker is sent the property for the else expression is selected
        String caseExpr = "select case " +
              " when symbol='DELL' then 3 * volume " +
              " else volume " +
              "end as p1 from " + SupportMarketDataBean.class.getName() + "#length(3)";

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(Long.class, selectTestCase.getEventType().getPropertyType("p1"));

        runCaseSyntax1WithElse();
    }

    public void testCaseSyntax1WithElse_OM() throws Exception
    {
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

        EPStatement selectTestCase = epService.getEPAdministrator().create(model);
        selectTestCase.addListener(testListener);
        assertEquals(Long.class, selectTestCase.getEventType().getPropertyType("p1"));

        runCaseSyntax1WithElse();
    }

    public void testCaseSyntax1WithElse_Compile()
    {
        String caseExpr = "select case " +
              "when symbol=\"DELL\" then volume*3 " +
              "else volume " +
              "end as p1 from " + SupportMarketDataBean.class.getName() + "#length(10)";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(caseExpr);
        assertEquals(caseExpr, model.toEPL());

        EPStatement selectTestCase = epService.getEPAdministrator().create(model);
        selectTestCase.addListener(testListener);
        assertEquals(Long.class, selectTestCase.getEventType().getPropertyType("p1"));

        runCaseSyntax1WithElse();
    }

    private void runCaseSyntax1WithElse()
    {
        sendMarketDataEvent("CSCO", 4000, 0);
        EventBean theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(4000l,theEvent.get("p1"));

        sendMarketDataEvent("DELL", 20, 0);
        theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(3 * 20L, theEvent.get("p1"));
    }

    public void testCaseSyntax1Branches3()
    {
        // Same test but the where clause doesn't match any of the condition of the case expresssion
        String caseExpr = "select case " +
            " when (symbol='GE') then volume " +
            " when (symbol='DELL') then volume / 2.0 " +
            " when (symbol='MSFT') then volume / 3.0 " +
            " end as p1 from " +   SupportMarketDataBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(Double.class, selectTestCase.getEventType().getPropertyType("p1"));

        sendMarketDataEvent("DELL", 10000, 0);
        EventBean theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(10000 / 2.0,theEvent.get("p1"));

        sendMarketDataEvent("MSFT", 10000, 0);
        theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(10000 / 3.0,theEvent.get("p1"));

        sendMarketDataEvent("GE", 10000, 0);
        theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(10000.0,theEvent.get("p1"));
    }

    public void testCaseSyntax2()
    {
        String caseExpr = "select case intPrimitive " +
                " when longPrimitive then (intPrimitive + longPrimitive) " +
                " when doublePrimitive then intPrimitive * doublePrimitive" +
                " when floatPrimitive then floatPrimitive / doublePrimitive " +
                " else (intPrimitive + longPrimitive + floatPrimitive + doublePrimitive) end as p1 " +
                " from " + SupportBean.class.getName() + "#length(10)";

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(Double.class, selectTestCase.getEventType().getPropertyType("p1"));

        // intPrimitive = longPrimitive
        // case result is intPrimitive + longPrimitive
        sendSupportBeanEvent(2, 2L, 1.0f, 1.0);
        EventBean theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(4.0, theEvent.get("p1"));
        // intPrimitive = doublePrimitive
        // case result is intPrimitive * doublePrimitive
        sendSupportBeanEvent(5, 1L, 1.0f, 5.0);
        theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(25.0, theEvent.get("p1"));
        // intPrimitive = floatPrimitive
        // case result is floatPrimitive / doublePrimitive
        sendSupportBeanEvent(12, 1L, 12.0f, 4.0);
        theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(3.0, theEvent.get("p1"));
        // all the properties of the event are different
        // The else part is computed: 1+2+3+4 = 10
        sendSupportBeanEvent(1, 2L, 3.0f, 4.0);
        theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(10.0, theEvent.get("p1"));
    }

    public void testCaseSyntax2StringsNBranches()
    {
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

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(String.class, selectTestCase.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 1, new Integer(0),0L,new Long(0L),'0',new Character('a'),(short)0,new Short((short)0),(byte)0,new Byte((byte)0),0.0f,new Float((float)0),0.0,new Double(0.0),null,SupportEnum.ENUM_VALUE_1);
        EventBean theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("true", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 2, new Integer(0),0L,new Long(0L),'0',new Character('a'),(short)0,new Short((short)0),(byte)0,new Byte((byte)0),0.0f,new Float((float)0),0.0,new Double(0.0),null,SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("false", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 3, new Integer(0),0L,new Long(0L),'0',new Character('a'),(short)0,new Short((short)0),(byte)0,new Byte((byte)0),0.0f,new Float((float)0),0.0,new Double(0.0),null,SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("3", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 4, new Integer(4),0L,new Long(0L),'0',new Character('a'),(short)0,new Short((short)0),(byte)0,new Byte((byte)0),0.0f,new Float((float)0),0.0,new Double(0.0),null,SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("4", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 5, new Integer(0),5L,new Long(0L),'0',new Character('a'),(short)0,new Short((short)0),(byte)0,new Byte((byte)0),0.0f,new Float((float)0),0.0,new Double(0.0),null,SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("5", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 6, new Integer(0),0L,new Long(6L),'0',new Character('a'),(short)0,new Short((short)0),(byte)0,new Byte((byte)0),0.0f,new Float((float)0),0.0,new Double(0.0),null,SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("6", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 7, new Integer(0),0L,new Long(0L),'A',new Character('a'),(short)0,new Short((short)0),(byte)0,new Byte((byte)0),0.0f,new Float((float)0),0.0,new Double(0.0),null,SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("A", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 8, new Integer(0),0L,new Long(0L),'A',new Character('a'),(short)0,new Short((short)0),(byte)0,new Byte((byte)0),0.0f,new Float((float)0),0.0,new Double(0.0),null,SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("a", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 9, new Integer(0),0L,new Long(0L),'A',new Character('a'),(short)9,new Short((short)0),(byte)0,new Byte((byte)0),0.0f,new Float((float)0),0.0,new Double(0.0),null,SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("9", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 10, new Integer(0),0L,new Long(0L),'A',new Character('a'),(short)9,new Short((short)10),(byte)11,new Byte((byte)12),13.0f,new Float((float)14),15.0,new Double(16.0),"testCoercion",SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("10", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 11, new Integer(0),0L,new Long(0L),'A',new Character('a'),(short)9,new Short((short)10),(byte)11,new Byte((byte)12),13.0f,new Float((float)14),15.0,new Double(16.0),"testCoercion",SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("11", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 12, new Integer(0),0L,new Long(0L),'A',new Character('a'),(short)9,new Short((short)10),(byte)11,new Byte((byte)12),13.0f,new Float((float)14),15.0,new Double(16.0),"testCoercion",SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("12", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 13, new Integer(0),0L,new Long(0L),'A',new Character('a'),(short)9,new Short((short)10),(byte)11,new Byte((byte)12),13.0f,new Float((float)14),15.0,new Double(16.0),"testCoercion",SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("13.0", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 14, new Integer(0),0L,new Long(0L),'A',new Character('a'),(short)9,new Short((short)10),(byte)11,new Byte((byte)12),13.0f,new Float((float)14),15.0,new Double(16.0),"testCoercion",SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("14.0", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 15, new Integer(0),0L,new Long(0L),'A',new Character('a'),(short)9,new Short((short)10),(byte)11,new Byte((byte)12),13.0f,new Float((float)14),15.0,new Double(16.0),"testCoercion",SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("15.0", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 16, new Integer(0),0L,new Long(0L),'A',new Character('a'),(short)9,new Short((short)10),(byte)11,new Byte((byte)12),13.0f,new Float((float)14),15.0,new Double(16.0),"testCoercion",SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("16.0", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), 17, new Integer(0),0L,new Long(0L),'A',new Character('a'),(short)9,new Short((short)10),(byte)11,new Byte((byte)12),13.0f,new Float((float)14),15.0,new Double(16.0),"testCoercion",SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("testCoercion", theEvent.get("p1"));

        sendSupportBeanEvent(true, new Boolean(false), -1, new Integer(0),0L,new Long(0L),'A',new Character('a'),(short)9,new Short((short)10),(byte)11,new Byte((byte)12),13.0f,new Float((float)14),15.0,new Double(16.0),"testCoercion",SupportEnum.ENUM_VALUE_1);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("x", theEvent.get("p1"));
    }

    public void testCaseSyntax2NoElseWithNull()
    {
       String caseExpr = "select case theString " +
                 " when null then true " +
                 " when '' then false end as p1" +
                 " from " + SupportBean.class.getName() + "#length(100)";

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent("x");
        assertEquals(null, testListener.assertOneGetNewAndReset().get("p1"));

        sendSupportBeanEvent("null");
        assertEquals(null, testListener.assertOneGetNewAndReset().get("p1"));

        sendSupportBeanEvent(null);
        assertEquals(true, testListener.assertOneGetNewAndReset().get("p1"));

        sendSupportBeanEvent("");
        assertEquals(false, testListener.assertOneGetNewAndReset().get("p1"));
    }

    public void testCaseSyntax1WithNull()
    {
       String caseExpr = "select case " +
                 " when theString is null then true " +
                 " when theString = '' then false end as p1" +
                 " from " + SupportBean.class.getName() + "#length(100)";

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent("x");
        assertEquals(null, testListener.assertOneGetNewAndReset().get("p1"));

        sendSupportBeanEvent("null");
        assertEquals(null, testListener.assertOneGetNewAndReset().get("p1"));

        sendSupportBeanEvent(null);
        assertEquals(true, testListener.assertOneGetNewAndReset().get("p1"));

        sendSupportBeanEvent("");
        assertEquals(false, testListener.assertOneGetNewAndReset().get("p1"));
    }

    public void testCaseSyntax2WithNull_OM() throws Exception
    {
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
        EPStatement selectTestCase = epService.getEPAdministrator().create(model);
        selectTestCase.addListener(testListener);
        assertEquals(Double.class, selectTestCase.getEventType().getPropertyType("p1"));

        runCaseSyntax2WithNull();
    }

    public void testCaseSyntax2WithNull_compile() throws Exception
    {
       String caseExpr = "select case intPrimitive " +
                 "when 1 then null " +
                 "when 2 then 1.0d " +
                 "when 3 then null " +
                 "else 2 " +
                 "end as p1 from " + SupportBean.class.getName() + "#length(100)";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(caseExpr);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(caseExpr, model.toEPL());

        EPStatement selectTestCase = epService.getEPAdministrator().create(model);
        selectTestCase.addListener(testListener);
        assertEquals(Double.class, selectTestCase.getEventType().getPropertyType("p1"));

        runCaseSyntax2WithNull();
    }

    public void testCaseSyntax2WithNull()
    {
       String caseExpr = "select case intPrimitive " +
                 " when 1 then null " +
                 " when 2 then 1.0" +
                 " when 3 then null " +
                 " else 2 " +
                 " end as p1 from " + SupportBean.class.getName() + "#length(100)";

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(Double.class, selectTestCase.getEventType().getPropertyType("p1"));

        runCaseSyntax2WithNull();
    }

    public void runCaseSyntax2WithNull()
    {
        sendSupportBeanEvent(4);
        assertEquals(2.0, testListener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(1);
        assertEquals(null, testListener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(2);
        assertEquals(1.0, testListener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(3);
        assertEquals(null, testListener.assertOneGetNewAndReset().get("p1"));
    }

    public void testCaseSyntax2WithNullBool()
    {
       String caseExpr = "select case boolBoxed " +
                 " when null then 1 " +
                 " when true then 2l" +
                 " when false then 3 " +
                 " end as p1 from " + SupportBean.class.getName() + "#length(100)";

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(Long.class, selectTestCase.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(null);
        assertEquals(1L, testListener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(false);
        assertEquals(3L, testListener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(true);
        assertEquals(2L, testListener.assertOneGetNewAndReset().get("p1"));
    }

    public void testCaseSyntax2WithCoercion()
    {
       String caseExpr = "select case intPrimitive " +
                 " when 1.0 then null " +
                 " when 4/2.0 then 'x'" +
                 " end as p1 from " + SupportBean.class.getName() + "#length(100)";

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(String.class, selectTestCase.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(1);
        assertEquals(null, testListener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(2);
        assertEquals("x", testListener.assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(3);
        assertEquals(null, testListener.assertOneGetNewAndReset().get("p1"));
    }

    public void testCaseSyntax2WithinExpression()
    {
       String caseExpr = "select 2 * (case " +
                 " intPrimitive when 1 then 2 " +
                 " when 2 then 3 " +
                 " else 10 end) as p1 " +
                 " from " + SupportBean.class.getName() + "#length(1)";

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(Integer.class, selectTestCase.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(1);
        EventBean theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(4, theEvent.get("p1"));

        sendSupportBeanEvent(2);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(6, theEvent.get("p1"));

        sendSupportBeanEvent(3);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(20, theEvent.get("p1"));
    }

    public void testCaseSyntax2Sum()
    {
       String caseExpr = "select case intPrimitive when 1 then sum(longPrimitive) " +
                 " when 2 then sum(floatPrimitive) " +
                 " else sum(intPrimitive) end as p1 " +
                 " from " + SupportBean.class.getName() + "#length(10)";

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(Double.class, selectTestCase.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(1, 10L, 3.0f, 4.0);
        EventBean theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(10d, theEvent.get("p1"));

        sendSupportBeanEvent(1, 15L, 3.0f, 4.0);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(25d, theEvent.get("p1"));

        sendSupportBeanEvent(2, 1L, 3.0f, 4.0);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(9d, theEvent.get("p1"));

        sendSupportBeanEvent(2, 1L, 3.0f, 4.0);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(12.0d, theEvent.get("p1"));

        sendSupportBeanEvent(5, 1L, 1.0f, 1.0);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(11.0d, theEvent.get("p1"));

        sendSupportBeanEvent(5, 1L, 1.0f, 1.0);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(16d, theEvent.get("p1"));
    }

    public void testCaseSyntax2EnumChecks()
    {
       String caseExpr = "select case supportEnum " +
                 " when " + SupportEnum.class.getName() + ".getValueForEnum(0) then 1 " +
                 " when " + SupportEnum.class.getName() + ".getValueForEnum(1) then 2 " +
                 " end as p1 " +
                 " from " + SupportBeanWithEnum.class.getName() + "#length(10)";

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(Integer.class, selectTestCase.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent("a", SupportEnum.ENUM_VALUE_1);
        EventBean theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(1, theEvent.get("p1"));

        sendSupportBeanEvent("b", SupportEnum.ENUM_VALUE_2);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(2, theEvent.get("p1"));

        sendSupportBeanEvent("c", SupportEnum.ENUM_VALUE_3);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(null, theEvent.get("p1"));
    }

    public void testCaseSyntax2EnumResult()
    {
       String caseExpr = "select case intPrimitive * 2 " +
                 " when 2 then " + SupportEnum.class.getName() + ".getValueForEnum(0) " +
                 " when 4 then " + SupportEnum.class.getName() + ".getValueForEnum(1) " +
                 " else " + SupportEnum.class.getName() + ".getValueForEnum(2) " +
                 " end as p1 " +
                 " from " + SupportBean.class.getName() + "#length(10)";

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(SupportEnum.class, selectTestCase.getEventType().getPropertyType("p1"));

        sendSupportBeanEvent(1);
        EventBean theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(SupportEnum.ENUM_VALUE_1, theEvent.get("p1"));

        sendSupportBeanEvent(2);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(SupportEnum.ENUM_VALUE_2, theEvent.get("p1"));

        sendSupportBeanEvent(3);
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(SupportEnum.ENUM_VALUE_3, theEvent.get("p1"));
    }

    public void testCaseSyntax2NoAsName()
    {
        String caseSubExpr = "case intPrimitive when 1 then 0 end";
        String caseExpr = "select " + caseSubExpr +
                 " from " + SupportBean.class.getName() + "#length(10)";

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        selectTestCase.addListener(testListener);
        assertEquals(Integer.class, selectTestCase.getEventType().getPropertyType(caseSubExpr));

        sendSupportBeanEvent(1);
        EventBean theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals(0, theEvent.get(caseSubExpr));
    }

    private void sendSupportBeanEvent(boolean b_, Boolean boolBoxed_, int i_, Integer intBoxed_, long l_, Long longBoxed_,
                                      char c_, Character charBoxed_, short s_, Short shortBoxed_, byte by_, Byte byteBoxed_,
                                      float f_, Float floatBoxed_, double d_, Double doubleBoxed_, String str_, SupportEnum enum_)
    {
        SupportBean theEvent = new SupportBean();
        theEvent.setBoolPrimitive(b_);
        theEvent.setBoolBoxed(boolBoxed_);
        theEvent.setIntPrimitive(i_);
        theEvent.setIntBoxed(intBoxed_);
        theEvent.setLongPrimitive(l_);
        theEvent.setLongBoxed(longBoxed_);
        theEvent.setCharPrimitive(c_);
        theEvent.setCharBoxed(charBoxed_);
        theEvent.setShortPrimitive(s_);
        theEvent.setShortBoxed(shortBoxed_);
        theEvent.setBytePrimitive(by_);
        theEvent.setByteBoxed(byteBoxed_);
        theEvent.setFloatPrimitive(f_);
        theEvent.setFloatBoxed(floatBoxed_);
        theEvent.setDoublePrimitive(d_);
        theEvent.setDoubleBoxed(doubleBoxed_);
        theEvent.setTheString(str_);
        theEvent.setEnumValue(enum_);
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

    private void sendSupportBeanEvent(int intPrimitive)
    {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendSupportBeanEvent(String theString)
    {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(theString);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendSupportBeanEvent(boolean boolBoxed)
    {
        SupportBean theEvent = new SupportBean();
        theEvent.setBoolBoxed(boolBoxed);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendSupportBeanEvent(String theString, SupportEnum supportEnum)
    {
        SupportBeanWithEnum theEvent = new SupportBeanWithEnum(theString, supportEnum);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendMarketDataEvent(String symbol, long volume, double price)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(TestCaseExpr.class);
}
