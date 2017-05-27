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
package com.espertech.esper.regression.nwtable.infra;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertNull;

public class ExecNWTableInfraSubqueryAtEventBean implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("ABean", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);

        runAssertionSubSelStar(epService, true);
        runAssertionSubSelStar(epService, false);
    }

    private void runAssertionSubSelStar(EPServiceProvider epService, boolean namedWindow) {
        String eplCreate = namedWindow ?
                "create window MyInfra#keepall as (c0 string, c1 int)" :
                "create table MyInfra(c0 string primary key, c1 int)";
        epService.getEPAdministrator().createEPL(eplCreate);

        // create insert into
        String eplInsert = "insert into MyInfra select theString as c0, intPrimitive as c1 from SupportBean";
        epService.getEPAdministrator().createEPL(eplInsert);

        // create subquery
        String eplSubquery = "select p00, (select * from MyInfra) @eventbean as detail from SupportBean_S0";
        EPStatement stmtSubquery = epService.getEPAdministrator().createEPL(eplSubquery);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSubquery.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertReceived(listener, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertReceived(listener, new Object[][]{{"E1", 1}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertReceived(listener, new Object[][]{{"E1", 1}, {"E2", 2}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void assertReceived(SupportUpdateListener listener, Object[][] values) {
        EventBean event = listener.assertOneGetNewAndReset();
        EventBean[] events = (EventBean[]) event.getFragment("detail");
        if (values == null) {
            assertNull(events);
            return;
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(events, "c0,c1".split(","), values);
    }
}
