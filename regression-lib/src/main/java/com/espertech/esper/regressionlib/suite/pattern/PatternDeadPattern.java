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
package com.espertech.esper.regressionlib.suite.pattern;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_C;

import static org.junit.Assert.assertTrue;

public class PatternDeadPattern implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String pattern = "select * from pattern[(SupportBean_A -> SupportBean_B) and not SupportBean_C]";
        // Adjust to 20000 to better test the limit
        EPCompiled compiled = env.compile(pattern);
        for (int i = 0; i < 1000; i++) {
            env.deploy(compiled);
        }

        env.sendEventBean(new SupportBean_C("C1"));

        long startTime = System.currentTimeMillis();
        env.sendEventBean(new SupportBean_A("A1"));
        long delta = System.currentTimeMillis() - startTime;
        assertTrue("performance: delta=" + delta, delta < 20);

        env.undeployAll();
    }
}
