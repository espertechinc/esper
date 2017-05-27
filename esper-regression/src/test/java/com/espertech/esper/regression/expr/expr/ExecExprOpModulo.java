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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecExprOpModulo implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        String epl = "select longBoxed % intBoxed as myMod " +
                " from " + SupportBean.class.getName() + "#length(3) where not(longBoxed > intBoxed)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, 1, 1, (short) 0);
        assertEquals(0L, listener.getLastNewData()[0].get("myMod"));
        listener.reset();

        sendEvent(epService, 2, 1, (short) 0);
        assertFalse(listener.getAndClearIsInvoked());

        sendEvent(epService, 2, 3, (short) 0);
        assertEquals(2L, listener.getLastNewData()[0].get("myMod"));
        listener.reset();
    }

    private void sendEvent(EPServiceProvider epService, long longBoxed, int intBoxed, short shortBoxed) {
        sendBoxedEvent(epService, longBoxed, intBoxed, shortBoxed);
    }

    private void sendBoxedEvent(EPServiceProvider epService, Long longBoxed, Integer intBoxed, Short shortBoxed) {
        SupportBean bean = new SupportBean();
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }
}
