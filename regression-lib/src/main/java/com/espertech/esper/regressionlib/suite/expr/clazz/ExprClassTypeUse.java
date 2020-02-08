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

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExprClassTypeUse {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprClassTypeUseEnum());
        executions.add(new ExprClassTypeConst());
        executions.add(new ExprClassTypeInnerClass());
        executions.add(new ExprClassTypeNewKeyword());
        return executions;
    }

    private static class ExprClassTypeNewKeyword implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "public class MyResult {\n" +
                "  private final String id;\n" +
                "  public MyResult(String id) {this.id = id;}\n" +
                "  public String getId() {return id;}\n" +
                "}";
            String epl = escapeClass(text) +
                "@name('s0') select new MyResult(theString) as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            Object result = env.listener("s0").assertOneGetNewAndReset().get("c0");
            try {
                assertEquals("E1", result.getClass().getMethod("getId").invoke(result));
            } catch (Throwable t) {
                fail(t.getMessage());
            }

            env.undeployAll();
        }
    }

    private static class ExprClassTypeInnerClass implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "public class MyConstants {\n" +
                "  public static class MyInnerClass {" +
                "    public final static String VALUE = \"abc\";\n" +
                "  }" +
                "}";
            String epl = escapeClass(text) +
                "@name('s0') select MyConstants$MyInnerClass.VALUE as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            sendSBAssert(env, "E1", 0, "abc");

            env.undeployAll();
        }
    }

    private static class ExprClassTypeConst implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "public class MyConstants {\n" +
                "  public final static String VALUE = \"test\";\n" +
                "}";
            String epl = escapeClass(text) +
                "@name('s0') select MyConstants.VALUE as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            sendSBAssert(env, "E1", 0, "test");

            env.undeployAll();
        }
    }

    private static class ExprClassTypeUseEnum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "public enum MyLevel {\n" +
                "  HIGH(3), MEDIUM(2), LOW(1);\n" +
                "  final int levelCode;\n" +
                "  MyLevel(int levelCode) {this.levelCode = levelCode;}\n" +
                "  public int getLevelCode() {return levelCode;}\n" +
                "}";
            String epl = escapeClass(text) +
                "@name('s0') select MyLevel.MEDIUM.getLevelCode() as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            sendSBAssert(env, "E1", 0, 2);

            env.undeployAll();
        }
    }

    private static String escapeClass(String text) {
        return "inlined_class \"\"\"\n" + text + "\"\"\" \n";
    }

    private static void sendSBAssert(RegressionEnvironment env, String theString, int intPrimitive, Object expected) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
        assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
    }
}
