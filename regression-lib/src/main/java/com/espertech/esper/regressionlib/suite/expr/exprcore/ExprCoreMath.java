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

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;
import com.espertech.esper.runtime.client.EPStatement;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class ExprCoreMath {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreMathDouble());
        executions.add(new ExprCoreMathLong());
        executions.add(new ExprCoreMathFloat());
        executions.add(new ExprCoreMathIntWNull());
        executions.add(new ExprCoreMathBigDec());
        executions.add(new ExprCoreMathBigDecConv());
        executions.add(new ExprCoreMathBigInt());
        executions.add(new ExprCoreMathBigIntConv());
        executions.add(new ExprCoreMathShortAndByteArithmetic());
        executions.add(new ExprCoreMathModulo());
        return executions;
    }

    private static class ExprCoreMathDouble implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expressions(fields, "10d+5d", "10d-5d", "10d*5d", "10d/5d", "10d%4d")
                .statementConsumer(stmt -> assertTypes(stmt, fields, Double.class, Double.class, Double.class, Double.class, Double.class));

            builder.assertion(new SupportBean()).expect(fields, 15d, 5d, 50d, 2d, 2d);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreMathLong implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expressions(fields, "10L+5L", "10L-5L", "10L*5L", "10L/5L")
                .statementConsumer(stmt -> assertTypes(stmt, fields, Long.class, Long.class, Long.class, Double.class));

            builder.assertion(new SupportBean()).expect(fields, 15L, 5L, 50L, 2d);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreMathFloat implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expressions(fields, "10f+5f", "10f-5f", "10f*5f", "10f/5f", "10f%4f")
                .statementConsumer(stmt -> assertTypes(stmt, fields, Float.class, Float.class, Float.class, Double.class, Float.class));

            builder.assertion(new SupportBean()).expect(fields, 15f, 5f, 50f, 2d, 2f);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreMathIntWNull implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expression(fields[0], "intPrimitive/intBoxed")
                .expression(fields[1], "intPrimitive*intBoxed")
                .expression(fields[2], "intPrimitive+intBoxed")
                .expression(fields[3], "intPrimitive-intBoxed")
                .expression(fields[4], "intBoxed/intPrimitive")
                .expression(fields[5], "intBoxed*intPrimitive")
                .expression(fields[6], "intBoxed+intPrimitive")
                .expression(fields[7], "intBoxed-intPrimitive")
                .statementConsumer(stmt -> assertTypes(stmt, fields, Double.class, Integer.class, Integer.class, Integer.class, Double.class, Integer.class, Integer.class, Integer.class));

            builder.assertion(makeEvent(100, 3)).expect(fields, 100 / 3d, 300, 103, 97, 3 / 100d, 300, 103, -97);
            builder.assertion(makeEvent(100, null)).expect(fields, null, null, null, null, null, null, null, null);
            builder.assertion(makeEvent(100, 0)).expect(fields, Double.POSITIVE_INFINITY, 0, 100, 100, 0d, 0, 100, -100);
            builder.assertion(makeEvent(-5, 0)).expect(fields, Double.NEGATIVE_INFINITY, 0, -5, -5, -0d, 0, -5, 5);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreMathBigDecConv implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expression(fields[0], "10+BigDecimal.valueOf(5,0)")
                .expression(fields[1], "10-BigDecimal.valueOf(5,0)")
                .expression(fields[2], "10*BigDecimal.valueOf(5,0)")
                .expression(fields[3], "10/BigDecimal.valueOf(5,0)")
                .statementConsumer(stmt -> assertTypes(stmt, fields, BigDecimal.class, BigDecimal.class, BigDecimal.class, BigDecimal.class));

            builder.assertion(new SupportBean()).expect(fields, BigDecimal.valueOf(15, 0), BigDecimal.valueOf(5, 0), BigDecimal.valueOf(50, 0), BigDecimal.valueOf(2, 0));

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreMathBigIntConv implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expression(fields[0], "10+BigInteger.valueOf(5)")
                .expression(fields[1], "10-BigInteger.valueOf(5)")
                .expression(fields[2], "10*BigInteger.valueOf(5)")
                .expression(fields[3], "10/BigInteger.valueOf(5)")
                .statementConsumer(stmt -> assertTypes(stmt, fields, BigInteger.class, BigInteger.class, BigInteger.class, Double.class));

            builder.assertion(new SupportBean()).expect(fields, BigInteger.valueOf(15), BigInteger.valueOf(5), BigInteger.valueOf(50), 2d);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreMathBigInt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expression(fields[0], "BigInteger.valueOf(10)+BigInteger.valueOf(5)")
                .expression(fields[1], "BigInteger.valueOf(10)-BigInteger.valueOf(5)")
                .expression(fields[2], "BigInteger.valueOf(10)*BigInteger.valueOf(5)")
                .expression(fields[3], "BigInteger.valueOf(10)/BigInteger.valueOf(5)")
                .statementConsumer(stmt -> assertTypes(stmt, fields, BigInteger.class, BigInteger.class, BigInteger.class, Double.class));

            builder.assertion(new SupportBean()).expect(fields, BigInteger.valueOf(15), BigInteger.valueOf(5), BigInteger.valueOf(50), 2d);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreMathBigDec implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expression(fields[0], "BigDecimal.valueOf(10,0)+BigDecimal.valueOf(5,0)")
                .expression(fields[1], "BigDecimal.valueOf(10,0)-BigDecimal.valueOf(5,0)")
                .expression(fields[2], "BigDecimal.valueOf(10,0)*BigDecimal.valueOf(5,0)")
                .expression(fields[3], "BigDecimal.valueOf(10,0)/BigDecimal.valueOf(5,0)")
                .statementConsumer(stmt -> assertTypes(stmt, fields, BigDecimal.class, BigDecimal.class, BigDecimal.class, BigDecimal.class));

            builder.assertion(new SupportBean()).expect(fields, BigDecimal.valueOf(15, 0), BigDecimal.valueOf(5, 0), BigDecimal.valueOf(50, 0), BigDecimal.valueOf(2, 0));

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreMathShortAndByteArithmetic implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7,c8,c9".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expression(fields[0], "shortPrimitive + shortBoxed")
                .expression(fields[1], "bytePrimitive + byteBoxed ")
                .expression(fields[2], "shortPrimitive - shortBoxed")
                .expression(fields[3], "bytePrimitive - byteBoxed ")
                .expression(fields[4], "shortPrimitive * shortBoxed")
                .expression(fields[5], "bytePrimitive * byteBoxed ")
                .expression(fields[6], "shortPrimitive / shortBoxed")
                .expression(fields[7], "bytePrimitive / byteBoxed")
                .expression(fields[8], "shortPrimitive + longPrimitive")
                .expression(fields[9], "bytePrimitive + longPrimitive");

            Consumer<EPStatement> typeVerifier = stmt -> {
                for (String field : fields) {
                    Class expected = Integer.class;
                    if (field.equals("c6") || field.equals("c7")) {
                        expected = Double.class;
                    }
                    if (field.equals("c8") || field.equals("c9")) {
                        expected = Long.class;
                    }
                    Assert.assertEquals("for field " + field, expected, stmt.getEventType().getPropertyType(field));
                }
            };
            builder.statementConsumer(typeVerifier);

            SupportBean bean = new SupportBean();
            bean.setShortPrimitive((short) 5);
            bean.setShortBoxed((short) 6);
            bean.setBytePrimitive((byte) 4);
            bean.setByteBoxed((byte) 2);
            bean.setLongPrimitive(10);
            env.sendEventBean(bean);
            builder.assertion(bean).expect(fields, 11, 6, -1, 2, 30, 8, 5d / 6d, 2d, 15L, 14L);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreMathModulo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expressions(fields, "longBoxed % intBoxed", "intPrimitive % intBoxed");

            builder.assertion(makeBoxedEvent(5, 1L, 1)).expect(fields, 0L, 0);
            builder.assertion(makeBoxedEvent(5, 2L, 3)).expect(fields, 2L, 2);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static SupportBean makeBoxedEvent(int intPrimitive, Long longBoxed, Integer intBoxed) {
        SupportBean bean = new SupportBean("E", intPrimitive);
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        return bean;
    }

    private static SupportBean makeEvent(Integer intPrimitive, Integer intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setIntPrimitive(intPrimitive);
        return bean;
    }

    private static void assertTypes(EPStatement stmt, String[] fields, Class... types) {
        assertEquals(fields.length, types.length);
        for (int i = 0; i < fields.length; i++) {
            assertEquals("failed for " + i, types[i], stmt.getEventType().getPropertyType(fields[i]));
        }
    }
}
