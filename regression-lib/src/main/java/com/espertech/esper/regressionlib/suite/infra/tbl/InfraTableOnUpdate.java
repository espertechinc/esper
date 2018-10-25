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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportTwoKeyEvent;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableOnUpdate implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String[] fields = "keyOne,keyTwo,p0".split(",");
        RegressionPath path = new RegressionPath();

        env.compileDeploy("create table varagg as (" +
            "keyOne string primary key, keyTwo int primary key, p0 long)", path);
        env.compileDeploy("on SupportBean merge varagg where theString = keyOne and " +
            "intPrimitive = keyTwo when not matched then insert select theString as keyOne, intPrimitive as keyTwo, 1 as p0", path);
        env.compileDeploy("@name('s0') select varagg[p00, id].p0 as value from SupportBean_S0", path).addListener("s0");
        env.compileDeploy("@name('update') on SupportTwoKeyEvent update varagg set p0 = newValue " +
            "where k1 = keyOne and k2 = keyTwo", path).addListener("update");

        Object[][] expectedType = new Object[][]{{"keyOne", String.class}, {"keyTwo", Integer.class}, {"p0", Long.class}};
        EventType updateStmtEventType = env.statement("update").getEventType();
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, updateStmtEventType, SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        env.sendEventBean(new SupportBean("G1", 10));
        assertValues(env, new Object[][]{{"G1", 10}}, new Long[]{1L});

        env.milestone(0);

        env.sendEventBean(new SupportTwoKeyEvent("G1", 10, 2));
        assertValues(env, new Object[][]{{"G1", 10}}, new Long[]{2L});
        EPAssertionUtil.assertProps(env.listener("update").getLastNewData()[0], fields, new Object[]{"G1", 10, 2L});
        EPAssertionUtil.assertProps(env.listener("update").getAndResetLastOldData()[0], fields, new Object[]{"G1", 10, 1L});

        // try property method invocation
        env.compileDeploy("create table MyTableSuppBean as (sb SupportBean)", path);
        env.compileDeploy("on SupportBean_S0 update MyTableSuppBean sb set sb.setLongPrimitive(10)", path);
        env.undeployAll();
    }

    private static void assertValues(RegressionEnvironment env, Object[][] keys, Long[] values) {
        assertEquals(keys.length, values.length);
        for (int i = 0; i < keys.length; i++) {
            env.sendEventBean(new SupportBean_S0((Integer) keys[i][1], (String) keys[i][0]));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("Failed for key '" + Arrays.toString(keys[i]) + "'", values[i], event.get("value"));
        }
    }

}
