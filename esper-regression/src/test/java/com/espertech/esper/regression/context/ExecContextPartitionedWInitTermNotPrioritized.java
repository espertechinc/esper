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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static junit.framework.TestCase.assertFalse;

public class ExecContextPartitionedWInitTermNotPrioritized implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        for (Class clazz : new Class[] {SupportBean.class, SupportBean_S0.class, SupportBean_S1.class, SupportBean_S2.class, ISupportA.class, ISupportB.class}) {
            configuration.addEventType(clazz);
        }
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionTermByFilter(epService);
    }

    private void runAssertionTermByFilter(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context ByP0 as partition by theString from SupportBean terminated by SupportBean(intPrimitive<0)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context ByP0 select theString, count(*) as cnt from SupportBean(intPrimitive>= 0)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendAssertSB(1, epService, listener, "A", 0);
        sendAssertSB(2, epService, listener, "A", 0);
        sendAssertNone(epService, listener, new SupportBean("A", -1));
        sendAssertSB(1, epService, listener, "A", 0);

        sendAssertSB(1, epService, listener, "B", 0);
        sendAssertNone(epService, listener, new SupportBean("B", -1));
        sendAssertSB(1, epService, listener, "B", 0);
        sendAssertSB(2, epService, listener, "B", 0);
        sendAssertNone(epService, listener, new SupportBean("B", -1));
        sendAssertSB(1, epService, listener, "B", 0);

        sendAssertNone(epService, listener, new SupportBean("C", -1));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendAssertSB(long expected, EPServiceProvider epService, SupportUpdateListener listener, String theString, int intPrimitive) {
        epService.getEPRuntime().sendEvent(new SupportBean(theString, intPrimitive));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString,cnt".split(","), new Object[] {theString, expected});
    }

    private void sendAssertNone(EPServiceProvider epService, SupportUpdateListener listener, Object event) {
        epService.getEPRuntime().sendEvent(event);
        assertFalse(listener.isInvoked());
    }
}
