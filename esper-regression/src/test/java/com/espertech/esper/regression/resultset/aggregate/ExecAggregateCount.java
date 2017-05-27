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
package com.espertech.esper.regression.resultset.aggregate;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import static org.junit.Assert.*;

public class ExecAggregateCount implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionCountPlusStar(epService);
        runAssertionCount(epService);
        runAssertionCountHaving(epService);
        runAssertionSumHaving(epService);
    }

    private void runAssertionCountPlusStar(EPServiceProvider epService) {
        // Test for ESPER-118
        String statementText = "select *, count(*) as cnt from " + SupportMarketDataBean.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "S0", 1L);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(1L, listener.getLastNewData()[0].get("cnt"));
        assertEquals("S0", listener.getLastNewData()[0].get("symbol"));

        sendEvent(epService, "S1", 1L);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(2L, listener.getLastNewData()[0].get("cnt"));
        assertEquals("S1", listener.getLastNewData()[0].get("symbol"));

        sendEvent(epService, "S2", 1L);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(3L, listener.getLastNewData()[0].get("cnt"));
        assertEquals("S2", listener.getLastNewData()[0].get("symbol"));

        stmt.destroy();
    }

    private void runAssertionCount(EPServiceProvider epService) {
        String statementText = "select count(*) as cnt from " + SupportMarketDataBean.class.getName() + "#time(1)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "DELL", 1L);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(1L, listener.getLastNewData()[0].get("cnt"));

        sendEvent(epService, "DELL", 1L);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(2L, listener.getLastNewData()[0].get("cnt"));

        sendEvent(epService, "DELL", 1L);
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(3L, listener.getLastNewData()[0].get("cnt"));

        // test invalid distinct
        SupportMessageAssertUtil.tryInvalid(epService, "select count(distinct *) from " + SupportMarketDataBean.class.getName(),
                "Error starting statement: Failed to validate select-clause expression 'count(distinct *)': Invalid use of the 'distinct' keyword with count and wildcard");

        stmt.destroy();
    }

    private void runAssertionCountHaving(EPServiceProvider epService) {
        String theEvent = SupportBean.class.getName();
        String statementText = "select irstream sum(intPrimitive) as mysum from " + theEvent + " having sum(intPrimitive) = 2";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService);
        assertFalse(listener.getAndClearIsInvoked());
        sendEvent(epService);
        assertEquals(2, listener.assertOneGetNewAndReset().get("mysum"));
        sendEvent(epService);
        assertEquals(2, listener.assertOneGetOldAndReset().get("mysum"));

        stmt.destroy();
    }

    private void runAssertionSumHaving(EPServiceProvider epService) {
        String theEvent = SupportBean.class.getName();
        String statementText = "select irstream count(*) as mysum from " + theEvent + " having count(*) = 2";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService);
        assertFalse(listener.getAndClearIsInvoked());
        sendEvent(epService);
        assertEquals(2L, listener.assertOneGetNewAndReset().get("mysum"));
        sendEvent(epService);
        assertEquals(2L, listener.assertOneGetOldAndReset().get("mysum"));

        stmt.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String symbol, Long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "f1");
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(EPServiceProvider epService) {
        SupportBean bean = new SupportBean("", 1);
        epService.getEPRuntime().sendEvent(bean);
    }
}
