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
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.CompilerPath;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.runtime.client.EPDeployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClientCompileVisibility {
    private final static String FIRST_MESSAGE = "Failed to resolve event type, named window or table by name 'MySchema'";

    private final static String CREATE_EPL =
        "${PREFIX} create schema MySchema();" +
            "${PREFIX} create variable int abc;\n" +
            "${PREFIX} create context MyContext partition by theString from SupportBean;\n" +
            "${PREFIX} create window MyWindow#keepall as SupportBean;\n" +
            "${PREFIX} create table MyTable as (c count(*));\n" +
            "${PREFIX} create expression MyExpr { 1 };\n" +
            "${PREFIX} create expression double myscript(intvalue) [0];\n";

    private final static String USER_EPL =
        "select 1 from MySchema;\n" +
            "select abc from SupportBean;\n" +
            "context MyContext select * from SupportBean;\n" +
            "on SupportBean update MyWindow set theString = 'a';\n" +
            "into table MyTable select count(*) as c from SupportBean;\n" +
            "select MyExpr() from SupportBean;\n" +
            "select myscript(1) from SupportBean;\n";

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientVisibilityNamedWindowSimple());
        execs.add(new ClientVisibilityAmbiguousPathWithPreconfigured());
        execs.add(new ClientVisibilityDefaultPrivate());
        execs.add(new ClientVisibilityAnnotationPrivate());
        execs.add(new ClientVisibilityAnnotationProtected());
        execs.add(new ClientVisibilityAnnotationPublic());
        execs.add(new ClientVisibilityModuleNameOption());
        execs.add(new ClientVisibilityAnnotationSendable());
        execs.add(new ClientVisibilityAnnotationInvalid());
        execs.add(new ClientVisibilityAmbiguousTwoPath());
        execs.add(new ClientVisibilityDisambiguateWithUses());
        execs.add(new ClientVisibilityBusRequiresPublic());
        return execs;
    }

    private static class ClientVisibilityBusRequiresPublic implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String message = "Event type 'ABC' with bus-visibility requires the public access modifier for the event type";
            tryInvalidCompile(env, "@Private @BusEventType create schema ABC()", message);
            tryInvalidCompile(env, "@BusEventType create schema ABC()", message);

            tryInvalidCompileWConfigure(config -> config.getCompiler().getByteCode().setBusModifierEventType(EventTypeBusModifier.BUS),
                "@private create schema ABC()", message);
            tryInvalidCompileWConfigure(config -> config.getCompiler().getByteCode().setBusModifierEventType(EventTypeBusModifier.BUS),
                "@protected create schema ABC()", message);

            for (NameAccessModifier modifier : new NameAccessModifier[]{NameAccessModifier.PROTECTED, NameAccessModifier.PRIVATE}) {
                tryInvalidCompileWConfigure(config -> {
                    config.getCompiler().getByteCode().setBusModifierEventType(EventTypeBusModifier.BUS);
                    config.getCompiler().getByteCode().setAccessModifierEventType(modifier);
                },
                    "create schema ABC()", message);
            }
        }
    }

    private static class ClientVisibilityDisambiguateWithUses implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionDisambiguate(env,
                "create variable int var_named = 1",
                "create variable String var_named = 'x'",
                "select var_named from SupportBean",
                () -> {
                    env.sendEventBean(new SupportBean());
                    assertEquals("x", env.listener("s0").assertOneGetNewAndReset().get("var_named"));
                });

            runAssertionDisambiguate(env,
                "create context MyContext partition by theString from SupportBean;",
                "create context MyContext partition by id from SupportBean_S0;",
                "context MyContext select p00 from SupportBean_S0",
                () -> {
                });

            runAssertionDisambiguate(env,
                "create window MyWindow#keepall as SupportBean",
                "create window MyWindow#keepall as SupportBean_S0",
                "select p00 from MyWindow",
                () -> {
                });

            runAssertionDisambiguate(env,
                "create table MyTable(c0 string)",
                "create table MyTable(c1 int)",
                "select c1 from MyTable",
                () -> {
                });

            runAssertionDisambiguate(env,
                "create expression MyExpr {1}",
                "create expression MyExpr {'y'}",
                "select MyExpr() as c0 from SupportBean",
                () -> {
                    env.sendEventBean(new SupportBean());
                    assertEquals("y", env.listener("s0").assertOneGetNewAndReset().get("c0"));
                });

            runAssertionDisambiguate(env,
                "create expression double myscript() [0];",
                "create expression string myscript() ['z'];",
                "select myscript() as c0 from SupportBean",
                () -> {
                    env.sendEventBean(new SupportBean());
                    assertEquals("z", env.listener("s0").assertOneGetNewAndReset().get("c0"));
                });

            runAssertionDisambiguate(env,
                "create schema MySchema as (p0 int);",
                "create schema MySchema as (p1 string);",
                "select p1 from MySchema",
                () -> {
                });
        }
    }

    private static class ClientVisibilityAnnotationInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "@protected @private create schema abc()",
                "Encountered both the @private and the @protected annotation");
            tryInvalidCompile(env, "@public @private create schema abc()",
                "Encountered both the @private and the @public annotation");
            tryInvalidCompile(env, "@public @protected create schema abc()",
                "Encountered both the @protected and the @public annotation");
        }
    }

    private static class ClientVisibilityAnnotationSendable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@Public @BusEventType create schema MyEvent(p0 string);\n" +
                "@name('s0') select * from MyEvent;\n").addListener("s0");
            env.sendEventMap(Collections.emptyMap(), "MyEvent");
            assertTrue(env.listener("s0").isInvoked());
            env.undeployAll();
        }
    }

    private static class ClientVisibilityModuleNameOption implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionOptionModuleName(env, "select 1 from SupportBean");
            runAssertionOptionModuleName(env, "module x; select 1 from SupportBean");
        }
    }

    private static class ClientVisibilityAnnotationProtected implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "module a.b.c;\n" + CREATE_EPL.replace("${PREFIX}", "@protected");
            EPCompiled compiled = env.compile(epl);
            tryInvalidNotVisible(env, compiled);

            RegressionPath path = new RegressionPath();
            path.add(compiled);
            env.compile("module a.b.c;\n" + USER_EPL, path);

            tryInvalidCompile(env, path, "module a.b.d;\n" + USER_EPL, FIRST_MESSAGE);
        }
    }

    private static class ClientVisibilityAnnotationPublic implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "module a.b.c;\n" + CREATE_EPL.replace("${PREFIX}", "@public");
            EPCompiled compiled = env.compile(epl);

            RegressionPath path = new RegressionPath();
            path.add(compiled);
            env.compile("module x;\n" + USER_EPL, path);
        }
    }

    private static class ClientVisibilityDefaultPrivate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = CREATE_EPL.replace("${PREFIX}", "");
            EPCompiled compiled = env.compile(epl);
            tryInvalidNotVisible(env, compiled);

            epl = epl + USER_EPL;
            env.compileDeploy(epl).undeployAll();
        }
    }

    private static class ClientVisibilityAnnotationPrivate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = CREATE_EPL.replace("${PREFIX}", "@private") + USER_EPL;
            EPCompiled compiled = env.compile(epl);
            tryInvalidNotVisible(env, compiled);
        }
    }

    private static class ClientVisibilityAmbiguousTwoPath implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String commonEPL = "create variable int abc;\n" +
                "create schema MySchema();" +
                "create context MyContext partition by theString from SupportBean;\n" +
                "create window MyWindow#keepall as SupportBean;\n" +
                "create table MyTable as (c count(*));\n" +
                "create expression MyExpr { 1 };\n" +
                "create expression double myscript(stringvalue) [0];\n";

            EPCompiled modOne = env.compile("module one;\n " + commonEPL, new RegressionPath());
            EPCompiled modTwo = env.compile("module two;\n " + commonEPL, new RegressionPath());

            RegressionPath path = new RegressionPath();
            path.add(modOne);
            path.add(modTwo);
            tryInvalidCompile(env, path, "select abc from SupportBean",
                "The variable by name 'abc' is ambiguous as it exists for multiple modules");
            tryInvalidCompile(env, path, "select 1 from MySchema",
                "The event type by name 'MySchema' is ambiguous as it exists for multiple modules");
            tryInvalidCompile(env, path, "context MyContext select * from SupportBean",
                "The context by name 'MyContext' is ambiguous as it exists for multiple modules");
            tryInvalidCompile(env, path, "select * from MyWindow",
                "The named window by name 'MyWindow' is ambiguous as it exists for multiple modules");
            tryInvalidCompile(env, path, "select * from MyTable",
                "The table by name 'MyTable' is ambiguous as it exists for multiple modules");
            tryInvalidCompile(env, path, "select MyExpr() from SupportBean",
                "The declared-expression by name 'MyExpr' is ambiguous as it exists for multiple modules");
            tryInvalidCompile(env, path, "select myscript('a') from SupportBean",
                "The script by name 'myscript' is ambiguous as it exists for multiple modules: A script by name 'myscript (1 parameters)' is exported by multiple modules");
        }
    }

    private static class ClientVisibilityAmbiguousPathWithPreconfigured implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            CompilerArguments args = new CompilerArguments(new Configuration());
            args.getOptions().setAccessModifierVariable(ctx -> NameAccessModifier.PUBLIC).setAccessModifierEventType(ctx -> NameAccessModifier.PUBLIC);
            EPCompiled compiled;
            try {
                compiled = EPCompilerProvider.getCompiler().compile(
                    "create variable int preconfigured_variable;\n" +
                        "create schema SupportBean_S1 as (p0 string);\n", args);
            } catch (EPCompileException e) {
                throw new RuntimeException(e);
            }
            path.add(compiled);

            tryInvalidCompile(env, path, "select preconfigured_variable from SupportBean",
                "The variable by name 'preconfigured_variable' is ambiguous as it exists in both the path space and the preconfigured space");
            tryInvalidCompile(env, path, "select 'test' from SupportBean_S1",
                "The event type by name 'SupportBean_S1' is ambiguous as it exists in both the path space and the preconfigured space");
        }
    }

    private static class ClientVisibilityNamedWindowSimple implements RegressionExecution {
        String[] fields = "c0,c1".split(",");

        public void run(RegressionEnvironment env) {
            EPCompiled compiledCreate = env.compile("create window MyWindow#length(2) as SupportBean", options -> options.setAccessModifierNamedWindow(ctx -> NameAccessModifier.PUBLIC));
            env.deploy(compiledCreate);

            EPCompiled compiledInsert = env.compile("insert into MyWindow select * from SupportBean",
                new CompilerArguments(new Configuration()).setPath(new CompilerPath().add(compiledCreate)));
            env.deploy(compiledInsert);

            EPCompiled compiledSelect = env.compile("@name('s0') select theString as c0, sum(intPrimitive) as c1 from MyWindow;\n",
                new CompilerArguments(new Configuration()).setPath(new CompilerPath().add(compiledCreate)));
            env.deploy(compiledSelect).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10});

            env.sendEventBean(new SupportBean("E2", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 30});

            env.sendEventBean(new SupportBean("E3", 25));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 45});

            env.sendEventBean(new SupportBean("E4", 26));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4", 51});

            env.undeployAll();
        }
    }

    private static void tryInvalidNotVisible(RegressionEnvironment env, EPCompiled compiled) {
        RegressionPath path = new RegressionPath();
        path.add(compiled);
        tryInvalidCompile(env, path, "select 1 from MySchema",
            "Failed to resolve event type, named window or table by name 'MySchema'");
        tryInvalidCompile(env, path, "select abc from SupportBean",
            "Failed to validate select-clause expression 'abc': Property named 'abc' is not valid in any stream");
        tryInvalidCompile(env, path, "context MyContext select * from SupportBean",
            "Context by name 'MyContext' could not be found");
        tryInvalidCompile(env, path, "on SupportBean update MyWindow set theString = 'a'",
            "A named window or table 'MyWindow' has not been declared");
        tryInvalidCompile(env, path, "into table MyTable select count(*) as c from SupportBean",
            "Invalid into-table clause: Failed to find table by name 'MyTable'");
        tryInvalidCompile(env, path, "select MyExpr() from SupportBean",
            "Failed to validate select-clause expression 'MyExpr': Unknown single-row function, expression declaration, script or aggregation function named 'MyExpr' could not be resolved");
        tryInvalidCompile(env, path, "select myscript(1) from SupportBean",
            "Failed to validate select-clause expression 'myscript(1)': Unknown single-row function, aggregation function or mapped or indexed property named 'myscript' could not be resolved");
    }

    private static void runAssertionDisambiguate(RegressionEnvironment env, String firstEpl, String secondEpl, String useEpl,
                                                 Runnable assertion) {
        EPCompiled first = env.compile("module a;\n @public " + firstEpl + "\n");
        EPCompiled second = env.compile("module b;\n @public " + secondEpl + "\n");
        env.deploy(first);
        env.deploy(second);

        RegressionPath path = new RegressionPath();
        path.add(first);
        path.add(second);
        env.compileDeploy("uses b;\n @name('s0') " + useEpl + "\n", path).addListener("s0");

        assertion.run();

        env.undeployAll();
    }

    private static void runAssertionOptionModuleName(RegressionEnvironment env, String epl) {
        EPCompiled compiledBoth;
        try {
            CompilerArguments args = new CompilerArguments(env.getConfiguration());
            args.getOptions().setModuleName(ctx -> "abc");
            compiledBoth = EPCompilerProvider.getCompiler().compile(epl, args);
        } catch (EPCompileException ex) {
            throw new RuntimeException();
        }

        EPDeployment deployed = SupportCompileDeployUtil.deploy(compiledBoth, env.runtime());
        assertEquals("abc", deployed.getModuleName()); // Option-provided module-name wins

        env.undeployAll();
    }

    private static void tryInvalidCompileWConfigure(Consumer<Configuration> configurer, String epl, String message) {
        try {
            Configuration configuration = new Configuration();
            configurer.accept(configuration);
            CompilerArguments args = new CompilerArguments(configuration);
            EPCompilerProvider.getCompiler().compile(epl, args);
        } catch (EPCompileException ex) {
            SupportMessageAssertUtil.assertMessage(ex, message);
        }
    }
}
