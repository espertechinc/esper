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
package com.espertech.esper.regressionlib.suite.expr.exprcore;

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanDuckTypeOne;
import com.espertech.esper.regressionlib.support.bean.SupportBeanDuckTypeTwo;

import static org.junit.Assert.assertEquals;

public class ExprCoreDotExpressionDuckTyping implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String epl = "@name('s0') select " +
            "(dt).makeString() as strval, " +
            "(dt).makeInteger() as intval, " +
            "(dt).makeCommon().makeString() as commonstrval, " +
            "(dt).makeCommon().makeInteger() as commonintval, " +
            "(dt).returnDouble() as commondoubleval " +
            "from SupportBeanDuckType dt ";
        env.compileDeploy(epl).addListener("s0");

        Object[][] rows = new Object[][]{
            {"strval", Object.class},
            {"intval", Object.class},
            {"commonstrval", Object.class},
            {"commonintval", Object.class},
            {"commondoubleval", Double.class}   // this one is strongly typed
        };
        env.assertStatement("s0", statement -> {
            for (int i = 0; i < rows.length; i++) {
                EventPropertyDescriptor prop = statement.getEventType().getPropertyDescriptors()[i];
                assertEquals(rows[i][0], prop.getPropertyName());
                assertEquals(rows[i][1], prop.getPropertyType());
            }
        });

        String[] fields = "strval,intval,commonstrval,commonintval,commondoubleval".split(",");

        env.sendEventBean(new SupportBeanDuckTypeOne("x"));
        env.assertPropsNew("s0", fields, new Object[]{"x", null, null, -1, 12.9876d});

        env.sendEventBean(new SupportBeanDuckTypeTwo(-10));
        env.assertPropsNew("s0", fields, new Object[]{null, -10, "mytext", null, 11.1234d});

        env.undeployAll();
    }
}
