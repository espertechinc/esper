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
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExecEPLStreamExpr implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionChainedParameterized(epService);
        runAssertionStreamFunction(epService);
        runAssertionInstanceMethodOuterJoin(epService);
        runAssertionInstanceMethodStatic(epService);
        runAssertionStreamInstanceMethodAliased(epService);
        runAssertionStreamInstanceMethodNoAlias(epService);
        runAssertionJoinStreamSelectNoWildcard(epService);
        runAssertionPatternStreamSelectNoWildcard(epService);
        runAssertionInvalidSelect(epService);
    }

    private void runAssertionChainedParameterized(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportChainTop", SupportChainTop.class);

        String subexpr = "top.getChildOne(\"abc\",10).getChildTwo(\"append\")";
        String epl = "select " +
                subexpr +
                " from SupportChainTop as top";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionChainedParam(epService, listener, stmt, subexpr);

        listener.reset();
        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        tryAssertionChainedParam(epService, listener, stmt, subexpr);

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

        stmt.destroy();
    }

    private void tryAssertionChainedParam(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt, String subexpr) {

        Object[][] rows = new Object[][]{
                {subexpr, SupportChainChildTwo.class}
        };
        for (int i = 0; i < rows.length; i++) {
            EventPropertyDescriptor prop = stmt.getEventType().getPropertyDescriptors()[i];
            assertEquals(rows[i][0], prop.getPropertyName());
            assertEquals(rows[i][1], prop.getPropertyType());
        }

        epService.getEPRuntime().sendEvent(new SupportChainTop());
        Object result = listener.assertOneGetNewAndReset().get(subexpr);
        assertEquals("abcappend", ((SupportChainChildTwo) result).getText());
    }

    private void runAssertionStreamFunction(EPServiceProvider epService) {
        String prefix = "select * from " + SupportMarketDataBean.class.getName() + " as s0 where " +
                SupportStaticMethodLib.class.getName();
        tryAssertionStreamFunction(epService, prefix + ".volumeGreaterZero(s0)");
        tryAssertionStreamFunction(epService, prefix + ".volumeGreaterZero(*)");
        tryAssertionStreamFunction(epService, prefix + ".volumeGreaterZeroEventBean(s0)");
        tryAssertionStreamFunction(epService, prefix + ".volumeGreaterZeroEventBean(*)");
    }

    private void tryAssertionStreamFunction(EPServiceProvider epService, String epl) {

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("ACME", 0, 0L, null));
        assertFalse(listenerOne.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("ACME", 0, 100L, null));
        assertTrue(listenerOne.isInvoked());

        stmtOne.destroy();
    }

    private void runAssertionInstanceMethodOuterJoin(EPServiceProvider epService) {
        String textOne = "select symbol, s1.getTheString() as theString from " +
                SupportMarketDataBean.class.getName() + "#keepall as s0 " +
                "left outer join " +
                SupportBean.class.getName() + "#keepall as s1 on s0.symbol=s1.theString";

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 0, 0L, null);
        epService.getEPRuntime().sendEvent(eventA);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), new String[]{"symbol", "theString"}, new Object[]{"ACME", null});

        stmtOne.destroy();
    }

    private void runAssertionInstanceMethodStatic(EPServiceProvider epService) {
        String textOne = "select symbol, s1.getSimpleProperty() as simpleprop, s1.makeDefaultBean() as def from " +
                SupportMarketDataBean.class.getName() + "#keepall as s0 " +
                "left outer join " +
                SupportBeanComplexProps.class.getName() + "#keepall as s1 on s0.symbol=s1.simpleProperty";

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

        stmtOne.destroy();
    }

    private void runAssertionStreamInstanceMethodAliased(EPServiceProvider epService) {
        String textOne = "select s0.getVolume() as volume, s0.getSymbol() as symbol, s0.getPriceTimesVolume(2) as pvf from " +
                SupportMarketDataBean.class.getName() + " as s0 ";

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        EventType type = stmtOne.getEventType();
        assertEquals(3, type.getPropertyNames().length);
        assertEquals(Long.class, type.getPropertyType("volume"));
        assertEquals(String.class, type.getPropertyType("symbol"));
        assertEquals(Double.class, type.getPropertyType("pvf"));

        SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 4, 99L, null);
        epService.getEPRuntime().sendEvent(eventA);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), new String[]{"volume", "symbol", "pvf"}, new Object[]{99L, "ACME", 4d * 99L * 2});

        stmtOne.destroy();
    }

    private void runAssertionStreamInstanceMethodNoAlias(EPServiceProvider epService) {
        String textOne = "select s0.getVolume(), s0.getPriceTimesVolume(3) from " +
                SupportMarketDataBean.class.getName() + " as s0 ";

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        EventType type = stmtOne.getEventType();
        assertEquals(2, type.getPropertyNames().length);
        assertEquals(Long.class, type.getPropertyType("s0.getVolume()"));
        assertEquals(Double.class, type.getPropertyType("s0.getPriceTimesVolume(3)"));

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
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{10, 10});

        stmtOne.destroy();
    }

    private void runAssertionJoinStreamSelectNoWildcard(EPServiceProvider epService) {
        // try with alias
        String textOne = "select s0 as s0stream, s1 as s1stream from " +
                SupportMarketDataBean.class.getName() + "#keepall as s0, " +
                SupportBean.class.getName() + "#keepall as s1";

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
                SupportMarketDataBean.class.getName() + "#keepall as s0, " +
                SupportBean.class.getName() + "#keepall as s1";

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

        stmtOne.destroy();
    }

    private void runAssertionPatternStreamSelectNoWildcard(EPServiceProvider epService) {
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

    private void runAssertionInvalidSelect(EPServiceProvider epService) {
        tryInvalid(epService, "select s0.getString(1,2,3) from " + SupportBean.class.getName() + " as s0", null);

        tryInvalid(epService, "select s0.abc() from " + SupportBean.class.getName() + " as s0",
                "Error starting statement: Failed to validate select-clause expression 's0.abc()': Failed to solve 'abc' to either an date-time or enumeration method, an event property or a method on the event underlying object: Failed to resolve method 'abc': Could not find enumeration method, date-time method or instance method named 'abc' in class '" + SupportBean.class.getName() + "' taking no parameters [");

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        tryInvalid(epService, "select s.theString from pattern [every [2] s=SupportBean] ee",
                "Error starting statement: Failed to validate select-clause expression 's.theString': Failed to resolve property 's.theString' (property 's' is an indexed property and requires an index or enumeration method to access values) [select s.theString from pattern [every [2] s=SupportBean] ee]");
    }

    private void tryInvalid(EPServiceProvider epService, String clause, String message) {
        try {
            epService.getEPAdministrator().createEPL(clause);
            fail();
        } catch (EPStatementException ex) {
            if (message != null) {
                SupportMessageAssertUtil.assertMessage(ex, message);
            }
        }
    }

    public static class MyTestEvent {

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
