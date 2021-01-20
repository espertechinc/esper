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

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.runtime.client.EPDeploymentDependencyConsumed;
import com.espertech.esper.runtime.client.EPDeploymentDependencyProvided;
import com.espertech.esper.runtime.client.util.EPObjectType;

import java.util.*;

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.assertEqualsAnyOrder;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ClientDeployListDependencies {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientDeployListDependenciesObjectTypes());
        execs.add(new ClientDeployListDependenciesWModuleName());
        execs.add(new ClientDeployListDependenciesNoDependencies());
        execs.add(new ClientDeployListDependencyStar());
        execs.add(new ClientDeployListDependenciesInvalid());
        return execs;
    }

    private static class ClientDeployListDependencyStar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('typea') @public create schema TypeA()", path);
            env.compileDeploy("@name('typeb') @public create schema TypeB()", path);
            env.compileDeploy("@name('typec') @public create schema TypeC(a TypeA, b TypeB)", path);
            env.compileDeploy("@name('typed') @public create schema TypeD(c TypeC)", path);
            env.compileDeploy("@name('typee') @public create schema TypeE(c TypeC)", path);

            String a = env.deploymentId("typea");
            String b = env.deploymentId("typeb");
            String c = env.deploymentId("typec");
            String d = env.deploymentId("typed");
            String e = env.deploymentId("typee");

            assertProvided(env, a, makeProvided(EPObjectType.EVENTTYPE, "TypeA", c));
            assertConsumed(env, a);

            assertProvided(env, b, makeProvided(EPObjectType.EVENTTYPE, "TypeB", c));
            assertConsumed(env, b);

            assertProvided(env, c, makeProvided(EPObjectType.EVENTTYPE, "TypeC", d, e));
            assertConsumed(env, c, new EPDeploymentDependencyConsumed.Item(a, EPObjectType.EVENTTYPE, "TypeA"), new EPDeploymentDependencyConsumed.Item(b, EPObjectType.EVENTTYPE, "TypeB"));

            assertProvided(env, d);
            assertConsumed(env, d, new EPDeploymentDependencyConsumed.Item(c, EPObjectType.EVENTTYPE, "TypeC"));

            assertProvided(env, e);
            assertConsumed(env, e, new EPDeploymentDependencyConsumed.Item(c, EPObjectType.EVENTTYPE, "TypeC"));

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.RUNTIMEOPS);
        }
    }

    private static class ClientDeployListDependenciesInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            assertNull(env.deployment().getDeploymentDependenciesConsumed("dummy"));
            assertNull(env.deployment().getDeploymentDependenciesProvided("dummy"));

            try {
                assertNull(env.deployment().getDeploymentDependenciesConsumed(null));
                fail();
            } catch (IllegalArgumentException ex) {
                // expected
            }

            try {
                assertNull(env.deployment().getDeploymentDependenciesProvided(null));
                fail();
            } catch (IllegalArgumentException ex) {
                // expected
            }
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.RUNTIMEOPS);
        }
    }

    private static class ClientDeployListDependenciesNoDependencies implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from SupportBean");
            assertNoneProvidedConsumed(env, "s0");
            env.compileDeploy("module A;\n @name('table') create table MyTable(k string, v string)");
            assertNoneProvidedConsumed(env, "table");
            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.RUNTIMEOPS);
        }
    }

    private static class ClientDeployListDependenciesWModuleName implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath pathA = new RegressionPath();
            env.compileDeploy("module A;\n @name('createA') @protected create window MyWindow#keepall as SupportBean", pathA);

            RegressionPath pathB = new RegressionPath();
            env.compileDeploy("module B;\n @name('createB') @protected create window MyWindow#keepall as SupportBean", pathB);

            env.compileDeploy("@name('B1') select * from MyWindow", pathB);
            env.compileDeploy("@name('A1') select * from MyWindow", pathA);
            env.compileDeploy("@name('A2') select * from MyWindow", pathA);
            env.compileDeploy("@name('B2') select * from MyWindow", pathB);

            assertProvided(env, env.deploymentId("createA"), makeProvided(EPObjectType.NAMEDWINDOW, "MyWindow", env.deploymentId("A1"), env.deploymentId("A2")));
            assertProvided(env, env.deploymentId("createB"), makeProvided(EPObjectType.NAMEDWINDOW, "MyWindow", env.deploymentId("B1"), env.deploymentId("B2")));
            for (String name : new String[]{"A1", "A2"}) {
                assertConsumed(env, env.deploymentId(name), new EPDeploymentDependencyConsumed.Item(env.deploymentId("createA"), EPObjectType.NAMEDWINDOW, "MyWindow"));
            }
            for (String name : new String[]{"B1", "B2"}) {
                assertConsumed(env, env.deploymentId(name), new EPDeploymentDependencyConsumed.Item(env.deploymentId("createB"), EPObjectType.NAMEDWINDOW, "MyWindow"));
            }

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.RUNTIMEOPS);
        }
    }

    private static class ClientDeployListDependenciesObjectTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplProvide =
                "@name('provide') @public create window MyWindow#keepall as SupportBean;\n" +
                    "@public create table MyTable(k string primary key, value string);\n" +
                    "@public create variable int MyVariable = 0;\n" +
                    "@public create context MyContext partition by theString from SupportBean;\n" +
                    "@public create schema MyEventType();\n" +
                    "@public create expression MyExpression { 0 };\n" +
                    "@public create expression double MyScript(stringvalue) [0];\n" +
                    "@public create index MyIndexA on MyWindow(intPrimitive);\n" +
                    "@public create index MyIndexB on MyTable(value);\n" +
                    "@public create inlined_class \"\"\" public class MyClass { public static String doIt() { return \"abc\"; } }\"\"\";\n";
            env.compileDeploy(eplProvide, path);

            String eplConsume = "@name('consume') context MyContext select MyVariable, count(*), MyTable['a'].value from MyWindow;\n" +
                "select MyExpression(), MyScript('a'), MyClass.doIt() from MyEventType;\n" +
                "on SupportBean as sb merge MyWindow as mw where sb.intPrimitive=mw.intPrimitive when matched then delete;\n" +
                "on SupportBean as sb merge MyTable as mt where sb.theString=mt.value when matched then delete;\n";
            env.compileDeploy(eplConsume, path);

            String deploymentIdProvide = env.deploymentId("provide");
            String deploymentIdConsume = env.deploymentId("consume");

            assertProvided(env, deploymentIdProvide,
                makeProvided(EPObjectType.NAMEDWINDOW, "MyWindow", deploymentIdConsume),
                makeProvided(EPObjectType.TABLE, "MyTable", deploymentIdConsume),
                makeProvided(EPObjectType.VARIABLE, "MyVariable", deploymentIdConsume),
                makeProvided(EPObjectType.CONTEXT, "MyContext", deploymentIdConsume),
                makeProvided(EPObjectType.EVENTTYPE, "MyEventType", deploymentIdConsume),
                makeProvided(EPObjectType.EXPRESSION, "MyExpression", deploymentIdConsume),
                makeProvided(EPObjectType.SCRIPT, "MyScript#1", deploymentIdConsume),
                makeProvided(EPObjectType.INDEX, "MyIndexA on named-window MyWindow", deploymentIdConsume),
                makeProvided(EPObjectType.INDEX, "MyIndexB on table MyTable", deploymentIdConsume),
                makeProvided(EPObjectType.CLASSPROVIDED, "MyClass", deploymentIdConsume));

            assertConsumed(env, deploymentIdConsume,
                new EPDeploymentDependencyConsumed.Item(deploymentIdProvide, EPObjectType.NAMEDWINDOW, "MyWindow"),
                new EPDeploymentDependencyConsumed.Item(deploymentIdProvide, EPObjectType.TABLE, "MyTable"),
                new EPDeploymentDependencyConsumed.Item(deploymentIdProvide, EPObjectType.VARIABLE, "MyVariable"),
                new EPDeploymentDependencyConsumed.Item(deploymentIdProvide, EPObjectType.CONTEXT, "MyContext"),
                new EPDeploymentDependencyConsumed.Item(deploymentIdProvide, EPObjectType.EVENTTYPE, "MyEventType"),
                new EPDeploymentDependencyConsumed.Item(deploymentIdProvide, EPObjectType.EXPRESSION, "MyExpression"),
                new EPDeploymentDependencyConsumed.Item(deploymentIdProvide, EPObjectType.SCRIPT, "MyScript#1"),
                new EPDeploymentDependencyConsumed.Item(deploymentIdProvide, EPObjectType.INDEX, "MyIndexA on named-window MyWindow"),
                new EPDeploymentDependencyConsumed.Item(deploymentIdProvide, EPObjectType.INDEX, "MyIndexB on table MyTable"),
                new EPDeploymentDependencyConsumed.Item(deploymentIdProvide, EPObjectType.CLASSPROVIDED, "MyClass"));

            assertEqualsAnyOrder(new String[]{env.deploymentId("provide")}, env.deployment().getDeployment(deploymentIdConsume).getDeploymentIdDependencies());

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.RUNTIMEOPS);
        }
    }

    private static EPDeploymentDependencyProvided.Item makeProvided(EPObjectType objectType, String objectName, String... deploymentIds) {
        return new EPDeploymentDependencyProvided.Item(objectType, objectName, new HashSet<>(Arrays.asList(deploymentIds)));
    }

    private static void assertConsumed(RegressionEnvironment env, String deploymentId, EPDeploymentDependencyConsumed.Item... expected) {
        EPDeploymentDependencyConsumed consumed = env.deployment().getDeploymentDependenciesConsumed(deploymentId);
        assertEqualsAnyOrder(expected, consumed.getDependencies().toArray());
    }

    private static void assertProvided(RegressionEnvironment env, String deploymentId, EPDeploymentDependencyProvided.Item... expected) {
        EPDeploymentDependencyProvided provided = env.deployment().getDeploymentDependenciesProvided(deploymentId);
        assertEqualsAnyOrder(expected, provided.getDependencies().toArray());
    }

    private static void assertNoneProvidedConsumed(RegressionEnvironment env, String statementName) {
        String deploymentId = env.deploymentId("s0");
        assertProvided(env, deploymentId);
        assertConsumed(env, deploymentId);
    }
}
