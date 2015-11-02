/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestSelectExprSQLCompat extends TestCase
{
    private SupportUpdateListener testListener;
    private Configuration config;

    public void setUp()
    {
        testListener = new SupportUpdateListener();
        config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
        config = null;
    }

    public void testQualifiedPropertyNamed()
    {
        EPServiceProvider epService = EPServiceProviderManager.getProvider("default", config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        runAssertionProperty(epService);
        runAssertionPrefixStream(epService);
    }

    public void testQualifiedPropertyUnnamed()
    {
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        runAssertionProperty(epService);
        runAssertionPrefixStream(epService);

        // allow no as-keyword
        epService.getEPAdministrator().createEPL("select intPrimitive abc from SupportBean");
    }

    private void runAssertionProperty(EPServiceProvider engine)
    {
        String epl = "select default.SupportBean.theString as val1, SupportBean.intPrimitive as val2 from SupportBean";
        EPStatement selectTestView = engine.getEPAdministrator().createEPL(epl);
        selectTestView.addListener(testListener);

        sendEvent(engine, "E1", 10);
        EventBean received = testListener.getAndResetLastNewData()[0];
        assertEquals("E1", received.get("val1"));
        assertEquals(10, received.get("val2"));
    }

    // Test stream name prefixed by engine URI
    private void runAssertionPrefixStream(EPServiceProvider engine)
    {
        String epl = "select theString from default.SupportBean";
        EPStatement selectTestView = engine.getEPAdministrator().createEPL(epl);
        selectTestView.addListener(testListener);

        sendEvent(engine, "E1", 10);
        EventBean received = testListener.getAndResetLastNewData()[0];
        assertEquals("E1", received.get("theString"));
    }

    private void sendEvent(EPServiceProvider engine, String s, int intPrimitive)
    {
        SupportBean bean = new SupportBean(s, intPrimitive);
        engine.getEPRuntime().sendEvent(bean);
    }

    private static final Log log = LogFactory.getLog(TestSelectExpr.class);
}
