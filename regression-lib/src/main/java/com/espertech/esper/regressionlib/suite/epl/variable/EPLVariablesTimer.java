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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import static org.junit.Assert.*;

public class EPLVariablesTimer implements RegressionExecution {
    public void run(RegressionEnvironment env) {

        long startTime = System.currentTimeMillis();
        String stmtTextSet = "@name('s0') on pattern [every timer:interval(100 milliseconds)] set var1 = current_timestamp, var2 = var1 + 1, var3 = var1 + var2";
        env.compileDeploy(stmtTextSet).addListener("s0");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        EventBean[] received = env.listener("s0").getNewDataListFlattened();
        assertTrue("received : " + received.length, received.length >= 5);

        for (int i = 0; i < received.length; i++) {
            long var1 = (Long) received[i].get("var1");
            long var2 = (Long) received[i].get("var2");
            long var3 = (Long) received[i].get("var3");
            assertTrue(var1 >= startTime);
            assertEquals(var1, var2 - 1);
            assertEquals(var3, var2 + var1);
        }

        env.undeployAll();
    }
}
