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
package com.espertech.esper.regressionrun.suite.infra;

import com.espertech.esper.common.client.util.Locking;
import com.espertech.esper.regressionlib.suite.infra.namedwindow.InfraNamedWindowOnUpdateWMultiDispatch;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

// see INFRA suite for additional Named Window tests
public class TestSuiteInfraNamedWindowWConfig extends TestCase {

    public void testInfraNamedWindowOnUpdateWMultiDispatch() {
        runAssertion(true, null, null);
        runAssertion(false, true, Locking.SPIN);
        runAssertion(false, true, Locking.SUSPEND);
        runAssertion(false, false, null);
    }

    private void runAssertion(boolean useDefault, Boolean preserve, Locking locking) {
        RegressionSession session = RegressionRunner.session();
        if (!useDefault) {
            session.getConfiguration().getRuntime().getThreading().setNamedWindowConsumerDispatchPreserveOrder(preserve);
            session.getConfiguration().getRuntime().getThreading().setNamedWindowConsumerDispatchLocking(locking);
        }

        InfraNamedWindowOnUpdateWMultiDispatch exec = new InfraNamedWindowOnUpdateWMultiDispatch();
        RegressionRunner.run(session, exec);
        session.destroy();
    }
}
