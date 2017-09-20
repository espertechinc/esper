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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecQuerytypeAggregateGroupedHaving implements RegressionExecution {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);

        runAssertionGroupByHaving(epService, false);
        runAssertionGroupByHaving(epService, true);
        runAssertionSumOneView(epService);
        runAssertionSumJoin(epService);
    }

    private void runAssertionGroupByHaving(EPServiceProvider epService, boolean join) {
        String epl = !join ?
                "select * from SupportBean#length_batch(3) group by theString having count(*) > 1" :
                "select theString, intPrimitive from SupportBean_S0#lastevent, SupportBean#length_batch(3) group by theString having count(*) > 1";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 21));

        EventBean[] received = listener.getNewDataListFlattened();
        EPAssertionUtil.assertPropsPerRow(received, "theString,intPrimitive".split(","),
                new Object[][]{{"E2", 20}, {"E2", 21}});
        listener.reset();
        stmt.destroy();
    }

    private void runAssertionSumOneView(EPServiceProvider epService) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String epl = "select irstream symbol, volume, sum(price) as mySum " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol " +
                "having sum(price) >= 50";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionSum(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionSumJoin(EPServiceProvider epService) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String epl = "select irstream symbol, volume, sum(price) as mySum " +
                "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                SupportMarketDataBean.class.getName() + "#length(3) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "  and one.theString = two.symbol " +
                "group by symbol " +
                "having sum(price) >= 50";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));

        tryAssertionSum(epService, listener, stmt);

        stmt.destroy();
    }

    private void tryAssertionSum(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {
        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("volume"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("mySum"));

        String[] fields = "symbol,volume,mySum".split(",");
        sendEvent(epService, SYMBOL_DELL, 10000, 49);
        assertFalse(listener.isInvoked());

        sendEvent(epService, SYMBOL_DELL, 20000, 54);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{SYMBOL_DELL, 20000L, 103d});

        sendEvent(epService, SYMBOL_IBM, 1000, 10);
        assertFalse(listener.isInvoked());

        sendEvent(epService, SYMBOL_IBM, 5000, 20);
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{SYMBOL_DELL, 10000L, 54d});

        sendEvent(epService, SYMBOL_IBM, 6000, 5);
        assertFalse(listener.isInvoked());
    }

    private void sendEvent(EPServiceProvider epService, String symbol, long volume, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }
}
