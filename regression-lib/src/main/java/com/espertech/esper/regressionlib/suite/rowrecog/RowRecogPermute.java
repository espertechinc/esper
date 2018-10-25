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
import com.espertech.esper.common.internal.collection.PermutationEnumeration;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import static org.junit.Assert.assertFalse;

public class RowRecogPermute implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        runPermute(env, false);
        runPermute(env, true);

        runDocSamples(env);

        runEquivalent(env, "mAtCh_Recognize_Permute(A)",
            "(A)");
        runEquivalent(env, "match_recognize_permute(A,B)",
            "(A B|B A)");
        runEquivalent(env, "match_recognize_permute(A,B,C)",
            "(A B C|A C B|B A C|B C A|C A B|C B A)");
        runEquivalent(env, "match_recognize_permute(A,B,C,D)",
            "(A B C D|A B D C|A C B D|A C D B|A D B C|A D C B|B A C D|B A D C|B C A D|B C D A|B D A C|B D C A|C A B D|C A D B|C B A D|C B D A|C D A B|C D B A|D A B C|D A C B|D B A C|D B C A|D C A B|D C B A)");

        runEquivalent(env, "match_recognize_permute((A B), C)",
            "((A B) C|C (A B))");
        runEquivalent(env, "match_recognize_permute((A|B), (C D), E)",
            "((A|B) (C D) E|(A|B) E (C D)|(C D) (A|B) E|(C D) E (A|B)|E (A|B) (C D)|E (C D) (A|B))");

        runEquivalent(env, "A match_recognize_permute(B,C) D",
            "A (B C|C B) D");

        runEquivalent(env, "match_recognize_permute(A, match_recognize_permute(B, C))",
            "(A (B C|C B)|(B C|C B) A)");
    }

    private void runDocSamples(RegressionEnvironment env) {
        runDocSampleUpToN(env);
    }

    private void runDocSampleUpToN(RegressionEnvironment env) {
        String[] fields = "a_id,b_id".split(",");
        String epl = "@name('s0') select * from TemperatureSensorEvent\n" +
            "match_recognize (\n" +
            "  partition by device\n" +
            "  measures A.id as a_id, B.id as b_id\n" +
            "  pattern (match_recognize_permute(A, B))\n" +
            "  define \n" +
            "\tA as A.temp < 100, \n" +
            "\tB as B.temp >= 100)";

        env.compileDeploy(epl).addListener("s0");

        env.sendEventObjectArray(new Object[]{"E1", 1, 99d}, "TemperatureSensorEvent");
        env.sendEventObjectArray(new Object[]{"E2", 1, 100d}, "TemperatureSensorEvent");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E2"});

        env.milestone(0);

        env.sendEventObjectArray(new Object[]{"E3", 1, 100d}, "TemperatureSensorEvent");
        env.sendEventObjectArray(new Object[]{"E4", 1, 99d}, "TemperatureSensorEvent");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4", "E3"});

        env.sendEventObjectArray(new Object[]{"E5", 1, 98d}, "TemperatureSensorEvent");
        assertFalse(env.listener("s0").isInvoked());

        env.undeployAll();
    }

    private static void runPermute(RegressionEnvironment env, boolean soda) {
        tryPermute(env, soda, "(A B C)|(A C B)|(B A C)|(B C A)|(C A B)|(C B A)");
        tryPermute(env, soda, "(match_recognize_permute(A,B,C))");
    }

    public static void tryPermute(RegressionEnvironment env, boolean soda, String pattern) {
        String epl = "@name('s0') select * from SupportBean " +
            "match_recognize (" +
            " partition by intPrimitive" +
            " measures A as a, B as b, C as c" +
            " pattern (" + pattern + ")" +
            " define" +
            " A as A.theString like \"A%\"," +
            " B as B.theString like \"B%\"," +
            " C as C.theString like \"C%\"" +
            ")";
        env.compileDeploy(soda, epl).addListener("s0");

        String[] prefixes = "A,B,C".split(",");
        String[] fields = "a,b,c".split(",");
        PermutationEnumeration e = new PermutationEnumeration(3);
        int count = 0;

        while (e.hasMoreElements()) {
            int[] indexes = e.nextElement();
            Object[] expected = new Object[3];
            for (int i = 0; i < 3; i++) {
                expected[indexes[i]] = sendEvent(env, prefixes[indexes[i]] + Integer.toString(count), count);
            }
            count++;

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);
        }

        env.undeployAll();
    }

    private static void runEquivalent(RegressionEnvironment env, String before, String after) {
        RowRecogRepetition.runEquivalent(env, before, after);
    }

    private static SupportBean sendEvent(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        env.sendEventBean(sb);
        return sb;
    }
}