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

import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ZoneFox {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new StartsWith());
        executions.add(new RegexSlashU());
        return executions;
    }

    private static class StartsWith implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select p00 as result from SupportBean_S0 where p00.startsWith('\\user\\bob')";
            String expected = "\\user\\bob";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(-1, expected));
            String actual = (String) env.listener("s0").assertOneGetNewAndReset().get("result");
            assertEquals(actual, expected);
            env.undeployAll();
        }
    }

    private static class RegexSlashU implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select p00 regexp '.*\\\\user\\\\.*' as result from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean_S0(-1, "\\user\\bob"));
            assertTrue((Boolean) env.listener("s0").assertOneGetNewAndReset().get("result"));
            env.undeployAll();
        }
    }
}
