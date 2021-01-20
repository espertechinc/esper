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
package com.espertech.esper.regressionlib.suite.expr.exprcore;

import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanWithArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExprCoreArrayAtElement {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreAAEPropRootedTopLevelProp(false));
        executions.add(new ExprCoreAAEPropRootedTopLevelProp(true));
        executions.add(new ExprCoreAAEPropRootedNestedProp(false));
        executions.add(new ExprCoreAAEPropRootedNestedProp(true));
        executions.add(new ExprCoreAAEPropRootedNestedNestedProp(false));
        executions.add(new ExprCoreAAEPropRootedNestedNestedProp(true));
        executions.add(new ExprCoreAAEPropRootedNestedArrayProp());
        executions.add(new ExprCoreAAEPropRootedNestedNestedArrayProp());
        executions.add(new ExprCoreAAEVariableRootedTopLevelProp(false));
        executions.add(new ExprCoreAAEVariableRootedTopLevelProp(true));
        executions.add(new ExprCoreAAEVariableRootedChained());
        executions.add(new ExprCoreAAEWithStaticMethodAndUDF(false));
        executions.add(new ExprCoreAAEWithStaticMethodAndUDF(true));
        executions.add(new ExprCoreAAEAdditionalInvalid());
        executions.add(new ExprCoreAAEWithStringSplit());
        return executions;
    }

    private static class ExprCoreAAEWithStringSplit implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select new String('a,b').split(',')[intPrimitive] as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");
            env.assertStatement("s0", statement -> assertEquals(String.class, statement.getEventType().getPropertyType("c0")));

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertPropsNew("s0", "c0".split(","), new Object[] {"b"});

            env.undeployAll();
        }
    }

    private static class ExprCoreAAEAdditionalInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplNoAnArrayIsString =
                    "create schema Lvl3 (id string);\n" +
                    "create schema Lvl2 (lvl3 Lvl3);\n" +
                    "create schema Lvl1 (lvl2 Lvl2);\n" +
                    "create schema Lvl0 (lvl1 Lvl1, indexNumber int);\n" +
                    "select lvl1.lvl2.lvl3.id[indexNumber] from Lvl0;\n";
            env.tryInvalidCompile(eplNoAnArrayIsString,
                "Failed to validate select-clause expression 'lvl1.lvl2.lvl3.id[indexNumber]': Could not perform array operation on type String");

            String eplNoAnArrayIsType =
                "create schema Lvl3 (id string);\n" +
                    "create schema Lvl2 (lvl3 Lvl3);\n" +
                    "create schema Lvl1 (lvl2 Lvl2);\n" +
                    "create schema Lvl0 (lvl1 Lvl1, indexNumber int);\n" +
                    "select lvl1.lvl2.lvl3[indexNumber] from Lvl0;\n";
            env.tryInvalidCompile(eplNoAnArrayIsType,
                "Failed to validate select-clause expression 'lvl1.lvl2.lvl3[indexNumber]': Could not perform array operation on type event type 'Lvl3'");
        }
    }

    private static class ExprCoreAAEWithStaticMethodAndUDF implements RegressionExecution {
        private boolean soda;

        public ExprCoreAAEWithStaticMethodAndUDF(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') inlined_class \"\"\"\n" +
                "  import com.espertech.esper.common.client.hook.singlerowfunc.*;\n" +
                "  @ExtensionSingleRowFunction(name=\"toArray\", methodName=\"toArray\")\n" +
                "  public class Helper {\n" +
                "    public static int[] toArray(int a, int b) {\n" +
                "      return new int[] {a, b};\n" +
                "    }\n" +
                "  }\n" +
                "\"\"\" " +
                "select " +
                ExprCoreArrayAtElement.class.getName() + ".getIntArray()[intPrimitive] as c0, " +
                ExprCoreArrayAtElement.class.getName() + ".getIntArray2Dim()[intPrimitive][intPrimitive] as c1, " +
                "toArray(3,30)[intPrimitive] as c2 " +
                "from SupportBean";
            env.compileDeploy(soda, epl).addListener("s0");
            String[] fields = "c0,c1,c2".split(",");
            env.assertStmtTypesAllSame("s0", fields, EPTypePremade.INTEGERBOXED.getEPType());

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertPropsNew("s0", fields, new Object[] {10, 20, 30});

            env.sendEventBean(new SupportBean("E2", 0));
            env.assertPropsNew("s0", fields, new Object[] {1, 1, 3});

            env.sendEventBean(new SupportBean("E3", 2));
            env.assertPropsNew("s0", fields, new Object[] {null, null, null});

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    private static class ExprCoreAAEVariableRootedChained implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "import " + MyHolder.class.getName() + ";\n" +
                "create variable MyHolder[] var_mh = new MyHolder[] {new MyHolder('a'), new MyHolder('b')};\n" +
                "@name('s0') select var_mh[intPrimitive].getId() as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = "c0".split(",");
            env.assertStmtTypesAllSame("s0", fields, EPTypePremade.STRING.getEPType());

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertPropsNew("s0", fields, new Object[]{"b"});

            env.undeployAll();
        }
    }

    private static class ExprCoreAAEVariableRootedTopLevelProp implements RegressionExecution {
        private final boolean soda;

        public ExprCoreAAEVariableRootedTopLevelProp(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplVariableIntArray = "@public create variable int[primitive] var_intarr = new int[] {1,2,3}";
            env.compileDeploy(soda, eplVariableIntArray, path);
            String eplVariableSBArray = "@public create variable " + MyHolder.class.getName() + " var_ = null";
            env.compileDeploy(soda, eplVariableSBArray, path);

            String epl = "@name('s0') select var_intarr[intPrimitive] as c0 from SupportBean";
            env.compileDeploy(soda, epl, path).addListener("s0");
            String[] fields = "c0".split(",");
            env.assertStmtTypesAllSame("s0", fields, EPTypePremade.INTEGERBOXED.getEPType());

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertPropsNew("s0", fields, new Object[]{2});

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    private static class ExprCoreAAEPropRootedNestedNestedArrayProp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplSchema = "@public create schema Lvl2(id string);\n" +
                "@public create schema Lvl1(lvl2 Lvl2[]);\n" +
                "@public @buseventtype create schema Lvl0(lvl1 Lvl1, indexNumber int, lvl0id string);\n";
            env.compileDeploy(eplSchema, path);

            String epl = "@name('s0') select lvl1.lvl2[indexNumber].id as c0, me.lvl1.lvl2[indexNumber].id as c1 from Lvl0 as me";
            env.compileDeploy(epl, path).addListener("s0");
            String[] fields = "c0,c1".split(",");
            env.assertStmtTypesAllSame("s0", fields, EPTypePremade.STRING.getEPType());

            Map<String, Object> lvl2One = CollectionUtil.buildMap("id", "a");
            Map<String, Object> lvl2Two = CollectionUtil.buildMap("id", "b");
            Map<String, Object> lvl1 = CollectionUtil.buildMap("lvl2", new Map[] {lvl2One, lvl2Two});
            Map<String, Object> lvl0 = CollectionUtil.buildMap("lvl1", lvl1, "indexNumber", 1);
            env.sendEventMap(lvl0, "Lvl0");
            env.assertPropsNew("s0", fields, new Object[]{"b", "b"});

            // Invalid tests
            // array value but no array provided
            env.tryInvalidCompile(path, "select lvl1.lvl2.id from Lvl0",
                "Failed to validate select-clause expression 'lvl1.lvl2.id': Failed to find a stream named 'lvl1' (did you mean 'Lvl0'?)");
            env.tryInvalidCompile(path, "select me.lvl1.lvl2.id from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1.lvl2.id': Failed to resolve property 'me.lvl1.lvl2.id' to a stream or nested property in a stream");

            // two index expressions
            env.tryInvalidCompile(path, "select lvl1.lvl2[indexNumber, indexNumber].id from Lvl0",
                "Failed to validate select-clause expression 'lvl1.lvl2[indexNumber,indexNumber].id': Incorrect number of index expressions for array operation, expected a single expression returning an integer value but received 2 expressions for operation on type collection of events of type 'Lvl2'");
            env.tryInvalidCompile(path, "select me.lvl1.lvl2[indexNumber, indexNumber].id from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1.lvl2[indexNumber,indexNumber].id': Incorrect number of index expressions for array operation, expected a single expression returning an integer value but received 2 expressions for operation on type collection of events of type 'Lvl2'");

            // double-array
            env.tryInvalidCompile(path, "select lvl1.lvl2[indexNumber][indexNumber].id from Lvl0",
                "Failed to validate select-clause expression 'lvl1.lvl2[indexNumber][indexNumber].id': Could not perform array operation on type event type 'Lvl2'");
            env.tryInvalidCompile(path, "select me.lvl1.lvl2[indexNumber][indexNumber].id from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1.lvl2[indexNumber][indexNumb...(41 chars)': Could not perform array operation on type event type 'Lvl2'");

            // wrong index expression type
            env.tryInvalidCompile(path, "select lvl1.lvl2[lvl0id].id from Lvl0",
                "Failed to validate select-clause expression 'lvl1.lvl2[lvl0id].id': Incorrect index expression for array operation, expected an expression returning an integer value but the expression 'lvl0id' returns 'String' for operation on type collection of events of type 'Lvl2'");
            env.tryInvalidCompile(path, "select me.lvl1.lvl2[lvl0id].id from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1.lvl2[lvl0id].id': Incorrect index expression for array operation, expected an expression returning an integer value but the expression 'lvl0id' returns 'String' for operation on type collection of events of type 'Lvl2'");

            env.undeployAll();
        }
    }

    private static class ExprCoreAAEPropRootedNestedNestedProp implements RegressionExecution {
        private final boolean soda;

        public ExprCoreAAEPropRootedNestedNestedProp(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplSchema = "@public create schema Lvl2(intarr int[]);\n" +
                "@public create schema Lvl1(lvl2 Lvl2);\n" +
                "@public @buseventtype create schema Lvl0(lvl1 Lvl1, indexNumber int, id string);\n";
            env.compileDeploy(eplSchema, path);

            String epl = "@name('s0') select " +
                "lvl1.lvl2.intarr[indexNumber] as c0, " +
                "lvl1.lvl2.intarr.size() as c1, " +
                "me.lvl1.lvl2.intarr[indexNumber] as c2, " +
                "me.lvl1.lvl2.intarr.size() as c3 " +
                "from Lvl0 as me";
            env.compileDeploy(soda, epl, path).addListener("s0");
            String[] fields = "c0,c1,c2,c3".split(",");
            env.assertStmtTypesAllSame("s0", fields, EPTypePremade.INTEGERBOXED.getEPType());

            Map<String, Object> lvl2 = CollectionUtil.buildMap("intarr", new Integer[]{1, 2, 3});
            Map<String, Object> lvl1 = CollectionUtil.buildMap("lvl2", lvl2);
            Map<String, Object> lvl0 = CollectionUtil.buildMap("lvl1", lvl1, "indexNumber", 2);
            env.sendEventMap(lvl0, "Lvl0");
            env.assertPropsNew("s0", fields, new Object[]{3, 3, 3, 3});

            // Invalid tests
            // not an index expression
            env.tryInvalidCompile(path, "select lvl1.lvl2[indexNumber] from Lvl0",
                "Failed to validate select-clause expression 'lvl1.lvl2[indexNumber]': Could not perform array operation on type event type 'Lvl2'");
            env.tryInvalidCompile(path, "select me.lvl1.lvl2[indexNumber] from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1.lvl2[indexNumber]': Could not perform array operation on type event type 'Lvl2'");

            // two index expressions
            env.tryInvalidCompile(path, "select lvl1.lvl2.intarr[indexNumber, indexNumber] from Lvl0",
                "Failed to validate select-clause expression 'lvl1.lvl2.intarr[indexNumber,indexN...(41 chars)': Incorrect number of index expressions for array operation, expected a single expression returning an integer value but received 2 expressions for operation on type Integer[]");
            env.tryInvalidCompile(path, "select me.lvl1.lvl2.intarr[indexNumber, indexNumber] from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1.lvl2.intarr[indexNumber,ind...(44 chars)': Incorrect number of index expressions for array operation, expected a single expression returning an integer value but received 2 expressions for operation on type Integer[]");

            // double-array
            env.tryInvalidCompile(path, "select lvl1.lvl2.intarr[indexNumber][indexNumber] from Lvl0",
                "Failed to validate select-clause expression 'lvl1.lvl2.intarr[indexNumber][index...(42 chars)': Could not perform array operation on type Integer");
            env.tryInvalidCompile(path, "select me.lvl1.lvl2.intarr[indexNumber][indexNumber] from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1.lvl2.intarr[indexNumber][in...(45 chars)': Could not perform array operation on type Integer");

            // wrong index expression type
            env.tryInvalidCompile(path, "select lvl1.lvl2.intarr[id] from Lvl0",
                "Failed to validate select-clause expression 'lvl1.lvl2.intarr[id]': Incorrect index expression for array operation, expected an expression returning an integer value but the expression 'id' returns 'String' for operation on type Integer[]");
            env.tryInvalidCompile(path, "select me.lvl1.lvl2.intarr[id] from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1.lvl2.intarr[id]': Incorrect index expression for array operation, expected an expression returning an integer value but the expression 'id' returns 'String' for operation on type Integer[]");

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    private static class ExprCoreAAEPropRootedNestedArrayProp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "create schema Lvl1(id string);\n" +
                "@public @buseventtype create schema Lvl0(lvl1 Lvl1[], indexNumber int, lvl0id string);\n" +
                "@name('s0') select lvl1[indexNumber].id as c0, me.lvl1[indexNumber].id as c1 from Lvl0 as me";
            env.compileDeploy(epl, path).addListener("s0");
            String[] fields = "c0,c1".split(",");
            env.assertStmtTypesAllSame("s0", fields, EPTypePremade.STRING.getEPType());

            Map<String, Object> lvl1One = CollectionUtil.buildMap("id", "a");
            Map<String, Object> lvl1Two = CollectionUtil.buildMap("id", "b");
            Map<String, Object> lvl0 = CollectionUtil.buildMap("lvl1", new Map[] {lvl1One, lvl1Two}, "indexNumber", 1);
            env.sendEventMap(lvl0, "Lvl0");
            env.assertPropsNew("s0", fields, new Object[]{"b", "b"});

            // Invalid tests
            // array value but no array provided
            env.tryInvalidCompile(path, "select lvl1.id from Lvl0",
                "Failed to validate select-clause expression 'lvl1.id': Failed to resolve property 'lvl1.id' (property 'lvl1' is an indexed property and requires an index or enumeration method to access values)");
            env.tryInvalidCompile(path, "select me.lvl1.id from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1.id': Property named 'lvl1.id' is not valid in stream 'me' (did you mean 'lvl0id'?)");

            // not an index expression
            env.tryInvalidCompile(path, "select lvl1.id[indexNumber] from Lvl0",
                "Failed to validate select-clause expression 'lvl1.id[indexNumber]': Could not find event property or method named 'id' in collection of events of type 'Lvl1'");
            env.tryInvalidCompile(path, "select me.lvl1.id[indexNumber] from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1.id[indexNumber]': Could not find event property or method named 'id' in collection of events of type 'Lvl1'");

            // two index expressions
            env.tryInvalidCompile(path, "select lvl1[indexNumber, indexNumber].id from Lvl0",
                "Failed to validate select-clause expression 'lvl1[indexNumber,indexNumber].id': Incorrect number of index expressions for array operation, expected a single expression returning an integer value but received 2 expressions for property 'lvl1'");
            env.tryInvalidCompile(path, "select me.lvl1[indexNumber, indexNumber].id from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1[indexNumber,indexNumber].id': Incorrect number of index expressions for array operation, expected a single expression returning an integer value but received 2 expressions for property 'lvl1'");

            // double-array
            env.tryInvalidCompile(path, "select lvl1[indexNumber][indexNumber].id from Lvl0",
                "Failed to validate select-clause expression 'lvl1[indexNumber][indexNumber].id': Could not perform array operation on type event type 'Lvl1'");
            env.tryInvalidCompile(path, "select me.lvl1[indexNumber][indexNumber].id from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1[indexNumber][indexNumber].id': Could not perform array operation on type event type 'Lvl1'");

            // wrong index expression type
            env.tryInvalidCompile(path, "select lvl1[lvl0id].id from Lvl0",
                "Failed to validate select-clause expression 'lvl1[lvl0id].id': Incorrect index expression for array operation, expected an expression returning an integer value but the expression 'lvl0id' returns 'String' for property 'lvl1'");
            env.tryInvalidCompile(path, "select me.lvl1[lvl0id].id from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1[lvl0id].id': Incorrect index expression for array operation, expected an expression returning an integer value but the expression 'lvl0id' returns 'String' for property 'lvl1'");

            env.undeployAll();
        }
    }

    private static class ExprCoreAAEPropRootedNestedProp implements RegressionExecution {
        private final boolean soda;

        public ExprCoreAAEPropRootedNestedProp(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplSchema =
                "@public create schema Lvl1(intarr int[]);\n" +
                    "@public @buseventtype create schema Lvl0(lvl1 Lvl1, indexNumber int, id string);\n";
            env.compileDeploy(eplSchema, path);

            String epl = "@name('s0') select " +
                "lvl1.intarr[indexNumber] as c0, " +
                "lvl1.intarr.size() as c1, " +
                "me.lvl1.intarr[indexNumber] as c2, " +
                "me.lvl1.intarr.size() as c3 " +
                "from Lvl0 as me";
            env.compileDeploy(soda, epl, path).addListener("s0");
            String[] fields = "c0,c1,c2,c3".split(",");
            env.assertStmtTypesAllSame("s0", fields, EPTypePremade.INTEGERBOXED.getEPType());

            Map<String, Object> lvl1 = CollectionUtil.buildMap("intarr", new Integer[]{1, 2, 3});
            Map<String, Object> lvl0 = CollectionUtil.buildMap("lvl1", lvl1, "indexNumber", 2);
            env.sendEventMap(lvl0, "Lvl0");
            env.assertPropsNew("s0", fields, new Object[]{3, 3, 3, 3});

            // Invalid tests
            // not an index expression
            env.tryInvalidCompile(path, "select lvl1[indexNumber] from Lvl0",
                "Failed to validate select-clause expression 'lvl1[indexNumber]': Invalid array operation for property 'lvl1'");
            env.tryInvalidCompile(path, "select me.lvl1[indexNumber] from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1[indexNumber]': Invalid array operation for property 'lvl1'");

            // two index expressions
            env.tryInvalidCompile(path, "select lvl1.intarr[indexNumber, indexNumber] from Lvl0",
                "Failed to validate select-clause expression 'lvl1.intarr[indexNumber,indexNumber]': Incorrect number of index expressions for array operation, expected a single expression returning an integer value but received 2 expressions for operation on type Integer[]");
            env.tryInvalidCompile(path, "select me.lvl1.intarr[indexNumber, indexNumber] from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1.intarr[indexNumber,indexNumber]': Incorrect number of index expressions for array operation, expected a single expression returning an integer value but received 2 expressions for operation on type Integer[]");

            // double-array
            env.tryInvalidCompile(path, "select lvl1.intarr[indexNumber][indexNumber] from Lvl0",
                "Failed to validate select-clause expression 'lvl1.intarr[indexNumber][indexNumber]': Could not perform array operation on type Integer");
            env.tryInvalidCompile(path, "select me.lvl1.intarr[indexNumber][indexNumber] from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1.intarr[indexNumber][indexNumber]': Could not perform array operation on type Integer");

            // wrong index expression type
            env.tryInvalidCompile(path, "select lvl1.intarr[id] from Lvl0",
                "Failed to validate select-clause expression 'lvl1.intarr[id]': Incorrect index expression for array operation, expected an expression returning an integer value but the expression 'id' returns 'String' for operation on type Integer[]");
            env.tryInvalidCompile(path, "select me.lvl1.intarr[id] from Lvl0 as me",
                "Failed to validate select-clause expression 'me.lvl1.intarr[id]': Incorrect index expression for array operation, expected an expression returning an integer value but the expression 'id' returns 'String' for operation on type Integer[]");

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    private static class ExprCoreAAEPropRootedTopLevelProp implements RegressionExecution {
        private final boolean soda;

        public ExprCoreAAEPropRootedTopLevelProp(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "intarr[indexNumber] as c0, " +
                "intarr.size() as c1, " +
                "me.intarr[indexNumber] as c2, " +
                "me.intarr.size() as c3 " +
                "from SupportBeanWithArray as me";
            env.compileDeploy(soda, epl).addListener("s0");
            String[] fields = "c0,c1,c2,c3".split(",");
            env.assertStmtTypesAllSame("s0", fields, EPTypePremade.INTEGERBOXED.getEPType());

            env.sendEventBean(new SupportBeanWithArray(1, new int[]{1, 2}));
            env.assertPropsNew("s0", fields, new Object[]{2, 2, 2, 2});

            // Invalid tests
            // two index expressions
            env.tryInvalidCompile("select intarr[indexNumber, indexNumber] from SupportBeanWithArray",
                "Failed to validate select-clause expression 'intarr[indexNumber,indexNumber]': Incorrect number of index expressions for array operation, expected a single expression returning an integer value but received 2 expressions for property 'intarr'");
            env.tryInvalidCompile("select me.intarr[indexNumber, indexNumber] from SupportBeanWithArray as me",
                "Failed to validate select-clause expression 'me.intarr[indexNumber,indexNumber]': Incorrect number of index expressions for array operation, expected a single expression returning an integer value but received 2 expressions for property 'intarr'");

            // double-array
            env.tryInvalidCompile("select intarr[indexNumber][indexNumber] from SupportBeanWithArray",
                "Failed to validate select-clause expression 'intarr[indexNumber][indexNumber]': Could not perform array operation on type Integer");
            env.tryInvalidCompile("select me.intarr[indexNumber][indexNumber] from SupportBeanWithArray as me",
                "Failed to validate select-clause expression 'me.intarr[indexNumber][indexNumber]': Could not perform array operation on type Integer");

            // wrong index expression type
            env.tryInvalidCompile("select intarr[id] from SupportBeanWithArray",
                "Failed to validate select-clause expression 'intarr[id]': Incorrect index expression for array operation, expected an expression returning an integer value but the expression 'id' returns 'String' for property 'intarr'");
            env.tryInvalidCompile("select me.intarr[id] from SupportBeanWithArray as me",
                "Failed to validate select-clause expression 'me.intarr[id]': Incorrect index expression for array operation, expected an expression returning an integer value but the expression 'id' returns 'String' for property 'intarr'");

            // not an array
            env.tryInvalidCompile("select indexNumber[indexNumber] from SupportBeanWithArray",
                "Failed to validate select-clause expression 'indexNumber[indexNumber]': Invalid array operation for property 'indexNumber'");
            env.tryInvalidCompile("select me.indexNumber[indexNumber] from SupportBeanWithArray as me",
                "Failed to validate select-clause expression 'me.indexNumber[indexNumber]': Invalid array operation for property 'indexNumber'");

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    public static int[] getIntArray() {
        return new int[] {1, 10};
    }

    public static int[][] getIntArray2Dim() {
        return new int[][] {{1, 10}, {2, 20}};
    }

    public static class MyHolder implements Serializable {
        private final String id;

        public MyHolder(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
