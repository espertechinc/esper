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

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPerf3StreamCoercion extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        listener = new SupportUpdateListener();
        System.gc();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testPerfCoercion3waySceneOne()
    {
        String stmtText = "select s1.intBoxed as value from " +
                SupportBean.class.getName() + "(theString='A')#length(1000000) s1," +
                SupportBean.class.getName() + "(theString='B')#length(1000000) s2," +
                SupportBean.class.getName() + "(theString='C')#length(1000000) s3" +
            " where s1.intBoxed=s2.longBoxed and s1.intBoxed=s3.doubleBoxed";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // preload
        for (int i = 0; i < 10000; i++)
        {
            sendEvent("B", 0, i, 0);
            sendEvent("C", 0, 0, i);
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++)
        {
            int index = 5000 + i % 1000;
            sendEvent("A", index, 0, 0);
            assertEquals(index, listener.assertOneGetNewAndReset().get("value"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1500);
        stmt.destroy();
    }

    public void testPerfCoercion3waySceneTwo()
    {
        String stmtText = "select s1.intBoxed as value from " +
                SupportBean.class.getName() + "(theString='A')#length(1000000) s1," +
                SupportBean.class.getName() + "(theString='B')#length(1000000) s2," +
                SupportBean.class.getName() + "(theString='C')#length(1000000) s3" +
            " where s1.intBoxed=s2.longBoxed and s1.intBoxed=s3.doubleBoxed";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // preload
        for (int i = 0; i < 10000; i++)
        {
            sendEvent("A", i, 0, 0);
            sendEvent("B", 0, i, 0);
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++)
        {
            int index = 5000 + i % 1000;
            sendEvent("C", 0, 0, index);
            assertEquals(index, listener.assertOneGetNewAndReset().get("value"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        stmt.destroy();
        assertTrue("Failed perf test, delta=" + delta, delta < 1500);
    }

    public void testPerfCoercion3waySceneThree()
    {
        String stmtText = "select s1.intBoxed as value from " +
                SupportBean.class.getName() + "(theString='A')#length(1000000) s1," +
                SupportBean.class.getName() + "(theString='B')#length(1000000) s2," +
                SupportBean.class.getName() + "(theString='C')#length(1000000) s3" +
            " where s1.intBoxed=s2.longBoxed and s1.intBoxed=s3.doubleBoxed";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // preload
        for (int i = 0; i < 10000; i++)
        {
            sendEvent("A", i, 0, 0);
            sendEvent("C", 0, 0, i);
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++)
        {
            int index = 5000 + i % 1000;
            sendEvent("B", 0, index, 0);
            assertEquals(index, listener.assertOneGetNewAndReset().get("value"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        stmt.destroy();
        assertTrue("Failed perf test, delta=" + delta, delta < 1500);
    }

    private void sendEvent(String theString, int intBoxed, long longBoxed, double doubleBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntBoxed(intBoxed);
        bean.setLongBoxed(longBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(TestPerf3StreamCoercion.class);
}
