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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;

public class ExecNWTableInfraSubqCorrelJoin implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S0Bean", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S1Bean", SupportBean_S1.class);

        // named window
        runAssertion(epService, true, false); // disable index-share
        runAssertion(epService, true, true); // enable-index-share

        // table
        runAssertion(epService, false, false);
    }

    private void runAssertion(EPServiceProvider epService, boolean namedWindow, boolean enableIndexShareCreate) {
        String createEpl = namedWindow ?
                "create window MyInfra#unique(theString) as select * from SupportBean" :
                "create table MyInfra(theString string primary key, intPrimitive int primary key)";
        if (enableIndexShareCreate) {
            createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
        }
        epService.getEPAdministrator().createEPL(createEpl);
        epService.getEPAdministrator().createEPL("insert into MyInfra select theString, intPrimitive from SupportBean");

        String consumeEpl = "select (select intPrimitive from MyInfra where theString = s1.p10) as val from S0Bean#lastevent as s0, S1Bean#lastevent as s1";
        EPStatement consumeStmt = epService.getEPAdministrator().createEPL(consumeEpl);
        SupportUpdateListener listener = new SupportUpdateListener();
        consumeStmt.addListener(listener);

        String[] fields = "val".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{30});

        consumeStmt.stop();
        consumeStmt.destroy();
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }
}
