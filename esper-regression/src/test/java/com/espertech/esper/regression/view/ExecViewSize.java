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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecViewSize implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        String statementText = "select irstream size from " + SupportMarketDataBean.class.getName() + "#size";
        EPStatement stmt = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "DELL", 1L);
        assertSize(listener, 1, 0);

        sendEvent(epService, "DELL", 1L);
        assertSize(listener, 2, 1);

        stmt.destroy();
        statementText = "select size, symbol, feed from " + SupportMarketDataBean.class.getName() + "#size(symbol, feed)";
        stmt = epService.getEPAdministrator().createEPL(statementText);
        stmt.addListener(listener);
        String[] fields = "size,symbol,feed".split(",");

        sendEvent(epService, "DELL", 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L, "DELL", "f1"});

        sendEvent(epService, "DELL", 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2L, "DELL", "f1"});
    }

    private void sendEvent(EPServiceProvider epService, String symbol, Long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "f1");
        epService.getEPRuntime().sendEvent(bean);
    }

    private void assertSize(SupportUpdateListener listener, long newSize, long oldSize) {
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "size", new Object[]{newSize}, new Object[]{oldSize});
    }
}
