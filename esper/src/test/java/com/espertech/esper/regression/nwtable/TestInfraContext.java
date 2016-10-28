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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestInfraContext extends TestCase
{
    private EPServiceProviderSPI epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = (EPServiceProviderSPI) EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }
    
    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testContext() {
        for (Class clazz : new Class[] {SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionContext(true);
        runAssertionContext(false);
    }

    private void runAssertionContext(boolean namedWindow) {
        epService.getEPAdministrator().createEPL("create context ContextOne start SupportBean_S0 end SupportBean_S1");

        String eplCreate = namedWindow ?
                "context ContextOne create window MyInfra#keepall() as (pkey0 string, pkey1 int, c0 long)" :
                "context ContextOne create table MyInfra as (pkey0 string primary key, pkey1 int primary key, c0 long)";
        epService.getEPAdministrator().createEPL(eplCreate);

        epService.getEPAdministrator().createEPL("context ContextOne insert into MyInfra select theString as pkey0, intPrimitive as pkey1, longPrimitive as c0 from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));  // start

        makeSendSupportBean("E1", 10, 100);
        makeSendSupportBean("E2", 20, 200);

        SupportUpdateListener listenerUnAggUngr = register("context ContextOne select * from MyInfra output snapshot when terminated");
        SupportUpdateListener listenerFullyAggUngr = register("context ContextOne select count(*) as thecnt from MyInfra output snapshot when terminated");
        SupportUpdateListener listenerAggUngr = register("context ContextOne select pkey0, count(*) as thecnt from MyInfra output snapshot when terminated");
        SupportUpdateListener listenerFullyAggGroup = register("context ContextOne select pkey0, count(*) as thecnt from MyInfra group by pkey0 output snapshot when terminated");
        SupportUpdateListener listenerAggGroup = register("context ContextOne select pkey0, pkey1, count(*) as thecnt from MyInfra group by pkey0 output snapshot when terminated");
        SupportUpdateListener listenerAggGroupRollup = register("context ContextOne select pkey0, pkey1, count(*) as thecnt from MyInfra group by rollup (pkey0, pkey1) output snapshot when terminated");

        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));  // end

        EPAssertionUtil.assertPropsPerRowAnyOrder(listenerUnAggUngr.getAndResetLastNewData(), "pkey0,pkey1,c0".split(","), new Object[][]{{"E1", 10, 100L}, {"E2", 20, 200L}});
        EPAssertionUtil.assertProps(listenerFullyAggUngr.assertOneGetNewAndReset(), "thecnt".split(","), new Object[]{2L});
        EPAssertionUtil.assertPropsPerRowAnyOrder(listenerAggUngr.getAndResetLastNewData(), "pkey0,thecnt".split(","), new Object[][]{{"E1", 2L}, {"E2", 2L}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(listenerFullyAggGroup.getAndResetLastNewData(), "pkey0,thecnt".split(","), new Object[][]{{"E1", 1L}, {"E2", 1L}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(listenerAggGroup.getAndResetLastNewData(), "pkey0,pkey1,thecnt".split(","), new Object[][]{{"E1", 10, 1L}, {"E2", 20, 1L}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(listenerAggGroupRollup.getAndResetLastNewData(), "pkey0,pkey1,thecnt".split(","), new Object[][]{
                {"E1", 10, 1L}, {"E2", 20, 1L}, {"E1", null, 1L}, {"E2", null, 1L}, {null, null, 2L}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private SupportUpdateListener register(String epl) {
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        return listener;
    }

    private void makeSendSupportBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        b.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(b);
    }

}
