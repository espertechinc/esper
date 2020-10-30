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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.*;

public class EPLVariablesOnSet {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLVariableOnSetSimple());
        execs.add(new EPLVariableOnSetSimpleSceneTwo());
        execs.add(new EPLVariableOnSetCompile());
        execs.add(new EPLVariableOnSetObjectModel());
        execs.add(new EPLVariableOnSetWithFilter());
        execs.add(new EPLVariableOnSetSubquery());
        execs.add(new EPLVariableOnSetWDeploy());
        execs.add(new EPLVariableOnSetAssignmentOrderNoDup());
        execs.add(new EPLVariableOnSetAssignmentOrderDup());
        execs.add(new EPLVariableOnSetRuntimeOrderMultiple());
        execs.add(new EPLVariableOnSetCoercion());
        execs.add(new EPLVariableOnSetInvalid());
        execs.add(new EPLVariableOnSetSubqueryMultikeyWArray());
        execs.add(new EPLVariableOnSetArrayAtIndex(false));
        execs.add(new EPLVariableOnSetArrayAtIndex(true));
        execs.add(new EPLVariableOnSetArrayBoxed());
        execs.add(new EPLVariableOnSetArrayInvalid());
        execs.add(new EPLVariableOnSetExpression());
        return execs;
    }

    private static class EPLVariableOnSetExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                    "import " + MyLocalVariable.class.getName() + ";\n" +
                    "@name('var') create variable MyLocalVariable VAR = new MyLocalVariable(1, 10);\n" +
                    "" +
                    "inlined_class \"\"\"\n" +
                    "  import " + MyLocalVariable.class.getName().replace("$", ".") + ";\n" +
                    "  public class Helper {\n" +
                    "    public static void swap(MyLocalVariable var) {\n" +
                    "      int temp = var.a;\n" +
                    "      var.a = var.b;\n" +
                    "      var.b = temp;\n" +
                    "    }\n" +
                    "  }\n" +
                    "\"\"\"\n" +
                    "@name('s0') on SupportBean set Helper.swap(VAR);\n";
            env.compileDeploy(epl).addListener("s0");

            assertVariable(env, 1, 10);

            env.sendEventBean(new SupportBean());

            assertVariable(env, 10, 1);

            env.undeployAll();

            String eplInvalid =
                "import " + MyLocalVariable.class.getName() + ";\n" +
                    "@name('var') create variable MyLocalVariable VARONE = new MyLocalVariable(1, 10);\n" +
                    "@name('var') create variable MyLocalVariable VARTWO = new MyLocalVariable(1, 10);\n" +
                    "" +
                    "inlined_class \"\"\"\n" +
                    "  import " + MyLocalVariable.class.getName().replace("$", ".") + ";\n" +
                    "  public class Helper {\n" +
                    "    public static void swap(MyLocalVariable varOne, MyLocalVariable varTwo) {\n" +
                    "    }\n" +
                    "  }\n" +
                    "\"\"\"\n" +
                    "@name('s0') on SupportBean set Helper.swap(VARONE, VARTWO);\n";
            tryInvalidCompile(env, eplInvalid, "Failed to validate assignment expression 'Helper.swap(VARONE,VARTWO)': Assignment expression must receive a single variable value");

            String eplConstant =
                "import " + MyLocalVariable.class.getName() + ";\n" +
                    "@name('var') create constant variable MyLocalVariable VAR = new MyLocalVariable(1, 10);\n" +
                    "@name('s0') on SupportBean set VAR.reset();\n";
            tryInvalidCompile(env, eplConstant, "Failed to validate assignment expression 'VAR.reset()': Variable by name 'VAR' is declared constant and may not be set");
        }

        private void assertVariable(RegressionEnvironment env, int aExpected, int bExpected) {
            MyLocalVariable value = (MyLocalVariable) env.runtime().getVariableService().getVariableValue(env.deploymentId("var"), "VAR");
            assertEquals(aExpected, value.a);
            assertEquals(bExpected, value.b);
        }
    }

    private static class EPLVariableOnSetArrayBoxed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                    "create variable java.lang.Double[] dbls = new java.lang.Double[3];\n" +
                    "@priority(1) on SupportBean set dbls[intPrimitive] = 1;\n" +
                    "@name('s0') select dbls as c0 from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            assertArrayEquals(new Double[] {null, 1d, null}, (Double[]) env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }

    private static class EPLVariableOnSetArrayInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplVariables = "@name('create') create variable double[primitive] doublearray;\n" +
                "create variable int[primitive] intarray;\n" +
                "create variable int notAnArray;";
            env.compile(eplVariables, path);

            // invalid property
            tryInvalidCompile(env, path, "on SupportBean set xxx[intPrimitive]=1d",
                "Failed to validate assignment expression 'xxx[intPrimitive]=1.0': Variable by name 'xxx' has not been created or configured");

            // index expression is not Integer
            tryInvalidCompile(env, path, "on SupportBean set doublearray[null]=1d",
                "Incorrect index expression for array operation, expected an expression returning an integer value but the expression 'null' returns 'null' for expression 'doublearray'");

            // type incompatible cannot assign
            tryInvalidCompile(env, path, "on SupportBean set intarray[intPrimitive]='x'",
                "Failed to validate assignment expression 'intarray[intPrimitive]=\"x\"': Invalid assignment of column '\"x\"' of type 'String' to event property 'intarray' typed as 'int', column and parameter types mismatch");

            // not-an-array
            tryInvalidCompile(env, path, "on SupportBean set notAnArray[intPrimitive]=1",
                "Failed to validate assignment expression 'notAnArray[intPrimitive]=1': Variable 'notAnArray' is not an array");

            path.clear();

            // runtime-behavior for index-overflow and null-array and null-index and
            String epl = "@name('create') create variable double[primitive] doublearray = new double[3];\n" +
                "on SupportBean set doublearray[intBoxed]=doubleBoxed;\n";
            env.compileDeploy(epl);

            // index returned is too large
            try {
                SupportBean sb = new SupportBean();
                sb.setIntBoxed(10);
                sb.setDoubleBoxed(10d);
                env.sendEventBean(sb);
                fail();
            } catch (RuntimeException ex) {
                assertTrue(ex.getMessage().contains("Array length 3 less than index 10 for variable 'doublearray'"));
            }

            // index returned null
            SupportBean sbIndexNull = new SupportBean();
            sbIndexNull.setDoubleBoxed(10d);
            env.sendEventBean(sbIndexNull);

            // rhs returned null for array-of-primitive
            SupportBean sbRHSNull = new SupportBean();
            sbRHSNull.setIntBoxed(1);
            env.sendEventBean(sbRHSNull);

            env.undeployAll();
        }
    }

    private static class EPLVariableOnSetArrayAtIndex implements RegressionExecution {
        private final boolean soda;

        public EPLVariableOnSetArrayAtIndex(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCreate = "@name('vars') @public create variable double[primitive] doublearray = new double[3];\n" +
                "@public create variable String[] stringarray = new String[] {'a', 'b', 'c'};\n";
            env.compileDeploy(eplCreate, path);

            String epl = "on SupportBean set (doublearray[intPrimitive])=1, (stringarray[intPrimitive])=\"x\"";
            env.compileDeploy(soda, epl, path);

            assertVariables(env, new double[3], "a,b,c".split(","));

            env.sendEventBean(new SupportBean("E1", 1));

            assertVariables(env, new double[] {0, 1, 0}, "a,x,c".split(","));

            env.undeployAll();
        }

        private void assertVariables(RegressionEnvironment env, double[] doubleExpected, String[] stringExpected) {
            Map<DeploymentIdNamePair, Object> vals = env.runtime().getVariableService().getVariableValueAll();
            String deploymentId = env.deploymentId("vars");
            double[] doubleArray = (double[]) vals.get(new DeploymentIdNamePair(deploymentId, "doublearray"));
            String[] stringArray = (String[]) vals.get(new DeploymentIdNamePair(deploymentId, "stringarray"));
            assertArrayEquals(doubleExpected, doubleArray, 0.0);
            assertArrayEquals(stringExpected, stringArray);
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    private static class EPLVariableOnSetSubqueryMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('var') @public create variable int total_sum = -1;\n" +
                "on SupportBean set total_sum = (select sum(value) as c0 from SupportEventWithIntArray#keepall group by array)";
            env.compileDeploy(epl);

            env.sendEventBean(new SupportEventWithIntArray("E1", new int[] {1, 2}, 10));
            env.sendEventBean(new SupportEventWithIntArray("E2", new int[] {1, 2}, 11));

            env.milestone(0);
            assertVariable(env, -1);

            env.sendEventBean(new SupportBean());
            assertVariable(env, 21);

            env.sendEventBean(new SupportEventWithIntArray("E3", new int[] {1, 2}, 12));
            env.sendEventBean(new SupportBean());
            assertVariable(env, 33);

            env.milestone(1);

            env.sendEventBean(new SupportEventWithIntArray("E4", new int[] {1}, 13));
            env.sendEventBean(new SupportBean());
            assertVariable(env, null);

            env.undeployAll();
        }

        private void assertVariable(RegressionEnvironment env, Integer expected) {
            assertEquals(expected, env.runtime().getVariableService().getVariableValue(env.deploymentId("var"), "total_sum"));
        }
    }

    private static class EPLVariableOnSetSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create variable boolean var_simple_set = true;\n" +
                "@name('set') on SupportBean_S0 set var_simple_set = false;\n" +
                "@name('s0') select var_simple_set as c0 from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(0));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 0));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }

    private static class EPLVariableOnSetSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String stmtTextSet = "@name('s0') on SupportBean_S0 as s0str set var1SS = (select p10 from SupportBean_S1#lastevent), var2SS = (select p11||s0str.p01 from SupportBean_S1#lastevent)";
            env.compileDeploy(stmtTextSet);
            String[] fieldsVar = new String[]{"var1SS", "var2SS"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fieldsVar, new Object[][]{{"a", "b"}});

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fieldsVar, new Object[][]{{null, null}});

            env.milestone(0);

            env.sendEventBean(new SupportBean_S1(0, "x", "y"));
            env.sendEventBean(new SupportBean_S0(1, "1", "2"));
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fieldsVar, new Object[][]{{"x", "y2"}});

            env.undeployAll();
        }
    }

    private static class EPLVariableOnSetAssignmentOrderNoDup implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtTextSet = "@name('set') on SupportBean set var1OND = intPrimitive, var2OND = var1OND + 1, var3OND = var1OND + var2OND";
            env.compileDeploy(stmtTextSet).addListener("set");
            String[] fieldsVar = new String[]{"var1OND", "var2OND", "var3OND"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{12, 2, null}});

            sendSupportBean(env, "S1", 3);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{3, 4, 7});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{3, 4, 7}});

            env.milestone(0);

            sendSupportBean(env, "S1", -1);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{-1, 0, -1});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{-1, 0, -1}});

            sendSupportBean(env, "S1", 90);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{90, 91, 181});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{90, 91, 181}});

            env.undeployAll();
        }
    }

    private static class EPLVariableOnSetAssignmentOrderDup implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String stmtTextSet = "@name('set') on SupportBean set var1OD = intPrimitive, var2OD = var2OD, var1OD = intBoxed, var3OD = var3OD + 1";
            env.compileDeploy(stmtTextSet).addListener("set");
            String[] fieldsVar = new String[]{"var1OD", "var2OD", "var3OD"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{0, 1, 2}});

            sendSupportBean(env, "S1", -1, 10);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{10, 1, 3});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{10, 1, 3}});

            sendSupportBean(env, "S2", -2, 20);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{20, 1, 4});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{20, 1, 4}});

            env.milestone(0);

            sendSupportBeanNewThread(env, "S3", -3, 30);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{30, 1, 5});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{30, 1, 5}});

            sendSupportBeanNewThread(env, "S4", -4, 40);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{40, 1, 6});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{40, 1, 6}});

            env.undeployAll();
        }
    }

    private static class EPLVariableOnSetObjectModel implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create("var1OM", "var2OM", "id"));
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean_A")));

            RegressionPath path = new RegressionPath();
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model, path);
            String stmtText = "@name('s0') select var1OM, var2OM, id from SupportBean_A";
            assertEquals(stmtText, model.toEPL());
            env.addListener("s0");

            String[] fieldsSelect = new String[]{"var1OM", "var2OM", "id"};
            sendSupportBean_A(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{10d, 11L, "E1"});

            model = new EPStatementObjectModel();
            model.setOnExpr(OnClause.createOnSet(Expressions.eq(Expressions.property("var1OM"), Expressions.property("intPrimitive"))).addAssignment(Expressions.eq(Expressions.property("var2OM"), Expressions.property("intBoxed"))));
            model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getSimpleName())));
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("set")));
            String stmtTextSet = "@name('set') on SupportBean set var1OM=intPrimitive, var2OM=intBoxed";
            env.compileDeploy(model, path).addListener("set");
            assertEquals(stmtTextSet, model.toEPL());

            EventType typeSet = env.statement("set").getEventType();
            assertEquals(Double.class, typeSet.getPropertyType("var1OM"));
            assertEquals(Long.class, typeSet.getPropertyType("var2OM"));
            assertEquals(Map.class, typeSet.getUnderlyingType());
            String[] fieldsVar = new String[]{"var1OM", "var2OM"};
            EPAssertionUtil.assertEqualsAnyOrder(fieldsVar, typeSet.getPropertyNames());

            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{10d, 11L}});
            sendSupportBean(env, "S1", 3, 4);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{3d, 4L});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{3d, 4L}});

            sendSupportBean_A(env, "E2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{3d, 4L, "E2"});

            env.undeployModuleContaining("set");
            env.undeployModuleContaining("s0");
        }
    }

    private static class EPLVariableOnSetSimpleSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            String textVar = "@name('s0_0') create variable int resvar = 1";
            env.compileDeploy(textVar, path).addListener("s0_0");
            String[] fieldsVarOne = new String[]{"resvar"};

            textVar = "@name('s0_1') create variable int durvar = 10";
            env.compileDeploy(textVar, path).addListener("s0_1");
            String[] fieldsVarTwo = new String[]{"durvar"};

            String textSet = "@name('s1') on SupportBean set resvar = intPrimitive, durvar = intPrimitive";
            env.compileDeploy(textSet, path).addListener("s1");
            String[] fieldsVarSet = new String[]{"resvar", "durvar"};

            String textSelect = "@name('s2') select irstream resvar, durvar, symbol from SupportMarketDataBean";
            env.compileDeploy(textSelect, path).addListener("s2");
            String[] fieldsSelect = new String[]{"resvar", "durvar", "symbol"};

            env.milestone(0);

            // read values
            sendMarketDataEvent(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fieldsSelect, new Object[]{1, 10, "E1"});

            env.milestone(1);

            // set new value
            sendSupportBean(env, 20);
            EPAssertionUtil.assertProps(env.listener("s0_0").getLastNewData()[0], fieldsVarOne, new Object[]{20});
            EPAssertionUtil.assertProps(env.listener("s0_1").getLastNewData()[0], fieldsVarTwo, new Object[]{20});
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fieldsVarSet, new Object[]{20, 20});
            env.listener("s0_0").reset();

            env.milestone(2);

            // read values
            sendMarketDataEvent(env, "E2");
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fieldsSelect, new Object[]{20, 20, "E2"});

            env.milestone(3);

            // set new value
            sendSupportBean(env, 1000);

            env.milestone(4);

            // read values
            sendMarketDataEvent(env, "E3");
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fieldsSelect, new Object[]{1000, 1000, "E3"});

            env.milestone(5);

            env.undeployModuleContaining("s1");
            env.undeployModuleContaining("s2");
            env.undeployModuleContaining("s0_0");
            env.undeployModuleContaining("s0_1");
        }

        private static void sendMarketDataEvent(RegressionEnvironment env, String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, null);
            env.sendEventBean(bean);
        }

        private static void sendSupportBean(RegressionEnvironment env, int intPrimitive) {
            SupportBean bean = new SupportBean("", intPrimitive);
            env.sendEventBean(bean);
        }
    }

    private static class EPLVariableOnSetCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String stmtText = "@name('s0') select var1C, var2C, id from SupportBean_A";
            env.eplToModelCompileDeploy(stmtText).addListener("s0");

            String[] fieldsSelect = new String[]{"var1C", "var2C", "id"};
            sendSupportBean_A(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{10d, 11L, "E1"});

            String stmtTextSet = "@name('set') on SupportBean set var1C=intPrimitive, var2C=intBoxed";
            env.eplToModelCompileDeploy(stmtTextSet).addListener("set");

            EventType typeSet = env.statement("set").getEventType();
            assertEquals(Double.class, typeSet.getPropertyType("var1C"));
            assertEquals(Long.class, typeSet.getPropertyType("var2C"));
            assertEquals(Map.class, typeSet.getUnderlyingType());
            String[] fieldsVar = new String[]{"var1C", "var2C"};
            EPAssertionUtil.assertEqualsAnyOrder(fieldsVar, typeSet.getPropertyNames());

            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{10d, 11L}});
            sendSupportBean(env, "S1", 3, 4);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{3d, 4L});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{3d, 4L}});

            sendSupportBean_A(env, "E2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{3d, 4L, "E2"});

            env.undeployModuleContaining("set");
            env.undeployModuleContaining("s0");
        }
    }

    private static class EPLVariableOnSetWDeploy implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String stmtText = "@name('s0') select var1RTC, theString from SupportBean(theString like 'E%')";
            env.compileDeploy(stmtText).addListener("s0");

            String[] fieldsSelect = new String[]{"var1RTC", "theString"};
            sendSupportBean(env, "E1", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{10, "E1"});

            env.milestone(0);

            sendSupportBean(env, "E2", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{10, "E2"});

            String stmtTextSet = "@name('set') on SupportBean(theString like 'S%') set var1RTC = intPrimitive";
            env.compileDeploy(stmtTextSet).addListener("set");

            EventType typeSet = env.statement("set").getEventType();
            assertEquals(Integer.class, typeSet.getPropertyType("var1RTC"));
            assertEquals(Map.class, typeSet.getUnderlyingType());
            assertTrue(Arrays.equals(typeSet.getPropertyNames(), new String[]{"var1RTC"}));

            String[] fieldsVar = new String[]{"var1RTC"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{10}});

            sendSupportBean(env, "S1", 3);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{3});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{3}});

            env.milestone(0);

            sendSupportBean(env, "E3", 4);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{3, "E3"});

            sendSupportBean(env, "S2", -1);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{-1});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{-1}});

            sendSupportBean(env, "E4", 5);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{-1, "E4"});

            env.undeployAll();
        }
    }

    private static class EPLVariableOnSetRuntimeOrderMultiple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String stmtTextSet = "@name('set') on SupportBean(theString like 'S%' or theString like 'B%') set var1ROM = intPrimitive, var2ROM = intBoxed";
            env.compileDeploy(stmtTextSet).addListener("set");
            String[] fieldsVar = new String[]{"var1ROM", "var2ROM"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{null, 1}});

            EventType typeSet = env.statement("set").getEventType();
            assertEquals(Integer.class, typeSet.getPropertyType("var1ROM"));
            assertEquals(Integer.class, typeSet.getPropertyType("var2ROM"));
            assertEquals(Map.class, typeSet.getUnderlyingType());
            EPAssertionUtil.assertEqualsAnyOrder(new String[]{"var1ROM", "var2ROM"}, typeSet.getPropertyNames());

            sendSupportBean(env, "S1", 3, null);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{3, null});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{3, null}});

            env.milestone(0);

            sendSupportBean(env, "S1", -1, -2);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{-1, -2});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{-1, -2}});

            String stmtText = "@name('s0') select var1ROM, var2ROM, theString from SupportBean(theString like 'E%' or theString like 'B%')";
            env.compileDeploy(stmtText).addListener("s0");
            String[] fieldsSelect = new String[]{"var1ROM", "var2ROM", "theString"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fieldsSelect, null);

            env.milestone(1);

            sendSupportBean(env, "E1", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{-1, -2, "E1"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{-1, -2}});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fieldsSelect, new Object[][]{{-1, -2, "E1"}});

            sendSupportBean(env, "S1", 11, 12);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{11, 12});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{11, 12}});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fieldsSelect, new Object[][]{{11, 12, "E1"}});

            sendSupportBean(env, "E2", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{11, 12, "E2"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fieldsSelect, new Object[][]{{11, 12, "E2"}});

            env.undeployAll();
        }
    }

    private static class EPLVariableOnSetWithFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtTextSet = "@name('set') on SupportBean(theString like 'S%') set papi_1 = 'end', papi_2 = false, papi_3 = null";
            env.compileDeploy(stmtTextSet).addListener("set");
            String[] fieldsVar = new String[]{"papi_1", "papi_2", "papi_3"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{"begin", true, "value"}});

            EventType typeSet = env.statement("set").getEventType();
            assertEquals(String.class, typeSet.getPropertyType("papi_1"));
            assertEquals(Boolean.class, typeSet.getPropertyType("papi_2"));
            assertEquals(String.class, typeSet.getPropertyType("papi_3"));
            assertEquals(Map.class, typeSet.getUnderlyingType());
            Arrays.sort(typeSet.getPropertyNames());
            assertTrue(Arrays.equals(typeSet.getPropertyNames(), fieldsVar));

            sendSupportBean(env, "S1", 3);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{"end", false, null});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{"end", false, null}});

            env.milestone(0);

            sendSupportBean(env, "S2", 4);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{"end", false, null});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{"end", false, null}});

            env.undeployAll();
        }
    }

    private static class EPLVariableOnSetCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String stmtTextSet = "@name('set') on SupportBean set var1COE = intPrimitive, var2COE = intPrimitive, var3COE=intBoxed";
            env.compileDeploy(stmtTextSet).addListener("set");
            String[] fieldsVar = new String[]{"var1COE", "var2COE", "var3COE"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{null, null, null}});

            EventType typeSet = env.statement("set").getEventType();
            assertEquals(Float.class, typeSet.getPropertyType("var1COE"));
            assertEquals(Double.class, typeSet.getPropertyType("var2COE"));
            assertEquals(Long.class, typeSet.getPropertyType("var3COE"));
            assertEquals(Map.class, typeSet.getUnderlyingType());
            EPAssertionUtil.assertEqualsAnyOrder(typeSet.getPropertyNames(), fieldsVar);

            String stmtText = "@name('s0') select irstream var1COE, var2COE, var3COE, id from SupportBean_A#length(2)";
            env.compileDeploy(stmtText).addListener("s0");
            String[] fieldsSelect = new String[]{"var1COE", "var2COE", "var3COE", "id"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fieldsSelect, null);

            sendSupportBean_A(env, "A1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{null, null, null, "A1"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fieldsSelect, new Object[][]{{null, null, null, "A1"}});

            sendSupportBean(env, "S1", 1, 2);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{1f, 1d, 2L});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{1f, 1d, 2L}});

            env.milestone(0);

            sendSupportBean_A(env, "A2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{1f, 1d, 2L, "A2"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fieldsSelect, new Object[][]{{1f, 1d, 2L, "A1"}, {1f, 1d, 2L, "A2"}});

            sendSupportBean(env, "S1", 10, 20);
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{10f, 10d, 20L});
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{10f, 10d, 20L}});

            sendSupportBean_A(env, "A3");
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fieldsSelect, new Object[]{10f, 10d, 20L, "A3"});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fieldsSelect, new Object[]{10f, 10d, 20L, "A1"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fieldsSelect, new Object[][]{{10f, 10d, 20L, "A2"}, {10f, 10d, 20L, "A3"}});

            env.undeployAll();
        }
    }

    private static class EPLVariableOnSetInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            tryInvalidCompile(env, "on SupportBean set dummy = 100",
                "Failed to validate assignment expression 'dummy=100': Variable by name 'dummy' has not been created or configured");

            tryInvalidCompile(env, "on SupportBean set var1IS = 1",
                "Failed to validate assignment expression 'var1IS=1': Variable 'var1IS' of declared type String cannot be assigned a value of type int");

            tryInvalidCompile(env, "on SupportBean set var3IS = 'abc'",
                "Failed to validate assignment expression 'var3IS=\"abc\"': Variable 'var3IS' of declared type Integer cannot be assigned a value of type String");

            tryInvalidCompile(env, "on SupportBean set var3IS = doublePrimitive",
                "Failed to validate assignment expression 'var3IS=doublePrimitive': Variable 'var3IS' of declared type Integer cannot be assigned a value of type Double");

            tryInvalidCompile(env, "on SupportBean set var2IS = 'false'", "skip");
            tryInvalidCompile(env, "on SupportBean set var3IS = 1.1", "skip");
            tryInvalidCompile(env, "on SupportBean set var3IS = 22222222222222", "skip");
            tryInvalidCompile(env, "on SupportBean set var3IS", "Failed to validate assignment expression 'var3IS': Missing variable assignment expression in assignment number 0");
        }
    }

    private static SupportBean_A sendSupportBean_A(RegressionEnvironment env, String id) {
        SupportBean_A bean = new SupportBean_A(id);
        env.sendEventBean(bean);
        return bean;
    }

    private static SupportBean sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
        return bean;
    }

    private static SupportBean sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive, Integer intBoxed) {
        SupportBean bean = makeSupportBean(theString, intPrimitive, intBoxed);
        env.sendEventBean(bean);
        return bean;
    }

    private static void sendSupportBeanNewThread(RegressionEnvironment env, final String theString, final int intPrimitive, final Integer intBoxed) {
        try {
            Thread t = new Thread() {
                public void run() {
                    SupportBean bean = makeSupportBean(theString, intPrimitive, intBoxed);
                    env.sendEventBean(bean);
                }
            };
            t.start();
            t.join();
        } catch (InterruptedException ex) {
            fail(ex.getMessage());
        }
    }

    private static SupportBean makeSupportBean(String theString, int intPrimitive, Integer intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        return bean;
    }

    public static class MyLocalVariable {
        public int a;
        public int b;

        public MyLocalVariable(int a, int b) {
            this.a = a;
            this.b = b;
        }

        public void reset() {
            throw new UnsupportedOperationException("reset not supported");
        }
    }
}
