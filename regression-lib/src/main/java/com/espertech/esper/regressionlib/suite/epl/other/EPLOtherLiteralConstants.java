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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

public class EPLOtherLiteralConstants implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String statement = "@name('s0') select 0x23 as mybyte, " +
            "'\u0041' as myunicode," +
            "08 as zero8, " +
            "09 as zero9, " +
            "008 as zeroZero8 " +
            "from SupportBean";
        env.compileDeploy(statement).addListener("s0");

        env.sendEventBean(new SupportBean("e1", 100));

        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(),
            "mybyte,myunicode,zero8,zero9,zeroZero8".split(","),
            new Object[]{(byte) 35, "A", 8, 9, 8});

        env.undeployAll();
    }
}
