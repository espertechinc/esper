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

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestPerf2StreamSimpleJoinCoercion extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        listener = new SupportUpdateListener();
    }

    public void tearDown()
    {
        listener = null;
        epService.initialize();
    }

    public void testPerformanceCoercionForward()
    {
        String stmt = "select A.longBoxed as value from " +
                SupportBean.class.getName() + "(theString='A').win:length(1000000) as A," +
                SupportBean.class.getName() + "(theString='B').win:length(1000000) as B" +
            " where A.longBoxed=B.intPrimitive";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listener);

        // preload
        for (int i = 0; i < 10000; i++)
        {
            epService.getEPRuntime().sendEvent(makeSupportEvent("A", 0, i));
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++)
        {
            int index = 5000 + i % 1000;
            epService.getEPRuntime().sendEvent(makeSupportEvent("B", index, 0));
            assertEquals((long)index, listener.assertOneGetNewAndReset().get("value"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        statement.destroy();
        assertTrue("Failed perf test, delta=" + delta, delta < 1500);
    }

    public void testPerformanceCoercionBack()
    {
        String stmt = "select A.intPrimitive as value from " +
                SupportBean.class.getName() + "(theString='A').win:length(1000000) as A," +
                SupportBean.class.getName() + "(theString='B').win:length(1000000) as B" +
            " where A.intPrimitive=B.longBoxed";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listener);

        // preload
        for (int i = 0; i < 10000; i++)
        {
            epService.getEPRuntime().sendEvent(makeSupportEvent("A", i, 0));
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++)
        {
            int index = 5000 + i % 1000;
            epService.getEPRuntime().sendEvent(makeSupportEvent("B", 0, index));
            assertEquals(index, listener.assertOneGetNewAndReset().get("value"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        statement.destroy();
        assertTrue("Failed perf test, delta=" + delta, delta < 1500);
    }

    private Object makeSupportEvent(String theString, int intPrimitive, long longBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setLongBoxed(longBoxed);
        return bean;
    }

    private static final Log log = LogFactory.getLog(TestPerf2StreamSimpleJoinCoercion.class);
}
