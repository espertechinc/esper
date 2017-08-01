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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanDuckType;
import com.espertech.esper.supportregression.bean.SupportBeanDuckTypeOne;
import com.espertech.esper.supportregression.bean.SupportBeanDuckTypeTwo;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecExprDotExpressionDuckTyping implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getExpression().setDuckTyping(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanDuckType", SupportBeanDuckType.class);

        String epl = "select " +
                "(dt).makeString() as strval, " +
                "(dt).makeInteger() as intval, " +
                "(dt).makeCommon().makeString() as commonstrval, " +
                "(dt).makeCommon().makeInteger() as commonintval, " +
                "(dt).returnDouble() as commondoubleval " +
                "from SupportBeanDuckType dt ";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Object[][] rows = new Object[][]{
                {"strval", Object.class},
                {"intval", Object.class},
                {"commonstrval", Object.class},
                {"commonintval", Object.class},
                {"commondoubleval", Double.class}   // this one is strongly typed
        };
        for (int i = 0; i < rows.length; i++) {
            EventPropertyDescriptor prop = stmt.getEventType().getPropertyDescriptors()[i];
            assertEquals(rows[i][0], prop.getPropertyName());
            assertEquals(rows[i][1], prop.getPropertyType());
        }

        String[] fields = "strval,intval,commonstrval,commonintval,commondoubleval".split(",");

        epService.getEPRuntime().sendEvent(new SupportBeanDuckTypeOne("x"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"x", null, null, -1, 12.9876d});

        epService.getEPRuntime().sendEvent(new SupportBeanDuckTypeTwo(-10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, -10, "mytext", null, 11.1234d});
    }
}
