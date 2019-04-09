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
package com.espertech.esper.regressionlib.suite.epl.fromclausemethod;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.UuidGenerator;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EPLFromClauseMethodMultikeyWArray {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLFromClauseMultikeyWArrayJoinArray());
        execs.add(new EPLFromClauseMultikeyWArrayJoinTwoField());
        execs.add(new EPLFromClauseMultikeyWArrayJoinComposite());
        execs.add(new EPLFromClauseMultikeyWArrayParameterizedByArray());
        execs.add(new EPLFromClauseMultikeyWArrayParameterizedByTwoField());
        return execs;
    }

    private static class EPLFromClauseMultikeyWArrayParameterizedByTwoField implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportEventWithManyArray as e,\n" +
                "method:" + SupportJoinResultIsArray.class.getName() + ".getResultTwoField(e.id, e.intOne) as s";
            env.compileDeploy(epl).addListener("s0");

            SupportBean sb1 = sendManyArrayGetSB(env, "MA1", new int[]{1, 2});
            SupportBean sb2 = sendManyArrayGetSB(env, "MA2", new int[]{1});
            SupportBean sb3 = sendManyArrayGetSB(env, "MA3", new int[]{});
            SupportBean sb4 = sendManyArrayGetSB(env, "MA4", null);

            sendManyArray(env, "MA3", new int[]{});
            assertReceivedUUID(env, sb3.getTheString());

            sendManyArray(env, "MA1", new int[]{1, 2});
            assertReceivedUUID(env, sb1.getTheString());

            sendManyArray(env, "MA4", null);
            assertReceivedUUID(env, sb4.getTheString());

            sendManyArray(env, "MA2", new int[]{1});
            assertReceivedUUID(env, sb2.getTheString());

            SupportBean sb5 = sendManyArrayGetSB(env, "MA1", new int[]{1, 3});
            assertNotEquals(sb5.getTheString(), sb1.getTheString());

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMultikeyWArrayParameterizedByArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportEventWithManyArray as e,\n" +
                "method:" + SupportJoinResultIsArray.class.getName() + ".getResultIntArray(e.intOne) as s";
            env.compileDeploy(epl).addListener("s0");

            sendManyArray(env, "E1", new int[]{1, 2});
            SupportBean sb12 = (SupportBean) env.listener("s0").assertOneGetNewAndReset().get("s");

            sendManyArray(env, "E2", new int[]{1, 2});
            assertReceivedUUID(env, sb12.getTheString());

            sendManyArray(env, "E3", new int[]{3});
            SupportBean sb3 = (SupportBean) env.listener("s0").assertOneGetNewAndReset().get("s");

            sendManyArray(env, "E4", new int[]{3});
            assertReceivedUUID(env, sb3.getTheString());

            sendManyArray(env, "E5", new int[]{1, 2});
            assertReceivedUUID(env, sb12.getTheString());

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMultikeyWArrayJoinComposite implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportEventWithManyArray as e,\n" +
                "method:" + SupportJoinResultIsArray.class.getName() + ".getArray() as s " +
                "where s.doubleArray = e.doubleOne and s.intArray = e.intOne and s.value > e.value";

            runAssertion(env, epl);

            sendManyArray(env, "E3", new double[]{3, 4}, new int[]{30, 40}, 1000);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMultikeyWArrayJoinTwoField implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportEventWithManyArray as e,\n" +
                "method:" + SupportJoinResultIsArray.class.getName() + ".getArray() as s " +
                "where s.doubleArray = e.doubleOne and s.intArray = e.intOne";

            runAssertion(env, epl);

            sendManyArray(env, "E3", new double[]{3, 4}, new int[]{30, 41}, 0);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMultikeyWArrayJoinArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportEventWithManyArray as e,\n" +
                "method:" + SupportJoinResultIsArray.class.getName() + ".getArray() as s " +
                "where s.doubleArray = e.doubleOne";

            runAssertion(env, epl);

            env.undeployAll();
        }
    }

    private static void runAssertion(RegressionEnvironment env, String epl) {

        env.compileDeploy(epl).addListener("s0");

        sendManyArray(env, "E1", new double[]{3, 4}, new int[]{30, 40}, 50);
        assertReceived(env, "E1", "DA2");

        env.milestone(0);

        sendManyArray(env, "E2", new double[]{1, 2}, new int[]{10, 20}, 60);
        assertReceived(env, "E2", "DA1");

        sendManyArray(env, "E3", new double[]{3, 4}, new int[]{30, 40}, 70);
        assertReceived(env, "E3", "DA2");

        sendManyArray(env, "E4", new double[]{1}, new int[]{30, 40}, 80);
        assertFalse(env.listener("s0").getAndClearIsInvoked());
    }

    private static void sendManyArray(RegressionEnvironment env, String id, double[] doubles, int[] ints, int value) {
        env.sendEventBean(new SupportEventWithManyArray(id).withDoubleOne(doubles).withIntOne(ints).withValue(value));
    }

    private static void sendManyArray(RegressionEnvironment env, String id, int[] ints) {
        env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(ints));
    }

    private static SupportBean sendManyArrayGetSB(RegressionEnvironment env, String id, int[] ints) {
        env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(ints));
        return (SupportBean) env.listener("s0").assertOneGetNewAndReset().get("s");
    }

    private static void assertReceived(RegressionEnvironment env, String idOne, String idTwo) {
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(),
            "e.id,s.id".split(","), new Object[]{idOne, idTwo});
    }

    public static class SupportJoinResultIsArray {
        public static SupportDoubleAndIntArray[] getArray() {
            return new SupportDoubleAndIntArray[]{
                new SupportDoubleAndIntArray("DA1", new double[]{1, 2}, new int[]{10, 20}, 100),
                new SupportDoubleAndIntArray("DA2", new double[]{3, 4}, new int[]{30, 40}, 300),
            };
        }

        public static SupportBean getResultIntArray(int[] array) {
            return new SupportBean(UuidGenerator.generate(), 0);
        }

        public static SupportBean getResultTwoField(String id, int[] array) {
            return new SupportBean(UuidGenerator.generate(), 0);
        }
    }

    private static void assertReceivedUUID(RegressionEnvironment env, String uuidExpected) {
        SupportBean sb = (SupportBean) env.listener("s0").assertOneGetNewAndReset().get("s");
        assertEquals(uuidExpected, sb.getTheString());
    }

    public static class SupportDoubleAndIntArray {
        private final String id;
        private final double[] doubleArray;
        private final int[] intArray;
        private final int value;

        public SupportDoubleAndIntArray(String id, double[] doubleArray, int[] intArray, int value) {
            this.id = id;
            this.doubleArray = doubleArray;
            this.intArray = intArray;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public double[] getDoubleArray() {
            return doubleArray;
        }

        public int[] getIntArray() {
            return intArray;
        }

        public int getValue() {
            return value;
        }
    }
}
