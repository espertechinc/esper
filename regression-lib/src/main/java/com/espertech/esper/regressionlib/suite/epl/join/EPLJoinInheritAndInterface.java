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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.ISupportAImpl;
import com.espertech.esper.regressionlib.support.bean.ISupportBImpl;

import static org.junit.Assert.*;

public class EPLJoinInheritAndInterface implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String epl = "@name('s0') select a, b from ISupportA#length(10), ISupportB#length(10) where a = b";
        env.compileDeployAddListenerMileZero(epl, "s0");

        env.sendEventBean(new ISupportAImpl("1", "ab1"));
        env.sendEventBean(new ISupportBImpl("2", "ab2"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new ISupportBImpl("1", "ab3"));
        assertTrue(env.listener("s0").isInvoked());
        EventBean theEvent = env.listener("s0").getAndResetLastNewData()[0];
        assertEquals("1", theEvent.get("a"));
        assertEquals("1", theEvent.get("b"));

        env.undeployAll();
    }
}
