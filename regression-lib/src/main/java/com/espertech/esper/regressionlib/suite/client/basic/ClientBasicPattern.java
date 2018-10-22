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
package com.espertech.esper.regressionlib.suite.client.basic;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertFalse;
import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertTrue;

public class ClientBasicPattern implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        EPCompiled compiled = env.compile("@name('s0') select * from pattern[timer:interval(10)]");

        env.advanceTime(0);

        env.deploy(compiled).addListener("s0").milestone(0);

        env.advanceTime(9999);
        assertFalse(env.listener("s0").getIsInvokedAndReset());

        env.milestone(1);

        env.advanceTime(10000);
        assertTrue(env.listener("s0").getIsInvokedAndReset());

        env.milestone(2);

        env.advanceTime(9999999);
        assertFalse(env.listener("s0").getIsInvokedAndReset());

        env.undeployAll();
    }
}
