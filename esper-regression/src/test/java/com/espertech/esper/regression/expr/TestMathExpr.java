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
package com.espertech.esper.regression.expr;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestMathExpr extends TestCase
{
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testIntDivisionIntResultZeroDevision()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getExpression().setIntegerDivision(true);
        config.getEngineDefaults().getExpression().setDivisionByZeroReturnsNull(true);
        config.addEventType("SupportBean", SupportBean.class);

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String viewExpr = "select intPrimitive/intBoxed as result from SupportBean";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);
        assertEquals(Integer.class, selectTestView.getEventType().getPropertyType("result"));

        sendEvent(epService, 100, 3);
        assertEquals(33, listener.assertOneGetNewAndReset().get("result"));

        sendEvent(epService, 100, null);
        assertEquals(null, listener.assertOneGetNewAndReset().get("result"));

        sendEvent(epService, 100, 0);
        assertEquals(null, listener.assertOneGetNewAndReset().get("result"));
    }

    public void testIntDivisionDoubleResultZeroDevision()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String viewExpr = "select intPrimitive/intBoxed as result from SupportBean";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("result"));

        sendEvent(epService, 100, 3);
        assertEquals(100/3d, listener.assertOneGetNewAndReset().get("result"));

        sendEvent(epService, 100, null);
        assertEquals(null, listener.assertOneGetNewAndReset().get("result"));

        sendEvent(epService, 100, 0);
        assertEquals(Double.POSITIVE_INFINITY, listener.assertOneGetNewAndReset().get("result"));

        sendEvent(epService, -5, 0);
        assertEquals(Double.NEGATIVE_INFINITY, listener.assertOneGetNewAndReset().get("result"));
    }

    private void sendEvent(EPServiceProvider epService, Integer intPrimitive, Integer intBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
