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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.util.*;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableCountMinSketch {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraFrequencyAndTopk());
        execs.add(new InfraDocSamples());
        execs.add(new InfraNonStringType());
        execs.add(new InfraInvalid());
        return execs;
    }

    private static class InfraDocSamples implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema WordEvent (word string)", path);
            env.compileDeploy("create schema EstimateWordCountEvent (word string)", path);

            env.compileDeploy("create table WordCountTable(wordcms countMinSketch())", path);
            env.compileDeploy("create table WordCountTable2(wordcms countMinSketch({\n" +
                "  epsOfTotalCount: 0.000002,\n" +
                "  confidence: 0.999,\n" +
                "  seed: 38576,\n" +
                "  topk: 20,\n" +
                "  agent: '" + CountMinSketchAgentStringUTF16Forge.class.getName() + "'" +
                "}))", path);
            env.compileDeploy("into table WordCountTable select countMinSketchAdd(word) as wordcms from WordEvent", path);
            env.compileDeploy("select WordCountTable.wordcms.countMinSketchFrequency(word) from EstimateWordCountEvent", path);
            env.compileDeploy("select WordCountTable.wordcms.countMinSketchTopk() from pattern[every timer:interval(10 sec)]", path);

            env.undeployAll();
        }
    }

    private static class InfraNonStringType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplTable = "create table MyApproxNS(bytefreq countMinSketch({" +
                "  epsOfTotalCount: 0.02," +
                "  confidence: 0.98," +
                "  topk: null," +
                "  agent: '" + MyBytesPassthruAgentForge.class.getName() + "'" +
                "}))";
            env.compileDeploy(eplTable, path);

            String eplInto = "into table MyApproxNS select countMinSketchAdd(body) as bytefreq from SupportByteArrEventStringId(id='A')";
            env.compileDeploy(eplInto, path);

            String eplRead = "@name('s0') select MyApproxNS.bytefreq.countMinSketchFrequency(body) as freq from SupportByteArrEventStringId(id='B')";
            env.compileDeploy(eplRead, path).addListener("s0");

            env.sendEventBean(new SupportByteArrEventStringId("A", new byte[]{1, 2, 3}));

            env.milestone(0);

            env.sendEventBean(new SupportByteArrEventStringId("B", new byte[]{0, 2, 3}));
            assertEquals(0L, env.listener("s0").assertOneGetNewAndReset().get("freq"));

            env.sendEventBean(new SupportByteArrEventStringId("B", new byte[]{1, 2, 3}));
            assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("freq"));

            env.undeployAll();
        }
    }

    private static class InfraFrequencyAndTopk implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "create table MyApproxFT(wordapprox countMinSketch({topk:3}));\n" +
                    "into table MyApproxFT select countMinSketchAdd(theString) as wordapprox from SupportBean;\n" +
                    "@name('frequency') select MyApproxFT.wordapprox.countMinSketchFrequency(p00) as freq from SupportBean_S0;\n" +
                    "@name('topk') select MyApproxFT.wordapprox.countMinSketchTopk() as topk from SupportBean_S1;\n";
            env.compileDeploy(epl, path).addListener("frequency").addListener("topk");

            env.sendEventBean(new SupportBean("E1", 0));
            assertOutput(env, "E1=1", "E1=1");

            env.milestone(0);

            assertOutput(env, "E1=1", "E1=1");
            env.sendEventBean(new SupportBean("E2", 0));

            env.milestone(1);

            assertOutput(env, "E1=1,E2=1", "E1=1,E2=1");

            env.sendEventBean(new SupportBean("E2", 0));
            assertOutput(env, "E1=1,E2=2", "E1=1,E2=2");

            env.sendEventBean(new SupportBean("E3", 0));
            assertOutput(env, "E1=1,E2=2,E3=1", "E1=1,E2=2,E3=1");

            env.milestone(2);

            env.sendEventBean(new SupportBean("E4", 0));
            assertOutput(env, "E1=1,E2=2,E3=1,E4=1", "E1=1,E2=2,E3=1");

            env.sendEventBean(new SupportBean("E4", 0));
            assertOutput(env, "E1=1,E2=2,E3=1,E4=2", "E1=1,E2=2,E4=2");

            // test join
            String eplJoin = "@name('join') select wordapprox.countMinSketchFrequency(s2.p20) as c0 from MyApproxFT, SupportBean_S2 s2 unidirectional";
            env.compileDeploy(eplJoin, path).addListener("join");

            env.milestone(2);

            env.sendEventBean(new SupportBean_S2(0, "E3"));
            assertEquals(1L, env.listener("join").assertOneGetNewAndReset().get("c0"));
            env.undeployModuleContaining("join");

            // test subquery
            String eplSubquery = "@name('subq') select (select wordapprox.countMinSketchFrequency(s2.p20) from MyApproxFT) as c0 from SupportBean_S2 s2";
            env.compileDeploy(eplSubquery, path).addListener("subq");

            env.milestone(3);

            env.sendEventBean(new SupportBean_S2(0, "E3"));
            assertEquals(1L, env.listener("subq").assertOneGetNewAndReset().get("c0"));
            env.undeployModuleContaining("subq");

            env.undeployAll();
        }
    }

    private static class InfraInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table MyCMS(wordcms countMinSketch())", path);

            // invalid "countMinSketch" declarations
            //
            tryInvalidCompile(env, path, "select countMinSketch() from SupportBean",
                "Failed to validate select-clause expression 'countMinSketch()': Count-min-sketch aggregation function 'countMinSketch' can only be used in create-table statements [");
            tryInvalidCompile(env, path, "create table MyTable(cms countMinSketch(5))",
                "Failed to validate table-column expression 'countMinSketch(5)': Count-min-sketch aggregation function 'countMinSketch'  expects either no parameter or a single json parameter object [");
            tryInvalidCompile(env, path, "create table MyTable(cms countMinSketch({xxx:3}))",
                "Failed to validate table-column expression 'countMinSketch({xxx=3})': Unrecognized parameter 'xxx' [");
            tryInvalidCompile(env, path, "create table MyTable(cms countMinSketch({epsOfTotalCount:'a'}))",
                "Failed to validate table-column expression 'countMinSketch({epsOfTotalCount=a})': Property 'epsOfTotalCount' expects an java.lang.Double but receives a value of type java.lang.String [");
            tryInvalidCompile(env, path, "create table MyTable(cms countMinSketch({agent:'a'}))",
                "Failed to validate table-column expression 'countMinSketch({agent=a})': Failed to instantiate agent provider: Could not load class by name 'a', please check imports [");
            tryInvalidCompile(env, path, "create table MyTable(cms countMinSketch({agent:'java.lang.String'}))",
                "Failed to validate table-column expression 'countMinSketch({agent=java.lang.String})': Failed to instantiate agent provider: Class 'java.lang.String' does not implement interface 'com.espertech.esper.common.client.util.CountMinSketchAgentForge' [");

            // invalid "countMinSketchAdd" declarations
            //
            tryInvalidCompile(env, path, "select countMinSketchAdd(theString) from SupportBean",
                "Failed to validate select-clause expression 'countMinSketchAdd(theString)': Count-min-sketch aggregation function 'countMinSketchAdd' can only be used with into-table");
            tryInvalidCompile(env, path, "into table MyCMS select countMinSketchAdd() as wordcms from SupportBean",
                "Failed to validate select-clause expression 'countMinSketchAdd()': Count-min-sketch aggregation function 'countMinSketchAdd' requires a single parameter expression");
            tryInvalidCompile(env, path, "into table MyCMS select countMinSketchAdd(body) as wordcms from SupportByteArrEventStringId",
                "Incompatible aggregation function for table 'MyCMS' column 'wordcms', expecting 'countMinSketch()' and received 'countMinSketchAdd(body)': Mismatching parameter return type, expected any of [class java.lang.String] but received byte(Array) [");
            tryInvalidCompile(env, path, "into table MyCMS select countMinSketchAdd(distinct 'abc') as wordcms from SupportByteArrEventStringId",
                "Failed to validate select-clause expression 'countMinSketchAdd(distinct \"abc\")': Count-min-sketch aggregation function 'countMinSketchAdd' is not supported with distinct [");

            // invalid "countMinSketchFrequency" declarations
            //
            tryInvalidCompile(env, path, "into table MyCMS select countMinSketchFrequency(theString) as wordcms from SupportBean",
                "Failed to validate select-clause expression 'countMinSketchFrequency(theString)': Unknown single-row function, aggregation function or mapped or indexed property named 'countMinSketchFrequency' could not be resolved ");
            tryInvalidCompile(env, path, "select countMinSketchFrequency() from SupportBean",
                "Failed to validate select-clause expression 'countMinSketchFrequency': Unknown single-row function, expression declaration, script or aggregation function named 'countMinSketchFrequency' could not be resolved");

            // invalid "countMinSketchTopk" declarations
            //
            tryInvalidCompile(env, path, "select countMinSketchTopk() from SupportBean",
                "Failed to validate select-clause expression 'countMinSketchTopk': Unknown single-row function, expression declaration, script or aggregation function named 'countMinSketchTopk' could not be resolved");
            tryInvalidCompile(env, path, "select MyCMS.wordcms.countMinSketchTopk(theString) from SupportBean",
                "Failed to validate select-clause expression 'MyCMS.wordcms.countMinSketchTopk(th...(43 chars)': Count-min-sketch aggregation function 'countMinSketchTopk' requires a no parameter expressions [");

            env.undeployAll();
        }
    }

    private static void assertOutput(RegressionEnvironment env, String frequencyList, String topkList) {
        assertFrequencies(env, frequencyList);
        assertTopk(env, topkList);
    }

    private static void assertFrequencies(RegressionEnvironment env, String frequencyList) {
        String[] pairs = frequencyList.split(",");
        for (int i = 0; i < pairs.length; i++) {
            String[] split = pairs[i].split("=");
            env.sendEventBean(new SupportBean_S0(0, split[0].trim()));
            Object value = env.listener("frequency").assertOneGetNewAndReset().get("freq");
            assertEquals("failed at index" + i, Long.parseLong(split[1]), value);
        }
    }

    private static void assertTopk(RegressionEnvironment env, String topkList) {

        env.sendEventBean(new SupportBean_S1(0));
        EventBean event = env.listener("topk").assertOneGetNewAndReset();
        CountMinSketchTopK[] arr = (CountMinSketchTopK[]) event.get("topk");

        String[] pairs = topkList.split(",");
        assertEquals("received " + Arrays.asList(arr), pairs.length, arr.length);

        for (String pair : pairs) {
            String[] pairArr = pair.split("=");
            long expectedFrequency = Long.parseLong(pairArr[1]);
            String expectedValue = pairArr[0].trim();
            int foundIndex = find(expectedFrequency, expectedValue, arr);
            assertFalse("failed to find '" + expectedValue + "=" + expectedFrequency + "' among remaining " + Arrays.asList(arr), foundIndex == -1);
            arr[foundIndex] = null;
        }
    }

    private static int find(long expectedFrequency, String expectedValue, CountMinSketchTopK[] arr) {
        for (int i = 0; i < arr.length; i++) {
            CountMinSketchTopK item = arr[i];
            if (item != null && item.getFrequency() == expectedFrequency && item.getValue().equals(expectedValue)) {
                return i;
            }
        }
        return -1;
    }

    public static class MyBytesPassthruAgentForge implements CountMinSketchAgentForge {
        public Class[] getAcceptableValueTypes() {
            return new Class[]{byte[].class};
        }

        public CodegenExpression codegenMake(CodegenMethod parent, CodegenClassScope classScope) {
            return newInstance(MyBytesPassthruAgent.class);
        }
    }

    public static class MyBytesPassthruAgent implements CountMinSketchAgent {

        public void add(CountMinSketchAgentContextAdd ctx) {
            if (ctx.getValue() == null) {
                return;
            }
            byte[] value = (byte[]) ctx.getValue();
            ctx.getState().add(value, 1);
        }

        public Long estimate(CountMinSketchAgentContextEstimate ctx) {
            if (ctx.getValue() == null) {
                return null;
            }
            byte[] value = (byte[]) ctx.getValue();
            return ctx.getState().frequency(value);
        }

        public Object fromBytes(CountMinSketchAgentContextFromBytes ctx) {
            return ctx.getBytes();
        }
    }
}