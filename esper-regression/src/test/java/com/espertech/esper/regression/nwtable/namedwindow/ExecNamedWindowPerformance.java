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
package com.espertech.esper.regression.nwtable.namedwindow;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecNamedWindowPerformance implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanRange", SupportBeanRange.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S1", SupportBean_S1.class);

        runAssertionOnSelectInKeywordPerformance(epService);
        runAssertionOnSelectEqualsAndRangePerformance(epService);
        runAssertionDeletePerformance(epService);
        runAssertionDeletePerformanceCoercion(epService);
        runAssertionDeletePerformanceTwoDeleters(epService);
        runAssertionDeletePerformanceIndexReuse(epService);
    }

    private void runAssertionOnSelectInKeywordPerformance(EPServiceProvider epService) {
        // create window
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean_S0");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean_S0");

        int maxRows = 10000;   // for performance testing change to int maxRows = 100000;
        for (int i = 0; i < maxRows; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(i, "p00_" + i));
        }

        String eplSingleIdx = "on SupportBean_S1 select sum(mw.id) as sumi from MyWindow mw where p00 in (p10, p11)";
        runOnDemandAssertion(epService, eplSingleIdx, 1, new SupportBean_S1(0, "x", "p00_6523"), 6523);

        String eplMultiIndex = "on SupportBean_S1 select sum(mw.id) as sumi from MyWindow mw where p10 in (p00, p01)";
        runOnDemandAssertion(epService, eplMultiIndex, 2, new SupportBean_S1(0, "p00_6524"), 6524);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindow", true);
    }

    private void runAssertionOnSelectEqualsAndRangePerformance(EPServiceProvider epService) {
        // create window one
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");

        // insert X rows
        int maxRows = 10000;   //for performance testing change to int maxRows = 100000;
        for (int i = 0; i < maxRows; i++) {
            SupportBean bean = new SupportBean((i < 5000) ? "A" : "B", i);
            bean.setLongPrimitive(i);
            bean.setLongBoxed((long) i + 1);
            epService.getEPRuntime().sendEvent(bean);
        }
        epService.getEPRuntime().sendEvent(new SupportBean("B", 100));

        String eplIdx1One = "on SupportBeanRange sbr select sum(intPrimitive) as sumi from MyWindow where intPrimitive = sbr.rangeStart";
        runOnDemandAssertion(epService, eplIdx1One, 1, new SupportBeanRange("R", 5501, 0), 5501);

        String eplIdx1Two = "on SupportBeanRange sbr select sum(intPrimitive) as sumi from MyWindow where intPrimitive between sbr.rangeStart and sbr.rangeEnd";
        runOnDemandAssertion(epService, eplIdx1Two, 1, new SupportBeanRange("R", 5501, 5503), 5501 + 5502 + 5503);

        String eplIdx1Three = "on SupportBeanRange sbr select sum(intPrimitive) as sumi from MyWindow where theString = key and intPrimitive between sbr.rangeStart and sbr.rangeEnd";
        runOnDemandAssertion(epService, eplIdx1Three, 1, new SupportBeanRange("R", "A", 4998, 5503), 4998 + 4999);

        String eplIdx1Four = "on SupportBeanRange sbr select sum(intPrimitive) as sumi from MyWindow " +
                "where theString = key and longPrimitive = rangeStart and intPrimitive between rangeStart and rangeEnd " +
                "and longBoxed between rangeStart and rangeEnd";
        runOnDemandAssertion(epService, eplIdx1Four, 1, new SupportBeanRange("R", "A", 4998, 5503), 4998);

        String eplIdx1Five = "on SupportBeanRange sbr select sum(intPrimitive) as sumi from MyWindow " +
                "where intPrimitive between rangeStart and rangeEnd " +
                "and longBoxed between rangeStart and rangeEnd";
        runOnDemandAssertion(epService, eplIdx1Five, 1, new SupportBeanRange("R", "A", 4998, 5001), 4998 + 4999 + 5000);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindow", true);
    }

    private void runOnDemandAssertion(EPServiceProvider epService, String epl, int numIndexes, Object theEvent, Integer expected) {
        assertEquals(0, ((EPServiceProviderSPI) epService).getNamedWindowMgmtService().getNamedWindowIndexes("MyWindow").length);

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(numIndexes, ((EPServiceProviderSPI) epService).getNamedWindowMgmtService().getNamedWindowIndexes("MyWindow").length);

        long start = System.currentTimeMillis();
        int loops = 1000;

        for (int i = 0; i < loops; i++) {
            epService.getEPRuntime().sendEvent(theEvent);
            assertEquals(expected, listener.assertOneGetNewAndReset().get("sumi"));
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("delta=" + delta, delta < 1000);

        stmt.destroy();
        assertEquals(0, ((EPServiceProviderSPI) epService).getNamedWindowMgmtService().getNamedWindowIndexes("MyWindow").length);
    }

    private void runAssertionDeletePerformance(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall as select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);

        // create delete stmt
        String stmtTextDelete = "on " + SupportBean_A.class.getName() + " delete from MyWindow where id = a";
        epService.getEPAdministrator().createEPL(stmtTextDelete);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // load window
        for (int i = 0; i < 50000; i++) {
            sendSupportBean(epService, "S" + i, i);
        }

        // delete rows
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtCreate.addListener(listener);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            sendSupportBean_A(epService, "S" + i);
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        assertTrue("Delta=" + delta, delta < 500);

        // assert they are deleted
        assertEquals(50000 - 10000, EPAssertionUtil.iteratorCount(stmtCreate.iterator()));
        assertEquals(10000, listener.getOldDataList().size());

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindow", true);
    }

    private void runAssertionDeletePerformanceCoercion(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall as select theString as a, longPrimitive as b from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindow where b = price";
        epService.getEPAdministrator().createEPL(stmtTextDelete);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString as a, longPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // load window
        for (int i = 0; i < 50000; i++) {
            sendSupportBean(epService, "S" + i, (long) i);
        }

        // delete rows
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtCreate.addListener(listener);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            sendMarketBean(epService, "S" + i, i);
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        assertTrue("Delta=" + delta, delta < 500);

        // assert they are deleted
        assertEquals(50000 - 10000, EPAssertionUtil.iteratorCount(stmtCreate.iterator()));
        assertEquals(10000, listener.getOldDataList().size());

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindow", true);
    }

    private void runAssertionDeletePerformanceTwoDeleters(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecNamedWindowPerformance.class)) {
            return;
        }

        // create window
        String stmtTextCreate = "create window MyWindow#keepall as select theString as a, longPrimitive as b from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);

        // create delete stmt one
        String stmtTextDeleteOne = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindow where b = price";
        epService.getEPAdministrator().createEPL(stmtTextDeleteOne);

        // create delete stmt two
        String stmtTextDeleteTwo = "on " + SupportBean_A.class.getName() + " delete from MyWindow where id = a";
        epService.getEPAdministrator().createEPL(stmtTextDeleteTwo);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString as a, longPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // load window
        for (int i = 0; i < 20000; i++) {
            sendSupportBean(epService, "S" + i, (long) i);
        }

        // delete all rows
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtCreate.addListener(listener);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            sendMarketBean(epService, "S" + i, i);
            sendSupportBean_A(epService, "S" + (i + 10000));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        assertTrue("Delta=" + delta, delta < 1500);

        // assert they are all deleted
        assertEquals(0, EPAssertionUtil.iteratorCount(stmtCreate.iterator()));
        assertEquals(20000, listener.getOldDataList().size());

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindow", true);
    }

    private void runAssertionDeletePerformanceIndexReuse(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall as select theString as a, longPrimitive as b from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);

        // create delete stmt
        EPStatement[] statements = new EPStatement[50];
        for (int i = 0; i < statements.length; i++) {
            String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindow where b = price";
            statements[i] = epService.getEPAdministrator().createEPL(stmtTextDelete);
        }

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString as a, longPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // load window
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            sendSupportBean(epService, "S" + i, (long) i);
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        assertTrue("Delta=" + delta, delta < 1000);
        assertEquals(10000, EPAssertionUtil.iteratorCount(stmtCreate.iterator()));

        // destroy all
        for (EPStatement statement : statements) {
            statement.destroy();
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindow", true);
    }

    private void sendSupportBean_A(EPServiceProvider epService, String id) {
        SupportBean_A bean = new SupportBean_A(id);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketBean(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, long longPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
