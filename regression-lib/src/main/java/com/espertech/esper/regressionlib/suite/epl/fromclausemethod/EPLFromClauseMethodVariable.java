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
package com.espertech.esper.regressionlib.suite.epl.fromclausemethod;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S2;

import java.io.Serializable;
import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

public class EPLFromClauseMethodVariable {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLFromClauseMethodConstantVariable());
        execs.add(new EPLFromClauseMethodNonConstantVariable(true));
        execs.add(new EPLFromClauseMethodNonConstantVariable(false));
        execs.add(new EPLFromClauseMethodContextVariable());
        execs.add(new EPLFromClauseMethodVariableMapAndOA());
        execs.add(new EPLFromClauseMethodVariableInvalid());
        return execs;
    }

    private static class EPLFromClauseMethodVariableInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // invalid footprint
            tryInvalidCompile(env, "select * from method:MyConstantServiceVariable.fetchABean() as h0",
                "Method footprint does not match the number or type of expression parameters, expecting no parameters in method: Could not find enumeration method, date-time method or instance method named 'fetchABean' in class '" + MyConstantServiceVariable.class.getName() + "' taking no parameters (nearest match found was 'fetchABean' taking type(s) 'int') [");

            // null variable value and metadata is instance method
            tryInvalidCompile(env, "select field1, field2 from method:MyNullMap.getMapData()",
                "Failed to access variable method invocation metadata: The variable value is null and the metadata method is an instance method");

            // variable with context and metadata is instance method
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context BetweenStartAndEnd start SupportBean end SupportBean", path);
            env.compileDeploy("context BetweenStartAndEnd create variable " + MyMethodHandlerMap.class.getName() + " themap", path);
            tryInvalidCompile(env, path, "context BetweenStartAndEnd select field1, field2 from method:themap.getMapData()",
                "Failed to access variable method invocation metadata: The variable value is null and the metadata method is an instance method");

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodVariableMapAndOA implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            for (String epl : new String[]{
                "@name('s0') select field1, field2 from method:MyMethodHandlerMap.getMapData()",
                "@name('s0') select field1, field2 from method:MyMethodHandlerOA.getOAData()"
            }) {
                env.compileDeploy(epl);
                EPAssertionUtil.assertProps(env.iterator("s0").next(), "field1,field2".split(","), new Object[]{"a", "b"});
                env.undeployAll();
            }
        }
    }

    private static class EPLFromClauseMethodContextVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context MyContext " +
                "initiated by SupportBean_S0 as c_s0 " +
                "terminated by SupportBean_S1(id=c_s0.id)", path);
            env.compileDeploy("context MyContext " +
                "create variable MyNonConstantServiceVariable var = MyNonConstantServiceVariableFactory.make()", path);
            env.compileDeploy("@name('s0') context MyContext " +
                "select id as c0 from SupportBean(intPrimitive=context.c_s0.id) as sb, " +
                "method:var.fetchABean(intPrimitive) as h0", path).addListener("s0");
            env.compileDeploy("context MyContext on SupportBean_S2(id = context.c_s0.id) set var.postfix=p20", path);

            env.sendEventBean(new SupportBean_S0(1));
            env.sendEventBean(new SupportBean_S0(2));

            sendEventAssert(env, "E1", 1, "_1_context_postfix");

            env.milestone(0);

            sendEventAssert(env, "E2", 2, "_2_context_postfix");

            env.sendEventBean(new SupportBean_S2(1, "a"));
            env.sendEventBean(new SupportBean_S2(2, "b"));

            env.milestone(1);

            sendEventAssert(env, "E1", 1, "_1_a");
            sendEventAssert(env, "E2", 2, "_2_b");

            // invalid context
            tryInvalidCompile(env, path, "select * from method:var.fetchABean(intPrimitive) as h0",
                "Variable by name 'var' has been declared for context 'MyContext' and can only be used within the same context");
            env.compileDeploy("create context ABC start @now end after 1 minute", path);
            tryInvalidCompile(env, path, "context ABC select * from method:var.fetchABean(intPrimitive) as h0",
                "Variable by name 'var' has been declared for context 'MyContext' and can only be used within the same context");

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodConstantVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select id as c0 from SupportBean as sb, " +
                "method:MyConstantServiceVariable.fetchABean(intPrimitive) as h0";
            env.compileDeploy(epl).addListener("s0");

            sendEventAssert(env, "E1", 10, "_10_");

            env.milestone(0);

            sendEventAssert(env, "E2", 20, "_20_");

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodNonConstantVariable implements RegressionExecution {
        private final boolean soda;

        public EPLFromClauseMethodNonConstantVariable(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String modifyEPL = "on SupportBean_S0 set MyNonConstantServiceVariable.postfix=p00";
            env.compileDeploy(soda, modifyEPL);

            String epl = "@name('s0') select id as c0 from SupportBean as sb, " +
                "method:MyNonConstantServiceVariable.fetchABean(intPrimitive) as h0";
            env.compileDeploy(soda, epl).addListener("s0");

            sendEventAssert(env, "E1", 10, "_10_postfix");

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(1, "newpostfix"));
            sendEventAssert(env, "E1", 20, "_20_newpostfix");

            env.milestone(1);

            // return to original value
            env.sendEventBean(new SupportBean_S0(2, "postfix"));
            sendEventAssert(env, "E1", 30, "_30_postfix");

            env.undeployAll();
        }
    }

    private static void sendEventAssert(RegressionEnvironment env, String theString, int intPrimitive, String expected) {
        String[] fields = "c0".split(",");
        env.sendEventBean(new SupportBean(theString, intPrimitive));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{expected});
    }

    public static class MyConstantServiceVariable implements Serializable {
        public SupportBean_A fetchABean(int intPrimitive) {
            return new SupportBean_A("_" + intPrimitive + "_");
        }
    }

    public static class MyNonConstantServiceVariable implements Serializable {
        private String postfix;

        public MyNonConstantServiceVariable(String postfix) {
            this.postfix = postfix;
        }

        public void setPostfix(String postfix) {
            this.postfix = postfix;
        }

        public String getPostfix() {
            return postfix;
        }

        public SupportBean_A fetchABean(int intPrimitive) {
            return new SupportBean_A("_" + intPrimitive + "_" + postfix);
        }
    }

    public static class MyStaticService {
        public static SupportBean_A fetchABean(int intPrimitive) {
            return new SupportBean_A("_" + intPrimitive + "_");
        }
    }

    public static class MyNonConstantServiceVariableFactory {
        public static MyNonConstantServiceVariable make() {
            return new MyNonConstantServiceVariable("context_postfix");
        }
    }

    public static class MyMethodHandlerMap implements Serializable {
        private final String field1;
        private final String field2;

        public MyMethodHandlerMap(String field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        public Map<String, Object> getMapDataMetadata() {
            Map<String, Object> fields = new HashMap<String, Object>();
            fields.put("field1", String.class);
            fields.put("field2", String.class);
            return fields;
        }

        public Map<String, Object>[] getMapData() {
            Map[] maps = new Map[1];
            HashMap<String, Object> row = new HashMap<String, Object>();
            maps[0] = row;
            row.put("field1", field1);
            row.put("field2", field2);
            return maps;
        }
    }

    public static class MyMethodHandlerOA implements Serializable {
        private final String field1;
        private final String field2;

        public MyMethodHandlerOA(String field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        public static LinkedHashMap<String, Object> getOADataMetadata() {
            LinkedHashMap<String, Object> fields = new LinkedHashMap<String, Object>();
            fields.put("field1", String.class);
            fields.put("field2", String.class);
            return fields;
        }

        public Object[][] getOAData() {
            return new Object[][]{{field1, field2}};
        }
    }
}
