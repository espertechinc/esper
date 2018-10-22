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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.EPSubscriberException;
import com.espertech.esper.runtime.client.scopetest.SupportSubscriberMRD;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.assertMessage;
import static org.junit.Assert.fail;

public class ClientRuntimeSubscriberDisallowed implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        env.compileDeploy("@name('s0') select * from SupportBean");

        EPStatement stmt = env.statement("s0");
        tryInvalid(() -> stmt.setSubscriber(new SupportSubscriberMRD()));
        tryInvalid(() -> stmt.setSubscriber(new SupportSubscriberMRD(), "update"));

        env.undeployAll();
    }

    private static void tryInvalid(Runnable r) {
        try {
            r.run();
            fail();
        } catch (EPSubscriberException ex) {
            assertMessage(ex, "Setting a subscriber is not allowed for the statement, the statement has been compiled with allowSubscriber=false");
        }
    }
}
