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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class ExecEPLSelectExprSQLCompat implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionProperty(epService);
        runAssertionPrefixStream(epService);

        runAssertionProperty(epService);
        runAssertionPrefixStream(epService);

        // allow no as-keyword
        epService.getEPAdministrator().createEPL("select intPrimitive abc from SupportBean");
    }

    private void runAssertionProperty(EPServiceProvider engine) {
        String epl = "select default.SupportBean.theString as val1, SupportBean.intPrimitive as val2 from SupportBean";
        EPStatement stmt = engine.getEPAdministrator().createEPL(epl);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        sendEvent(engine, "E1", 10);
        EventBean received = testListener.getAndResetLastNewData()[0];
        assertEquals("E1", received.get("val1"));
        assertEquals(10, received.get("val2"));

        stmt.destroy();
    }

    // Test stream name prefixed by engine URI
    private void runAssertionPrefixStream(EPServiceProvider engine) {
        String epl = "select theString from default.SupportBean";
        EPStatement stmt = engine.getEPAdministrator().createEPL(epl);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        sendEvent(engine, "E1", 10);
        EventBean received = testListener.getAndResetLastNewData()[0];
        assertEquals("E1", received.get("theString"));

        stmt.destroy();
    }

    private void sendEvent(EPServiceProvider engine, String s, int intPrimitive) {
        SupportBean bean = new SupportBean(s, intPrimitive);
        engine.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecEPLSelectExpr.class);
}
