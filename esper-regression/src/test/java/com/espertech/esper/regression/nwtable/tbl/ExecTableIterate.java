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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecTableIterate implements RegressionExecution {

    private final static String METHOD_NAME = "method:SupportStaticMethodLib.fetchTwoRows3Cols()";

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        epService.getEPAdministrator().getConfiguration().addImport(SupportStaticMethodLib.class);

        epService.getEPAdministrator().createEPL("@Resilient create table MyTable(pkey0 string primary key, pkey1 int primary key, c0 long)");
        epService.getEPAdministrator().createEPL("@Resilient insert into MyTable select theString as pkey0, intPrimitive as pkey1, longPrimitive as c0 from SupportBean");

        sendSupportBean(epService, "E1", 10, 100);
        sendSupportBean(epService, "E2", 20, 200);

        runAssertion(epService, true);
        runAssertion(epService, false);
    }

    private void runAssertion(EPServiceProvider epService, boolean useTable) {
        runUnaggregatedUngroupedSelectStar(epService, useTable);
        runFullyAggregatedAndUngrouped(epService, useTable);
        runAggregatedAndUngrouped(epService, useTable);
        runFullyAggregatedAndGrouped(epService, useTable);
        runAggregatedAndGrouped(epService, useTable);
        runAggregatedAndGroupedRollup(epService, useTable);
    }

    private void runUnaggregatedUngroupedSelectStar(EPServiceProvider epService, boolean useTable) {
        String epl = "select * from " + (useTable ? "MyTable" : METHOD_NAME);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), "pkey0,pkey1,c0".split(","), new Object[][]{{"E1", 10, 100L}, {"E2", 20, 200L}});
    }

    private void runFullyAggregatedAndUngrouped(EPServiceProvider epService, boolean useTable) {
        String epl = "select count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        for (int i = 0; i < 2; i++) {
            EventBean event = stmt.iterator().next();
            assertEquals(2L, event.get("thecnt"));
        }
    }

    private void runAggregatedAndUngrouped(EPServiceProvider epService, boolean useTable) {
        String epl = "select pkey0, count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        for (int i = 0; i < 2; i++) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), "pkey0,thecnt".split(","), new Object[][]{{"E1", 2L}, {"E2", 2L}});
        }
    }

    private void runFullyAggregatedAndGrouped(EPServiceProvider epService, boolean useTable) {
        String epl = "select pkey0, count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME) + " group by pkey0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        for (int i = 0; i < 2; i++) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), "pkey0,thecnt".split(","), new Object[][]{{"E1", 1L}, {"E2", 1L}});
        }
    }

    private void runAggregatedAndGrouped(EPServiceProvider epService, boolean useTable) {
        String epl = "select pkey0, pkey1, count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME) + " group by pkey0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        for (int i = 0; i < 2; i++) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), "pkey0,pkey1,thecnt".split(","), new Object[][]{{"E1", 10, 1L}, {"E2", 20, 1L}});
        }
    }

    private void runAggregatedAndGroupedRollup(EPServiceProvider epService, boolean useTable) {
        String epl = "select pkey0, pkey1, count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME) + " group by rollup (pkey0, pkey1)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        for (int i = 0; i < 2; i++) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), "pkey0,pkey1,thecnt".split(","), new Object[][]{
                    {"E1", 10, 1L},
                    {"E2", 20, 1L},
                    {"E1", null, 1L},
                    {"E2", null, 1L},
                    {null, null, 2L},
            });
        }
    }

    private SupportBean sendSupportBean(EPServiceProvider epService, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }
}
