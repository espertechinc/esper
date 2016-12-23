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

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import junit.framework.TestCase;

public class TestTableIterate extends TestCase {

    private final String METHOD_NAME = "method:SupportStaticMethodLib.fetchTwoRows3Cols()";

    private EPServiceProvider epService;

    public void setUp() {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        for (Class clazz : new Class[] {SupportBean.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        epService.getEPAdministrator().getConfiguration().addImport(SupportStaticMethodLib.class);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testIterate() {
        epService.getEPAdministrator().createEPL("@Resilient create table MyTable(pkey0 string primary key, pkey1 int primary key, c0 long)");
        epService.getEPAdministrator().createEPL("@Resilient insert into MyTable select theString as pkey0, intPrimitive as pkey1, longPrimitive as c0 from SupportBean");

        sendSupportBean("E1", 10, 100);
        sendSupportBean("E2", 20, 200);

        runAssertion(true);
        runAssertion(false);
    }

    private void runAssertion(boolean useTable) {
        runUnaggregatedUngroupedSelectStar(useTable);
        runFullyAggregatedAndUngrouped(useTable);
        runAggregatedAndUngrouped(useTable);
        runFullyAggregatedAndGrouped(useTable);
        runAggregatedAndGrouped(useTable);
        runAggregatedAndGroupedRollup(useTable);
    }

    private void runUnaggregatedUngroupedSelectStar(boolean useTable) {
        String epl = "select * from " + (useTable ? "MyTable" : METHOD_NAME);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), "pkey0,pkey1,c0".split(","), new Object[][]{{"E1", 10, 100L}, {"E2", 20, 200L}});
    }

    private void runFullyAggregatedAndUngrouped(boolean useTable) {
        String epl = "select count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        for (int i = 0; i < 2; i++) {
            EventBean event = stmt.iterator().next();
            assertEquals(2L, event.get("thecnt"));
        }
    }

    private void runAggregatedAndUngrouped(boolean useTable) {
        String epl = "select pkey0, count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        for (int i = 0; i < 2; i++) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), "pkey0,thecnt".split(","), new Object[][]{{"E1", 2L}, {"E2", 2L}});
        }
    }

    private void runFullyAggregatedAndGrouped(boolean useTable) {
        String epl = "select pkey0, count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME) + " group by pkey0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        for (int i = 0; i < 2; i++) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), "pkey0,thecnt".split(","), new Object[][]{{"E1", 1L}, {"E2", 1L}});
        }
    }

    private void runAggregatedAndGrouped(boolean useTable) {
        String epl = "select pkey0, pkey1, count(*) as thecnt from " + (useTable ? "MyTable" : METHOD_NAME) + " group by pkey0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        for (int i = 0; i < 2; i++) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), "pkey0,pkey1,thecnt".split(","), new Object[][]{{"E1", 10, 1L}, {"E2", 20, 1L}});
        }
    }

    private void runAggregatedAndGroupedRollup(boolean useTable) {
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

    private SupportBean sendSupportBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }
}
