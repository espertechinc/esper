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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.StreamSelector;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ExecEPLIStreamRStreamConfigSelectorIRStream implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getStreamSelection().setDefaultStreamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String stmtText = "select * from " + SupportBean.class.getName() + "#length(3)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener testListener = new SupportUpdateListener();
        statement.addListener(testListener);

        Object eventOld = sendEvent(epService, "a");
        sendEvent(epService, "b");
        sendEvent(epService, "c");
        testListener.reset();

        Object eventNew = sendEvent(epService, "d");
        assertTrue(testListener.isInvoked());
        assertSame(eventNew, testListener.getLastNewData()[0].getUnderlying());    // receive 'a' as new data
        assertSame(eventOld, testListener.getLastOldData()[0].getUnderlying());    // receive 'a' as new data
    }

    private Object sendEvent(EPServiceProvider epService, String stringValue) {
        SupportBean theEvent = new SupportBean(stringValue, 0);
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }
}
