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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.CreateVariableClause;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.client.soda.Expressions;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.client.variable.VariableValueException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.*;

public class EPLVariablesCreate {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLVariableOM());
        execs.add(new EPLVariableCompileStartStop());
        execs.add(new EPLVariableSubscribeAndIterate());
        execs.add(new EPLVariableDeclarationAndSelect());
        execs.add(new EPLVariableInvalid());
        execs.add(new EPLVariableDimensionAndPrimitive());
        return execs;
    }

    private static class EPLVariableDimensionAndPrimitive implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@name('vars') create variable int[primitive] int_prim = null;\n" +
                    "create variable int[] int_boxed = null;\n" +
                    "create variable java.lang.Object[] objectarray = null;\n" +
                    "create variable java.lang.Object[][] objectarray_2dim = null;\n";
            String id = env.compileDeploy(epl).deploymentId("vars");

            runAssertionSetGet(env, id, "int_prim", new int[]{1, 2},
                (expected, received) -> EPAssertionUtil.assertEqualsExactOrder((int[]) expected, (int[]) received));
            runAssertionGetSetInvalid(env, id, "int_prim", new String[0]);

            runAssertionSetGet(env, id, "int_boxed", new Integer[]{1, 2},
                (expected, received) -> EPAssertionUtil.assertEqualsExactOrder((Object[]) expected, (Object[]) received));
            runAssertionGetSetInvalid(env, id, "int_boxed", new int[0]);

            runAssertionSetGet(env, id, "objectarray", new Integer[]{1, 2},
                (expected, received) -> EPAssertionUtil.assertEqualsExactOrder((Object[]) expected, (Object[]) received));
            runAssertionGetSetInvalid(env, id, "objectarray", new int[0]);

            runAssertionSetGet(env, id, "objectarray_2dim", new Object[][]{{1, 2}},
                (expected, received) -> EPAssertionUtil.assertEqualsExactOrder((Object[]) expected, (Object[]) received));
            runAssertionGetSetInvalid(env, id, "objectarray_2dim", new int[0]);

            env.undeployAll();
        }

    }

    private static void runAssertionSetGet(RegressionEnvironment env, String deploymentId, String variableName, Object value, BiConsumer<Object, Object> assertion) {
        env.runtime().getVariableService().setVariableValue(deploymentId, variableName, value);
        Object returned = env.runtime().getVariableService().getVariableValue(deploymentId, variableName);
        assertion.accept(value, returned);
    }

    private static void runAssertionGetSetInvalid(RegressionEnvironment env, String id, String variableName, Object value) {
        try {
            env.runtime().getVariableService().setVariableValue(id, variableName, value);
            fail();
        } catch (VariableValueException ex) {
            // expected
        }
    }

    private static class EPLVariableOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setCreateVariable(CreateVariableClause.create("long", "var1OMCreate", null));
            env.compileDeploy(model, path);
            assertEquals("create variable long var1OMCreate", model.toEPL());

            model = new EPStatementObjectModel();
            model.setCreateVariable(CreateVariableClause.create("string", "var2OMCreate", Expressions.constant("abc")));
            env.compileDeploy(model, path);
            assertEquals("create variable string var2OMCreate = \"abc\"", model.toEPL());

            String stmtTextSelect = "@name('s0') select var1OMCreate, var2OMCreate from SupportBean";
            env.compileDeploy(stmtTextSelect, path).addListener("s0");

            String[] fieldsVar = new String[]{"var1OMCreate", "var2OMCreate"};
            sendSupportBean(env, "E1", 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsVar, new Object[]{null, "abc"});

            env.compileDeploy("create variable double[] arrdouble = {1.0d,2.0d}");

            env.undeployAll();
        }
    }

    private static class EPLVariableCompileStartStop implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            String text = "create variable long var1CSS";
            env.eplToModelCompileDeploy(text, path);

            text = "create variable string var2CSS = \"abc\"";
            env.eplToModelCompileDeploy(text, path);

            String stmtTextSelect = "@name('s0') select var1CSS, var2CSS from SupportBean";
            env.compileDeploy(stmtTextSelect, path).addListener("s0");

            String[] fieldsVar = new String[]{"var1CSS", "var2CSS"};
            sendSupportBean(env, "E1", 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsVar, new Object[]{null, "abc"});

            // ESPER-545
            String createText = "@name('create') create variable int FOO = 0";
            env.compileDeploy(createText, path);
            env.compileDeploy("on pattern [every SupportBean] set FOO = FOO + 1", path);
            env.sendEventBean(new SupportBean());
            assertEquals(1, env.runtime().getVariableService().getVariableValue(env.deploymentId("create"), "FOO"));

            env.undeployAll();

            env.compileDeploy(createText);
            assertEquals(0, env.runtime().getVariableService().getVariableValue(env.deploymentId("create"), "FOO"));

            // cleanup of variable when statement exception occurs
            env.compileDeploy("create variable int x = 123");
            tryInvalidCompile(env, "select missingScript(x) from SupportBean", "skip");
            env.compileDeploy("create variable int x = 123");

            env.undeployAll();
        }
    }

    private static class EPLVariableSubscribeAndIterate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtCreateTextOne = "@name('create-one') create variable long var1SAI = null";
            env.compileDeploy(stmtCreateTextOne, path);
            assertEquals(StatementType.CREATE_VARIABLE, env.statement("create-one").getProperty(StatementProperty.STATEMENTTYPE));
            assertEquals("var1SAI", env.statement("create-one").getProperty(StatementProperty.CREATEOBJECTNAME));
            env.addListener("create-one");

            String[] fieldsVar1 = new String[]{"var1SAI"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("create-one"), fieldsVar1, new Object[][]{{null}});
            assertFalse(env.listener("create-one").isInvoked());

            EventType typeCreateOne = env.statement("create-one").getEventType();
            assertEquals(Long.class, typeCreateOne.getPropertyType("var1SAI"));
            assertEquals(Map.class, typeCreateOne.getUnderlyingType());
            assertTrue(Arrays.equals(typeCreateOne.getPropertyNames(), new String[]{"var1SAI"}));

            String stmtCreateTextTwo = "@name('create-two') create variable long var2SAI = 20";
            env.compileDeploy(stmtCreateTextTwo, path).addListener("create-two");
            String[] fieldsVar2 = new String[]{"var2SAI"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("create-two"), fieldsVar2, new Object[][]{{20L}});
            assertFalse(env.listener("create-two").isInvoked());

            String stmtTextSet = "@name('set') on SupportBean set var1SAI = intPrimitive * 2, var2SAI = var1SAI + 1";
            env.compileDeploy(stmtTextSet, path);

            sendSupportBean(env, "E1", 100);
            EPAssertionUtil.assertProps(env.listener("create-one").getLastNewData()[0], fieldsVar1, new Object[]{200L});
            EPAssertionUtil.assertProps(env.listener("create-one").getLastOldData()[0], fieldsVar1, new Object[]{null});
            env.listener("create-one").reset();
            EPAssertionUtil.assertProps(env.listener("create-two").getLastNewData()[0], fieldsVar2, new Object[]{201L});
            EPAssertionUtil.assertProps(env.listener("create-two").getLastOldData()[0], fieldsVar2, new Object[]{20L});
            env.listener("create-one").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("create-one").iterator(), fieldsVar1, new Object[][]{{200L}});
            EPAssertionUtil.assertPropsPerRow(env.statement("create-two").iterator(), fieldsVar2, new Object[][]{{201L}});

            env.milestone(0);

            sendSupportBean(env, "E2", 200);
            EPAssertionUtil.assertProps(env.listener("create-one").getLastNewData()[0], fieldsVar1, new Object[]{400L});
            EPAssertionUtil.assertProps(env.listener("create-one").getLastOldData()[0], fieldsVar1, new Object[]{200L});
            env.listener("create-one").reset();
            EPAssertionUtil.assertProps(env.listener("create-two").getLastNewData()[0], fieldsVar2, new Object[]{401L});
            EPAssertionUtil.assertProps(env.listener("create-two").getLastOldData()[0], fieldsVar2, new Object[]{201L});
            env.listener("create-one").reset();
            EPAssertionUtil.assertPropsPerRow(env.statement("create-one").iterator(), fieldsVar1, new Object[][]{{400L}});
            EPAssertionUtil.assertPropsPerRow(env.statement("create-two").iterator(), fieldsVar2, new Object[][]{{401L}});

            env.undeployModuleContaining("set");
            env.undeployModuleContaining("create-two");
            env.compileDeploy(stmtCreateTextTwo);

            EPAssertionUtil.assertPropsPerRow(env.statement("create-one").iterator(), fieldsVar1, new Object[][]{{400L}});
            EPAssertionUtil.assertPropsPerRow(env.statement("create-two").iterator(), fieldsVar2, new Object[][]{{20L}});
            env.undeployAll();
        }
    }

    private static class EPLVariableDeclarationAndSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Object[][] variables = new Object[][]{
                {"varX1", "int", "1", 1},
                {"varX2", "int", "'2'", 2},
                {"varX3", "INTEGER", " 3+2 ", 5},
                {"varX4", "bool", " true|false ", true},
                {"varX5", "boolean", " varX1=1 ", true},
                {"varX6", "double", " 1.11 ", 1.11d},
                {"varX7", "double", " 1.20d ", 1.20d},
                {"varX8", "Double", " ' 1.12 ' ", 1.12d},
                {"varX9", "float", " 1.13f*2f ", 2.26f},
                {"varX10", "FLOAT", " -1.14f ", -1.14f},
                {"varX11", "string", " ' XXXX ' ", " XXXX "},
                {"varX12", "string", " \"a\" ", "a"},
                {"varX13", "character", "'a'", 'a'},
                {"varX14", "char", "'x'", 'x'},
                {"varX15", "short", " 20 ", (short) 20},
                {"varX16", "SHORT", " ' 9 ' ", (short) 9},
                {"varX17", "long", " 20*2 ", (long) 40},
                {"varX18", "LONG", " ' 9 ' ", (long) 9},
                {"varX19", "byte", " 20*2 ", (byte) 40},
                {"varX20", "BYTE", "9+1", (byte) 10},
                {"varX21", "int", null, null},
                {"varX22", "bool", null, null},
                {"varX23", "double", null, null},
                {"varX24", "float", null, null},
                {"varX25", "string", null, null},
                {"varX26", "char", null, null},
                {"varX27", "short", null, null},
                {"varX28", "long", null, null},
                {"varX29", "BYTE", null, null},
            };

            RegressionPath path = new RegressionPath();
            for (int i = 0; i < variables.length; i++) {
                String text = "create variable " + variables[i][1] + " " + variables[i][0];
                if (variables[i][2] != null) {
                    text += " = " + variables[i][2];
                }
                env.compileDeploy(text, path);
            }

            env.milestone(0);

            // select all variables
            StringBuilder buf = new StringBuilder();
            String delimiter = "";
            buf.append("@name('s0') select ");
            for (int i = 0; i < variables.length; i++) {
                buf.append(delimiter);
                buf.append(variables[i][0]);
                delimiter = ",";
            }
            buf.append(" from SupportBean");
            env.compileDeploy(buf.toString(), path).addListener("s0");

            // assert initialization values
            sendSupportBean(env, "E1", 1);
            EventBean received = env.listener("s0").assertOneGetNewAndReset();
            for (int i = 0; i < variables.length; i++) {
                assertEquals("Failed for " + Arrays.toString(variables[i]), variables[i][3], received.get((String) variables[i][0]));
            }

            env.undeployAll();
        }
    }

    private static class EPLVariableInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmt = "create variable somedummy myvar = 10";
            tryInvalidCompile(env, stmt, "Cannot create variable 'myvar', type 'somedummy' is not a recognized type [create variable somedummy myvar = 10]");

            stmt = "create variable string myvar = 5";
            tryInvalidCompile(env, stmt, "Variable 'myvar' of declared type java.lang.String cannot be initialized by a value of type java.lang.Integer [create variable string myvar = 5]");

            stmt = "create variable string myvar = 'a'";
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable string myvar = 'a'", path);
            tryInvalidCompile(env, path, stmt, "A variable by name 'myvar' has already been declared");

            tryInvalidCompile(env, "select * from SupportBean output every somevar events",
                "Error in the output rate limiting clause: Variable named 'somevar' has not been declared [");

            env.undeployAll();
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }
}
