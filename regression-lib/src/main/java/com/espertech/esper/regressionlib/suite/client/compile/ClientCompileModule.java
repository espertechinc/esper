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
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPDeployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
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
        return execs;
    }

    private static class ClientCompileModuleCommentTrailing implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final String epl =
                "@public @buseventtype create map schema Fubar as (foo String, bar Double);" + System.lineSeparator()
                    + "/** comment after */";
            env.compileDeploy(epl).undeployAll();
        }
    }

    private static class ClientCompileModuleTwoModules implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String resource = "regression/test_module_12.epl";
            InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
            assertNotNull(input);

            Module module;
            try {
                module = EPCompilerProvider.getCompiler().readModule(input, resource);
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
            moduleTwo.setUses(new HashSet<>(Arrays.asList("a", "b")));
            moduleTwo.setImports(new HashSet<>(Arrays.asList("c", "d")));
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
    }

    private static class ClientCompileModuleLineNumberAndComments implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String moduleText = NEWLINE + NEWLINE + "select * from ABC;" +
                NEWLINE + "select * from DEF";

            Module module = env.parseModule(moduleText);
            assertEquals(2, module.getItems().size());
            assertEquals(3, module.getItems().get(0).getLineNumber());
            assertEquals(4, module.getItems().get(1).getLineNumber());

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
    }

    private static class ClientCompileModuleParse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Module module = env.readModule("regression/test_module_4.epl");
            assertModule(module, null, "abd", null, new String[]{
                "select * from ABC",
                "/* Final comment */"
                }, new boolean[]{false, true},
                new int[]{3, 8},
                new int[]{12, 0},
                new int[]{37, 0}
            );

            module = env.readModule("regression/test_module_1.epl");
            assertModule(module, "abc", "def,jlk", null, new String[]{
                "select * from A",
                "select * from B" + NEWLINE + "where C=d",
                "/* Test ; Comment */" + NEWLINE + "update ';' where B=C",
                "update D"
                }
            );

            module = env.readModule("regression/test_module_2.epl");
            assertModule(module, "abc.def.hij", "def.hik,jlk.aja", null, new String[]{
                "// Note 4 white spaces after * and before from" + NEWLINE + "select * from A",
                "select * from B",
                "select *    " + NEWLINE + "    from C",
                }
            );

            module = env.readModule("regression/test_module_3.epl");
            assertModule(module, null, null, null, new String[]{
                "create window ABC",
                "select * from ABC"
                }
            );

            module = env.readModule("regression/test_module_5.epl");
            assertModule(module, "abd.def", null, null, new String[0]);

            module = env.readModule("regression/test_module_6.epl");
            assertModule(module, null, null, null, new String[0]);

            module = env.readModule("regression/test_module_7.epl");
            assertModule(module, null, null, null, new String[0]);

            module = env.readModule("regression/test_module_8.epl");
            assertModule(module, "def.jfk", null, null, new String[0]);

            module = env.parseModule("module mymodule; uses mymodule2; import abc; select * from MyEvent;");
            assertModule(module, "mymodule", "mymodule2", "abc", new String[]{
                "select * from MyEvent"
            });

            module = env.readModule("regression/test_module_11.epl");
            assertModule(module, null, null, "com.mycompany.pck1", new String[0]);

            module = env.readModule("regression/test_module_10.epl");
            assertModule(module, "abd.def", "one.use,two.use", "com.mycompany.pck1,com.mycompany.*", new String[]{
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
    }

    private static class ClientCompileModuleParseFail implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidIO("regression/dummy_not_there.epl",
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
    }

    private static void tryInvalidControlCharacters(RegressionEnvironment env) {
        String epl = "select * \u008F from SupportBean";
        tryInvalidCompile(env, epl, "Failed to parse: Unrecognized control characters found in text, failed to parse text ");
    }

    private static void tryInvalidIO(String resource, String message) {
        try {
            EPCompilerProvider.getCompiler().readModule(resource, ClientCompileModule.class.getClassLoader());
            fail();
        } catch (IOException ex) {
            assertEquals(message, ex.getMessage());
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void tryInvalidParse(RegressionEnvironment env, String resource, String message) {
        try {
            EPCompilerProvider.getCompiler().readModule(resource, env.getClass().getClassLoader());
            fail();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (ParseException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    private static void assertModule(Module module, String name, String usesCSV, String importsCSV, String[] statements) {
        assertModule(module, name, usesCSV, importsCSV, statements, new boolean[statements.length], new int[statements.length], new int[statements.length], new int[statements.length]);
    }

    private static void assertModule(Module module, String name, String usesCSV, String importsCSV, String[] statementsExpected,
                                     boolean[] commentsExpected,
                                     int[] lineNumsExpected,
                                     int[] charStartsExpected,
                                     int[] charEndsExpected) {
        assertEquals(name, module.getName());

        String[] expectedUses = usesCSV == null ? new String[0] : usesCSV.split(",");
        EPAssertionUtil.assertEqualsExactOrder(expectedUses, module.getUses().toArray());

        String[] expectedImports = importsCSV == null ? new String[0] : importsCSV.split(",");
        EPAssertionUtil.assertEqualsExactOrder(expectedImports, module.getImports().toArray());

        String[] stmtsFound = new String[module.getItems().size()];
        boolean[] comments = new boolean[module.getItems().size()];
        int[] lineNumsFound = new int[module.getItems().size()];
        int[] charStartsFound = new int[module.getItems().size()];
        int[] charEndsFound = new int[module.getItems().size()];

        for (int i = 0; i < module.getItems().size(); i++) {
            stmtsFound[i] = module.getItems().get(i).getExpression();
            comments[i] = module.getItems().get(i).isCommentOnly();
            lineNumsFound[i] = module.getItems().get(i).getLineNumber();
            charStartsFound[i] = module.getItems().get(i).getCharPosStart();
            charEndsFound[i] = module.getItems().get(i).getCharPosEnd();
        }

        EPAssertionUtil.assertEqualsExactOrder(statementsExpected, stmtsFound);
        EPAssertionUtil.assertEqualsExactOrder(commentsExpected, comments);

        boolean isCompareLineNums = false;
        for (int l : lineNumsExpected) {
            if (l > 0) {
                isCompareLineNums = true;
            }
        }
        if (isCompareLineNums) {
            EPAssertionUtil.assertEqualsExactOrder(lineNumsExpected, lineNumsFound);
            // Start and end character position can be platform-dependent
            // commented-out: EPAssertionUtil.assertEqualsExactOrder(charStartsExpected, charStartsFound);
            // commented-out: EPAssertionUtil.assertEqualsExactOrder(charEndsExpected, charEndsFound);
        }
    }

    private static EPCompiled compileModule(RegressionEnvironment env, Module module) {
        try {
            return EPCompilerProvider.getCompiler().compile(module, new CompilerArguments(env.getConfiguration()));
        } catch (EPCompileException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Module makeModule(String name, String... statements) {
        ModuleItem[] items = new ModuleItem[statements.length];
        for (int i = 0; i < statements.length; i++) {
            items[i] = new ModuleItem(statements[i], false, 0, 0, 0);
        }
        return new Module(name, null, new HashSet<>(), new HashSet<>(), Arrays.asList(items), null);
    }
}
