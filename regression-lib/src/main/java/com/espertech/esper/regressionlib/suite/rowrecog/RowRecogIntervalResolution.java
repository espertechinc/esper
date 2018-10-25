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
package com.espertech.esper.regressionlib.suite.rowrecog;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RowRecogIntervalResolution implements RegressionExecution {

    private final long flipTime;

    public RowRecogIntervalResolution(long flipTime) {
        this.flipTime = flipTime;
    }

    public void run(RegressionEnvironment env) {
        env.advanceTime(0);

        String text = "@name('s0') select * from SupportBean " +
            "match_recognize (" +
            " measures A as a" +
            " pattern (A*)" +
            " interval 10 seconds" +
            ")";
        env.compileDeploy(text).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 1));

        env.advanceTime(flipTime - 1);
        assertFalse(env.listener("s0").getIsInvokedAndReset());

        env.milestone(0);

        env.advanceTime(flipTime);
        assertTrue(env.listener("s0").getIsInvokedAndReset());

        env.undeployAll();
    }
}
