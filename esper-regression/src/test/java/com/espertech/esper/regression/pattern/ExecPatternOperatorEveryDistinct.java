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
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecPatternOperatorEveryDistinct implements RegressionExecution, SupportBeanConstants {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType("B", SupportBean_B.class);

        runAssertionExpireSeenBeforeKey(epService);
        runAssertionEveryDistinctOverFilter(epService);
        runAssertionRepeatOverDistinct(epService);
        runAssertionTimerWithinOverDistinct(epService);
        runAssertionEveryDistinctOverRepeat(epService);
        runAssertionEveryDistinctOverTimerWithin(epService);
        runAssertionEveryDistinctOverAnd(epService);
        runAssertionEveryDistinctOverOr(epService);
        runAssertionEveryDistinctOverNot(epService);
        runAssertionEveryDistinctOverFollowedBy(epService);
        runAssertionEveryDistinctWithinFollowedBy(epService);
        runAssertionFollowedByWithDistinct(epService);
        runAssertionInvalid(epService);
        runAssertionMonthScoped(epService);
    }

    private void runAssertionExpireSeenBeforeKey(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String expression = "select * from pattern [every-distinct(a.intPrimitive, 1 sec) a=SupportBean(theString like 'A%')]";
        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A1"});

        epService.getEPRuntime().sendEvent(new SupportBean("A2", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A3"});

        epService.getEPRuntime().sendEvent(new SupportBean("A4", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A5", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));

        epService.getEPRuntime().sendEvent(new SupportBean("A4", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A4"});
        epService.getEPRuntime().sendEvent(new SupportBean("A5", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A5"});

        epService.getEPRuntime().sendEvent(new SupportBean("A6", 1));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1999));
        epService.getEPRuntime().sendEvent(new SupportBean("A7", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        epService.getEPRuntime().sendEvent(new SupportBean("A7", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A7"});

        statement.destroy();
    }

    private void runAssertionEveryDistinctOverFilter(EPServiceProvider epService) throws Exception {
        String expression = "select * from pattern [every-distinct(intPrimitive) a=SupportBean]";
        runEveryDistinctOverFilter(epService, expression);

        expression = "select * from pattern [every-distinct(intPrimitive,2 minutes) a=SupportBean]";
        runEveryDistinctOverFilter(epService, expression);
    }

    private void runEveryDistinctOverFilter(EPServiceProvider epService, String expression) {
        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("a.theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        assertEquals("E3", listener.assertOneGetNewAndReset().get("a.theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        assertEquals("E4", listener.assertOneGetNewAndReset().get("a.theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E7", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E8", 0));
        assertEquals("E8", listener.assertOneGetNewAndReset().get("a.theString"));

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(expression);
        assertEquals(expression, model.toEPL());
        epService.getEPAdministrator().create(model);

        statement.destroy();
    }

    private void runAssertionRepeatOverDistinct(EPServiceProvider epService) throws Exception {
        String expression = "select * from pattern [[2] every-distinct(a.intPrimitive) a=SupportBean]";
        runRepeatOverDistinct(epService, expression);

        expression = "select * from pattern [[2] every-distinct(a.intPrimitive, 1 hour) a=SupportBean]";
        runRepeatOverDistinct(epService, expression);
    }

    private void runRepeatOverDistinct(EPServiceProvider epService, String expression) {

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("E1", theEvent.get("a[0].theString"));
        assertEquals("E3", theEvent.get("a[1].theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 2));
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private void runAssertionEveryDistinctOverRepeat(EPServiceProvider epService) throws Exception {
        String expression = "select * from pattern [every-distinct(a[0].intPrimitive) [2] a=SupportBean]";
        runEveryDistinctOverRepeat(epService, expression);

        expression = "select * from pattern [every-distinct(a[0].intPrimitive, a[0].intPrimitive, 1 hour) [2] a=SupportBean]";
        runEveryDistinctOverRepeat(epService, expression);
    }

    private void runEveryDistinctOverRepeat(EPServiceProvider epService, String expression) {

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("E1", theEvent.get("a[0].theString"));
        assertEquals("E2", theEvent.get("a[1].theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 1));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("E5", theEvent.get("a[0].theString"));
        assertEquals("E6", theEvent.get("a[1].theString"));

        statement.destroy();
    }

    private void runAssertionTimerWithinOverDistinct(EPServiceProvider epService) throws Exception {
        // for 10 seconds, look for every distinct A
        String expression = "select * from pattern [(every-distinct(a.intPrimitive) a=SupportBean) where timer:within(10 sec)]";
        runTimerWithinOverDistinct(epService, expression);

        expression = "select * from pattern [(every-distinct(a.intPrimitive, 2 days 2 minutes) a=SupportBean) where timer:within(10 sec)]";
        runTimerWithinOverDistinct(epService, expression);
    }

    private void runTimerWithinOverDistinct(EPServiceProvider epService, String expression) {

        sendTimer(0, epService);
        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("a.theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        assertEquals("E3", listener.assertOneGetNewAndReset().get("a.theString"));

        sendTimer(11000, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 1));
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private void runAssertionEveryDistinctOverTimerWithin(EPServiceProvider epService) throws Exception {
        String expression = "select * from pattern [every-distinct(a.intPrimitive) (a=SupportBean where timer:within(10 sec))]";
        runEveryDistinctOverTimerWithin(epService, expression);

        expression = "select * from pattern [every-distinct(a.intPrimitive, 1 hour) (a=SupportBean where timer:within(10 sec))]";
        runEveryDistinctOverTimerWithin(epService, expression);
    }

    private void runEveryDistinctOverTimerWithin(EPServiceProvider epService, String expression) {

        sendTimer(0, epService);
        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("a.theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        sendTimer(5000, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        assertEquals("E3", listener.assertOneGetNewAndReset().get("a.theString"));

        sendTimer(10000, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 2));
        assertFalse(listener.isInvoked());

        sendTimer(15000, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E7", 2));
        assertFalse(listener.isInvoked());

        sendTimer(20000, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E8", 2));
        assertFalse(listener.isInvoked());

        sendTimer(25000, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E9", 1));
        assertFalse(listener.isInvoked());

        sendTimer(50000, epService);
        epService.getEPRuntime().sendEvent(new SupportBean("E10", 1));
        assertEquals("E10", listener.assertOneGetNewAndReset().get("a.theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E11", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E12", 2));
        assertEquals("E12", listener.assertOneGetNewAndReset().get("a.theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E13", 2));
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private void runAssertionEveryDistinctOverAnd(EPServiceProvider epService) throws Exception {
        String expression = "select * from pattern [every-distinct(a.intPrimitive, b.intPrimitive) (a=SupportBean(theString like 'A%') and b=SupportBean(theString like 'B%'))]";
        runEveryDistinctOverAnd(epService, expression);

        expression = "select * from pattern [every-distinct(a.intPrimitive, b.intPrimitive, 1 hour) (a=SupportBean(theString like 'A%') and b=SupportBean(theString like 'B%'))]";
        runEveryDistinctOverAnd(epService, expression);
    }

    private void runEveryDistinctOverAnd(EPServiceProvider epService, String expression) {

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("B1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B1"});

        epService.getEPRuntime().sendEvent(new SupportBean("A2", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B2", 10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B3", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A3", "B3"});

        epService.getEPRuntime().sendEvent(new SupportBean("A4", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B4", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A4", "B4"});

        epService.getEPRuntime().sendEvent(new SupportBean("A5", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B5", 10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A6", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B6", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A6", "B6"});

        epService.getEPRuntime().sendEvent(new SupportBean("A7", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B7", 20));
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private void runAssertionEveryDistinctOverOr(EPServiceProvider epService) throws Exception {
        String expression = "select * from pattern [every-distinct(coalesce(a.intPrimitive, 0) + coalesce(b.intPrimitive, 0)) (a=SupportBean(theString like 'A%') or b=SupportBean(theString like 'B%'))]";
        runEveryDistinctOverOr(epService, expression);

        expression = "select * from pattern [every-distinct(coalesce(a.intPrimitive, 0) + coalesce(b.intPrimitive, 0), 1 hour) (a=SupportBean(theString like 'A%') or b=SupportBean(theString like 'B%'))]";
        runEveryDistinctOverOr(epService, expression);
    }

    private void runEveryDistinctOverOr(EPServiceProvider epService, String expression) {

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", null});

        epService.getEPRuntime().sendEvent(new SupportBean("B1", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{null, "B1"});

        epService.getEPRuntime().sendEvent(new SupportBean("B2", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("A3", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B3", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("B4", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{null, "B4"});

        epService.getEPRuntime().sendEvent(new SupportBean("B5", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{null, "B5"});

        epService.getEPRuntime().sendEvent(new SupportBean("B6", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("A4", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("A5", 4));
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private void runAssertionEveryDistinctOverNot(EPServiceProvider epService) throws Exception {
        String expression = "select * from pattern [every-distinct(a.intPrimitive) (a=SupportBean(theString like 'A%') and not SupportBean(theString like 'B%'))]";
        runEveryDistinctOverNot(epService, expression);

        expression = "select * from pattern [every-distinct(a.intPrimitive, 1 hour) (a=SupportBean(theString like 'A%') and not SupportBean(theString like 'B%'))]";
        runEveryDistinctOverNot(epService, expression);
    }

    private void runEveryDistinctOverNot(EPServiceProvider epService, String expression) {

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A1"});

        epService.getEPRuntime().sendEvent(new SupportBean("A2", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A3"});

        epService.getEPRuntime().sendEvent(new SupportBean("B1", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A4", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A4"});

        epService.getEPRuntime().sendEvent(new SupportBean("A5", 1));
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private void runAssertionEveryDistinctOverFollowedBy(EPServiceProvider epService) throws Exception {
        String expression = "select * from pattern [every-distinct(a.intPrimitive + b.intPrimitive) (a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%'))]";
        runEveryDistinctOverFollowedBy(epService, expression);

        expression = "select * from pattern [every-distinct(a.intPrimitive + b.intPrimitive, 1 hour) (a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%'))]";
        runEveryDistinctOverFollowedBy(epService, expression);
    }

    private void runEveryDistinctOverFollowedBy(EPServiceProvider epService, String expression) {
        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("B1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B1"});

        epService.getEPRuntime().sendEvent(new SupportBean("A2", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B2", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("B3", -8));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A4", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B4", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A4", "B4"});

        epService.getEPRuntime().sendEvent(new SupportBean("A5", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("B5", 0));
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private void runAssertionEveryDistinctWithinFollowedBy(EPServiceProvider epService) throws Exception {
        String expression = "select * from pattern [(every-distinct(a.intPrimitive) a=SupportBean(theString like 'A%')) -> b=SupportBean(intPrimitive=a.intPrimitive)]";
        runEveryDistinctWithinFollowedBy(epService, expression);

        expression = "select * from pattern [(every-distinct(a.intPrimitive, 2 hours 1 minute) a=SupportBean(theString like 'A%')) -> b=SupportBean(intPrimitive=a.intPrimitive)]";
        runEveryDistinctWithinFollowedBy(epService, expression);
    }

    private void runEveryDistinctWithinFollowedBy(EPServiceProvider epService, String expression) {
        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B1", 0));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("B2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B2"});

        epService.getEPRuntime().sendEvent(new SupportBean("A2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("A3", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("A4", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("B3", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A3", "B3"});

        epService.getEPRuntime().sendEvent(new SupportBean("B4", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("B5", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A2", "B5"});

        epService.getEPRuntime().sendEvent(new SupportBean("A5", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B6", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A6", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("B7", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A6", "B7"});

        statement.destroy();
    }

    private void runAssertionFollowedByWithDistinct(EPServiceProvider epService) throws Exception {
        String expression = "select * from pattern [every-distinct(a.intPrimitive) a=SupportBean(theString like 'A%') -> every-distinct(b.intPrimitive) b=SupportBean(theString like 'B%')]";
        runFollowedByWithDistinct(epService, expression);

        expression = "select * from pattern [every-distinct(a.intPrimitive, 1 day) a=SupportBean(theString like 'A%') -> every-distinct(b.intPrimitive) b=SupportBean(theString like 'B%')]";
        runFollowedByWithDistinct(epService, expression);
    }

    private void runFollowedByWithDistinct(EPServiceProvider epService, String expression) {
        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B1"});
        epService.getEPRuntime().sendEvent(new SupportBean("B2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B2"});
        epService.getEPRuntime().sendEvent(new SupportBean("B3", 0));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A2", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B4", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B4"});

        epService.getEPRuntime().sendEvent(new SupportBean("A3", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B5", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A3", "B5"});
        epService.getEPRuntime().sendEvent(new SupportBean("B6", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("B7", 3));
        EventBean[] events = listener.getAndResetLastNewData();
        EPAssertionUtil.assertPropsPerRowAnyOrder(events, "a.theString,b.theString".split(","),
                new Object[][]{{"A1", "B7"}, {"A3", "B7"}});

        statement.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) throws Exception {
        tryInvalid(epService, "a=A->every-distinct(a.intPrimitive) B",
                "Failed to validate pattern every-distinct expression 'a.intPrimitive': Failed to resolve property 'a.intPrimitive' to a stream or nested property in a stream [a=A->every-distinct(a.intPrimitive) B]");

        tryInvalid(epService, "every-distinct(dummy) A",
                "Failed to validate pattern every-distinct expression 'dummy': Property named 'dummy' is not valid in any stream [every-distinct(dummy) A]");

        tryInvalid(epService, "every-distinct(2 sec) A",
                "Every-distinct node requires one or more distinct-value expressions that each return non-constant result values [every-distinct(2 sec) A]");
    }

    private void runAssertionMonthScoped(EPServiceProvider epService) {
        String[] fields = "a.theString,a.intPrimitive".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();

        sendCurrentTime(epService, "2002-02-01T09:00:00.000");
        epService.getEPAdministrator().createEPL("select * from pattern [every-distinct(theString, 1 month) a=SupportBean]").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        sendCurrentTimeWithMinus(epService, "2002-03-01T09:00:00.000", 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        assertFalse(listener.isInvoked());

        sendCurrentTime(epService, "2002-03-01T09:00:00.000");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 4});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendCurrentTimeWithMinus(EPServiceProvider epService, String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }

    private void sendCurrentTime(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private static void tryInvalid(EPServiceProvider epService, String statement, String message) throws Exception {
        try {
            epService.getEPAdministrator().createPattern(statement);
            fail();
        } catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    private void sendTimer(long timeInMSec, EPServiceProvider epService) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
