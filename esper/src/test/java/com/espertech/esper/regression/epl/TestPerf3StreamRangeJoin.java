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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.support.bean.SupportBeanRange;
import com.espertech.esper.support.bean.SupportBean_ST0;
import com.espertech.esper.support.bean.SupportBean_ST1;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestPerf3StreamRangeJoin extends TestCase
{
    private static final Log log = LogFactory.getLog(TestPerf3StreamRangeJoin.class);
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_ST0", SupportBean_ST0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_ST1", SupportBean_ST1.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanRange", SupportBeanRange.class);
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    /**
     * This join algorithm profits from merge join cartesian indicated via @hint.
     */
    public void testPerf3StreamKeyAndRange() {
        epService.getEPAdministrator().createEPL("create window ST0.win:keepall() as SupportBean_ST0");
        epService.getEPAdministrator().createEPL("@Name('I1') insert into ST0 select * from SupportBean_ST0");
        epService.getEPAdministrator().createEPL("create window ST1.win:keepall() as SupportBean_ST1");
        epService.getEPAdministrator().createEPL("@Name('I2') insert into ST1 select * from SupportBean_ST1");

        // Preload
        log.info("Preloading events");
        for (int i = 0; i < 10000; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "G", i));
            epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1", "G", i));
        }
        log.info("Done preloading");

        String epl = "@Hint('PREFER_MERGE_JOIN') select * from SupportBeanRange.std:lastevent() a " +
                "inner join ST0 st0 on st0.key0 = a.key " +
                "inner join ST1 st1 on st1.key1 = a.key " +
                "where " +
                "st0.p00 between rangeStart and rangeEnd and st1.p10 between rangeStart and rangeEnd";
        runAssertion(epl);

        epl = "@Hint('PREFER_MERGE_JOIN') select * from SupportBeanRange.std:lastevent() a, ST0 st0, ST1 st1 " +
                "where st0.key0 = a.key and st1.key1 = a.key and " +
                "st0.p00 between rangeStart and rangeEnd and st1.p10 between rangeStart and rangeEnd";
        runAssertion(epl);
    }

    /**
     * This join algorithm uses merge join cartesian (not nested iteration).
     */
    public void testPerf3StreamRangeOnly() {
        epService.getEPAdministrator().createEPL("create window ST0.win:keepall() as SupportBean_ST0");
        epService.getEPAdministrator().createEPL("@Name('I1') insert into ST0 select * from SupportBean_ST0");
        epService.getEPAdministrator().createEPL("create window ST1.win:keepall() as SupportBean_ST1");
        epService.getEPAdministrator().createEPL("@Name('I2') insert into ST1 select * from SupportBean_ST1");

        // Preload
        log.info("Preloading events");
        for (int i = 0; i < 10000; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "ST0", i));
            epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1", "ST1", i));
        }
        log.info("Done preloading");

        // start query
        //String epl = "select * from SupportBeanRange.std:lastevent() a, ST0 st0, ST1 st1 " +
        //        "where st0.key0 = a.key and st1.key1 = a.key";
        String epl = "select * from SupportBeanRange.std:lastevent() a, ST0 st0, ST1 st1 " +
                "where st0.p00 between rangeStart and rangeEnd and st1.p10 between rangeStart and rangeEnd";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        // Repeat
        log.info("Querying");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBeanRange("R", "R", 100, 101));
            assertEquals(4, listener.getAndResetLastNewData().length);
        }
        log.info("Done Querying");
        long endTime = System.currentTimeMillis();
        log.info("delta=" + (endTime - startTime));

        assertTrue((endTime - startTime) < 500);
        stmt.destroy();
    }

    /**
     * This join algorithm profits from nested iteration execution.
     */
    public void testPerf3StreamUnidirectionalKeyAndRange() {
        epService.getEPAdministrator().createEPL("create window SBR.win:keepall() as SupportBeanRange");
        epService.getEPAdministrator().createEPL("@Name('I1') insert into SBR select * from SupportBeanRange");
        epService.getEPAdministrator().createEPL("create window ST1.win:keepall() as SupportBean_ST1");
        epService.getEPAdministrator().createEPL("@Name('I2') insert into ST1 select * from SupportBean_ST1");

        // Preload
        log.info("Preloading events");
        epService.getEPRuntime().sendEvent(new SupportBeanRange("ST1", "G", 4000, 4004));
        for (int i = 0; i < 10000; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1", "G", i));
        }
        log.info("Done preloading");

        String epl = "select * from SupportBean_ST0 st0 unidirectional, SBR a, ST1 st1 " +
                "where st0.key0 = a.key and st1.key1 = a.key and " +
                "st1.p10 between rangeStart and rangeEnd";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        // Repeat
        log.info("Querying");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 500; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "G", -1));
            assertEquals(5, listener.getAndResetLastNewData().length);
        }
        log.info("Done Querying");
        long delta = System.currentTimeMillis() - startTime;
        log.info("delta=" + delta);

        // This works best with a nested iteration join (and not a cardinal join)
        assertTrue("delta=" + delta, delta < 500);
        stmt.destroy();
    }

    private void runAssertion(String epl) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        // Repeat
        log.info("Querying");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBeanRange("R", "G", 100, 101));
            assertEquals(4, listener.getAndResetLastNewData().length);
        }
        log.info("Done Querying");
        long endTime = System.currentTimeMillis();
        log.info("delta=" + (endTime - startTime));

        assertTrue((endTime - startTime) < 500);
        stmt.destroy();
    }
}
