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
package com.espertech.esper.regressionlib.suite.expr.clazz;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.compiler.client.option.InlinedClassInspectionContext;
import com.espertech.esper.compiler.client.option.InlinedClassInspectionOption;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExprClassStaticMethod {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprClassStaticMethodLocal(false));
        executions.add(new ExprClassStaticMethodLocal(true));
        executions.add(new ExprClassStaticMethodCreate(false));
        executions.add(new ExprClassStaticMethodCreate(true));
        executions.add(new ExprClassStaticMethodCreateCompileVsRuntime());
        executions.add(new ExprClassStaticMethodLocalFAFQuery());
        executions.add(new ExprClassStaticMethodCreateFAFQuery());
        executions.add(new ExprClassStaticMethodLocalWithPackageName());
        executions.add(new ExprClassStaticMethodCreateClassWithPackageName());
        executions.add(new ExprClassStaticMethodLocalAndCreateClassTogether());
        executions.add(new ExprClassInvalidCompile());
        executions.add(new ExprClassDocSamples());
        executions.add(new ExprClassCompilerInlinedClassInspectionOption());
        return executions;
    }

    private static class ExprClassCompilerInlinedClassInspectionOption implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "inlined_class \"\"\"\n" +
                "  import java.io.File;" +
                "  import java.util.Arrays;" +
                "  public class MyUtility {\n" +
                "    public static void fib(int n) {\n" +
                "      System.out.println(Arrays.asList(new File(\".\").list()));\n" +
                "    }\n" +
                "  }\n" +
                "\"\"\"\n" +
                "@name('s0') select MyUtility.fib(intPrimitive) from SupportBean";

            MySupportInlinedClassInspection support = new MySupportInlinedClassInspection();
            env.compile(epl, compilerOptions -> compilerOptions.setInlinedClassInspection(support));

            env.assertThat(() -> {
                assertEquals(1, support.contexts.size());
                InlinedClassInspectionContext ctx = support.contexts.get(0);
                assertEquals("MyUtility", ctx.getJaninoClassFiles()[0].getThisClassName());
            });
        }
    }

    private static class ExprClassDocSamples implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "inlined_class \"\"\"\n" +
                "  public class MyUtility {\n" +
                "    public static double fib(int n) {\n" +
                "      if (n <= 1)\n" +
                "        return n;\n" +
                "      return fib(n-1) + fib(n-2);\n" +
                "    }\n" +
                "  }\n" +
                "\"\"\"\n" +
                "select MyUtility.fib(intPrimitive) from SupportBean";
            env.compile(epl);

            RegressionPath path = new RegressionPath();
            String eplCreate = "@public create inlined_class \"\"\" \n" +
                "  public class MyUtility {\n" +
                "    public static double midPrice(double buy, double sell) {\n" +
                "      return (buy + sell) / 2;\n" +
                "    }\n" +
                "  }\n" +
                "\"\"\"";
            env.compile(eplCreate, path);
            env.compile("select MyUtility.midPrice(doublePrimitive, doubleBoxed) from SupportBean", path);
        }
    }

    private static class ExprClassStaticMethodLocalAndCreateClassTogether implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplClass = "inlined_class \"\"\"\n" +
                "    public class MyUtil {\n" +
                "        public static String returnBubba() {\n" +
                "            return \"bubba\";\n" +
                "        }\n" +
                "    }\n" +
                "\"\"\" \n" +
                "@public create inlined_class \"\"\"\n" +
                "    public class MyClass {\n" +
                "        public static String doIt() {\n" +
                "            return \"|\" + MyUtil.returnBubba() + \"|\";\n" +
                "        }\n" +
                "    }\n" +
                "\"\"\"\n";
            env.compileDeploy(eplClass, path);

            String epl = "@name('s0') select MyClass.doIt() as c0 from SupportBean\n";
            env.compileDeploy(epl, path).addListener("s0");

            sendSBAssert(env, "E1", 1, "|bubba|");

            env.undeployAll();
        }
    }

    private static class ExprClassStaticMethodLocalWithPackageName implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') inlined_class \"\"\"\n" +
                            "    package mypackage;" +
                            "    public class MyUtil {\n" +
                            "        public static String doIt() {\n" +
                            "            return \"test\";\n" +
                            "        }\n" +
                            "    }\n" +
                            "\"\"\" \n" +
                            "select mypackage.MyUtil.doIt() as c0 from SupportBean\n";
            env.compileDeploy(epl).addListener("s0");

            sendSBAssert(env, "E1", 1, "test");

            env.undeployAll();
        }
    }

    private static class ExprClassStaticMethodCreateClassWithPackageName implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                    "create inlined_class \"\"\"\n" +
                    "    package mypackage;" +
                    "    public class MyUtil {\n" +
                    "        public static String doIt(String theString, int intPrimitive) {\n" +
                    "            return theString + Integer.toString(intPrimitive);\n" +
                    "        }\n" +
                    "    }\n" +
                    "\"\"\";\n" +
                    "@name('s0') select mypackage.MyUtil.doIt(theString, intPrimitive) as c0 from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");

            sendSBAssert(env, "E1", 1, "E11");

            env.undeployAll();
        }
    }

    private static class ExprClassStaticMethodCreateFAFQuery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplWindow =
                    "@public create inlined_class \"\"\"\n" +
                            "    public class MyClass {\n" +
                            "        public static String doIt(String parameter) {\n" +
                            "            return \"abc\";\n" +
                            "        }\n" +
                            "    }\n" +
                            "\"\"\";\n" +
                            "@public create window MyWindow#keepall as (theString string);\n" +
                            "on SupportBean merge MyWindow insert select theString;\n";
            env.compileDeploy(eplWindow, path);

            env.sendEventBean(new SupportBean("E1", 1));

            String eplFAF = "select MyClass.doIt(theString) as c0 from MyWindow";
            EPFireAndForgetQueryResult result = env.compileExecuteFAF(eplFAF, path);
            assertEquals("abc", result.getArray()[0].get("c0"));

            env.milestone(0);

            result = env.compileExecuteFAF(eplFAF, path);
            assertEquals("abc", result.getArray()[0].get("c0"));

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.FIREANDFORGET);
        }
    }

    private static class ExprClassStaticMethodLocalFAFQuery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplWindow = "@public create window MyWindow#keepall as (theString string);\n" +
                    "on SupportBean merge MyWindow insert select theString;\n";
            env.compileDeploy(eplWindow, path);

            env.sendEventBean(new SupportBean("E1", 1));

            String eplFAF = "inlined_class \"\"\"\n" +
                    "    public class MyClass {\n" +
                    "        public static String doIt(String parameter) {\n" +
                    "            return '>' + parameter + '<';\n" +
                    "        }\n" +
                    "    }\n" +
                    "\"\"\"\n select MyClass.doIt(theString) as c0 from MyWindow";
            EPFireAndForgetQueryResult result = env.compileExecuteFAF(eplFAF, path);
            assertEquals(">E1<", result.getArray()[0].get("c0"));

            env.milestone(0);

            result = env.compileExecuteFAF(eplFAF, path);
            assertEquals(">E1<", result.getArray()[0].get("c0"));

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.FIREANDFORGET);
        }
    }

    private static class ExprClassInvalidCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // we allow empty class text
            env.compile("inlined_class \"\"\" \"\"\" select * from SupportBean");

            // invalid class
            env.tryInvalidCompile("inlined_class \"\"\" x \"\"\" select * from SupportBean",
                    "Failed to compile class: Line 1, Column 2: One of 'class enum interface @' expected instead of 'x' for class [\"\"\" x \"\"\"]");

            // invalid already deployed
            RegressionPath path = new RegressionPath();
            String createClassEPL = "@public create inlined_class \"\"\" public class MyClass {}\"\"\"";
            env.compile(createClassEPL, path);
            env.tryInvalidCompile(path, createClassEPL,
                "Class 'MyClass' has already been declared");

            // duplicate local class
            String eplDuplLocal = "inlined_class \"\"\" class MyDuplicate{} \"\"\" inlined_class \"\"\" class MyDuplicate{} \"\"\" select * from SupportBean";
            env.tryInvalidCompile(eplDuplLocal, "Duplicate class by name 'MyDuplicate'");

            // duplicate local class and create-class class
            String eplDuplLocalAndCreate = "inlined_class \"\"\" class MyDuplicate{} \"\"\" create inlined_class \"\"\" class MyDuplicate{} \"\"\"";
            env.tryInvalidCompile(eplDuplLocalAndCreate, "Duplicate class by name 'MyDuplicate'");

            // duplicate create-class class
            String eplDuplCreate = "create inlined_class \"\"\" public class MyDuplicate{} \"\"\";\n" +
                "create inlined_class \"\"\" public class MyDuplicate{} \"\"\";\n";
            env.tryInvalidCompile(eplDuplCreate, "Class 'MyDuplicate' has already been declared");
        }
    }

    private static class ExprClassStaticMethodCreateCompileVsRuntime implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplTemplate = "@public create inlined_class \"\"\"\n" +
                    "    public class MyClass {\n" +
                    "        public static int doIt(int parameter) {\n" +
                    "            return %REPLACE%;\n" +
                    "        }\n" +
                    "    }\n" +
                    "\"\"\"\n";
            EPCompiled compiledReturnZero = env.compile(eplTemplate.replace("%REPLACE%", "0"));
            EPCompiled compiledReturnPlusOne = env.compile(eplTemplate.replace("%REPLACE%", "parameter+1"));

            RegressionPath path = new RegressionPath();
            path.add(compiledReturnZero);
            EPCompiled compiledQuery = env.compile("@name('s0') select MyClass.doIt(intPrimitive) as c0 from SupportBean;\n", path);
            env.deploy(compiledReturnPlusOne);
            env.deploy(compiledQuery).addListener("s0");

            sendSBAssert(env, "E1", 10, 11);

            env.milestone(0);

            sendSBAssert(env, "E2", 20, 21);

            env.undeployAll();
        }
    }

    private static class ExprClassStaticMethodCreate implements RegressionExecution {
        private final boolean soda;

        public ExprClassStaticMethodCreate(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplClass = "@public create inlined_class \"\"\"\n" +
                    "    public class MyClass {\n" +
                    "        public static String doIt(String parameter) {\n" +
                    "            return \"|\" + parameter + \"|\";\n" +
                    "        }\n" +
                    "    }\n" +
                    "\"\"\"";
            env.compileDeploy(soda, eplClass, path);
            env.compileDeploy(soda, "@name('s0') select MyClass.doIt(theString) as c0 from SupportBean", path);
            env.addListener("s0");

            sendSBAssert(env, "E1", 0, "|E1|");

            env.milestone(0);

            sendSBAssert(env, "E2", 0, "|E2|");

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    private static class ExprClassStaticMethodLocal implements RegressionExecution {
        private final boolean soda;

        public ExprClassStaticMethodLocal(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') inlined_class \"\"\"\n" +
                    "    public class MyClass {\n" +
                    "        public static String doIt(String parameter) {\n" +
                    "            return \"|\" + parameter + \"|\";\n" +
                    "        }\n" +
                    "    }\n" +
                    "\"\"\" " +
                    "select MyClass.doIt(theString) as c0 from SupportBean\n";
            env.compileDeploy(soda, epl).addListener("s0");

            sendSBAssert(env, "E1", 0, "|E1|");

            env.milestone(0);

            sendSBAssert(env, "E2", 0, "|E2|");

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    private static void sendSBAssert(RegressionEnvironment env, String theString, int intPrimitive, Object expected) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
        env.assertEqualsNew("s0", "c0", expected);
    }

    private static class MyClass {
        public String doIt(String parameter) {
            return "|" + parameter + "|";
        }
    }

    private static class MySupportInlinedClassInspection implements InlinedClassInspectionOption {
        private List<InlinedClassInspectionContext> contexts = new ArrayList<>();

        public void visit(InlinedClassInspectionContext env) {
            contexts.add(env);
        }
    }
}
