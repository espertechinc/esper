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

import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPatternExpandUtil;
import com.espertech.esper.common.internal.epl.rowrecog.expr.RowRecogExprNode;
import com.espertech.esper.common.internal.epl.rowrecog.expr.RowRecogExprNodePrecedenceEnum;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.support.util.SupportStatementCompileHook;

import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class RowRecogRepetition {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new RowRecogRepetitionRepeats(false));
        execs.add(new RowRecogRepetitionRepeats(true));
        execs.add(new RowRecogRepetitionPrev());
        execs.add(new RowRecogRepetitionInvalid());
        execs.add(new RowRecogRepetitionDocSamples());
        execs.add(new RowRecogRepetitionEquivalent());
        return execs;
    }

    private static class RowRecogRepetitionDocSamples implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            runDocSampleExactlyN(env, milestone);
            runDocSampleNOrMore_and_BetweenNandM(env, "A{2,} B", milestone);
            runDocSampleNOrMore_and_BetweenNandM(env, "A{2,3} B", milestone);
            runDocSampleUpToN(env, milestone);
        }
    }

    private static void runDocSampleUpToN(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "a0_id,a1_id,b_id".split(",");
        String epl = "@name('s0') select * from TemperatureSensorEvent\n" +
            "match_recognize (\n" +
            "  partition by device\n" +
            "  measures A[0].id as a0_id, A[1].id as a1_id, B.id as b_id\n" +
            "  pattern (A{,2} B)\n" +
            "  define \n" +
            "\tA as A.temp >= 100,\n" +
            "\tB as B.temp >= 102)";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventObjectArray(new Object[]{"E1", 1, 99d}, "TemperatureSensorEvent");
        env.sendEventObjectArray(new Object[]{"E2", 1, 100d}, "TemperatureSensorEvent");
        env.sendEventObjectArray(new Object[]{"E3", 1, 100d}, "TemperatureSensorEvent");

        env.milestoneInc(milestone);

        env.sendEventObjectArray(new Object[]{"E4", 1, 101d}, "TemperatureSensorEvent");
        env.sendEventObjectArray(new Object[]{"E5", 1, 102d}, "TemperatureSensorEvent");
        env.assertPropsNew("s0", fields, new Object[]{"E3", "E4", "E5"});

        env.undeployAll();
    }

    private static void runDocSampleNOrMore_and_BetweenNandM(RegressionEnvironment env, String pattern, AtomicInteger milestone) {
        String[] fields = "a0_id,a1_id,a2_id,b_id".split(",");
        String epl = "@name('s0') select * from TemperatureSensorEvent\n" +
            "match_recognize (\n" +
            "  partition by device\n" +
            "  measures A[0].id as a0_id, A[1].id as a1_id, A[2].id as a2_id, B.id as b_id\n" +
            "  pattern (" + pattern + ")\n" +
            "  define \n" +
            "\tA as A.temp >= 100,\n" +
            "\tB as B.temp >= 102)";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventObjectArray(new Object[]{"E1", 1, 99d}, "TemperatureSensorEvent");

        env.milestoneInc(milestone);

        env.sendEventObjectArray(new Object[]{"E2", 1, 100d}, "TemperatureSensorEvent");
        env.sendEventObjectArray(new Object[]{"E3", 1, 100d}, "TemperatureSensorEvent");
        env.sendEventObjectArray(new Object[]{"E4", 1, 101d}, "TemperatureSensorEvent");
        env.sendEventObjectArray(new Object[]{"E5", 1, 102d}, "TemperatureSensorEvent");
        env.assertPropsNew("s0", fields, new Object[]{"E2", "E3", "E4", "E5"});

        env.undeployAll();
    }

    private static void runDocSampleExactlyN(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "a0_id,a1_id".split(",");
        String epl = "@name('s0') select * from TemperatureSensorEvent\n" +
            "match_recognize (\n" +
            "  partition by device\n" +
            "  measures A[0].id as a0_id, A[1].id as a1_id\n" +
            "  pattern (A{2})\n" +
            "  define \n" +
            "\tA as A.temp >= 100)";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventObjectArray(new Object[]{"E1", 1, 99d}, "TemperatureSensorEvent");
        env.sendEventObjectArray(new Object[]{"E2", 1, 100d}, "TemperatureSensorEvent");

        env.sendEventObjectArray(new Object[]{"E3", 1, 100d}, "TemperatureSensorEvent");
        env.assertPropsNew("s0", fields, new Object[]{"E2", "E3"});

        env.milestoneInc(milestone);

        env.sendEventObjectArray(new Object[]{"E4", 1, 101d}, "TemperatureSensorEvent");
        env.sendEventObjectArray(new Object[]{"E5", 1, 102d}, "TemperatureSensorEvent");
        env.assertPropsNew("s0", fields, new Object[]{"E4", "E5"});

        env.undeployAll();
    }

    private static class RowRecogRepetitionInvalid implements RegressionExecution {
        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }

        public void run(RegressionEnvironment env) {
            String template = "select * from SupportBean " +
                "match_recognize (" +
                "  measures A as a" +
                "  pattern (REPLACE) " +
                ")";
            env.compileDeploy("create variable int myvariable = 0");

            env.tryInvalidCompile(template.replaceAll("REPLACE", "A{}"),
                "Invalid match-recognize quantifier '{}', expecting an expression");
            env.tryInvalidCompile(template.replaceAll("REPLACE", "A{null}"),
                "Pattern quantifier 'null' must return an integer-type value");
            env.tryInvalidCompile(template.replaceAll("REPLACE", "A{myvariable}"),
                "Pattern quantifier 'myvariable' must return a constant value");
            env.tryInvalidCompile(template.replaceAll("REPLACE", "A{prev(A)}"),
                "Invalid match-recognize pattern expression 'prev(A)': Aggregation, sub-select, previous or prior functions are not supported in this context");

            String expected = "Invalid pattern quantifier value -1, expecting a minimum of 1";
            env.tryInvalidCompile(template.replaceAll("REPLACE", "A{-1}"), expected);
            env.tryInvalidCompile(template.replaceAll("REPLACE", "A{,-1}"), expected);
            env.tryInvalidCompile(template.replaceAll("REPLACE", "A{-1,10}"), expected);
            env.tryInvalidCompile(template.replaceAll("REPLACE", "A{-1,}"), expected);
            env.tryInvalidCompile(template.replaceAll("REPLACE", "A{5,3}"),
                "Invalid pattern quantifier value 5, expecting a minimum of 1 and maximum of 3");

            env.undeployAll();
        }
    }

    private static class RowRecogRepetitionPrev implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select * from SupportBean " +
                "match_recognize (" +
                "  measures A as a" +
                "  pattern (A{3}) " +
                "  define " +
                "    A as A.intPrimitive > prev(A.intPrimitive)" +
                ")";

            env.compileDeploy(text).addListener("s0");

            sendEvent("A1", 1, env);
            sendEvent("A2", 4, env);
            sendEvent("A3", 2, env);

            env.milestone(0);

            sendEvent("A4", 6, env);
            sendEvent("A5", 5, env);
            SupportBean b6 = sendEvent("A6", 6, env);
            SupportBean b7 = sendEvent("A7", 7, env);
            SupportBean b8 = sendEvent("A9", 8, env);
            env.assertPropsNew("s0", "a".split(","), new Object[]{new Object[]{b6, b7, b8}});

            env.undeployAll();
        }
    }

    private static class RowRecogRepetitionRepeats implements RegressionExecution {
        private final boolean soda;

        public RowRecogRepetitionRepeats(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            // Atom Assertions
            //
            //

            // single-bound assertions
            runAssertionRepeatSingleBound(env, soda);

            // defined-range assertions
            runAssertionsRepeatRange(env, soda);

            // lower-bounds assertions
            runAssertionsUpTo(env, soda);

            // upper-bounds assertions
            runAssertionsAtLeast(env, soda);

            // Nested Assertions
            //
            //

            // single-bound nested assertions
            runAssertionNestedRepeatSingle(env, soda);

            // defined-range nested assertions
            runAssertionNestedRepeatRange(env, soda);

            // lower-bounds nested assertions
            runAssertionsNestedUpTo(env, soda);

            // upper-bounds nested assertions
            runAssertionsNestedAtLeast(env, soda);
        }

        public String name() {
            return "RowRecogRepetitionRepeats{" +
                    "soda=" + soda +
                    '}';
        }
    }

    private static class RowRecogRepetitionEquivalent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            //
            // Single-bounds Repeat.
            //
            runEquivalent(env, "A{1}", "A");
            runEquivalent(env, "A{2}", "A A");
            runEquivalent(env, "A{3}", "A A A");
            runEquivalent(env, "A{1} B{2}", "A B B");
            runEquivalent(env, "A{1} B{2} C{3}", "A B B C C C");
            runEquivalent(env, "(A{2})", "(A A)");
            runEquivalent(env, "A?{2}", "A? A?");
            runEquivalent(env, "A*{2}", "A* A*");
            runEquivalent(env, "A+{2}", "A+ A+");
            runEquivalent(env, "A??{2}", "A?? A??");
            runEquivalent(env, "A*?{2}", "A*? A*?");
            runEquivalent(env, "A+?{2}", "A+? A+?");
            runEquivalent(env, "(A B){1}", "(A B)");
            runEquivalent(env, "(A B){2}", "(A B) (A B)");
            runEquivalent(env, "(A B)?{2}", "(A B)? (A B)?");
            runEquivalent(env, "(A B)*{2}", "(A B)* (A B)*");
            runEquivalent(env, "(A B)+{2}", "(A B)+ (A B)+");

            runEquivalent(env, "A B{2} C", "A B B C");
            runEquivalent(env, "A (B{2}) C", "A (B B) C");
            runEquivalent(env, "(A{2}) C", "(A A) C");
            runEquivalent(env, "A (B{2}|C{2})", "A (B B|C C)");
            runEquivalent(env, "A{2} B{2} C{2}", "A A B B C C");
            runEquivalent(env, "A{2} B C{2}", "A A B C C");
            runEquivalent(env, "A B{2} C{2}", "A B B C C");

            // range bounds
            runEquivalent(env, "A{1, 3}", "A A? A?");
            runEquivalent(env, "A{2, 4}", "A A A? A?");
            runEquivalent(env, "A?{1, 3}", "A? A? A?");
            runEquivalent(env, "A*{1, 3}", "A* A* A*");
            runEquivalent(env, "A+{1, 3}", "A+ A* A*");
            runEquivalent(env, "A??{1, 3}", "A?? A?? A??");
            runEquivalent(env, "A*?{1, 3}", "A*? A*? A*?");
            runEquivalent(env, "A+?{1, 3}", "A+? A*? A*?");
            runEquivalent(env, "(A B)?{1, 3}", "(A B)? (A B)? (A B)?");
            runEquivalent(env, "(A B)*{1, 3}", "(A B)* (A B)* (A B)*");
            runEquivalent(env, "(A B)+{1, 3}", "(A B)+ (A B)* (A B)*");

            // lower-only bounds
            runEquivalent(env, "A{2,}", "A A A*");
            runEquivalent(env, "A?{2,}", "A? A? A*");
            runEquivalent(env, "A*{2,}", "A* A* A*");
            runEquivalent(env, "A+{2,}", "A+ A+ A*");
            runEquivalent(env, "A??{2,}", "A?? A?? A*?");
            runEquivalent(env, "A*?{2,}", "A*? A*? A*?");
            runEquivalent(env, "A+?{2,}", "A+? A+? A*?");
            runEquivalent(env, "(A B)?{2,}", "(A B)? (A B)? (A B)*");
            runEquivalent(env, "(A B)*{2,}", "(A B)* (A B)* (A B)*");
            runEquivalent(env, "(A B)+{2,}", "(A B)+ (A B)+ (A B)*");

            // upper-only bounds
            runEquivalent(env, "A{,2}", "A? A?");
            runEquivalent(env, "A?{,2}", "A? A?");
            runEquivalent(env, "A*{,2}", "A* A*");
            runEquivalent(env, "A+{,2}", "A* A*");
            runEquivalent(env, "A??{,2}", "A?? A??");
            runEquivalent(env, "A*?{,2}", "A*? A*?");
            runEquivalent(env, "A+?{,2}", "A*? A*?");
            runEquivalent(env, "(A B){,2}", "(A B)? (A B)?");
            runEquivalent(env, "(A B)?{,2}", "(A B)? (A B)?");
            runEquivalent(env, "(A B)*{,2}", "(A B)* (A B)*");
            runEquivalent(env, "(A B)+{,2}", "(A B)* (A B)*");

            //
            // Nested Repeat.
            //
            runEquivalent(env, "(A B){2}", "(A B) (A B)");
            runEquivalent(env, "(A){2}", "A A");
            runEquivalent(env, "(A B C){3}", "(A B C) (A B C) (A B C)");
            runEquivalent(env, "(A B){2} (C D){2}", "(A B) (A B) (C D) (C D)");
            runEquivalent(env, "((A B){2} C){2}", "((A B) (A B) C) ((A B) (A B) C)");
            runEquivalent(env, "((A|B){2} (C|D){2}){2}", "((A|B) (A|B) (C|D) (C|D)) ((A|B) (A|B) (C|D) (C|D))");
        }
    }

    private static void runAssertionNestedRepeatSingle(RegressionEnvironment env, boolean soda) {
        runTwiceAB(env, soda, "(A B) (A B)");
        runTwiceAB(env, soda, "(A B){2}");

        runAThenTwiceBC(env, soda, "A (B C) (B C)");
        runAThenTwiceBC(env, soda, "A (B C){2}");
    }

    private static void runAssertionNestedRepeatRange(RegressionEnvironment env, boolean soda) {
        runOnceOrTwiceABThenC(env, soda, "(A B) (A B)? C");
        runOnceOrTwiceABThenC(env, soda, "(A B){1,2} C");
    }

    private static void runAssertionsAtLeast(RegressionEnvironment env, boolean soda) {
        runAtLeast2AThenB(env, soda, "A A A* B");
        runAtLeast2AThenB(env, soda, "A{2,} B");
        runAtLeast2AThenB(env, soda, "A{2,4} B");
    }

    private static void runAssertionsUpTo(RegressionEnvironment env, boolean soda) {
        runUpTo2AThenB(env, soda, "A? A? B");
        runUpTo2AThenB(env, soda, "A{,2} B");
    }

    private static void runAssertionsRepeatRange(RegressionEnvironment env, boolean soda) {
        run2To3AThenB(env, soda, "A A A? B");
        run2To3AThenB(env, soda, "A{2,3} B");
    }

    private static void runAssertionsNestedUpTo(RegressionEnvironment env, boolean soda) {
        runUpTo2ABThenC(env, soda, "(A B)? (A B)? C");
        runUpTo2ABThenC(env, soda, "(A B){,2} C");
    }

    private static void runAssertionsNestedAtLeast(RegressionEnvironment env, boolean soda) {
        runAtLeast2ABThenC(env, soda, "(A B) (A B) (A B)* C");
        runAtLeast2ABThenC(env, soda, "(A B){2,} C");
    }

    private static void runAssertionRepeatSingleBound(RegressionEnvironment env, boolean soda) {
        runExactly2A(env, soda, "A A");
        runExactly2A(env, soda, "A{2}");
        runExactly2A(env, soda, "(A{2})");

        // concatenation
        runAThen2BThenC(env, soda, "A B B C");
        runAThen2BThenC(env, soda, "A B{2} C");

        // nested
        runAThen2BThenC(env, false, "A (B B) C");
        runAThen2BThenC(env, false, "A (B{2}) C");

        // alteration
        runAThen2BOr2C(env, soda, "A (B B|C C)");
        runAThen2BOr2C(env, soda, "A (B{2}|C{2})");

        // multiple
        run2AThen2B(env, soda, "A A B B");
        run2AThen2B(env, soda, "A{2} B{2}");
    }

    private static void runAtLeast2ABThenC(RegressionEnvironment env, boolean soda, String pattern) {
        runAssertion(env, soda, pattern, "a,b,c", new boolean[]{true, true, false}, new String[]{
            "A1,B1,A2,B2,C1",
            "A1,B1,A2,B2,A3,B3,C1"
        }, new String[]{"A1,B1,C1", "A1,B1,A2,C1", "B1,A1,B2,C1"});
    }

    private static void runOnceOrTwiceABThenC(RegressionEnvironment env, boolean soda, String pattern) {
        runAssertion(env, soda, pattern, "a,b,c", new boolean[]{true, true, false}, new String[]{
            "A1,B1,C1",
            "A1,B1,A2,B2,C1"
        }, new String[]{"C1", "A1,A2,C2", "B1,A1,C1"});
    }

    private static void runAtLeast2AThenB(RegressionEnvironment env, boolean soda, String pattern) {
        runAssertion(env, soda, pattern, "a,b", new boolean[]{true, false}, new String[]{
            "A1,A2,B1",
            "A1,A2,A3,B1"
        }, new String[]{"A1,B1", "B1"});
    }

    private static void runUpTo2AThenB(RegressionEnvironment env, boolean soda, String pattern) {
        runAssertion(env, soda, pattern, "a,b", new boolean[]{true, false}, new String[]{
            "B1",
            "A1,B1",
            "A1,A2,B1"
        }, new String[]{"A1"});
    }

    private static void run2AThen2B(RegressionEnvironment env, boolean soda, String pattern) {
        runAssertion(env, soda, pattern, "a,b", new boolean[]{true, true}, new String[]{
            "A1,A2,B1,B2",
        }, new String[]{"A1,A2,B1", "B1,B2,A1,A2", "A1,B1,A2,B2"});
    }

    private static void runUpTo2ABThenC(RegressionEnvironment env, boolean soda, String pattern) {
        runAssertion(env, soda, pattern, "a,b,c", new boolean[]{true, true, false}, new String[]{
            "C1",
            "A1,B1,C1",
            "A1,B1,A2,B2,C1",
        }, new String[]{"A1,B1,A2,B2", "A1,A2"});
    }

    private static void run2To3AThenB(RegressionEnvironment env, boolean soda, String pattern) {
        runAssertion(env, soda, pattern, "a,b", new boolean[]{true, false}, new String[]{
            "A1,A2,A3,B1",
            "A1,A2,B1",
        }, new String[]{"A1,B1", "A1,A2", "B1"});
    }

    private static void runAThen2BOr2C(RegressionEnvironment env, boolean soda, String pattern) {
        runAssertion(env, soda, pattern, "a,b,c", new boolean[]{false, true, true}, new String[]{
            "A1,C1,C2",
            "A2,B1,B2",
        }, new String[]{"B1,B2", "C1,C2", "A1,B1,C1", "A1,C1,B1"});
    }

    private static void runTwiceAB(RegressionEnvironment env, boolean soda, String pattern) {
        runAssertion(env, soda, pattern, "a,b", new boolean[]{true, true}, new String[]{
            "A1,B1,A2,B2",
        }, new String[]{"A1,A2,B1", "A1,A2,B1,B2", "A1,B1,B2,A2"});
    }

    private static void runAThenTwiceBC(RegressionEnvironment env, boolean soda, String pattern) {
        runAssertion(env, soda, pattern, "a,b,c", new boolean[]{false, true, true}, new String[]{
            "A1,B1,C1,B2,C2",
        }, new String[]{"A1,B1,C1,B2", "A1,B1,C1,C2", "A1,B1,B2,C1,C2"});
    }

    private static void runAThen2BThenC(RegressionEnvironment env, boolean soda, String pattern) {
        runAssertion(env, soda, pattern, "a,b,c", new boolean[]{false, true, false}, new String[]{
            "A1,B1,B2,C1",
        }, new String[]{"B1,B2,C1", "A1,B1,C1", "A1,B1,B2"});
    }

    private static void runExactly2A(RegressionEnvironment env, boolean soda, String pattern) {
        runAssertion(env, soda, pattern, "a", new boolean[]{true}, new String[]{
            "A1,A2",
            "A3,A4",
        }, new String[]{"A5"});
    }

    private static void runAssertion(RegressionEnvironment env, boolean soda, String pattern, String propertyNames, boolean[] arrayProp,
                              String[] sequencesWithMatch,
                              String[] sequencesNoMatch) {
        String[] props = propertyNames.split(",");
        String measures = makeMeasures(props);
        String defines = makeDefines(props);

        String text = "@name('s0') select * from SupportBean " +
            "match_recognize (" +
            " partition by intPrimitive" +
            " measures " + measures +
            " pattern (" + pattern + ")" +
            " define " + defines +
            ")";
        env.compileDeploy(soda, text).addListener("s0");

        int sequenceNum = 0;
        for (String aSequencesWithMatch : sequencesWithMatch) {
            runAssertionSequence(env, true, props, arrayProp, sequenceNum, aSequencesWithMatch);
            sequenceNum++;
        }

        for (String aSequencesNoMatch : sequencesNoMatch) {
            runAssertionSequence(env, false, props, arrayProp, sequenceNum, aSequencesNoMatch);
            sequenceNum++;
        }

        env.undeployAll();
    }

    private static void runAssertionSequence(RegressionEnvironment env, boolean match, String[] propertyNames, boolean[] arrayProp, int sequenceNum, String sequence) {

        // send events
        String[] events = sequence.split(",");
        Map<String, List<SupportBean>> sent = new HashMap<>();
        for (String anEvent : events) {
            String type = new String(new char[]{anEvent.charAt(0)});
            SupportBean bean = sendEvent(anEvent, sequenceNum, env);
            String propName = type.toLowerCase(Locale.ENGLISH);
            if (!sent.containsKey(propName)) {
                sent.put(propName, new ArrayList<>());
            }
            sent.get(propName).add(bean);
        }

        // prepare expected
        Object[] expected = new Object[propertyNames.length];
        for (int i = 0; i < propertyNames.length; i++) {
            List<SupportBean> sentForType = sent.get(propertyNames[i]);
            if (arrayProp[i]) {
                expected[i] = sentForType == null ? null : sentForType.toArray(new SupportBean[0]);
            } else {
                if (match) {
                    assertTrue(sentForType.size() == 1);
                    expected[i] = sentForType.get(0);
                }
            }
        }

        if (match) {
            env.assertPropsNew("s0", propertyNames, expected);
        } else {
            env.assertListenerNotInvoked("s0");
        }
    }

    private static String makeDefines(String[] props) {
        String delimiter = "";
        StringWriter buf = new StringWriter();
        for (String prop : props) {
            buf.append(delimiter);
            delimiter = ", ";
            buf.append(prop.toUpperCase(Locale.ENGLISH));
            buf.append(" as ");
            buf.append(prop.toUpperCase(Locale.ENGLISH));
            buf.append(".theString like \"");
            buf.append(prop.toUpperCase(Locale.ENGLISH));
            buf.append("%\"");
        }
        return buf.toString();
    }

    private static String makeMeasures(String[] props) {
        String delimiter = "";
        StringWriter buf = new StringWriter();
        for (String prop : props) {
            buf.append(delimiter);
            delimiter = ", ";
            buf.append(prop.toUpperCase(Locale.ENGLISH));
            buf.append(" as ");
            buf.append(prop);
        }
        return buf.toString();
    }

    private static SupportBean sendEvent(String theString, int intPrimitive, RegressionEnvironment env) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        env.sendEventBean(sb);
        return sb;
    }

    protected static void runEquivalent(RegressionEnvironment env, String before, String after) {
        String hook = "@Hook(type=" + HookType.class.getName() + ".INTERNAL_COMPILE,hook='" + SupportStatementCompileHook.resetGetClassName() + "')";
        String epl = hook + "@name('s0') select * from SupportBean#keepall " +
            "match_recognize (" +
            " measures A as a" +
            " pattern (" + before + ")" +
            " define" +
            " A as A.theString like \"A%\"" +
            ")";

        EPStatementObjectModel model = env.eplToModel(epl);
        env.compileDeploy(model);
        env.undeployAll();

        env.assertThat(() -> {
            StatementSpecCompiled spec = SupportStatementCompileHook.getSpecs().get(0);
            RowRecogExprNode expanded = null;
            try {
                expanded = RowRecogPatternExpandUtil.expand(spec.getRaw().getMatchRecognizeSpec().getPattern(), null);
            } catch (ExprValidationException e) {
                fail(e.getMessage());
            }
            StringWriter writer = new StringWriter();
            expanded.toEPL(writer, RowRecogExprNodePrecedenceEnum.MINIMUM);
            assertEquals(after, writer.toString());
        });
    }
}