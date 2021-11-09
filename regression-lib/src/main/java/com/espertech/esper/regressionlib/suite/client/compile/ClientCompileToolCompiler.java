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

import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.internal.compiler.abstraction.CompilerAbstractionToolProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class ClientCompileToolCompiler {
    private static final Logger log = LoggerFactory.getLogger(ClientCompileToolCompiler.class);

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompileToolCompilerBasic());
        return execs;
    }

    private static class ClientCompileToolCompilerBasic implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                log.info("Tools compiler is not in classpath");
                return;
            }

            String epl = "select * from SupportBean";
            CompilerArguments args = new CompilerArguments(env.getConfiguration());
            args.getOptions().setCompilerHook(ctx -> new CompilerAbstractionToolProvider(compiler));
            try {
                env.getCompiler().compile(epl, args);
            } catch (EPCompileException e) {
                throw new RuntimeException(e);
            }
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }
}
