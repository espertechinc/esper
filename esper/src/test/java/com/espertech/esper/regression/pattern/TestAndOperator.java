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

package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.regression.support.*;
import com.espertech.esper.support.bean.SupportBeanConstants;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportBean_B;
import com.espertech.esper.support.bean.SupportBean_C;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;

public class TestAndOperator extends TestCase implements SupportBeanConstants
{
    public void testOp() throws Exception
    {
        EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase;

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " and every d=" + EVENT_D_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS + " and d=" + EVENT_D_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " and d=" + EVENT_D_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every(b=" + EVENT_B_CLASS + " and d=" + EVENT_D_CLASS + ")");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
        testCaseList.addTest(testCase);

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        PatternExpr pattern = Patterns.every(Patterns.and(Patterns.filter(EVENT_B_CLASS, "b"), Patterns.filter(EVENT_D_CLASS, "d")));
        model.setFromClause(FromClause.create(PatternStream.create(pattern)));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals("select * from pattern [every (b=" + EVENT_B_CLASS + " and d=" + EVENT_D_CLASS + ")]", model.toEPL());
        testCase = new EventExpressionCase(model);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every( b=" + EVENT_B_CLASS + " and every d=" + EVENT_D_CLASS + ")");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS + " and every d=" + EVENT_D_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
        testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D1"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every( every b=" + EVENT_B_CLASS + " and d=" + EVENT_D_CLASS + ")");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D1"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every a=" + EVENT_A_CLASS + " and d=" + EVENT_D_CLASS + " and b=" + EVENT_B_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every( every b=" + EVENT_B_CLASS + " and every d=" + EVENT_D_CLASS + ")");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
        testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D1"));
        for (int i = 0; i < 3; i++)
        {
            testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"));
        }
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"));
        for (int i = 0; i < 5; i++)
        {
            testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        }
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("a=" + EVENT_A_CLASS + " and d=" + EVENT_D_CLASS + " and b=" + EVENT_B_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every a=" + EVENT_A_CLASS + " and every d=" + EVENT_D_CLASS + " and b=" + EVENT_B_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A2"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"), "a", events.getEvent("A1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"), "a", events.getEvent("A2"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"), "a", events.getEvent("A1"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"), "a", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS + " and b=" + EVENT_B_CLASS);
        testCase.add("B1", "b", events.getEvent("B1"), "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every a=" + EVENT_A_CLASS + " and every d=" + EVENT_D_CLASS + " and every b=" + EVENT_B_CLASS);
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A2"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"), "a", events.getEvent("A2"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"), "a", events.getEvent("A1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"), "a", events.getEvent("A2"));
        testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"), "a", events.getEvent("A1"));
        testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"), "a", events.getEvent("A2"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D1"), "a", events.getEvent("A2"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"), "a", events.getEvent("A1"));
        testCase.add("B3", "b", events.getEvent("B3"), "d", events.getEvent("D2"), "a", events.getEvent("A2"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"), "a", events.getEvent("A1"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"), "a", events.getEvent("A2"));
        testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"), "a", events.getEvent("A1"));
        testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"), "a", events.getEvent("A2"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"), "a", events.getEvent("A1"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"), "a", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (a=" + EVENT_A_CLASS + " and every d=" + EVENT_D_CLASS + " and b=" + EVENT_B_CLASS + ")");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"), "a", events.getEvent("A1"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"), "a", events.getEvent("A1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + " and b=" + EVENT_B_CLASS + ")");
        testCase.add("B1", "b", events.getEvent("B1"), "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"), "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"), "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass(), getName());
        util.runTest();
    }

    public void testAndNotDefaultTrue() {

        // ESPER-402
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        SupportUpdateListener listener = new SupportUpdateListener();
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}
        engine.getEPAdministrator().getConfiguration().addEventType("CallWaiting", SupportBean_A.class);
        engine.getEPAdministrator().getConfiguration().addEventType("CallFinished", SupportBean_B.class);
        engine.getEPAdministrator().getConfiguration().addEventType("CallPickedUp", SupportBean_C.class);
        String pattern =
                " insert into NumberOfWaitingCalls(calls) " +
                " select count(*)" +
                " from pattern[every call=CallWaiting ->" + 
                        " (not CallFinished(id=call.id) and" +
                        " not CallPickedUp(id=call.id))]";
        engine.getEPAdministrator().createEPL(pattern).addListener(listener);
        engine.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        engine.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        engine.getEPRuntime().sendEvent(new SupportBean_C("C1"));
        assertTrue(listener.isInvoked());
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testAndWithEveryAndTerminationOptimization() {
        // When all other sub-expressions to an AND are gone,
        // then there is no need to retain events of the subexpression still active
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        engine.getEPAdministrator().getConfiguration().addEventType(SupportBean_A.class);
        engine.getEPAdministrator().getConfiguration().addEventType(SupportBean_B.class);

        String epl = "select * from pattern [a=SupportBean_A and every b=SupportBean_B]";
        EPStatement stmt = engine.getEPAdministrator().createEPL(epl);

        engine.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        for (int i = 0; i < 10; i++) {
            engine.getEPRuntime().sendEvent(new SupportBean_B("B" + i));
        }

        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        engine.getEPRuntime().sendEvent(new SupportBean_B("B_last"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.id,b.id".split(","), new Object[] {"A1", "B_last"});
    }
}