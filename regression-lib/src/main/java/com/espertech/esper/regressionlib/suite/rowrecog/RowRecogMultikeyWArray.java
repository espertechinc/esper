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
package com.espertech.esper.regressionlib.suite.rowrecog;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;

import java.util.ArrayList;
import java.util.Collection;

public class RowRecogMultikeyWArray {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new RowRecogPartitionMultikeyWArray());
        execs.add(new RowRecogPartitionMultikeyPlain());
        return execs;
    }

    private static class RowRecogPartitionMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select * from SupportEventWithIntArray " +
                "match_recognize (" +
                " partition by array" +
                " measures A.id as a, B.id as b" +
                " pattern (A B)" +
                " define" +
                " A as A.value = 1," +
                " B as B.value = 2" +
                ")";

            env.compileDeploy(text).addListener("s0");

            sendArray(env, "E1", new int[]{1, 2}, 1);
            sendArray(env, "E2", new int[]{1}, 1);
            sendArray(env, "E3", null, 1);
            sendArray(env, "E4", new int[]{}, 1);

            env.milestone(0);

            sendArray(env, "E10", new int[]{1, 2}, 2);
            assertReceived(env, "E1", "E10");

            sendArray(env, "E11", new int[]{}, 2);
            assertReceived(env, "E4", "E11");

            sendArray(env, "E12", new int[]{1}, 2);
            assertReceived(env, "E2", "E12");

            sendArray(env, "E13", null, 2);
            assertReceived(env, "E3", "E13");

            env.undeployAll();
        }
    }

    private static class RowRecogPartitionMultikeyPlain implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select * from SupportBean " +
                "match_recognize (" +
                " partition by intPrimitive, longPrimitive" +
                " measures A.theString as a, B.theString as b" +
                " pattern (A B)" +
                " define" +
                " A as A.doublePrimitive = 1," +
                " B as B.doublePrimitive = 2" +
                ")";

            env.compileDeploy(text).addListener("s0");

            sendSB(env, "E1", 1, 2, 1);
            sendSB(env, "E2", 1, 3, 1);
            sendSB(env, "E3", 2, 2, 1);

            env.milestone(0);

            sendSB(env, "E10", 2, 2, 2);
            assertReceived(env, "E3", "E10");

            sendSB(env, "E11", 1, 3, 2);
            assertReceived(env, "E2", "E11");

            sendSB(env, "E12", 1, 2, 2);
            assertReceived(env, "E1", "E12");

            env.undeployAll();
        }
    }

    private static void sendSB(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        env.sendEventBean(sb);
    }

    private static void sendArray(RegressionEnvironment env, String id, int[] array, int value) {
        env.sendEventBean(new SupportEventWithIntArray(id, array, value));
    }

    private static void assertReceived(RegressionEnvironment env, String a, String b) {
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a,b".split(","), new Object[] {a, b});
    }
}
