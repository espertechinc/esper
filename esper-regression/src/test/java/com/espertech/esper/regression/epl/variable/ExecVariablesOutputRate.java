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
package com.espertech.esper.regression.epl.variable;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecVariablesOutputRate implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addVariable("var_output_limit", long.class, "3");

        runAssertionOutputRateEventsAll(epService);
        runAssertionOutputRateEventsAll_OM(epService);
        runAssertionOutputRateEventsAll_Compile(epService);
        runAssertionOutputRateTimeAll(epService);
    }

    private void runAssertionOutputRateEventsAll(EPServiceProvider epService) throws Exception {
        epService.getEPRuntime().setVariableValue("var_output_limit", 3L);
        String stmtTextSelect = "select count(*) as cnt from " + SupportBean.class.getName() + " output last every var_output_limit events";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);

        tryAssertionOutputRateEventsAll(epService, listener);

        stmtSelect.destroy();
    }

    private void runAssertionOutputRateEventsAll_OM(EPServiceProvider epService) throws Exception {
        epService.getEPRuntime().setVariableValue("var_output_limit", 3L);
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.countStar(), "cnt"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName())));
        model.setOutputLimitClause(OutputLimitClause.create(OutputLimitSelector.LAST, "var_output_limit"));

        String stmtTextSelect = "select count(*) as cnt from " + SupportBean.class.getName() + " output last every var_output_limit events";
        EPStatement stmtSelect = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);
        assertEquals(stmtTextSelect, model.toEPL());

        tryAssertionOutputRateEventsAll(epService, listener);

        stmtSelect.destroy();
    }

    private void runAssertionOutputRateEventsAll_Compile(EPServiceProvider epService) throws Exception {
        epService.getEPRuntime().setVariableValue("var_output_limit", 3L);
        String stmtTextSelect = "select count(*) as cnt from " + SupportBean.class.getName() + " output last every var_output_limit events";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtTextSelect);
        EPStatement stmtSelect = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);
        assertEquals(stmtTextSelect, model.toEPL());

        tryAssertionOutputRateEventsAll(epService, listener);

        stmtSelect.destroy();
    }

    private void tryAssertionOutputRateEventsAll(EPServiceProvider epService, SupportUpdateListener listener) throws Exception {
        sendSupportBeans(epService, "E1", "E2");   // varargs: sends 2 events
        assertFalse(listener.isInvoked());

        sendSupportBeans(epService, "E3");
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{3L});
        listener.reset();

        // set output limit to 5
        String stmtTextSet = "on " + SupportMarketDataBean.class.getName() + " set var_output_limit = volume";
        epService.getEPAdministrator().createEPL(stmtTextSet);
        sendSetterBean(epService, 5L);

        sendSupportBeans(epService, "E4", "E5", "E6", "E7"); // send 4 events
        assertFalse(listener.isInvoked());

        sendSupportBeans(epService, "E8");
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{8L});
        listener.reset();

        // set output limit to 2
        sendSetterBean(epService, 2L);

        sendSupportBeans(epService, "E9"); // send 1 events
        assertFalse(listener.isInvoked());

        sendSupportBeans(epService, "E10");
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{10L});
        listener.reset();

        // set output limit to 1
        sendSetterBean(epService, 1L);

        sendSupportBeans(epService, "E11");
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{11L});
        listener.reset();

        sendSupportBeans(epService, "E12");
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{12L});
        listener.reset();

        // set output limit to null -- this continues at the current rate
        sendSetterBean(epService, null);

        sendSupportBeans(epService, "E13");
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{13L});
        listener.reset();
    }

    private void runAssertionOutputRateTimeAll(EPServiceProvider epService) throws Exception {
        epService.getEPRuntime().setVariableValue("var_output_limit", 3L);
        sendTimer(epService, 0);

        String stmtTextSelect = "select count(*) as cnt from " + SupportBean.class.getName() + " output snapshot every var_output_limit seconds";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);

        sendSupportBeans(epService, "E1", "E2");   // varargs: sends 2 events
        sendTimer(epService, 2999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 3000);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{2L});
        listener.reset();

        // set output limit to 5
        String stmtTextSet = "on " + SupportMarketDataBean.class.getName() + " set var_output_limit = volume";
        epService.getEPAdministrator().createEPL(stmtTextSet);
        sendSetterBean(epService, 5L);

        // set output limit to 1 second
        sendSetterBean(epService, 1L);

        sendTimer(epService, 3200);
        sendSupportBeans(epService, "E3", "E4");
        sendTimer(epService, 3999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 4000);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{4L});
        listener.reset();

        // set output limit to 4 seconds (takes effect next time rescheduled, and is related to reference point which is 0)
        sendSetterBean(epService, 4L);

        sendTimer(epService, 4999);
        assertFalse(listener.isInvoked());
        sendTimer(epService, 5000);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{4L});
        listener.reset();

        sendTimer(epService, 7999);
        assertFalse(listener.isInvoked());
        sendTimer(epService, 8000);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{4L});
        listener.reset();

        sendSupportBeans(epService, "E5", "E6");   // varargs: sends 2 events

        sendTimer(epService, 11999);
        assertFalse(listener.isInvoked());
        sendTimer(epService, 12000);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], new String[]{"cnt"}, new Object[]{6L});
        listener.reset();

        sendTimer(epService, 13000);
        // set output limit to 2 seconds (takes effect next time event received, and is related to reference point which is 0)
        sendSetterBean(epService, 2L);
        sendSupportBeans(epService, "E7", "E8");   // varargs: sends 2 events
        assertFalse(listener.isInvoked());

        sendTimer(epService, 13999);
        assertFalse(listener.isInvoked());
        // set output limit to null : should stay at 2 seconds
        sendSetterBean(epService, null);
        try {
            sendTimer(epService, 14000);
            fail();
        } catch (RuntimeException ex) {
            // expected
        }
        stmtSelect.destroy();
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendSupportBeans(EPServiceProvider epService, String... strings) {
        for (String theString : strings) {
            sendSupportBean(epService, theString);
        }
    }

    private void sendSupportBean(EPServiceProvider epService, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSetterBean(EPServiceProvider epService, Long longValue) {
        SupportMarketDataBean bean = new SupportMarketDataBean("", 0, longValue, "");
        epService.getEPRuntime().sendEvent(bean);
    }
}
