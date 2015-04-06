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
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanString;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.bean.SupportPriceEvent;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestAggregateRowForAll extends TestCase
{
    private final static String JOIN_KEY = "KEY";

    private EPServiceProvider epService;
    private SupportUpdateListener listener;
    private EPStatement selectTestView;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testSumOneView()
    {
        String viewExpr = "select irstream sum(longBoxed) as mySum " +
                          "from " + SupportBean.class.getName() + ".win:time(10 sec)";
        selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        runAssert();
    }

    public void testSumJoin()
    {
        String viewExpr = "select irstream sum(longBoxed) as mySum " +
                          "from " + SupportBeanString.class.getName() + ".win:time(10) as one, " +
                                    SupportBean.class.getName() + ".win:time(10 sec) as two " +
                          "where one.theString = two.theString";

        selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(JOIN_KEY));

        runAssert();
    }

    private void runAssert()
    {
        // assert select result type
        assertEquals(Long.class, selectTestView.getEventType().getPropertyType("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), new String[]{"mySum"}, new Object[][]{{null}});

        sendTimerEvent(0);
        sendEvent(10);
        assertEquals(10L, listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), new String[]{"mySum"}, new Object[][]{{10L}});

        sendTimerEvent(5000);
        sendEvent(15);
        assertEquals(25L, listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), new String[]{"mySum"}, new Object[][]{{25L}});

        sendTimerEvent(8000);
        sendEvent(-5);
        assertEquals(20L, listener.getAndResetLastNewData()[0].get("mySum"));
        assertNull(listener.getLastOldData());
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), new String[]{"mySum"}, new Object[][]{{20L}});

        sendTimerEvent(10000);
        assertEquals(20L, listener.getLastOldData()[0].get("mySum"));
        assertEquals(10L, listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), new String[]{"mySum"}, new Object[][]{{10L}});

        sendTimerEvent(15000);
        assertEquals(10L, listener.getLastOldData()[0].get("mySum"));
        assertEquals(-5L, listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), new String[]{"mySum"}, new Object[][]{{-5L}});

        sendTimerEvent(18000);
        assertEquals(-5L, listener.getLastOldData()[0].get("mySum"));
        assertNull(listener.getAndResetLastNewData()[0].get("mySum"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), new String[]{"mySum"}, new Object[][]{{null}});
    }

    public void testAvgPerSym() throws Throwable
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select irstream avg(price) as avgp, sym from " + SupportPriceEvent.class.getName() + ".std:groupwin(sym).win:length(2)"
        );
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportPriceEvent(1, "A"));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("A", theEvent.get("sym"));
        assertEquals(1.0, theEvent.get("avgp"));

        epService.getEPRuntime().sendEvent(new SupportPriceEvent(2, "B"));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("B", theEvent.get("sym"));
        assertEquals(1.5, theEvent.get("avgp"));

        epService.getEPRuntime().sendEvent(new SupportPriceEvent(9, "A"));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("A", theEvent.get("sym"));
        assertEquals((1 + 2 + 9) / 3.0, theEvent.get("avgp"));

        epService.getEPRuntime().sendEvent(new SupportPriceEvent(18, "B"));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("B", theEvent.get("sym"));
        assertEquals((1 + 2 + 9 + 18) / 4.0, theEvent.get("avgp"));

        epService.getEPRuntime().sendEvent(new SupportPriceEvent(5, "A"));
        theEvent = listener.getLastNewData()[0];
        assertEquals("A", theEvent.get("sym"));
        assertEquals((2 + 9 + 18 + 5) / 4.0, theEvent.get("avgp"));
        theEvent = listener.getLastOldData()[0];
        assertEquals("A", theEvent.get("sym"));
        assertEquals((5 + 2 + 9 + 18) / 4.0, theEvent.get("avgp"));
    }

    public void testSelectStarStdGroupBy() {
        String stmtText = "select istream * from "+ SupportMarketDataBean.class.getName()
                +".std:groupwin(symbol).win:length(2)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(listener);

        sendEvent("A", 1);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1.0, listener.getLastNewData()[0].get("price"));
        assertTrue(listener.getLastNewData()[0].getUnderlying() instanceof SupportMarketDataBean);
    }

    public void testSelectExprStdGroupBy() {
        String stmtText = "select istream price from "+ SupportMarketDataBean.class.getName()
                +".std:groupwin(symbol).win:length(2)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(listener);

        sendEvent("A", 1);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1.0, listener.getLastNewData()[0].get("price"));
    }

    public void testSelectAvgExprStdGroupBy() {
        String stmtText = "select istream avg(price) as aprice from "+ SupportMarketDataBean.class.getName()
                +".std:groupwin(symbol).win:length(2)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(listener);

        sendEvent("A", 1);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1.0, listener.getLastNewData()[0].get("aprice"));
        sendEvent("B", 3);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(2.0, listener.getLastNewData()[0].get("aprice"));
    }

    public void testSelectAvgStdGroupByUni() {
        String stmtText = "select istream average as aprice from "+ SupportMarketDataBean.class.getName()
                +".std:groupwin(symbol).win:length(2).stat:uni(price)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(listener);

        sendEvent("A", 1);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(1.0, listener.getLastNewData()[0].get("aprice"));
        sendEvent("B", 3);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(3.0, listener.getLastNewData()[0].get("aprice"));
        sendEvent("A", 3);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(2.0, listener.getLastNewData()[0].get("aprice"));
        sendEvent("A", 10);
        sendEvent("A", 20);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(15.0, listener.getLastNewData()[0].get("aprice"));
    }

    public void testSelectAvgExprGroupBy() {
        String stmtText = "select istream avg(price) as aprice, symbol from "+ SupportMarketDataBean.class.getName()
                +".win:length(2) group by symbol";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(listener);

        sendEvent("A", 1);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1.0, listener.getLastNewData()[0].get("aprice"));
        assertEquals("A", listener.getLastNewData()[0].get("symbol"));
        sendEvent("B", 3);
        //there is no A->1 as we already got it out
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(3.0, listener.getLastNewData()[0].get("aprice"));
        assertEquals("B", listener.getLastNewData()[0].get("symbol"));
        sendEvent("B", 5);
        // there is NOW a A->null entry
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(2, listener.getLastNewData().length);
        assertEquals(null, listener.getLastNewData()[0].get("aprice"));
        assertEquals(4.0, listener.getLastNewData()[1].get("aprice"));
        assertEquals("B", listener.getLastNewData()[1].get("symbol"));
        sendEvent("A", 10);
        sendEvent("A", 20);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(2, listener.getLastNewData().length);
        assertEquals(15.0, listener.getLastNewData()[0].get("aprice"));//A
        assertEquals(null, listener.getLastNewData()[1].get("aprice"));//B
    }

    private Object sendEvent(String symbol, double price) {
        Object theEvent = new SupportMarketDataBean(symbol, price, null, null);
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }

    private void sendEvent(long longBoxed, int intBoxed, short shortBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(JOIN_KEY);
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(long longBoxed)
    {
        sendEvent(longBoxed, 0, (short)0);
    }

    private void sendEventInt(int intBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEventFloat(float floatBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setFloatBoxed(floatBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendTimerEvent(long msec)
    {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(msec));
    }

    private static final Log log = LogFactory.getLog(TestAggregateRowForAll.class);
}
