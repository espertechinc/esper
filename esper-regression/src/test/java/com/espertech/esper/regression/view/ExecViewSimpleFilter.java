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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

public class ExecViewSimpleFilter implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNotEqualsOp(epService);
        runAssertionCombinationEqualsOp(epService);
    }

    private void runAssertionNotEqualsOp(EPServiceProvider epService) {
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select * from " + SupportBean.class.getName() +
                        "(theString != 'a')");
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, "a");
        assertFalse(listener.isInvoked());

        Object theEvent = sendEvent(epService, "b");
        assertSame(theEvent, listener.getAndResetLastNewData()[0].getUnderlying());

        sendEvent(epService, "a");
        assertFalse(listener.isInvoked());

        theEvent = sendEvent(epService, null);
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private void runAssertionCombinationEqualsOp(EPServiceProvider epService) {
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select * from " + SupportBean.class.getName() +
                        "(theString != 'a', intPrimitive=0)");
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, "b", 1);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "a", 0);
        assertFalse(listener.isInvoked());

        Object theEvent = sendEvent(epService, "x", 0);
        assertSame(theEvent, listener.getAndResetLastNewData()[0].getUnderlying());

        sendEvent(epService, null, 0);
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private Object sendEvent(EPServiceProvider epService, String stringValue) {
        return sendEvent(epService, stringValue, -1);
    }

    private Object sendEvent(EPServiceProvider epService, String stringValue, int intPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(stringValue);
        theEvent.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }
}
