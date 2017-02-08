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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.patternassert.*;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;

public class TestNotOperator extends TestCase implements SupportBeanConstants
{
    public void testOp() throws Exception
    {
        EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase = null;

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " and not d=" + EVENT_D_CLASS);
        testCase.add("B1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        String text = "select * from pattern [every b=" + EVENT_B_CLASS + " and not g=" + EVENT_G_CLASS + "]";
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        PatternExpr pattern = Patterns.and()
                .add(Patterns.everyFilter(EVENT_B_CLASS, "b"))
                .add(Patterns.notFilter(EVENT_G_CLASS, "g"));
        model.setFromClause(FromClause.create(PatternStream.create(pattern)));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(text, model.toEPL());
        testCase = new EventExpressionCase(model);
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS + " and not g=" + EVENT_G_CLASS);
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS + " and not d=" + EVENT_D_CLASS);
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " and not a=" + EVENT_A_CLASS + "(id='A1')");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " and not a2=" + EVENT_A_CLASS + "(id='A2')");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + " and not b3=" + EVENT_B_CLASS + "(id='B3'))");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + " or not " + EVENT_D_CLASS + "())");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (every b=" + EVENT_B_CLASS + " and not " + EVENT_B_CLASS + "(id='B2'))");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + " and not " + EVENT_B_CLASS + "(id='B2'))");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(b=" + EVENT_B_CLASS + " -> d=" + EVENT_D_CLASS + ") and " +
                " not " + EVENT_A_CLASS);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(b=" + EVENT_B_CLASS + " -> d=" + EVENT_D_CLASS + ") and " +
                " not " + EVENT_G_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + " -> d=" + EVENT_D_CLASS + ") and " +
                " not " + EVENT_G_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + " -> d=" + EVENT_D_CLASS + ") and " +
                " not " + EVENT_G_CLASS + "(id='x')");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass(), getName());
        util.runTest();
    }

    public void testUniformEvents() throws Exception
    {
        EventCollection events = EventCollectionFactory.getSetTwoExternalClock(0, 1000);
        CaseList results = new CaseList();
        EventExpressionCase desc = null;

        desc = new EventExpressionCase("every a=" + EVENT_A_CLASS + "() and not a1=" + EVENT_A_CLASS + "(id=\"A4\")");
        desc.add("B1", "a", events.getEvent("B1"));
        desc.add("B2", "a", events.getEvent("B2"));
        desc.add("B3", "a", events.getEvent("B3"));
        results.addTest(desc);

        PatternTestHarness util = new PatternTestHarness(events, results, this.getClass(), getName());
        util.runTest();
    }

    public void testNotTimeInterval()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("BBB", SupportBean.class);
        config.addEventType("AAA", SupportMarketDataBean.class);
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String text = "select A.theString as theString from pattern " +
                    "[every A=BBB(intPrimitive=123) -> (timer:interval(30 seconds) and not AAA(volume=123, symbol=A.theString))]";
        EPStatement statement = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendTimer(0, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 123));

        sendTimer(10000, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 123));

        sendTimer(20000, epService);
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("E1", 0, 123L, ""));

        sendTimer(30000, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 123));
        assertFalse(listener.isInvoked());

        sendTimer(40000, epService);
        String fields[] = new String[] {"theString"};
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2"});

        statement.stop();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testNotFollowedBy()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("A", SupportBean.class);
        config.addEventType("B", SupportMarketDataBean.class);
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String stmtText = "select * from pattern [ every( A(intPrimitive>0) -> (B and not A(intPrimitive=0) ) ) ]";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        // A(a=1) A(a=2) A(a=0) A(a=3) ...
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("E5", "M1", 1d));
        assertTrue(listener.isInvoked());

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void sendTimer(long timeInMSec, EPServiceProvider epService)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
