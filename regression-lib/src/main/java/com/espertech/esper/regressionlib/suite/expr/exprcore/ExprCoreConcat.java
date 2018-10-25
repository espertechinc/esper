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
package com.espertech.esper.regressionlib.suite.expr.exprcore;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import org.junit.Assert;

public class ExprCoreConcat implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String epl = "@name('s0') select p00 || p01 as c1, p00 || p01 || p02 as c2, p00 || '|' || p01 as c3 from SupportBean_S0";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean_S0(1, "a", "b", "c"));
        assertConcat(env, "ab", "abc", "a|b");

        env.sendEventBean(new SupportBean_S0(1, null, "b", "c"));
        assertConcat(env, null, null, null);

        env.sendEventBean(new SupportBean_S0(1, "", "b", "c"));
        assertConcat(env, "b", "bc", "|b");

        env.sendEventBean(new SupportBean_S0(1, "123", null, "c"));
        assertConcat(env, null, null, null);

        env.sendEventBean(new SupportBean_S0(1, "123", "456", "c"));
        assertConcat(env, "123456", "123456c", "123|456");

        env.sendEventBean(new SupportBean_S0(1, "123", "456", null));
        assertConcat(env, "123456", null, "123|456");

        env.undeployAll();
    }

    private void assertConcat(RegressionEnvironment env, String c1, String c2, String c3) {
        EventBean theEvent = env.listener("s0").getLastNewData()[0];
        Assert.assertEquals(c1, theEvent.get("c1"));
        Assert.assertEquals(c2, theEvent.get("c2"));
        Assert.assertEquals(c3, theEvent.get("c3"));
        env.listener("s0").reset();
    }
}
