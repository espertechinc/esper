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
package com.espertech.esper.regressionlib.suite.client.extension;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.client.SupportDeploymentDependencies;
import com.espertech.esper.runtime.client.util.EPObjectType;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ClientExtendAggregationInlinedClass {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientExtendAggregationInlinedLocalClass());
        execs.add(new ClientExtendAggregationInlinedFAF());
        execs.add(new ClientExtendAggregationInlinedSameModule());
        execs.add(new ClientExtendAggregationInlinedOtherModule());
        execs.add(new ClientExtendAggregationInlinedInvalid());
        execs.add(new ClientExtendAggregationInlinedMultiModuleUses());
        return execs;
    }

    final static String INLINEDCLASS_CONCAT = "inlined_class \"\"\"\n" +
        "import com.espertech.esper.common.client.hook.aggfunc.*;\n" +
        "import com.espertech.esper.common.internal.epl.expression.core.*;\n" +
        "import com.espertech.esper.common.client.hook.forgeinject.*;\n" +
        "import com.espertech.esper.common.client.serde.*;\n" +
        "import com.espertech.esper.common.client.type.*;\n" +
        "import java.io.*;\n" +
        "@ExtensionAggregationFunction(name=\"concat\")\n" +
        "public class ConcatAggForge implements AggregationFunctionForge {\n" +
        "  public void validate(AggregationFunctionValidationContext validationContext) throws ExprValidationException {\n" +
        "    EPType paramType = validationContext.getParameterTypes()[0];\n" +
        "    if (paramType == EPTypeNull.INSTANCE || ((EPTypeClass) paramType).getType() != String.class) {\n" +
        "      throw new ExprValidationException(\"Invalid parameter type '\" + paramType + \"'\");\n" +
        "    }\n" +
        "  }\n" +
        "\n" +
        "  public EPTypeClass getValueType() {\n" +
        "    return new EPTypeClass(String.class);\n" +
        "  }\n" +
        "\n" +
        "  public AggregationFunctionMode getAggregationFunctionMode() {\n" +
        "    AggregationFunctionModeManaged mode = new AggregationFunctionModeManaged();\n" +
        "    mode.setHasHA(true);\n" +
        "    mode.setSerde(ConcatAggSerde.class);\n" +
        "    mode.setInjectionStrategyAggregationFunctionFactory(new InjectionStrategyClassNewInstance(ConcatAggFactory.class.getName()));\n" +
        "    return mode;\n" +
        "  }\n" +
        "\n" +
        "  public static class ConcatAggFactory implements AggregationFunctionFactory {\n" +
        "    public AggregationFunction newAggregator(AggregationFunctionFactoryContext ctx) {\n" +
        "      return new ConcatAggFunction();\n" +
        "    }\n" +
        "  }\n" +
        "\n" +
        "  public static class ConcatAggFunction implements AggregationFunction {\n" +
        "    private final static String DELIMITER = \",\";\n" +
        "    private StringBuilder builder;\n" +
        "    private String delimiter;\n" +
        "\n" +
        "    public ConcatAggFunction() {\n" +
        "      super();\n" +
        "      builder = new StringBuilder();\n" +
        "      delimiter = \"\";\n" +
        "    }\n" +
        "\n" +
        "    public void enter(Object value) {\n" +
        "      if (value != null) {\n" +
        "        builder.append(delimiter);\n" +
        "        builder.append(value.toString());\n" +
        "        delimiter = DELIMITER;\n" +
        "      }\n" +
        "    }\n" +
        "\n" +
        "    public void leave(Object value) {\n" +
        "      if (value != null) {\n" +
        "        builder.delete(0, value.toString().length() + 1);\n" +
        "      }\n" +
        "    }\n" +
        "  \n" +
        "    public String getValue() {\n" +
        "      return builder.toString();\n" +
        "    }\n" +
        "  \n" +
        "    public void clear() {\n" +
        "      builder = new StringBuilder();\n" +
        "      delimiter = \"\";\n" +
        "    }\n" +
        "  }\n" +
        "  public static class ConcatAggSerde {\n" +
        "    public static void write(DataOutput output, AggregationFunction value) throws IOException {\n" +
        "      ConcatAggFunction agg = (ConcatAggFunction) value;\n" +
        "      output.writeUTF(agg.getValue());\n" +
        "    }\n" +
        "\n" +
        "    public static AggregationFunction read(DataInput input) throws IOException {\n" +
        "      ConcatAggFunction concatAggFunction = new ConcatAggFunction();\n" +
        "      String current = input.readUTF();\n" +
        "      if (!current.isEmpty()) {\n" +
        "        concatAggFunction.enter(current);\n" +
        "      }\n" +
        "      return concatAggFunction;\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\"\"\"\n";

    private static class ClientExtendAggregationInlinedLocalClass implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0')\n" +
                INLINEDCLASS_CONCAT +
                "select concat(theString) as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            sendAssertConcat(env, "A", "A");
            sendAssertConcat(env, "B", "A,B");

            env.milestone(0);

            sendAssertConcat(env, "C", "A,B,C");

            env.undeployAll();
        }
    }

    private static class ClientExtendAggregationInlinedInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplTwiceLocal = INLINEDCLASS_CONCAT.replace("ConcatAggForge", "ConcatAggForgeOne") + INLINEDCLASS_CONCAT.replace("ConcatAggForge", "ConcatAggForgeTwo") +
                "select concat(theString) from SupportBean";
            env.tryInvalidCompile(eplTwiceLocal,
                "The plug-in aggregation function 'concat' occurs multiple times");

            String eplTwiceCreate = "create " + INLINEDCLASS_CONCAT.replace("ConcatAggForge", "ConcatAggForgeOne") + ";\n" +
                "create " + INLINEDCLASS_CONCAT.replace("ConcatAggForge", "ConcatAggForgeTwo") + ";\n" +
                "select concat(theString) from SupportBean";
            env.tryInvalidCompile(eplTwiceCreate,
                "The plug-in aggregation function 'concat' occurs multiple times");

            RegressionPath path = new RegressionPath();
            env.compile("@public create " + INLINEDCLASS_CONCAT.replace("ConcatAggForge", "ConcatAggForgeOne"), path);
            env.compile("@public create " + INLINEDCLASS_CONCAT.replace("ConcatAggForge", "ConcatAggForgeTwo"), path);
            String eplTwiceInPath = "select concat(theString) from SupportBean";
            env.tryInvalidCompile(path, eplTwiceInPath,
                "The plug-in aggregation function 'concat' occurs multiple times");
        }
    }

    private static class ClientExtendAggregationInlinedFAF implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplWindow = "@public create window MyWindow#keepall as (theString string);\n" +
                "on SupportBean merge MyWindow insert select theString;\n";
            env.compileDeploy(eplWindow, path);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 1));

            env.assertThat(() -> {
                String eplFAF = INLINEDCLASS_CONCAT +
                    "select concat(theString) as c0 from MyWindow";
                EPFireAndForgetQueryResult result = env.compileExecuteFAF(eplFAF, path);
                assertEquals("E1,E2", result.getArray()[0].get("c0"));
            });

            env.undeployAll();
        }
    }

    private static class ClientExtendAggregationInlinedSameModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create " + INLINEDCLASS_CONCAT + ";\n" +
                "@name('s0') select concat(theString) as c0 from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");

            sendAssertConcat(env, "A", "A");

            env.milestone(0);

            sendAssertConcat(env, "B", "A,B");

            SupportDeploymentDependencies.assertEmpty(env, "s0");

            env.undeployAll();
        }
    }

    private static class ClientExtendAggregationInlinedOtherModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplCreateInlined = "@name('clazz') @public create " + INLINEDCLASS_CONCAT + ";\n";
            RegressionPath path = new RegressionPath();
            env.compile(eplCreateInlined.replace("builder.toString()", "null"), path);

            String eplSelect = "@name('s0') select concat(theString) as c0 from SupportBean";
            EPCompiled compiledSelect = env.compile(eplSelect, path);

            env.compileDeploy(eplCreateInlined);
            env.deploy(compiledSelect).addListener("s0");

            sendAssertConcat(env, "A", "A");

            env.milestone(0);

            sendAssertConcat(env, "B", "A,B");

            // assert dependencies
            SupportDeploymentDependencies.assertSingle(env, "s0", "clazz", EPObjectType.CLASSPROVIDED, "ConcatAggForge");

            env.undeployAll();
        }
    }

    private static class ClientExtendAggregationInlinedMultiModuleUses implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            for (String module : new String[]{"XXX", "YYY", "ZZZ"}) {
                String epl = "module " + module + "; @public create " + INLINEDCLASS_CONCAT.replace("ConcatAggForge", "ConcatAggForge" + module).replace("builder.toString()", "\"" + module + "\"");
                env.compileDeploy(epl, path);
            }

            String eplSelect = "uses YYY; @name('s0') select concat(theString) as c0 from SupportBean";
            env.compileDeploy(eplSelect, path).addListener("s0");

            sendAssertConcat(env, "A", "YYY");

            env.undeployAll();
        }
    }

    private static void sendAssertConcat(RegressionEnvironment env, String theString, String expected) {
        env.sendEventBean(new SupportBean(theString, 0));
        env.assertEqualsNew("s0", "c0", expected);
    }
}
