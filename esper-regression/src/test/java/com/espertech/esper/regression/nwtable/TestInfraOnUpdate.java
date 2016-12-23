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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestInfraOnUpdate extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listenerWindow;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listenerWindow = new SupportUpdateListener();
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listenerWindow = null;
    }

    public void testUpdateOrderOfFields() throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        runAssertionUpdateOrderOfFields(true);
        runAssertionUpdateOrderOfFields(false);
    }

    public void testSubquerySelf() {
        runAssertionSubquerySelf(true);
        runAssertionSubquerySelf(false);
    }

    private void runAssertionUpdateOrderOfFields(boolean namedWindow) throws Exception {

        String eplCreate = namedWindow ?
                "create window MyInfra#keepall as SupportBean" :
                "create table MyInfra(theString string primary key, intPrimitive int, intBoxed int, doublePrimitive double)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfra select theString, intPrimitive, intBoxed, doublePrimitive from SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL("on SupportBean_S0 as sb " +
                "update MyInfra as mywin" +
                " set intPrimitive=id, intBoxed=mywin.intPrimitive, doublePrimitive=initial.intPrimitive" +
                " where mywin.theString = sb.p00");
        stmt.addListener(listenerWindow);
        String[] fields = "intPrimitive,intBoxed,doublePrimitive".split(",");

        epService.getEPRuntime().sendEvent(makeSupportBean("E1", 1, 2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(5, "E1"));
        EPAssertionUtil.assertProps(listenerWindow.getAndResetLastNewData()[0], fields, new Object[]{5, 5, 1.0});

        epService.getEPRuntime().sendEvent(makeSupportBean("E2", 10, 20));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(6, "E2"));
        EPAssertionUtil.assertProps(listenerWindow.getAndResetLastNewData()[0], fields, new Object[]{6, 6, 10.0});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(7, "E1"));
        EPAssertionUtil.assertProps(listenerWindow.getAndResetLastNewData()[0], fields, new Object[]{7, 7, 5.0});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionSubquerySelf(boolean namedWindow) {
        // ESPER-507

        String eplCreate = namedWindow ?
                "create window MyInfra#keepall as SupportBean" :
                "create table MyInfra(theString string primary key, intPrimitive int)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfra select theString, intPrimitive from SupportBean");

        // This is better done with "set intPrimitive = intPrimitive + 1"
        String epl = "@Name(\"Self Update\")\n" +
                "on SupportBean_A c\n" +
                "update MyInfra s\n" +
                "set intPrimitive = (select intPrimitive from MyInfra t where t.theString = c.id) + 1\n" +
                "where s.theString = c.id";
        epService.getEPAdministrator().createEPL(epl);
        
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 6));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), "theString,intPrimitive".split(","), new Object[][]{{"E1", 3}, {"E2", 7}});
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private SupportBean makeSupportBean(String theString, int intPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        return sb;
    }
}