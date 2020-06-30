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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithLongArray;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.common.client.type.EPTypePremade.INTEGERBOXED;
import static com.espertech.esper.common.client.type.EPTypePremade.STRING;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypes;
import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypesAllSame;

public class ExprEnumMinMax {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumMinMaxEvents());
        execs.add(new ExprEnumMinMaxScalar());
        execs.add(new ExprEnumMinMaxScalarWithPredicate());
        execs.add(new ExprEnumMinMaxScalarChain());
        execs.add(new ExprEnumInvalid());
        return execs;
    }

    private static class ExprEnumMinMaxScalarChain implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportEventWithLongArray");
            builder.expression(fields[0], "coll.max().minus(1 minute) >= coll.min()");

            builder.assertion(new SupportEventWithLongArray("E1", new long[]{150000, 140000, 200000, 190000}))
                .expect(fields, true);

            builder.assertion(new SupportEventWithLongArray("E2", new long[]{150000, 139999, 200000, 190000}))
                .expect(fields, true);

            builder.run(env);
        }
    }

    private static class ExprEnumMinMaxScalarWithPredicate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.min(v => extractNum(v))");
            builder.expression(fields[1], "strvals.max(v => extractNum(v))");
            builder.expression(fields[2], "strvals.min(v => v)");
            builder.expression(fields[3], "strvals.max(v => v)");
            builder.expression(fields[4], "strvals.min( (v, i) => extractNum(v) + i*10)");
            builder.expression(fields[5], "strvals.max( (v, i) => extractNum(v) + i*10)");
            builder.expression(fields[6], "strvals.min( (v, i, s) => extractNum(v) + i*10 + s*100)");
            builder.expression(fields[7], "strvals.max( (v, i, s) => extractNum(v) + i*10 + s*100)");

            builder.statementConsumer(stmt -> assertTypes(stmt.getEventType(), fields, new EPTypeClass[]{INTEGERBOXED.getEPType(), INTEGERBOXED.getEPType(), STRING.getEPType(), STRING.getEPType(),
                INTEGERBOXED.getEPType(), INTEGERBOXED.getEPType(), INTEGERBOXED.getEPType(), INTEGERBOXED.getEPType()}));

            builder.assertion(SupportCollection.makeString("E2,E1,E5,E4")).expect(fields, 1, 5, "E1", "E5", 2, 34, 402, 434);

            builder.assertion(SupportCollection.makeString("E1")).expect(fields, 1, 1, "E1", "E1", 1, 1, 101, 101);

            builder.assertion(SupportCollection.makeString(null)).expect(fields, null, null, null, null, null, null, null, null);

            builder.assertion(SupportCollection.makeString("")).expect(fields, null, null, null, null, null, null, null, null);

            builder.run(env);
        }
    }

    private static class ExprEnumMinMaxEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.min(x => p00)");
            builder.expression(fields[1], "contained.max(x => p00)");
            builder.expression(fields[2], "contained.min( (x, i) => p00 + i*10)");
            builder.expression(fields[3], "contained.max( (x, i) => p00 + i*10)");
            builder.expression(fields[4], "contained.min( (x, i, s) => p00 + i*10 + s*100)");
            builder.expression(fields[5], "contained.max( (x, i, s) => p00 + i*10 + s*100)");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, INTEGERBOXED.getEPType()));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2")).expect(fields, 2, 12, 12, 22, 312, 322);

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,12", "E2,0", "E2,2")).expect(fields, 0, 12, 10, 22, 310, 322);

            builder.assertion(SupportBean_ST0_Container.make2ValueNull()).expect(fields, null, null, null, null, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value()).expect(fields, null, null, null, null, null, null);

            builder.run(env);
        }
    }

    private static class ExprEnumMinMaxScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.min()");
            builder.expression(fields[1], "strvals.max()");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, STRING.getEPType()));

            builder.assertion(SupportCollection.makeString("E2,E1,E5,E4")).expect(fields, "E1", "E5");

            builder.assertion(SupportCollection.makeString("E1")).expect(fields, "E1", "E1");

            builder.assertion(SupportCollection.makeString(null)).expect(fields, null, null);

            builder.assertion(SupportCollection.makeString("")).expect(fields, null, null);

            builder.run(env);
        }
    }

    private static class ExprEnumInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select contained.min() from SupportBean_ST0_Container";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'contained.min()': Invalid input for built-in enumeration method 'min' and 0-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type '" + SupportBean_ST0.class.getName() + "'");

            epl = "select contained.min(x => null) from SupportBean_ST0_Container";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'contained.min()': Null-type is not allowed");
        }
    }

    public static class MyService {
        public static int extractNum(String arg) {
            return Integer.parseInt(arg.substring(1));
        }

        public static BigDecimal extractBigDecimal(String arg) {
            return new BigDecimal(arg.substring(1));
        }
    }

    public static class MyEvent {
        private MyEvent myevent;

        public MyEvent getMyevent() {
            return myevent;
        }
    }
}
