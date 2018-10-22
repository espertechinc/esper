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
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.option.StatementNameContext;
import com.espertech.esper.compiler.client.option.StatementNameOption;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ClientCompileStatementName {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompileStatementNameResolve());
        return execs;
    }

    private static class ClientCompileStatementNameResolve implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            MyStatementNameResolver.getContexts().clear();
            CompilerArguments args = new CompilerArguments(env.getConfiguration());
            args.getOptions().setStatementName(new MyStatementNameResolver());
            String epl = "select * from SupportBean";
            EPCompiled compiled = env.compile(epl, args);

            StatementNameContext ctx = MyStatementNameResolver.getContexts().get(0);
            assertEquals(epl, ctx.getEplSupplier().get());
            assertEquals(null, ctx.getStatementName());
            assertEquals(null, ctx.getModuleName());
            assertEquals(0, ctx.getAnnotations().length);
            assertEquals(0, ctx.getStatementNumber());

            env.deploy(compiled);
            assertEquals("hello", env.statement("hello").getName());
            env.undeployAll();
        }
    }

    private static class MyStatementNameResolver implements StatementNameOption {
        private static List<StatementNameContext> contexts = new ArrayList<>();

        public static List<StatementNameContext> getContexts() {
            return contexts;
        }

        public String getValue(StatementNameContext env) {
            contexts.add(env);
            return "hello";
        }
    }
}
