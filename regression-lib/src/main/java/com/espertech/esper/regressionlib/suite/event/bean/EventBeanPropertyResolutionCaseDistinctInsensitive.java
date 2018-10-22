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
import com.espertech.esper.regressionlib.support.bean.SupportBeanDupProperty;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventBeanPropertyResolutionCaseDistinctInsensitive implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        env.compileDeploy("@name('s0') select MYPROPERTY, myproperty, myProperty from SupportBeanDupProperty");
        env.addListener("s0");

        env.sendEventBean(new SupportBeanDupProperty("lowercamel", "uppercamel", "upper", "lower"));
        EventBean result = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("upper", result.get("MYPROPERTY"));
        assertEquals("lower", result.get("myproperty"));
        assertTrue(result.get("myProperty").equals("lowercamel") || result.get("myProperty").equals("uppercamel")); // JDK6 versus JDK7 JavaBean inspector

        tryInvalidCompile(env, "select MyProperty from SupportBeanDupProperty",
            "Unable to determine which property to use for \"MyProperty\" because more than one property matched [");

        env.undeployAll();
    }
}
