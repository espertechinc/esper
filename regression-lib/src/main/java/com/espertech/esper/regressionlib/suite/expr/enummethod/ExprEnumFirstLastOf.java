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
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.assertTypesAllSame;
import static org.junit.Assert.assertEquals;

public class ExprEnumFirstLastOf {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumFirstLastScalar());
        execs.add(new ExprEnumFirstLastEventProperty());
        execs.add(new ExprEnumFirstLastEvent());
        execs.add(new ExprEnumFirstLastEventWithPredicate());
        return execs;
    }

    private static class ExprEnumFirstLastScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.firstOf()");
            builder.expression(fields[1], "strvals.lastOf()");
            builder.expression(fields[2], "strvals.firstOf(x => x like '%1%')");
            builder.expression(fields[3], "strvals.lastOf(x => x like '%1%')");
            builder.expression(fields[4], "strvals.firstOf((x, i) => x like '%1%' and i >= 1)");
            builder.expression(fields[5], "strvals.lastOf((x, i) => x like '%1%' and i >= 1)");
            builder.expression(fields[6], "strvals.firstOf((x, i, s) => x like '%1%' and i >= 1 and s > 2)");
            builder.expression(fields[7], "strvals.lastOf((x, i, s) => x like '%1%' and i >= 1 and s > 2)");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, String.class));
            
            builder.assertion(SupportCollection.makeString("E1,E2,E3")).expect(fields, "E1", "E3", "E1", "E1", null, null, null, null);

            builder.assertion(SupportCollection.makeString("E1")).expect(fields, "E1", "E1", "E1", "E1", null, null, null, null);

            builder.assertion(SupportCollection.makeString("E2,E3,E4")).expect(fields, "E2", "E4", null, null, null, null, null, null);

            builder.assertion(SupportCollection.makeString("")).expect(fields, null, null, null, null, null, null, null, null);

            builder.assertion(SupportCollection.makeString(null)).expect(fields, null, null, null, null, null, null, null, null);

            builder.assertion(SupportCollection.makeString("E5,E2,E3,A1,B1")).expect(fields, "E5", "B1", "A1", "B1", "A1", "B1", "A1", "B1");

            builder.assertion(SupportCollection.makeString("A1,B1,E5,E2,E3")).expect(fields, "A1", "E3", "A1", "B1", "B1", "B1", "B1", "B1");

            builder.assertion(SupportCollection.makeString("A1,B1")).expect(fields, "A1", "B1", "A1", "B1", "B1", "B1", null, null);

            builder.run(env);
        }
    }

    private static class ExprEnumFirstLastEventProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.firstOf().p00");
            builder.expression(fields[1], "contained.lastOf().p00");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, Integer.class));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E3,3")).expect(fields, 1, 3);

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1")).expect(fields, 1, 1);

            builder.assertion(SupportBean_ST0_Container.make2ValueNull()).expect(fields, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value()).expect(fields, null, null);

            builder.run(env);
        }
    }

    private static class ExprEnumFirstLastEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.firstOf()");
            builder.expression(fields[1], "contained.lastOf()");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, SupportBean_ST0.class));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E3,9", "E2,9"))
                .verify("c0", value -> assertId(value, "E1"))
                .verify("c1", value -> assertId(value, "E2"));

            builder.assertion(SupportBean_ST0_Container.make2Value("E2,2"))
                .verify("c0", value -> assertId(value, "E2"))
                .verify("c1", value -> assertId(value, "E2"));

            builder.assertion(SupportBean_ST0_Container.make2ValueNull()).expect(fields, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value()).expect(fields, null, null);

            builder.run(env);
        }
    }

    private static class ExprEnumFirstLastEventWithPredicate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.firstOf(x => p00 = 9)");
            builder.expression(fields[1], "contained.lastOf(x => p00 = 9)");
            builder.expression(fields[2], "contained.firstOf( (x, i) => p00 = 9 and i >= 1)");
            builder.expression(fields[3], "contained.lastOf( (x, i) => p00 = 9 and i >= 1)");
            builder.expression(fields[4], "contained.firstOf( (x, i, s) => p00 = 9 and i >= 1 and s > 2)");
            builder.expression(fields[5], "contained.lastOf((x, i, s) => p00 = 9 and i >= 1 and s > 2)");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, SupportBean_ST0.class));

            SupportBean_ST0_Container beanOne = SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E2,9");
            builder.assertion(beanOne).expect(fields, beanOne.getContained().get(1), beanOne.getContained().get(2),
                beanOne.getContained().get(1), beanOne.getContained().get(2),
                beanOne.getContained().get(1), beanOne.getContained().get(2));

            builder.assertion(SupportBean_ST0_Container.make2ValueNull()).expect(fields, null, null, null, null, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value()).expect(fields, null, null, null, null, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,1", "E2,1")).expect(fields, null, null, null, null, null, null);

            SupportBean_ST0_Container beanTwo = SupportBean_ST0_Container.make2Value("E1,1", "E2,9");
            builder.assertion(beanTwo).expect(fields, beanTwo.getContained().get(1), beanTwo.getContained().get(1),
                beanTwo.getContained().get(1), beanTwo.getContained().get(1), null, null);

            SupportBean_ST0_Container beanThree = SupportBean_ST0_Container.make2Value("E2,9", "E1,1");
            builder.assertion(beanThree).expect(fields, beanThree.getContained().get(0), beanThree.getContained().get(0),
                null, null, null, null);

            builder.run(env);
        }
    }

    private static void assertId(Object value, String id) {
        SupportBean_ST0 result = (SupportBean_ST0) value;
        assertEquals(id, result.getId());
    }
}
