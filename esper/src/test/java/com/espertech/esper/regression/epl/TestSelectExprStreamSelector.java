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

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.soda.SelectClause;
import com.espertech.esper.client.soda.FromClause;
import com.espertech.esper.client.soda.FilterStream;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.bean.SupportBeanComplexProps;
import com.espertech.esper.support.client.SupportConfigFactory;

import java.util.Map;

public class TestSelectExprStreamSelector extends TestCase
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

    public void testInvalidSelectWildcardProperty()
    {
        try
        {
            String stmtOneText = "select simpleProperty.* as a from " + SupportBeanComplexProps.class.getName() + " as s0";
            epService.getEPAdministrator().createEPL(stmtOneText);
            fail();
        }
        catch (Exception ex)
        {
            assertEquals("Error starting statement: The property wildcard syntax must be used without column name [select simpleProperty.* as a from com.espertech.esper.support.bean.SupportBeanComplexProps as s0]", ex.getMessage());
        }
    }

    public void testInsertTransposeNestedProperty()
    {
    	String stmtOneText = "insert into StreamA select nested.* from " + SupportBeanComplexProps.class.getName() + " as s0";
    	SupportUpdateListener listenerOne = new SupportUpdateListener();
    	EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtOneText);
        stmtOne.addListener(listenerOne);
        assertEquals(SupportBeanComplexProps.SupportBeanSpecialGetterNested.class, stmtOne.getEventType().getUnderlyingType());

        String stmtTwoText = "select nestedValue from StreamA";
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTwoText);
        stmtTwo.addListener(listenerTwo);
        assertEquals(String.class, stmtTwo.getEventType().getPropertyType("nestedValue"));

        epService.getEPRuntime().sendEvent(SupportBeanComplexProps.makeDefaultBean());

        assertEquals("nestedValue", listenerOne.assertOneGetNewAndReset().get("nestedValue"));
        assertEquals("nestedValue", listenerTwo.assertOneGetNewAndReset().get("nestedValue"));
    }

    public void testInsertFromPattern()
    {
    	String stmtOneText = "insert into streamA select a.* from pattern [every a=" + SupportBean.class.getName() + "]";
    	SupportUpdateListener listenerOne = new SupportUpdateListener();
    	EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtOneText);
        stmtOne.addListener(listenerOne);

        String stmtTwoText = "insert into streamA select a.* from pattern [every a=" + SupportBean.class.getName() + " where timer:within(30 sec)]";
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTwoText);
        stmtTwo.addListener(listenerTwo);

        EventType eventType = stmtOne.getEventType();
        assertEquals(SupportBean.class, eventType.getUnderlyingType());

        Object theEvent = sendBeanEvent("E1", 10);
        assertSame(theEvent, listenerTwo.assertOneGetNewAndReset().getUnderlying());

        theEvent = sendBeanEvent("E2", 10);
        assertSame(theEvent, listenerTwo.assertOneGetNewAndReset().getUnderlying());

        String stmtThreeText = "insert into streamB select a.*, 'abc' as abc from pattern [every a=" + SupportBean.class.getName() + " where timer:within(30 sec)]";
        EPStatement stmtThree = epService.getEPAdministrator().createEPL(stmtThreeText);
        assertEquals(Pair.class, stmtThree.getEventType().getUnderlyingType());
        assertEquals(String.class, stmtThree.getEventType().getPropertyType("abc"));
        assertEquals(String.class, stmtThree.getEventType().getPropertyType("theString"));
    }

    public void testObjectModelJoinAlias()
    {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create()
                .addStreamWildcard("s0")
                .addStreamWildcard("s1", "s1stream")
                .addWithAsProvidedName("theString", "sym"));
        model.setFromClause(FromClause.create()
                .add(FilterStream.create(SupportBean.class.getName(), "s0").addView("keepall"))
                .add(FilterStream.create(SupportMarketDataBean.class.getName(), "s1").addView("keepall")));

        EPStatement selectTestView = epService.getEPAdministrator().create(model);
        selectTestView.addListener(testListener);

        String viewExpr = "select s0.*, s1.* as s1stream, theString as sym from " + SupportBean.class.getName() + "#keepall() as s0, " +
                SupportMarketDataBean.class.getName() + "#keepall() as s1";
        assertEquals(viewExpr, model.toEPL());
        EPStatementObjectModel modelReverse = epService.getEPAdministrator().compileEPL(model.toEPL());
        assertEquals(viewExpr, modelReverse.toEPL());

        EventType type = selectTestView.getEventType();
        assertEquals(SupportMarketDataBean.class, type.getPropertyType("s1stream"));
        assertEquals(Pair.class, type.getUnderlyingType());

        sendBeanEvent("E1");
        assertFalse(testListener.isInvoked());

        Object theEvent = sendMarketEvent("E1");
        EventBean outevent = testListener.assertOneGetNewAndReset();
        assertSame(theEvent, outevent.get("s1stream"));
    }

    public void testNoJoinWildcardNoAlias()
    {
        String viewExpr = "select *, win.* from " + SupportBean.class.getName() + "#length(3) as win";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        EventType type = selectTestView.getEventType();
        assertTrue(type.getPropertyNames().length > 15);
        assertEquals(SupportBean.class, type.getUnderlyingType());

        Object theEvent = sendBeanEvent("E1", 16);
        assertSame(theEvent, testListener.assertOneGetNewAndReset().getUnderlying());
    }

    public void testJoinWildcardNoAlias()
    {
        String viewExpr = "select *, s1.* from " + SupportBean.class.getName() + "#length(3) as s0, " +
                SupportMarketDataBean.class.getName() + "#keepall() as s1";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        EventType type = selectTestView.getEventType();
        assertEquals(7, type.getPropertyNames().length);
        assertEquals(Long.class, type.getPropertyType("volume"));
        assertEquals(SupportBean.class, type.getPropertyType("s0"));
        assertEquals(SupportMarketDataBean.class, type.getPropertyType("s1"));
        assertEquals(Pair.class, type.getUnderlyingType());

        Object eventOne = sendBeanEvent("E1", 13);
        assertFalse(testListener.isInvoked());

        Object eventTwo = sendMarketEvent("E2");
        String[] fields = new String[] {"s0", "s1", "symbol", "volume"};
        EventBean received = testListener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{eventOne, eventTwo, "E2", 0L});
    }

    public void testNoJoinWildcardWithAlias()
    {
        String viewExpr = "select *, win.* as s0 from " + SupportBean.class.getName() + "#length(3) as win";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        EventType type = selectTestView.getEventType();
        assertTrue(type.getPropertyNames().length > 15);
        assertEquals(Pair.class, type.getUnderlyingType());
        assertEquals(SupportBean.class, type.getPropertyType("s0"));

        Object theEvent = sendBeanEvent("E1", 15);
        String[] fields = new String[] {"theString", "intPrimitive", "s0"};
        EPAssertionUtil.assertProps(testListener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 15, theEvent});
    }

    public void testJoinWildcardWithAlias()
    {
        String viewExpr = "select *, s1.* as s1stream, s0.* as s0stream from " + SupportBean.class.getName() + "#length(3) as s0, " +
                SupportMarketDataBean.class.getName() + "#keepall() as s1";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        EventType type = selectTestView.getEventType();
        assertEquals(4, type.getPropertyNames().length);
        assertEquals(SupportBean.class, type.getPropertyType("s0stream"));
        assertEquals(SupportBean.class, type.getPropertyType("s0"));
        assertEquals(SupportMarketDataBean.class, type.getPropertyType("s1stream"));
        assertEquals(SupportMarketDataBean.class, type.getPropertyType("s1"));
        assertEquals(Map.class, type.getUnderlyingType());

        Object eventOne = sendBeanEvent("E1", 13);
        assertFalse(testListener.isInvoked());

        Object eventTwo = sendMarketEvent("E2");
        String[] fields = new String[] {"s0", "s1", "s0stream", "s1stream"};
        EventBean received = testListener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{eventOne, eventTwo, eventOne, eventTwo});
    }

    public void testNoJoinWithAliasWithProperties()
    {
        String viewExpr = "select theString.* as s0, intPrimitive as a, theString.* as s1, intPrimitive as b from " + SupportBean.class.getName() + "#length(3) as theString";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        EventType type = selectTestView.getEventType();
        assertEquals(4, type.getPropertyNames().length);
        assertEquals(Map.class, type.getUnderlyingType());
        assertEquals(int.class, type.getPropertyType("a"));
        assertEquals(int.class, type.getPropertyType("b"));
        assertEquals(SupportBean.class, type.getPropertyType("s0"));
        assertEquals(SupportBean.class, type.getPropertyType("s1"));

        Object theEvent = sendBeanEvent("E1", 12);
        String[] fields = new String[] {"s0", "s1", "a", "b"};
        EPAssertionUtil.assertProps(testListener.assertOneGetNewAndReset(), fields, new Object[]{theEvent, theEvent, 12, 12});
    }

    public void testJoinWithAliasWithProperties()
    {
        String viewExpr = "select intPrimitive, s1.* as s1stream, theString, symbol as sym, s0.* as s0stream from " + SupportBean.class.getName() + "#length(3) as s0, " +
                SupportMarketDataBean.class.getName() + "#keepall() as s1";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        EventType type = selectTestView.getEventType();
        assertEquals(5, type.getPropertyNames().length);
        assertEquals(int.class, type.getPropertyType("intPrimitive"));
        assertEquals(SupportMarketDataBean.class, type.getPropertyType("s1stream"));
        assertEquals(SupportBean.class, type.getPropertyType("s0stream"));
        assertEquals(String.class, type.getPropertyType("sym"));
        assertEquals(String.class, type.getPropertyType("theString"));
        assertEquals(Map.class, type.getUnderlyingType());

        Object eventOne = sendBeanEvent("E1", 13);
        assertFalse(testListener.isInvoked());

        Object eventTwo = sendMarketEvent("E2");
        String[] fields = new String[] {"intPrimitive", "sym", "theString", "s0stream", "s1stream"};
        EventBean received = testListener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{13, "E2", "E1", eventOne, eventTwo});
        EventBean theEvent = (EventBean) ((Map)received.getUnderlying()).get("s0stream");
        assertSame(eventOne, theEvent.getUnderlying());
    }

    public void testNoJoinNoAliasWithProperties()
    {
        String viewExpr = "select intPrimitive as a, string.*, intPrimitive as b from " + SupportBean.class.getName() + "#length(3) as string";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        EventType type = selectTestView.getEventType();
        assertEquals(22, type.getPropertyNames().length);
        assertEquals(Pair.class, type.getUnderlyingType());
        assertEquals(int.class, type.getPropertyType("a"));
        assertEquals(int.class, type.getPropertyType("b"));
        assertEquals(String.class, type.getPropertyType("theString"));

        sendBeanEvent("E1", 10);
        String[] fields = new String[] {"a", "theString", "intPrimitive", "b"};
        EPAssertionUtil.assertProps(testListener.assertOneGetNewAndReset(), fields, new Object[]{10, "E1", 10, 10});
    }

    public void testJoinNoAliasWithProperties()
    {
        String viewExpr = "select intPrimitive, s1.*, symbol as sym from " + SupportBean.class.getName() + "#length(3) as s0, " +
                SupportMarketDataBean.class.getName() + "#keepall() as s1";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        EventType type = selectTestView.getEventType();
        assertEquals(7, type.getPropertyNames().length);
        assertEquals(int.class, type.getPropertyType("intPrimitive"));
        assertEquals(Pair.class, type.getUnderlyingType());

        sendBeanEvent("E1", 11);
        assertFalse(testListener.isInvoked());

        Object theEvent = sendMarketEvent("E1");
        String[] fields = new String[] {"intPrimitive", "sym", "symbol"};
        EventBean received = testListener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{11, "E1", "E1"});
        assertSame(theEvent, ((Pair)received.getUnderlying()).getFirst());
    }

    public void testAloneNoJoinNoAlias()
    {
        String viewExpr = "select theString.* from " + SupportBean.class.getName() + "#length(3) as theString";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        EventType type = selectTestView.getEventType();
        assertTrue(type.getPropertyNames().length > 10);
        assertEquals(SupportBean.class, type.getUnderlyingType());

        Object theEvent = sendBeanEvent("E1");
        assertSame(theEvent, testListener.assertOneGetNewAndReset().getUnderlying());
    }

    public void testAloneNoJoinAlias()
    {
        String viewExpr = "select theString.* as s0 from " + SupportBean.class.getName() + "#length(3) as theString";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        EventType type = selectTestView.getEventType();
        assertEquals(1, type.getPropertyNames().length);
        assertEquals(SupportBean.class, type.getPropertyType("s0"));
        assertEquals(Map.class, type.getUnderlyingType());

        Object theEvent = sendBeanEvent("E1");
        assertSame(theEvent, testListener.assertOneGetNewAndReset().get("s0"));
    }

    public void testAloneJoinAlias()
    {
        String viewExpr = "select s1.* as s1 from " + SupportBean.class.getName() + "#length(3) as s0, " +
                SupportMarketDataBean.class.getName() + "#keepall() as s1";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        EventType type = selectTestView.getEventType();
        assertEquals(SupportMarketDataBean.class, type.getPropertyType("s1"));
        assertEquals(Map.class, type.getUnderlyingType());

        sendBeanEvent("E1");
        assertFalse(testListener.isInvoked());

        Object theEvent = sendMarketEvent("E1");
        assertSame(theEvent, testListener.assertOneGetNewAndReset().get("s1"));

        selectTestView.destroy();

        // reverse streams
        viewExpr = "select s0.* as szero from " + SupportBean.class.getName() + "#length(3) as s0, " +
                SupportMarketDataBean.class.getName() + "#keepall() as s1";
        selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        type = selectTestView.getEventType();
        assertEquals(SupportBean.class, type.getPropertyType("szero"));
        assertEquals(Map.class, type.getUnderlyingType());

        sendMarketEvent("E1");
        assertFalse(testListener.isInvoked());

        theEvent = sendBeanEvent("E1");
        assertSame(theEvent, testListener.assertOneGetNewAndReset().get("szero"));
    }

    public void testAloneJoinNoAlias()
    {
        String viewExpr = "select s1.* from " + SupportBean.class.getName() + "#length(3) as s0, " +
                SupportMarketDataBean.class.getName() + "#keepall() as s1";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        EventType type = selectTestView.getEventType();
        assertEquals(Long.class, type.getPropertyType("volume"));
        assertEquals(SupportMarketDataBean.class, type.getUnderlyingType());

        sendBeanEvent("E1");
        assertFalse(testListener.isInvoked());

        Object theEvent = sendMarketEvent("E1");
        assertSame(theEvent, testListener.assertOneGetNewAndReset().getUnderlying());

        selectTestView.destroy();

        // reverse streams
        viewExpr = "select s0.* from " + SupportBean.class.getName() + "#length(3) as s0, " +
                SupportMarketDataBean.class.getName() + "#keepall() as s1";
        selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        type = selectTestView.getEventType();
        assertEquals(String.class, type.getPropertyType("theString"));
        assertEquals(SupportBean.class, type.getUnderlyingType());

        sendMarketEvent("E1");
        assertFalse(testListener.isInvoked());

        theEvent = sendBeanEvent("E1");
        assertSame(theEvent, testListener.assertOneGetNewAndReset().getUnderlying());
    }

    public void testInvalidSelect()
    {
        tryInvalid("select theString.* as theString, theString from " + SupportBean.class.getName() + "#length(3) as theString",
                   "Error starting statement: Column name 'theString' appears more then once in select clause [select theString.* as theString, theString from com.espertech.esper.support.bean.SupportBean#length(3) as theString]");

        tryInvalid("select s1.* as abc from " + SupportBean.class.getName() + "#length(3) as s0",
                   "Error starting statement: Stream selector 's1.*' does not match any stream name in the from clause [select s1.* as abc from com.espertech.esper.support.bean.SupportBean#length(3) as s0]");

        tryInvalid("select s0.* as abc, s0.* as abc from " + SupportBean.class.getName() + "#length(3) as s0",
                   "Error starting statement: Column name 'abc' appears more then once in select clause [select s0.* as abc, s0.* as abc from com.espertech.esper.support.bean.SupportBean#length(3) as s0]");

        tryInvalid("select s0.*, s1.* from " + SupportBean.class.getName() + "#keepall() as s0, " + SupportBean.class.getName() + "#keepall() as s1",
                   "Error starting statement: A column name must be supplied for all but one stream if multiple streams are selected via the stream.* notation [select s0.*, s1.* from com.espertech.esper.support.bean.SupportBean#keepall() as s0, com.espertech.esper.support.bean.SupportBean#keepall() as s1]");
    }

    private void tryInvalid(String clause, String message)
    {
        try
        {
            epService.getEPAdministrator().createEPL(clause);
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals(message, ex.getMessage());
        }
    }

    private SupportBean sendBeanEvent(String s)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean sendBeanEvent(String s, int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportMarketDataBean sendMarketEvent(String s)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(s, 0d, 0L, "");
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }
}
