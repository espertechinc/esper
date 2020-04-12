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
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

public class ExprCoreConcat implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String[] fields = "c1,c2,c3".split(",");
        SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_S0")
            .expression(fields[0], "p00 || p01")
            .expression(fields[1], "p00 || p01 || p02")
            .expression(fields[2], "p00 || '|' || p01");

        builder.assertion(new SupportBean_S0(1, "a", "b", "c")).expect(fields, "ab", "abc", "a|b");
        builder.assertion(new SupportBean_S0(1, null, "b", "c")).expect(fields, null, null, null);
        builder.assertion(new SupportBean_S0(1, "", "b", "c")).expect(fields, "b", "bc", "|b");
        builder.assertion(new SupportBean_S0(1, "123", null, "c")).expect(fields, null, null, null);
        builder.assertion(new SupportBean_S0(1, "123", "456", "c")).expect(fields, "123456", "123456c", "123|456");
        builder.assertion(new SupportBean_S0(1, "123", "456", null)).expect(fields, "123456", null, "123|456");

        builder.run(env);
        env.undeployAll();
    }
}
