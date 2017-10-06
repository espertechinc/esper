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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanRange;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.bean.SupportBean_ST1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecJoin3StreamRangePerformance implements RegressionExecution {
    private final static Logger log = LoggerFactory.getLogger(ExecJoin3StreamRangePerformance.class);

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_ST0", SupportBean_ST0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_ST1", SupportBean_ST1.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanRange", SupportBeanRange.class);

        runAssertionPerf3StreamKeyAndRange(epService);
        runAssertionPerf3StreamRangeOnly(epService);
        runAssertionPerf3StreamUnidirectionalKeyAndRange(epService);
    }

    /**
     * This join algorithm profits from merge join cartesian indicated via @hint.
     *
     * @param epService
     */
    private void runAssertionPerf3StreamKeyAndRange(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window ST0#keepall as SupportBean_ST0");
        epService.getEPAdministrator().createEPL("@Name('I1') insert into ST0 select * from SupportBean_ST0");
        epService.getEPAdministrator().createEPL("create window ST1#keepall as SupportBean_ST1");
        epService.getEPAdministrator().createEPL("@Name('I2') insert into ST1 select * from SupportBean_ST1");

        // Preload
        log.info("Preloading events");
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "G", i));
            epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1", "G", i));
        }
        log.info("Done preloading");

        String epl = "@Hint('PREFER_MERGE_JOIN') select * from SupportBeanRange#lastevent a " +
                "inner join ST0 st0 on st0.key0 = a.key " +
                "inner join ST1 st1 on st1.key1 = a.key " +
                "where " +
                "st0.p00 between rangeStart and rangeEnd and st1.p10 between rangeStart and rangeEnd";
        tryAssertion(epService, epl);

        epl = "@Hint('PREFER_MERGE_JOIN') select * from SupportBeanRange#lastevent a, ST0 st0, ST1 st1 " +
                "where st0.key0 = a.key and st1.key1 = a.key and " +
                "st0.p00 between rangeStart and rangeEnd and st1.p10 between rangeStart and rangeEnd";
        tryAssertion(epService, epl);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("ST0", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("ST1", true);
    }

    /**
     * This join algorithm uses merge join cartesian (not nested iteration).
     *
     * @param epService
     */
    private void runAssertionPerf3StreamRangeOnly(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window ST0#keepall as SupportBean_ST0");
        epService.getEPAdministrator().createEPL("@Name('I1') insert into ST0 select * from SupportBean_ST0");
        epService.getEPAdministrator().createEPL("create window ST1#keepall as SupportBean_ST1");
        epService.getEPAdministrator().createEPL("@Name('I2') insert into ST1 select * from SupportBean_ST1");

        // Preload
        log.info("Preloading events");
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "ST0", i));
            epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1", "ST1", i));
        }
        log.info("Done preloading");

        // start query
        //String epl = "select * from SupportBeanRange#lastevent a, ST0 st0, ST1 st1 " +
        //        "where st0.key0 = a.key and st1.key1 = a.key";
        String epl = "select * from SupportBeanRange#lastevent a, ST0 st0, ST1 st1 " +
                "where st0.p00 between rangeStart and rangeEnd and st1.p10 between rangeStart and rangeEnd";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // Repeat
        log.info("Querying");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBeanRange("R", "R", 100, 101));
            assertEquals(4, listener.getAndResetLastNewData().length);
        }
        log.info("Done Querying");
        long endTime = System.currentTimeMillis();
        log.info("delta=" + (endTime - startTime));

        assertTrue((endTime - startTime) < 500);
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("ST0", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("ST1", true);
    }

    /**
     * This join algorithm profits from nested iteration execution.
     *
     * @param epService
     */
    private void runAssertionPerf3StreamUnidirectionalKeyAndRange(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window SBR#keepall as SupportBeanRange");
        epService.getEPAdministrator().createEPL("@Name('I1') insert into SBR select * from SupportBeanRange");
        epService.getEPAdministrator().createEPL("create window ST1#keepall as SupportBean_ST1");
        epService.getEPAdministrator().createEPL("@Name('I2') insert into ST1 select * from SupportBean_ST1");

        // Preload
        log.info("Preloading events");
        epService.getEPRuntime().sendEvent(new SupportBeanRange("ST1", "G", 4000, 4004));
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1", "G", i));
        }
        log.info("Done preloading");

        String epl = "select * from SupportBean_ST0 st0 unidirectional, SBR a, ST1 st1 " +
                "where st0.key0 = a.key and st1.key1 = a.key and " +
                "st1.p10 between rangeStart and rangeEnd";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // Repeat
        log.info("Querying");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "G", -1));
            assertEquals(5, listener.getAndResetLastNewData().length);
        }
        log.info("Done Querying");
        long delta = System.currentTimeMillis() - startTime;
        log.info("delta=" + delta);

        // This works best with a nested iteration join (and not a cardinal join)
        assertTrue("delta=" + delta, delta < 500);
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("ST0", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("ST1", true);
    }

    private void tryAssertion(EPServiceProvider epService, String epl) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // Repeat
        log.info("Querying");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
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
