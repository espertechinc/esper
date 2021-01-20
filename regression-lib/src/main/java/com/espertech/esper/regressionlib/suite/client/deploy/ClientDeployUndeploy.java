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

import com.espertech.esper.regressionlib.framework.*;
import com.espertech.esper.runtime.client.EPUndeployException;
import com.espertech.esper.runtime.client.EPUndeployNotFoundException;
import com.espertech.esper.runtime.client.EPUndeployPreconditionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ClientDeployUndeploy {
    private static final Logger log = LoggerFactory.getLogger(ClientDeployUndeploy.class);

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientUndeployInvalid());
        execs.add(new ClientUndeployDependencyChain());
        execs.add(new ClientUndeployPrecondDepScript());
        execs.add(new ClientUndeployPrecondDepNamedWindow());
        execs.add(new ClientUndeployPrecondDepVariable());
        execs.add(new ClientUndeployPrecondDepContext());
        execs.add(new ClientUndeployPrecondDepEventType());
        execs.add(new ClientUndeployPrecondDepExprDecl());
        execs.add(new ClientUndeployPrecondDepTable());
        execs.add(new ClientUndeployPrecondDepIndex());
        execs.add(new ClientUndeployPrecondDepClass());
        return execs;
    }

    private static class ClientUndeployDependencyChain implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create variable int A = 10", path);
            env.compileDeploy("@public create variable int B = A", path);
            env.compileDeploy("@public create variable int C = B", path);
            env.compileDeploy("@name('s0') @public create variable int D = C", path);

            assertEquals(10, env.runtime().getVariableService().getVariableValue(env.deploymentId("s0"), "D"));

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.RUNTIMEOPS);
        }
    }

    private static class ClientUndeployInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            try {
                env.deployment().undeploy("nofound");
                fail();
            } catch (EPUndeployNotFoundException ex) {
                SupportMessageAssertUtil.assertMessage(ex.getMessage(), "Deployment id 'nofound' cannot be found");
            } catch (EPUndeployException t) {
                fail();
            }
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    public static class ClientUndeployPrecondDepNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('infra') @public create window SimpleWindow#keepall as SupportBean", path);

            String text = "Named window 'SimpleWindow'";
            tryDeployInvalidUndeploy(env, path, "infra", "@name('A') select * from SimpleWindow", "A", text);
            tryDeployInvalidUndeploy(env, path, "infra", "@name('B') select (select * from SimpleWindow) from SupportBean", "B", text);

            env.undeployModuleContaining("infra");
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    public static class ClientUndeployPrecondDepTable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('infra') @public create table SimpleTable(col1 string primary key, col2 string)", path);

            String text = "Table 'SimpleTable'";
            tryDeployInvalidUndeploy(env, path, "infra", "@name('A') select SimpleTable['a'] from SupportBean", "A", text);
            tryDeployInvalidUndeploy(env, path, "infra", "@name('B') select (select * from SimpleTable) from SupportBean", "B", text);
            tryDeployInvalidUndeploy(env, path, "infra", "@name('C') create index MyIndex on SimpleTable(col2)", "C", text);

            env.undeployModuleContaining("infra");
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    public static class ClientUndeployPrecondDepVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('variable') @public create variable string varstring", path);

            String text = "Variable 'varstring'";
            tryDeployInvalidUndeploy(env, path, "variable", "@name('A') select varstring from SupportBean", "A", text);
            tryDeployInvalidUndeploy(env, path, "variable", "@name('B') on SupportBean set varstring='a'", "B", text);

            env.undeployModuleContaining("variable");
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    public static class ClientUndeployPrecondDepContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('ctx') @public create context MyContext partition by theString from SupportBean", path);

            String text = "Context 'MyContext'";
            tryDeployInvalidUndeploy(env, path, "ctx", "@name('A') context MyContext select count(*) from SupportBean", "A", text);

            env.undeployModuleContaining("ctx");
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    public static class ClientUndeployPrecondDepEventType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('schema') @public create schema MySchema(col string)", path);

            String text = "Event type 'MySchema'";
            tryDeployInvalidUndeploy(env, path, "schema", "@name('A') insert into MySchema select 'a' as col from SupportBean", "A", text);
            tryDeployInvalidUndeploy(env, path, "schema", "@name('B') select count(*) from MySchema", "B", text);

            env.undeployModuleContaining("schema");
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    public static class ClientUndeployPrecondDepExprDecl implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('expr') @public create expression myexpression { 0 }", path);

            String text = "Declared-expression 'myexpression'";
            tryDeployInvalidUndeploy(env, path, "expr", "@name('A') select myexpression() as col from SupportBean", "A", text);
            tryDeployInvalidUndeploy(env, path, "expr", "@name('B') select (select myexpression from SupportBean#keepall) from SupportBean", "B", text);

            env.undeployModuleContaining("expr");
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    public static class ClientUndeployPrecondDepClass implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('clazz') @public create inlined_class \"\"\" public class MyClass { public static String doIt() { return \"def\"; } }\"\"\"", path);

            String text = "Application-inlined class 'MyClass'";
            tryDeployInvalidUndeploy(env, path, "clazz", "@name('A') select MyClass.doIt() as col from SupportBean", "A", text);

            env.undeployModuleContaining("clazz");
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    public static class ClientUndeployPrecondDepScript implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('script') @public create expression double myscript(stringvalue) [0]", path);

            String text = "Script 'myscript (1 parameters)'";
            tryDeployInvalidUndeploy(env, path, "script", "@name('A') select myscript('a') as col from SupportBean", "A", text);

            env.undeployModuleContaining("script");
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    public static class ClientUndeployPrecondDepIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String text;

            // Table
            env.compileDeploy("@name('infra') @public create table MyTable(k1 string primary key, i1 int)", path);
            env.compileDeploy("@name('index') @public create index MyIndexOnTable on MyTable(i1)", path);

            text = "Index 'MyIndexOnTable'";
            tryDeployInvalidUndeploy(env, path, "index", "@name('A') select * from SupportBean as sb, MyTable as mt where sb.intPrimitive = mt.i1", "A", text);
            tryDeployInvalidUndeploy(env, path, "index", "@name('B') select * from SupportBean as sb where exists (select * from MyTable as mt where sb.intPrimitive = mt.i1)", "B", text);

            env.undeployModuleContaining("index");
            env.undeployModuleContaining("infra");

            // Named window
            env.compileDeploy("@name('infra') @public create window MyWindow#keepall as SupportBean", path);
            env.compileDeploy("@name('index') @public create index MyIndexOnNW on MyWindow(intPrimitive)", path);

            text = "Index 'MyIndexOnNW'";
            tryDeployInvalidUndeploy(env, path, "index", "@name('A') on SupportBean_S0 as s0 delete from MyWindow as mw where mw.intPrimitive = s0.id", "A", text);

            env.undeployModuleContaining("index");
            env.undeployModuleContaining("infra");
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    private static void tryDeployInvalidUndeploy(RegressionEnvironment env, RegressionPath path, String thingStatementName, String epl, String dependingStatementName, String text) {
        env.compileDeploy(epl, path);
        log.info("Deployed as " + env.deploymentId(dependingStatementName) + ": " + epl);
        String message = "A precondition is not satisfied: " + text + " cannot be un-deployed as it is referenced by deployment '" + env.deploymentId(dependingStatementName) + "'";
        tryInvalidUndeploy(env, thingStatementName, message);
        env.undeployModuleContaining(dependingStatementName);
    }

    private static void tryInvalidUndeploy(RegressionEnvironment env, String statementName, String message) {
        try {
            env.runtime().getDeploymentService().undeploy(env.statement(statementName).getDeploymentId());
            fail();
        } catch (EPUndeployPreconditionException ex) {
            if (!message.equals("skip")) {
                SupportMessageAssertUtil.assertMessage(ex.getMessage(), message);
            }
        } catch (EPUndeployException ex) {
            fail();
        }
    }
}
