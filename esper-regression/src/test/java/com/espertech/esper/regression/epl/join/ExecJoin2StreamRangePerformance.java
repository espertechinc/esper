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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanRange;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class ExecJoin2StreamRangePerformance implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanRange", SupportBeanRange.class);

        runAssertionPerfKeyAndRangeOuterJoin(epService);
        runAssertionPerfRelationalOp(epService);
        runAssertionPerfKeyAndRange(epService);
        runAssertionPerfKeyAndRangeInverted(epService);
        runAssertionPerfUnidirectionalRelOp(epService);
    }

    private void runAssertionPerfKeyAndRangeOuterJoin(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanRange", SupportBeanRange.class);

        epService.getEPAdministrator().createEPL("create window SBR#keepall as SupportBeanRange");
        epService.getEPAdministrator().createEPL("@Name('I1') insert into SBR select * from SupportBeanRange");
        epService.getEPAdministrator().createEPL("create window SB#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("@Name('I2') insert into SB select * from SupportBean");

        // Preload
        log.info("Preloading events");
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("G", i));
            epService.getEPRuntime().sendEvent(new SupportBeanRange("R", "G", i - 1, i + 2));
        }
        log.info("Done preloading");

        // create
        String epl = "select * " +
                "from SB sb " +
                "full outer join " +
                "SBR sbr " +
                "on theString = key " +
                "where intPrimitive between rangeStart and rangeEnd";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // Repeat
        log.info("Querying");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("G", 9990));
            assertEquals(4, listener.getAndResetLastNewData().length);

            epService.getEPRuntime().sendEvent(new SupportBeanRange("R", "G", 4, 10));
            assertEquals(7, listener.getAndResetLastNewData().length);
        }
        log.info("Done Querying");
        long endTime = System.currentTimeMillis();
        log.info("delta=" + (endTime - startTime));

        assertTrue((endTime - startTime) < 500);
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("SBR", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("SB", true);
    }

    private void runAssertionPerfRelationalOp(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window SBR#keepall as SupportBeanRange");
        epService.getEPAdministrator().createEPL("@Name('I1') insert into SBR select * from SupportBeanRange");
        epService.getEPAdministrator().createEPL("create window SB#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("@Name('I2') insert into SB select * from SupportBean");

        // Preload
        log.info("Preloading events");
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
            epService.getEPRuntime().sendEvent(new SupportBeanRange("E", i, -1));
        }
        log.info("Done preloading");

        // start query
        String epl = "select * from SBR a, SB b where a.rangeStart < b.intPrimitive";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // Repeat
        log.info("Querying");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("B", 10));
            assertEquals(10, listener.getAndResetLastNewData().length);

            epService.getEPRuntime().sendEvent(new SupportBeanRange("R", 9990, -1));
            assertEquals(9, listener.getAndResetLastNewData().length);
        }
        log.info("Done Querying");
        long endTime = System.currentTimeMillis();
        log.info("delta=" + (endTime - startTime));

        assertTrue((endTime - startTime) < 500);
        stmt.destroy();
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("SBR", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("SB", true);
    }

    private void runAssertionPerfKeyAndRange(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window SBR#keepall as SupportBeanRange");
        epService.getEPAdministrator().createEPL("@Name('I1') insert into SBR select * from SupportBeanRange");
        epService.getEPAdministrator().createEPL("create window SB#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("@Name('I2') insert into SB select * from SupportBean");

        // Preload
        log.info("Preloading events");
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                epService.getEPRuntime().sendEvent(new SupportBean(Integer.toString(i), j));
                epService.getEPRuntime().sendEvent(new SupportBeanRange("R", Integer.toString(i), j - 1, j + 1));
            }
        }
        log.info("Done preloading");

        // start query
        String epl = "select * from SBR sbr, SB sb where sbr.key = sb.theString and sb.intPrimitive between sbr.rangeStart and sbr.rangeEnd";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // repeat
        log.info("Querying");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("55", 10));
            assertEquals(3, listener.getAndResetLastNewData().length);

            epService.getEPRuntime().sendEvent(new SupportBeanRange("R", "56", 12, 20));
            assertEquals(9, listener.getAndResetLastNewData().length);
        }
        log.info("Done Querying");
        long endTime = System.currentTimeMillis();
        log.info("delta=" + (endTime - startTime));

        // test no event found
        epService.getEPRuntime().sendEvent(new SupportBeanRange("R", "56", 2000, 3000));
        epService.getEPRuntime().sendEvent(new SupportBeanRange("R", "X", 2000, 3000));
        assertFalse(listener.isInvoked());

        assertTrue("delta=" + (endTime - startTime), (endTime - startTime) < 1500);
        stmt.destroy();

        // delete all events
        epService.getEPAdministrator().createEPL("on SupportBean delete from SBR");
        epService.getEPAdministrator().createEPL("on SupportBean delete from SB");
        epService.getEPRuntime().sendEvent(new SupportBean("D", -1));

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("SBR", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("SB", true);
    }

    private void runAssertionPerfKeyAndRangeInverted(EPServiceProvider epService) {

        epService.getEPAdministrator().createEPL("create window SB#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("@Name('I2') insert into SB select * from SupportBean");

        // Preload
        log.info("Preloading events");
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E", i));
        }
        log.info("Done preloading");

        // start query
        String epl = "select * from SupportBeanRange#lastevent sbr, SB sb where sbr.key = sb.theString and sb.intPrimitive not in [sbr.rangeStart:sbr.rangeEnd]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // repeat
        log.info("Querying");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBeanRange("R", "E", 5, 9995));
            assertEquals(9, listener.getAndResetLastNewData().length);
        }
        log.info("Done Querying");
        long endTime = System.currentTimeMillis();
        log.info("delta=" + (endTime - startTime));

        assertTrue((endTime - startTime) < 500);
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("SB", true);
    }

    private void runAssertionPerfUnidirectionalRelOp(EPServiceProvider epService) {

        epService.getEPAdministrator().createEPL("create window SB#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("@Name('I') insert into SB select * from SupportBean");

        // Preload
        log.info("Preloading events");
        for (int i = 0; i < 100000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
        }
        log.info("Done preloading");

        // Test range
        String rangeEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive between r.rangeStart and r.rangeEnd";
        String rangeEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange r unidirectional " +
                "where a.intPrimitive between r.rangeStart and r.rangeEnd";
        String rangeEplThree = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange#lastevent r, SB a " +
                "where a.intPrimitive between r.rangeStart and r.rangeEnd";
        String rangeEplFour = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange#lastevent r " +
                "where a.intPrimitive between r.rangeStart and r.rangeEnd";
        String rangeEplFive = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a\n" +
                "where a.intPrimitive >= r.rangeStart and a.intPrimitive <= r.rangeEnd";
        String rangeEplSix = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive <= r.rangeEnd and a.intPrimitive >= r.rangeStart";
        AssertionCallback rangeCallback = new AssertionCallback() {
            public Object getEvent(int iteration) {
                return new SupportBeanRange("E", iteration + 50000, iteration + 50100);
            }

            public Object[] getExpectedValue(int iteration) {
                return new Object[]{50000 + iteration, 50100 + iteration};
            }
        };
        tryAssertion(epService, rangeEplOne, 100, rangeCallback);
        tryAssertion(epService, rangeEplTwo, 100, rangeCallback);
        tryAssertion(epService, rangeEplThree, 100, rangeCallback);
        tryAssertion(epService, rangeEplFour, 100, rangeCallback);
        tryAssertion(epService, rangeEplFive, 100, rangeCallback);
        tryAssertion(epService, rangeEplSix, 100, rangeCallback);

        // Test Greater-Equals
        String geEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive >= r.rangeStart and a.intPrimitive <= 99200";
        String geEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange r unidirectional " +
                "where a.intPrimitive >= r.rangeStart and a.intPrimitive <= 99200";
        AssertionCallback geCallback = new AssertionCallback() {
            public Object getEvent(int iteration) {
                return new SupportBeanRange("E", iteration + 99000, null);
            }

            public Object[] getExpectedValue(int iteration) {
                return new Object[]{99000 + iteration, 99200};
            }
        };
        tryAssertion(epService, geEplOne, 100, geCallback);
        tryAssertion(epService, geEplTwo, 100, geCallback);

        // Test Greater-Then
        String gtEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive > r.rangeStart and a.intPrimitive <= 99200";
        String gtEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange r unidirectional " +
                "where a.intPrimitive > r.rangeStart and a.intPrimitive <= 99200";
        String gtEplThree = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange#lastevent r, SB a " +
                "where a.intPrimitive > r.rangeStart and a.intPrimitive <= 99200";
        String gtEplFour = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange#lastevent r " +
                "where a.intPrimitive > r.rangeStart and a.intPrimitive <= 99200";
        AssertionCallback gtCallback = new AssertionCallback() {
            public Object getEvent(int iteration) {
                return new SupportBeanRange("E", iteration + 99000, null);
            }

            public Object[] getExpectedValue(int iteration) {
                return new Object[]{99001 + iteration, 99200};
            }
        };
        tryAssertion(epService, gtEplOne, 100, gtCallback);
        tryAssertion(epService, gtEplTwo, 100, gtCallback);
        tryAssertion(epService, gtEplThree, 100, gtCallback);
        tryAssertion(epService, gtEplFour, 100, gtCallback);

        // Test Less-Then
        String ltEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive < r.rangeStart and a.intPrimitive > 100";
        String ltEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange r unidirectional " +
                "where a.intPrimitive < r.rangeStart and a.intPrimitive > 100";
        AssertionCallback ltCallback = new AssertionCallback() {
            public Object getEvent(int iteration) {
                return new SupportBeanRange("E", iteration + 500, null);
            }

            public Object[] getExpectedValue(int iteration) {
                return new Object[]{101, 499 + iteration};
            }
        };
        tryAssertion(epService, ltEplOne, 100, ltCallback);
        tryAssertion(epService, ltEplTwo, 100, ltCallback);

        // Test Less-Equals
        String leEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive <= r.rangeStart and a.intPrimitive > 100";
        String leEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SB a, SupportBeanRange r unidirectional " +
                "where a.intPrimitive <= r.rangeStart and a.intPrimitive > 100";
        AssertionCallback leCallback = new AssertionCallback() {
            public Object getEvent(int iteration) {
                return new SupportBeanRange("E", iteration + 500, null);
            }

            public Object[] getExpectedValue(int iteration) {
                return new Object[]{101, 500 + iteration};
            }
        };
        tryAssertion(epService, leEplOne, 100, leCallback);
        tryAssertion(epService, leEplTwo, 100, leCallback);

        // Test open range
        String openEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive > r.rangeStart and a.intPrimitive < r.rangeEnd";
        String openEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive in (r.rangeStart:r.rangeEnd)";
        AssertionCallback openCallback = new AssertionCallback() {
            public Object getEvent(int iteration) {
                return new SupportBeanRange("E", iteration + 3, iteration + 7);
            }

            public Object[] getExpectedValue(int iteration) {
                return new Object[]{iteration + 4, iteration + 6};
            }
        };
        tryAssertion(epService, openEplOne, 100, openCallback);
        tryAssertion(epService, openEplTwo, 100, openCallback);

        // Test half-open range
        String hopenEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive >= r.rangeStart and a.intPrimitive < r.rangeEnd";
        String hopenEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive in [r.rangeStart:r.rangeEnd)";
        AssertionCallback halfOpenCallback = new AssertionCallback() {
            public Object getEvent(int iteration) {
                return new SupportBeanRange("E", iteration + 3, iteration + 7);
            }

            public Object[] getExpectedValue(int iteration) {
                return new Object[]{iteration + 3, iteration + 6};
            }
        };
        tryAssertion(epService, hopenEplOne, 100, halfOpenCallback);
        tryAssertion(epService, hopenEplTwo, 100, halfOpenCallback);

        // Test half-closed range
        String hclosedEplOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive > r.rangeStart and a.intPrimitive <= r.rangeEnd";
        String hclosedEplTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive in (r.rangeStart:r.rangeEnd]";
        AssertionCallback halfClosedCallback = new AssertionCallback() {
            public Object getEvent(int iteration) {
                return new SupportBeanRange("E", iteration + 3, iteration + 7);
            }

            public Object[] getExpectedValue(int iteration) {
                return new Object[]{iteration + 4, iteration + 7};
            }
        };
        tryAssertion(epService, hclosedEplOne, 100, halfClosedCallback);
        tryAssertion(epService, hclosedEplTwo, 100, halfClosedCallback);

        // Test inverted closed range
        String invertedClosedEPLOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive not in [r.rangeStart:r.rangeEnd]";
        String invertedClosedEPLTwo = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive not between r.rangeStart and r.rangeEnd";
        AssertionCallback invertedClosedCallback = new AssertionCallback() {
            public Object getEvent(int iteration) {
                return new SupportBeanRange("E", 20, 99990);
            }

            public Object[] getExpectedValue(int iteration) {
                return new Object[]{0, 99999};
            }
        };
        tryAssertion(epService, invertedClosedEPLOne, 100, invertedClosedCallback);
        tryAssertion(epService, invertedClosedEPLTwo, 100, invertedClosedCallback);

        // Test inverted open range
        String invertedOpenEPLOne = "select min(a.intPrimitive) as mini, max(a.intPrimitive) as maxi from SupportBeanRange r unidirectional, SB a " +
                "where a.intPrimitive not in (r.rangeStart:r.rangeEnd)";
        tryAssertion(epService, invertedOpenEPLOne, 100, invertedClosedCallback);
    }

    public void tryAssertion(EPServiceProvider epService, String epl, int numLoops, AssertionCallback assertionCallback) {
        String[] fields = "mini,maxi".split(",");

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // Send range query events
        log.info("Querying");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numLoops; i++) {
            //if (i % 10 == 0) {
            //    log.info("At loop #" + i);
            //}
            epService.getEPRuntime().sendEvent(assertionCallback.getEvent(i));
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, assertionCallback.getExpectedValue(i));
        }
        log.info("Done Querying");
        long endTime = System.currentTimeMillis();
        log.info("delta=" + (endTime - startTime));

        assertTrue((endTime - startTime) < 1500);
        stmt.destroy();
    }

    private static final Logger log = LoggerFactory.getLogger(ExecJoin2StreamRangePerformance.class);

    private static interface AssertionCallback {
        public Object getEvent(int iteration);

        public Object[] getExpectedValue(int iteration);
    }
}
