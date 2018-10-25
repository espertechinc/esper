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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class EPLOtherIStreamRStreamConfigSelectorIRStream implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String stmtText = "@name('s0') select * from SupportBean#length(3)";
        env.compileDeploy(stmtText).addListener("s0");

        Object eventOld = sendEvent(env, "a");
        sendEvent(env, "b");
        sendEvent(env, "c");
        env.listener("s0").reset();

        Object eventNew = sendEvent(env, "d");
        assertTrue(env.listener("s0").isInvoked());
        assertSame(eventNew, env.listener("s0").getLastNewData()[0].getUnderlying());    // receive 'a' as new data
        assertSame(eventOld, env.listener("s0").getLastOldData()[0].getUnderlying());    // receive 'a' as new data

        env.undeployAll();
    }

    private Object sendEvent(RegressionEnvironment env, String stringValue) {
        SupportBean theEvent = new SupportBean(stringValue, 0);
        env.sendEventBean(theEvent);
        return theEvent;
    }
}
