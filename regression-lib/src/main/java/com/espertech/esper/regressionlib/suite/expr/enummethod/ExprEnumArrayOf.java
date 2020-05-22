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
package com.espertech.esper.regressionlib.suite.expr.enummethod;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.assertEqualsExactOrder;
import static com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container.make2Value;
import static com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container.make2ValueNull;
import static com.espertech.esper.regressionlib.support.bean.SupportCollection.makeString;
import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.assertTypesAllSame;

public class ExprEnumArrayOf {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumArrayOfWSelectFromScalar());
        execs.add(new ExprEnumArrayOfWSelectFromScalarWIndex());
        execs.add(new ExprEnumArrayOfWSelectFromEvent());
        execs.add(new ExprEnumArrayOfEvents());
        execs.add(new ExprEnumArrayOfScalar());
        return execs;
    }

    private static class ExprEnumArrayOfScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.arrayOf()");
            builder.expression(fields[1], "strvals.arrayOf(v => v)");
            builder.expression(fields[2], "strvals.arrayOf( (v, i) => v || '_' || Integer.toString(i))");
            builder.expression(fields[3], "strvals.arrayOf( (v, i, s) => v || '_' || Integer.toString(i) || '_' || Integer.toString(s))");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, String[].class));

            builder.assertion(SupportCollection.makeString("A,B,C"))
                .expect(fields, csv("A,B,C"), csv("A,B,C"), csv("A_0,B_1,C_2"), csv("A_0_3,B_1_3,C_2_3"));

            builder.assertion(SupportCollection.makeString(""))
                .expect(fields, csv(""), csv(""), csv(""), csv(""));

            builder.assertion(SupportCollection.makeString("A"))
                .expect(fields, csv("A"), csv("A"), csv("A_0"), csv("A_0_1"));

            builder.assertion(SupportCollection.makeString(null))
                .expect(fields, null, null, null, null);

            builder.run(env);
        }
    }

    private static class ExprEnumArrayOfEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.arrayOf(x => x.p00)");
            builder.expression(fields[1], "contained.arrayOf((x, i) => x.p00 + i*10)");
            builder.expression(fields[2], "contained.arrayOf((x, i, s) => x.p00 + i*10 + s*100)");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, Integer[].class));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E2,2"))
                .expect(fields, intArray(1, 9, 2), intArray(1, 19, 22), intArray(301, 319, 322));

            builder.assertion(SupportBean_ST0_Container.make2ValueNull())
                .expect(fields, null, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value())
                .expect(fields, intArray(), intArray(), intArray());

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,9"))
                .expect(fields, intArray(9), intArray(9), intArray(109));

            builder.run(env);
        }
    }

    private static class ExprEnumArrayOfWSelectFromEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.selectFrom(v => v.id).arrayOf()");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, String[].class));

            builder.assertion(make2Value("E1,12", "E2,11", "E3,2"))
                .verify(fields[0], val -> assertArrayEquals(new String[]{"E1", "E2", "E3"}, val));

            builder.assertion(make2Value("E4,14"))
                .verify(fields[0], val -> assertArrayEquals(new String[]{"E4"}, val));

            builder.assertion(make2Value())
                .verify(fields[0], val -> assertArrayEquals(new String[0], val));

            builder.assertion(make2ValueNull())
                .verify(fields[0], Assert::assertNull);

            builder.run(env);
        }
    }

    private static class ExprEnumArrayOfWSelectFromScalarWIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.selectfrom((v, i) => v || '-' || Integer.toString(i)).arrayOf()");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, String[].class));

            builder.assertion(makeString("E1,E2,E3"))
                .verify(fields[0], val -> assertArrayEquals(new String[]{"E1-0", "E2-1", "E3-2"}, val));

            builder.assertion(makeString("E4"))
                .verify(fields[0], val -> assertArrayEquals(new String[]{"E4-0"}, val));

            builder.assertion(makeString(""))
                .verify(fields[0], val -> assertArrayEquals(new String[0], val));

            builder.assertion(makeString(null))
                .verify(fields[0], Assert::assertNull);

            builder.run(env);
        }
    }

    private static class ExprEnumArrayOfWSelectFromScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.selectfrom(v => Integer.parseInt(v)).arrayOf()");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, Integer[].class));

            builder.assertion(makeString("1,2,3"))
                .verify(fields[0], val -> assertArrayEquals(new Integer[]{1, 2, 3}, val));

            builder.assertion(makeString("1"))
                .verify(fields[0], val -> assertArrayEquals(new Integer[]{1}, val));

            builder.assertion(makeString(""))
                .verify(fields[0], val -> assertArrayEquals(new Integer[]{}, val));

            builder.assertion(makeString(null))
                .verify(fields[0], Assert::assertNull);

            builder.run(env);
        }
    }

    private static void assertArrayEquals(String[] expected, Object received) {
        assertEqualsExactOrder(expected, (String[]) received);
    }

    private static void assertArrayEquals(Integer[] expected, Object received) {
        assertEqualsExactOrder(expected, (Integer[]) received);
    }

    private static Integer[] intArray(Integer... ints) {
        return ints;
    }

    private static String[] csv(String csv) {
        if (csv.isEmpty()) {
            return new String[0];
        }
        return csv.split(",");
    }
}
