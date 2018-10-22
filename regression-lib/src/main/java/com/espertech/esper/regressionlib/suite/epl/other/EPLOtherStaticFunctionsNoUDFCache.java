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
import com.espertech.esper.regressionlib.support.bean.SupportTemperatureBean;

import static junit.framework.TestCase.assertTrue;

public class EPLOtherStaticFunctionsNoUDFCache implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String text = "@name('s0') select SupportStaticMethodLib.sleep(100) as val from SupportTemperatureBean as temp";
        env.compileDeploy(text).addListener("s0");

        long startTime = System.currentTimeMillis();
        env.sendEventBean(new SupportTemperatureBean("a"));
        env.sendEventBean(new SupportTemperatureBean("a"));
        env.sendEventBean(new SupportTemperatureBean("a"));
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta > 120);
        env.undeployAll();

        // test plug-in single-row function
        String textSingleRow = "@name('s0') select sleepme(100) as val from SupportTemperatureBean as temp";
        env.compileDeploy(textSingleRow).addListener("s0");

        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            env.sendEventBean(new SupportTemperatureBean("a"));
        }
        delta = System.currentTimeMillis() - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1000);
        env.undeployAll();
    }
}
