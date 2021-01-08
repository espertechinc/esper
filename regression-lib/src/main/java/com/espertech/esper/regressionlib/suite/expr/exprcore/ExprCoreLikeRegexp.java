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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ExprCoreLikeRegexp {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreLikeWConstants());
        executions.add(new ExprCoreLikeWExprs());
        executions.add(new ExprCoreRegexpWConstants());
        executions.add(new ExprCoreRegexpWExprs());
        executions.add(new ExprCoreLikeRegexStringAndNull());
        executions.add(new ExprCoreLikeRegExInvalid());
        executions.add(new ExprCoreLikeRegexEscapedChar());
        executions.add(new ExprCoreLikeRegexStringAndNullOM());
        executions.add(new ExprCoreRegexStringAndNullCompile());
        executions.add(new ExprCoreLikeRegexNumericAndNull());
        return executions;
    }

    private static class ExprCoreLikeWConstants implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expressions(fields, "theString like 'A%'", "intPrimitive like '1%'");
            builder.assertion(new SupportBean("Bxx", 0)).expect(fields, false, false);
            builder.assertion(new SupportBean("Ayyy", 100)).expect(fields, true, true);
            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreLikeWExprs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_S0")
                .expressions(fields, "p00 like p01", "id like p02");

            builder.assertion(new SupportBean_S0(413, "%XXaXX", "%a%", "%1%", null)).expect(fields, true, true);
            builder.assertion(new SupportBean_S0(413, "%XXcXX", "%b%", "%2%", null)).expect(fields, false, false);
            builder.assertion(new SupportBean_S0(413, "%XXcXX", "%c%", "%3%", null)).expect(fields, true, true);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreRegexpWConstants implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expressions(fields, "theString regexp '.*Jack.*'", "intPrimitive regexp '.*1.*'");

            builder.assertion(new SupportBean("Joe", 0)).expect(fields, false, false);
            builder.assertion(new SupportBean("TheJackWhite", 100)).expect(fields, true, true);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreRegexpWExprs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_S0")
                .expressions(fields, "p00 regexp p01", "id regexp p02");

            builder.assertion(new SupportBean_S0(413, "XXAXX", ".*A.*", ".*1.*", null)).expect(fields, true, true);
            builder.assertion(new SupportBean_S0(413, "XXaXX", ".*B.*", ".*2.*", null)).expect(fields, false, false);
            builder.assertion(new SupportBean_S0(413, "XXCXX", ".*C.*", ".*3.*", null)).expect(fields, true, true);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreLikeRegexStringAndNull implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select p00 like p01 as r1, " +
                " p00 like p01 escape \"!\" as r2," +
                " p02 regexp p03 as r3 " +
                " from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            runLikeRegexStringAndNull(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreLikeRegExInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidExpr(env, "intPrimitive like 'a' escape null");
            tryInvalidExpr(env, "intPrimitive like boolPrimitive");
            tryInvalidExpr(env, "boolPrimitive like string");
            tryInvalidExpr(env, "string like string escape intPrimitive");

            tryInvalidExpr(env, "intPrimitive regexp doublePrimitve");
            tryInvalidExpr(env, "intPrimitive regexp boolPrimitive");
            tryInvalidExpr(env, "boolPrimitive regexp string");
            tryInvalidExpr(env, "string regexp intPrimitive");

            env.tryInvalidCompile("select theString regexp \"*any*\" from SupportBean",
                "Failed to validate select-clause expression 'theString regexp \"*any*\"': Failed to compile regex pattern '*any*': Dangling meta character '*' near index 0");
        }
    }

    private static class ExprCoreLikeRegexEscapedChar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_S0")
                .expressions(fields, "p00 regexp '\\\\w*-ABC'");

            builder.assertion(new SupportBean_S0(-1, "TBT-ABC")).expect(fields, true);
            builder.assertion(new SupportBean_S0(-1, "TBT-BC")).expect(fields, false);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreLikeRegexStringAndNullOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select p00 like p01 as r1, " +
                "p00 like p01 escape \"!\" as r2, " +
                "p02 regexp p03 as r3 " +
                "from SupportBean_S0";

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            model.setSelectClause(SelectClause.create()
                .add(Expressions.like(Expressions.property("p00"), Expressions.property("p01")), "r1")
                .add(Expressions.like(Expressions.property("p00"), Expressions.property("p01"), Expressions.constant("!")), "r2")
                .add(Expressions.regexp(Expressions.property("p02"), Expressions.property("p03")), "r3")
            );
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean_S0")));
            model = SerializableObjectCopier.copyMayFail(model);
            assertEquals(stmtText, model.toEPL());

            EPCompiled compiled = env.compile(model, new CompilerArguments(env.getConfiguration()));
            env.deploy(compiled).addListener("s0").milestone(0);

            runLikeRegexStringAndNull(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreRegexStringAndNullCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select p00 like p01 as r1, " +
                "p00 like p01 escape \"!\" as r2, " +
                "p02 regexp p03 as r3 " +
                "from SupportBean_S0";

            EPStatementObjectModel model = env.eplToModel(epl);
            model = SerializableObjectCopier.copyMayFail(model);
            assertEquals(epl, model.toEPL());

            EPCompiled compiled = env.compile(model, new CompilerArguments(env.getConfiguration()));
            env.deploy(compiled).addListener("s0").milestone(0);

            runLikeRegexStringAndNull(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreLikeRegexNumericAndNull implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expressions(fields, "intBoxed like '%01%'", "doubleBoxed regexp '[0-9][0-9].[0-9]'");

            builder.assertion(makeSupportBeanEvent(101, 1.1)).expect(fields, true, false);
            builder.assertion(makeSupportBeanEvent(102, 11d)).expect(fields, false, true);
            builder.assertion(makeSupportBeanEvent(null, null)).expect(fields, null, null);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static void runLikeRegexStringAndNull(RegressionEnvironment env) {
        sendS0Event(env, -1, "a", "b", "c", "d");
        assertReceived(env, new Object[][]{{"r1", false}, {"r2", false}, {"r3", false}});

        sendS0Event(env, -1, null, "b", null, "d");
        assertReceived(env, new Object[][]{{"r1", null}, {"r2", null}, {"r3", null}});

        sendS0Event(env, -1, "a", null, "c", null);
        assertReceived(env, new Object[][]{{"r1", null}, {"r2", null}, {"r3", null}});

        sendS0Event(env, -1, null, null, null, null);
        assertReceived(env, new Object[][]{{"r1", null}, {"r2", null}, {"r3", null}});

        sendS0Event(env, -1, "abcdef", "%de_", "a", "[a-c]");
        assertReceived(env, new Object[][]{{"r1", true}, {"r2", true}, {"r3", true}});

        sendS0Event(env, -1, "abcdef", "b%de_", "d", "[a-c]");
        assertReceived(env, new Object[][]{{"r1", false}, {"r2", false}, {"r3", false}});

        sendS0Event(env, -1, "!adex", "!%de_", "", ".");
        assertReceived(env, new Object[][]{{"r1", true}, {"r2", false}, {"r3", false}});

        sendS0Event(env, -1, "%dex", "!%de_", "a", ".");
        assertReceived(env, new Object[][]{{"r1", false}, {"r2", true}, {"r3", true}});
    }

    private static void sendS0Event(RegressionEnvironment env, int id, String p00, String p01, String p02, String p03) {
        SupportBean_S0 bean = new SupportBean_S0(id, p00, p01, p02, p03);
        env.sendEventBean(bean);
    }

    private static void assertReceived(RegressionEnvironment env, Object[][] objects) {
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        for (Object[] object : objects) {
            String key = (String) object[0];
            Object result = object[1];
            assertEquals("key=" + key + " result=" + result, result, theEvent.get(key));
        }
    }

    private static void tryInvalidExpr(RegressionEnvironment env, String expr) {
        String statement = "select " + expr + " from SupportBean";
        env.tryInvalidCompile(statement, "skip");
    }

    private static SupportBean makeSupportBeanEvent(Integer intBoxed, Double doubleBoxed) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        return bean;
    }
}
