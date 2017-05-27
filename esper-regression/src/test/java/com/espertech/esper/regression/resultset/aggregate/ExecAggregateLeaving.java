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

public class ExecAggregateLeaving implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String epl = "select leaving() as val from SupportBean#length(3)";
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

        tryInvalid(epService, "select leaving(1) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'leaving(1)': The 'leaving' function expects no parameters");
    }

    private void runAssertion(EPServiceProvider epService, SupportUpdateListener listener) {
        String[] fields = "val".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true});
    }
}
