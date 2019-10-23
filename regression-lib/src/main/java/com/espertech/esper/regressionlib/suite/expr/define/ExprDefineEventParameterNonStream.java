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
package com.espertech.esper.regressionlib.suite.expr.define;

import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ExprDefineEventParameterNonStream {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprDefineEventParamPatternPOJO());
        execs.add(new ExprDefineEventParamPatternMap());
        execs.add(new ExprDefineEventParamContextProperty());
        execs.add(new ExprDefineEventParamSubqueryPOJO());
        execs.add(new ExprDefineEventParamSubqueryMap());
        execs.add(new ExprDefineEventParamSubqueryMapWithWhere());
        return execs;
    }

    private static class ExprDefineEventParamSubqueryPOJO implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') expression combineProperties {v -> v.p00 || v.p01} select combineProperties((select * from SupportBean_S0#keepall)) as c0 from SupportBean_S1 as p";
            env.compileDeploy(epl).addListener("s0");

            sendAssertS1(env, null);

            sendS0(env, "a", "b");
            sendAssertS1(env, "ab");

            sendS0(env, "d", "e");
            sendAssertS1(env, null); // since subquery returns two rows

            env.undeployAll();
        }
    }

    private static class ExprDefineEventParamSubqueryMap implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@public @buseventtype create schema EventOne(p0 string, p1 string);\n" +
                    "@public @buseventtype create schema EventTwo();\n" +
                    "@name('s0') expression combineProperties {v -> v.p0 || v.p1} select combineProperties((select * from EventOne#lastevent)) as c0 from EventTwo as p";
            env.compileDeploy(epl).addListener("s0");

            sendAssertEventTwo(env, null);

            sendEventOne(env, "a", "b");
            sendAssertEventTwo(env, "ab");

            sendEventOne(env, "c", "d");
            sendAssertEventTwo(env, "cd");

            env.undeployAll();
        }
    }

    private static class ExprDefineEventParamSubqueryMapWithWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@public @buseventtype create schema EventOne(p0 string, p1 string);\n" +
                    "@public @buseventtype create schema EventTwo();\n" +
                    "@name('s0') expression combineProperties {v -> v.p0 || v.p1} select combineProperties((select * from EventOne#keepall where p0='a')) as c0 from EventTwo as p";
            env.compileDeploy(epl).addListener("s0");

            sendAssertEventTwo(env, null);

            sendEventOne(env, "c", "d");
            sendAssertEventTwo(env, null);

            sendEventOne(env, "a", "d");
            sendAssertEventTwo(env, "ad");

            sendEventOne(env, "a", "e");
            sendAssertEventTwo(env, null);

            env.undeployAll();
        }
    }

    private static class ExprDefineEventParamContextProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                    "@public @buseventtype create schema EventOne(p0 string, p1 string);\n" +
                    "@public @buseventtype create schema EventTwo();\n" +
                    "create context PerEventOne initiated by EventOne e1;\n" +
                    "@name('s0') expression combineProperties {v -> v.p0 || v.p1} \n" +
                    "context PerEventOne select combineProperties(context.e1) as c0 from EventTwo;\n";
            env.compileDeploy(epl).addListener("s0");

            sendEventOne(env, "a", "b");
            sendAssertEventTwo(env, "ab");

            env.undeployAll();
        }
    }

    private static class ExprDefineEventParamPatternMap implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@public @buseventtype create schema EventOne(p0 string, p1 string);\n" +
                "@public @buseventtype create schema EventTwo();\n" +
                "@name('s0') expression combineProperties {v -> v.p0 || v.p1} select combineProperties(p.a) as c0 from pattern [a=EventOne -> EventTwo] as p";
            env.compileDeploy(epl).addListener("s0");

            sendEventOne(env, "a", "b");
            sendAssertEventTwo(env, "ab");

            env.undeployAll();
        }
    }

    private static class ExprDefineEventParamPatternPOJO implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') expression combineProperties {v -> v.p00 || v.p01} select combineProperties(p.a) as c0 from pattern [a=SupportBean_S0 -> SupportBean_S1] as p";
            env.compileDeploy(epl).addListener("s0");

            sendS0(env, "a", "b");
            sendAssertS1(env, "ab");

            env.undeployAll();
        }
    }

    private static void sendEventOne(RegressionEnvironment env, String p0, String p1) {
        env.sendEventMap(CollectionUtil.buildMap("p0", p0, "p1", p1), "EventOne");
    }

    private static void sendAssertEventTwo(RegressionEnvironment env, Object expected) {
        env.sendEventMap(Collections.emptyMap(), "EventTwo");
        assertReceived(env, expected);
    }

    private static void sendS0(RegressionEnvironment env, String p00, String p01) {
        env.sendEventBean(new SupportBean_S0(0, p00, p01));
    }

    private static void sendAssertS1(RegressionEnvironment env, Object expected) {
        env.sendEventBean(new SupportBean_S1(0));
        assertReceived(env, expected);
    }

    private static void assertReceived(RegressionEnvironment env, Object expected) {
        assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
    }
}
