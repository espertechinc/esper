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
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecViewSort implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setAllowMultipleExpiryPolicies(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        String epl = "select irstream * from SupportBean#sort(3, intPrimitive desc, longPrimitive)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "theString,intPrimitive,longPrimitive".split(",");

        epService.getEPRuntime().sendEvent(makeEvent("E1", 100, 0L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 100, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 100, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 99, 5L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 99, 5L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 100, 0L}, {"E2", 99, 5L}});

        epService.getEPRuntime().sendEvent(makeEvent("E3", 100, -1L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 100, -1L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3", 100, -1L}, {"E1", 100, 0L}, {"E2", 99, 5L}});

        epService.getEPRuntime().sendEvent(makeEvent("E4", 100, 1L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E4", 100, 1L}, new Object[] {"E2", 99, 5L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3", 100, -1L}, {"E1", 100, 0L}, {"E4", 100, 1L}});

        epService.getEPRuntime().sendEvent(makeEvent("E5", 101, 10L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E5", 101, 10L}, new Object[] {"E4", 100, 1L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E5", 101, 10L}, {"E3", 100, -1L}, {"E1", 100, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E6", 101, 11L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E6", 101, 11L}, new Object[] {"E1", 100, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E5", 101, 10L}, {"E6", 101, 11L}, {"E3", 100, -1L}});

        epService.getEPRuntime().sendEvent(makeEvent("E6", 100, 0L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E6", 100, 0L}, new Object[] {"E6", 100, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E5", 101, 10L}, {"E6", 101, 11L}, {"E3", 100, -1L}});

        stmt.destroy();
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

}
