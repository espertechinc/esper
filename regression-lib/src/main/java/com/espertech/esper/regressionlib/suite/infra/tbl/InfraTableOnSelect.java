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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableOnSelect implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create table varagg as (" +
            "key string primary key, total sum(int))", path);
        env.compileDeploy("into table varagg " +
            "select sum(intPrimitive) as total from SupportBean group by theString", path);
        env.compileDeploy("@name('s0') on SupportBean_S0 select total as value from varagg where key = p00", path).addListener("s0");

        assertValues(env, "G1,G2", new Integer[]{null, null});

        env.sendEventBean(new SupportBean("G1", 100));
        assertValues(env, "G1,G2", new Integer[]{100, null});

        env.milestone(0);

        env.sendEventBean(new SupportBean("G2", 200));
        assertValues(env, "G1,G2", new Integer[]{100, 200});

        env.compileDeploy("@name('i1') on SupportBean_S1 select total from varagg where key = p10", path).addListener("i1");

        env.sendEventBean(new SupportBean("G2", 300));

        env.milestone(1);

        env.sendEventBean(new SupportBean_S1(0, "G2"));
        assertEquals(500, env.listener("i1").assertOneGetNewAndReset().get("total"));

        env.undeployAll();
    }

    private static void assertValues(RegressionEnvironment env, String keys, Integer[] values) {
        String[] keyarr = keys.split(",");
        for (int i = 0; i < keyarr.length; i++) {
            env.sendEventBean(new SupportBean_S0(0, keyarr[i]));
            if (values[i] == null) {
                assertFalse(env.listener("s0").isInvoked());
            } else {
                EventBean event = env.listener("s0").assertOneGetNewAndReset();
                assertEquals("Failed for key '" + keyarr[i] + "'", values[i], event.get("value"));
            }
        }
    }
}
