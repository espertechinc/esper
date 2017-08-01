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
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.bean.SupportBean_N;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportTradeEvent;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.patternassert.*;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecPatternUseResult implements RegressionExecution, SupportBeanConstants {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNumeric(epService);
        runAssertionObjectId(epService);
        runAssertionFollowedByFilter(epService);
        runAssertionPatternTypeCacheForRepeat(epService);
    }

    private void runAssertionPatternTypeCacheForRepeat(EPServiceProvider epService) throws Exception {
        // UEJ-229-28464 bug fix for type reuse for dissimilar types
        String epl = "create objectarray schema TypeOne(symbol string, price double);\n" +
                "create objectarray schema TypeTwo(symbol string, market string, price double);\n" +
                "\n" +
                "@Name('Out2') select a[0].symbol from pattern [ [2] a=TypeOne ]\n;" +
                "@Name('Out3') select a[0].market from pattern [ [2] a=TypeTwo ];";
        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        SupportUpdateListener listenerOut2 = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("Out2").addListener(listenerOut2);

        SupportUpdateListener listenerOut3 = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("Out3").addListener(listenerOut3);

        epService.getEPRuntime().sendEvent(new Object[] {"GE", 10}, "TypeOne");
        epService.getEPRuntime().sendEvent(new Object[] {"GE", 10}, "TypeOne");
        assertTrue(listenerOut2.getIsInvokedAndReset());

        epService.getEPRuntime().sendEvent(new Object[] {"GE", "m1", 5}, "TypeTwo");
        epService.getEPRuntime().sendEvent(new Object[] {"GE", "m2", 5}, "TypeTwo");
        assertTrue(listenerOut3.getIsInvokedAndReset());

        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(result.getDeploymentId());
    }

    private void runAssertionNumeric(EPServiceProvider epService) throws Exception {
        final String event = SupportBean_N.class.getName();

        EventCollection events = EventCollectionFactory.getSetThreeExternalClock(0, 1000);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase;

        testCase = new EventExpressionCase("na=" + event + " -> nb=" + event + "(doublePrimitive = na.doublePrimitive)");
        testCase.add("N6", "na", events.getEvent("N1"), "nb", events.getEvent("N6"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("na=" + event + "(intPrimitive=87) -> nb=" + event + "(intPrimitive > na.intPrimitive)");
        testCase.add("N8", "na", events.getEvent("N3"), "nb", events.getEvent("N8"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("na=" + event + "(intPrimitive=87) -> nb=" + event + "(intPrimitive < na.intPrimitive)");
        testCase.add("N4", "na", events.getEvent("N3"), "nb", events.getEvent("N4"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("na=" + event + "(intPrimitive=66) -> every nb=" + event + "(intPrimitive >= na.intPrimitive)");
        testCase.add("N3", "na", events.getEvent("N2"), "nb", events.getEvent("N3"));
        testCase.add("N4", "na", events.getEvent("N2"), "nb", events.getEvent("N4"));
        testCase.add("N8", "na", events.getEvent("N2"), "nb", events.getEvent("N8"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("na=" + event + "(boolBoxed=false) -> every nb=" + event + "(boolPrimitive = na.boolPrimitive)");
        testCase.add("N4", "na", events.getEvent("N2"), "nb", events.getEvent("N4"));
        testCase.add("N5", "na", events.getEvent("N2"), "nb", events.getEvent("N5"));
        testCase.add("N8", "na", events.getEvent("N2"), "nb", events.getEvent("N8"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every na=" + event + " -> every nb=" + event + "(intPrimitive=na.intPrimitive)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every na=" + event + "() -> every nb=" + event + "(doublePrimitive=na.doublePrimitive)");
        testCase.add("N5", "na", events.getEvent("N2"), "nb", events.getEvent("N5"));
        testCase.add("N6", "na", events.getEvent("N1"), "nb", events.getEvent("N6"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every na=" + event + "(boolBoxed=false) -> every nb=" + event + "(boolBoxed=na.boolBoxed)");
        testCase.add("N5", "na", events.getEvent("N2"), "nb", events.getEvent("N5"));
        testCase.add("N8", "na", events.getEvent("N2"), "nb", events.getEvent("N8"));
        testCase.add("N8", "na", events.getEvent("N5"), "nb", events.getEvent("N8"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("na=" + event + "(boolBoxed=false) -> nb=" + event + "(intPrimitive<na.intPrimitive)" +
                " -> nc=" + event + "(intPrimitive > nb.intPrimitive)");
        testCase.add("N6", "na", events.getEvent("N2"), "nb", events.getEvent("N5"), "nc", events.getEvent("N6"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("na=" + event + "(intPrimitive=86) -> nb=" + event + "(intPrimitive<na.intPrimitive)" +
                " -> nc=" + event + "(intPrimitive > na.intPrimitive)");
        testCase.add("N8", "na", events.getEvent("N4"), "nb", events.getEvent("N5"), "nc", events.getEvent("N8"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("na=" + event + "(intPrimitive=86) -> (nb=" + event + "(intPrimitive<na.intPrimitive)" +
                " or nc=" + event + "(intPrimitive > na.intPrimitive))");
        testCase.add("N5", "na", events.getEvent("N4"), "nb", events.getEvent("N5"), "nc", null);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("na=" + event + "(intPrimitive=86) -> (nb=" + event + "(intPrimitive>na.intPrimitive)" +
                " or nc=" + event + "(intBoxed < na.intBoxed))");
        testCase.add("N8", "na", events.getEvent("N4"), "nb", events.getEvent("N8"), "nc", null);
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("na=" + event + "(intPrimitive=86) -> (nb=" + event + "(intPrimitive>na.intPrimitive)" +
                " and nc=" + event + "(intBoxed < na.intBoxed))");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("na=" + event + "() -> every nb=" + event + "(doublePrimitive in [0:na.doublePrimitive])");
        testCase.add("N4", "na", events.getEvent("N1"), "nb", events.getEvent("N4"));
        testCase.add("N6", "na", events.getEvent("N1"), "nb", events.getEvent("N6"));
        testCase.add("N7", "na", events.getEvent("N1"), "nb", events.getEvent("N7"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("na=" + event + "() -> every nb=" + event + "(doublePrimitive in (0:na.doublePrimitive))");
        testCase.add("N4", "na", events.getEvent("N1"), "nb", events.getEvent("N4"));
        testCase.add("N7", "na", events.getEvent("N1"), "nb", events.getEvent("N7"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("na=" + event + "() -> every nb=" + event + "(intPrimitive in (na.intPrimitive:na.doublePrimitive))");
        testCase.add("N7", "na", events.getEvent("N1"), "nb", events.getEvent("N7"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("na=" + event + "() -> every nb=" + event + "(intPrimitive in (na.intPrimitive:60))");
        testCase.add("N6", "na", events.getEvent("N1"), "nb", events.getEvent("N6"));
        testCase.add("N7", "na", events.getEvent("N1"), "nb", events.getEvent("N7"));
        testCaseList.addTest(testCase);

        PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
        util.runTest(epService);
    }

    private void runAssertionObjectId(EPServiceProvider epService) throws Exception {
        final String event = SupportBean_S0.class.getName();

        EventCollection events = EventCollectionFactory.getSetFourExternalClock(0, 1000);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase;

        testCase = new EventExpressionCase("X1=" + event + "() -> X2=" + event + "(p00=X1.p00)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("X1=" + event + "(p00='B') -> X2=" + event + "(p00=X1.p00)");
        testCase.add("e6", "X1", events.getEvent("e2"), "X2", events.getEvent("e6"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("X1=" + event + "(p00='B') -> every X2=" + event + "(p00=X1.p00)");
        testCase.add("e6", "X1", events.getEvent("e2"), "X2", events.getEvent("e6"));
        testCase.add("e11", "X1", events.getEvent("e2"), "X2", events.getEvent("e11"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every X1=" + event + "(p00='B') -> every X2=" + event + "(p00=X1.p00)");
        testCase.add("e6", "X1", events.getEvent("e2"), "X2", events.getEvent("e6"));
        testCase.add("e11", "X1", events.getEvent("e2"), "X2", events.getEvent("e11"));
        testCase.add("e11", "X1", events.getEvent("e6"), "X2", events.getEvent("e11"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every X1=" + event + "() -> X2=" + event + "(p00=X1.p00)");
        testCase.add("e6", "X1", events.getEvent("e2"), "X2", events.getEvent("e6"));
        testCase.add("e8", "X1", events.getEvent("e3"), "X2", events.getEvent("e8"));
        testCase.add("e10", "X1", events.getEvent("e9"), "X2", events.getEvent("e10"));
        testCase.add("e11", "X1", events.getEvent("e6"), "X2", events.getEvent("e11"));
        testCase.add("e12", "X1", events.getEvent("e7"), "X2", events.getEvent("e12"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every X1=" + event + "() -> every X2=" + event + "(p00=X1.p00)");
        testCase.add("e6", "X1", events.getEvent("e2"), "X2", events.getEvent("e6"));
        testCase.add("e8", "X1", events.getEvent("e3"), "X2", events.getEvent("e8"));
        testCase.add("e10", "X1", events.getEvent("e9"), "X2", events.getEvent("e10"));
        testCase.add("e11", "X1", events.getEvent("e2"), "X2", events.getEvent("e11"));
        testCase.add("e11", "X1", events.getEvent("e6"), "X2", events.getEvent("e11"));
        testCase.add("e12", "X1", events.getEvent("e7"), "X2", events.getEvent("e12"));
        testCaseList.addTest(testCase);

        PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
        util.runTest(epService);
    }

    private void runAssertionFollowedByFilter(EPServiceProvider epService) {
        // Test for ESPER-121
        epService.getEPAdministrator().getConfiguration().addEventType("FxTradeEvent", SupportTradeEvent.class);

        String expression = "every tradeevent1=FxTradeEvent(userId in ('U1000','U1001','U1002') ) -> " +
                "(tradeevent2=FxTradeEvent(userId in ('U1000','U1001','U1002') and " +
                "  userId != tradeevent1.userId and " +
                "  ccypair = tradeevent1.ccypair and " +
                "  direction = tradeevent1.direction) -> " +
                " tradeevent3=FxTradeEvent(userId in ('U1000','U1001','U1002') and " +
                "  userId != tradeevent1.userId and " +
                "  userId != tradeevent2.userId and " +
                "  ccypair = tradeevent1.ccypair and " +
                "  direction = tradeevent1.direction)" +
                ") where timer:within(600 sec)";

        EPStatement statement = epService.getEPAdministrator().createPattern(expression);
        MyUpdateListener listener = new MyUpdateListener();
        statement.addListener(listener);

        Random random = new Random();
        String[] users = {"U1000", "U1001", "U1002"};
        String[] ccy = {"USD", "JPY", "EUR"};
        String[] direction = {"B", "S"};

        for (int i = 0; i < 100; i++) {
            SupportTradeEvent theEvent = new
                    SupportTradeEvent(i, users[random.nextInt(users.length)],
                    ccy[random.nextInt(ccy.length)], direction[random.nextInt(direction.length
            )]);
            epService.getEPRuntime().sendEvent(theEvent);
        }

        assertEquals(0, listener.badMatchCount);
        statement.destroy();
    }

    private class MyUpdateListener implements UpdateListener {
        private int badMatchCount;

        public void update(EventBean[] newEvents, EventBean[]
                oldEvents) {
            if (newEvents != null) {
                for (EventBean eventBean : newEvents) {
                    handleEvent(eventBean);
                }
            }
        }

        private void handleEvent(EventBean eventBean) {
            SupportTradeEvent tradeevent1 = (SupportTradeEvent)
                    eventBean.get("tradeevent1");
            SupportTradeEvent tradeevent2 = (SupportTradeEvent)
                    eventBean.get("tradeevent2");
            SupportTradeEvent tradeevent3 = (SupportTradeEvent)
                    eventBean.get("tradeevent3");

            if (tradeevent1.getUserId().equals(tradeevent2.getUserId()) ||
                            tradeevent1.getUserId().equals(tradeevent3.getUserId()) ||
                            tradeevent2.getUserId().equals(tradeevent3.getUserId())) {
                /*
                System.out.println("Bad Match : ");
                System.out.println(tradeevent1);
                System.out.println(tradeevent2);
                System.out.println(tradeevent3 + "\n");
                */
                badMatchCount++;
            }
        }
    }
}
