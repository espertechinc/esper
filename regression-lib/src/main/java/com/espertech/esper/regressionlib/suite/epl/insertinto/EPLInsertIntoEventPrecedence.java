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
package com.espertech.esper.regressionlib.suite.epl.insertinto;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanNumeric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class EPLInsertIntoEventPrecedence {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoEventPrecConstantInfraMergeInsertInto(false));
        execs.add(new EPLInsertIntoEventPrecConstantInfraMergeInsertInto(true));
        execs.add(new EPLInsertIntoEventPrecConstantInsertInto());
        execs.add(new EPLInsertIntoEventPrecConstantOnSplit());
        execs.add(new EPLInsertIntoEventPrecNonConstInsertIntoContainedEvent());
        execs.add(new EPLInsertIntoEventPrecSubqueryOnSplitSODA());
        execs.add(new EPLInsertIntoEventPrecSubqueryInsertIntoSODA());
        execs.add(new EPLInsertIntoEventPrecSubqueryMergeSODA());
        execs.add(new EPLInsertIntoEventPrecSubqueryOnInsertSODA());
        execs.add(new EPLInsertIntoEventPrecConstantInsertIntoOutputRate());
        execs.add(new EPLInsertIntoEventPrecInvalid());
        return execs;
    }

    public static List<RegressionExecution> executionsNoLatch() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoEventPrecConstantInsertInto());
        execs.add(new EPLInsertIntoEventPrecConstantInfraMergeInsertInto(false));
        execs.add(new EPLInsertIntoEventPrecConstantOnSplit());
        execs.add(new EPLInsertIntoEventPrecNonConstInsertIntoContainedEvent());
        return execs;
    }

    public static class EPLInsertIntoEventPrecNonConstInsertIntoContainedEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                    "@public @buseventtype create schema LvlA as " + LvlA.class.getName() + ";\n" +
                            "insert into LvlB event-precedence(precedence) select * from LvlA[b];\n" +
                            "insert into LvlC event-precedence(precedence) select * from LvlB[c];\n" +
                            "@name('s0') select id from LvlC;\n";
            env.compileDeploy(epl).addListener("s0");

            // precedenced
            LvlC[] c1 = new LvlC[]{new LvlC("A", 0), new LvlC("B", 1)};
            LvlC[] c2 = new LvlC[]{new LvlC("C", 1), new LvlC("D", 0)};
            LvlA a0 = new LvlA(new LvlB(10, c1), new LvlB(11, c2));
            sendAssert(env, a0, "C,B,D,A");

            // same-precedence with one raised
            LvlC[] c11 = new LvlC[]{new LvlC("A", 0), new LvlC("B", 0)};
            LvlC[] c12 = new LvlC[]{new LvlC("C", 1), new LvlC("D", 0)};
            LvlA a1 = new LvlA(new LvlB(10, c11), new LvlB(10, c12));
            sendAssert(env, a1, "C,A,B,D");

            // no precedences
            LvlC[] c21 = new LvlC[]{new LvlC("A", 0), new LvlC("B", 0)};
            LvlC[] c22 = new LvlC[]{new LvlC("C", 0), new LvlC("D", 0)};
            LvlA a2 = new LvlA(new LvlB(0, c21), new LvlB(0, c22));
            sendAssert(env, a2, "A,B,C,D");

            // three precedence levels
            LvlC[] c31 = new LvlC[]{new LvlC("A", 2), new LvlC("B", 1), new LvlC("C", 0)};
            LvlC[] c32 = new LvlC[]{new LvlC("D", 2), new LvlC("E", 0), new LvlC("F", 1)};
            LvlC[] c33 = new LvlC[]{new LvlC("G", 1), new LvlC("H", 2), new LvlC("I", 0)};
            LvlA a3 = new LvlA(new LvlB(100, c31), new LvlB(101, c32), new LvlB(103, c33));
            sendAssert(env, a3, "H,D,A,G,F,B,I,E,C");

            // two precedence levels
            LvlC[] c41 = new LvlC[]{new LvlC("A", 0), new LvlC("B", 1), new LvlC("C", 0)};
            LvlC[] c42 = new LvlC[]{new LvlC("D", 1), new LvlC("E", 0), new LvlC("F", 1)};
            LvlC[] c43 = new LvlC[]{new LvlC("G", 1), new LvlC("H", 1), new LvlC("I", 0)};
            LvlA c4 = new LvlA(new LvlB(103, c41), new LvlB(100, c42), new LvlB(102, c43));
            sendAssert(env, c4, "B,G,H,D,F,A,C,I,E");

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, LvlA one, String s) {
            env.sendEventBean(one);
            String[] split = s.split(",");
            Object[][] expected = new Object[split.length][];
            for (int i = 0; i < split.length; i++) {
                expected[i] = new Object[]{split[i]};
            }
            env.assertPropsPerRowNewFlattened("s0", new String[]{"id"}, expected);
        }
    }

    public static class EPLInsertIntoEventPrecConstantOnSplit implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "on SupportBean \n" +
                    "insert into Out event-precedence(1) select 1 as id\n" +
                    "insert into Out event-precedence(2) select 2 as id\n" +
                    "insert into Out event-precedence(3) select 3 as id\n" +
                    "output all;\n" +
                    "@name('s0') select * from Out ";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean());
            env.assertPropsPerRowNewFlattened("s0", new String[]{"id"}, new Object[][]{{3}, {2}, {1}});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoEventPrecConstantInsertInto implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('window') create window MyWindow#keepall as (id int);\n" +
                    "insert into MyWindow event-precedence(4) select 4 as id from SupportBean;\n" +
                    "insert into MyWindow event-precedence(2) select 2 as id from SupportBean;\n" +
                    "insert into MyWindow select 0 as id from SupportBean;\n" +
                    "insert into MyWindow event-precedence(5) select 5 as id from SupportBean;\n" +
                    "insert into MyWindow event-precedence(1) select 1 as id from SupportBean;\n" +
                    "insert into MyWindow event-precedence(3) select 3 as id from SupportBean;\n";
            env.compileDeploy(epl);

            env.sendEventBean(new SupportBean());
            env.assertPropsPerRowIterator("window", new String[]{"id"}, new Object[][]{{5}, {4}, {3}, {2}, {1}, {0}});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoEventPrecSubqueryInsertIntoSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create schema Out (id string)", path);
            env.compileDeploy(true, "insert into Out event-precedence((select intOne from SupportBeanNumeric#lastevent)) select \"a\" as id from SupportBean", path);
            env.compileDeploy(true, "insert into Out event-precedence((select intTwo from SupportBeanNumeric#lastevent)) select \"b\" as id from SupportBean", path);
            env.compileDeploy("@name('s0') select * from Out", path).addListener("s0");

            sendSBAssert(env, "a", "b");

            env.sendEventBean(new SupportBeanNumeric(-2, -1));
            sendSBAssert(env, "b", "a");

            env.undeployAll();
        }

        private void sendSBAssert(RegressionEnvironment env, String first, String second) {
            env.sendEventBean(new SupportBean());
            env.assertPropsPerRowNewFlattened("s0", new String[]{"id"}, new Object[][]{{first}, {second}});
        }
    }

    private static class EPLInsertIntoEventPrecConstantInfraMergeInsertInto implements RegressionExecution {

        private final boolean namedWindow;

        public EPLInsertIntoEventPrecConstantInfraMergeInsertInto(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String epl;
            if (namedWindow) {
                epl = "create window InfraMerge#keepall as (mergeid string);\n";
            } else {
                epl = "create table InfraMerge(mergeid string primary key);\n";
            }
            epl += "@name('WindowOut') create window WindowOut#keepall as (outid string);\n" +
                    "on SupportBean sb merge InfraMerge mw where sb.theString = mw.mergeid\n" +
                    "when not matched\n" +
                    "then insert into WindowOut event-precedence(0) select 'a' as outid\n" +
                    "then insert into WindowOut select 'b' as outid\n" +
                    "then insert into WindowOut event-precedence(1) select 'c' as outid\n" +
                    ";\n";
            env.compileDeploy(epl);

            env.sendEventBean(new SupportBean());
            env.assertPropsPerRowIterator("WindowOut", new String[]{"outid"}, new Object[][]{{"c"}, {"a"}, {"b"}});

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                    "namedWindow=" + namedWindow +
                    '}';
        }
    }

    private static class EPLInsertIntoEventPrecSubqueryOnSplitSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create schema Out (id string)", path);
            String epl = "on SupportBean " +
                    "insert into Out event-precedence((select intOne from SupportBeanNumeric#lastevent)) select \"a\" as id " +
                    "insert into Out event-precedence((select intTwo from SupportBeanNumeric#lastevent)) select \"b\" as id " +
                    "output all";
            env.compileDeploy(true, epl, path);
            env.compileDeploy("@name('s0') select * from Out", path).addListener("s0");

            sendSBAssert(env, "a", "b");

            env.sendEventBean(new SupportBeanNumeric(1, 2));
            sendSBAssert(env, "b", "a");

            env.sendEventBean(new SupportBeanNumeric(2, 1));
            sendSBAssert(env, "a", "b");

            env.undeployAll();
        }

        private void sendSBAssert(RegressionEnvironment env, String first, String second) {
            env.sendEventBean(new SupportBean());
            env.assertPropsPerRowNewFlattened("s0", new String[]{"id"}, new Object[][]{{first}, {second}});
        }
    }

    private static class EPLInsertIntoEventPrecSubqueryMergeSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy(
                    "@public create window MyWindow#keepall as (id string);\n" +
                            "@public create schema Out (id string);\n", path);
            String epl = "on SupportBean merge MyWindow where theString=id " +
                    "when not matched " +
                    "then insert into Out event-precedence((select intOne from SupportBeanNumeric#lastevent)) select \"a\" as id " +
                    "then insert into Out event-precedence((select intTwo from SupportBeanNumeric#lastevent)) select \"b\" as id";
            env.compileDeploy(true, epl, path);
            env.compileDeploy("@name('s0') select * from Out", path).addListener("s0");

            sendSBAssert(env, "a", "b");

            env.sendEventBean(new SupportBeanNumeric(1, 2));
            sendSBAssert(env, "b", "a");

            env.sendEventBean(new SupportBeanNumeric(2, 1));
            sendSBAssert(env, "a", "b");

            env.undeployAll();
        }

        private void sendSBAssert(RegressionEnvironment env, String first, String second) {
            env.sendEventBean(new SupportBean());
            env.assertPropsPerRowNewFlattened("s0", new String[]{"id"}, new Object[][]{{first}, {second}});
        }
    }

    private static class EPLInsertIntoEventPrecSubqueryOnInsertSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create schema Out (id string);\n" +
                    "@public create window MyWindow#keepall as (value string);\n", path);
            env.compileExecuteFAF("insert into MyWindow select 'x' as value", path);

            String eplOne = "on SupportBean insert into Out event-precedence((select intOne from SupportBeanNumeric#lastevent)) select \"a\" as id from MyWindow";
            env.compileDeploy(true, eplOne, path);

            String eplTwo = "on SupportBean insert into Out event-precedence((select intTwo from SupportBeanNumeric#lastevent)) select \"b\" as id from MyWindow;\n" +
                    "@name('s0') select * from Out;\n";
            env.compileDeploy(false, eplTwo, path).addListener("s0");

            sendSBAssert(env, "a", "b");

            env.sendEventBean(new SupportBeanNumeric(1, 2));
            sendSBAssert(env, "b", "a");

            env.sendEventBean(new SupportBeanNumeric(2, 1));
            sendSBAssert(env, "a", "b");

            env.undeployAll();
        }

        private void sendSBAssert(RegressionEnvironment env, String first, String second) {
            env.sendEventBean(new SupportBean());
            env.assertPropsPerRowNewFlattened("s0", new String[]{"id"}, new Object[][]{{first}, {second}});
        }
    }

    private static class EPLInsertIntoEventPrecConstantInsertIntoOutputRate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                    "create schema Out(id int);\n" +
                            "insert into Out event-precedence(1) select 1 + intPrimitive * 10 as id from SupportBean output every 2 events;\n" +
                            "insert into Out event-precedence(2) select 2 + intPrimitive * 10 as id from SupportBean output every 2 events;\n" +
                            "insert into Out event-precedence(" + EPLInsertIntoEventPrecedence.class.getName() + ".computeEventPrecedence(3, *)) select 3 + intPrimitive * 10 as id from SupportBean output every 2 events;\n" +
                            "@name('s0') select * from Out;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            env.assertPropsPerRowNewFlattened("s0", new String[]{"id"}, new Object[][]{{13}, {23}, {12}, {22}, {11}, {21}});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoEventPrecInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy(
                    "@public create window MyWindow#keepall as (id string);\n" +
                            "@public create table MyTable(id string primary key);\n", path);

            env.tryInvalidCompile(path, "insert into Out event-precedence('a') select * from SupportBean",
                    "Event-precedence expected an expression returning an integer value but the expression '\"a\"' returns String");

            env.tryInvalidCompile(path, "insert into Out event-precedence(intPrimitive) select theString from SupportBean",
                    "Failed to validate event-precedence considering only the output event type 'Out': Failed to validate event-precedence clause expression 'intPrimitive': Property named 'intPrimitive' is not valid in any stream (NOTE: this validation only considers the result event itself and not incoming streams)");

            env.tryInvalidCompile(path, "on SupportBean insert into Out event-precedence(null) select 'a' as id",
                    "Event-precedence expected an expression returning an integer value but the expression 'null' returns null");

            env.tryInvalidCompile(path, "on SupportBean merge MyWindow when not matched then insert into Out event-precedence(cast(1, short)) select 'a' as id",
                    "Validation failed in when-not-matched (clause 1): Event-precedence expected an expression returning an integer value but the expression 'cast(1,short)' returns Short");

            env.tryInvalidCompileFAF(path, "insert into MyWindow(id) event-precedence(10) values ('a')",
                    "Fire-and-forget insert-queries do not allow event-precedence");

            env.tryInvalidCompile(path, "insert into MyTable event-precedence(1) select 'a' as id from SupportBean",
                    "Event-precedence is not allowed when inserting into a table");

            env.tryInvalidCompile(path, "on SupportBean merge MyWindow insert event-precedence(1) select 'a' as id",
                    "Incorrect syntax near 'event-precedence' (a reserved keyword)");

            env.undeployAll();
        }
    }

    /**
     * Sample Performance Test
     */
    /*
    public static class EPLInsertIntoPerformance implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                    "insert into MyStream1 select * from SupportBean;\n" +
                    "insert into MyStream2 select * from MyStream1;\n" +
                    "insert into MyStream3 select * from MyStream2;\n" +
                    "insert into MyStream4 select * from MyStream3;\n" +
                    "insert into MyStream5 select * from MyStream4;\n" +
                    "insert into MyStream6 select * from MyStream5;\n" +
                    "insert into MyStream7 select * from MyStream6;\n" +
                    "insert into MyStream8 select * from MyStream7;\n" +
                            "@name('s0') select count(*) as cnt from MyStream8;\n";
            env.compileDeploy(epl);
            int count = 10000000;

            long start = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                env.sendEventBean(new SupportBean());
            }
            long end = System.currentTimeMillis();
            env.assertPropsPerRowIterator("s0", new String[] {"cnt"}, new Object[][] {{count * 1L}});
            System.out.println((end-start) / 1000d);
        }
    }
     */

    public static int computeEventPrecedence(int value, Object param) {
        assertTrue(param instanceof HashMap);
        return value;
    }

    public static class LvlA implements Serializable {
        private final LvlB[] b;

        public LvlA(LvlB... b) {
            this.b = b;
        }

        public LvlB[] getB() {
            return b;
        }
    }

    public static class LvlB implements Serializable {
        private final int precedence;
        private final LvlC[] c;

        public LvlB(int precedence, LvlC[] c) {
            this.precedence = precedence;
            this.c = c;
        }

        public int getPrecedence() {
            return precedence;
        }

        public LvlC[] getC() {
            return c;
        }
    }

    public static class LvlC implements Serializable {
        private final String id;
        private final int precedence;

        public LvlC(String id, int precedence) {
            this.id = id;
            this.precedence = precedence;
        }

        public String getId() {
            return id;
        }

        public int getPrecedence() {
            return precedence;
        }
    }
}
