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
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.assertMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExprClassForEPLObjects {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprClassResolutionFromClauseMethod());
        executions.add(new ExprClassResolutionOutputColType());
        executions.add(new ExprClassResolutionInvalid());
        executions.add(new ExprClassResolutionScript());
        return executions;
    }

    private static class ExprClassResolutionOutputColType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "inlined_class \"\"\"\n" +
                    "  public class MyBean {\n" +
                    "    private final int id;" +
                    "    public MyBean(int id) {this.id = id;}\n" +
                    "    public int getId() {return id;}\n" +
                    "    public static MyBean getBean(int id) {return new MyBean(id);}\n" +
                    "  }\n" +
                    "\"\"\" \n" +
                    "@name('s0') select MyBean.getBean(intPrimitive) as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");
            env.assertStatement("s0", statement -> assertEquals("MyBean", statement.getEventType().getPropertyType("c0").getSimpleName()));

            env.sendEventBean(new SupportBean("E1", 10));
            env.assertEventNew("s0", event -> {
                Object result = event.get("c0");
                try {
                    assertEquals(10, result.getClass().getMethod("getId").invoke(result));
                } catch (Throwable t) {
                    fail(t.getMessage());
                }
            });

            env.undeployAll();
        }
    }

    private static class ExprClassResolutionInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test Annotation
            String eplAnnotation = escapeClass("public @interface MyAnnotation{}") +
                "@MyAnnotation @name('s0') select * from SupportBean\n";
            env.tryInvalidCompile(eplAnnotation, "Failed to process statement annotations: Failed to resolve @-annotation class: Could not load annotation class by name 'MyAnnotation', please check imports");

            // test Create-Schema for bean type
            String eplBeanEventType = escapeClass("public class MyEventBean {}") +
                "create schema MyEvent as MyEventBean\n";
            env.tryInvalidCompile(eplBeanEventType, "Could not load class by name 'MyEventBean', please check imports");

            // test Create-Schema for property type
            String eplPropertyType = escapeClass("public class MyEventBean {}") +
                "create schema MyEvent as (field1 MyEventBean)\n";
            env.tryInvalidCompile(eplPropertyType, "Nestable type configuration encountered an unexpected property type name");

            String eplNamedWindow = escapeClass("public class MyType {}") +
                "create window MyWindow(myfield MyType)\n";
            env.tryInvalidCompile(eplNamedWindow, "Nestable type configuration encountered an unexpected property type name");

            String eplTable = escapeClass("public class MyType {}") +
                "create table MyTable(myfield MyType)\n";
            env.tryInvalidCompile(eplTable, "skip");
        }
    }

    private static class ExprClassResolutionScript implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplScript = escapeClass("public class MyScriptResult {}") +
                "expression Object[] js:myItemProducerScript() [\n" +
                "myItemProducerScript();" +
                "function myItemProducerScript() {" +
                "  var arrayType = Java.type(\"MyScriptResult\");\n" +
                "  var rows = new arrayType(2);\n" +
                "  return rows;\n" +
                "}]" +
                "@name('s0') select myItemProducerScript() from SupportBean";
            env.compileDeploy(eplScript).addListener("s0");

            env.assertThat(() -> {
                try {
                    env.sendEventBean(new SupportBean("E1", 1));
                    fail();
                } catch (EPException ex) {
                    assertMessage(ex, "java.lang.RuntimeException: Unexpected exception in statement 's0'");
                }
            });

            env.undeployAll();
        }
    }

    private static class ExprClassResolutionFromClauseMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCreateClass =
                "@public create inlined_class \"\"\"\n" +
                    "  public class MyFromClauseMethod {\n" +
                    "    public static MyBean[] getBeans() {\n" +
                    "       return new MyBean[] {new MyBean(1), new MyBean(2)};\n" +
                    "    }\n" +
                    "    public static class MyBean {\n" +
                    "      private final int id;" +
                    "      public MyBean(int id) {this.id = id;}\n" +
                    "      public int getId() {return id;}\n" +
                    "    }\n" +
                    "  }\n" +
                    "\"\"\" \n";
            env.compileDeploy(eplCreateClass, path);

            String epl = "@name('s0')" +
                "@name('s0') select s.id as c0 from SupportBean as e,\n" +
                "method:MyFromClauseMethod.getBeans() as s";
            EPCompiled compiled = env.compile(epl, path);
            for (Map.Entry<String, byte[]> classEntry : compiled.getClasses().entrySet()) {
                if (classEntry.getKey().contains("MyFromClauseMethod")) {
                    fail("EPCompiled should not contain create-class class");
                }
            }
            env.deploy(compiled).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            env.assertPropsPerRowLastNew("s0", "c0".split(","), new Object[][]{{1}, {2}});

            env.undeployAll();
        }
    }

    private static String escapeClass(String text) {
        return "inlined_class \"\"\"\n" + text + "\"\"\" \n";
    }
}
