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
package com.espertech.esper.regressionlib.suite.resultset.outputlimit;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

public class ResultSetOutputLimitMicrosecondResolution implements RegressionExecution {

    private final long startTime;
    private final String size;
    private final long flipTime;
    private final long repeatTime;

    public ResultSetOutputLimitMicrosecondResolution(long startTime, String size, long flipTime, long repeatTime) {
        this.startTime = startTime;
        this.size = size;
        this.flipTime = flipTime;
        this.repeatTime = repeatTime;
    }

    public void run(RegressionEnvironment env) {
        env.advanceTime(startTime);
        String epl = "@name('s0') select * from SupportBean output every " + size + " seconds";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 10));
        env.advanceTime(flipTime - 1);
        env.assertListenerNotInvoked("s0");

        env.advanceTime(flipTime);
        env.assertListenerInvoked("s0");

        env.sendEventBean(new SupportBean("E2", 10));
        env.advanceTime(repeatTime + flipTime - 1);
        env.assertListenerNotInvoked("s0");

        env.advanceTime(repeatTime + flipTime);
        env.assertListenerInvoked("s0");

        env.undeployAll();
    }
}
