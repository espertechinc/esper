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
package com.espertech.esper.regressionlib.suite.epl.variable;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EPLVariableEngineConfigXML implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String stmtTextSet = "@name('set') on SupportBean set p_1 = theString, p_2 = boolBoxed, p_3 = intBoxed, p_4 = intBoxed";
        env.compileDeploy(stmtTextSet).addListener("set");
        String[] fieldsVar = new String[]{"p_1", "p_2", "p_3", "p_4"};
        EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{null, true, 10L, 11.1d}});

        EventType typeSet = env.statement("set").getEventType();
        assertEquals(String.class, typeSet.getPropertyType("p_1"));
        assertEquals(Boolean.class, typeSet.getPropertyType("p_2"));
        assertEquals(Long.class, typeSet.getPropertyType("p_3"));
        assertEquals(Double.class, typeSet.getPropertyType("p_4"));
        Arrays.sort(typeSet.getPropertyNames());
        assertTrue(Arrays.equals(typeSet.getPropertyNames(), fieldsVar));

        SupportBean bean = new SupportBean();
        bean.setTheString("text");
        bean.setBoolBoxed(false);
        bean.setIntBoxed(200);
        env.sendEventBean(bean);
        EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{"text", false, 200L, 200d});
        EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{"text", false, 200L, 200d}});

        bean = new SupportBean();   // leave all fields null
        env.sendEventBean(bean);
        EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{null, null, null, null});
        EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{null, null, null, null}});

        env.undeployAll();
    }
}
