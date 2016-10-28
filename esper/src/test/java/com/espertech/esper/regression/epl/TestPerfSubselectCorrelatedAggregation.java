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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Random;

public class TestPerfSubselectCorrelatedAggregation extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();        
        config.addEventType("SupportBean", SupportBean.class);
        config.addEventType("S0", SupportBean_S0.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testPerformanceCorrelatedAggregation() {
        String stmtText = "select p00, " +
                "(select sum(intPrimitive) from SupportBean#keepall() where theString = s0.p00) as sump00 " +
                "from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        String[] fields = "p00,sump00".split(",");

        // preload
        int max = 50000;
        for (int i = 0; i < max; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("T" + i, -i));
            epService.getEPRuntime().sendEvent(new SupportBean("T" + i, 10));
        }

        // excercise
        long start = System.currentTimeMillis();
        Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            int index = random.nextInt(max);
            epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "T" + index));
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"T" + index, -index + 10});
        }
        long end = System.currentTimeMillis();
        long delta = end - start;

        //System.out.println("delta=" + delta);
        assertTrue("delta=" + delta, delta < 500);
    }
}
