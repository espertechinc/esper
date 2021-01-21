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
import com.espertech.esper.regressionlib.support.bean.SupportBeanNumeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExprCoreBigNumberSupport {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprCoreBigNumberEquals());
        execs.add(new ExprCoreBigNumberRelOp());
        execs.add(new ExprCoreBigNumberBetween());
        execs.add(new ExprCoreBigNumberIn());
        execs.add(new ExprCoreBigNumberMath());
        execs.add(new ExprCoreBigNumberAggregation());
        execs.add(new ExprCoreBigNumberMinMax());
        execs.add(new ExprCoreBigNumberFilterEquals());
        execs.add(new ExprCoreBigNumberJoin());
        execs.add(new ExprCoreBigNumberCastAndUDF());
        return execs;
    }

    private static class ExprCoreBigNumberEquals implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            // test equals BigDecimal
            String epl = "@name('s0') select * from SupportBeanNumeric where bigdec = 1 or bigdec = intOne or bigdec = doubleOne";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

            sendBigNumEvent(env, -1, 1);
            env.assertListenerInvoked("s0");
            sendBigNumEvent(env, -1, 2);
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBeanNumeric(2, 0, null, new BigDecimal(2), 0, 0));
            env.assertListenerInvoked("s0");
            env.sendEventBean(new SupportBeanNumeric(3, 0, null, new BigDecimal(2), 0, 0));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBeanNumeric(0, 0, null, new BigDecimal(3d), 3d, 0));
            env.assertListenerInvoked("s0");
            env.sendEventBean(new SupportBeanNumeric(0, 0, null, new BigDecimal(3.9999d), 4d, 0));
            env.assertListenerNotInvoked("s0");

            // test equals BigInteger
            env.undeployAll();
            epl = "@name('s0') select * from SupportBeanNumeric where bigdec = bigint or bigint = intOne or bigint = 1";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

            env.sendEventBean(new SupportBeanNumeric(0, 0, BigInteger.valueOf(2), new BigDecimal(2), 0, 0));
            env.assertListenerInvoked("s0");
            env.sendEventBean(new SupportBeanNumeric(0, 0, BigInteger.valueOf(3), new BigDecimal(2), 0, 0));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBeanNumeric(2, 0, BigInteger.valueOf(2), null, 0, 0));
            env.assertListenerInvoked("s0");
            env.sendEventBean(new SupportBeanNumeric(3, 0, BigInteger.valueOf(2), null, 0, 0));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBeanNumeric(0, 0, BigInteger.valueOf(1), null, 0, 0));
            env.assertListenerInvoked("s0");
            env.sendEventBean(new SupportBeanNumeric(0, 0, BigInteger.valueOf(4), null, 0, 0));
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ExprCoreBigNumberRelOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // relational op tests handled by relational op unit test
            String epl = "@name('s0') select * from SupportBeanNumeric where bigdec < 10 and bigint > 10";
            env.compileDeploy(epl).addListener("s0");

            sendBigNumEvent(env, 10, 10);
            env.assertListenerNotInvoked("s0");

            sendBigNumEvent(env, 11, 9);
            env.assertListenerInvoked("s0");
            env.undeployAll();

            epl = "@name('s0') select * from SupportBeanNumeric where bigdec < 10.0";
            env.compileDeployAddListenerMile(epl, "s0", 1);

            sendBigNumEvent(env, 0, 11);
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBeanNumeric(null, new BigDecimal(9.999)));
            env.assertListenerInvoked("s0");
            env.undeployAll();

            // test float
            env.compileDeployAddListenerMile("@name('s0') select * from SupportBeanNumeric where floatOne < 10f and floatTwo > 10f", "s0", 2);

            env.sendEventBean(new SupportBeanNumeric(true, 1f, 20f));
            env.assertListenerInvoked("s0");
            env.sendEventBean(new SupportBeanNumeric(true, 20f, 1f));
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ExprCoreBigNumberBetween implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBeanNumeric where bigdec between 10 and 20 or bigint between 100 and 200";
            env.compileDeploy(epl).addListener("s0");

            sendBigNumEvent(env, 0, 9);
            env.assertListenerNotInvoked("s0");

            sendBigNumEvent(env, 0, 10);
            env.assertListenerInvoked("s0");

            sendBigNumEvent(env, 99, 0);
            env.assertListenerNotInvoked("s0");

            sendBigNumEvent(env, 100, 0);
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ExprCoreBigNumberIn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBeanNumeric where bigdec in (10, 20d) or bigint in (0x02, 3)";
            env.compileDeploy(epl).addListener("s0");

            sendBigNumEvent(env, 0, 9);
            env.assertListenerNotInvoked("s0");

            sendBigNumEvent(env, 0, 10);
            env.assertListenerInvoked("s0");

            env.sendEventBean(new SupportBeanNumeric(null, new BigDecimal(20d)));
            env.assertListenerInvoked("s0");

            sendBigNumEvent(env, 99, 0);
            env.assertListenerNotInvoked("s0");

            sendBigNumEvent(env, 2, 0);
            env.assertListenerInvoked("s0");

            sendBigNumEvent(env, 3, 0);
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ExprCoreBigNumberMath implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBeanNumeric " +
                "where bigdec+bigint=100 or bigdec+1=2 or bigdec+2d=5.0 or bigint+5L=8 or bigint+5d=9.0";
            env.compileDeploy(epl).addListener("s0");

            sendBigNumEvent(env, 50, 49);
            env.assertListenerNotInvoked("s0");

            sendBigNumEvent(env, 50, 50);
            env.assertListenerInvoked("s0");

            sendBigNumEvent(env, 0, 1);
            env.assertListenerInvoked("s0");

            sendBigNumEvent(env, 0, 2);
            env.assertListenerNotInvoked("s0");

            sendBigNumEvent(env, 0, 3);
            env.assertListenerInvoked("s0");

            sendBigNumEvent(env, 0, 0);
            env.assertListenerNotInvoked("s0");

            sendBigNumEvent(env, 3, 0);
            env.assertListenerInvoked("s0");

            sendBigNumEvent(env, 4, 0);
            env.assertListenerInvoked("s0");
            env.undeployAll();

            env.compileDeployAddListenerMile("@name('s0') select bigdec+bigint as v1, bigdec+2 as v2, bigdec+3d as v3, bigint+5L as v4, bigint+5d as v5 " +
                " from SupportBeanNumeric", "s0", 1);
            env.listenerReset("s0");

            env.assertStatement("s0", statement -> {
                assertEquals(BigDecimal.class, statement.getEventType().getPropertyType("v1"));
                assertEquals(BigDecimal.class, statement.getEventType().getPropertyType("v2"));
                assertEquals(BigDecimal.class, statement.getEventType().getPropertyType("v3"));
                assertEquals(BigInteger.class, statement.getEventType().getPropertyType("v4"));
                assertEquals(BigDecimal.class, statement.getEventType().getPropertyType("v5"));
            });

            sendBigNumEvent(env, 1, 2);
            env.assertPropsNew("s0", "v1,v2,v3,v4,v5".split(","),
                new Object[]{new BigDecimal(3), new BigDecimal(4), new BigDecimal(5d), BigInteger.valueOf(6), new BigDecimal(6d)});

            // test aggregation-sum, multiplication and division all together; test for ESPER-340
            env.undeployAll();

            env.compileDeployAddListenerMile("@name('s0') select (sum(bigdecTwo * bigdec)/sum(bigdec)) as avgRate from SupportBeanNumeric", "s0", 2);
            env.assertStatement("s0", statement -> assertEquals(BigDecimal.class, statement.getEventType().getPropertyType("avgRate")));
            sendBigNumEvent(env, 0, 5);
            env.assertEventNew("s0", event -> {
                Object avgRate = event.get("avgRate");
                assertTrue(avgRate instanceof BigDecimal);
                assertEquals(new BigDecimal(5d), avgRate);
            });

            env.undeployAll();
        }
    }

    private static class ExprCoreBigNumberAggregation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String fields = "sum(bigint),sum(bigdec)," +
                "avg(bigint),avg(bigdec)," +
                "median(bigint),median(bigdec)," +
                "stddev(bigint),stddev(bigdec)," +
                "avedev(bigint),avedev(bigdec)," +
                "min(bigint),min(bigdec)";
            String epl = "@name('s0') select " + fields + " from SupportBeanNumeric";
            env.compileDeploy(epl).addListener("s0");

            String[] fieldList = fields.split(",");
            sendBigNumEvent(env, 1, 2);
            env.assertPropsNew("s0", fieldList,
                new Object[]{BigInteger.valueOf(1), new BigDecimal(2d),        // sum
                    new BigDecimal(1), new BigDecimal(2),               // avg
                    1d, 2d,               // median
                    null, null,
                    0.0, 0.0,
                    BigInteger.valueOf(1), new BigDecimal(2),
                });

            env.milestone(1);

            sendBigNumEvent(env, 4, 5);
            env.assertPropsNew("s0", fieldList,
                new Object[]{BigInteger.valueOf(5), BigDecimal.valueOf(7),        // sum
                    BigDecimal.valueOf(2.5d), BigDecimal.valueOf(3.5d),               // avg
                    2.5d, 3.5d,               // median
                    2.1213203435596424, 2.1213203435596424,
                    1.5, 1.5,
                    BigInteger.valueOf(1), BigDecimal.valueOf(2),
                });

            env.undeployAll();
        }
    }

    private static class ExprCoreBigNumberMinMax implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select min(bigint, 10) as v1, min(10, bigint) as v2, " +
                "max(bigdec, 10) as v3, max(10, 100d, bigint, bigdec) as v4 from SupportBeanNumeric";
            env.compileDeploy(epl).addListener("s0");

            String[] fieldList = "v1,v2,v3,v4".split(",");

            sendBigNumEvent(env, 1, 2);
            env.assertPropsNew("s0", fieldList,
                new Object[]{BigInteger.valueOf(1), BigInteger.valueOf(1), new BigDecimal(10), new BigDecimal(100d)});

            sendBigNumEvent(env, 40, 300);
            env.assertPropsNew("s0", fieldList,
                new Object[]{BigInteger.valueOf(10), BigInteger.valueOf(10), new BigDecimal(300), new BigDecimal(300)});

            sendBigNumEvent(env, 250, 200);
            env.assertPropsNew("s0", fieldList,
                new Object[]{BigInteger.valueOf(10), BigInteger.valueOf(10), new BigDecimal(200), new BigDecimal(250)});

            env.undeployAll();
        }
    }

    private static class ExprCoreBigNumberFilterEquals implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fieldList = "bigdec".split(",");
            String epl = "@name('s0') select bigdec from SupportBeanNumeric(bigdec = 4)";
            env.compileDeploy(epl).addListener("s0");

            sendBigNumEvent(env, 0, 2);
            env.assertListenerNotInvoked("s0");

            sendBigNumEvent(env, 0, 4);
            env.assertPropsNew("s0", fieldList, new Object[]{new BigDecimal(4)});

            env.undeployAll();
            env.compileDeployAddListenerMile("@name('s0') select bigdec from SupportBeanNumeric(bigdec = 4d)", "s0", 1);

            sendBigNumEvent(env, 0, 4);
            env.assertListenerInvoked("s0");

            env.sendEventBean(new SupportBeanNumeric(BigInteger.valueOf(0), new BigDecimal(4d)));
            env.assertPropsNew("s0", fieldList, new Object[]{new BigDecimal(4d)});

            env.undeployAll();
            env.compileDeployAddListenerMile("@name('s0') select bigdec from SupportBeanNumeric(bigint = 4)", "s0", 2);

            sendBigNumEvent(env, 3, 4);
            env.assertListenerNotInvoked("s0");

            sendBigNumEvent(env, 4, 3);
            env.assertPropsNew("s0", fieldList, new Object[]{new BigDecimal(3)});

            env.undeployAll();
        }
    }

    private static class ExprCoreBigNumberJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fieldList = "bigint,bigdec".split(",");
            String epl = "@name('s0') select bigint,bigdec from SupportBeanNumeric#keepall(), SupportBean#keepall " +
                "where intPrimitive = bigint and doublePrimitive = bigdec";
            env.compileDeploy(epl).addListener("s0");

            sendSupportBean(env, 2, 3);
            sendBigNumEvent(env, 0, 2);
            sendBigNumEvent(env, 2, 0);
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBeanNumeric(BigInteger.valueOf(2), new BigDecimal(3d)));
            env.assertPropsNew("s0", fieldList, new Object[]{BigInteger.valueOf(2), new BigDecimal(3d)});

            env.undeployAll();
        }
    }

    private static class ExprCoreBigNumberCastAndUDF implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select SupportStaticMethodLib.myBigIntFunc(cast(2, BigInteger)) as v1, SupportStaticMethodLib.myBigDecFunc(cast(3d, BigDecimal)) as v2 from SupportBeanNumeric";
            env.compileDeploy(epl).addListener("s0");

            String[] fieldList = "v1,v2".split(",");
            sendBigNumEvent(env, 0, 2);
            env.assertPropsNew("s0", fieldList, new Object[]{BigInteger.valueOf(2), new BigDecimal(3.0)});

            env.undeployAll();
        }
    }

    private static void sendBigNumEvent(RegressionEnvironment env, int bigInt, double bigDec) {
        SupportBeanNumeric bean = new SupportBeanNumeric(BigInteger.valueOf(bigInt), new BigDecimal(bigDec));
        bean.setBigdecTwo(new BigDecimal(bigDec));
        env.sendEventBean(bean);
    }

    private static void sendSupportBean(RegressionEnvironment env, int intPrimitive, double doublePrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setDoublePrimitive(doublePrimitive);
        env.sendEventBean(bean);
    }
}