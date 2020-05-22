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

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.assertTypes;
import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.assertTypesAllSame;

public class ExprEnumSumOf {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumSumEvents());
        execs.add(new ExprEnumSumEventsPlus());
        execs.add(new ExprEnumSumScalar());
        execs.add(new ExprEnumSumScalarStringValue());
        execs.add(new ExprEnumInvalid());
        execs.add(new ExprEnumSumArray());
        return execs;
    }

    private static class ExprEnumSumArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean");
            builder.expression(fields[0], "{1d, 2d}.sumOf()");
            builder.expression(fields[1], "{BigInteger.valueOf(1), BigInteger.valueOf(2)}.sumOf()");
            builder.expression(fields[2], "{1L, 2L}.sumOf()");
            builder.expression(fields[3], "{1L, 2L, null}.sumOf()");

            builder.assertion(new SupportBean()).expect(fields, 3d, BigInteger.valueOf(3), 3L, 3L);

            builder.run(env);
        }
    }

    private static class ExprEnumSumEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_Container");
            builder.expression(fields[0], "beans.sumOf(x => intBoxed)");
            builder.expression(fields[1], "beans.sumOf(x => doubleBoxed)");
            builder.expression(fields[2], "beans.sumOf(x => longBoxed)");
            builder.expression(fields[3], "beans.sumOf(x => bigDecimal)");
            builder.expression(fields[4], "beans.sumOf(x => bigInteger)");

            builder.statementConsumer(stmt -> assertTypes(stmt.getEventType(), fields, new Class[]{Integer.class, Double.class, Long.class, BigDecimal.class, BigInteger.class}));

            builder.assertion(new SupportBean_Container(null)).expect(fields, null, null, null, null, null);

            builder.assertion(new SupportBean_Container(Collections.emptyList())).expect(fields, null, null, null, null, null);

            List<SupportBean> listOne = new ArrayList<>(Arrays.asList(make(2, 3d, 4L, 5, 6)));
            builder.assertion(new SupportBean_Container(listOne)).expect(fields, 2, 3d, 4L, new BigDecimal(5), new BigInteger("6"));

            List<SupportBean> listTwo = new ArrayList<>(Arrays.asList(make(2, 3d, 4L, 5, 6), make(4, 6d, 8L, 10, 12)));
            builder.assertion(new SupportBean_Container(listTwo)).expect(fields, 2 + 4, 3d + 6d, 4L + 8L, new BigDecimal(5 + 10), new BigInteger("18"));

            builder.run(env);
        }
    }

    private static class ExprEnumSumEventsPlus implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_Container");
            builder.expression(fields[0], "beans.sumOf(x => intBoxed)");
            builder.expression(fields[1], "beans.sumOf( (x, i) => intBoxed + i*10)");
            builder.expression(fields[2], "beans.sumOf( (x, i, s) => intBoxed + i*10 + s*100)");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, Integer.class));

            builder.assertion(new SupportBean_Container(null)).expect(fields, null, null, null);

            builder.assertion(new SupportBean_Container(Collections.emptyList())).expect(fields, null, null, null);

            List<SupportBean> listOne = new ArrayList<>(Arrays.asList(makeSB("E1", 10)));
            builder.assertion(new SupportBean_Container(listOne)).expect(fields, 10, 10, 110);

            List<SupportBean> listTwo = new ArrayList<>(Arrays.asList(makeSB("E1", 10), makeSB("E2", 11)));
            builder.assertion(new SupportBean_Container(listTwo)).expect(fields, 21, 31, 431);

            builder.run(env);
        }

        private SupportBean makeSB(String theString, int intBoxed) {
            SupportBean bean = new SupportBean(theString, intBoxed);
            bean.setIntBoxed(intBoxed);
            return bean;
        }
    }

    private static class ExprEnumSumScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "intvals.sumOf()");
            builder.expression(fields[1], "bdvals.sumOf()");

            builder.statementConsumer(stmt -> assertTypes(stmt.getEventType(), fields, new Class[]{Integer.class, BigDecimal.class}));

            builder.assertion(SupportCollection.makeNumeric("1,4,5")).expect(fields, 1 + 4 + 5, new BigDecimal(1 + 4 + 5));

            builder.assertion(SupportCollection.makeNumeric("3,4")).expect(fields, 3 + 4, new BigDecimal(3 + 4));

            builder.assertion(SupportCollection.makeNumeric("3")).expect(fields, 3, new BigDecimal(3));

            builder.assertion(SupportCollection.makeNumeric("")).expect(fields, null, null);

            builder.assertion(SupportCollection.makeNumeric(null)).expect(fields, null, null);

            builder.run(env);
        }
    }

    private static class ExprEnumSumScalarStringValue implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.sumOf(v => extractNum(v))");
            builder.expression(fields[1], "strvals.sumOf(v => extractBigDecimal(v))");
            builder.expression(fields[2], "strvals.sumOf( (v, i) => extractNum(v) + i*10)");
            builder.expression(fields[3], "strvals.sumOf( (v, i, s) => extractNum(v) + i*10 + s*100)");

            builder.statementConsumer(stmt -> assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, BigDecimal.class, Integer.class, Integer.class}));

            builder.assertion(SupportCollection.makeString("E2,E1,E5,E4")).expect(fields, 2 + 1 + 5 + 4, new BigDecimal(2 + 1 + 5 + 4), 2 + 11 + 25 + 34, 402 + 411 + 425 + 434);

            builder.assertion(SupportCollection.makeString("E1")).expect(fields, 1, new BigDecimal(1), 1, 101);

            builder.assertion(SupportCollection.makeString(null)).expect(fields, null, null, null, null);

            builder.assertion(SupportCollection.makeString("")).expect(fields, null, null, null, null);

            builder.run(env);
        }
    }

    private static class ExprEnumInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select beans.sumof() from SupportBean_Container";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'beans.sumof()': Invalid input for built-in enumeration method 'sumof' and 0-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type '");
        }
    }

    private static SupportBean make(Integer intBoxed, Double doubleBoxed, Long longBoxed, int bigDecimal, int bigInteger) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setLongBoxed(longBoxed);
        bean.setBigDecimal(new BigDecimal(bigDecimal));
        bean.setBigInteger(new BigInteger(Integer.toString(bigInteger)));
        return bean;
    }
}
