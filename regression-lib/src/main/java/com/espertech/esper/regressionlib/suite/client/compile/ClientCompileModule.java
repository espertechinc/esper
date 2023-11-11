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
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.module.ModuleItem;
import com.espertech.esper.common.client.module.ModuleProperty;
import com.espertech.esper.common.client.module.ParseException;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPDeployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.*;

public class ClientCompileModule {
    private final static String NEWLINE = System.getProperty("line.separator");

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompileModuleWImports());
        execs.add(new ClientCompileModuleLineNumberAndComments());
        execs.add(new ClientCompileModuleTwoModules());
        execs.add(new ClientCompileModuleParse());
        execs.add(new ClientCompileModuleParseFail());
        execs.add(new ClientCompileModuleCommentTrailing());
        execs.add(new ClientCompileModuleEPLModuleText());
        return execs;
    }

    private static class ClientCompileModuleEPLModuleText implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPDeployment deployment;

            String epl = "@name('s0') select * from SupportBean";
            env.compileDeploy(epl);
            deployment = env.deployment().getDeployment(env.deploymentId("s0"));
            assertEquals(epl, deployment.getModuleProperties().get(ModuleProperty.MODULETEXT));
            env.undeployAll();

            env.eplToModelCompileDeploy(epl);
            deployment = env.deployment().getDeployment(env.deploymentId("s0"));
            assertEquals(epl, deployment.getModuleProperties().get(ModuleProperty.MODULETEXT));
            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    private static class ClientCompileModuleCommentTrailing implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final String epl =
                "@public @buseventtype create map schema Fubar as (foo String, bar Double);" + System.lineSeparator()
                    + "/** comment after */";
            env.compileDeploy(epl).undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    private static class ClientCompileModuleTwoModules implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String resource = "regression/test_module_12.epl";
            InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
            assertNotNull(input);

            Module module;
            try {
                module = env.getCompiler().readModule(input, resource);
                module.setUri("uri1");
                module.setArchiveName("archive1");
                module.setModuleUserObjectCompileTime("obj1");
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }

            EPCompiled compiled = env.compile(module);
            EPDeployment deployed;
            try {
                deployed = env.deployment().deploy(compiled);
            } catch (EPDeployException e) {
                throw new RuntimeException(e);
            }

            EPDeployment deplomentInfo = env.deployment().getDeployment(deployed.getDeploymentId());
            assertEquals("regression.test", deplomentInfo.getModuleName());
            assertEquals(2, deplomentInfo.getStatements().length);
            assertEquals("create schema MyType(col1 integer)", deplomentInfo.getStatements()[0].getProperty(StatementProperty.EPL));

            String moduleText = "module regression.test.two;" +
                "uses regression.test;" +
                "create schema MyTypeTwo(col1 integer, col2.col3 string);" +
                "select * from MyTypeTwo;";
            Module moduleTwo = env.parseModule(moduleText);
            moduleTwo.setUri("uri2");
            moduleTwo.setArchiveName("archive2");
            moduleTwo.setModuleUserObjectCompileTime("obj2");
            moduleTwo.setUses(new LinkedHashSet<>(Arrays.asList("a", "b")));
            moduleTwo.setImports(new LinkedHashSet<>(Arrays.asList("c", "d")));
            EPCompiled compiledTwo = env.compile(moduleTwo);
            env.deploy(compiledTwo);

            String[] deploymentIds = env.deployment().getDeployments();
            assertEquals(2, deploymentIds.length);

            List<EPDeployment> infoList = new ArrayList<>();
            for (int i = 0; i < deploymentIds.length; i++) {
                infoList.add(env.deployment().getDeployment(deploymentIds[i]));
            }
            Collections.sort(infoList, new Comparator<EPDeployment>() {
                public int compare(EPDeployment o1, EPDeployment o2) {
                    return o1.getModuleName().compareTo(o2.getModuleName());
                }
            });
            EPDeployment infoOne = infoList.get(0);
            EPDeployment infoTwo = infoList.get(1);
            assertEquals("regression.test", infoOne.getModuleName());
            assertEquals("uri1", infoOne.getModuleProperties().get(ModuleProperty.URI));
            assertEquals("archive1", infoOne.getModuleProperties().get(ModuleProperty.ARCHIVENAME));
            assertEquals("obj1", infoOne.getModuleProperties().get(ModuleProperty.USEROBJECT));
            assertNull(infoOne.getModuleProperties().get(ModuleProperty.USES));
            assertNotNull(infoOne.getModuleProperties().get(ModuleProperty.MODULETEXT));
            assertNotNull(infoOne.getLastUpdateDate());
            assertEquals("regression.test.two", infoTwo.getModuleName());
            assertEquals("uri2", infoTwo.getModuleProperties().get(ModuleProperty.URI));
            assertEquals("archive2", infoTwo.getModuleProperties().get(ModuleProperty.ARCHIVENAME));
            assertEquals("obj2", infoTwo.getModuleProperties().get(ModuleProperty.USEROBJECT));
            assertNotNull(infoOne.getModuleProperties().get(ModuleProperty.MODULETEXT));
            assertNotNull(infoTwo.getLastUpdateDate());
            EPAssertionUtil.assertEqualsExactOrder("a,b".split(","), (String[]) infoTwo.getModuleProperties().get(ModuleProperty.USES));
            EPAssertionUtil.assertEqualsExactOrder("c,d".split(","), (String[]) infoTwo.getModuleProperties().get(ModuleProperty.IMPORTS));

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    private static class ClientCompileModuleLineNumberAndComments implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String moduleText = NEWLINE + NEWLINE + "select * from ABC;" +
                NEWLINE + "select * from DEF";

            Module module = env.parseModule(moduleText);
            assertEquals(2, module.getItems().size());
            assertEquals(3, module.getItems().get(0).getLineNumber());
            assertEquals(3, module.getItems().get(0).getLineNumberEnd());
            assertEquals(4, module.getItems().get(1).getLineNumber());
            assertEquals(4, module.getItems().get(1).getLineNumberEnd());

            module = env.parseModule("/* abc */");
            EPCompiled compiled = env.compile(module);
            EPDeployment resultOne;
            try {
                resultOne = env.deployment().deploy(compiled);
            } catch (EPDeployException e) {
                throw new RuntimeException(e);
            }
            assertEquals(0, resultOne.getStatements().length);

            module = env.parseModule("select * from SupportBean; \r\n/* abc */\r\n");
            compiled = env.compile(module);
            EPDeployment resultTwo;
            try {
                resultTwo = env.deployment().deploy(compiled);
            } catch (EPDeployException e) {
                throw new RuntimeException(e);
            }
            assertEquals(1, resultTwo.getStatements().length);

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    private static class ClientCompileModuleWImports implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Module module = makeModule("com.testit", "@Name('A') select SupportStaticMethodLib.plusOne(intPrimitive) as val from SupportBean");
            module.getImports().add(SupportStaticMethodLib.class.getPackage().getName() + ".*");

            EPCompiled compiled = compileModule(env, module);
            env.deploy(compiled).addListener("A");

            env.sendEventBean(new SupportBean("E1", 4));
            assertEquals(5, env.listener("A").assertOneGetNewAndReset().get("val"));

            env.undeployAll();

            String epl = "import " + SupportStaticMethodLib.class.getName() + ";\n" +
                "@Name('A') select SupportStaticMethodLib.plusOne(intPrimitive) as val from SupportBean;\n";
            env.compileDeploy(epl).addListener("A");

            env.sendEventBean(new SupportBean("E1", 6));
            assertEquals(7, env.listener("A").assertOneGetNewAndReset().get("val"));

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    private static class ClientCompileModuleParse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Module module = env.readModule("regression/test_module_4.epl");
            assertModuleNoLines(module, null, "abd", null, new String[]{
                "select * from ABC",
                "/* Final comment */"
                });
            assertModuleLinesOnly(module, new ModuleItem(null, false, 3, -1, -1, 7, 3, 3),
                    new ModuleItem(null, true, 8, -1, -1, 9, -1, -1));

            module = env.readModule("regression/test_module_1.epl");
            assertModuleNoLines(module, "abc", "def,jlk", null, new String[]{
                "select * from A",
                "select * from B" + NEWLINE + "where C=d",
                "/* Test ; Comment */" + NEWLINE + "update ';' where B=C",
                "update D"
                }
            );

            module = env.readModule("regression/test_module_2.epl");
            assertModuleNoLines(module, "abc.def.hij", "def.hik,jlk.aja", null, new String[]{
                "// Note 4 white spaces after * and before from" + NEWLINE + "select * from A",
                "select * from B",
                "select *    " + NEWLINE + "    from C",
                }
            );

            module = env.readModule("regression/test_module_3.epl");
            assertModuleNoLines(module, null, null, null, new String[]{
                "create window ABC",
                "select * from ABC"
                }
            );

            module = env.readModule("regression/test_module_5.epl");
            assertModuleNoLines(module, "abd.def", null, null, new String[0]);

            module = env.readModule("regression/test_module_6.epl");
            assertModuleNoLines(module, null, null, null, new String[0]);

            module = env.readModule("regression/test_module_7.epl");
            assertModuleNoLines(module, null, null, null, new String[0]);

            module = env.readModule("regression/test_module_8.epl");
            assertModuleNoLines(module, "def.jfk", null, null, new String[0]);

            module = env.parseModule("module mymodule; uses mymodule2; import abc; select * from MyEvent;");
            assertModuleNoLines(module, "mymodule", "mymodule2", "abc", new String[]{
                "select * from MyEvent"
            });

            module = env.readModule("regression/test_module_11.epl");
            assertModuleNoLines(module, null, null, "com.mycompany.pck1", new String[0]);

            module = env.readModule("regression/test_module_10.epl");
            assertModuleNoLines(module, "abd.def", "one.use,two.use", "com.mycompany.pck1,com.mycompany.*", new String[]{
                "select * from A",
                }
            );

            assertEquals("org.mycompany.events", env.parseModule("module org.mycompany.events; select * from java.lang.Object;").getName());
            assertEquals("glob.update.me", env.parseModule("module glob.update.me; select * from java.lang.Object;").getName());
            assertEquals("seconds.until.every.where", env.parseModule("uses seconds.until.every.where; select * from java.lang.Object;").getUses().toArray()[0]);
            assertEquals("seconds.until.every.where", env.parseModule("import seconds.until.every.where; select * from java.lang.Object;").getImports().toArray()[0]);

            // Test script square brackets
            module = env.readModule("regression/test_module_13.epl");
            assertEquals(1, module.getItems().size());

            module = env.readModule("regression/test_module_14.epl");
            assertEquals(4, module.getItems().size());

            module = env.readModule("regression/test_module_15.epl");
            assertEquals(1, module.getItems().size());
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    private static class ClientCompileModuleParseFail implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidIO(env, "regression/dummy_not_there.epl",
                "Failed to find resource 'regression/dummy_not_there.epl' in classpath");

            tryInvalidParse(env, "regression/test_module_1_fail.epl",
                "Keyword 'module' must be followed by a name or package name (set of names separated by dots) for resource 'regression/test_module_1_fail.epl'");

            tryInvalidParse(env, "regression/test_module_2_fail.epl",
                "Keyword 'uses' must be followed by a name or package name (set of names separated by dots) for resource 'regression/test_module_2_fail.epl'");

            tryInvalidParse(env, "regression/test_module_3_fail.epl",
                "Keyword 'module' must be followed by a name or package name (set of names separated by dots) for resource 'regression/test_module_3_fail.epl'");

            tryInvalidParse(env, "regression/test_module_4_fail.epl",
                "Keyword 'uses' must be followed by a name or package name (set of names separated by dots) for resource 'regression/test_module_4_fail.epl'");

            tryInvalidParse(env, "regression/test_module_5_fail.epl",
                "Keyword 'import' must be followed by a name or package name (set of names separated by dots) for resource 'regression/test_module_5_fail.epl'");

            tryInvalidParse(env, "regression/test_module_6_fail.epl",
                "The 'module' keyword must be the first declaration in the module file for resource 'regression/test_module_6_fail.epl'");

            tryInvalidParse(env, "regression/test_module_7_fail.epl",
                "Duplicate use of the 'module' keyword for resource 'regression/test_module_7_fail.epl'");

            tryInvalidParse(env, "regression/test_module_8_fail.epl",
                "The 'uses' and 'import' keywords must be the first declaration in the module file or follow the 'module' declaration");

            tryInvalidParse(env, "regression/test_module_9_fail.epl",
                "The 'uses' and 'import' keywords must be the first declaration in the module file or follow the 'module' declaration");

            // try control chars
            tryInvalidControlCharacters(env);
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS, RegressionFlag.INVALIDITY);
        }
    }

    private static void tryInvalidControlCharacters(RegressionEnvironment env) {
        String epl = "select * \u008F from SupportBean";
        env.tryInvalidCompile(epl, "Failed to parse: Unrecognized control characters found in text, failed to parse text ");
    }

    private static void tryInvalidIO(RegressionEnvironment env, String resource, String message) {
        try {
            env.getCompiler().readModule(resource, ClientCompileModule.class.getClassLoader());
            fail();
        } catch (IOException ex) {
            assertEquals(message, ex.getMessage());
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void tryInvalidParse(RegressionEnvironment env, String resource, String message) {
        try {
            env.getCompiler().readModule(resource, env.getClass().getClassLoader());
            fail();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (ParseException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    private static void assertModuleNoLines(Module module, String name, String usesCSV, String importsCSV, String[] statements) {
        assertEquals(name, module.getName());

        String[] expectedUses = usesCSV == null ? new String[0] : usesCSV.split(",");
        EPAssertionUtil.assertEqualsExactOrder(expectedUses, module.getUses().toArray());

        String[] expectedImports = importsCSV == null ? new String[0] : importsCSV.split(",");
        EPAssertionUtil.assertEqualsExactOrder(expectedImports, module.getImports().toArray());

        String[] stmtsFound = new String[module.getItems().size()];
        for (int i = 0; i < module.getItems().size(); i++) {
            stmtsFound[i] = module.getItems().get(i).getExpression();
        }
        EPAssertionUtil.assertEqualsExactOrder(statements, stmtsFound);
    }

    private static void assertModuleLinesOnly(Module module, ModuleItem...expecteds) {
        assertEquals(expecteds.length, module.getItems().size());
        for (int i = 0; i < expecteds.length; i++) {
            ModuleItem expected = expecteds[i];
            ModuleItem actual = module.getItems().get(i);
            String message = "Failed to item#" + i;
            assertEquals(message, expected.isCommentOnly(), actual.isCommentOnly());
            assertEquals(message, expected.getLineNumber(), actual.getLineNumber());
            assertEquals(message, expected.getLineNumberEnd(), actual.getLineNumberEnd());
            assertEquals(message, expected.getLineNumberContent(), actual.getLineNumberContent());
            assertEquals(message, expected.getLineNumberContentEnd(), actual.getLineNumberContent());
        }
    }

    private static EPCompiled compileModule(RegressionEnvironment env, Module module) {
        try {
            return env.getCompiler().compile(module, new CompilerArguments(env.getConfiguration()));
        } catch (EPCompileException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Module makeModule(String name, String... statements) {
        ModuleItem[] items = new ModuleItem[statements.length];
        for (int i = 0; i < statements.length; i++) {
            items[i] = new ModuleItem(statements[i], false, 0, 0, 0, 0, 0, 0);
        }
        return new Module(name, null, new HashSet<>(), new HashSet<>(), Arrays.asList(items), null);
    }
}
