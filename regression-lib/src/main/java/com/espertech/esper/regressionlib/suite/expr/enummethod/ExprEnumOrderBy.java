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

import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.assertST0Id;
import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.assertValuesArrayScalar;

public class ExprEnumOrderBy {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumOrderByEvents());
        execs.add(new ExprEnumOrderByEventsPlus());
        execs.add(new ExprEnumOrderByScalar());
        execs.add(new ExprEnumOrderByScalarWithParam());
        execs.add(new ExprEnumOrderByInvalid());
        return execs;
    }

    private static class ExprEnumOrderByEventsPlus implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.orderBy( (x, i) => case when i <= 2 then p00 else i-10 end)");
            builder.expression(fields[1], "contained.orderByDesc( (x, i) => case when i <= 2 then p00 else i-10 end)");
            builder.expression(fields[2], "contained.orderBy( (x, i, s) => case when s <= 2 then p00 else i-10 end)");
            builder.expression(fields[3], "contained.orderByDesc( (x, i, s) => case when s <= 2 then p00 else i-10 end)");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, SupportBean_ST0.class)));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,2"))
                .verify("c0", val -> assertST0Id(val, "E1,E2"))
                .verify("c1", val -> assertST0Id(val, "E2,E1"))
                .verify("c2", val -> assertST0Id(val, "E1,E2"))
                .verify("c3", val -> assertST0Id(val, "E2,E1"));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,2", "E3,3", "E4,4"))
                .verify("c0", val -> assertST0Id(val, "E4,E1,E2,E3"))
                .verify("c1", val -> assertST0Id(val, "E3,E2,E1,E4"))
                .verify("c2", val -> assertST0Id(val, "E1,E2,E3,E4"))
                .verify("c3", val -> assertST0Id(val, "E4,E3,E2,E1"));

            builder.assertion(SupportBean_ST0_Container.make2ValueNull()).expect(fields, null, null, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value())
                .verify("c0", val -> assertST0Id(val, ""))
                .verify("c1", val -> assertST0Id(val, ""))
                .verify("c2", val -> assertST0Id(val, ""))
                .verify("c3", val -> assertST0Id(val, ""));

            builder.run(env);
        }
    }

    private static class ExprEnumOrderByEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.orderBy(x => p00)");
            builder.expression(fields[1], "contained.orderBy(x => 10 - p00)");
            builder.expression(fields[2], "contained.orderBy(x => 0)");
            builder.expression(fields[3], "contained.orderByDesc(x => p00)");
            builder.expression(fields[4], "contained.orderByDesc(x => 10 - p00)");
            builder.expression(fields[5], "contained.orderByDesc(x => 0)");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, SupportBean_ST0.class)));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,2"))
                .verify("c0", val -> assertST0Id(val, "E1,E2"))
                .verify("c1", val -> assertST0Id(val, "E2,E1"))
                .verify("c2", val -> assertST0Id(val, "E1,E2"))
                .verify("c3", val -> assertST0Id(val, "E2,E1"))
                .verify("c4", val -> assertST0Id(val, "E1,E2"))
                .verify("c5", val -> assertST0Id(val, "E1,E2"));

            builder.assertion(SupportBean_ST0_Container.make2Value("E3,1", "E2,2", "E4,1", "E1,2"))
                .verify("c0", val -> assertST0Id(val, "E3,E4,E2,E1"))
                .verify("c1", val -> assertST0Id(val, "E2,E1,E3,E4"))
                .verify("c2", val -> assertST0Id(val, "E3,E2,E4,E1"))
                .verify("c3", val -> assertST0Id(val, "E2,E1,E3,E4"))
                .verify("c4", val -> assertST0Id(val, "E3,E4,E2,E1"))
                .verify("c5", val -> assertST0Id(val, "E3,E2,E4,E1"));

            builder.assertion(SupportBean_ST0_Container.make2ValueNull()).expect(fields, null, null, null, null, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value())
                .verify("c0", val -> assertST0Id(val, ""))
                .verify("c1", val -> assertST0Id(val, ""))
                .verify("c2", val -> assertST0Id(val, ""))
                .verify("c3", val -> assertST0Id(val, ""))
                .verify("c4", val -> assertST0Id(val, ""))
                .verify("c5", val -> assertST0Id(val, ""));

            builder.run(env);
        }
    }

    private static class ExprEnumOrderByScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.orderBy()");
            builder.expression(fields[1], "strvals.orderByDesc()");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, String.class)));

            builder.assertion(SupportCollection.makeString("E2,E1,E5,E4"))
                .verify("c0", val -> assertValuesArrayScalar(val, "E1", "E2", "E4", "E5"))
                .verify("c1", val -> assertValuesArrayScalar(val, "E5", "E4", "E2", "E1"));

            LambdaAssertionUtil.assertSingleAndEmptySupportColl(builder, fields);
            builder.run(env);
        }
    }

    private static class ExprEnumOrderByScalarWithParam implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.orderBy(v => extractNum(v))");
            builder.expression(fields[1], "strvals.orderByDesc(v => extractNum(v))");
            builder.expression(fields[2], "strvals.orderBy( (v, i) => case when i <= 2 then extractNum(v) else i-10 end)");
            builder.expression(fields[3], "strvals.orderByDesc( (v, i) => case when i <= 2 then extractNum(v) else i-10 end)");
            builder.expression(fields[4], "strvals.orderBy( (v, i, s) => case when s <= 2 then extractNum(v) else i-10 end)");
            builder.expression(fields[5], "strvals.orderByDesc( (v, i, s) => case when s <= 2 then extractNum(v) else i-10 end)");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, String.class)));

            builder.assertion(SupportCollection.makeString("E2,E1,E5,E4"))
                .verify("c0", val -> assertValuesArrayScalar(val, "E1", "E2", "E4", "E5"))
                .verify("c1", val -> assertValuesArrayScalar(val, "E5", "E4", "E2", "E1"))
                .verify("c2", val -> assertValuesArrayScalar(val, "E4", "E1", "E2", "E5"))
                .verify("c3", val -> assertValuesArrayScalar(val, "E5", "E2", "E1", "E4"))
                .verify("c4", val -> assertValuesArrayScalar(val, "E2", "E1", "E5", "E4"))
                .verify("c5", val -> assertValuesArrayScalar(val, "E4", "E5", "E1", "E2"));

            builder.assertion(SupportCollection.makeString("E2,E1"))
                .verify("c0", val -> assertValuesArrayScalar(val, "E1", "E2"))
                .verify("c1", val -> assertValuesArrayScalar(val, "E2", "E1"))
                .verify("c2", val -> assertValuesArrayScalar(val, "E1", "E2"))
                .verify("c3", val -> assertValuesArrayScalar(val, "E2", "E1"))
                .verify("c4", val -> assertValuesArrayScalar(val, "E1", "E2"))
                .verify("c5", val -> assertValuesArrayScalar(val, "E2", "E1"));

            LambdaAssertionUtil.assertSingleAndEmptySupportColl(builder, fields);

            builder.run(env);
        }
    }

    private static class ExprEnumOrderByInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select contained.orderBy() from SupportBean_ST0_Container";
            env.tryInvalidCompile(epl, "Failed to validate select-clause expression 'contained.orderBy()': Invalid input for built-in enumeration method 'orderBy' and 0-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type '" + SupportBean_ST0.class.getName() + "'");

            epl = "select strvals.orderBy(v => null) from SupportCollection";
            env.tryInvalidCompile(epl, "Failed to validate select-clause expression 'strvals.orderBy()': Null-type is not allowed");
        }
    }
}
