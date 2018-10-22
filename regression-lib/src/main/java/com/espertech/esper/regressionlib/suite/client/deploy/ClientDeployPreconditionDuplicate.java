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
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.util.StringValue;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPDeployPreconditionException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

public class ClientDeployPreconditionDuplicate {
    private static final String MODULE_NAME_UNNAMED = StringValue.UNNAMED;

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientDeployPrecondDupNamedWindow());
        execs.add(new ClientDeployPrecondDupTable());
        execs.add(new ClientDeployPrecondDupEventType());
        execs.add(new ClientDeployPrecondDupVariable());
        execs.add(new ClientDeployPrecondDupExprDecl());
        execs.add(new ClientDeployPrecondDupScript());
        execs.add(new ClientDeployPrecondDupContext());
        execs.add(new ClientDeployPrecondDupIndex());
        return execs;
    }

    public static class ClientDeployPrecondDupNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window SimpleWindow#keepall as SupportBean";
            env.compileDeploy(epl, path);
            tryInvalidDeploy(env, epl, "A named window by name 'SimpleWindow'", MODULE_NAME_UNNAMED);
            env.undeployAll();
            path.clear();

            epl = "module ABC; create window SimpleWindow#keepall as SupportBean";
            env.compileDeploy(epl, path);
            tryInvalidDeploy(env, epl, "A named window by name 'SimpleWindow'", "ABC");

            env.undeployAll();
        }
    }

    public static class ClientDeployPrecondDupTable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create table SimpleTable(col1 string)";
            env.compileDeploy(epl, path);
            tryInvalidDeploy(env, epl, "A table by name 'SimpleTable'", MODULE_NAME_UNNAMED);
            env.undeployAll();
        }
    }

    public static class ClientDeployPrecondDupEventType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create schema MySchema (col1 string)";
            env.compileDeploy(epl, path);
            tryInvalidDeploy(env, epl, "An event type by name 'MySchema'", MODULE_NAME_UNNAMED);
            env.undeployAll();
        }
    }

    public static class ClientDeployPrecondDupVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create variable string myvariable";
            env.compileDeploy(epl, path);
            tryInvalidDeploy(env, epl, "A variable by name 'myvariable'", MODULE_NAME_UNNAMED);
            env.undeployAll();
        }
    }

    public static class ClientDeployPrecondDupExprDecl implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create expression expr_one {0}";
            env.compileDeploy(epl, path);
            tryInvalidDeploy(env, epl, "A declared-expression by name 'expr_one'", MODULE_NAME_UNNAMED);
            env.undeployAll();
        }
    }

    public static class ClientDeployPrecondDupScript implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create expression double myscript(stringvalue) [0]";
            env.compileDeploy(epl, path);
            tryInvalidDeploy(env, epl, "A script by name 'myscript (1 parameters)'", MODULE_NAME_UNNAMED);
            env.undeployAll();
        }
    }

    public static class ClientDeployPrecondDupContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create context MyContext as partition by theString from SupportBean";
            env.compileDeploy(epl, path);
            tryInvalidDeploy(env, epl, "A context by name 'MyContext'", MODULE_NAME_UNNAMED);
            env.undeployAll();
        }
    }

    public static class ClientDeployPrecondDupIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl;
            EPCompiled compiled;

            env.compileDeploy("create table MyTable (col1 string primary key, col2 string)", path);
            epl = "create index MyIndexOnTable on MyTable(col2)";
            compiled = env.compile(epl, path);
            env.deploy(compiled);
            tryInvalidDeploy(env, compiled, "An index by name 'MyIndexOnTable'", MODULE_NAME_UNNAMED);

            env.compileDeploy("create window MyWindow#keepall as SupportBean", path);
            epl = "create index MyIndexOnNW on MyWindow(intPrimitive)";
            compiled = env.compile(epl, path);
            env.deploy(compiled);
            tryInvalidDeploy(env, compiled, "An index by name 'MyIndexOnNW'", MODULE_NAME_UNNAMED);

            env.undeployAll();
        }
    }

    private static void tryInvalidDeploy(RegressionEnvironment env, String epl, String text, String moduleName) {
        EPCompiled compiled = env.compile(epl, options -> options.setAccessModifierNamedWindow(ctx -> NameAccessModifier.PUBLIC).setAccessModifierTable(ctx -> NameAccessModifier.PUBLIC).setAccessModifierEventType(ctx -> NameAccessModifier.PUBLIC)
            .setAccessModifierVariable(ctx -> NameAccessModifier.PUBLIC).setAccessModifierExpression(ctx -> NameAccessModifier.PUBLIC).setAccessModifierScript(ctx -> NameAccessModifier.PUBLIC).setAccessModifierContext(ctx -> NameAccessModifier.PUBLIC));
        tryInvalidDeploy(env, compiled, text, moduleName);
    }

    private static void tryInvalidDeploy(RegressionEnvironment env, EPCompiled compiled, String text, String moduleName) {
        String message = "A precondition is not satisfied: " + text + " has already been created for module '" + moduleName + "'";
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
}
