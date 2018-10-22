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
package com.espertech.esper.regressionlib.suite.infra.nwtable;

import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;

public class InfraNWTableCreateIndexAdvancedSyntax implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        assertCompileSODA(env, "create index MyIndex on MyWindow((x,y) dummy_name(\"a\",10101))");
        assertCompileSODA(env, "create index MyIndex on MyWindow(x dummy_name)");
        assertCompileSODA(env, "create index MyIndex on MyWindow((x,y,z) dummy_name)");
        assertCompileSODA(env, "create index MyIndex on MyWindow(x dummy_name, (y,z) dummy_name_2(\"a\"), p dummyname3)");

        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window MyWindow#keepall as SupportSpatialPoint", path);

        tryInvalidCompile(env, path, "create index MyIndex on MyWindow(())",
            "Invalid empty list of index expressions");

        tryInvalidCompile(env, path, "create index MyIndex on MyWindow(intPrimitive+1)",
            "Invalid index expression 'intPrimitive+1'");

        tryInvalidCompile(env, path, "create index MyIndex on MyWindow((x, y))",
            "Invalid multiple index expressions");

        tryInvalidCompile(env, path, "create index MyIndex on MyWindow(x.y)",
            "Invalid index expression 'x.y'");

        tryInvalidCompile(env, path, "create index MyIndex on MyWindow(id xxxx)",
            "Unrecognized advanced-type index 'xxxx'");

        env.undeployAll();
    }

    private static void assertCompileSODA(RegressionEnvironment env, String epl) {
        EPStatementObjectModel model = env.eplToModel(epl);
        assertEquals(epl, model.toEPL());
    }
}
