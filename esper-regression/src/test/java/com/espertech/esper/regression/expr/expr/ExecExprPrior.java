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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.util.Random;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecExprPrior implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPriorTimewindowStats(epService);
        runAssertionPriorStreamAndVariable(epService);
        runAssertionPriorTimeWindow(epService);
        runAssertionPriorExtTimedWindow(epService);
        runAssertionPriorTimeBatchWindow(epService);
        runAssertionPriorUnbound(epService);
        runAssertionPriorNoDataWindowWhere(epService);
        if (!InstrumentationHelper.ENABLED) {
            runAssertionLongRunningSingle(epService);
            runAssertionLongRunningUnbound(epService);
            runAssertionLongRunningMultiple(epService);
        }
        runAssertionPriorLengthWindow(epService);
        runAssertionPriorLengthWindowWhere(epService);
        runAssertionPriorSortWindow(epService);
        runAssertionPriorTimeBatchWindowJoin(epService);
    }

    private void runAssertionPriorTimewindowStats(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        String epl = "SELECT prior(1, average) as value FROM SupportBean()#time(5 minutes)#uni(intPrimitive)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(null, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 4));
        assertEquals(1.0, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        assertEquals(2.5, listener.assertOneGetNewAndReset().get("value"));

        stmt.destroy();
    }

    private void runAssertionPriorStreamAndVariable(EPServiceProvider epService) {
        tryAssertionPriorStreamAndVariable(epService, "1");

        // try variable
        epService.getEPAdministrator().createEPL("create constant variable int NUM_PRIOR = 1");
        tryAssertionPriorStreamAndVariable(epService, "NUM_PRIOR");

        // must be a constant-value expression
        epService.getEPAdministrator().createEPL("create variable int NUM_PRIOR_NONCONST = 1");
        try {
            tryAssertionPriorStreamAndVariable(epService, "NUM_PRIOR_NONCONST");
            fail();
        } catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Failed to validate select-clause expression 'prior(NUM_PRIOR_NONCONST,s0)': Prior function requires a constant-value integer-typed index expression as the first parameter");
        }
    }

    private void tryAssertionPriorStreamAndVariable(EPServiceProvider epService, String priorIndex) {
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);
        String text = "select prior(" + priorIndex + ", s0) as result from S0#length(2) as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean_S0 e1 = new SupportBean_S0(3);
        epService.getEPRuntime().sendEvent(e1);
        assertEquals(null, listener.assertOneGetNewAndReset().get("result"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertEquals(e1, listener.assertOneGetNewAndReset().get("result"));
        assertEquals(SupportBean_S0.class, stmt.getEventType().getPropertyType("result"));

        stmt.destroy();
    }

    private void runAssertionPriorTimeWindow(EPServiceProvider epService) {
        String epl = "select irstream symbol as currSymbol, " +
                " prior(2, symbol) as priorSymbol, " +
                " prior(2, price) as priorPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#time(1 min) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("priorSymbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("priorPrice"));

        sendTimer(epService, 0);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "D1", 1);
        assertNewEvents(listener, "D1", null, null);

        sendTimer(epService, 1000);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "D2", 2);
        assertNewEvents(listener, "D2", null, null);

        sendTimer(epService, 2000);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "D3", 3);
        assertNewEvents(listener, "D3", "D1", 1d);

        sendTimer(epService, 3000);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "D4", 4);
        assertNewEvents(listener, "D4", "D2", 2d);

        sendTimer(epService, 4000);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "D5", 5);
        assertNewEvents(listener, "D5", "D3", 3d);

        sendTimer(epService, 30000);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "D6", 6);
        assertNewEvents(listener, "D6", "D4", 4d);

        sendTimer(epService, 60000);
        assertOldEvents(listener, "D1", null, null);
        sendTimer(epService, 61000);
        assertOldEvents(listener, "D2", null, null);
        sendTimer(epService, 62000);
        assertOldEvents(listener, "D3", "D1", 1d);
        sendTimer(epService, 63000);
        assertOldEvents(listener, "D4", "D2", 2d);
        sendTimer(epService, 64000);
        assertOldEvents(listener, "D5", "D3", 3d);
        sendTimer(epService, 90000);
        assertOldEvents(listener, "D6", "D4", 4d);

        sendMarketEvent(epService, "D7", 7);
        assertNewEvents(listener, "D7", "D5", 5d);
        sendMarketEvent(epService, "D8", 8);
        sendMarketEvent(epService, "D9", 9);
        sendMarketEvent(epService, "D10", 10);
        sendMarketEvent(epService, "D11", 11);
        listener.reset();

        // release batch
        sendTimer(epService, 150000);
        EventBean[] oldData = listener.getLastOldData();
        assertNull(listener.getLastNewData());
        assertEquals(5, oldData.length);
        assertEvent(oldData[0], "D7", "D5", 5d);
        assertEvent(oldData[1], "D8", "D6", 6d);
        assertEvent(oldData[2], "D9", "D7", 7d);
        assertEvent(oldData[3], "D10", "D8", 8d);
        assertEvent(oldData[4], "D11", "D9", 9d);

        stmt.destroy();
    }

    private void runAssertionPriorExtTimedWindow(EPServiceProvider epService) {
        String epl = "select irstream symbol as currSymbol, " +
                " prior(2, symbol) as priorSymbol, " +
                " prior(3, price) as priorPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#ext_timed(volume, 1 min) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("priorSymbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("priorPrice"));

        sendMarketEvent(epService, "D1", 1, 0);
        assertNewEvents(listener, "D1", null, null);

        sendMarketEvent(epService, "D2", 2, 1000);
        assertNewEvents(listener, "D2", null, null);

        sendMarketEvent(epService, "D3", 3, 3000);
        assertNewEvents(listener, "D3", "D1", null);

        sendMarketEvent(epService, "D4", 4, 4000);
        assertNewEvents(listener, "D4", "D2", 1d);

        sendMarketEvent(epService, "D5", 5, 5000);
        assertNewEvents(listener, "D5", "D3", 2d);

        sendMarketEvent(epService, "D6", 6, 30000);
        assertNewEvents(listener, "D6", "D4", 3d);

        sendMarketEvent(epService, "D7", 7, 60000);
        assertEvent(listener.getLastNewData()[0], "D7", "D5", 4d);
        assertEvent(listener.getLastOldData()[0], "D1", null, null);
        listener.reset();

        sendMarketEvent(epService, "D8", 8, 61000);
        assertEvent(listener.getLastNewData()[0], "D8", "D6", 5d);
        assertEvent(listener.getLastOldData()[0], "D2", null, null);
        listener.reset();

        sendMarketEvent(epService, "D9", 9, 63000);
        assertEvent(listener.getLastNewData()[0], "D9", "D7", 6d);
        assertEvent(listener.getLastOldData()[0], "D3", "D1", null);
        listener.reset();

        sendMarketEvent(epService, "D10", 10, 64000);
        assertEvent(listener.getLastNewData()[0], "D10", "D8", 7d);
        assertEvent(listener.getLastOldData()[0], "D4", "D2", 1d);
        listener.reset();

        sendMarketEvent(epService, "D10", 10, 150000);
        EventBean[] oldData = listener.getLastOldData();
        assertEquals(6, oldData.length);
        assertEvent(oldData[0], "D5", "D3", 2d);

        stmt.destroy();
    }

    private void runAssertionPriorTimeBatchWindow(EPServiceProvider epService) {
        String epl = "select irstream symbol as currSymbol, " +
                " prior(3, symbol) as priorSymbol, " +
                " prior(2, price) as priorPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#time_batch(1 min) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("priorSymbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("priorPrice"));

        sendTimer(epService, 0);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "A", 1);
        sendMarketEvent(epService, "B", 2);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 60000);
        assertEquals(2, listener.getLastNewData().length);
        assertEvent(listener.getLastNewData()[0], "A", null, null);
        assertEvent(listener.getLastNewData()[1], "B", null, null);
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 80000);
        sendMarketEvent(epService, "C", 3);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 120000);
        assertEquals(1, listener.getLastNewData().length);
        assertEvent(listener.getLastNewData()[0], "C", null, 1d);
        assertEquals(2, listener.getLastOldData().length);
        assertEvent(listener.getLastOldData()[0], "A", null, null);
        listener.reset();

        sendTimer(epService, 300000);
        sendMarketEvent(epService, "D", 4);
        sendMarketEvent(epService, "E", 5);
        sendMarketEvent(epService, "F", 6);
        sendMarketEvent(epService, "G", 7);
        sendTimer(epService, 360000);
        assertEquals(4, listener.getLastNewData().length);
        assertEvent(listener.getLastNewData()[0], "D", "A", 2d);
        assertEvent(listener.getLastNewData()[1], "E", "B", 3d);
        assertEvent(listener.getLastNewData()[2], "F", "C", 4d);
        assertEvent(listener.getLastNewData()[3], "G", "D", 5d);

        stmt.destroy();
    }

    private void runAssertionPriorUnbound(EPServiceProvider epService) {
        String epl = "select symbol as currSymbol, " +
                " prior(3, symbol) as priorSymbol, " +
                " prior(2, price) as priorPrice " +
                "from " + SupportMarketDataBean.class.getName();

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("priorSymbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("priorPrice"));

        sendMarketEvent(epService, "A", 1);
        assertNewEvents(listener, "A", null, null);

        sendMarketEvent(epService, "B", 2);
        assertNewEvents(listener, "B", null, null);

        sendMarketEvent(epService, "C", 3);
        assertNewEvents(listener, "C", null, 1d);

        sendMarketEvent(epService, "D", 4);
        assertNewEvents(listener, "D", "A", 2d);

        sendMarketEvent(epService, "E", 5);
        assertNewEvents(listener, "E", "B", 3d);

        stmt.destroy();
    }

    private void runAssertionPriorNoDataWindowWhere(EPServiceProvider epService) {
        String text = "select * from " + SupportMarketDataBean.class.getName() +
                " where prior(1, price) = 100";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendMarketEvent(epService, "IBM", 75);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "IBM", 100);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "IBM", 120);
        assertTrue(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionLongRunningSingle(EPServiceProvider epService) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.endTest();
        } // excluded from instrumentation, too much data

        String epl = "select symbol as currSymbol, " +
                " prior(3, symbol) as prior0Symbol " +
                "from " + SupportMarketDataBean.class.getName() + "#sort(3, symbol)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Random random = new Random();
        // 200000 is a better number for a memory test, however for short unit tests this is 2000
        for (int i = 0; i < 2000; i++) {
            if (i % 10000 == 0) {
                //System.out.println(i);
            }

            sendMarketEvent(epService, Integer.toString(random.nextInt()), 4);

            if (i % 1000 == 0) {
                listener.reset();
            }
        }

        stmt.destroy();
    }

    private void runAssertionLongRunningUnbound(EPServiceProvider epService) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.endTest();
        } // excluded from instrumentation, too much data

        String epl = "select symbol as currSymbol, " +
                " prior(3, symbol) as prior0Symbol " +
                "from " + SupportMarketDataBean.class.getName();

        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertFalse(stmt.getStatementContext().isStatelessSelect());

        Random random = new Random();
        // 200000 is a better number for a memory test, however for short unit tests this is 2000
        for (int i = 0; i < 2000; i++) {
            if (i % 10000 == 0) {
                //System.out.println(i);
            }

            sendMarketEvent(epService, Integer.toString(random.nextInt()), 4);

            if (i % 1000 == 0) {
                listener.reset();
            }
        }

        stmt.destroy();
    }

    private void runAssertionLongRunningMultiple(EPServiceProvider epService) {

        String epl = "select symbol as currSymbol, " +
                " prior(3, symbol) as prior0Symbol, " +
                " prior(2, symbol) as prior1Symbol, " +
                " prior(1, symbol) as prior2Symbol, " +
                " prior(0, symbol) as prior3Symbol, " +
                " prior(0, price) as prior0Price, " +
                " prior(1, price) as prior1Price, " +
                " prior(2, price) as prior2Price, " +
                " prior(3, price) as prior3Price " +
                "from " + SupportMarketDataBean.class.getName() + "#sort(3, symbol)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Random random = new Random();
        // 200000 is a better number for a memory test, however for short unit tests this is 2000
        for (int i = 0; i < 2000; i++) {
            if (i % 10000 == 0) {
                //System.out.println(i);
            }

            sendMarketEvent(epService, Integer.toString(random.nextInt()), 4);

            if (i % 1000 == 0) {
                listener.reset();
            }
        }

        stmt.destroy();
    }

    private void runAssertionPriorLengthWindow(EPServiceProvider epService) {
        String epl = "select irstream symbol as currSymbol, " +
                "prior(0, symbol) as prior0Symbol, " +
                "prior(1, symbol) as prior1Symbol, " +
                "prior(2, symbol) as prior2Symbol, " +
                "prior(3, symbol) as prior3Symbol, " +
                "prior(0, price) as prior0Price, " +
                "prior(1, price) as prior1Price, " +
                "prior(2, price) as prior2Price, " +
                "prior(3, price) as prior3Price " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("prior0Symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prior0Price"));

        sendMarketEvent(epService, "A", 1);
        assertNewEvents(listener, "A", "A", 1d, null, null, null, null, null, null);
        sendMarketEvent(epService, "B", 2);
        assertNewEvents(listener, "B", "B", 2d, "A", 1d, null, null, null, null);
        sendMarketEvent(epService, "C", 3);
        assertNewEvents(listener, "C", "C", 3d, "B", 2d, "A", 1d, null, null);

        sendMarketEvent(epService, "D", 4);
        EventBean newEvent = listener.getLastNewData()[0];
        EventBean oldEvent = listener.getLastOldData()[0];
        assertEventProps(listener, newEvent, "D", "D", 4d, "C", 3d, "B", 2d, "A", 1d);
        assertEventProps(listener, oldEvent, "A", "A", 1d, null, null, null, null, null, null);

        sendMarketEvent(epService, "E", 5);
        newEvent = listener.getLastNewData()[0];
        oldEvent = listener.getLastOldData()[0];
        assertEventProps(listener, newEvent, "E", "E", 5d, "D", 4d, "C", 3d, "B", 2d);
        assertEventProps(listener, oldEvent, "B", "B", 2d, "A", 1d, null, null, null, null);

        sendMarketEvent(epService, "F", 6);
        newEvent = listener.getLastNewData()[0];
        oldEvent = listener.getLastOldData()[0];
        assertEventProps(listener, newEvent, "F", "F", 6d, "E", 5d, "D", 4d, "C", 3d);
        assertEventProps(listener, oldEvent, "C", "C", 3d, "B", 2d, "A", 1d, null, null);

        sendMarketEvent(epService, "G", 7);
        newEvent = listener.getLastNewData()[0];
        oldEvent = listener.getLastOldData()[0];
        assertEventProps(listener, newEvent, "G", "G", 7d, "F", 6d, "E", 5d, "D", 4d);
        assertEventProps(listener, oldEvent, "D", "D", 4d, "C", 3d, "B", 2d, "A", 1d);

        sendMarketEvent(epService, "G", 8);
        oldEvent = listener.getLastOldData()[0];
        assertEventProps(listener, oldEvent, "E", "E", 5d, "D", 4d, "C", 3d, "B", 2d);

        stmt.destroy();
    }

    private void runAssertionPriorLengthWindowWhere(EPServiceProvider epService) {
        String epl = "select prior(2, symbol) as currSymbol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(1) " +
                "where prior(2, price) > 100";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendMarketEvent(epService, "A", 1);
        sendMarketEvent(epService, "B", 130);
        sendMarketEvent(epService, "C", 10);
        assertFalse(listener.isInvoked());
        sendMarketEvent(epService, "D", 5);
        assertEquals("B", listener.assertOneGetNewAndReset().get("currSymbol"));

        stmt.destroy();
    }

    private void runAssertionPriorSortWindow(EPServiceProvider epService) {
        String epl = "select irstream symbol as currSymbol, " +
                " prior(0, symbol) as prior0Symbol, " +
                " prior(1, symbol) as prior1Symbol, " +
                " prior(2, symbol) as prior2Symbol, " +
                " prior(3, symbol) as prior3Symbol, " +
                " prior(0, price) as prior0Price, " +
                " prior(1, price) as prior1Price, " +
                " prior(2, price) as prior2Price, " +
                " prior(3, price) as prior3Price " +
                "from " + SupportMarketDataBean.class.getName() + "#sort(3, symbol)";
        tryPriorSortWindow(epService, epl);

        epl = "select irstream symbol as currSymbol, " +
                " prior(3, symbol) as prior3Symbol, " +
                " prior(1, symbol) as prior1Symbol, " +
                " prior(2, symbol) as prior2Symbol, " +
                " prior(0, symbol) as prior0Symbol, " +
                " prior(2, price) as prior2Price, " +
                " prior(1, price) as prior1Price, " +
                " prior(0, price) as prior0Price, " +
                " prior(3, price) as prior3Price " +
                "from " + SupportMarketDataBean.class.getName() + "#sort(3, symbol)";
        tryPriorSortWindow(epService, epl);
    }

    private void runAssertionPriorTimeBatchWindowJoin(EPServiceProvider epService) {
        String epl = "select theString as currSymbol, " +
                "prior(2, symbol) as priorSymbol, " +
                "prior(1, price) as priorPrice " +
                "from " + SupportBean.class.getName() + "#keepall, " +
                SupportMarketDataBean.class.getName() + "#time_batch(1 min)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("priorSymbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("priorPrice"));

        sendTimer(epService, 0);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "A", 1);
        sendMarketEvent(epService, "B", 2);
        sendBeanEvent(epService, "X1");
        assertFalse(listener.isInvoked());

        sendTimer(epService, 60000);
        assertEquals(2, listener.getLastNewData().length);
        assertEvent(listener.getLastNewData()[0], "X1", null, null);
        assertEvent(listener.getLastNewData()[1], "X1", null, 1d);
        assertNull(listener.getLastOldData());
        listener.reset();

        sendMarketEvent(epService, "C1", 11);
        sendMarketEvent(epService, "C2", 12);
        sendMarketEvent(epService, "C3", 13);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 120000);
        assertEquals(3, listener.getLastNewData().length);
        assertEvent(listener.getLastNewData()[0], "X1", "A", 2d);
        assertEvent(listener.getLastNewData()[1], "X1", "B", 11d);
        assertEvent(listener.getLastNewData()[2], "X1", "C1", 12d);

        stmt.destroy();
    }

    private void tryPriorSortWindow(EPServiceProvider epService, String epl) {
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendMarketEvent(epService, "COX", 30);
        assertNewEvents(listener, "COX", "COX", 30d, null, null, null, null, null, null);

        sendMarketEvent(epService, "IBM", 45);
        assertNewEvents(listener, "IBM", "IBM", 45d, "COX", 30d, null, null, null, null);

        sendMarketEvent(epService, "MSFT", 33);
        assertNewEvents(listener, "MSFT", "MSFT", 33d, "IBM", 45d, "COX", 30d, null, null);

        sendMarketEvent(epService, "XXX", 55);
        EventBean newEvent = listener.getLastNewData()[0];
        EventBean oldEvent = listener.getLastOldData()[0];
        assertEventProps(listener, newEvent, "XXX", "XXX", 55d, "MSFT", 33d, "IBM", 45d, "COX", 30d);
        assertEventProps(listener, oldEvent, "XXX", "XXX", 55d, "MSFT", 33d, "IBM", 45d, "COX", 30d);

        sendMarketEvent(epService, "BOO", 20);
        newEvent = listener.getLastNewData()[0];
        oldEvent = listener.getLastOldData()[0];
        assertEventProps(listener, newEvent, "BOO", "BOO", 20d, "XXX", 55d, "MSFT", 33d, "IBM", 45d);
        assertEventProps(listener, oldEvent, "MSFT", "MSFT", 33d, "IBM", 45d, "COX", 30d, null, null);

        sendMarketEvent(epService, "DOR", 1);
        newEvent = listener.getLastNewData()[0];
        oldEvent = listener.getLastOldData()[0];
        assertEventProps(listener, newEvent, "DOR", "DOR", 1d, "BOO", 20d, "XXX", 55d, "MSFT", 33d);
        assertEventProps(listener, oldEvent, "IBM", "IBM", 45d, "COX", 30d, null, null, null, null);

        sendMarketEvent(epService, "AAA", 2);
        newEvent = listener.getLastNewData()[0];
        oldEvent = listener.getLastOldData()[0];
        assertEventProps(listener, newEvent, "AAA", "AAA", 2d, "DOR", 1d, "BOO", 20d, "XXX", 55d);
        assertEventProps(listener, oldEvent, "DOR", "DOR", 1d, "BOO", 20d, "XXX", 55d, "MSFT", 33d);

        sendMarketEvent(epService, "AAB", 2);
        oldEvent = listener.getLastOldData()[0];
        assertEventProps(listener, oldEvent, "COX", "COX", 30d, null, null, null, null, null, null);
        listener.reset();

        statement.stop();
    }

    private void assertNewEvents(SupportUpdateListener listener, String currSymbol,
                                 String priorSymbol,
                                 Double priorPrice) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);

        assertEvent(newData[0], currSymbol, priorSymbol, priorPrice);

        listener.reset();
    }

    private void assertEvent(EventBean eventBean,
                             String currSymbol,
                             String priorSymbol,
                             Double priorPrice) {
        assertEquals(currSymbol, eventBean.get("currSymbol"));
        assertEquals(priorSymbol, eventBean.get("priorSymbol"));
        assertEquals(priorPrice, eventBean.get("priorPrice"));
    }

    private void assertNewEvents(SupportUpdateListener listener, String currSymbol,
                                 String prior0Symbol,
                                 Double prior0Price,
                                 String prior1Symbol,
                                 Double prior1Price,
                                 String prior2Symbol,
                                 Double prior2Price,
                                 String prior3Symbol,
                                 Double prior3Price) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);
        assertEventProps(listener, newData[0], currSymbol, prior0Symbol, prior0Price, prior1Symbol, prior1Price, prior2Symbol, prior2Price, prior3Symbol, prior3Price);

        listener.reset();
    }

    private void assertEventProps(SupportUpdateListener listener, EventBean eventBean,
                                  String currSymbol,
                                  String prior0Symbol,
                                  Double prior0Price,
                                  String prior1Symbol,
                                  Double prior1Price,
                                  String prior2Symbol,
                                  Double prior2Price,
                                  String prior3Symbol,
                                  Double prior3Price) {
        assertEquals(currSymbol, eventBean.get("currSymbol"));
        assertEquals(prior0Symbol, eventBean.get("prior0Symbol"));
        assertEquals(prior0Price, eventBean.get("prior0Price"));
        assertEquals(prior1Symbol, eventBean.get("prior1Symbol"));
        assertEquals(prior1Price, eventBean.get("prior1Price"));
        assertEquals(prior2Symbol, eventBean.get("prior2Symbol"));
        assertEquals(prior2Price, eventBean.get("prior2Price"));
        assertEquals(prior3Symbol, eventBean.get("prior3Symbol"));
        assertEquals(prior3Price, eventBean.get("prior3Price"));

        listener.reset();
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendMarketEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketEvent(EPServiceProvider epService, String symbol, double price, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendBeanEvent(EPServiceProvider epService, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void assertOldEvents(SupportUpdateListener listener, String currSymbol,
                                 String priorSymbol,
                                 Double priorPrice) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(newData);
        assertEquals(1, oldData.length);

        assertEquals(currSymbol, oldData[0].get("currSymbol"));
        assertEquals(priorSymbol, oldData[0].get("priorSymbol"));
        assertEquals(priorPrice, oldData[0].get("priorPrice"));

        listener.reset();
    }
}
