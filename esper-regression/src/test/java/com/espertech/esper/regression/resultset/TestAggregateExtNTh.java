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

package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestAggregateExtNTh extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testNth()
    {
        String epl = "select " +
                "theString, " +
                "nth(intPrimitive,0) as int1, " +  // current
                "nth(intPrimitive,1) as int2 " +   // one before
                "from SupportBean#keepall group by theString output last every 3 events order by theString";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        runAssertion();

        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);
        assertEquals(epl, model.toEPL());

        runAssertion();
        
        tryInvalid("select nth() from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'nth(*)': The nth aggregation function requires two parameters, an expression returning aggregation values and a numeric index constant [select nth() from SupportBean]");
    }

    private void runAssertion() {
        String[] fields = "theString,int1,int2".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 11));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 12));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"G1", 12, 10}, {"G2", 11, null}});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 30));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 25));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"G2", 25, 20}});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", -1));
        epService.getEPRuntime().sendEvent(new SupportBean("G1", -2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 8));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"G1", -2, -1}, {"G2", 8, 25}});
    }

    private void tryInvalid(String epl, String message) {
        try
        {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }
}