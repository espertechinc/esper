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
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.patternassert.*;

import java.lang.reflect.Array;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalidPattern;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecPatternOperatorMatchUntilExpr implements RegressionExecution, SupportBeanConstants {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("A", SupportBean_A.class.getName());
        configuration.addEventType("B", SupportBean_B.class.getName());
        configuration.addEventType("C", SupportBean_C.class.getName());
        configuration.addEventType("SupportBean", SupportBean.class.getName());
        configuration.addImport(SupportStaticMethodLib.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionOp(epService);
        runAssertionSelectArray(epService);
        runAssertionUseFilter(epService);
        runAssertionRepeatUseTags(epService);
        runAssertionArrayFunctionRepeat(epService);
        runAssertionExpressionBounds(epService);
        runAssertionBoundRepeatWithNot(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionOp(EPServiceProvider epService) throws Exception {
        EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase;

        testCase = new EventExpressionCase("a=A(id='A2') until D");
        testCase.add("D1", "a[0]", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("a=A until D");
        testCase.add("D1", "a[0]", events.getEvent("A1"), "a[1]", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=B until a=A");
        testCase.add("A1", "b[0]", null, "a", events.getEvent("A1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=B until D(id='D3')");
        testCase.add("D3", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(a=A or b=B) until d=D(id='D3')");
        testCase.add("D3", new Object[][]{
                {"a[0]", events.getEvent("A1")},
                {"a[1]", events.getEvent("A2")},
                {"b[0]", events.getEvent("B1")},
                {"b[1]", events.getEvent("B2")},
                {"b[2]", events.getEvent("B3")},
                {"d", events.getEvent("D3")}});
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(a=A or b=B) until (g=G or d=D)");
        testCase.add("D1", new Object[][]{
                {"a[0]", events.getEvent("A1")},
                {"a[1]", events.getEvent("A2")},
                {"b[0]", events.getEvent("B1")},
                {"b[1]", events.getEvent("B2")},
                {"d", events.getEvent("D1")}});
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(d=D) until a=A(id='A1')");
        testCase.add("A1");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("a=A until G(id='GX')");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[2] a=A");
        testCase.add("A2", "a[0]", events.getEvent("A1"), "a[1]", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[2:2] a=A");
        testCase.add("A2", "a[0]", events.getEvent("A1"), "a[1]", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[1] a=A");
        testCase.add("A1", "a[0]", events.getEvent("A1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[1:1] a=A");
        testCase.add("A1", "a[0]", events.getEvent("A1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[3] a=A");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[3] b=B");
        testCase.add("B3", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[4] (a=A or b=B)");
        testCase.add("A2", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "a[0]", events.getEvent("A1"), "a[1]", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        // the until ends the matching returning permanently false
        testCase = new EventExpressionCase("[2] b=B until a=A(id='A1')");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[2] b=B until c=C");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[2:2] b=B until g=G(id='G1')");
        testCase.add("B2", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[:4] b=B until g=G(id='G1')");
        testCase.add("G1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"), "g", events.getEvent("G1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[:3] b=B until g=G(id='G1')");
        testCase.add("G1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"), "g", events.getEvent("G1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[:2] b=B until g=G(id='G1')");
        testCase.add("G1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "g", events.getEvent("G1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[:1] b=B until g=G(id='G1')");
        testCase.add("G1", "b[0]", events.getEvent("B1"), "g", events.getEvent("G1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[:1] b=B until a=A(id='A1')");
        testCase.add("A1", "b[0]", null, "a", events.getEvent("A1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[1:] b=B until g=G(id='G1')");
        testCase.add("G1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"), "g", events.getEvent("G1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[1:] b=B until a=A");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[2:] b=B until a=A(id='A2')");
        testCase.add("A2", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "a", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[2:] b=B until c=C");
        testCaseList.addTest(testCase);

        // same event triggering both clauses, until always wins, match does not count
        testCase = new EventExpressionCase("[2:] b=B until e=B(id='B2')");
        testCaseList.addTest(testCase);

        // same event triggering both clauses, until always wins, match does not count
        testCase = new EventExpressionCase("[1:] b=B until e=B(id='B1')");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[1:2] b=B until a=A(id='A2')");
        testCase.add("A2", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", null, "a", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[1:3] b=B until G");
        testCase.add("G1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"), "b[3]", null);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[1:2] b=B until G");
        testCase.add("G1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", null);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[1:10] b=B until F");
        testCase.add("F1", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", null);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[1:10] b=B until C");
        testCase.add("C1", "b[0]", events.getEvent("B1"), "b[1]", null);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[0:1] b=B until C");
        testCase.add("C1", "b[0]", events.getEvent("B1"), "b[1]", null);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("c=C -> [2] b=B -> d=D");
        testCase.add("D3", "c", events.getEvent("C1"), "b[0]", events.getEvent("B2"), "b[1]", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[3] d=D or [3] b=B");
        testCase.add("B3", "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"), "b[2]", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[3] d=D or [4] b=B");
        testCase.add("D3", "d[0]", events.getEvent("D1"), "d[1]", events.getEvent("D2"), "d[2]", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[2] d=D and [2] b=B");
        testCase.add("D2", "d[0]", events.getEvent("D1"), "d[1]", events.getEvent("D2"), "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("d=D until timer:interval(7 sec)");
        testCase.add("E1", "d[0]", events.getEvent("D1"), "d[1]", null, "d[2]", null);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (d=D until b=B)");
        testCase.add("B1", "d[0]", null, "b", events.getEvent("B1"));
        testCase.add("B2", "d[0]", null, "b", events.getEvent("B2"));
        testCase.add("B3", "d[0]", events.getEvent("D1"), "d[1]", events.getEvent("D2"), "d[2]", null, "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        // note precendence: every is higher then until
        testCase = new EventExpressionCase("every d=D until b=B");
        testCase.add("B1", "d[0]", null, "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(every d=D) until b=B");
        testCase.add("B1", "d[0]", null, "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("a=A until (every (timer:interval(6 sec) and not A))");
        testCase.add("G1", "a[0]", events.getEvent("A1"), "a[1]", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("A until (every (timer:interval(7 sec) and not A))");
        testCase.add("D3");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[2] (a=A or b=B)");
        testCase.add("B1", "a[0]", events.getEvent("A1"), "b[0]", events.getEvent("B1"), "b[1]", null);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every [2] a=A");
        testCase.add("A2", new Object[][]{
                {"a[0]", events.getEvent("A1")},
                {"a[1]", events.getEvent("A2")},
        });
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every [2] a=A until d=D");  // every has precedence; ESPER-339
        testCase.add("D1", new Object[][]{
                {"a[0]", events.getEvent("A1")},
                {"a[1]", events.getEvent("A2")},
                {"d", events.getEvent("D1")},
        });
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("[3] (a=A or b=B)");
        testCase.add("B2", "a[0]", events.getEvent("A1"), "b[0]", events.getEvent("B1"), "b[1]", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(a=A until b=B) until c=C");
        testCase.add("C1", "a[0]", events.getEvent("A1"), "b[0]", events.getEvent("B1"), "c", events.getEvent("C1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(a=A until b=B) until g=G");
        testCase.add("G1", new Object[][]{{"a[0]", events.getEvent("A1")}, {"b[0]", events.getEvent("B1")},
            {"a[1]", events.getEvent("A2")}, {"b[1]", events.getEvent("B2")},
            {"b[2]", events.getEvent("B3")},
            {"g", events.getEvent("G1")}
        });
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("B until not B");
        testCaseList.addTest(testCase);

        PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
        util.runTest(epService);
    }

    private void runAssertionSelectArray(EPServiceProvider epService) {
        String stmt = "select a, b, a[0] as a0, a[0].id as a0Id, a[1] as a1, a[1].id as a1Id, a[2] as a2, a[2].id as a2Id from pattern [a=A until b=B]";
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listener);

        Object eventA1 = new SupportBean_A("A1");
        epService.getEPRuntime().sendEvent(eventA1);

        Object eventA2 = new SupportBean_A("A2");
        epService.getEPRuntime().sendEvent(eventA2);
        assertFalse(listener.isInvoked());

        Object eventB1 = new SupportBean_B("B1");
        epService.getEPRuntime().sendEvent(eventB1);

        EventBean theEvent = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("a"), new Object[]{eventA1, eventA2});
        assertSame(eventA1, theEvent.get("a0"));
        assertSame(eventA2, theEvent.get("a1"));
        assertNull(theEvent.get("a2"));
        assertEquals("A1", theEvent.get("a0Id"));
        assertEquals("A2", theEvent.get("a1Id"));
        assertNull(null, theEvent.get("a2Id"));
        assertSame(eventB1, theEvent.get("b"));

        // try wildcard
        stmt = "select * from pattern [a=A until b=B]";
        statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(eventA1);
        epService.getEPRuntime().sendEvent(eventA2);
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(eventB1);

        theEvent = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("a"), new Object[]{eventA1, eventA2});
        assertSame(eventA1, theEvent.get("a[0]"));
        assertSame(eventA2, theEvent.get("a[1]"));
        assertNull(theEvent.get("a[2]"));
        assertEquals("A1", theEvent.get("a[0].id"));
        assertEquals("A2", theEvent.get("a[1].id"));
        assertNull(null, theEvent.get("a[2].id"));
        assertSame(eventB1, theEvent.get("b"));

        statement.destroy();
    }

    private void runAssertionUseFilter(EPServiceProvider epService) {
        String stmt;
        SupportUpdateListener listener;
        EPStatement statement;
        EventBean theEvent;

        stmt = "select * from pattern [a=A until b=B -> c=C(id = ('C' || a[0].id || a[1].id || b.id))]";
        listener = new SupportUpdateListener();
        statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listener);

        Object eventA1 = new SupportBean_A("A1");
        epService.getEPRuntime().sendEvent(eventA1);

        Object eventA2 = new SupportBean_A("A2");
        epService.getEPRuntime().sendEvent(eventA2);

        Object eventB1 = new SupportBean_B("B1");
        epService.getEPRuntime().sendEvent(eventB1);

        epService.getEPRuntime().sendEvent(new SupportBean_C("C1"));
        assertFalse(listener.isInvoked());

        Object eventC1 = new SupportBean_C("CA1A2B1");
        epService.getEPRuntime().sendEvent(eventC1);
        theEvent = listener.assertOneGetNewAndReset();
        assertSame(eventA1, theEvent.get("a[0]"));
        assertSame(eventA2, theEvent.get("a[1]"));
        assertNull(theEvent.get("a[2]"));
        assertSame(eventB1, theEvent.get("b"));
        assertSame(eventC1, theEvent.get("c"));
        statement.destroy();

        // Test equals-optimization with array event
        stmt = "select * from pattern [a=A until b=B -> c=SupportBean(theString = a[1].id)]";
        listener = new SupportUpdateListener();
        statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 20));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A2", 10));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(10, theEvent.get("c.intPrimitive"));
        statement.destroy();

        // Test in-optimization
        stmt = "select * from pattern [a=A until b=B -> c=SupportBean(theString in(a[2].id))]";
        listener = new SupportUpdateListener();
        statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A3"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));

        epService.getEPRuntime().sendEvent(new SupportBean("A2", 20));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 5));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(5, theEvent.get("c.intPrimitive"));
        statement.destroy();

        // Test not-in-optimization
        stmt = "select * from pattern [a=A until b=B -> c=SupportBean(theString!=a[0].id and theString!=a[1].id and theString!=a[2].id)]";
        listener = new SupportUpdateListener();
        statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A3"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));

        epService.getEPRuntime().sendEvent(new SupportBean("A2", 20));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 20));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A6", 5));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(5, theEvent.get("c.intPrimitive"));
        statement.destroy();

        // Test range-optimization
        stmt = "select * from pattern [a=SupportBean(theString like 'A%') until b=SupportBean(theString like 'B%') -> c=SupportBean(intPrimitive between a[0].intPrimitive and a[1].intPrimitive)]";
        listener = new SupportUpdateListener();
        statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 5));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 8));
        epService.getEPRuntime().sendEvent(new SupportBean("B1", -1));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 20));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 5));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(5, theEvent.get("c.intPrimitive"));

        statement.destroy();
    }

    private void runAssertionRepeatUseTags(EPServiceProvider epService) {
        String stmt = "select * from pattern [every [2] (a=A() -> b=B(id=a.id))]";
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("A2"));
        assertTrue(listener.isInvoked());

        statement.destroy();

        // test with timer:interval
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String query = "select * from pattern [every ([2:]e1=SupportBean(theString='2') until timer:interval(5))->([2:]e2=SupportBean(theString='3') until timer:interval(2))]";
        epService.getEPAdministrator().createEPL(query);

        epService.getEPRuntime().sendEvent(new SupportBean("2", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("2", 0));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(5000));

        epService.getEPRuntime().sendEvent(new SupportBean("3", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("3", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("3", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("3", 0));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));

        epService.getEPRuntime().sendEvent(new SupportBean("2", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("2", 0));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(15000));

        // test followed by 3 streams
        epService.getEPAdministrator().destroyAllStatements();
        listener.reset();
        String epl = "select * from pattern [ every [2] A=SupportBean(theString='1') " +
                "-> [2] B=SupportBean(theString='2' and intPrimitive=A[0].intPrimitive)" +
                "-> [2] C=SupportBean(theString='3' and intPrimitive=A[0].intPrimitive)]";
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("1", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("3", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("3", 10));
        assertTrue(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionArrayFunctionRepeat(EPServiceProvider epService) {
        String stmt = "select SupportStaticMethodLib.arrayLength(a) as length, java.lang.reflect.Array.getLength(a) as l2 from pattern [[1:] a=A until B]";
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A3"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("A2"));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(3, theEvent.get("length"));
        assertEquals(3, theEvent.get("l2"));

        statement.destroy();
    }

    private void runAssertionExpressionBounds(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();

        epService.getEPAdministrator().getConfiguration().addVariable("lower", int.class, null);
        epService.getEPAdministrator().getConfiguration().addVariable("upper", int.class, null);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S0", SupportBean_S0.class);

        // test variables - closed bounds
        epService.getEPRuntime().setVariableValue("lower", 2);
        epService.getEPRuntime().setVariableValue("upper", 3);
        String stmtOne = "[lower:upper] a=SupportBean (theString = 'A') until b=SupportBean (theString = 'B')";
        validateStmt(epService, stmtOne, 0, false, null);
        validateStmt(epService, stmtOne, 1, false, null);
        validateStmt(epService, stmtOne, 2, true, 2);
        validateStmt(epService, stmtOne, 3, true, 3);
        validateStmt(epService, stmtOne, 4, true, 3);
        validateStmt(epService, stmtOne, 5, true, 3);

        // test variables - half open
        epService.getEPRuntime().setVariableValue("lower", 3);
        epService.getEPRuntime().setVariableValue("upper", null);
        String stmtTwo = "[lower:] a=SupportBean (theString = 'A') until b=SupportBean (theString = 'B')";
        validateStmt(epService, stmtTwo, 0, false, null);
        validateStmt(epService, stmtTwo, 1, false, null);
        validateStmt(epService, stmtTwo, 2, false, null);
        validateStmt(epService, stmtTwo, 3, true, 3);
        validateStmt(epService, stmtTwo, 4, true, 4);
        validateStmt(epService, stmtTwo, 5, true, 5);

        // test variables - half closed
        epService.getEPRuntime().setVariableValue("lower", null);
        epService.getEPRuntime().setVariableValue("upper", 2);
        String stmtThree = "[:upper] a=SupportBean (theString = 'A') until b=SupportBean (theString = 'B')";
        validateStmt(epService, stmtThree, 0, true, null);
        validateStmt(epService, stmtThree, 1, true, 1);
        validateStmt(epService, stmtThree, 2, true, 2);
        validateStmt(epService, stmtThree, 3, true, 2);
        validateStmt(epService, stmtThree, 4, true, 2);
        validateStmt(epService, stmtThree, 5, true, 2);

        // test followed-by - bounded
        epService.getEPAdministrator().createEPL("@Name('S1') select * from pattern [s0=SupportBean_S0 -> [s0.id] b=SupportBean]").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "b[0].theString,b[1].theString".split(","), new Object[]{"E1", "E2"});

        // test substitution parameter
        String epl = "select * from pattern[[?] SupportBean]";
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(epl);
        prepared.setObject(1, 2);
        epService.getEPAdministrator().create(prepared);

        // test exactly-1
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String eplExact1 = "select * from pattern [a=A -> [1] every (timer:interval(10) and not B)]";
        EPStatement stmtExact1 = epService.getEPAdministrator().createEPL(eplExact1);
        stmtExact1.addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(5000));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(6000));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(15999));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(16000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.id".split(","), new Object[]{"A1"});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(999999));
        assertFalse(listener.isInvoked());
        stmtExact1.destroy();

        // test until
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000000));
        String eplUntilOne = "select * from pattern [a=A -> b=B until ([1] every (timer:interval(10) and not C))]";
        epService.getEPAdministrator().createEPL(eplUntilOne).addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1005000));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1006000));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1014999));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B2"));
        epService.getEPRuntime().sendEvent(new SupportBean_C("C1"));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1015000));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1024998));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1024999));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.id,b[0].id,b[1].id".split(","), new Object[]{"A1", "B1", "B2"});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1999999));
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionBoundRepeatWithNot(EPServiceProvider epService) {

        String[] fields = "e[0].intPrimitive,e[1].intPrimitive".split(",");
        String epl = "select * from pattern [every [2] (e = SupportBean(theString='A') and not SupportBean(theString='B'))]";
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, 2});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 5));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A", 6));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5, 6});
    }

    private void validateStmt(EPServiceProvider epService, String stmtText, int numEventsA, boolean match, Integer matchCount) {
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createPattern(stmtText);
        stmt.addListener(listener);

        for (int i = 0; i < numEventsA; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("A", i));
        }
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("B", -1));

        assertEquals(match, listener.isInvoked());
        if (!match) {
            return;
        }
        Object valueATag = listener.assertOneGetNewAndReset().get("a");
        if (matchCount == null) {
            assertNull(valueATag);
        } else {
            assertEquals((int) matchCount, Array.getLength(valueATag));
        }

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        tryInvalidPattern(epService, "[:0] A until B", "Incorrect range specification, a bounds value of zero or negative value is not allowed [[:0] A until B]");
        tryInvalidPattern(epService, "[10:4] A", "Incorrect range specification, lower bounds value '10' is higher then higher bounds '4' [[10:4] A]");
        tryInvalidPattern(epService, "[-1] A", "Incorrect range specification, a bounds value of zero or negative value is not allowed [[-1] A]");
        tryInvalidPattern(epService, "[4:6] A", "Variable bounds repeat operator requires an until-expression [[4:6] A]");
        tryInvalidPattern(epService, "[0:0] A", "Incorrect range specification, a bounds value of zero or negative value is not allowed [[0:0] A]");
        tryInvalidPattern(epService, "[0] A", "Incorrect range specification, a bounds value of zero or negative value is not allowed [[0] A]");
        tryInvalidPattern(epService, "[1] a=A(a[0].id='a')", "Failed to validate filter expression 'a[0].id=\"a\"': Property named 'a[0].id' is not valid in any stream [[1] a=A(a[0].id='a')]");
        tryInvalidPattern(epService, "a=A -> B(a[0].id='a')", "Failed to validate filter expression 'a[0].id=\"a\"': Property named 'a[0].id' is not valid in any stream [a=A -> B(a[0].id='a')]");
        tryInvalidPattern(epService, "(a=A until c=B) -> c=C", "Tag 'c' for event 'C' has already been declared for events of type " + SupportBean_B.class.getName() + " [(a=A until c=B) -> c=C]");
        tryInvalidPattern(epService, "((a=A until b=B) until a=A)", "Tag 'a' for event 'A' used in the repeat-until operator cannot also appear in other filter expressions [((a=A until b=B) until a=A)]");
        tryInvalidPattern(epService, "a=SupportBean -> [a.theString] b=SupportBean", "Match-until bounds value expressions must return a numeric value [a=SupportBean -> [a.theString] b=SupportBean]");
        tryInvalidPattern(epService, "a=SupportBean -> [:a.theString] b=SupportBean", "Match-until bounds value expressions must return a numeric value [a=SupportBean -> [:a.theString] b=SupportBean]");
        tryInvalidPattern(epService, "a=SupportBean -> [a.theString:1] b=SupportBean", "Match-until bounds value expressions must return a numeric value [a=SupportBean -> [a.theString:1] b=SupportBean]");
    }
}
