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
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import junit.framework.TestCase;

public class TestInfraSubqueryAtEventBean extends TestCase
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

    public void testSubSelStar() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);

        runAssertionSubSelStar(true);
        runAssertionSubSelStar(false);
    }

    private void runAssertionSubSelStar(boolean namedWindow)
    {
        String eplCreate = namedWindow ?
                "create window MyInfra.win:keepall() as (c0 string, c1 int)" :
                "create table MyInfra(c0 string primary key, c1 int)";
        epService.getEPAdministrator().createEPL(eplCreate);

        // create insert into
        String eplInsert = "insert into MyInfra select theString as c0, intPrimitive as c1 from SupportBean";
        epService.getEPAdministrator().createEPL(eplInsert);

        // create subquery
        String eplSubquery = "select p00, (select * from MyInfra) @eventbean as detail from SupportBean_S0";
        EPStatement stmtSubquery = epService.getEPAdministrator().createEPL(eplSubquery);
        stmtSubquery.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertReceived(null);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertReceived(new Object[][] {{"E1", 1}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertReceived(new Object[][] {{"E1", 1}, {"E2", 2}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void assertReceived(Object[][] values) {
        EventBean event = listener.assertOneGetNewAndReset();
        EventBean[] events = (EventBean[]) event.getFragment("detail");
        if (values == null) {
            assertNull(events);
            return;
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(events, "c0,c1".split(","), values);
    }
}
