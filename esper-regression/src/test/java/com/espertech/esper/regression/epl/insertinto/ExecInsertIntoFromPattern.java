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
package com.espertech.esper.regression.epl.insertinto;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecInsertIntoFromPattern implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPropsWildcard(epService);
        runAssertionProps(epService);
        runAssertionNoProps(epService);
    }

    private void runAssertionPropsWildcard(EPServiceProvider epService) {
        String stmtText =
                "insert into MyThirdStream(es0id, es1id) " +
                        "select es0.id, es1.id " +
                        "from " +
                        "pattern [every (es0=" + SupportBean_S0.class.getName() +
                        " or es1=" + SupportBean_S1.class.getName() + ")]";
        epService.getEPAdministrator().createEPL(stmtText);

        String stmtTwoText =
                "select * from MyThirdStream";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtTwoText);

        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);

        sendEventsAndAssert(epService, updateListener);

        statement.destroy();
    }

    private void runAssertionProps(EPServiceProvider epService) {
        String stmtText =
                "insert into MySecondStream(s0, s1) " +
                        "select es0, es1 " +
                        "from " +
                        "pattern [every (es0=" + SupportBean_S0.class.getName() +
                        " or es1=" + SupportBean_S1.class.getName() + ")]";
        epService.getEPAdministrator().createEPL(stmtText);

        String stmtTwoText =
                "select s0.id as es0id, s1.id as es1id from MySecondStream";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtTwoText);

        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);

        sendEventsAndAssert(epService, updateListener);

        statement.destroy();
    }

    private void runAssertionNoProps(EPServiceProvider epService) {
        String stmtText =
                "insert into MyStream " +
                        "select es0, es1 " +
                        "from " +
                        "pattern [every (es0=" + SupportBean_S0.class.getName() +
                        " or es1=" + SupportBean_S1.class.getName() + ")]";
        epService.getEPAdministrator().createEPL(stmtText);

        String stmtTwoText =
                "select es0.id as es0id, es1.id as es1id from MyStream#length(10)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtTwoText);

        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);

        sendEventsAndAssert(epService, updateListener);

        statement.destroy();
    }

    private void sendEventsAndAssert(EPServiceProvider epService, SupportUpdateListener updateListener) {
        sendEventS1(epService, 10, "");
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertNull(theEvent.get("es0id"));
        assertEquals(10, theEvent.get("es1id"));

        sendEventS0(epService, 20, "");
        theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals(20, theEvent.get("es0id"));
        assertNull(theEvent.get("es1id"));
    }

    private void sendEventS0(EPServiceProvider epService, int id, String p00) {
        SupportBean_S0 theEvent = new SupportBean_S0(id, p00);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEventS1(EPServiceProvider epService, int id, String p10) {
        SupportBean_S1 theEvent = new SupportBean_S1(id, p10);
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
