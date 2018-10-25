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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RowRecogClausePresence implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        runAssertionMeasurePresence(env, 0, "B.size()", 1);
        runAssertionMeasurePresence(env, 0, "100+B.size()", 101);
        runAssertionMeasurePresence(env, 1000000, "B.anyOf(v=>theString='E2')", true);

        runAssertionDefineNotPresent(env, true);
        runAssertionDefineNotPresent(env, false);
    }

    private void runAssertionDefineNotPresent(RegressionEnvironment env, boolean soda) {

        String epl = "@name('s0') select * from SupportBean " +
            "match_recognize (" +
            " measures A as a, B as b" +
            " pattern (A B)" +
            ")";
        env.compileDeploy(soda, epl).addListener("s0");

        String[] fields = "a,b".split(",");
        SupportBean[] beans = new SupportBean[4];
        for (int i = 0; i < beans.length; i++) {
            beans[i] = new SupportBean("E" + i, i);
        }

        env.sendEventBean(beans[0]);
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(beans[1]);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{beans[0], beans[1]});

        env.sendEventBean(beans[2]);
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(beans[3]);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{beans[2], beans[3]});

        env.undeployAll();
    }

    private void runAssertionMeasurePresence(RegressionEnvironment env, long baseTime, String select, Object value) {

        env.advanceTime(baseTime);
        String epl = "@name('s0') select * from SupportBean  " +
            "match_recognize (" +
            "    measures A as a, A.theString as id, " + select + " as val " +
            "    pattern (A B*) " +
            "    interval 1 minute " +
            "    define " +
            "        A as (A.intPrimitive=1)," +
            "        B as (B.intPrimitive=2))";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 1));
        env.sendEventBean(new SupportBean("E2", 2));

        env.advanceTimeSpan(baseTime + 60 * 1000 * 2);
        assertEquals(value, env.listener("s0").getNewDataListFlattened()[0].get("val"));

        env.undeployAll();
    }
}