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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.variable.VariableConstantValueException;
import com.espertech.esper.common.client.variable.VariableNotFoundException;
import com.espertech.esper.common.client.variable.VariableValueException;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.support.*;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.filter.SupportFilterHelper;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterItem;
import com.espertech.esper.runtime.internal.kernel.service.EPVariableServiceSPI;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.*;

public class EPLVariablesUse {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLVariableUseSimplePreconfigured());
        execs.add(new EPLVariableUseSimpleSameModule());
        execs.add(new EPLVariableUseSimpleTwoModules());
        execs.add(new EPLVariableUseEPRuntime());
        execs.add(new EPLVariableUseDotSeparateThread());
        execs.add(new EPLVariableUseInvokeMethod());
        execs.add(new EPLVariableUseConstantVariable());
        execs.add(new EPLVariableUseVariableInFilterBoolean());
        execs.add(new EPLVariableUseVariableInFilter());
        execs.add(new EPLVariableUseFilterConstantCustomTypePreconfigured());
        execs.add(new EPLVariableUseWVarargs());
        return execs;
    }

    private static class EPLVariableUseWVarargs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean(varargsTestClient.functionWithVarargs(longBoxed, varargsTestClient.getTestObject(theString))) as t";
            env.compileDeploy(epl).addListener("s0");

            SupportBean sb = new SupportBean("5", 0);
            sb.setLongBoxed(5L);
            env.sendEventBean(sb);
            env.listener("s0").assertOneGetNewAndReset();

            env.undeployAll();
        }
    }

    private static class EPLVariableUseFilterConstantCustomTypePreconfigured implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from MyVariableCustomEvent(name=my_variable_custom_typed)").addListener("s0");

            env.sendEventBean(new MyVariableCustomEvent(MyVariableCustomType.of("abc")));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLVariableUseSimplePreconfigured implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select var_simple_preconfig_const as c0 from SupportBean").addListener("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 0));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.milestone(1);

            env.undeployAll();
        }
    }

    private static class EPLVariableUseSimpleSameModule implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create variable boolean var_simple_module_const = true;\n" +
                "@name('s0') select var_simple_module_const as c0 from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            env.milestone(0);
            env.sendEventBean(new SupportBean("E1", 0));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("c0"));
            env.undeployAll();
        }
    }

    private static class EPLVariableUseSimpleTwoModules implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable boolean var_simple_twomodule_const = true", path);
            env.compileDeploy("@name('s0') select var_simple_twomodule_const as c0 from SupportBean", path);
            env.addListener("s0");
            env.milestone(0);
            env.sendEventBean(new SupportBean("E1", 0));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("c0"));
            env.undeployAll();
        }
    }

    private static class EPLVariableUseDotSeparateThread implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.runtime().getVariableService().setVariableValue(null, "mySimpleVariableService", new EPLVariablesUse.MySimpleVariableService());

            EPStatement epStatement = env.compileDeploy("@Name('s0') select mySimpleVariableService.doSomething() as c0 from SupportBean").statement("s0");

            final CountDownLatch latch = new CountDownLatch(1);
            final List<String> values = new ArrayList<String>();
            epStatement.setSubscriber(new Object() {
                public void update(final Map<?, ?> event) {
                    String value = (String) event.get("c0");
                    values.add(value);
                    latch.countDown();
                }
            });

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(new Runnable() {
                public void run() {
                    env.sendEventBean(new SupportBean());
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                fail();
            }
            executorService.shutdownNow();

            assertEquals(1, values.size());
            assertEquals("hello", values.get(0));

            env.undeployAll();
        }
    }

    private static class EPLVariableUseInvokeMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // declared via EPL
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create constant variable MySimpleVariableService myService = MySimpleVariableServiceFactory.makeService()", path);

            // exercise
            String epl = "@name('s0') select " +
                "myService.doSomething() as c0, " +
                "myInitService.doSomething() as c1 " +
                "from SupportBean";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{"hello", "hello"});

            env.undeployAll();
        }
    }

    private static class EPLVariableUseConstantVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            env.compileDeploy("create const variable int MYCONST = 10", path);
            tryOperator(env, path, "MYCONST = intBoxed", new Object[][]{{10, true}, {9, false}, {null, false}});

            tryOperator(env, path, "MYCONST > intBoxed", new Object[][]{{11, false}, {10, false}, {9, true}, {8, true}});
            tryOperator(env, path, "MYCONST >= intBoxed", new Object[][]{{11, false}, {10, true}, {9, true}, {8, true}});
            tryOperator(env, path, "MYCONST < intBoxed", new Object[][]{{11, true}, {10, false}, {9, false}, {8, false}});
            tryOperator(env, path, "MYCONST <= intBoxed", new Object[][]{{11, true}, {10, true}, {9, false}, {8, false}});

            tryOperator(env, path, "intBoxed < MYCONST", new Object[][]{{11, false}, {10, false}, {9, true}, {8, true}});
            tryOperator(env, path, "intBoxed <= MYCONST", new Object[][]{{11, false}, {10, true}, {9, true}, {8, true}});
            tryOperator(env, path, "intBoxed > MYCONST", new Object[][]{{11, true}, {10, false}, {9, false}, {8, false}});
            tryOperator(env, path, "intBoxed >= MYCONST", new Object[][]{{11, true}, {10, true}, {9, false}, {8, false}});

            tryOperator(env, path, "intBoxed in (MYCONST)", new Object[][]{{11, false}, {10, true}, {9, false}, {8, false}});
            tryOperator(env, path, "intBoxed between MYCONST and MYCONST", new Object[][]{{11, false}, {10, true}, {9, false}, {8, false}});

            tryOperator(env, path, "MYCONST != intBoxed", new Object[][]{{10, false}, {9, true}, {null, false}});
            tryOperator(env, path, "intBoxed != MYCONST", new Object[][]{{10, false}, {9, true}, {null, false}});

            tryOperator(env, path, "intBoxed not in (MYCONST)", new Object[][]{{11, true}, {10, false}, {9, true}, {8, true}});
            tryOperator(env, path, "intBoxed not between MYCONST and MYCONST", new Object[][]{{11, true}, {10, false}, {9, true}, {8, true}});

            tryOperator(env, path, "MYCONST is intBoxed", new Object[][]{{10, true}, {9, false}, {null, false}});
            tryOperator(env, path, "intBoxed is MYCONST", new Object[][]{{10, true}, {9, false}, {null, false}});

            tryOperator(env, path, "MYCONST is not intBoxed", new Object[][]{{10, false}, {9, true}, {null, true}});
            tryOperator(env, path, "intBoxed is not MYCONST", new Object[][]{{10, false}, {9, true}, {null, true}});

            // try coercion
            tryOperator(env, path, "MYCONST = shortBoxed", new Object[][]{{(short) 10, true}, {(short) 9, false}, {null, false}});
            tryOperator(env, path, "shortBoxed = MYCONST", new Object[][]{{(short) 10, true}, {(short) 9, false}, {null, false}});

            tryOperator(env, path, "MYCONST > shortBoxed", new Object[][]{{(short) 11, false}, {(short) 10, false}, {(short) 9, true}, {(short) 8, true}});
            tryOperator(env, path, "shortBoxed < MYCONST", new Object[][]{{(short) 11, false}, {(short) 10, false}, {(short) 9, true}, {(short) 8, true}});

            tryOperator(env, path, "shortBoxed in (MYCONST)", new Object[][]{{(short) 11, false}, {(short) 10, true}, {(short) 9, false}, {(short) 8, false}});

            // test SODA
            env.undeployAll();

            String epl = "@name('variable') create constant variable int MYCONST = 10";
            env.eplToModelCompileDeploy(epl);

            // test invalid
            tryInvalidCompile(env, path, "on SupportBean set MYCONST = 10",
                "Failed to validate assignment expression 'MYCONST=10': Variable by name 'MYCONST' is declared constant and may not be set [on SupportBean set MYCONST = 10]");
            tryInvalidCompile(env, path, "select * from SupportBean output when true then set MYCONST=1",
                "Failed to validate the output rate limiting clause: Failed to validate assignment expression 'MYCONST=1': Variable by name 'MYCONST' is declared constant and may not be set [select * from SupportBean output when true then set MYCONST=1]");

            // assure no update via API
            tryInvalidSetAPI(env, env.deploymentId("variable"), "MYCONST", 1);

            // add constant variable via config API
            tryInvalidSetAPI(env, null, "MYCONST_TWO", "dummy");
            tryInvalidSetAPI(env, null, "MYCONST_THREE", false);

            // try ESPER-653
            env.compileDeploy("@name('s0') create constant variable java.util.Date START_TIME = java.util.Calendar.getInstance().getTime()");
            Object value = env.iterator("s0").next().get("START_TIME");
            assertNotNull(value);
            env.undeployModuleContaining("s0");

            // test array constant
            env.undeployAll();
            env.compileDeploy("create constant variable string[] var_strings = {'E1', 'E2'}", path);
            env.compileDeploy("@name('s0') select var_strings from SupportBean", path);
            assertEquals(String[].class, env.statement("s0").getEventType().getPropertyType("var_strings"));
            env.undeployModuleContaining("s0");

            tryAssertionArrayVar(env, path, "var_strings");

            tryOperator(env, path, "intBoxed in (10, 8)", new Object[][]{{11, false}, {10, true}, {9, false}, {8, true}});

            env.compileDeploy("create constant variable int [ ] var_ints = {8, 10}", path);
            tryOperator(env, path, "intBoxed in (var_ints)", new Object[][]{{11, false}, {10, true}, {9, false}, {8, true}});

            env.compileDeploy("create constant variable int[]  var_intstwo = {9}", path);
            tryOperator(env, path, "intBoxed in (var_ints, var_intstwo)", new Object[][]{{11, false}, {10, true}, {9, true}, {8, true}});

            tryInvalidCompile(env, "create constant variable SupportBean[] var_beans",
                "Cannot create variable 'var_beans', type 'SupportBean' cannot be declared as an array type as it is an event type [create constant variable SupportBean[] var_beans]");

            // test array of primitives
            env.compileDeploy("@name('s0') create variable byte[] myBytesBoxed");
            Object[][] expectedType = new Object[][]{{"myBytesBoxed", Byte[].class}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);
            env.undeployModuleContaining("s0");

            env.compileDeploy("@name('s0') create variable byte[primitive] myBytesPrimitive");
            expectedType = new Object[][]{{"myBytesPrimitive", byte[].class}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);
            env.undeployAll();

            // test enum constant
            env.compileDeploy("create constant variable SupportEnum var_enumone = SupportEnum.ENUM_VALUE_2", path);
            tryOperator(env, path, "var_enumone = enumValue", new Object[][]{{SupportEnum.ENUM_VALUE_3, false}, {SupportEnum.ENUM_VALUE_2, true}, {SupportEnum.ENUM_VALUE_1, false}});

            env.compileDeploy("create constant variable SupportEnum[] var_enumarr = {SupportEnum.ENUM_VALUE_2, SupportEnum.ENUM_VALUE_1}", path);
            tryOperator(env, path, "enumValue in (var_enumarr, var_enumone)", new Object[][]{{SupportEnum.ENUM_VALUE_3, false}, {SupportEnum.ENUM_VALUE_2, true}, {SupportEnum.ENUM_VALUE_1, true}});

            env.compileDeploy("create variable SupportEnum var_enumtwo = SupportEnum.ENUM_VALUE_2", path);
            env.compileDeploy("on SupportBean set var_enumtwo = enumValue", path);

            env.undeployAll();
        }

        private static void tryAssertionArrayVar(RegressionEnvironment env, RegressionPath path, String varName) {
            env.compileDeploy("@name('s0') select * from SupportBean(theString in (" + varName + "))", path).addListener("s0");

            sendBeanAssert(env, "E1", true);
            sendBeanAssert(env, "E2", true);
            sendBeanAssert(env, "E3", false);

            env.undeployAll();
        }

        private static void sendBeanAssert(RegressionEnvironment env, String theString, boolean expected) {
            env.sendEventBean(new SupportBean(theString, 1));
            assertEquals(expected, env.listener("s0").getAndClearIsInvoked());
        }
    }

    private static class EPLVariableUseEPRuntime implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPVariableServiceSPI runtimeSPI = (EPVariableServiceSPI) env.runtime().getVariableService();
            Map<DeploymentIdNamePair, Class> types = runtimeSPI.getVariableTypeAll();
            assertEquals(Integer.class, types.get(new DeploymentIdNamePair(null, "var1")));
            assertEquals(String.class, types.get(new DeploymentIdNamePair(null, "var2")));

            assertEquals(Integer.class, runtimeSPI.getVariableType(null, "var1"));
            assertEquals(String.class, runtimeSPI.getVariableType(null, "var2"));

            String stmtTextSet = "on SupportBean set var1 = intPrimitive, var2 = theString";
            env.compileDeploy(stmtTextSet);

            assertVariableValuesPreconfigured(env, new String[]{"var1", "var2"}, new Object[]{-1, "abc"});
            sendSupportBean(env, null, 99);
            assertVariableValuesPreconfigured(env, new String[]{"var1", "var2"}, new Object[]{99, null});

            env.runtime().getVariableService().setVariableValue(null, "var2", "def");
            assertVariableValuesPreconfigured(env, new String[]{"var1", "var2"}, new Object[]{99, "def"});

            env.milestone(0);

            assertVariableValuesPreconfigured(env, new String[]{"var1", "var2"}, new Object[]{99, "def"});
            env.runtime().getVariableService().setVariableValue(null, "var1", 123);
            assertVariableValuesPreconfigured(env, new String[]{"var1", "var2"}, new Object[]{123, "def"});

            env.milestone(1);

            assertVariableValuesPreconfigured(env, new String[]{"var1", "var2"}, new Object[]{123, "def"});
            Map<DeploymentIdNamePair, Object> newValues = new HashMap<>();
            newValues.put(new DeploymentIdNamePair(null, "var1"), 20);
            env.runtime().getVariableService().setVariableValue(newValues);
            assertVariableValuesPreconfigured(env, new String[]{"var1", "var2"}, new Object[]{20, "def"});

            newValues.put(new DeploymentIdNamePair(null, "var1"), (byte) 21);
            newValues.put(new DeploymentIdNamePair(null, "var2"), "test");
            env.runtime().getVariableService().setVariableValue(newValues);
            assertVariableValuesPreconfigured(env, new String[]{"var1", "var2"}, new Object[]{21, "test"});

            newValues.put(new DeploymentIdNamePair(null, "var1"), null);
            newValues.put(new DeploymentIdNamePair(null, "var2"), null);
            env.runtime().getVariableService().setVariableValue(newValues);
            assertVariableValuesPreconfigured(env, new String[]{"var1", "var2"}, new Object[]{null, null});

            // try variable not found
            try {
                env.runtime().getVariableService().setVariableValue(null, "dummy", null);
                fail();
            } catch (VariableNotFoundException ex) {
                // expected
                assertEquals("Variable by name 'dummy' has not been declared", ex.getMessage());
            }

            // try variable not found
            try {
                newValues.put(new DeploymentIdNamePair(null, "dummy2"), 20);
                env.runtime().getVariableService().setVariableValue(newValues);
                fail();
            } catch (VariableNotFoundException ex) {
                // expected
                assertEquals("Variable by name 'dummy2' has not been declared", ex.getMessage());
            }

            // create new variable on the fly
            env.compileDeploy("@name('create') create variable int dummy = 20 + 20");
            assertEquals(40, env.runtime().getVariableService().getVariableValue(env.deploymentId("create"), "dummy"));

            // try type coercion
            try {
                env.runtime().getVariableService().setVariableValue(env.deploymentId("create"), "dummy", "abc");
                fail();
            } catch (VariableValueException ex) {
                // expected
                assertEquals("Variable 'dummy' of declared type java.lang.Integer cannot be assigned a value of type java.lang.String", ex.getMessage());
            }

            try {
                env.runtime().getVariableService().setVariableValue(env.deploymentId("create"), "dummy", 100L);
                fail();
            } catch (VariableValueException ex) {
                // expected
                assertEquals("Variable 'dummy' of declared type java.lang.Integer cannot be assigned a value of type java.lang.Long", ex.getMessage());
            }

            try {
                env.runtime().getVariableService().setVariableValue(null, "var2", 0);
                fail();
            } catch (VariableValueException ex) {
                // expected
                assertEquals("Variable 'var2' of declared type java.lang.String cannot be assigned a value of type java.lang.Integer", ex.getMessage());
            }

            // coercion
            env.runtime().getVariableService().setVariableValue(null, "var1", (short) -1);
            assertVariableValuesPreconfigured(env, new String[]{"var1", "var2"}, new Object[]{-1, null});

            // rollback for coercion failed
            newValues = new LinkedHashMap<>();    // preserve order
            newValues.put(new DeploymentIdNamePair(null, "var2"), "xyz");
            newValues.put(new DeploymentIdNamePair(null, "var1"), 4.4d);
            try {
                env.runtime().getVariableService().setVariableValue(newValues);
                fail();
            } catch (VariableValueException ex) {
                // expected
            }
            assertVariableValuesPreconfigured(env, new String[]{"var1", "var2"}, new Object[]{-1, null});

            // rollback for variable not found
            newValues = new LinkedHashMap<>();    // preserve order
            newValues.put(new DeploymentIdNamePair(null, "var2"), "xyz");
            newValues.put(new DeploymentIdNamePair(null, "var1"), 1);
            newValues.put(new DeploymentIdNamePair(null, "notfoundvariable"), null);
            try {
                env.runtime().getVariableService().setVariableValue(newValues);
                fail();
            } catch (VariableNotFoundException ex) {
                // expected
            }
            assertVariableValuesPreconfigured(env, new String[]{"var1", "var2"}, new Object[]{-1, null});

            env.undeployAll();
        }
    }

    private static class EPLVariableUseVariableInFilterBoolean implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String stmtTextSet = "@name('set') on SupportBean_S0 set var1IFB = p00, var2IFB = p01";
            env.compileDeploy(stmtTextSet).addListener("set");
            String[] fieldsVar = new String[]{"var1IFB", "var2IFB"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{null, null}});

            String stmtTextSelect = "@name('s0') select theString, intPrimitive from SupportBean(theString = var1IFB or theString = var2IFB)";
            String[] fieldsSelect = new String[]{"theString", "intPrimitive"};
            env.compileDeploy(stmtTextSelect).addListener("s0");

            sendSupportBean(env, null, 1);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            sendSupportBeanS0NewThread(env, 100, "a", "b");
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{"a", "b"});

            sendSupportBean(env, "a", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{"a", 2});

            sendSupportBean(env, null, 1);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendSupportBean(env, "b", 3);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{"b", 3});

            sendSupportBean(env, "c", 4);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            sendSupportBeanS0NewThread(env, 100, "e", "c");
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{"e", "c"});

            sendSupportBean(env, "c", 5);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{"c", 5});

            sendSupportBean(env, "e", 6);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{"e", 6});

            env.undeployAll();
        }
    }

    private static class EPLVariableUseVariableInFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String stmtTextSet = "@name('set') on SupportBean_S0 set var1IF = p00";
            env.compileDeploy(stmtTextSet).addListener("set");
            String[] fieldsVar = new String[]{"var1IF"};
            EPAssertionUtil.assertPropsPerRow(env.iterator("set"), fieldsVar, new Object[][]{{null}});

            String stmtTextSelect = "@name('s0') select theString, intPrimitive from SupportBean(theString = var1IF)";
            String[] fieldsSelect = new String[]{"theString", "intPrimitive"};
            env.compileDeploy(stmtTextSelect).addListener("s0");

            sendSupportBean(env, null, 1);
            assertFalse(env.listener("s0").isInvoked());

            sendSupportBeanS0NewThread(env, 100, "a", "b");
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{"a"});

            sendSupportBean(env, "a", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{"a", 2});

            env.milestone(0);

            sendSupportBean(env, null, 1);
            assertFalse(env.listener("s0").isInvoked());

            sendSupportBeanS0NewThread(env, 100, "e", "c");
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsVar, new Object[]{"e"});

            env.milestone(1);

            sendSupportBean(env, "c", 5);
            assertFalse(env.listener("s0").isInvoked());

            sendSupportBean(env, "e", 6);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect, new Object[]{"e", 6});

            env.undeployAll();
        }
    }

    private static SupportBean sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
        return bean;
    }

    private static void sendSupportBeanS0NewThread(RegressionEnvironment env, final int id, final String p00, final String p01) {
        try {
            Thread t = new Thread() {
                public void run() {
                    env.sendEventBean(new SupportBean_S0(id, p00, p01));
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

    private static void assertVariableValuesPreconfigured(RegressionEnvironment env, String[] names, Object[] values) {
        assertEquals(names.length, values.length);

        // assert one-by-one
        for (int i = 0; i < names.length; i++) {
            assertEquals(values[i], env.runtime().getVariableService().getVariableValue(null, names[i]));
        }

        // get and assert all
        Map<DeploymentIdNamePair, Object> all = env.runtime().getVariableService().getVariableValueAll();
        for (int i = 0; i < names.length; i++) {
            assertEquals(values[i], all.get(new DeploymentIdNamePair(null, names[i])));
        }

        // get by request
        Set<DeploymentIdNamePair> nameSet = new HashSet<>();
        for (String name : names) {
            nameSet.add(new DeploymentIdNamePair(null, name));
        }
        Map<DeploymentIdNamePair, Object> valueSet = env.runtime().getVariableService().getVariableValue(nameSet);
        for (int i = 0; i < names.length; i++) {
            assertEquals(values[i], valueSet.get(new DeploymentIdNamePair(null, names[i])));
        }
    }

    public static class A implements Serializable {
        public String getValue() {
            return "";
        }
    }

    public static class B {
    }

    private static void tryInvalidSetAPI(RegressionEnvironment env, String deploymentId, String variableName, Object newValue) {
        try {
            env.runtime().getVariableService().setVariableValue(deploymentId, variableName, newValue);
            fail();
        } catch (VariableConstantValueException ex) {
            assertEquals(ex.getMessage(), "Variable by name '" + variableName + "' is declared as constant and may not be assigned a new value");
        }
        try {
            env.runtime().getVariableService().setVariableValue(Collections.<DeploymentIdNamePair, Object>singletonMap(new DeploymentIdNamePair(deploymentId, variableName), newValue));
            fail();
        } catch (VariableConstantValueException ex) {
            assertEquals(ex.getMessage(), "Variable by name '" + variableName + "' is declared as constant and may not be assigned a new value");
        }
    }

    private static void tryOperator(RegressionEnvironment env, RegressionPath path, String operator, Object[][] testdata) {
        env.compileDeploy("@name('s0') select theString as c0,intPrimitive as c1 from SupportBean(" + operator + ")", path);
        env.addListener("s0");

        // initiate
        env.sendEventBean(new SupportBean_S0(10, "S01"));

        for (int i = 0; i < testdata.length; i++) {
            SupportBean bean = new SupportBean();
            Object testValue = testdata[i][0];
            if (testValue instanceof Integer) {
                bean.setIntBoxed((Integer) testValue);
            } else if (testValue instanceof SupportEnum) {
                bean.setEnumValue((SupportEnum) testValue);
            } else {
                bean.setShortBoxed((Short) testValue);
            }
            boolean expected = (Boolean) testdata[i][1];

            env.sendEventBean(bean);
            assertEquals("Failed at " + i, expected, env.listener("s0").getAndClearIsInvoked());
        }

        // assert type of expression
        FilterItem item = SupportFilterHelper.getFilterSingle(env.statement("s0"));
        assertTrue(item.getOp() != FilterOperator.BOOLEAN_EXPRESSION);

        env.undeployModuleContaining("s0");
    }

    public static class MySimpleVariableServiceFactory {
        public static MySimpleVariableService makeService() {
            return new MySimpleVariableService();
        }
    }

    public static class MySimpleVariableService {
        public String doSomething() {
            return "hello";
        }
    }

    public enum MyEnumWithOverride {

        LONG {
            @Override
            public int getValue() {
                return 1;
            }
        },
        SHORT {
            @Override
            public int getValue() {
                return -1;
            }
        };

        public abstract int getValue();
    }

    public static class MyVariableCustomEvent {
        private final MyVariableCustomType name;

        MyVariableCustomEvent(MyVariableCustomType name) {
            this.name = name;
        }

        public MyVariableCustomType getName() {
            return name;
        }
    }

    public static class MyVariableCustomType {
        private final String name;

        MyVariableCustomType(String name) {
            this.name = name;
        }

        public static MyVariableCustomType of(String name) {
            return new MyVariableCustomType(name);
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MyVariableCustomType myType = (MyVariableCustomType) o;
            return Objects.equals(name, myType.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    public static class SupportVarargsObject {

        private Long value;

        public SupportVarargsObject(Long value) {
            this.value = value;
        }

        public Long getValue() {
            return value;
        }

        public void setValue(Long value) {
            this.value = value;
        }
    }

    public interface SupportVarargsClient extends Serializable {

        boolean functionWithVarargs(Long longValue, Object... objects);

        public SupportVarargsObject getTestObject(String stringValue);
    }

    public static class SupportVarargsClientImpl implements SupportVarargsClient {

        public boolean functionWithVarargs(Long longValue, Object... objects) {
            SupportVarargsObject obj = (SupportVarargsObject) objects[0];
            return longValue.equals(obj.getValue());
        }

        public SupportVarargsObject getTestObject(String stringValue) {
            return new SupportVarargsObject(Long.parseLong(stringValue));
        }
    }
}
