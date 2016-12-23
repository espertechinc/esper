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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.bean.SupportSensorEvent;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This test uses unique and sort views to obtain from a set of market data events the 3 currently most expensive stocks
 * and their symbols.
 * The unique view plays the role of filtering only the most recent events and making prior events for a symbol 'old'
 * data to the sort view, which removes these prior events for a symbol from the sorted window.
 */
public class TestViewUniqueSorted extends TestCase
{
    private static String SYMBOL_CSCO = "CSCO.O";
    private static String SYMBOL_IBM = "IBM.N";
    private static String SYMBOL_MSFT = "MSFT.O";
    private static String SYMBOL_C = "C.N";

    private EPServiceProvider epService;
    private SupportUpdateListener testListener;

    public void setUp()
    {
        testListener = new SupportUpdateListener();
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getViewResources().setAllowMultipleExpiryPolicies(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
    }

    public void testExpressionParameter()
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#unique(Math.abs(intPrimitive))");
        sendEvent("E1", 10);
        sendEvent("E2", -10);
        sendEvent("E3", -5);
        sendEvent("E4", 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), "theString".split(","), new Object[][]{{"E2"}, {"E4"}});
    }

    public void testWindowStats()
    {
        // Get the top 3 volumes for each symbol
        EPStatement top3Prices = epService.getEPAdministrator().createEPL(
                "select * from " + SupportMarketDataBean.class.getName() +
                "#unique(symbol)#sort(3, price desc)");
        top3Prices.addListener(testListener);

        testListener.reset();

        Object beans[] = new Object[10];

        beans[0] = makeEvent(SYMBOL_CSCO, 50);
        epService.getEPRuntime().sendEvent(beans[0]);

        Object[] result = toObjectArray(top3Prices.iterator());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{beans[0]}, result);
        assertTrue(testListener.isInvoked());
        EPAssertionUtil.assertEqualsExactOrder((Object[]) null, testListener.getLastOldData());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{beans[0]}, new Object[]{testListener.getLastNewData()[0].getUnderlying()}
        );
        testListener.reset();

        beans[1] = makeEvent(SYMBOL_CSCO, 20);
        beans[2] = makeEvent(SYMBOL_IBM, 50);
        beans[3] = makeEvent(SYMBOL_MSFT, 40);
        beans[4] = makeEvent(SYMBOL_C, 100);
        beans[5] = makeEvent(SYMBOL_IBM, 10);

        epService.getEPRuntime().sendEvent(beans[1]);
        epService.getEPRuntime().sendEvent(beans[2]);
        epService.getEPRuntime().sendEvent(beans[3]);
        epService.getEPRuntime().sendEvent(beans[4]);
        epService.getEPRuntime().sendEvent(beans[5]);

        result = toObjectArray(top3Prices.iterator());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{beans[4], beans[3], beans[5]}, result);

        beans[6] = makeEvent(SYMBOL_CSCO, 110);
        beans[7] = makeEvent(SYMBOL_C, 30);
        beans[8] = makeEvent(SYMBOL_CSCO, 30);

        epService.getEPRuntime().sendEvent(beans[6]);
        epService.getEPRuntime().sendEvent(beans[7]);
        epService.getEPRuntime().sendEvent(beans[8]);

        result = toObjectArray(top3Prices.iterator());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{beans[3], beans[8], beans[7]}, result);
    }

    public void testSensorPerEvent() throws Exception {
        String stmtString =
              "SELECT irstream * " +
              "FROM\n " +
              SupportSensorEvent.class.getName() + "#groupwin(type)#time(1 hour)#unique(device)#sort(1, measurement desc) as high ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtString);
        stmt.addListener(testListener);

        EPRuntime runtime = epService.getEPRuntime();

        SupportSensorEvent eventOne = new SupportSensorEvent(1, "Temperature", "Device1", 5.0, 96.5);
        runtime.sendEvent(eventOne);
        EPAssertionUtil.assertUnderlyingPerRow(testListener.assertInvokedAndReset(), new Object[] {eventOne}, null);

        SupportSensorEvent eventTwo = new SupportSensorEvent(2, "Temperature", "Device2", 7.0, 98.5);
        runtime.sendEvent(eventTwo);
        EPAssertionUtil.assertUnderlyingPerRow(testListener.assertInvokedAndReset(), new Object[] {eventTwo}, new Object[] {eventOne});

        SupportSensorEvent eventThree = new SupportSensorEvent(3, "Temperature", "Device2", 4.0, 99.5);
        runtime.sendEvent(eventThree);
        EPAssertionUtil.assertUnderlyingPerRow(testListener.assertInvokedAndReset(), new Object[] {eventThree}, new Object[] {eventTwo});

        Iterator<EventBean> it = stmt.iterator();
        SupportSensorEvent theEvent = (SupportSensorEvent) it.next().getUnderlying();
        assertEquals(3,theEvent.getId());
    }

    public void testReuseUnique()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#unique(intBoxed)");
        stmt.addListener(testListener);

        SupportBean beanOne = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(beanOne);
        testListener.reset();

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#unique(intBoxed)");
        SupportUpdateListener testListenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(testListenerTwo);
        stmt.start(); // no effect

        SupportBean beanTwo = new SupportBean("E2", 2);
        epService.getEPRuntime().sendEvent(beanTwo);

        assertSame(beanTwo, testListener.getLastNewData()[0].getUnderlying());
        assertSame(beanOne, testListener.getLastOldData()[0].getUnderlying());
        assertSame(beanTwo, testListenerTwo.getLastNewData()[0].getUnderlying());
        assertNull(testListenerTwo.getLastOldData());
    }

    private Object makeEvent(String symbol, double price)
    {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, price, 0L, "");
        return theEvent;
    }

    private void sendEvent(String theString, int intPrimitive)
    {
        epService.getEPRuntime().sendEvent(new SupportBean(theString, intPrimitive));
    }

    private Object[] toObjectArray(Iterator<EventBean> it)
    {
        List<Object> result = new LinkedList<Object>();
        for (;it.hasNext();)
        {
            EventBean theEvent = it.next();
            result.add(theEvent.getUnderlying());
        }
        return result.toArray();
    }
}
