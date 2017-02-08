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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.*;
import junit.framework.TestCase;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPerf3StreamOuterJoinCoercion extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testPerfCoercion3waySceneOne()
    {
        String stmtText = "select s1.intBoxed as v1, s2.longBoxed as v2, s3.doubleBoxed as v3 from " +
                SupportBean.class.getName() + "(theString='A')#length(1000000) s1 " +
                " left outer join " +
                SupportBean.class.getName() + "(theString='B')#length(1000000) s2 on s1.intBoxed=s2.longBoxed " +
                " left outer join " +
                SupportBean.class.getName() + "(theString='C')#length(1000000) s3 on s1.intBoxed=s3.doubleBoxed";

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
            EventBean theEvent = listener.assertOneGetNewAndReset();
            assertEquals(index, theEvent.get("v1"));
            assertEquals((long)index, theEvent.get("v2"));
            assertEquals((double) index, theEvent.get("v3"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1500);
    }

    public void testPerfCoercion3waySceneTwo()
    {
        String stmtText = "select s1.intBoxed as v1, s2.longBoxed as v2, s3.doubleBoxed as v3 from " +
                SupportBean.class.getName() + "(theString='A')#length(1000000) s1 " +
                " left outer join " +
                SupportBean.class.getName() + "(theString='B')#length(1000000) s2 on s1.intBoxed=s2.longBoxed " +
                " left outer join " +
                SupportBean.class.getName() + "(theString='C')#length(1000000) s3 on s1.intBoxed=s3.doubleBoxed";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // preload
        for (int i = 0; i < 10000; i++)
        {
            sendEvent("B", 0, i, 0);
            sendEvent("A", i, 0, 0);
        }

        listener.reset();
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++)
        {
            int index = 5000 + i % 1000;
            sendEvent("C", 0, 0, index);
            EventBean theEvent = listener.assertOneGetNewAndReset();
            assertEquals(index, theEvent.get("v1"));
            assertEquals((long)index, theEvent.get("v2"));
            assertEquals((double) index, theEvent.get("v3"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1500);
    }

    public void testPerfCoercion3waySceneThree() throws Exception
    {
        String stmtText = "select s1.intBoxed as v1, s2.longBoxed as v2, s3.doubleBoxed as v3 from " +
                SupportBean.class.getName() + "(theString='A')#length(1000000) s1 " +
                " left outer join " +
                SupportBean.class.getName() + "(theString='B')#length(1000000) s2 on s1.intBoxed=s2.longBoxed " +
                " left outer join " +
                SupportBean.class.getName() + "(theString='C')#length(1000000) s3 on s1.intBoxed=s3.doubleBoxed";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // preload
        for (int i = 0; i < 10000; i++)
        {
            sendEvent("A", i, 0, 0);
            sendEvent("C", 0, 0, i);
        }

        listener.reset();
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++)
        {
            int index = 5000 + i % 1000;
            sendEvent("B", 0, index, 0);
            EventBean theEvent = listener.assertOneGetNewAndReset();
            assertEquals(index, theEvent.get("v1"));
            assertEquals((long)index, theEvent.get("v2"));
            assertEquals((double) index, theEvent.get("v3"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1500);
    }

    public void testPerfCoercion3wayRange()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_ST0", SupportBean_ST0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_ST1", SupportBean_ST1.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanRange", SupportBeanRange.class);

        String stmtText = "select * from " +
                "SupportBeanRange#keepall sbr " +
                " left outer join " +
                "SupportBean_ST0#keepall s0 on s0.key0=sbr.key" +
                " left outer join " +
                "SupportBean_ST1#keepall s1 on s1.key1=s0.key0" +
                " where s0.p00 between sbr.rangeStartLong and sbr.rangeEndLong";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // preload
        log.info("Preload");
        for (int i = 0; i < 10; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1_" + i, "K", i));
        }
        for (int i = 0; i < 10000; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0_" + i, "K", i));
        }
        log.info("Preload done");

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++)
        {
            long index = 5000 + i;
            epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("R", "K", index, index + 2));
            assertEquals(30, listener.getAndResetLastNewData().length);
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0X", "K", 5000));
        assertEquals(10, listener.getAndResetLastNewData().length);

        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1X", "K", 5004));
        assertEquals(301, listener.getAndResetLastNewData().length);

        assertTrue("Failed perf test, delta=" + delta, delta < 500);
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

    private static final Logger log = LoggerFactory.getLogger(TestPerf3StreamOuterJoinCoercion.class);
}
