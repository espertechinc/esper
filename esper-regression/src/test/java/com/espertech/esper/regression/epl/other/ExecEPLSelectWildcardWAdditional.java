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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ExecEPLSelectWildcardWAdditional implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        Map<String, Object> typeMap = new HashMap<>();
        typeMap.put("int", Integer.class);
        typeMap.put("theString", String.class);
        configuration.addEventType("mapEvent", typeMap);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSingleOM(epService);
        runAssertionSingle(epService);
        runAssertionSingleInsertInto(epService);
        runAssertionJoinInsertInto(epService);
        runAssertionJoinNoCommonProperties(epService);
        runAssertionJoinCommonProperties(epService);
        runAssertionCombinedProperties(epService);
        runAssertionMapEvents(epService);
        runAssertionInvalidRepeatedProperties(epService);
    }

    private void runAssertionSingleOM(EPServiceProvider epService) throws Exception {
        String eventName = SupportBeanSimple.class.getName();

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard().add(Expressions.concat("myString", "myString"), "concat"));
        model.setFromClause(FromClause.create(FilterStream.create(eventName).addView(View.create("win", "length", Expressions.constant(5)))));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String text = "select *, myString||myString as concat from " + eventName + ".win:length(5)";
        assertEquals(text, model.toEPL());

        EPStatement statement = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        assertSimple(epService, listener);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("myString", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("myInt", int.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("concat", String.class, null, false, false, false, false, false),
        }, statement.getEventType().getPropertyDescriptors());

        statement.destroy();
    }

    private void runAssertionSingle(EPServiceProvider epService) throws Exception {
        String eventName = SupportBeanSimple.class.getName();
        String text = "select *, myString||myString as concat from " + eventName + "#length(5)";

        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        assertSimple(epService, listener);

        statement.destroy();
    }

    private void runAssertionSingleInsertInto(EPServiceProvider epService) throws InterruptedException {
        String eventName = SupportBeanSimple.class.getName();
        String text = "insert into someEvent select *, myString||myString as concat from " + eventName + "#length(5)";
        String textTwo = "select * from someEvent#length(5)";

        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        statement = epService.getEPAdministrator().createEPL(textTwo);
        SupportUpdateListener insertListener = new SupportUpdateListener();
        statement.addListener(insertListener);
        assertSimple(epService, listener);
        assertProperties(Collections.emptyMap(), insertListener);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionJoinInsertInto(EPServiceProvider epService) throws InterruptedException {
        String eventNameOne = SupportBeanSimple.class.getName();
        String eventNameTwo = SupportMarketDataBean.class.getName();
        String text = "insert into someJoinEvent select *, myString||myString as concat " +
                "from " + eventNameOne + "#length(5) as eventOne, "
                + eventNameTwo + "#length(5) as eventTwo";
        String textTwo = "select * from someJoinEvent#length(5)";

        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        statement = epService.getEPAdministrator().createEPL(textTwo);
        SupportUpdateListener insertListener = new SupportUpdateListener();
        statement.addListener(insertListener);

        assertNoCommonProperties(epService, listener);
        assertProperties(Collections.emptyMap(), insertListener);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionJoinNoCommonProperties(EPServiceProvider epService) throws Exception {
        String eventNameOne = SupportBeanSimple.class.getName();
        String eventNameTwo = SupportMarketDataBean.class.getName();
        String text = "select *, myString||myString as concat " +
                "from " + eventNameOne + "#length(5) as eventOne, "
                + eventNameTwo + "#length(5) as eventTwo";

        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        assertNoCommonProperties(epService, listener);

        statement.destroy();

        text = "select *, myString||myString as concat " +
                "from " + eventNameOne + "#length(5) as eventOne, " +
                eventNameTwo + "#length(5) as eventTwo " +
                "where eventOne.myString = eventTwo.symbol";

        listener.reset();
        statement = epService.getEPAdministrator().createEPL(text);
        statement.addListener(listener);

        assertNoCommonProperties(epService, listener);

        statement.destroy();
    }

    private void runAssertionJoinCommonProperties(EPServiceProvider epService) throws Exception {
        String eventNameOne = SupportBean_A.class.getName();
        String eventNameTwo = SupportBean_B.class.getName();
        String text = "select *, eventOne.id||eventTwo.id as concat " +
                "from " + eventNameOne + "#length(5) as eventOne, " +
                eventNameTwo + "#length(5) as eventTwo ";

        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        assertCommonProperties(epService, listener);

        statement.destroy();

        text = "select *, eventOne.id||eventTwo.id as concat " +
                "from " + eventNameOne + "#length(5) as eventOne, " +
                eventNameTwo + "#length(5) as eventTwo " +
                "where eventOne.id = eventTwo.id";

        listener.reset();
        statement = epService.getEPAdministrator().createEPL(text);
        statement.addListener(listener);

        assertCommonProperties(epService, listener);

        statement.destroy();
    }

    private void runAssertionCombinedProperties(EPServiceProvider epService) throws InterruptedException {
        String eventName = SupportBeanCombinedProps.class.getName();
        String text = "select *, indexed[0].mapped('0ma').value||indexed[0].mapped('0mb').value as concat from " + eventName + "#length(5)";

        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        assertCombinedProps(epService, listener);
        statement.destroy();
    }

    private void runAssertionMapEvents(EPServiceProvider epService) {
        String text = "select *, theString||theString as concat from mapEvent#length(5)";

        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        // The map to send into the runtime
        Map<String, Object> props = new HashMap<>();
        props.put("int", 1);
        props.put("theString", "xx");
        epService.getEPRuntime().sendEvent(props, "mapEvent");

        // The map of expected results
        Map<String, Object> properties = new HashMap<>();
        properties.put("int", 1);
        properties.put("theString", "xx");
        properties.put("concat", "xxxx");

        assertProperties(properties, listener);

        statement.destroy();
    }

    private void runAssertionInvalidRepeatedProperties(EPServiceProvider epService) throws InterruptedException {
        String eventName = SupportBeanSimple.class.getName();
        String text = "select *, myString||myString as myString from " + eventName + "#length(5)";

        try {
            epService.getEPAdministrator().createEPL(text);
            fail();
        } catch (EPException ex) {
            //Expected
        }
    }

    private void assertNoCommonProperties(EPServiceProvider epService, SupportUpdateListener listener) throws InterruptedException {
        SupportBeanSimple eventSimple = sendSimpleEvent(epService, "string");
        SupportMarketDataBean eventMarket = sendMarketEvent(epService, "string");

        EventBean theEvent = listener.getLastNewData()[0];
        Map<String, Object> properties = new HashMap<>();
        properties.put("concat", "stringstring");
        assertProperties(properties, listener);
        assertSame(eventSimple, theEvent.get("eventOne"));
        assertSame(eventMarket, theEvent.get("eventTwo"));
    }

    private void assertSimple(EPServiceProvider epService, SupportUpdateListener listener) throws InterruptedException {
        SupportBeanSimple theEvent = sendSimpleEvent(epService, "string");

        assertEquals("stringstring", listener.getLastNewData()[0].get("concat"));
        Map<String, Object> properties = new HashMap<>();
        properties.put("concat", "stringstring");
        properties.put("myString", "string");
        properties.put("myInt", 0);
        assertProperties(properties, listener);

        assertEquals(Pair.class, listener.getLastNewData()[0].getEventType().getUnderlyingType());
        assertTrue(listener.getLastNewData()[0].getUnderlying() instanceof Pair);
        Pair pair = (Pair) listener.getLastNewData()[0].getUnderlying();
        assertEquals(theEvent, pair.getFirst());
        assertEquals("stringstring", ((Map) pair.getSecond()).get("concat"));
    }

    private void assertCommonProperties(EPServiceProvider epService, SupportUpdateListener listener) throws InterruptedException {
        sendABEvents(epService, "string");
        EventBean theEvent = listener.getLastNewData()[0];
        Map<String, Object> properties = new HashMap<>();
        properties.put("concat", "stringstring");
        assertProperties(properties, listener);
        assertNotNull(theEvent.get("eventOne"));
        assertNotNull(theEvent.get("eventTwo"));
    }

    private void assertCombinedProps(EPServiceProvider epService, SupportUpdateListener listener) throws InterruptedException {
        sendCombinedProps(epService);
        EventBean eventBean = listener.getLastNewData()[0];

        assertEquals("0ma0", eventBean.get("indexed[0].mapped('0ma').value"));
        assertEquals("0ma1", eventBean.get("indexed[0].mapped('0mb').value"));
        assertEquals("1ma0", eventBean.get("indexed[1].mapped('1ma').value"));
        assertEquals("1ma1", eventBean.get("indexed[1].mapped('1mb').value"));

        assertEquals("0ma0", eventBean.get("array[0].mapped('0ma').value"));
        assertEquals("1ma1", eventBean.get("array[1].mapped('1mb').value"));

        assertEquals("0ma00ma1", eventBean.get("concat"));
    }

    private void assertProperties(Map<String, Object> properties, SupportUpdateListener listener) {
        EventBean theEvent = listener.getLastNewData()[0];
        for (String property : properties.keySet()) {
            assertEquals(properties.get(property), theEvent.get(property));
        }
    }

    private SupportBeanSimple sendSimpleEvent(EPServiceProvider epService, String s) {
        SupportBeanSimple bean = new SupportBeanSimple(s, 0);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportMarketDataBean sendMarketEvent(EPServiceProvider epService, String symbol) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0.0, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void sendABEvents(EPServiceProvider epService, String id) {
        SupportBean_A beanOne = new SupportBean_A(id);
        SupportBean_B beanTwo = new SupportBean_B(id);
        epService.getEPRuntime().sendEvent(beanOne);
        epService.getEPRuntime().sendEvent(beanTwo);
    }

    private void sendCombinedProps(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(SupportBeanCombinedProps.makeDefaultBean());
    }
}
