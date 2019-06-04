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
package com.espertech.esper.regressionlib.suite.client.deploy;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPDeployPreconditionException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

public class ClientDeployPreconditionDependency {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientVisibilityDeployDepScript());
        execs.add(new ClientVisibilityDeployDepVariablePublic());
        execs.add(new ClientVisibilityDeployDepVariablePath());
        execs.add(new ClientVisibilityDeployDepEventTypePublic());
        execs.add(new ClientVisibilityDeployDepEventTypePath());
        execs.add(new ClientVisibilityDeployDepNamedWindow());
        execs.add(new ClientVisibilityDeployDepTable());
        execs.add(new ClientVisibilityDeployDepExprDecl());
        execs.add(new ClientVisibilityDeployDepContext());
        execs.add(new ClientVisibilityDeployDepNamedWindowOfNamedModule());
        execs.add(new ClientVisibilityDeployDepIndex());
        return execs;
    }

    public static class ClientVisibilityDeployDepEventTypePublic implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Configuration configuration = env.runtime().getConfigurationDeepCopy();
            configuration.getCommon().addEventType(SomeEvent.class);

            EPCompiled compiled;
            try {
                compiled = EPCompilerProvider.getCompiler().compile("select * from SomeEvent", new CompilerArguments(configuration));
            } catch (EPCompileException e) {
                throw new RuntimeException(e);
            }

            tryInvalidDeploy(env, compiled, "pre-configured event type 'SomeEvent'");
        }
    }

    public static class ClientVisibilityDeployDepVariablePublic implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Configuration configuration = env.runtime().getConfigurationDeepCopy();
            configuration.getCommon().addVariable("mypublicvariable", String.class, null, true);
            configuration.getCommon().addEventType(SupportBean.class);

            EPCompiled compiled;
            try {
                compiled = EPCompilerProvider.getCompiler().compile("select mypublicvariable from SupportBean", new CompilerArguments(configuration));
            } catch (EPCompileException e) {
                throw new RuntimeException(e);
            }

            tryInvalidDeploy(env, compiled, "pre-configured variable 'mypublicvariable'");
        }
    }

    public static class ClientVisibilityDeployDepVariablePath implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compile("@name('infra') create variable string somevariable = 'a'", path); // Note: not deploying, just adding to path

            String text = "dependency variable 'somevariable'";
            tryInvalidDeploy(env, path, "select somevariable from SupportBean", text);
        }
    }

    public static class ClientVisibilityDeployDepNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compile("@name('infra') create window SimpleWindow#keepall as SupportBean", path); // Note: not deploying, just adding to path

            String text = "dependency named window 'SimpleWindow'";
            tryInvalidDeploy(env, path, "select * from SimpleWindow", text);
        }
    }

    public static class ClientVisibilityDeployDepTable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compile("@name('infra') create table SimpleTable(col1 string)", path); // Note: not deploying, just adding to path

            String text = "dependency table 'SimpleTable'";
            tryInvalidDeploy(env, path, "select * from SimpleTable", text);
        }
    }

    public static class ClientVisibilityDeployDepExprDecl implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compile("@name('infra') create expression someexpression { 0 }", path); // Note: not deploying, just adding to path

            String text = "dependency declared-expression 'someexpression'";
            tryInvalidDeploy(env, path, "select someexpression() from SupportBean", text);
        }
    }

    public static class ClientVisibilityDeployDepScript implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compile("@name('infra') create expression double myscript(stringvalue) [0]", path); // Note: not deploying, just adding to path

            String text = "dependency script 'myscript'";
            tryInvalidDeploy(env, path, "select myscript('abc') from SupportBean", text);
        }
    }

    public static class ClientVisibilityDeployDepContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compile("@name('infra') create context MyContext partition by theString from SupportBean", path); // Note: not deploying, just adding to path

            String text = "dependency context 'MyContext'";
            tryInvalidDeploy(env, path, "context MyContext select * from SupportBean", text);
        }
    }

    public static class ClientVisibilityDeployDepIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String text;

            // Table
            env.compileDeploy("@name('infra') create table MyTable(col1 string primary key, col2 string)", path);
            env.compile("@name('infra') create index MyIndexForTable on MyTable(col2)", path); // Note: not deploying, just adding to path

            text = "dependency index 'MyIndexForTable'";
            tryInvalidDeploy(env, path, "select * from SupportBean as sb, MyTable as mt where mt.col2 = sb.theString", text);
            tryInvalidDeploy(env, path, "select * from SupportBean as sb where exists (select * from MyTable as mt where mt.col2 = sb.theString)", text);

            // Named Window
            env.compileDeploy("@name('infra') create window MyWindow#keepall as SupportBean", path);
            env.compile("@name('infra') create index MyIndexForNW on MyWindow(intPrimitive)", path); // Note: not deploying, just adding to path

            text = "dependency index 'MyIndexForNW'";
            tryInvalidDeploy(env, path, "on SupportBean_S0 as sb update MyWindow as mw set theString='a' where sb.id = mw.intPrimitive", text);

            env.undeployAll();
        }
    }

    public static class ClientVisibilityDeployDepEventTypePath implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compile("@name('infra') create schema MySchema(col1 string)", path); // Note: not deploying, just adding to path

            String text = "dependency event type 'MySchema'";
            tryInvalidDeploy(env, path, "insert into MySchema select 'a' as col1 from SupportBean", text);
        }
    }

    private static class ClientVisibilityDeployDepNamedWindowOfNamedModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            EPCompiled windowABC = env.compile("module ABC; create window MyWindow#keepall as SupportBean", path);
            path.clear();

            env.compile("module DEF; create window MyWindow#keepall as SupportBean", path);
            EPCompiled insertDEF = env.compile("select * from MyWindow", path);
            env.deploy(windowABC);

            tryInvalidDeploy(env, insertDEF, "dependency named window 'MyWindow' module 'DEF'");

            env.undeployAll();
        }
    }

    private static void tryInvalidDeploy(RegressionEnvironment env, RegressionPath path, String epl, String text) {
        EPCompiled compiled = env.compile(epl, path);
        tryInvalidDeploy(env, compiled, text);
    }

    private static void tryInvalidDeploy(RegressionEnvironment env, EPCompiled compiled, String text) {
        String message = "A precondition is not satisfied: Required " + text + " cannot be found";
        try {
            env.runtime().getDeploymentService().deploy(compiled);
            fail();
        } catch (EPDeployPreconditionException ex) {
            if (!message.equals("skip")) {
                SupportMessageAssertUtil.assertMessage(ex.getMessage(), message);
            }
        } catch (EPDeployException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    public static class SomeEvent implements Serializable {
    }
}
