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
package com.espertech.esper.regressionlib.suite.expr.define;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST1;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;

import static org.junit.Assert.assertEquals;

public class ExprDefineConfigurations implements RegressionExecution {

    private final int expectedInvocationCount;

    public ExprDefineConfigurations(int expectedInvocationCount) {
        this.expectedInvocationCount = expectedInvocationCount;
    }

    public void run(RegressionEnvironment env) {
        env.compileDeploy("@name('s0') expression myExpr {v => alwaysTrue(null) } " +
            "select myExpr(st0) as c0, myExpr(st1) as c1, myExpr(st0) as c2, myExpr(st1) as c3 from SupportBean_ST0#lastevent as st0, SupportBean_ST1#lastevent as st1");
        env.addListener("s0");

        // send event and assert
        SupportStaticMethodLib.getInvocations().clear();
        env.sendEventBean(new SupportBean_ST0("a", 0));
        env.sendEventBean(new SupportBean_ST1("a", 0));
        assertEquals(expectedInvocationCount, SupportStaticMethodLib.getInvocations().size());

        env.undeployAll();
    }
}
