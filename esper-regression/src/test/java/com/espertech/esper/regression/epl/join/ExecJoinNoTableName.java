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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertNotNull;

public class ExecJoinNoTableName implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        SupportUpdateListener updateListener = new SupportUpdateListener();
        String joinStatement = "select * from " +
                SupportMarketDataBean.class.getName() + "#length(3)," +
                SupportBean.class.getName() + "#length(3)" +
                " where symbol=theString and volume=longBoxed";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        Object[] setOne = new Object[5];
        Object[] setTwo = new Object[5];

        for (int i = 0; i < setOne.length; i++) {
            setOne[i] = new SupportMarketDataBean("IBM", 0, (long) i, "");

            SupportBean theEvent = new SupportBean();
            theEvent.setTheString("IBM");
            theEvent.setLongBoxed((long) i);
            setTwo[i] = theEvent;
        }

        sendEvent(epService, setOne[0]);
        sendEvent(epService, setTwo[0]);
        assertNotNull(updateListener.getLastNewData());
    }

    private void sendEvent(EPServiceProvider epService, Object theEvent) {
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
