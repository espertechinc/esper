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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.regression.epl.other.ExecEPLSelectExpr;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class ExecExprConcat implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        String epl = "select p00 || p01 as c1, p00 || p01 || p02 as c2, p00 || '|' || p01 as c3" +
                " from " + SupportBean_S0.class.getName() + "#length(10)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "a", "b", "c"));
        assertConcat(listener, "ab", "abc", "a|b");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, null, "b", "c"));
        assertConcat(listener, null, null, null);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "", "b", "c"));
        assertConcat(listener, "b", "bc", "|b");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "123", null, "c"));
        assertConcat(listener, null, null, null);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "123", "456", "c"));
        assertConcat(listener, "123456", "123456c", "123|456");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "123", "456", null));
        assertConcat(listener, "123456", null, "123|456");
    }

    private void assertConcat(SupportUpdateListener listener, String c1, String c2, String c3) {
        EventBean theEvent = listener.getLastNewData()[0];
        assertEquals(c1, theEvent.get("c1"));
        assertEquals(c2, theEvent.get("c2"));
        assertEquals(c3, theEvent.get("c3"));
        listener.reset();
    }

    private static final Logger log = LoggerFactory.getLogger(ExecEPLSelectExpr.class);
}
