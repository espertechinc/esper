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

package com.espertech.esper.regression.view;

import com.espertech.esper.client.*;
import com.espertech.esper.client.annotation.Description;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanComplexProps;
import com.espertech.esper.support.bean.SupportBeanKeywords;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;

public class TestSelectExpr extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener testListener;

    public void setUp()
    {
        testListener = new SupportUpdateListener();
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }
    
    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
    }

    public void testPrecedenceNoColumnName() {
        tryPrecedenceNoColumnName("3*2+1", "3*2+1", 7);
        tryPrecedenceNoColumnName("(3*2)+1", "3*2+1", 7);
        tryPrecedenceNoColumnName("3*(2+1)", "3*(2+1)", 9);
    }

    private void tryPrecedenceNoColumnName(String selectColumn, String expectedColumn, Object value) {
        String epl = "select " + selectColumn + " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(testListener);
        if (!stmt.getEventType().getPropertyNames()[0].equals(expectedColumn)) {
            fail("Expected '" + expectedColumn + "' but was " + stmt.getEventType().getPropertyNames()[0]);
        }

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EventBean event = testListener.assertOneGetNewAndReset();
        assertEquals(value, event.get(expectedColumn));
        stmt.destroy();
    }

    public void testGraphSelect()
    {
        epService.getEPAdministrator().createEPL("insert into MyStream select nested from " + SupportBeanComplexProps.class.getName());

        String viewExpr = "select nested.nestedValue, nested.nestedNested.nestedNestedValue from MyStream";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        epService.getEPRuntime().sendEvent(SupportBeanComplexProps.makeDefaultBean());
        assertNotNull(testListener.assertOneGetNewAndReset());
    }

    public void testKeywordsAllowed()
    {
        String fields = "count,escape,every,sum,avg,max,min,coalesce,median,stddev,avedev,events,first,last,unidirectional,pattern,sql,metadatasql,prev,prior,weekday,lastweekday,cast,snapshot,variable,window,left,right,full,outer,join";
        epService.getEPAdministrator().getConfiguration().addEventType("Keywords", SupportBeanKeywords.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " + fields + " from Keywords");
        stmt.addListener(testListener);
        epService.getEPRuntime().sendEvent(new SupportBeanKeywords());
        EPAssertionUtil.assertEqualsExactOrder(stmt.getEventType().getPropertyNames(), fields.split(","));

        EventBean theEvent = testListener.assertOneGetNewAndReset();

        String[] fieldsArr = fields.split(",");
        for (int i = 0; i < fieldsArr.length; i++)
        {
            assertEquals(1, theEvent.get(fieldsArr[i]));
        }
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select escape as stddev, count(*) as count, last from Keywords");
        stmt.addListener(testListener);
        epService.getEPRuntime().sendEvent(new SupportBeanKeywords());

        theEvent = testListener.assertOneGetNewAndReset();
        assertEquals(1, theEvent.get("stddev"));
        assertEquals(1L, theEvent.get("count"));
        assertEquals(1, theEvent.get("last"));
    }

    public void testEscapeString()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        // The following EPL syntax compiles but fails to match a string "A'B", we are looking into:
        // EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean(string='A\\\'B')");

        tryEscapeMatch("A'B", "\"A'B\"");       // opposite quotes
        tryEscapeMatch("A'B", "'A\\'B'");      // escape '
        tryEscapeMatch("A'B", "'A\\u0027B'");   // unicode

        tryEscapeMatch("A\"B", "'A\"B'");       // opposite quotes
        tryEscapeMatch("A\"B", "'A\\\"B'");      // escape "
        tryEscapeMatch("A\"B", "'A\\u0022B'");   // unicode

        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('A\\\'B') @Description(\"A\\\"B\") select * from SupportBean");
        assertEquals("A\'B", stmt.getName());
        Description desc = (Description) stmt.getAnnotations()[1];
        assertEquals("A\"B", desc.value());
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select 'volume' as field1, \"sleep\" as field2, \"\\u0041\" as unicodeA from SupportBean");
        stmt.addListener(testListener);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(testListener.assertOneGetNewAndReset(), new String[]{"field1", "field2", "unicodeA"}, new Object[]{"volume", "sleep", "A"});
        stmt.destroy();

        tryStatementMatch("John's", "select * from SupportBean(theString='John\\'s')");
        tryStatementMatch("John's", "select * from SupportBean(theString='John\\u0027s')");
        tryStatementMatch("Quote \"Hello\"", "select * from SupportBean(theString like \"Quote \\\"Hello\\\"\")");
        tryStatementMatch("Quote \"Hello\"", "select * from SupportBean(theString like \"Quote \\u0022Hello\\u0022\")");
    }

    private void tryEscapeMatch(String property, String escaped)
    {
        String epl = "select * from SupportBean(theString=" + escaped + ")";
        String text = "trying >" + escaped + "< (" + escaped.length() + " chars) EPL " + epl;
        log.info("tryEscapeMatch for " + text);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(testListener);
        epService.getEPRuntime().sendEvent(new SupportBean(property, 1));
        assertEquals(testListener.assertOneGetNewAndReset().get("intPrimitive"), 1);
        stmt.destroy();
    }

    private void tryStatementMatch(String property, String epl)
    {
        String text = "trying EPL " + epl;
        log.info("tryEscapeMatch for " + text);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(testListener);
        epService.getEPRuntime().sendEvent(new SupportBean(property, 1));
        assertEquals(testListener.assertOneGetNewAndReset().get("intPrimitive"), 1);
        stmt.destroy();
    }

    public void testGetEventType()
    {
        String viewExpr = "select theString, boolBoxed aBool, 3*intPrimitive, floatBoxed+floatPrimitive result" +
                          " from " + SupportBean.class.getName() + ".win:length(3) " +
                          " where boolBoxed = true";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        EventType type = selectTestView.getEventType();
        log.debug(".testGetEventType properties=" + Arrays.toString(type.getPropertyNames()));
        EPAssertionUtil.assertEqualsAnyOrder(type.getPropertyNames(), new String[]{"3*intPrimitive", "theString", "result", "aBool"});
        assertEquals(String.class, type.getPropertyType("theString"));
        assertEquals(Boolean.class, type.getPropertyType("aBool"));
        assertEquals(Float.class, type.getPropertyType("result"));
        assertEquals(Integer.class, type.getPropertyType("3*intPrimitive"));
    }

    public void testWindowStats()
    {
        String viewExpr = "select theString, boolBoxed as aBool, 3*intPrimitive, floatBoxed+floatPrimitive as result" +
                          " from " + SupportBean.class.getName() + ".win:length(3) " +
                          " where boolBoxed = true";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(testListener);

        testListener.reset();

        sendEvent("a", false, 0, 0, 0);
        sendEvent("b", false, 0, 0, 0);
        assertTrue(testListener.getLastNewData() == null);
        sendEvent("c", true, 3, 10, 20);

        EventBean received = testListener.getAndResetLastNewData()[0];
        assertEquals("c", received.get("theString"));
        assertEquals(true, received.get("aBool"));
        assertEquals(30f, received.get("result"));
    }

    private void sendEvent(String s, boolean b, int i, float f1, float f2)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setBoolBoxed(b);
        bean.setIntPrimitive(i);
        bean.setFloatPrimitive(f1);
        bean.setFloatBoxed(f2);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Log log = LogFactory.getLog(TestSelectExpr.class);
}
