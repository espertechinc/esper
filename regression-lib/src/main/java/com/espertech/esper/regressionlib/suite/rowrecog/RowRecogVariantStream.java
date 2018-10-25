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
package com.espertech.esper.regressionlib.suite.rowrecog;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;

public class RowRecogVariantStream implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create variant schema MyVariantType as SupportBean_S0, SupportBean_S1", path);

        String[] fields = "a,b".split(",");
        String text = "@name('s0') select * from MyVariantType#keepall " +
            "match_recognize (" +
            "  measures A.id? as a, B.id? as b" +
            "  pattern (A B) " +
            "  define " +
            "    A as typeof(A) = 'SupportBean_S0'," +
            "    B as typeof(B) = 'SupportBean_S1'" +
            ")";

        env.compileDeploy(text, path).addListener("s0");
        env.compileDeploy("insert into MyVariantType select * from SupportBean_S0", path);
        env.compileDeploy("insert into MyVariantType select * from SupportBean_S1", path);

        env.sendEventBean(new SupportBean_S0(1, "S0"));
        env.sendEventBean(new SupportBean_S1(2, "S1"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{1, 2}});
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
            new Object[][]{{1, 2}});

        String epl = "// Declare one sample type\n" +
            "create schema ST0 as (col string)\n;" +
            "// Declare second sample type\n" +
            "create schema ST1 as (col string)\n;" +
            "// Declare variant stream holding either type\n" +
            "create variant schema MyVariantStream as ST0, ST1\n;" +
            "// Populate variant stream\n" +
            "insert into MyVariantStream select * from ST0\n;" +
            "// Populate variant stream\n" +
            "insert into MyVariantStream select * from ST1\n;" +
            "// Simple pattern to match ST0 ST1 pairs\n" +
            "select * from MyVariantType#time(1 min)\n" +
            "match_recognize (\n" +
            "measures A.id? as a, B.id? as b\n" +
            "pattern (A B)\n" +
            "define\n" +
            "A as typeof(A) = 'ST0',\n" +
            "B as typeof(B) = 'ST1'\n" +
            ");";
        env.compileDeploy(epl, path);
        env.undeployAll();
    }
}