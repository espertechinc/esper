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
package com.espertech.esper.regression.event.revision;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportDeltaFive;
import com.espertech.esper.supportregression.bean.SupportDeltaOne;
import com.espertech.esper.supportregression.bean.SupportRevisionFull;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;

public class ExecEventRevisionWindowedTime implements RegressionExecution {
    private final String[] fields = "k0,p1,p5".split(",");

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("FullEvent", SupportRevisionFull.class);
        configuration.addEventType("D1", SupportDeltaOne.class);
        configuration.addEventType("D5", SupportDeltaFive.class);

        ConfigurationRevisionEventType configRev = new ConfigurationRevisionEventType();
        configRev.setKeyPropertyNames(new String[]{"k0"});
        configRev.addNameBaseEventType("FullEvent");
        configRev.addNameDeltaEventType("D1");
        configRev.addNameDeltaEventType("D5");
        configuration.addRevisionEventType("RevisableQuote", configRev);
    }

    public void run(EPServiceProvider epService) throws Exception {
        if (SupportConfigFactory.skipTest(ExecEventRevisionWindowedTime.class)) {
            return;
        }
        runAssertionTimeWindow(epService);
    }

    private void runAssertionTimeWindow(EPServiceProvider epService) {
        sendTimer(epService, 0);
        EPStatement stmtCreateWin = epService.getEPAdministrator().createEPL("create window RevQuote#time(10 sec) as select * from RevisableQuote");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from FullEvent");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from D1");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from D5");

        EPStatement consumerOne = epService.getEPAdministrator().createEPL("select irstream * from RevQuote");
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        consumerOne.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new SupportRevisionFull("a", "a10", "a50"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"a", "a10", "a50"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"a", "a10", "a50"});

        sendTimer(epService, 1000);

        epService.getEPRuntime().sendEvent(new SupportDeltaFive("a", "a11", "a51"));
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"a", "a11", "a51"});
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"a", "a10", "a50"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"a", "a11", "a51"});

        sendTimer(epService, 2000);

        epService.getEPRuntime().sendEvent(new SupportRevisionFull("b", "b10", "b50"));
        epService.getEPRuntime().sendEvent(new SupportRevisionFull("c", "c10", "c50"));

        sendTimer(epService, 3000);
        epService.getEPRuntime().sendEvent(new SupportDeltaOne("c", "c11", "c51"));

        sendTimer(epService, 8000);
        epService.getEPRuntime().sendEvent(new SupportDeltaOne("c", "c12", "c52"));
        listenerOne.reset();

        sendTimer(epService, 10000);
        assertFalse(listenerOne.isInvoked());

        sendTimer(epService, 11000);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetOldAndReset(), fields, new Object[]{"a", "a11", "a51"});

        sendTimer(epService, 12000);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetOldAndReset(), fields, new Object[]{"b", "b10", "b50"});

        sendTimer(epService, 13000);
        assertFalse(listenerOne.isInvoked());

        sendTimer(epService, 18000);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetOldAndReset(), fields, new Object[]{"c", "c12", "c52"});
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
