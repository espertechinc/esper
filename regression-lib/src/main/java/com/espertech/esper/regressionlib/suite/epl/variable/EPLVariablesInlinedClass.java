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
package com.espertech.esper.regressionlib.suite.epl.variable;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanNumeric;
import com.espertech.esper.regressionlib.support.client.SupportDeploymentDependencies;
import com.espertech.esper.runtime.client.util.EPObjectType;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.assertProps;

public class EPLVariablesInlinedClass {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLVariablesInlinedClassLocal());
        execs.add(new EPLVariablesInlinedClassGlobal());
        return execs;
    }

    private static class EPLVariablesInlinedClassGlobal implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplClass = "@public @name('clazz') create inlined_class \"\"\"\n" +
                "public class MyStateful implements java.io.Serializable {\n" +
                "    private String value = \"X\";\n" +
                "    public String getValue() {return value;}\n" +
                "    public void setValue(String value) {this.value = value;}\n" +
                "}\n" +
                "\"\"\"\n";
            env.compileDeploy(eplClass, path);

            String epl = "create variable MyStateful msf = new MyStateful();\n" +
                "@name('s0') select msf.value as c0 from SupportBean;\n" +
                "on SupportBean_S0 set msf.setValue(p00);\n";
            env.compileDeploy(epl, path).addListener("s0");

            sendAssert(env, "X");
            env.sendEventBean(new SupportBean_S0(1, "A"));
            sendAssert(env, "A");

            env.milestone(0);

            sendAssert(env, "A");
            env.sendEventBean(new SupportBean_S0(2, "B"));
            sendAssert(env, "B");

            SupportDeploymentDependencies.assertSingle(env, "s0", "clazz", EPObjectType.CLASSPROVIDED, "MyStateful");

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String expected) {
            env.sendEventBean(new SupportBean());
            assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[] {expected});
        }
    }

    private static class EPLVariablesInlinedClassLocal implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "inlined_class \"\"\"\n" +
                "public class MyStateful implements java.io.Serializable {\n" +
                "    private final int a;\n" +
                "    private final int b;\n" +
                "    public MyStateful(int a, int b) {\n" +
                "        this.a = a;\n" +
                "        this.b = b;\n" +
                "    }\n" +
                "    public int getA() {return a;}\n" +
                "    public int getB() {return b;}\n" +
                "    public void setA(int a) {this.a = a;}\n" +
                "    public void setB(int b) {this.b = b;}\n" +
                "}\n" +
                "\"\"\"\n" +
                "create variable MyStateful msf = new MyStateful(2, 3);\n" +
                "@name('s0') select msf.a as c0, msf.b as c1 from SupportBean;\n" +
                "on SupportBeanNumeric set msf.setA(intOne), msf.setB(intTwo);\n";
            env.compileDeploy(epl).addListener("s0");

            sendAssert(env, 2, 3);

            env.milestone(0);

            sendAssert(env, 2, 3);
            env.sendEventBean(new SupportBeanNumeric(10, 20));
            sendAssert(env, 10, 20);

            env.milestone(1);

            sendAssert(env, 10, 20);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, int expectedA, int expectedB) {
            env.sendEventBean(new SupportBean());
            assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {expectedA, expectedB});
        }
    }
}
