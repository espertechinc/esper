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
package com.espertech.esper.regression.epl;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestPerfSubselectIn extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("MyEvent", SupportBean.class);
        config.addEventType("S0", SupportBean_S0.class);
        config.addEventType("S1", SupportBean_S1.class);
        config.addEventType("S2", SupportBean_S2.class);
        config.addEventType("S3", SupportBean_S3.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testPerformanceInKeywordAsPartOfSubquery()
    {
        String eplSingleIndex = "select (select p00 from S0#keepall as s0 where s0.p01 in (s1.p10, s1.p11)) as c0 from S1 as s1";
        EPStatement stmtSingleIdx = epService.getEPAdministrator().createEPL(eplSingleIndex);
        stmtSingleIdx.addListener(listener);

        runAssertionPerformanceInKeywordAsPartOfSubquery();
        stmtSingleIdx.destroy();

        String eplMultiIdx = "select (select p00 from S0#keepall as s0 where s1.p11 in (s0.p00, s0.p01)) as c0 from S1 as s1";
        EPStatement stmtMultiIdx = epService.getEPAdministrator().createEPL(eplMultiIdx);
        stmtMultiIdx.addListener(listener);

        runAssertionPerformanceInKeywordAsPartOfSubquery();
    }

    private void runAssertionPerformanceInKeywordAsPartOfSubquery() {
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(i, "v" + i, "p00_"+i));
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 2000; i++) {
            int index = 5000 + i % 1000;
            epService.getEPRuntime().sendEvent(new SupportBean_S1(index, "x", "p00_" + index));
            assertEquals("v" + index, listener.assertOneGetNewAndReset().get("c0"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 500);
    }

    public void testPerformanceWhereClauseCoercion()
    {
        String stmtText = "select intPrimitive from MyEvent(theString='A') as s0 where intPrimitive in (" +
                            "select longBoxed from MyEvent(theString='B')#length(10000) where s0.intPrimitive = longBoxed)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // preload with 10k events
        for (int i = 0; i < 10000; i++)
        {
            SupportBean bean = new SupportBean();
            bean.setTheString("B");
            bean.setLongBoxed((long)i);
            epService.getEPRuntime().sendEvent(bean);
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++)
        {
            int index = 5000 + i % 1000;
            SupportBean bean = new SupportBean();
            bean.setTheString("A");
            bean.setIntPrimitive(index);
            epService.getEPRuntime().sendEvent(bean);
            assertEquals(index, listener.assertOneGetNewAndReset().get("intPrimitive"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 2000);
    }

    public void testPerformanceWhereClause()
    {
        String stmtText = "select id from S0 as s0 where p00 in (" +
                            "select p10 from S1#length(10000) where s0.p00 = p10)";
        tryPerformanceOneCriteria(stmtText);
    }

    private void tryPerformanceOneCriteria(String stmtText)
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // preload with 10k events
        for (int i = 0; i < 10000; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean_S1(i, Integer.toString(i)));
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++)
        {
            int index = 5000 + i % 1000;
            epService.getEPRuntime().sendEvent(new SupportBean_S0(index, Integer.toString(index)));
            assertEquals(index, listener.assertOneGetNewAndReset().get("id"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1000);
    }
}
