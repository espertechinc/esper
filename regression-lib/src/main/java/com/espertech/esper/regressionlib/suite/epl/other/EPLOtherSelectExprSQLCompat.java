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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EPLOtherSelectExprSQLCompat {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherProperty());
        return execs;
    }

    private static class EPLOtherProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select SupportBean.theString as val1, SupportBean.intPrimitive as val2 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "E1", 10);
            env.assertEventNew("s0", received -> {
                assertEquals("E1", received.get("val1"));
                assertEquals(10, received.get("val2"));
            });

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, String s, int intPrimitive) {
        SupportBean bean = new SupportBean(s, intPrimitive);
        env.sendEventBean(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(EPLOtherSelectExprSQLCompat.class);
}
