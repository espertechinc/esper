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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.ISupportA;
import com.espertech.esper.supportregression.bean.ISupportAImpl;
import com.espertech.esper.supportregression.bean.ISupportB;
import com.espertech.esper.supportregression.bean.ISupportBImpl;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecJoinInheritAndInterface implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        String epl = "select a, b from " +
                ISupportA.class.getName() + "#length(10), " +
                ISupportB.class.getName() + "#length(10)" +
                " where a = b";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new ISupportAImpl("1", "ab1"));
        epService.getEPRuntime().sendEvent(new ISupportBImpl("2", "ab2"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new ISupportBImpl("1", "ab3"));
        assertTrue(listener.isInvoked());
        EventBean theEvent = listener.getAndResetLastNewData()[0];
        assertEquals("1", theEvent.get("a"));
        assertEquals("1", theEvent.get("b"));
    }
}
