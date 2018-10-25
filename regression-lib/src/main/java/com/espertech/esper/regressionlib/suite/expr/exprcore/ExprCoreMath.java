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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.EPStatement;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
            String epl = "@name('s0') select " +
                "10d+5d as c0," +
                "10d-5d as c1," +
                "10d*5d as c2," +
                "10d/5d as c3," +
                "10d%4d as c4" +
                " from SupportBean";

            env.compileDeploy(epl).addListener("s0");

            String[] fields = "c0,c1,c2,c3,c4".split(",");
            assertTypes(env.statement("s0"), fields, Double.class, Double.class, Double.class, Double.class, Double.class);

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{15d, 5d, 50d, 2d, 2d});

            env.undeployAll();
        }
    }

    private static class ExprCoreMathLong implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "10L+5L as c0," +
                "10L-5L as c1," +
                "10L*5L as c2," +
                "10L/5L as c3" +
                " from SupportBean";

            env.compileDeploy(epl).addListener("s0");

            String[] fields = "c0,c1,c2,c3".split(",");
            assertTypes(env.statement("s0"), fields, Long.class, Long.class, Long.class, Double.class);

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{15L, 5L, 50L, 2d});

            env.undeployAll();
        }
    }

    private static class ExprCoreMathFloat implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "10f+5f as c0," +
                "10f-5f as c1," +
                "10f*5f as c2," +
                "10f/5f as c3," +
                "10f%4f as c4" +
                " from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "c0,c1,c2,c3,c4".split(",");
            assertTypes(env.statement("s0"), fields, Float.class, Float.class, Float.class, Double.class, Float.class);

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{15f, 5f, 50f, 2d, 2f});

            env.undeployAll();
        }
    }

    private static class ExprCoreMathIntWNull implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intPrimitive/intBoxed as result from SupportBean";

            env.compileDeploy(epl).addListener("s0");
            assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("result"));

            sendEvent(env, 100, 3);
            assertEquals(100 / 3d, env.listener("s0").assertOneGetNewAndReset().get("result"));

            sendEvent(env, 100, null);
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("result"));

            sendEvent(env, 100, 0);
            assertEquals(Double.POSITIVE_INFINITY, env.listener("s0").assertOneGetNewAndReset().get("result"));

            sendEvent(env, -5, 0);
            assertEquals(Double.NEGATIVE_INFINITY, env.listener("s0").assertOneGetNewAndReset().get("result"));

            env.undeployAll();
        }
    }

    private static class ExprCoreMathBigDecConv implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "10+BigDecimal.valueOf(5,0) as c0," +
                "10-BigDecimal.valueOf(5,0) as c1," +
                "10*BigDecimal.valueOf(5,0) as c2," +
                "10/BigDecimal.valueOf(5,0) as c3" +
                " from SupportBean";

            env.compileDeploy(epl).addListener("s0");

            String[] fields = "c0,c1,c2,c3".split(",");
            assertTypes(env.statement("s0"), fields, BigDecimal.class, BigDecimal.class, BigDecimal.class, BigDecimal.class);

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{BigDecimal.valueOf(15, 0), BigDecimal.valueOf(5, 0), BigDecimal.valueOf(50, 0), BigDecimal.valueOf(2, 0)});

            env.undeployAll();
        }
    }

    private static class ExprCoreMathBigIntConv implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "10+BigInteger.valueOf(5) as c0," +
                "10-BigInteger.valueOf(5) as c1," +
                "10*BigInteger.valueOf(5) as c2," +
                "10/BigInteger.valueOf(5) as c3" +
                " from SupportBean";

            env.compileDeploy(epl).addListener("s0");

            String[] fields = "c0,c1,c2,c3".split(",");
            assertTypes(env.statement("s0"), fields, BigInteger.class, BigInteger.class, BigInteger.class, Double.class);

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{BigInteger.valueOf(15), BigInteger.valueOf(5), BigInteger.valueOf(50), 2d});

            env.undeployAll();
        }
    }

    private static class ExprCoreMathBigInt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "BigInteger.valueOf(10)+BigInteger.valueOf(5) as c0," +
                "BigInteger.valueOf(10)-BigInteger.valueOf(5) as c1," +
                "BigInteger.valueOf(10)*BigInteger.valueOf(5) as c2," +
                "BigInteger.valueOf(10)/BigInteger.valueOf(5) as c3" +
                " from SupportBean";

            env.compileDeploy(epl).addListener("s0");

            String[] fields = "c0,c1,c2,c3".split(",");
            assertTypes(env.statement("s0"), fields, BigInteger.class, BigInteger.class, BigInteger.class, Double.class);

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{BigInteger.valueOf(15), BigInteger.valueOf(5), BigInteger.valueOf(50), 2d});

            env.undeployAll();
        }
    }

    private static class ExprCoreMathBigDec implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@Name('s0') select " +
                "BigDecimal.valueOf(10,0)+BigDecimal.valueOf(5,0) as c0," +
                "BigDecimal.valueOf(10,0)-BigDecimal.valueOf(5,0) as c1," +
                "BigDecimal.valueOf(10,0)*BigDecimal.valueOf(5,0) as c2," +
                "BigDecimal.valueOf(10,0)/BigDecimal.valueOf(5,0) as c3" +
                " from SupportBean";

            env.compileDeploy(epl).addListener("s0");

            String[] fields = "c0,c1,c2,c3".split(",");
            assertTypes(env.statement("s0"), fields, BigDecimal.class, BigDecimal.class, BigDecimal.class, BigDecimal.class);

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{BigDecimal.valueOf(15, 0), BigDecimal.valueOf(5, 0), BigDecimal.valueOf(50, 0), BigDecimal.valueOf(2, 0)});

            env.undeployAll();
        }
    }

    private static class ExprCoreMathShortAndByteArithmetic implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "shortPrimitive + shortBoxed as c0," +
                "bytePrimitive + byteBoxed as c1, " +
                "shortPrimitive - shortBoxed as c2," +
                "bytePrimitive - byteBoxed as c3, " +
                "shortPrimitive * shortBoxed as c4," +
                "bytePrimitive * byteBoxed as c5, " +
                "shortPrimitive / shortBoxed as c6," +
                "bytePrimitive / byteBoxed as c7," +
                "shortPrimitive + longPrimitive as c8," +
                "bytePrimitive + longPrimitive as c9 " +
                "from SupportBean";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7,c8,c9".split(",");

            for (String field : fields) {
                Class expected = Integer.class;
                if (field.equals("c6") || field.equals("c7")) {
                    expected = Double.class;
                }
                if (field.equals("c8") || field.equals("c9")) {
                    expected = Long.class;
                }
                Assert.assertEquals("for field " + field, expected, env.statement("s0").getEventType().getPropertyType(field));
            }

            SupportBean bean = new SupportBean();
            bean.setShortPrimitive((short) 5);
            bean.setShortBoxed((short) 6);
            bean.setBytePrimitive((byte) 4);
            bean.setByteBoxed((byte) 2);
            bean.setLongPrimitive(10);
            env.sendEventBean(bean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{11, 6, -1, 2, 30, 8, 5d / 6d, 2d, 15L, 14L});

            env.undeployAll();
        }
    }

    private static class ExprCoreMathModulo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select longBoxed % intBoxed as myMod from SupportBean#length(3) where not(longBoxed > intBoxed)";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, 1, 1, (short) 0);
            Assert.assertEquals(0L, env.listener("s0").getLastNewData()[0].get("myMod"));
            env.listener("s0").reset();

            sendEvent(env, 2, 1, (short) 0);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendEvent(env, 2, 3, (short) 0);
            Assert.assertEquals(2L, env.listener("s0").getLastNewData()[0].get("myMod"));
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, long longBoxed, int intBoxed, short shortBoxed) {
        sendBoxedEvent(env, longBoxed, intBoxed, shortBoxed);
    }

    private static void sendBoxedEvent(RegressionEnvironment env, Long longBoxed, Integer intBoxed, Short shortBoxed) {
        SupportBean bean = new SupportBean();
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env, Integer intPrimitive, Integer intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static void assertTypes(EPStatement stmt, String[] fields, Class... types) {
        for (int i = 0; i < fields.length; i++) {
            assertEquals("failed for " + i, types[i], stmt.getEventType().getPropertyType(fields[i]));
        }
    }
}
