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
package com.espertech.esper.regressionlib.suite.client.compile;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBean;

import java.util.ArrayList;
import java.util.List;

public class ClientCompileEnginePath {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompileEnginePathObjectTypes());
        return execs;
    }

    public static class ClientCompileEnginePathObjectTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String deployed = "create variable int myvariable = 10;\n" +
                "create schema MySchema();\n" +
                "create expression myExpr { 'abc' };\n" +
                "create window MyWindow#keepall as SupportBean_S0;\n" +
                "create table MyTable(y string);\n" +
                "create context MyContext start SupportBean_S0 end SupportBean_S1;\n" +
                "create expression myScript() [ 2 ]";
            env.compileDeploy(deployed, new RegressionPath());

            String epl = "@name('s0') select myvariable as c0, myExpr() as c1, myScript() as c2, preconfigured_variable as c3 from SupportBean;\n" +
                "select * from MySchema;" +
                "on SupportBean_S1 delete from MyWindow;\n" +
                "on SupportBean_S1 delete from MyTable;\n" +
                "context MyContext select * from SupportBean;\n";
            compileDeployWEnginePath(env, epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2,c3".split(","),
                new Object[]{10, "abc", 2, 5});

            env.undeployAll();
        }
    }

    private static RegressionEnvironment compileDeployWEnginePath(RegressionEnvironment env, String epl) {
        EPCompiled compiled;
        try {
            compiled = compileWEnginePathAndEmptyConfig(env, epl);
        } catch (EPCompileException ex) {
            throw new RuntimeException(ex);
        }
        env.deploy(compiled);
        return env;
    }

    private static EPCompiled compileWEnginePathAndEmptyConfig(RegressionEnvironment env, String epl) throws EPCompileException {
        CompilerArguments args = new CompilerArguments(new Configuration());
        args.getPath().add(env.runtime().getRuntimePath());
        return EPCompilerProvider.getCompiler().compile(epl, args);
    }
}
