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
package com.espertech.esper.regressionlib.suite.client.extension;

import com.espertech.esper.common.internal.epl.pattern.guard.GuardForge;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClientExtendPatternGuard implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        runAssertionGuard(env);
        runAssertionGuardVariable(env);
        runAssertionInvalid(env);
    }

    private void runAssertionGuard(RegressionEnvironment env) {
        if (env.isHA()) {
            return;
        }
        String stmtText = "@name('s0') select * from pattern [(every SupportBean) where myplugin:count_to(10)]";
        env.compileDeploy(stmtText).addListener("s0");

        for (int i = 0; i < 10; i++) {
            env.sendEventBean(new SupportBean());
            assertTrue(env.listener("s0").getAndClearIsInvoked());
        }

        env.sendEventBean(new SupportBean());
        assertFalse(env.listener("s0").isInvoked());

        env.undeployAll();
    }

    private void runAssertionGuardVariable(RegressionEnvironment env) {
        if (env.isHA()) {
            return;
        }
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create variable int COUNT_TO = 3", path);
        String stmtText = "@name('s0') select * from pattern [(every SupportBean) where myplugin:count_to(COUNT_TO)]";
        env.compileDeploy(stmtText, path).addListener("s0");

        for (int i = 0; i < 3; i++) {
            env.sendEventBean(new SupportBean());
            assertTrue(env.listener("s0").getAndClearIsInvoked());
        }

        env.sendEventBean(new SupportBean());
        assertFalse(env.listener("s0").isInvoked());

        env.undeployAll();
    }

    private void runAssertionInvalid(RegressionEnvironment env) {
        tryInvalidCompile(env, "select * from pattern [every SupportBean where namespace:name(10)]",
            "Failed to resolve pattern guard 'SupportBean where namespace:name(10)': Error casting guard forge instance to " + GuardForge.class.getName() + " interface for guard 'name'");
    }
}
