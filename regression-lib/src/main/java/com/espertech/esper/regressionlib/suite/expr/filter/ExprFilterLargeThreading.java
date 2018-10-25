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
package com.espertech.esper.regressionlib.suite.expr.filter;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportTradeEvent;
import org.junit.Assert;

import static org.junit.Assert.assertFalse;

public class ExprFilterLargeThreading implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        runAssertionNoVariable(env);
    }

    private void runAssertionNoVariable(RegressionEnvironment env) {
        String epl = "@name('s0') select * from pattern[a=SupportBean -> every event1=SupportTradeEvent(userId like '123%')]";
        env.compileDeploy(epl).addListener("s0").milestone(0);
        env.sendEventBean(new SupportBean());

        env.sendEventBean(new SupportTradeEvent(1, null, 1001));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportTradeEvent(2, "1234", 1001));
        Assert.assertEquals(2, env.listener("s0").assertOneGetNewAndReset().get("event1.id"));

        env.undeployAll();
    }
}
