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
package com.espertech.esper.regressionlib.suite.event.bean;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import static org.junit.Assert.assertEquals;

public class EventBeanPropertyResolutionCaseInsensitiveEngineDefault implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        tryCaseInsensitive(env, "BeanWCIED", "@name('s0') select THESTRING, INTPRIMITIVE from BeanWCIED where THESTRING='A'", "THESTRING", "INTPRIMITIVE");
        tryCaseInsensitive(env, "BeanWCIED", "@name('s0') select ThEsTrInG, INTprimitIVE from BeanWCIED where THESTRing='A'", "ThEsTrInG", "INTprimitIVE");
    }

    protected static void tryCaseInsensitive(RegressionEnvironment env, String eventTypeName, String stmtText, String propOneName, String propTwoName) {
        env.compileDeploy(stmtText).addListener("s0");

        env.sendEventBean(new SupportBean("A", 10), eventTypeName);
        EventBean result = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("A", result.get(propOneName));
        assertEquals(10, result.get(propTwoName));

        env.undeployAll();
    }

}
