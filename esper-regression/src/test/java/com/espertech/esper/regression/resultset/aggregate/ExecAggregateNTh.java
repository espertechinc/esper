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
package com.espertech.esper.regression.resultset.aggregate;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecAggregateNTh implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {

        String epl = "select " +
                "theString, " +
                "nth(intPrimitive,0) as int1, " +  // current
                "nth(intPrimitive,1) as int2 " +   // one before
                "from SupportBean#keepall group by theString output last every 3 events order by theString";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        runAssertion(epService, listener);

        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);
        assertEquals(epl, model.toEPL());

        runAssertion(epService, listener);

        tryInvalid(epService, "select nth() from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'nth(*)': The nth aggregation function requires two parameters, an expression returning aggregation values and a numeric index constant [select nth() from SupportBean]");
    }

    private void runAssertion(EPServiceProvider epService, SupportUpdateListener listener) {
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
}