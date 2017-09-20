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
package com.espertech.esper.regression.resultset.querytype;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecQuerytypeRowPerGroupHaving implements RegressionExecution {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableCode(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionHavingCount(epService);
        runAssertionSumJoin(epService);
        runAssertionSumOneView(epService);
    }

    private void runAssertionHavingCount(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        String text = "select * from SupportBean(intPrimitive = 3)#length(10) as e1 group by theString having count(*) > 2";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 3));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 3));
        assertTrue(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionSumJoin(EPServiceProvider epService) {
        String epl = "select irstream symbol, sum(price) as mySum " +
                "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                " " + SupportMarketDataBean.class.getName() + "#length(3) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE')" +
                "       and one.theString = two.symbol " +
                "group by symbol " +
                "having sum(price) >= 100";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));
        epService.getEPRuntime().sendEvent(new SupportBeanString("AAA"));

        tryAssertion(epService, listener);

        stmt.destroy();
    }

    private void runAssertionSumOneView(EPServiceProvider epService) {
        String epl = "select irstream symbol, sum(price) as mySum " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol " +
                "having sum(price) >= 100";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);

        stmt.destroy();
    }

    private void tryAssertion(EPServiceProvider epService, SupportUpdateListener listener) {
        sendEvent(epService, SYMBOL_DELL, 10);
        assertFalse(listener.isInvoked());

        sendEvent(epService, SYMBOL_DELL, 60);
        assertFalse(listener.isInvoked());

        sendEvent(epService, SYMBOL_DELL, 30);
        assertNewEvent(listener, SYMBOL_DELL, 100);

        sendEvent(epService, SYMBOL_IBM, 30);
        assertOldEvent(listener, SYMBOL_DELL, 100);

        sendEvent(epService, SYMBOL_IBM, 80);
        assertNewEvent(listener, SYMBOL_IBM, 110);
    }

    private void assertNewEvent(SupportUpdateListener listener, String symbol, double newSum) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);

        assertEquals(newSum, newData[0].get("mySum"));
        assertEquals(symbol, newData[0].get("symbol"));

        listener.reset();
        assertFalse(listener.isInvoked());
    }

    private void assertOldEvent(SupportUpdateListener listener, String symbol, double newSum) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(newData);
        assertEquals(1, oldData.length);

        assertEquals(newSum, oldData[0].get("mySum"));
        assertEquals(symbol, oldData[0].get("symbol"));

        listener.reset();
        assertFalse(listener.isInvoked());
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }
}
