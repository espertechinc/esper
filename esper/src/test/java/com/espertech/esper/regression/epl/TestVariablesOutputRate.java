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

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;

public class TestVariablesOutputRate extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testOutputRateEventsAll() throws Exception
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var_output_limit", long.class, "3");

        String stmtTextSelect = "select count(*) as cnt from " + SupportBean.class.getName() + " output last every var_output_limit events";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        stmtSelect.addListener(listener);

        runAssertionOutputRateEventsAll();
    }

    public void testOutputRateEventsAll_OM() throws Exception
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var_output_limit", long.class, "3");

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.countStar(), "cnt"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName())));
        model.setOutputLimitClause(OutputLimitClause.create(OutputLimitSelector.LAST, "var_output_limit"));

        String stmtTextSelect = "select count(*) as cnt from " + SupportBean.class.getName() + " output last every var_output_limit events";
        EPStatement stmtSelect = epService.getEPAdministrator().create(model);
        stmtSelect.addListener(listener);
        assertEquals(stmtTextSelect, model.toEPL());

        runAssertionOutputRateEventsAll();
    }

    public void testOutputRateEventsAll_Compile() throws Exception
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var_output_limit", long.class, "3");

        String stmtTextSelect = "select count(*) as cnt from " + SupportBean.class.getName() + " output last every var_output_limit events";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtTextSelect);
        EPStatement stmtSelect = epService.getEPAdministrator().create(model);
        stmtSelect.addListener(listener);
        assertEquals(stmtTextSelect, model.toEPL());

        runAssertionOutputRateEventsAll();
    }

    private void runAssertionOutputRateEventsAll() throws Exception
    {
        sendSupportBeans("E1", "E2");   // varargs: sends 2 events
        assertFalse(listener.isInvoked());

        sendSupportBeans("E3");
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{3L});
        listener.reset();

        // set output limit to 5
        String stmtTextSet = "on " + SupportMarketDataBean.class.getName() + " set var_output_limit = volume";
        epService.getEPAdministrator().createEPL(stmtTextSet);
        sendSetterBean(5L);

        sendSupportBeans("E4", "E5", "E6", "E7"); // send 4 events
        assertFalse(listener.isInvoked());

        sendSupportBeans("E8");
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{8L});
        listener.reset();

        // set output limit to 2
        sendSetterBean(2L);

        sendSupportBeans("E9"); // send 1 events
        assertFalse(listener.isInvoked());

        sendSupportBeans("E10");
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{10L});
        listener.reset();

        // set output limit to 1
        sendSetterBean(1L);

        sendSupportBeans("E11");
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{11L});
        listener.reset();

        sendSupportBeans("E12");
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{12L});
        listener.reset();

        // set output limit to null -- this continues at the current rate
        sendSetterBean(null);

        sendSupportBeans("E13");
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{13L});
        listener.reset();
    }

    public void testOutputRateTimeAll() throws Exception
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var_output_limit", long.class, "3");
        sendTimer(0);

        String stmtTextSelect = "select count(*) as cnt from " + SupportBean.class.getName() + " output snapshot every var_output_limit seconds";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        stmtSelect.addListener(listener);

        sendSupportBeans("E1", "E2");   // varargs: sends 2 events
        sendTimer(2999);
        assertFalse(listener.isInvoked());

        sendTimer(3000);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{2L});
        listener.reset();

        // set output limit to 5
        String stmtTextSet = "on " + SupportMarketDataBean.class.getName() + " set var_output_limit = volume";
        epService.getEPAdministrator().createEPL(stmtTextSet);
        sendSetterBean(5L);

        // set output limit to 1 second
        sendSetterBean(1L);

        sendTimer(3200);
        sendSupportBeans("E3", "E4");
        sendTimer(3999);
        assertFalse(listener.isInvoked());

        sendTimer(4000);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{4L});
        listener.reset();

        // set output limit to 4 seconds (takes effect next time rescheduled, and is related to reference point which is 0)
        sendSetterBean(4L);

        sendTimer(4999);
        assertFalse(listener.isInvoked());
        sendTimer(5000);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{4L});
        listener.reset();

        sendTimer(7999);
        assertFalse(listener.isInvoked());
        sendTimer(8000);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{4L});
        listener.reset();

        sendSupportBeans("E5", "E6");   // varargs: sends 2 events

        sendTimer(11999);
        assertFalse(listener.isInvoked());
        sendTimer(12000);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{6L});
        listener.reset();

        sendTimer(13000);
        // set output limit to 2 seconds (takes effect next time event received, and is related to reference point which is 0)
        sendSetterBean(2L);
        sendSupportBeans("E7", "E8");   // varargs: sends 2 events
        assertFalse(listener.isInvoked());

        sendTimer(13999);
        assertFalse(listener.isInvoked());
        // set output limit to null : should stay at 2 seconds
        sendSetterBean(null);
        try {
            sendTimer(14000);
            fail();
        }
        catch (RuntimeException ex) {
            // expected
        }
    }

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendSupportBeans(String ...strings)
    {
        for (String theString : strings)
        {
            sendSupportBean(theString);
        }
    }

    private SupportBean sendSupportBean(String theString)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportMarketDataBean sendSetterBean(Long longValue)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean("", 0, longValue, "");
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }
}
