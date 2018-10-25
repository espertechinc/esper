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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import static org.junit.Assert.assertFalse;

public class RowRecogEnumMethod implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String[] fields = "c0,c1".split(",");
        String epl = "@name('s0') select * from SupportBean match_recognize ("
            + "partition by theString "
            + "measures A.theString as c0, C.intPrimitive as c1 "
            + "pattern (A B+ C) "
            + "define "
            + "B as B.intPrimitive > A.intPrimitive, "
            + "C as C.doublePrimitive > B.firstOf().intPrimitive)";
        // can also be expressed as: B[0].intPrimitive
        env.compileDeploy(epl).addListener("s0");

        sendEvent(env, "E1", 10, 0);
        sendEvent(env, "E1", 11, 50);
        sendEvent(env, "E1", 12, 11);
        assertFalse(env.listener("s0").isInvoked());

        env.milestone(0);

        sendEvent(env, "E2", 10, 0);
        sendEvent(env, "E2", 11, 50);

        env.milestone(1);

        sendEvent(env, "E2", 12, 12);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 12});

        env.undeployAll();
    }

    private void sendEvent(RegressionEnvironment env, String theString, int intPrimitive, double doublePrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setDoublePrimitive(doublePrimitive);
        env.sendEventBean(bean);
    }
}