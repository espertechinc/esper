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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.patternassert.*;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;

public class TestWhileGuard extends TestCase implements SupportBeanConstants
{
    public void testOp() throws Exception
    {
        EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase = null;

        testCase = new EventExpressionCase("a=A -> (every b=B) while(b.id != 'B2')");
        testCase.add("B1", "a", events.getEvent("A1"), "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("a=A -> (every b=B) while(b.id != 'B3')");
        testCase.add("B1", "a", events.getEvent("A1"), "b", events.getEvent("B1"));
        testCase.add("B2", "a", events.getEvent("A1"), "b", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(every b=B) while(b.id != 'B3')");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        String text = "select * from pattern [(every b=" + EVENT_B_CLASS + ") while (b.id!=\"B3\")]";
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        Expression guardExpr = Expressions.neq("b.id", "B3");
        PatternExpr every = Patterns.every(Patterns.filter(Filter.create(EVENT_B_CLASS), "b"));
        PatternExpr patternGuarded = Patterns.whileGuard(every, guardExpr);
        model.setFromClause(FromClause.create(PatternStream.create(patternGuarded)));
        assertEquals(text, model.toEPL());
        testCase = new EventExpressionCase(model);
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(every b=B) while(b.id != 'B1')");
        testCaseList.addTest(testCase);

        PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass(), getName());
        util.runTest();
    }
    
    public void testVariable() {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class.getName());
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addVariable("myVariable", "boolean", true);

        String expression =
            "select * from pattern [every a=SupportBean(theString like 'A%') -> (every b=SupportBean(theString like 'B%')) while (myVariable)]";

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B1", 100));
        assertEquals(2, listener.getAndResetLastNewData().length);

        epService.getEPRuntime().setVariableValue("myVariable", false);
        
        epService.getEPRuntime().sendEvent(new SupportBean("A3", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("A4", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("B2", 200));
        assertFalse(listener.isInvoked());
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testInvalid() {
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        tryInvalid(epService, "select * from pattern [every SupportBean while ('abc')]",
                "Invalid parameter for pattern guard 'SupportBean while (\"abc\")': Expression pattern guard requires a single expression as a parameter returning a true or false (boolean) value [select * from pattern [every SupportBean while ('abc')]]");
        tryInvalid(epService, "select * from pattern [every SupportBean while (abc)]",
                "Failed to validate pattern guard expression 'abc': Property named 'abc' is not valid in any stream [select * from pattern [every SupportBean while (abc)]]");
    }

    private void tryInvalid(EPServiceProvider epService, String epl, String message) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }
}