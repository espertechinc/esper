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

package com.espertech.esper.regression.expr;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.event.MappedEventBean;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportStaticMethodLib;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestStreamExpr extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testChainedParameterized() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportChainTop", SupportChainTop.class);

        String subexpr="top.getChildOne(\"abc\",10).getChildTwo(\"append\")";
        String epl = "select " +
                subexpr +
                " from SupportChainTop as top";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        runAssertionChainedParam(stmt, subexpr);

        listener.reset();
        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        runAssertionChainedParam(stmt, subexpr);

        // test property hosts a method
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanStaticOuter", SupportBeanStaticOuter.class);
        stmt = epService.getEPAdministrator().createEPL("select inside.getMyString() as val," +
                "inside.insideTwo.getMyOtherString() as val2 " +
                "from SupportBeanStaticOuter");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanStaticOuter());
        EventBean result = listener.assertOneGetNewAndReset();
        assertEquals("hello", result.get("val"));
        assertEquals("hello2", result.get("val2"));
    }

    private void runAssertionChainedParam(EPStatement stmt, String subexpr) {

        Object[][] rows = new Object[][] {
                {subexpr, SupportChainChildTwo.class}
                };
        for (int i = 0; i < rows.length; i++) {
            EventPropertyDescriptor prop = stmt.getEventType().getPropertyDescriptors()[i];
            assertEquals(rows[i][0], prop.getPropertyName());
            assertEquals(rows[i][1], prop.getPropertyType());
        }

        epService.getEPRuntime().sendEvent(new SupportChainTop());
        Object result = listener.assertOneGetNewAndReset().get(subexpr);
        assertEquals("abcappend", ((SupportChainChildTwo)result).getText());
    }

    public void testStreamFunction()
    {
        String prefix = "select * from " + SupportMarketDataBean.class.getName() + " as s0 where " +
                SupportStaticMethodLib.class.getName();
        runAssertionStreamFunction(prefix + ".volumeGreaterZero(s0)");
        runAssertionStreamFunction(prefix + ".volumeGreaterZero(*)");
        runAssertionStreamFunction(prefix + ".volumeGreaterZeroEventBean(s0)");
        runAssertionStreamFunction(prefix + ".volumeGreaterZeroEventBean(*)");
    }

    private void runAssertionStreamFunction(String epl) {

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("ACME", 0, 0L, null));
        assertFalse(listenerOne.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("ACME", 0, 100L, null));
        assertTrue(listenerOne.isInvoked());

        stmtOne.destroy();
    }

    public void testInstanceMethodOuterJoin()
    {
        String textOne = "select symbol, s1.getTheString() as theString from " +
                            SupportMarketDataBean.class.getName() + ".win:keepall() as s0 " +
                            "left outer join " +
                            SupportBean.class.getName() + ".win:keepall() as s1 on s0.symbol=s1.theString";

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 0, 0L, null);
        epService.getEPRuntime().sendEvent(eventA);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), new String[]{"symbol", "theString"}, new Object[]{"ACME", null});
    }

    public void testInstanceMethodStatic()
    {
        String textOne = "select symbol, s1.getSimpleProperty() as simpleprop, s1.makeDefaultBean() as def from " +
                            SupportMarketDataBean.class.getName() + ".win:keepall() as s0 " +
                            "left outer join " +
                            SupportBeanComplexProps.class.getName() + ".win:keepall() as s1 on s0.symbol=s1.simpleProperty";

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 0, 0L, null);
        epService.getEPRuntime().sendEvent(eventA);
        EventBean theEvent = listenerOne.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, new String[]{"symbol", "simpleprop"}, new Object[]{"ACME", null});
        assertNull(theEvent.get("def"));

        SupportBeanComplexProps eventComplexProps = SupportBeanComplexProps.makeDefaultBean();
        eventComplexProps.setSimpleProperty("ACME");
        epService.getEPRuntime().sendEvent(eventComplexProps);
        theEvent = listenerOne.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, new String[]{"symbol", "simpleprop"}, new Object[]{"ACME", "ACME"});
        assertNotNull(theEvent.get("def"));
    }

    public void testStreamInstanceMethodAliased()
    {
        String textOne = "select s0.getVolume() as volume, s0.getSymbol() as symbol, s0.getPriceTimesVolume(2) as pvf from " +
                            SupportMarketDataBean.class.getName() + " as s0 ";

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        EventType type = stmtOne.getEventType();
        assertEquals(3, type.getPropertyNames().length);
        assertEquals(Long.class, type.getPropertyType("volume"));
        assertEquals(String.class, type.getPropertyType("symbol"));
        assertEquals(double.class, type.getPropertyType("pvf"));

        SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 4, 99L, null);
        epService.getEPRuntime().sendEvent(eventA);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), new String[]{"volume", "symbol", "pvf"}, new Object[]{99L, "ACME", 4d * 99L * 2});
    }

    public void testStreamInstanceMethodNoAlias()
    {
        String textOne = "select s0.getVolume(), s0.getPriceTimesVolume(3) from " +
                            SupportMarketDataBean.class.getName() + " as s0 ";

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        EventType type = stmtOne.getEventType();
        assertEquals(2, type.getPropertyNames().length);
        assertEquals(Long.class, type.getPropertyType("s0.getVolume()"));
        assertEquals(double.class, type.getPropertyType("s0.getPriceTimesVolume(3)"));

        SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 4, 2L, null);
        epService.getEPRuntime().sendEvent(eventA);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), new String[]{"s0.getVolume()", "s0.getPriceTimesVolume(3)"}, new Object[]{2L, 4d * 2L * 3d});

        // try instance method that accepts EventBean
        epService.getEPAdministrator().getConfiguration().addEventType("MyTestEvent", MyTestEvent.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "s0.getValueAsInt(s0, 'id') as c0," +
                "s0.getValueAsInt(*, 'id') as c1" +
                " from MyTestEvent as s0");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new MyTestEvent(10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {10, 10});
    }

    public void testJoinStreamSelectNoWildcard()
    {
        // try with alias
        String textOne = "select s0 as s0stream, s1 as s1stream from " +
                            SupportMarketDataBean.class.getName() + ".win:keepall() as s0, " +
                            SupportBean.class.getName() + ".win:keepall() as s1";

        // Attach listener to feed
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtOne.getText());
        assertEquals(textOne, model.toEPL());
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        EventType type = stmtOne.getEventType();
        assertEquals(2, type.getPropertyNames().length);
        assertEquals(SupportMarketDataBean.class, type.getPropertyType("s0stream"));
        assertEquals(SupportBean.class, type.getPropertyType("s1stream"));

        SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 0, 0L, null);
        epService.getEPRuntime().sendEvent(eventA);

        SupportBean eventB = new SupportBean();
        epService.getEPRuntime().sendEvent(eventB);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), new String[]{"s0stream", "s1stream"}, new Object[]{eventA, eventB});

        stmtOne.destroy();

        // try no alias
        textOne = "select s0, s1 from " +
                            SupportMarketDataBean.class.getName() + ".win:keepall() as s0, " +
                            SupportBean.class.getName() + ".win:keepall() as s1";

        // Attach listener to feed
        stmtOne = epService.getEPAdministrator().createEPL(textOne);
        stmtOne.addListener(listenerOne);

        type = stmtOne.getEventType();
        assertEquals(2, type.getPropertyNames().length);
        assertEquals(SupportMarketDataBean.class, type.getPropertyType("s0"));
        assertEquals(SupportBean.class, type.getPropertyType("s1"));

        epService.getEPRuntime().sendEvent(eventA);
        epService.getEPRuntime().sendEvent(eventB);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), new String[]{"s0", "s1"}, new Object[]{eventA, eventB});
    }

    public void testPatternStreamSelectNoWildcard()
    {
        // try with alias
        String textOne = "select * from pattern [every e1=" + SupportMarketDataBean.class.getName() + " -> e2=" +
                            SupportBean.class.getName() + "(" + SupportStaticMethodLib.class.getName() + ".compareEvents(e1, e2))]";

        // Attach listener to feed
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 0, 0L, null);
        epService.getEPRuntime().sendEvent(eventA);

        SupportBean eventB = new SupportBean("ACME", 1);
        epService.getEPRuntime().sendEvent(eventB);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), new String[]{"e1", "e2"}, new Object[]{eventA, eventB});

        stmtOne.destroy();
    }

    public void testStreamSelectConversionFunctionObject()
    {
        String textOne = "insert into EventStream select * from " + SupportBean.class.getName() + ".win:length(100)";
        String textTwo = "insert into EventStream select " + SupportStaticMethodLib.class.getName() + ".convertEvent(s0) from " + SupportMarketDataBean.class.getName() + ".win:length(100) as s0";

        // Attach listener to feed
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);
        EventType type = stmtOne.getEventType();
        assertEquals(SupportBean.class, type.getUnderlyingType());

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(textTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);
        type = stmtTwo.getEventType();
        assertEquals(SupportBean.class, type.getUnderlyingType());

        // send event for joins to match on
        SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 0, 0L, null);
        epService.getEPRuntime().sendEvent(eventA);
        EventBean theEvent = listenerTwo.assertOneGetNewAndReset();
        assertTrue(theEvent.getEventType() instanceof BeanEventType);
        assertTrue (theEvent.getUnderlying() instanceof SupportBean);
        assertEquals("ACME", theEvent.get("theString"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testStreamSelectConversionFunctionMap()
    {
        // try the same with a map
        Map<String, Object> types = new HashMap<String, Object>();
        types.put("one", String.class);
        types.put("two", String.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MapOne", types);
        epService.getEPAdministrator().getConfiguration().addEventType("MapTwo", types);

        // test wrapped
        String textOne = "insert into Stream0 select * from MapOne";
        String textTwo = "insert into Stream0 select " + SupportStaticMethodLib.class.getName() + ".convertEventMap(s0) from MapTwo as s0";

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);
        EventType type = stmtOne.getEventType();
        assertEquals(Map.class, type.getUnderlyingType());

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(textTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);
        type = stmtTwo.getEventType();
        assertEquals(Map.class, type.getUnderlyingType());

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("one", "1");
        values.put("two", "2");
        epService.getEPRuntime().sendEvent(values, "MapTwo");

        EventBean theEvent = listenerTwo.assertOneGetNewAndReset();
        assertTrue (theEvent.getUnderlying() instanceof Map);
        assertEquals("1", theEvent.get("one"));
        assertEquals("|2|", theEvent.get("two"));

        epService.getEPAdministrator().destroyAllStatements();

        // test native
        epService.getEPAdministrator().createEPL("insert into MapOne select " + SupportStaticMethodLib.class.getName() + ".convertEventMap(s0) from MapTwo as s0");
        EPStatement stmtThree = epService.getEPAdministrator().createEPL("select * from MapOne");
        stmtThree.addListener(listener);

        Map<String, Object> valuesTwo = new HashMap<String, Object>();
        valuesTwo.put("one", "3");
        valuesTwo.put("two", "4");
        epService.getEPRuntime().sendEvent(valuesTwo, "MapTwo");
        EventBean eventTwo = listener.assertOneGetNewAndReset();
        assertTrue (eventTwo.getUnderlying() instanceof Map);
        assertTrue (eventTwo instanceof MappedEventBean);
        assertEquals("3", eventTwo.get("one"));
        assertEquals("|4|", eventTwo.get("two"));
    }

    public void testStreamSelectConversionFunctionObjectArray()
    {
        String[] props = {"one", "two"};
        Object[] types = {String.class, String.class};
        epService.getEPAdministrator().getConfiguration().addEventType("OAOne", props, types);
        epService.getEPAdministrator().getConfiguration().addEventType("OATwo", props, types);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        // test wrapped
        String textOne = "insert into Stream0 select * from OAOne";
        String textTwo = "insert into Stream0 select " + SupportStaticMethodLib.class.getName() + ".convertEventObjectArray(s0) from OATwo as s0";

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);
        EventType type = stmtOne.getEventType();
        assertEquals(Object[].class, type.getUnderlyingType());

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(textTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);
        type = stmtTwo.getEventType();
        assertEquals(Object[].class, type.getUnderlyingType());

        epService.getEPRuntime().sendEvent(new Object[] {"1", "2"}, "OATwo");

        EventBean theEvent = listenerTwo.assertOneGetNewAndReset();
        assertTrue (theEvent.getUnderlying() instanceof Object[]);
        assertEquals("1", theEvent.get("one"));
        assertEquals("|2|", theEvent.get("two"));

        epService.getEPAdministrator().destroyAllStatements();

        // test native
        epService.getEPAdministrator().createEPL("insert into OAOne select " + SupportStaticMethodLib.class.getName() + ".convertEventObjectArray(s0) from OATwo as s0");
        EPStatement stmtThree = epService.getEPAdministrator().createEPL("select * from OAOne");
        stmtThree.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[] {"3", "4"}, "OATwo");
        EventBean eventTwo = listener.assertOneGetNewAndReset();
        assertTrue (eventTwo instanceof ObjectArrayBackedEventBean);
        assertTrue (eventTwo.getUnderlying() instanceof Object[]);
        assertEquals("3", eventTwo.get("one"));
        assertEquals("|4|", eventTwo.get("two"));

        epService.getEPAdministrator().destroyAllStatements();

        // test Object-array with single object-array property, should not convert
        epService.getEPAdministrator().getConfiguration().addEventType("OAInner", new String[] {"id"}, new Object[] {String.class});
        epService.getEPAdministrator().getConfiguration().addEventType("OAOuter", new String[] {"inside"}, new Object[] {"OAInner"});

        epService.getEPAdministrator().createEPL("insert into OAOuter select " + this.getClass().getName() + ".getObjectArray() as id from SupportBean").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals("OAOuter", listener.assertOneGetNew().getEventType().getName());
        assertEquals("id1", listener.assertOneGetNew().get("inside.id"));
    }

    public void testInvalidSelect()
    {
        tryInvalid("select s0.getString(1,2,3) from " + SupportBean.class.getName() + " as s0", null);

        tryInvalid("select s0.abc() from " + SupportBean.class.getName() + " as s0",
                   "Error starting statement: Failed to validate select-clause expression 's0.abc()': Failed to solve 'abc' to either an date-time or enumeration method, an event property or a method on the event underlying object: Failed to resolve method 'abc': Could not find enumeration method, date-time method or instance method named 'abc' in class 'com.espertech.esper.support.bean.SupportBean' taking no parameters [select s0.abc() from com.espertech.esper.support.bean.SupportBean as s0]");

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        tryInvalid("select s.theString from pattern [every [2] s=SupportBean] ee",
                "Error starting statement: Failed to validate select-clause expression 's.theString': Failed to resolve property 's.theString' (property 's' is an indexed property and requires an index or enumeration method to access values) [select s.theString from pattern [every [2] s=SupportBean] ee]");
    }

    public static Object[] getObjectArray() {
        return new Object[] {"id1"};
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
            if (message != null)
            {
                assertEquals(message, ex.getMessage());
            }
        }
    }

    private SupportMarketDataBean sendMarketEvent(String s)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(s, 0d, 0L, "");
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private static class MyTestEvent {

        private int id;

        private MyTestEvent(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public int getValueAsInt(EventBean event, String propertyName) {
            return (Integer) event.get(propertyName);
        }
    }
}
