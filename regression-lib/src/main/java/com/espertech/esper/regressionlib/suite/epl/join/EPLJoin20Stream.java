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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean_S0;

import java.io.StringWriter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EPLJoin20Stream implements RegressionExecution {
    public void run(RegressionEnvironment env) {

        StringWriter buf = new StringWriter();
        buf.append("@name('s0') select * from ");

        String delimiter = "";
        for (int i = 0; i < 20; i++) {
            buf.append(delimiter);
            buf.append("SupportBean_S0(id=" + i + ")#lastevent as s_" + i);
            delimiter = ", ";
        }
        env.compileDeployAddListenerMileZero(buf.toString(), "s0");

        for (int i = 0; i < 19; i++) {
            env.sendEventBean(new SupportBean_S0(i));
        }
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(new SupportBean_S0(19));
        assertTrue(env.listener("s0").isInvoked());

        env.undeployAll();
    }
}