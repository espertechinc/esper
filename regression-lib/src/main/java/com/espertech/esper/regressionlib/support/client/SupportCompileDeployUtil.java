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
package com.espertech.esper.regressionlib.support.client;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.CompilerOptions;
import com.espertech.esper.compiler.client.CompilerPath;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.runtime.client.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class SupportCompileDeployUtil {
    public static void threadSleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void threadpoolAwait(ExecutorService threadPool, int num, TimeUnit unit) {
        try {
            threadPool.awaitTermination(num, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertFutures(Future<Boolean>[] futures) {
        try {
            for (Future future : futures) {
                assertEquals(true, future.get());
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static EPDeployment compileDeploy(EPRuntime runtime, String epl) {
        try {
            Configuration configuration = runtime.getConfigurationDeepCopy();
            CompilerArguments args = new CompilerArguments(configuration);
            args.getPath().add(runtime.getRuntimePath());
            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, args);
            return runtime.getDeploymentService().deploy(compiled);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static EPDeployment compileDeploy(String epl, EPRuntime runtime, Configuration configuration) {
        EPCompiled compiled = compile(epl, configuration, new RegressionPath());
        try {
            return runtime.getDeploymentService().deploy(compiled);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static EPCompiled compile(String epl, Configuration configuration, RegressionPath path) {
        try {
            return EPCompilerProvider.getCompiler().compile(epl, new CompilerArguments(configuration).setPath(new CompilerPath().addAll(path.getCompileds())).setOptions(new CompilerOptions().setAccessModifierContext(ctx -> NameAccessModifier.PUBLIC).setAccessModifierEventType(ctx -> NameAccessModifier.PUBLIC)));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static EPDeployment deploy(EPCompiled compiledStmt, EPRuntime runtime) {
        try {
            return runtime.getDeploymentService().deploy(compiledStmt);
        } catch (EPDeployException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deployAddListener(EPCompiled compiledStmt, String stmtName, UpdateListener listener, EPRuntime runtime) {
        try {
            EPDeployment deployed = runtime.getDeploymentService().deploy(compiledStmt, new DeploymentOptions().setStatementNameRuntime(ctx -> stmtName));
            if (deployed.getStatements().length != 1) {
                throw new UnsupportedOperationException("This method is designed for a single statement");
            }
            deployed.getStatements()[0].addListener(listener);
        } catch (EPDeployException e) {
            throw new RuntimeException(e);
        }
    }

    public static void compileDeployAddListener(String epl, String stmtName, UpdateListener listener, EPRuntime runtime, Configuration configuration) {
        EPCompiled compiled = compile(epl, configuration, new RegressionPath());
        deployAddListener(compiled, stmtName, listener, runtime);
    }

    public static void threadJoin(Thread t) {
        try {
            t.join();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
