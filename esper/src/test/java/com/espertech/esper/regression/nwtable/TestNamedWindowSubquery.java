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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestNamedWindowSubquery extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ABean", SupportBean_S0.class);
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testSubqueryTwoConsumerWindow() throws Exception {
        String epl =
            "\n create window MyWindowTwo#length(1) as (mycount long);" +
            "\n @Name('insert-count') insert into MyWindowTwo select 1L as mycount from SupportBean;" +
            "\n create variable long myvar = 0;" +
            "\n @Name('assign') on MyWindowTwo set myvar = (select mycount from MyWindowTwo);";
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider();
        engine.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        engine.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        engine.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(1L, engine.getEPRuntime().getVariableValue("myvar"));   // if the subquery-consumer executes first, this will be null
    }

    public void testSubqueryLateConsumerAggregation() {
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall() as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyWindow where (select count(*) from MyWindow) > 0");
        stmt.addListener(listener);
        
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        assertTrue(listener.isInvoked());
    }
}
