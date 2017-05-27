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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecNamedWindowSubquery implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSubqueryTwoConsumerWindow(epService);
        runAssertionSubqueryLateConsumerAggregation(epService);
    }

    private void runAssertionSubqueryTwoConsumerWindow(EPServiceProvider epService) throws Exception {
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

    private void runAssertionSubqueryLateConsumerAggregation(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyWindow where (select count(*) from MyWindow) > 0");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        assertTrue(listener.isInvoked());
    }
}
