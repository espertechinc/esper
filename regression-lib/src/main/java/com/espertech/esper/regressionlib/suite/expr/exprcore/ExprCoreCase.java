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
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanWithEnum;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

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
        return executions;
    }

    private static class ExprCoreCaseWithArrayResult implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case when intPrimitive = 1 then { 1, 2 } else { 1, 2 } end as c1 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertEqualsExactOrder((Integer[]) env.listener("s0").assertOneGetNewAndReset().get("c1"), new Integer[]{1, 2});

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
            assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("p1"));

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

            assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("p1"));

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

            assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("p1"));

            runCaseSyntax1Sum(env);

            env.undeployAll();
        }
    }

    private static void runCaseSyntax1Sum(RegressionEnvironment env) {
        sendMarketDataEvent(env, "DELL", 10000, 50);
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(50.0, theEvent.get("p1"));

        sendMarketDataEvent(env, "DELL", 10000, 50);
        theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(100.0, theEvent.get("p1"));

        sendMarketDataEvent(env, "CSCO", 4000, 5);
        theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(null, theEvent.get("p1"));

        sendMarketDataEvent(env, "GE", 20, 30);
        theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(20.0, theEvent.get("p1"));
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
            assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("p1"));

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

            assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("p1"));

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

            assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("p1"));

            runCaseSyntax1WithElse(env);

            env.undeployAll();
        }
    }

    private static void runCaseSyntax1WithElse(RegressionEnvironment env) {
        sendMarketDataEvent(env, "CSCO", 4000, 0);
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(4000L, theEvent.get("p1"));

        sendMarketDataEvent(env, "DELL", 20, 0);
        theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(3 * 20L, theEvent.get("p1"));
    }

    private static class ExprCoreCaseSyntax1Branches3 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Same test but the where clause doesn't match any of the condition of the case expresssion
            String epl = "@name('s0') select case " +
                " when (symbol='GE') then volume " +
                " when (symbol='DELL') then volume / 2.0 " +
                " when (symbol='MSFT') then volume / 3.0 " +
                " end as p1 from " + SupportMarketDataBean.class.getSimpleName();
            env.compileDeploy(epl).addListener("s0");
            assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("p1"));

            sendMarketDataEvent(env, "DELL", 10000, 0);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(10000 / 2.0, theEvent.get("p1"));

            sendMarketDataEvent(env, "MSFT", 10000, 0);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(10000 / 3.0, theEvent.get("p1"));

            sendMarketDataEvent(env, "GE", 10000, 0);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(10000.0, theEvent.get("p1"));

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case intPrimitive " +
                " when longPrimitive then (intPrimitive + longPrimitive) " +
                " when doublePrimitive then intPrimitive * doublePrimitive" +
                " when floatPrimitive then floatPrimitive / doublePrimitive " +
                " else (intPrimitive + longPrimitive + floatPrimitive + doublePrimitive) end as p1 " +
                " from SupportBean#length(10)";

            env.compileDeploy(epl).addListener("s0");

            assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("p1"));

            // intPrimitive = longPrimitive
            // case result is intPrimitive + longPrimitive
            sendSupportBeanEvent(env, 2, 2L, 1.0f, 1.0);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(4.0, theEvent.get("p1"));
            // intPrimitive = doublePrimitive
            // case result is intPrimitive * doublePrimitive
            sendSupportBeanEvent(env, 5, 1L, 1.0f, 5.0);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(25.0, theEvent.get("p1"));
            // intPrimitive = floatPrimitive
            // case result is floatPrimitive / doublePrimitive
            sendSupportBeanEvent(env, 12, 1L, 12.0f, 4.0);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(3.0, theEvent.get("p1"));
            // all the properties of the event are different
            // The else part is computed: 1+2+3+4 = 10
            sendSupportBeanEvent(env, 1, 2L, 3.0f, 4.0);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(10.0, theEvent.get("p1"));

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

            assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 1, new Integer(0), 0L, new Long(0L), '0', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
            EventBean theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("true", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 2, new Integer(0), 0L, new Long(0L), '0', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("false", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 3, new Integer(0), 0L, new Long(0L), '0', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("3", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 4, new Integer(4), 0L, new Long(0L), '0', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("4", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 5, new Integer(0), 5L, new Long(0L), '0', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("5", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 6, new Integer(0), 0L, new Long(6L), '0', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("6", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 7, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("A", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 8, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 0, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("a", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 9, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 0), (byte) 0, new Byte((byte) 0), 0.0f, new Float((float) 0), 0.0, new Double(0.0), null, SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("9", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 10, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("10", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 11, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("11", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 12, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("12", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 13, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("13.0", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 14, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("14.0", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 15, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("15.0", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 16, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("16.0", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), 17, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("testCoercion", theEvent.get("p1"));

            sendSupportBeanEvent(env, true, new Boolean(false), -1, new Integer(0), 0L, new Long(0L), 'A', new Character('a'), (short) 9, new Short((short) 10), (byte) 11, new Byte((byte) 12), 13.0f, new Float((float) 14), 15.0, new Double(16.0), "testCoercion", SupportEnum.ENUM_VALUE_1);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals("x", theEvent.get("p1"));

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2NoElseWithNull implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case theString " +
                " when null then true " +
                " when '' then false end as p1" +
                " from SupportBean#length(100)";

            env.compileDeploy(epl).addListener("s0");
            assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("p1"));

            sendSupportBeanEvent(env, "x");
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("p1"));

            sendSupportBeanEvent(env, "null");
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("p1"));

            sendSupportBeanEvent(env, null);
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("p1"));

            sendSupportBeanEvent(env, "");
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("p1"));

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
            assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("p1"));

            sendSupportBeanEvent(env, "x");
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("p1"));

            sendSupportBeanEvent(env, "null");
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("p1"));

            sendSupportBeanEvent(env, null);
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("p1"));

            sendSupportBeanEvent(env, "");
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("p1"));

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
            assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("p1"));

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
            assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("p1"));

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
            assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("p1"));

            runCaseSyntax2WithNull(env);

            env.undeployAll();
        }
    }

    private static void runCaseSyntax2WithNull(RegressionEnvironment env) {
        sendSupportBeanEvent(env, 4);
        assertEquals(2.0, env.listener("s0").assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(env, 1);
        assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(env, 2);
        assertEquals(1.0, env.listener("s0").assertOneGetNewAndReset().get("p1"));
        sendSupportBeanEvent(env, 3);
        assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("p1"));
    }

    private static class ExprCoreCaseSyntax2WithNullBool implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select case boolBoxed " +
                " when null then 1 " +
                " when true then 2l" +
                " when false then 3 " +
                " end as p1 from SupportBean#length(100)";

            env.compileDeploy(epl).addListener("s0");
            assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("p1"));

            sendSupportBeanEvent(env, null);
            assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("p1"));
            sendSupportBeanEvent(env, false);
            assertEquals(3L, env.listener("s0").assertOneGetNewAndReset().get("p1"));
            sendSupportBeanEvent(env, true);
            assertEquals(2L, env.listener("s0").assertOneGetNewAndReset().get("p1"));

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
            assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("p1"));

            sendSupportBeanEvent(env, 1);
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("p1"));
            sendSupportBeanEvent(env, 2);
            assertEquals("x", env.listener("s0").assertOneGetNewAndReset().get("p1"));
            sendSupportBeanEvent(env, 3);
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("p1"));

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
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("p1"));

            sendSupportBeanEvent(env, 1);
            EventBean theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(4, theEvent.get("p1"));

            sendSupportBeanEvent(env, 2);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(6, theEvent.get("p1"));

            sendSupportBeanEvent(env, 3);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(20, theEvent.get("p1"));

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
            assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("p1"));

            sendSupportBeanEvent(env, 1, 10L, 3.0f, 4.0);
            EventBean theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(10d, theEvent.get("p1"));

            sendSupportBeanEvent(env, 1, 15L, 3.0f, 4.0);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(25d, theEvent.get("p1"));

            sendSupportBeanEvent(env, 2, 1L, 3.0f, 4.0);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(9d, theEvent.get("p1"));

            sendSupportBeanEvent(env, 2, 1L, 3.0f, 4.0);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(12.0d, theEvent.get("p1"));

            sendSupportBeanEvent(env, 5, 1L, 1.0f, 1.0);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(11.0d, theEvent.get("p1"));

            sendSupportBeanEvent(env, 5, 1L, 1.0f, 1.0);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(16d, theEvent.get("p1"));

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
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("p1"));

            sendSupportBeanEvent(env, "a", SupportEnum.ENUM_VALUE_1);
            EventBean theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(1, theEvent.get("p1"));

            sendSupportBeanEvent(env, "b", SupportEnum.ENUM_VALUE_2);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(2, theEvent.get("p1"));

            sendSupportBeanEvent(env, "c", SupportEnum.ENUM_VALUE_3);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(null, theEvent.get("p1"));

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
            assertEquals(SupportEnum.class, env.statement("s0").getEventType().getPropertyType("p1"));

            sendSupportBeanEvent(env, 1);
            EventBean theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(SupportEnum.ENUM_VALUE_1, theEvent.get("p1"));

            sendSupportBeanEvent(env, 2);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(SupportEnum.ENUM_VALUE_2, theEvent.get("p1"));

            sendSupportBeanEvent(env, 3);
            theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(SupportEnum.ENUM_VALUE_3, theEvent.get("p1"));

            env.undeployAll();
        }
    }

    private static class ExprCoreCaseSyntax2NoAsName implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String caseSubExpr = "case intPrimitive when 1 then 0 end";
            String epl = "@name('s0') select " + caseSubExpr +
                " from SupportBean#length(10)";

            env.compileDeploy(epl).addListener("s0");
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType(caseSubExpr));

            sendSupportBeanEvent(env, 1);
            EventBean theEvent = env.listener("s0").getAndResetLastNewData()[0];
            assertEquals(0, theEvent.get(caseSubExpr));

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
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        env.sendEventBean(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ExprCoreCase.class);
}
