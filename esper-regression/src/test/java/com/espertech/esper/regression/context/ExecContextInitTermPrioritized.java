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
package com.espertech.esper.regression.context;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;

public class ExecContextInitTermPrioritized implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getExecution().setPrioritized(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNonOverlappingSubqueryAndInvalid(epService);
        runAssertionAtNowWithSelectedEventEnding(epService);
    }

    private void runAssertionNonOverlappingSubqueryAndInvalid(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(ExecContextInitTerm.Event.class);
        sendTimeEvent(epService, "2002-05-1T10:00:00.000");

        String epl =
                "\n @Name('ctx') create context RuleActivityTime as start (0, 9, *, *, *) end (0, 17, *, *, *);" +
                        "\n @Name('window') context RuleActivityTime create window EventsWindow#firstunique(productID) as Event;" +
                        "\n @Name('variable') create variable boolean IsOutputTriggered_2 = false;" +
                        "\n @Name('A') context RuleActivityTime insert into EventsWindow select * from Event(not exists (select * from EventsWindow));" +
                        "\n @Name('B') context RuleActivityTime insert into EventsWindow select * from Event(not exists (select * from EventsWindow));" +
                        "\n @Name('C') context RuleActivityTime insert into EventsWindow select * from Event(not exists (select * from EventsWindow));" +
                        "\n @Name('D') context RuleActivityTime insert into EventsWindow select * from Event(not exists (select * from EventsWindow));" +
                        "\n @Name('out') context RuleActivityTime select * from EventsWindow";

        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPAdministrator().getStatement("out").addListener(new SupportUpdateListener());

        epService.getEPRuntime().sendEvent(new ExecContextInitTerm.Event("A1"));

        // invalid - subquery not the same context
        tryInvalid(epService, "insert into EventsWindow select * from Event(not exists (select * from EventsWindow))",
                "Failed to validate subquery number 1 querying EventsWindow: Named window by name 'EventsWindow' has been declared for context 'RuleActivityTime' and can only be used within the same context");

        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(result.getDeploymentId());
    }

    private void runAssertionAtNowWithSelectedEventEnding(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        String[] fields = "theString".split(",");
        epService.getEPAdministrator().createEPL("@Priority(1) create context C1 start @now end SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL("@Priority(0) context C1 select * from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2"});
    }

    private void sendTimeEvent(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }
}
