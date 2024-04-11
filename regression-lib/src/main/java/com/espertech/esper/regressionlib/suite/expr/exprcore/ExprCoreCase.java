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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.support.bean.SupportBeanWithEnum;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.espertech.esper.common.client.type.EPTypeClassParameterized.from;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExprCoreCase {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreCaseSyntax1Sum());
        executions.add(new ExprCoreCaseSyntax1SumOM());
        executions.add(new ExprCoreCaseSyntax1SumCompile());
        executions.add(new ExprCoreCaseSyntax1WithElse());
        executions.add(new ExprCoreCaseSyntax1WithElseOM());
        executions.add(new ExprCoreCaseSyntax1WithElseCompile());
        executions.add(new ExprCoreCaseSyntax1Branches3());
        executions.add(new ExprCoreCaseSyntax2());
        executions.add(new ExprCoreCaseSyntax2StringsNBranches());
        executions.add(new ExprCoreCaseSyntax2NoElseWithNull());
        executions.add(new ExprCoreCaseSyntax1WithNull());
        executions.add(new ExprCoreCaseSyntax2WithNullOM());
        executions.add(new ExprCoreCaseSyntax2WithNullCompile());
        executions.add(new ExprCoreCaseSyntax2WithNull());
        executions.add(new ExprCoreCaseSyntax2WithNullBool());
        executions.add(new ExprCoreCaseSyntax2WithCoercion());
        executions.add(new ExprCoreCaseSyntax2WithinExpression());
        executions.add(new ExprCoreCaseSyntax2Sum());
        executions.add(new ExprCoreCaseSyntax2EnumChecks());
        executions.add(new ExprCoreCaseSyntax2EnumResult());
        executions.add(new ExprCoreCaseSyntax2NoAsName());
        executions.add(new ExprCoreCaseWithArrayResult());
        executions.add(new ExprCoreCaseWithTypeParameterizedProperty());
        executions.add(new ExprCoreCaseWithTypeParameterizedPropertyInvalid());
        return executions;
    }

    private static class ExprCoreCaseWithTypeParameterizedProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "List<Integer>", "List<Integer>", from(List.class, Integer.class));
            runAssertion(env, "List<List<Integer>>", "List<List<Integer>>", from(List.class, from(List.class, Integer.class)));
            runAssertion(env, "List<int[primitive]>", "List<int[primitive]>", from(List.class, int[].class));

            runAssertion(env, "Integer[]", "String[]", new EPTypeClass(Object[].class));

            runAssertion(env, "List<Integer>", "List<Object>", new EPTypeClass(List.class));
            runAssertion(env, "Collection<Integer>", "List<Object>", EPTypePremade.OBJECT.getEPType());

            runAssertion(env, "Collection<Integer>", "null", from(Collection.class, Integer.class));
        }

        private void runAssertion(RegressionEnvironment env, String typeOne, String typeTwo, EPType expected) {
            String eplSyntaxOne = getEPLSyntaxOne(typeOne, typeTwo);
            runAssertionEPL(env, eplSyntaxOne, expected);

            String eplSyntaxTwo = getEPLSyntaxTwo(typeOne, typeTwo);
            runAssertionEPL(env, eplSyntaxTwo, expected);
        }

        private void runAssertionEPL(RegressionEnvironment env, String epl, EPType expected) {
            env.compileDeploy(epl);
            env.assertStatement("s0", statement -> assertEquals(expected, statement.getEventType().getPropertyEPType("thecase")));
            env.undeployAll();
        }

        private String getEPLSyntaxOne(String typeOne, String typeTwo) {
            return "create schema MyEvent(switch boolean, fieldOne " + typeOne + ", fieldTwo " + typeTwo + ");\n" +
                "@name('s0') select case when switch then fieldOne else fieldTwo end as thecase from MyEvent;\n";
        }

        private String getEPLSyntaxTwo(String typeOne, String typeTwo) {
            return "create schema MyEvent(switch boolean, fieldOne " + typeOne + ", fieldTwo " + typeTwo + ");\n" +
                "@name('s0') select case switch when true then fieldOne when false then fieldTwo end as thecase from MyEvent;\n";
        }
    }

    private static class ExprCoreCaseWithTypeParameterizedPropertyInvalid implements RegressionExecution {
        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }

        public void run(RegressionEnvironment env) {
            runInvalid(env, "int[primitive]", "String[]", "Cannot coerce to int[] type String[]");
            runInvalid(env, "long[primitive]", "long[]", "Cannot coerce to long[] type Long[]");
            runInvalid(env, "null", "null", "Null-type return value is not allowed");
        }

        private void runInvalid(RegressionEnvironment env, String typeOne, String typeTwo, String detail) {
            String eplSyntaxOne = getEPLSyntaxOne(typeOne, typeTwo);
            runInvalidEPL(env, eplSyntaxOne, detail);

            String eplSyntaxTwo = getEPLSyntaxTwo(typeOne, typeTwo);
            runInvalidEPL(env, eplSyntaxTwo, detail);
        }

        private void runInvalidEPL(RegressionEnvironment env, String epl, String detail) {
            try {
                env.compileWCheckedEx(epl);
                fail();
            } catch (EPCompileException ex) {
                if (!ex.getMessage().contains(detail)) {
                    assertEquals(detail, ex.getMessage());
                }
            }
        }

        private String getEPLSyntaxOne(String typeOne, String typeTwo) {
            return "create schema MyEvent(switch boolean, fieldOne " + typeOne + ", fieldTwo " + typeTwo + ");\n" +
                "@name('s0') select case when switch then fieldOne else fieldTwo end as thecase from MyEvent;\n";
        }

        private String getEPLSyntaxTwo(String typeOne, String typeTwo) {
            return "create schema MyEvent(switch boolean, fieldOne " + typeOne + ", fieldTwo " + typeTwo + ");\n" +
                "@name('s0') select case switch when true then fieldOne when false then fieldTwo end as thecase from MyEvent;\n";
        }
    }

    private static class ExprCoreCaseWithArrayResult implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case when intPrimitive = 1 then { 1, 2 } else { 1, 2 } end as c1 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertEventNew("s0", event -> EPAssertionUtil.assertEqualsExactOrder((Integer[]) event.get("c1"), new Integer[]{1, 2}));

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax1Sum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Testing the two forms of the case expression
            // Furthermore the test checks the different when clauses and actions related.
            String epl = "@name('s0') select case " +
                " when symbol='GE' then volume " +
                " when symbol='DELL' then sum(price) " +
                "end as p1 from SupportMarketDataBean#length(10)";

            env.compileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", "p1", EPTypePremade.DOUBLEBOXED.getEPType());

            runCaseSyntax1Sum(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax1SumOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create().add(Expressions.caseWhenThen()
                .add(Expressions.eq("symbol", "GE"), Expressions.property("volume"))
                .add(Expressions.eq("symbol", "DELL"), Expressions.sum("price")), "p1"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getSimpleName()).addView("win", "length", Expressions.constant(10))));
            model = SerializableObjectCopier.copyMayFail(model);

            String epl = "select case" +
                " when symbol=\"GE\" then volume" +
                " when symbol=\"DELL\" then sum(price) " +
                "end as p1 from SupportMarketDataBean.win:length(10)";

            assertEquals(epl, model.toEPL());
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0").milestone(0);

            env.assertStmtType("s0", "p1", EPTypePremade.DOUBLEBOXED.getEPType());

            runCaseSyntax1Sum(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax1SumCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case" +
                " when symbol=\"GE\" then volume" +
                " when symbol=\"DELL\" then sum(price) " +
                "end as p1 from SupportMarketDataBean#length(10)";
            env.eplToModelCompileDeploy(epl).addListener("s0").milestone(0);

            env.assertStmtType("s0", "p1", EPTypePremade.DOUBLEBOXED.getEPType());

            runCaseSyntax1Sum(env);

            env.undeployAll();
        }
    }

    private static void runCaseSyntax1Sum(RegressionEnvironment env) {
        sendMarketDataEvent(env, "DELL", 10000, 50);
        env.assertEqualsNew("s0", "p1", 50.0);

        sendMarketDataEvent(env, "DELL", 10000, 50);
        env.assertEqualsNew("s0", "p1", 100.0);

        sendMarketDataEvent(env, "CSCO", 4000, 5);
        env.assertEqualsNew("s0", "p1", null);

        sendMarketDataEvent(env, "GE", 20, 30);
        env.assertEqualsNew("s0", "p1", 20.0);
    }

    private static class ExprCoreCaseSyntax1WithElse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Adding to the EPL statement an else expression
            // when a CSCO ticker is sent the property for the else expression is selected
            String epl = "@name('s0') select case " +
                " when symbol='DELL' then 3 * volume " +
                " else volume " +
                "end as p1 from SupportMarketDataBean#length(3)";

            env.compileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", "p1", EPTypePremade.LONGBOXED.getEPType());

            runCaseSyntax1WithElse(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax1WithElseOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create().add(Expressions.caseWhenThen()
                .setElse(Expressions.property("volume"))
                .add(Expressions.eq("symbol", "DELL"), Expressions.multiply(Expressions.property("volume"), Expressions.constant(3))), "p1"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getSimpleName()).addView("length", Expressions.constant(10))));
            model = SerializableObjectCopier.copyMayFail(model);

            String epl = "select case " +
                "when symbol=\"DELL\" then volume*3 " +
                "else volume " +
                "end as p1 from SupportMarketDataBean#length(10)";
            assertEquals(epl, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0").milestone(0);

            env.assertStmtType("s0", "p1", EPTypePremade.LONGBOXED.getEPType());

            runCaseSyntax1WithElse(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax1WithElseCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case " +
                "when symbol=\"DELL\" then volume*3 " +
                "else volume " +
                "end as p1 from SupportMarketDataBean#length(10)";
            env.eplToModelCompileDeploy(epl).addListener("s0");

            env.assertStmtType("s0", "p1", EPTypePremade.LONGBOXED.getEPType());

            runCaseSyntax1WithElse(env);

            env.undeployAll();
        }
    }

    private static void runCaseSyntax1WithElse(RegressionEnvironment env) {
        sendMarketDataEvent(env, "CSCO", 4000, 0);
        env.assertEqualsNew("s0", "p1", 4000L);

        sendMarketDataEvent(env, "DELL", 20, 0);
        env.assertEqualsNew("s0", "p1", 3 * 20L);
    }

    private static class ExprCoreCaseSyntax1Branches3 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportMarketDataBean")
                .expressions(fields, "case when (symbol='GE') then volume " +
                    " when (symbol='DELL') then volume / 2.0 " +
                    " when (symbol='MSFT') then volume / 3.0 " +
                    " end")
                .statementConsumer(stmt -> assertEquals(Double.class, stmt.getEventType().getPropertyType("c0")));

            builder.assertion(makeMarketDataEvent("DELL", 10000, 0)).expect(fields, 10000 / 2.0);
            builder.assertion(makeMarketDataEvent("MSFT", 10000, 0)).expect(fields, 10000 / 3.0);
            builder.assertion(makeMarketDataEvent("GE", 10000, 0)).expect(fields, 10000.0);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expressions(fields, "case intPrimitive " +
                    " when longPrimitive then (intPrimitive + longPrimitive) " +
                    " when doublePrimitive then intPrimitive * doublePrimitive" +
                    " when floatPrimitive then floatPrimitive / doublePrimitive " +
                    " else (intPrimitive + longPrimitive + floatPrimitive + doublePrimitive) end")
                .statementConsumer(stmt -> assertEquals(Double.class, stmt.getEventType().getPropertyType("c0")));

            // intPrimitive = longPrimitive
            // case result is intPrimitive + longPrimitive
            builder.assertion(makeSupportBeanEvent(2, 2L, 1.0f, 1.0)).expect(fields, 4.0);

            // intPrimitive = doublePrimitive
            // case result is intPrimitive * doublePrimitive
            builder.assertion(makeSupportBeanEvent(5, 1L, 1.0f, 5.0)).expect(fields, 25.0);

            // intPrimitive = floatPrimitive
            // case result is floatPrimitive / doublePrimitive
            builder.assertion(makeSupportBeanEvent(12, 1L, 12.0f, 4.0)).expect(fields, 3.0);

            // all the properties of the event are different
            // The else part is computed: 1+2+3+4 = 10
            builder.assertion(makeSupportBeanEvent(1, 2L, 3.0f, 4.0)).expect(fields, 10.0);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2StringsNBranches implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Test of the various coercion user cases.
            String epl = "@name('s0') select case intPrimitive" +
                " when 1 then Boolean.toString(boolPrimitive) " +
                " when 2 then Boolean.toString(boolBoxed) " +
                " when 3 then Integer.toString(intPrimitive) " +
                " when 4 then Integer.toString(intBoxed)" +
                " when 5 then Long.toString(longPrimitive) " +
                " when 6 then Long.toString(longBoxed) " +
                " when 7 then Character.toString(charPrimitive) " +
                " when 8 then Character.toString(charBoxed) " +
                " when 9 then Short.toString(shortPrimitive) " +
                " when 10 then Short.toString(shortBoxed) " +
                " when 11 then Byte.toString(bytePrimitive) " +
                " when 12 then Byte.toString(byteBoxed) " +
                " when 13 then Float.toString(floatPrimitive) " +
                " when 14 then Float.toString(floatBoxed) " +
                " when 15 then Double.toString(doublePrimitive) " +
                " when 16 then Double.toString(doubleBoxed) " +
                " when 17 then theString " +
                " else 'x' end as p1 " +
                " from SupportBean#length(1)";

            env.compileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", "p1", EPTypePremade.STRING.getEPType());

            sendSupportBeanEvent(env, true, false, 1, 0, 0L, 0L, '0', 'a', (short) 0, (short) 0, (byte) 0, (byte) 0, 0.0f, (float) 0, 0.0, 0.0, null, SupportEnum.ENUM_VALUE_1);
            assertP1(env, "true");

            sendSupportBeanEvent(env, true, false, 2, 0, 0L, 0L, '0', 'a', (short) 0, (short) 0, (byte) 0, (byte) 0, 0.0f, (float) 0, 0.0, 0.0, null, SupportEnum.ENUM_VALUE_1);
            assertP1(env, "false");

            sendSupportBeanEvent(env, true, false, 3, 0, 0L, 0L, '0', 'a', (short) 0, (short) 0, (byte) 0, (byte) 0, 0.0f, (float) 0, 0.0, 0.0, null, SupportEnum.ENUM_VALUE_1);
            assertP1(env, "3");

            sendSupportBeanEvent(env, true, false, 4, 4, 0L, 0L, '0', 'a', (short) 0, (short) 0, (byte) 0, (byte) 0, 0.0f, (float) 0, 0.0, 0.0, null, SupportEnum.ENUM_VALUE_1);
            assertP1(env, "4");

            sendSupportBeanEvent(env, true, false, 5, 0, 5L, 0L, '0', 'a', (short) 0, (short) 0, (byte) 0, (byte) 0, 0.0f, (float) 0, 0.0, 0.0, null, SupportEnum.ENUM_VALUE_1);
            assertP1(env, "5");

            sendSupportBeanEvent(env, true, false, 6, 0, 0L, 6L, '0', 'a', (short) 0, (short) 0, (byte) 0, (byte) 0, 0.0f, (float) 0, 0.0, 0.0, null, SupportEnum.ENUM_VALUE_1);
            assertP1(env, "6");

            sendSupportBeanEvent(env, true, false, 7, 0, 0L, 0L, 'A', 'a', (short) 0, (short) 0, (byte) 0, (byte) 0, 0.0f, (float) 0, 0.0, 0.0, null, SupportEnum.ENUM_VALUE_1);
            assertP1(env, "A");

            sendSupportBeanEvent(env, true, false, 8, 0, 0L, 0L, 'A', 'a', (short) 0, (short) 0, (byte) 0, (byte) 0, 0.0f, (float) 0, 0.0, 0.0, null, SupportEnum.ENUM_VALUE_1);
            assertP1(env, "a");

            sendSupportBeanEvent(env, true, false, 9, 0, 0L, 0L, 'A', 'a', (short) 9, (short) 0, (byte) 0, (byte) 0, 0.0f, (float) 0, 0.0, 0.0, null, SupportEnum.ENUM_VALUE_1);
            assertP1(env, "9");

            sendSupportBeanEvent(env, true, false, 10, 0, 0L, 0L, 'A', 'a', (short) 9, (short) 10, (byte) 11, (byte) 12, 13.0f, (float) 14, 15.0, 16.0, "testCoercion", SupportEnum.ENUM_VALUE_1);
            assertP1(env, "10");

            sendSupportBeanEvent(env, true, false, 11, 0, 0L, 0L, 'A', 'a', (short) 9, (short) 10, (byte) 11, (byte) 12, 13.0f, (float) 14, 15.0, 16.0, "testCoercion", SupportEnum.ENUM_VALUE_1);
            assertP1(env, "11");

            sendSupportBeanEvent(env, true, false, 12, 0, 0L, 0L, 'A', 'a', (short) 9, (short) 10, (byte) 11, (byte) 12, 13.0f, (float) 14, 15.0, 16.0, "testCoercion", SupportEnum.ENUM_VALUE_1);
            assertP1(env, "12");

            sendSupportBeanEvent(env, true, false, 13, 0, 0L, 0L, 'A', 'a', (short) 9, (short) 10, (byte) 11, (byte) 12, 13.0f, (float) 14, 15.0, 16.0, "testCoercion", SupportEnum.ENUM_VALUE_1);
            assertP1(env, "13.0");

            sendSupportBeanEvent(env, true, false, 14, 0, 0L, 0L, 'A', 'a', (short) 9, (short) 10, (byte) 11, (byte) 12, 13.0f, (float) 14, 15.0, 16.0, "testCoercion", SupportEnum.ENUM_VALUE_1);
            assertP1(env, "14.0");

            sendSupportBeanEvent(env, true, false, 15, 0, 0L, 0L, 'A', 'a', (short) 9, (short) 10, (byte) 11, (byte) 12, 13.0f, (float) 14, 15.0, 16.0, "testCoercion", SupportEnum.ENUM_VALUE_1);
            assertP1(env, "15.0");

            sendSupportBeanEvent(env, true, false, 16, 0, 0L, 0L, 'A', 'a', (short) 9, (short) 10, (byte) 11, (byte) 12, 13.0f, (float) 14, 15.0, 16.0, "testCoercion", SupportEnum.ENUM_VALUE_1);
            assertP1(env, "16.0");

            sendSupportBeanEvent(env, true, false, 17, 0, 0L, 0L, 'A', 'a', (short) 9, (short) 10, (byte) 11, (byte) 12, 13.0f, (float) 14, 15.0, 16.0, "testCoercion", SupportEnum.ENUM_VALUE_1);
            assertP1(env, "testCoercion");

            sendSupportBeanEvent(env, true, false, -1, 0, 0L, 0L, 'A', 'a', (short) 9, (short) 10, (byte) 11, (byte) 12, 13.0f, (float) 14, 15.0, 16.0, "testCoercion", SupportEnum.ENUM_VALUE_1);
            assertP1(env, "x");

            env.undeployAll();
        }

        private void assertP1(RegressionEnvironment env, String expected) {
            env.assertListener("s0", listener -> {
                EventBean theEvent = listener.getAndResetLastNewData()[0];
                assertEquals(expected, theEvent.get("p1"));
            });
        }
    }

    private static class ExprCoreCaseSyntax2NoElseWithNull implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case theString " +
                " when null then true " +
                " when '' then false end as p1" +
                " from SupportBean#length(100)";

            env.compileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", "p1", EPTypePremade.BOOLEANBOXED.getEPType());

            sendSupportBeanEvent(env, "x");
            env.assertEqualsNew("s0", "p1", null);

            sendSupportBeanEvent(env, "null");
            env.assertEqualsNew("s0", "p1", null);

            sendSupportBeanEvent(env, null);
            env.assertEqualsNew("s0", "p1", true);

            sendSupportBeanEvent(env, "");
            env.assertEqualsNew("s0", "p1", false);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax1WithNull implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case " +
                " when theString is null then true " +
                " when theString = '' then false end as p1" +
                " from SupportBean#length(100)";

            env.compileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", "p1", EPTypePremade.BOOLEANBOXED.getEPType());

            sendSupportBeanEvent(env, "x");
            env.assertEqualsNew("s0", "p1", null);

            sendSupportBeanEvent(env, "null");
            env.assertEqualsNew("s0", "p1", null);

            sendSupportBeanEvent(env, null);
            env.assertEqualsNew("s0", "p1", true);

            sendSupportBeanEvent(env, "");
            env.assertEqualsNew("s0", "p1", false);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2WithNullOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "select case intPrimitive " +
                "when 1 then null " +
                "when 2 then 1.0d " +
                "when 3 then null " +
                "else 2 " +
                "end as p1 from SupportBean#length(100)";

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create().add(Expressions.caseSwitch("intPrimitive")
                .setElse(Expressions.constant(2))
                .add(Expressions.constant(1), Expressions.constant(null))
                .add(Expressions.constant(2), Expressions.constant(1.0))
                .add(Expressions.constant(3), Expressions.constant(null)), "p1"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getSimpleName()).addView("length", Expressions.constant(100))));
            model = SerializableObjectCopier.copyMayFail(model);

            assertEquals(epl, model.toEPL());
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0").milestone(0);
            env.assertStmtType("s0", "p1", EPTypePremade.DOUBLEBOXED.getEPType());

            runCaseSyntax2WithNull(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2WithNullCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case intPrimitive " +
                "when 1 then null " +
                "when 2 then 1.0d " +
                "when 3 then null " +
                "else 2 " +
                "end as p1 from SupportBean#length(100)";

            env.eplToModelCompileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", "p1", EPTypePremade.DOUBLEBOXED.getEPType());

            runCaseSyntax2WithNull(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2WithNull implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case intPrimitive " +
                " when 1 then null " +
                " when 2 then 1.0" +
                " when 3 then null " +
                " else 2 " +
                " end as p1 from SupportBean#length(100)";

            env.compileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", "p1", EPTypePremade.DOUBLEBOXED.getEPType());

            runCaseSyntax2WithNull(env);

            env.undeployAll();
        }
    }

    private static void runCaseSyntax2WithNull(RegressionEnvironment env) {
        sendSupportBeanEvent(env, 4);
        env.assertEqualsNew("s0", "p1", 2.0);
        sendSupportBeanEvent(env, 1);
        env.assertEqualsNew("s0", "p1", null);
        sendSupportBeanEvent(env, 2);
        env.assertEqualsNew("s0", "p1", 1.0);
        sendSupportBeanEvent(env, 3);
        env.assertEqualsNew("s0", "p1", null);
    }

    private static class ExprCoreCaseSyntax2WithNullBool implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case boolBoxed " +
                " when null then 1 " +
                " when true then 2l" +
                " when false then 3 " +
                " end as p1 from SupportBean#length(100)";

            env.compileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", "p1", EPTypePremade.LONGBOXED.getEPType());

            sendSupportBeanEvent(env, null);
            env.assertEqualsNew("s0", "p1", 1L);
            sendSupportBeanEvent(env, false);
            env.assertEqualsNew("s0", "p1", 3L);
            sendSupportBeanEvent(env, true);
            env.assertEqualsNew("s0", "p1", 2L);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2WithCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case intPrimitive " +
                " when 1.0 then null " +
                " when 4/2.0 then 'x'" +
                " end as p1 from SupportBean#length(100)";

            env.compileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", "p1", EPTypePremade.STRING.getEPType());

            sendSupportBeanEvent(env, 1);
            env.assertEqualsNew("s0", "p1", null);

            sendSupportBeanEvent(env, 2);
            env.assertEqualsNew("s0", "p1", "x");
            sendSupportBeanEvent(env, 3);
            env.assertEqualsNew("s0", "p1", null);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2WithinExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select 2 * (case " +
                " intPrimitive when 1 then 2 " +
                " when 2 then 3 " +
                " else 10 end) as p1 " +
                " from SupportBean#length(1)";

            env.compileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", "p1", EPTypePremade.INTEGERBOXED.getEPType());

            sendSupportBeanEvent(env, 1);
            env.assertEqualsNew("s0", "p1", 4);

            sendSupportBeanEvent(env, 2);
            env.assertEqualsNew("s0", "p1", 6);

            sendSupportBeanEvent(env, 3);
            env.assertEqualsNew("s0", "p1", 20);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2Sum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case intPrimitive when 1 then sum(longPrimitive) " +
                " when 2 then sum(floatPrimitive) " +
                " else sum(intPrimitive) end as p1 " +
                " from SupportBean#length(10)";

            env.compileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", "p1", EPTypePremade.DOUBLEBOXED.getEPType());

            sendSupportBeanEvent(env, 1, 10L, 3.0f, 4.0);
            env.assertEqualsNew("s0", "p1", 10d);

            sendSupportBeanEvent(env, 1, 15L, 3.0f, 4.0);
            env.assertEqualsNew("s0", "p1", 25d);

            sendSupportBeanEvent(env, 2, 1L, 3.0f, 4.0);
            env.assertEqualsNew("s0", "p1", 9d);

            sendSupportBeanEvent(env, 2, 1L, 3.0f, 4.0);
            env.assertEqualsNew("s0", "p1", 12d);

            sendSupportBeanEvent(env, 5, 1L, 1.0f, 1.0);
            env.assertEqualsNew("s0", "p1", 11d);

            sendSupportBeanEvent(env, 5, 1L, 1.0f, 1.0);
            env.assertEqualsNew("s0", "p1", 16d);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2EnumChecks implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case supportEnum " +
                " when " + SupportEnum.class.getSimpleName() + ".getValueForEnum(0) then 1 " +
                " when " + SupportEnum.class.getSimpleName() + ".getValueForEnum(1) then 2 " +
                " end as p1 " +
                " from " + SupportBeanWithEnum.class.getSimpleName() + "#length(10)";

            env.compileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", "p1", EPTypePremade.INTEGERBOXED.getEPType());

            sendSupportBeanEvent(env, "a", SupportEnum.ENUM_VALUE_1);
            env.assertEqualsNew("s0", "p1", 1);

            sendSupportBeanEvent(env, "b", SupportEnum.ENUM_VALUE_2);
            env.assertEqualsNew("s0", "p1", 2);

            sendSupportBeanEvent(env, "c", SupportEnum.ENUM_VALUE_3);
            env.assertEqualsNew("s0", "p1", null);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2EnumResult implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case intPrimitive * 2 " +
                " when 2 then " + SupportEnum.class.getSimpleName() + ".getValueForEnum(0) " +
                " when 4 then " + SupportEnum.class.getSimpleName() + ".getValueForEnum(1) " +
                " else " + SupportEnum.class.getSimpleName() + ".getValueForEnum(2) " +
                " end as p1 " +
                " from SupportBean#length(10)";

            env.compileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", "p1", EPTypePremade.getOrCreate(SupportEnum.class));

            sendSupportBeanEvent(env, 1);
            env.assertEqualsNew("s0", "p1", SupportEnum.ENUM_VALUE_1);

            sendSupportBeanEvent(env, 2);
            env.assertEqualsNew("s0", "p1", SupportEnum.ENUM_VALUE_2);

            sendSupportBeanEvent(env, 3);
            env.assertEqualsNew("s0", "p1", SupportEnum.ENUM_VALUE_3);

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2NoAsName implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String caseSubExpr = "case intPrimitive when 1 then 0 end";
            String epl = "@name('s0') select " + caseSubExpr +
                " from SupportBean#length(10)";

            env.compileDeploy(epl).addListener("s0");
            env.assertStmtType("s0", caseSubExpr, EPTypePremade.INTEGERBOXED.getEPType());

            sendSupportBeanEvent(env, 1);
            env.assertEqualsNew("s0", caseSubExpr, 0);

            env.undeployAll();
        }
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, boolean b, Boolean boolBoxed, int i, Integer intBoxed, long l, Long longBoxed,
                                             char c, Character charBoxed, short s, Short shortBoxed, byte by, Byte byteBoxed,
                                             float f, Float floatBoxed, double d, Double doubleBoxed, String str, SupportEnum enumval) {
        SupportBean theEvent = new SupportBean();
        theEvent.setBoolPrimitive(b);
        theEvent.setBoolBoxed(boolBoxed);
        theEvent.setIntPrimitive(i);
        theEvent.setIntBoxed(intBoxed);
        theEvent.setLongPrimitive(l);
        theEvent.setLongBoxed(longBoxed);
        theEvent.setCharPrimitive(c);
        theEvent.setCharBoxed(charBoxed);
        theEvent.setShortPrimitive(s);
        theEvent.setShortBoxed(shortBoxed);
        theEvent.setBytePrimitive(by);
        theEvent.setByteBoxed(byteBoxed);
        theEvent.setFloatPrimitive(f);
        theEvent.setFloatBoxed(floatBoxed);
        theEvent.setDoublePrimitive(d);
        theEvent.setDoubleBoxed(doubleBoxed);
        theEvent.setTheString(str);
        theEvent.setEnumValue(enumval);
        env.sendEventBean(theEvent);
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, int intPrimitive, long longPrimitive, float floatPrimitive, double doublePrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        theEvent.setLongPrimitive(longPrimitive);
        theEvent.setFloatPrimitive(floatPrimitive);
        theEvent.setDoublePrimitive(doublePrimitive);
        env.sendEventBean(theEvent);
    }

    private static SupportBean makeSupportBeanEvent(int intPrimitive, long longPrimitive, float floatPrimitive, double doublePrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        theEvent.setLongPrimitive(longPrimitive);
        theEvent.setFloatPrimitive(floatPrimitive);
        theEvent.setDoublePrimitive(doublePrimitive);
        return theEvent;
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, int intPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        env.sendEventBean(theEvent);
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, String theString) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(theString);
        env.sendEventBean(theEvent);
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, boolean boolBoxed) {
        SupportBean theEvent = new SupportBean();
        theEvent.setBoolBoxed(boolBoxed);
        env.sendEventBean(theEvent);
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, String theString, SupportEnum supportEnum) {
        SupportBeanWithEnum theEvent = new SupportBeanWithEnum(theString, supportEnum);
        env.sendEventBean(theEvent);
    }

    private static void sendMarketDataEvent(RegressionEnvironment env, String symbol, long volume, double price) {
        SupportMarketDataBean bean = makeMarketDataEvent(symbol, volume, price);
        env.sendEventBean(bean);
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol, long volume, double price) {
        return new SupportMarketDataBean(symbol, price, volume, null);
    }

    private static final Logger log = LoggerFactory.getLogger(ExprCoreCase.class);
}
