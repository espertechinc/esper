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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecNWTableInfraSubqFilteredCorrel implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ABean", SupportBean_S0.class);

        // named window tests
        runAssertion(epService, true, false, false, false);  // no-share
        runAssertion(epService, true, false, false, true);   // no-share create
        runAssertion(epService, true, true, false, false);   // share no-create
        runAssertion(epService, true, true, true, false);    // disable share no-create
        runAssertion(epService, true, true, true, true);     // disable share create

        // table tests
        runAssertion(epService, false, false, false, false);  // table no-create
        runAssertion(epService, false, false, false, true);  // table create
    }

    private void runAssertion(EPServiceProvider epService, boolean namedWindow, boolean enableIndexShareCreate, boolean disableIndexShareConsumer, boolean createExplicitIndex) {

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);

        String createEpl = namedWindow ?
                "create window MyInfra#keepall as select * from SupportBean" :
                "create table MyInfra (theString string primary key, intPrimitive int primary key)";
        if (enableIndexShareCreate) {
            createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
        }
        epService.getEPAdministrator().createEPL(createEpl);
        epService.getEPAdministrator().createEPL("insert into MyInfra select theString, intPrimitive from SupportBean");

        EPStatement indexStmt = null;
        if (createExplicitIndex) {
            indexStmt = epService.getEPAdministrator().createEPL("create index MyIndex on MyInfra(theString)");
        }

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", -2));

        String consumeEpl = "select (select intPrimitive from MyInfra(intPrimitive<0) sw where s0.p00=sw.theString) as val from S0 s0";
        if (disableIndexShareConsumer) {
            consumeEpl = "@Hint('disable_window_subquery_indexshare') " + consumeEpl;
        }
        EPStatement consumeStmt = epService.getEPAdministrator().createEPL(consumeEpl);
        consumeStmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E1"));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "E2"));
        assertEquals(-2, listener.assertOneGetNewAndReset().get("val"));

        epService.getEPRuntime().sendEvent(new SupportBean("E3", -3));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-3, "E3"));
        assertEquals(-3, listener.assertOneGetNewAndReset().get("val"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "E4"));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val"));

        consumeStmt.stop();
        if (indexStmt != null) {
            indexStmt.stop();
        }
        consumeStmt.destroy();
        if (indexStmt != null) {
            indexStmt.destroy();
        }
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }
}
