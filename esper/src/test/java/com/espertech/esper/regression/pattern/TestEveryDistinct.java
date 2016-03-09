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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanConstants;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportBean_B;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestEveryDistinct extends TestCase implements SupportBeanConstants
{
    public void testExpireSeenBeforeKey() {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}

        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String expression = "select * from pattern [every-distinct(a.intPrimitive, 1 sec) a=SupportBean(theString like 'A%')]";
        EPStatement statement = engine.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A1"});

        engine.getEPRuntime().sendEvent(new SupportBean("A2", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("A3", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A3"});

        engine.getEPRuntime().sendEvent(new SupportBean("A4", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("A5", 2));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));

        engine.getEPRuntime().sendEvent(new SupportBean("A4", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A4"});
        engine.getEPRuntime().sendEvent(new SupportBean("A5", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A5"});

        engine.getEPRuntime().sendEvent(new SupportBean("A6", 1));
        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(1999));
        engine.getEPRuntime().sendEvent(new SupportBean("A7", 2));
        assertFalse(listener.isInvoked());
        
        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        engine.getEPRuntime().sendEvent(new SupportBean("A7", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A7"});

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testEveryDistinctOverFilter() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}

        String expression = "select * from pattern [every-distinct(intPrimitive) a=SupportBean]";
        runEveryDistinctOverFilter(engine, expression);

        expression = "select * from pattern [every-distinct(intPrimitive,2 minutes) a=SupportBean]";
        runEveryDistinctOverFilter(engine, expression);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void runEveryDistinctOverFilter(EPServiceProvider engine, String expression) {
        EPStatement statement = engine.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("a.theString"));

        engine.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        assertEquals("E3", listener.assertOneGetNewAndReset().get("a.theString"));

        engine.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        assertEquals("E4", listener.assertOneGetNewAndReset().get("a.theString"));

        engine.getEPRuntime().sendEvent(new SupportBean("E5", 2));
        engine.getEPRuntime().sendEvent(new SupportBean("E6", 3));
        engine.getEPRuntime().sendEvent(new SupportBean("E7", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("E8", 0));
        assertEquals("E8", listener.assertOneGetNewAndReset().get("a.theString"));

        EPStatementObjectModel model = engine.getEPAdministrator().compileEPL(expression);
        assertEquals(expression, model.toEPL());
        engine.getEPAdministrator().create(model);

        statement.destroy();
    }

    public void testRepeatOverDistinct() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}

        String expression = "select * from pattern [[2] every-distinct(a.intPrimitive) a=SupportBean]";
        runRepeatOverDistinct(engine, expression);

        expression = "select * from pattern [[2] every-distinct(a.intPrimitive, 1 hour) a=SupportBean]";
        runRepeatOverDistinct(engine, expression);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void runRepeatOverDistinct(EPServiceProvider engine, String expression) {

        EPStatement statement = engine.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("E1", theEvent.get("a[0].theString"));
        assertEquals("E3", theEvent.get("a[1].theString"));

        engine.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        engine.getEPRuntime().sendEvent(new SupportBean("E5", 2));
        assertFalse(listener.isInvoked());
    }

    public void testEveryDistinctOverRepeat() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}

        String expression = "select * from pattern [every-distinct(a[0].intPrimitive) [2] a=SupportBean]";
        runEveryDistinctOverRepeat(engine, expression);

        expression = "select * from pattern [every-distinct(a[0].intPrimitive, a[0].intPrimitive, 1 hour) [2] a=SupportBean]";
        runEveryDistinctOverRepeat(engine, expression);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void runEveryDistinctOverRepeat(EPServiceProvider engine, String expression) {

        EPStatement statement = engine.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("E1", theEvent.get("a[0].theString"));
        assertEquals("E2", theEvent.get("a[1].theString"));

        engine.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("E4", 2));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("E5", 2));
        engine.getEPRuntime().sendEvent(new SupportBean("E6", 1));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("E5", theEvent.get("a[0].theString"));
        assertEquals("E6", theEvent.get("a[1].theString"));
    }

    public void testTimerWithinOverDistinct() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}

        // for 10 seconds, look for every distinct A
        String expression = "select * from pattern [(every-distinct(a.intPrimitive) a=SupportBean) where timer:within(10 sec)]";
        runTimerWithinOverDistinct(engine, expression);

        expression = "select * from pattern [(every-distinct(a.intPrimitive, 2 days 2 minutes) a=SupportBean) where timer:within(10 sec)]";
        runTimerWithinOverDistinct(engine, expression);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }
    
    private void runTimerWithinOverDistinct(EPServiceProvider engine, String expression) {

        sendTimer(0, engine);
        EPStatement statement = engine.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("a.theString"));

        engine.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        assertEquals("E3", listener.assertOneGetNewAndReset().get("a.theString"));

        sendTimer(11000, engine);
        engine.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("E5", 1));
        assertFalse(listener.isInvoked());
    }

    public void testEveryDistinctOverTimerWithin() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}

        String expression = "select * from pattern [every-distinct(a.intPrimitive) (a=SupportBean where timer:within(10 sec))]";
        runEveryDistinctOverTimerWithin(engine, expression);

        expression = "select * from pattern [every-distinct(a.intPrimitive, 1 hour) (a=SupportBean where timer:within(10 sec))]";
        runEveryDistinctOverTimerWithin(engine, expression);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void runEveryDistinctOverTimerWithin(EPServiceProvider engine, String expression) {

        sendTimer(0, engine);
        EPStatement statement = engine.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("a.theString"));

        engine.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        sendTimer(5000, engine);
        engine.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        assertEquals("E3", listener.assertOneGetNewAndReset().get("a.theString"));
        
        sendTimer(10000, engine);
        engine.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("E5", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("E6", 2));
        assertFalse(listener.isInvoked());

        sendTimer(15000, engine);
        engine.getEPRuntime().sendEvent(new SupportBean("E7", 2));
        assertFalse(listener.isInvoked());

        sendTimer(20000, engine);
        engine.getEPRuntime().sendEvent(new SupportBean("E8", 2));
        assertFalse(listener.isInvoked());

        sendTimer(25000, engine);
        engine.getEPRuntime().sendEvent(new SupportBean("E9", 1));
        assertFalse(listener.isInvoked());

        sendTimer(50000, engine);
        engine.getEPRuntime().sendEvent(new SupportBean("E10", 1));
        assertEquals("E10", listener.assertOneGetNewAndReset().get("a.theString"));

        engine.getEPRuntime().sendEvent(new SupportBean("E11", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("E12", 2));
        assertEquals("E12", listener.assertOneGetNewAndReset().get("a.theString"));

        engine.getEPRuntime().sendEvent(new SupportBean("E13", 2));
        assertFalse(listener.isInvoked());
    }

    public void testEveryDistinctOverAnd() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}

        String expression = "select * from pattern [every-distinct(a.intPrimitive, b.intPrimitive) (a=SupportBean(theString like 'A%') and b=SupportBean(theString like 'B%'))]";
        runEveryDistinctOverAnd(engine, expression);

        expression = "select * from pattern [every-distinct(a.intPrimitive, b.intPrimitive, 1 hour) (a=SupportBean(theString like 'A%') and b=SupportBean(theString like 'B%'))]";
        runEveryDistinctOverAnd(engine, expression);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void runEveryDistinctOverAnd(EPServiceProvider engine, String expression) {

        EPStatement statement = engine.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        assertFalse(listener.isInvoked());
        engine.getEPRuntime().sendEvent(new SupportBean("B1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B1"});

        engine.getEPRuntime().sendEvent(new SupportBean("A2", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("B2", 10));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("A3", 2));
        engine.getEPRuntime().sendEvent(new SupportBean("B3", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A3", "B3"});

        engine.getEPRuntime().sendEvent(new SupportBean("A4", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("B4", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A4", "B4"});

        engine.getEPRuntime().sendEvent(new SupportBean("A5", 2));
        engine.getEPRuntime().sendEvent(new SupportBean("B5", 10));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("A6", 2));
        engine.getEPRuntime().sendEvent(new SupportBean("B6", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A6", "B6"});

        engine.getEPRuntime().sendEvent(new SupportBean("A7", 2));
        engine.getEPRuntime().sendEvent(new SupportBean("B7", 20));
        assertFalse(listener.isInvoked());
    }

    public void testEveryDistinctOverOr() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}

        String expression = "select * from pattern [every-distinct(coalesce(a.intPrimitive, 0) + coalesce(b.intPrimitive, 0)) (a=SupportBean(theString like 'A%') or b=SupportBean(theString like 'B%'))]";
        runEveryDistinctOverOr(engine, expression);

        expression = "select * from pattern [every-distinct(coalesce(a.intPrimitive, 0) + coalesce(b.intPrimitive, 0), 1 hour) (a=SupportBean(theString like 'A%') or b=SupportBean(theString like 'B%'))]";
        runEveryDistinctOverOr(engine, expression);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void runEveryDistinctOverOr(EPServiceProvider engine, String expression) {

        EPStatement statement = engine.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", null});

        engine.getEPRuntime().sendEvent(new SupportBean("B1", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{null, "B1"});

        engine.getEPRuntime().sendEvent(new SupportBean("B2", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("A2", 2));
        engine.getEPRuntime().sendEvent(new SupportBean("A3", 2));
        engine.getEPRuntime().sendEvent(new SupportBean("B3", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("B4", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{null, "B4"});

        engine.getEPRuntime().sendEvent(new SupportBean("B5", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{null, "B5"});

        engine.getEPRuntime().sendEvent(new SupportBean("B6", 3));
        engine.getEPRuntime().sendEvent(new SupportBean("A4", 3));
        engine.getEPRuntime().sendEvent(new SupportBean("A5", 4));
        assertFalse(listener.isInvoked());
    }

    public void testEveryDistinctOverNot() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}

        String expression = "select * from pattern [every-distinct(a.intPrimitive) (a=SupportBean(theString like 'A%') and not SupportBean(theString like 'B%'))]";
        runEveryDistinctOverNot(engine, expression);

        expression = "select * from pattern [every-distinct(a.intPrimitive, 1 hour) (a=SupportBean(theString like 'A%') and not SupportBean(theString like 'B%'))]";
        runEveryDistinctOverNot(engine, expression);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void runEveryDistinctOverNot(EPServiceProvider engine, String expression) {

        EPStatement statement = engine.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A1"});

        engine.getEPRuntime().sendEvent(new SupportBean("A2", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("A3", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A3"});

        engine.getEPRuntime().sendEvent(new SupportBean("B1", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("A4", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString".split(","), new Object[]{"A4"});

        engine.getEPRuntime().sendEvent(new SupportBean("A5", 1));
        assertFalse(listener.isInvoked());
    }

    public void testEveryDistinctOverFollowedBy() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}

        String expression = "select * from pattern [every-distinct(a.intPrimitive + b.intPrimitive) (a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%'))]";
        runEveryDistinctOverFollowedBy(engine, expression);

        expression = "select * from pattern [every-distinct(a.intPrimitive + b.intPrimitive, 1 hour) (a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%'))]";
        runEveryDistinctOverFollowedBy(engine, expression);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void runEveryDistinctOverFollowedBy(EPServiceProvider engine, String expression) {
        EPStatement statement = engine.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        assertFalse(listener.isInvoked());
        engine.getEPRuntime().sendEvent(new SupportBean("B1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B1"});

        engine.getEPRuntime().sendEvent(new SupportBean("A2", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("B2", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("A3", 10));
        engine.getEPRuntime().sendEvent(new SupportBean("B3", -8));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("A4", 2));
        engine.getEPRuntime().sendEvent(new SupportBean("B4", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A4", "B4"});

        engine.getEPRuntime().sendEvent(new SupportBean("A5", 3));
        engine.getEPRuntime().sendEvent(new SupportBean("B5", 0));
        assertFalse(listener.isInvoked());
    }

    public void testEveryDistinctWithinFollowedBy() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}

        String expression = "select * from pattern [(every-distinct(a.intPrimitive) a=SupportBean(theString like 'A%')) -> b=SupportBean(intPrimitive=a.intPrimitive)]";
        runEveryDistinctWithinFollowedBy(engine, expression);

        expression = "select * from pattern [(every-distinct(a.intPrimitive, 2 hours 1 minute) a=SupportBean(theString like 'A%')) -> b=SupportBean(intPrimitive=a.intPrimitive)]";
        runEveryDistinctWithinFollowedBy(engine, expression);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void runEveryDistinctWithinFollowedBy(EPServiceProvider engine, String expression) {
        EPStatement statement = engine.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("B1", 0));
        assertFalse(listener.isInvoked());
        engine.getEPRuntime().sendEvent(new SupportBean("B2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B2"});

        engine.getEPRuntime().sendEvent(new SupportBean("A2", 2));
        engine.getEPRuntime().sendEvent(new SupportBean("A3", 3));
        engine.getEPRuntime().sendEvent(new SupportBean("A4", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("B3", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A3", "B3"});

        engine.getEPRuntime().sendEvent(new SupportBean("B4", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("B5", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A2", "B5"});

        engine.getEPRuntime().sendEvent(new SupportBean("A5", 2));
        engine.getEPRuntime().sendEvent(new SupportBean("B6", 2));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("A6", 4));
        engine.getEPRuntime().sendEvent(new SupportBean("B7", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A6", "B7"});
    }

    public void testFollowedByWithDistinct() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}

        String expression = "select * from pattern [every-distinct(a.intPrimitive) a=SupportBean(theString like 'A%') -> every-distinct(b.intPrimitive) b=SupportBean(theString like 'B%')]";
        runFollowedByWithDistinct(engine, expression);

        expression = "select * from pattern [every-distinct(a.intPrimitive, 1 day) a=SupportBean(theString like 'A%') -> every-distinct(b.intPrimitive) b=SupportBean(theString like 'B%')]";
        runFollowedByWithDistinct(engine, expression);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }
    
    private void runFollowedByWithDistinct(EPServiceProvider engine, String expression) {
        EPStatement statement = engine.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("A1", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("B1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B1"});
        engine.getEPRuntime().sendEvent(new SupportBean("B2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B2"});
        engine.getEPRuntime().sendEvent(new SupportBean("B3", 0));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("A2", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("B4", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A1", "B4"});

        engine.getEPRuntime().sendEvent(new SupportBean("A3", 2));
        engine.getEPRuntime().sendEvent(new SupportBean("B5", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.theString,b.theString".split(","), new Object[]{"A3", "B5"});
        engine.getEPRuntime().sendEvent(new SupportBean("B6", 1));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new SupportBean("B7", 3));
        EventBean[] events = listener.getAndResetLastNewData();
        EPAssertionUtil.assertPropsPerRowAnyOrder(events, "a.theString,b.theString".split(","),
                new Object[][] {{"A1", "B7"}, {"A3", "B7"}});
    }

    public void testInvalid() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("A", SupportBean_A.class);
        config.addEventType("B", SupportBean_B.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(engine, this.getClass(), getName());}

        tryInvalid(engine, "a=A->every-distinct(a.intPrimitive) B",
                "Failed to validate pattern every-distinct expression 'a.intPrimitive': Failed to resolve property 'a.intPrimitive' to a stream or nested property in a stream [a=A->every-distinct(a.intPrimitive) B]");

        tryInvalid(engine, "every-distinct(dummy) A",
                "Failed to validate pattern every-distinct expression 'dummy': Property named 'dummy' is not valid in any stream [every-distinct(dummy) A]");

        tryInvalid(engine, "every-distinct(2 sec) A",
                "Every-distinct node requires one or more distinct-value expressions that each return non-constant result values [every-distinct(2 sec) A]");

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testMonthScoped() {
        String[] fields = "a.theString,a.intPrimitive".split(",");
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        SupportUpdateListener listener = new SupportUpdateListener();

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        sendCurrentTime(epService, "2002-02-01T9:00:00.000");
        epService.getEPAdministrator().createEPL("select * from pattern [every-distinct(theString, 1 month) a=SupportBean]").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        sendCurrentTimeWithMinus(epService, "2002-03-01T9:00:00.000", 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        assertFalse(listener.isInvoked());

        sendCurrentTime(epService, "2002-03-01T9:00:00.000");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1", 4});

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void sendCurrentTimeWithMinus(EPServiceProvider epService, String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }

    private void sendCurrentTime(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private static void tryInvalid(EPServiceProvider engine, String statement, String message) throws Exception
    {
        try
        {
            engine.getEPAdministrator().createPattern(statement);
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals(message, ex.getMessage());
        }
    }

    private void sendTimer(long timeInMSec, EPServiceProvider epService)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
