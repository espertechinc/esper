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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanAbstractSub;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecNamedWindowOnUpdate implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        runAssertionUpdateNonPropertySet(epService);
        runAssertionMultipleDataWindowIntersect(epService);
        runAssertionMultipleDataWindowUnion(epService);
        runAssertionSubclass(epService);
    }

    private void runAssertionUpdateNonPropertySet(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("setBeanLongPrimitive999", this.getClass().getName(), "setBeanLongPrimitive999");
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().createEPL("create window MyWindowUNP#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindowUNP select * from SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL("on SupportBean_S0 as sb " +
                "update MyWindowUNP as mywin" +
                " set mywin.setIntPrimitive(10)," +
                "     setBeanLongPrimitive999(mywin)");
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmt.addListener(listenerWindow);

        String[] fields = "intPrimitive,longPrimitive".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(listenerWindow.getAndResetLastNewData()[0], fields, new Object[]{10, 999L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMultipleDataWindowIntersect(EPServiceProvider epService) {
        String stmtTextCreate = "create window MyWindowMDW#unique(theString)#length(2) as select * from SupportBean";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        String stmtTextInsertOne = "insert into MyWindowMDW select * from SupportBean";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        String stmtTextUpdate = "on SupportBean_A update MyWindowMDW set intPrimitive=intPrimitive*100 where theString=id";
        epService.getEPAdministrator().createEPL(stmtTextUpdate);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        EventBean[] newevents = listenerWindow.getLastNewData();
        EventBean[] oldevents = listenerWindow.getLastOldData();

        assertEquals(1, newevents.length);
        EPAssertionUtil.assertProps(newevents[0], "intPrimitive".split(","), new Object[]{300});
        assertEquals(1, oldevents.length);
        oldevents = EPAssertionUtil.sort(oldevents, "theString");
        EPAssertionUtil.assertPropsPerRow(oldevents, "theString,intPrimitive".split(","), new Object[][]{{"E2", 3}});

        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), "theString,intPrimitive".split(","), new Object[][]{{"E1", 2}, {"E2", 300}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMultipleDataWindowUnion(EPServiceProvider epService) {
        String stmtTextCreate = "create window MyWindowMU#unique(theString)#length(2) retain-union as select * from SupportBean";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        String stmtTextInsertOne = "insert into MyWindowMU select * from SupportBean";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        String stmtTextUpdate = "on SupportBean_A update MyWindowMU mw set mw.intPrimitive=intPrimitive*100 where theString=id";
        epService.getEPAdministrator().createEPL(stmtTextUpdate);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        EventBean[] newevents = listenerWindow.getLastNewData();
        EventBean[] oldevents = listenerWindow.getLastOldData();

        assertEquals(1, newevents.length);
        EPAssertionUtil.assertProps(newevents[0], "intPrimitive".split(","), new Object[]{300});
        assertEquals(1, oldevents.length);
        EPAssertionUtil.assertPropsPerRow(oldevents, "theString,intPrimitive".split(","), new Object[][]{{"E2", 3}});

        EventBean[] events = EPAssertionUtil.sort(stmtCreate.iterator(), "theString");
        EPAssertionUtil.assertPropsPerRow(events, "theString,intPrimitive".split(","), new Object[][]{{"E1", 2}, {"E2", 300}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSubclass(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindowSC#keepall as select * from " + SupportBeanAbstractSub.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowSC select * from " + SupportBeanAbstractSub.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create update
        String stmtTextUpdate = "on " + SupportBean.class.getName() + " update MyWindowSC set v1=theString, v2=theString";
        epService.getEPAdministrator().createEPL(stmtTextUpdate);

        epService.getEPRuntime().sendEvent(new SupportBeanAbstractSub("value2"));
        listenerWindow.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], new String[]{"v1", "v2"}, new Object[]{"E1", "E1"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    // Don't delete me, dynamically-invoked
    public static void setBeanLongPrimitive999(SupportBean event) {
        event.setLongPrimitive(999);
    }
}