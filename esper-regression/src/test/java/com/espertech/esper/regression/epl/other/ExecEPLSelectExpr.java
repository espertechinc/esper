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
import com.espertech.esper.client.annotation.Description;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.bean.SupportBeanKeywords;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ExecEPLSelectExpr implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPrecedenceNoColumnName(epService);
        runAssertionGraphSelect(epService);
        runAssertionKeywordsAllowed(epService);
        runAssertionEscapeString(epService);
        runAssertionGetEventType(epService);
        runAssertionWindowStats(epService);
    }

    private void runAssertionPrecedenceNoColumnName(EPServiceProvider epService) {
        tryPrecedenceNoColumnName(epService, "3*2+1", "3*2+1", 7);
        tryPrecedenceNoColumnName(epService, "(3*2)+1", "3*2+1", 7);
        tryPrecedenceNoColumnName(epService, "3*(2+1)", "3*(2+1)", 9);
    }

    private void tryPrecedenceNoColumnName(EPServiceProvider epService, String selectColumn, String expectedColumn, Object value) {
        String epl = "select " + selectColumn + " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);
        if (!stmt.getEventType().getPropertyNames()[0].equals(expectedColumn)) {
            fail("Expected '" + expectedColumn + "' but was " + stmt.getEventType().getPropertyNames()[0]);
        }

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EventBean event = testListener.assertOneGetNewAndReset();
        assertEquals(value, event.get(expectedColumn));
        stmt.destroy();
    }

    private void runAssertionGraphSelect(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("insert into MyStream select nested from " + SupportBeanComplexProps.class.getName());

        String epl = "select nested.nestedValue, nested.nestedNested.nestedNestedValue from MyStream";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        epService.getEPRuntime().sendEvent(SupportBeanComplexProps.makeDefaultBean());
        assertNotNull(testListener.assertOneGetNewAndReset());

        stmt.destroy();
    }

    private void runAssertionKeywordsAllowed(EPServiceProvider epService) {
        String fields = "count,escape,every,sum,avg,max,min,coalesce,median,stddev,avedev,events,first,last,unidirectional,pattern,sql,metadatasql,prev,prior,weekday,lastweekday,cast,snapshot,variable,window,left,right,full,outer,join";
        epService.getEPAdministrator().getConfiguration().addEventType("Keywords", SupportBeanKeywords.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " + fields + " from Keywords");
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);
        epService.getEPRuntime().sendEvent(new SupportBeanKeywords());
        EPAssertionUtil.assertEqualsExactOrder(stmt.getEventType().getPropertyNames(), fields.split(","));

        EventBean theEvent = testListener.assertOneGetNewAndReset();

        String[] fieldsArr = fields.split(",");
        for (String aFieldsArr : fieldsArr) {
            assertEquals(1, theEvent.get(aFieldsArr));
        }
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select escape as stddev, count(*) as count, last from Keywords");
        stmt.addListener(testListener);
        epService.getEPRuntime().sendEvent(new SupportBeanKeywords());

        theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(1, theEvent.get("stddev"));
        assertEquals(1L, theEvent.get("count"));
        assertEquals(1, theEvent.get("last"));

        stmt.destroy();
    }

    private void runAssertionEscapeString(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        // The following EPL syntax compiles but fails to match a string "A'B", we are looking into:
        // EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean(string='A\\\'B')");

        tryEscapeMatch(epService, "A'B", "\"A'B\"");       // opposite quotes
        tryEscapeMatch(epService, "A'B", "'A\\'B'");      // escape '
        tryEscapeMatch(epService, "A'B", "'A\\u0027B'");   // unicode

        tryEscapeMatch(epService, "A\"B", "'A\"B'");       // opposite quotes
        tryEscapeMatch(epService, "A\"B", "'A\\\"B'");      // escape "
        tryEscapeMatch(epService, "A\"B", "'A\\u0022B'");   // unicode

        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('A\\\'B') @Description(\"A\\\"B\") select * from SupportBean");
        assertEquals("A\'B", stmt.getName());
        Description desc = (Description) stmt.getAnnotations()[1];
        assertEquals("A\"B", desc.value());
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select 'volume' as field1, \"sleep\" as field2, \"\\u0041\" as unicodeA from SupportBean");
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(testListener.assertOneGetNewAndReset(), new String[]{"field1", "field2", "unicodeA"}, new Object[]{"volume", "sleep", "A"});
        stmt.destroy();

        tryStatementMatch(epService, "John's", "select * from SupportBean(theString='John\\'s')");
        tryStatementMatch(epService, "John's", "select * from SupportBean(theString='John\\u0027s')");
        tryStatementMatch(epService, "Quote \"Hello\"", "select * from SupportBean(theString like \"Quote \\\"Hello\\\"\")");
        tryStatementMatch(epService, "Quote \"Hello\"", "select * from SupportBean(theString like \"Quote \\u0022Hello\\u0022\")");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryEscapeMatch(EPServiceProvider epService, String property, String escaped) {
        String epl = "select * from SupportBean(theString=" + escaped + ")";
        String text = "trying >" + escaped + "< (" + escaped.length() + " chars) EPL " + epl;
        log.info("tryEscapeMatch for " + text);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);
        epService.getEPRuntime().sendEvent(new SupportBean(property, 1));
        assertEquals(testListener.assertOneGetNewAndReset().get("intPrimitive"), 1);
        stmt.destroy();
    }

    private void tryStatementMatch(EPServiceProvider epService, String property, String epl) {
        String text = "trying EPL " + epl;
        log.info("tryEscapeMatch for " + text);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);
        epService.getEPRuntime().sendEvent(new SupportBean(property, 1));
        assertEquals(testListener.assertOneGetNewAndReset().get("intPrimitive"), 1);
        stmt.destroy();
    }

    private void runAssertionGetEventType(EPServiceProvider epService) {
        String epl = "select theString, boolBoxed aBool, 3*intPrimitive, floatBoxed+floatPrimitive result" +
                " from " + SupportBean.class.getName() + "#length(3) " +
                " where boolBoxed = true";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        EventType type = stmt.getEventType();
        log.debug(".testGetEventType properties=" + Arrays.toString(type.getPropertyNames()));
        EPAssertionUtil.assertEqualsAnyOrder(type.getPropertyNames(), new String[]{"3*intPrimitive", "theString", "result", "aBool"});
        assertEquals(String.class, type.getPropertyType("theString"));
        assertEquals(Boolean.class, type.getPropertyType("aBool"));
        assertEquals(Float.class, type.getPropertyType("result"));
        assertEquals(Integer.class, type.getPropertyType("3*intPrimitive"));

        stmt.destroy();
    }

    private void runAssertionWindowStats(EPServiceProvider epService) {
        String epl = "select theString, boolBoxed as aBool, 3*intPrimitive, floatBoxed+floatPrimitive as result" +
                " from " + SupportBean.class.getName() + "#length(3) " +
                " where boolBoxed = true";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        sendEvent(epService, "a", false, 0, 0, 0);
        sendEvent(epService, "b", false, 0, 0, 0);
        assertTrue(testListener.getLastNewData() == null);
        sendEvent(epService, "c", true, 3, 10, 20);

        EventBean received = testListener.getAndResetLastNewData()[0];
        assertEquals("c", received.get("theString"));
        assertEquals(true, received.get("aBool"));
        assertEquals(30f, received.get("result"));

        stmt.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String s, boolean b, int i, float f1, float f2) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setBoolBoxed(b);
        bean.setIntPrimitive(i);
        bean.setFloatPrimitive(f1);
        bean.setFloatBoxed(f2);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecEPLSelectExpr.class);
}
