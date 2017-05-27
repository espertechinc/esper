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
package com.espertech.esper.regression.epl.fromclausemethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationMethodRef;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanInt;
import com.espertech.esper.supportregression.epl.SupportJoinMethods;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class ExecFromClauseMethodJoinPerformance implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        configuration.addEventType(SupportBeanInt.class);

        ConfigurationMethodRef configMethod = new ConfigurationMethodRef();
        configMethod.setLRUCache(10);
        configuration.addMethodRef(SupportJoinMethods.class.getName(), configMethod);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertion1Stream2HistInnerJoinPerformance(epService);
        runAssertion1Stream2HistOuterJoinPerformance(epService);
        runAssertion2Stream1HistTwoSidedEntryIdenticalIndex(epService);
        runAssertion2Stream1HistTwoSidedEntryMixedIndex(epService);
    }

    private void runAssertion1Stream2HistInnerJoinPerformance(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt#lastevent as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', 100) as h0, " +
                "method:SupportJoinMethods.fetchVal('H1', 100) as h1 " +
                "where h0.index = p00 and h1.index = p00";

        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "id,valh0,valh1".split(",");
        Random random = new Random();

        long start = System.currentTimeMillis();
        for (int i = 1; i < 5000; i++) {
            int num = random.nextInt(98) + 1;
            sendBeanInt(epService, "E1", num);

            Object[][] result = new Object[][]{{"E1", "H0" + num, "H1" + num}};
            EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        stmt.destroy();
        assertTrue("Delta to large, at " + delta + " msec", delta < 1000);
    }

    private void runAssertion1Stream2HistOuterJoinPerformance(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt#lastevent as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H0', 100) as h0 " +
                " on h0.index = p00 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H1', 100) as h1 " +
                " on h1.index = p00";

        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "id,valh0,valh1".split(",");
        Random random = new Random();

        long start = System.currentTimeMillis();
        for (int i = 1; i < 5000; i++) {
            int num = random.nextInt(98) + 1;
            sendBeanInt(epService, "E1", num);

            Object[][] result = new Object[][]{{"E1", "H0" + num, "H1" + num}};
            EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        stmt.destroy();
        assertTrue("Delta to large, at " + delta + " msec", delta < 1000);
    }

    private void runAssertion2Stream1HistTwoSidedEntryIdenticalIndex(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as s0id, s1.id as s1id, h0.val as valh0 " +
                "from SupportBeanInt(id like 'E%')#lastevent as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', 100) as h0, " +
                "SupportBeanInt(id like 'F%')#lastevent as s1 " +
                "where h0.index = s0.p00 and h0.index = s1.p00";

        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "s0id,s1id,valh0".split(",");
        Random random = new Random();

        long start = System.currentTimeMillis();
        for (int i = 1; i < 1000; i++) {
            int num = random.nextInt(98) + 1;
            sendBeanInt(epService, "E1", num);
            sendBeanInt(epService, "F1", num);

            Object[][] result = new Object[][]{{"E1", "F1", "H0" + num}};
            EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);

            // send reset events to avoid duplicate matches
            sendBeanInt(epService, "E1", 0);
            sendBeanInt(epService, "F1", 0);
            listener.reset();
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("Delta to large, at " + delta + " msec", delta < 1000);
        stmt.destroy();
    }

    private void runAssertion2Stream1HistTwoSidedEntryMixedIndex(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as s0id, s1.id as s1id, h0.val as valh0, h0.index as indexh0 from " +
                "method:SupportJoinMethods.fetchVal('H0', 100) as h0, " +
                "SupportBeanInt(id like 'H%')#lastevent as s1, " +
                "SupportBeanInt(id like 'E%')#lastevent as s0 " +
                "where h0.index = s0.p00 and h0.val = s1.id";

        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "s0id,s1id,valh0,indexh0".split(",");
        Random random = new Random();

        long start = System.currentTimeMillis();
        for (int i = 1; i < 1000; i++) {
            int num = random.nextInt(98) + 1;
            sendBeanInt(epService, "E1", num);
            sendBeanInt(epService, "H0" + num, num);

            Object[][] result = new Object[][]{{"E1", "H0" + num, "H0" + num, num}};
            EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);

            // send reset events to avoid duplicate matches
            sendBeanInt(epService, "E1", 0);
            sendBeanInt(epService, "F1", 0);
            listener.reset();
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        stmt.destroy();
        assertTrue("Delta to large, at " + delta + " msec", delta < 1000);
    }

    private void sendBeanInt(EPServiceProvider epService, String id, int p00, int p01, int p02, int p03) {
        epService.getEPRuntime().sendEvent(new SupportBeanInt(id, p00, p01, p02, p03, -1, -1));
    }

    private void sendBeanInt(EPServiceProvider epService, String id, int p00) {
        sendBeanInt(epService, id, p00, -1, -1, -1);
    }
}
