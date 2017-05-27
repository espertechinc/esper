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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecViewMultipleExpiry implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setAllowMultipleExpiryPolicies(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        // Testing the two forms of the case expression
        // Furthermore the test checks the different when clauses and actions related.
        String caseExpr = "select volume " +
                "from " + SupportMarketDataBean.class.getName() + "#unique(symbol)#time(10)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        sendMarketDataEvent(epService, "DELL", 1, 50);
        sendMarketDataEvent(epService, "DELL", 2, 50);
        Object[] values = EPAssertionUtil.iteratorToArray(stmt.iterator());
        assertEquals(1, values.length);
    }

    private void sendMarketDataEvent(EPServiceProvider epService, String symbol, long volume, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }
}
