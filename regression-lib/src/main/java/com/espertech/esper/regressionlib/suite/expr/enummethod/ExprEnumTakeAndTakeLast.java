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
import com.espertech.esper.regressionlib.support.expreval.SupportEvalAssertionBuilder;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.assertST0Id;
import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.assertValuesArrayScalar;

public class ExprEnumTakeAndTakeLast {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumTakeEvents());
        execs.add(new ExprEnumTakeScalar());
        execs.add(new ExprEnumTakeInvalid());
        return execs;
    }

    private static class ExprEnumTakeEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.take(2)");
            builder.expression(fields[1], "contained.take(1)");
            builder.expression(fields[2], "contained.take(0)");
            builder.expression(fields[3], "contained.take(-1)");
            builder.expression(fields[4], "contained.takeLast(2)");
            builder.expression(fields[5], "contained.takeLast(1)");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, SupportBean_ST0.class)));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,2", "E3,3"))
                .verify("c0", val -> assertST0Id(val, "E1,E2"))
                .verify("c1", val -> assertST0Id(val, "E1"))
                .verify("c2", val -> assertST0Id(val, ""))
                .verify("c3", val -> assertST0Id(val, ""))
                .verify("c4", val -> assertST0Id(val, "E2,E3"))
                .verify("c5", val -> assertST0Id(val, "E3"));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,2"))
                .verify("c0", val -> assertST0Id(val, "E1,E2"))
                .verify("c1", val -> assertST0Id(val, "E1"))
                .verify("c2", val -> assertST0Id(val, ""))
                .verify("c3", val -> assertST0Id(val, ""))
                .verify("c4", val -> assertST0Id(val, "E1,E2"))
                .verify("c5", val -> assertST0Id(val, "E2"));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1"))
                .verify("c0", val -> assertST0Id(val, "E1"))
                .verify("c1", val -> assertST0Id(val, "E1"))
                .verify("c2", val -> assertST0Id(val, ""))
                .verify("c3", val -> assertST0Id(val, ""))
                .verify("c4", val -> assertST0Id(val, "E1"))
                .verify("c5", val -> assertST0Id(val, "E1"));

            SupportEvalAssertionBuilder assertionEmpty = builder.assertion(SupportBean_ST0_Container.make2Value());
            for (String field : fields) {
                assertionEmpty.verify(field, val -> assertST0Id(val, ""));
            }

            SupportEvalAssertionBuilder assertionNull = builder.assertion(SupportBean_ST0_Container.make2ValueNull());
            for (String field : fields) {
                assertionNull.verify(field, val -> assertST0Id(val, null));
            }

            builder.run(env);
        }
    }

    private static class ExprEnumTakeScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.take(2)");
            builder.expression(fields[1], "strvals.take(1)");
            builder.expression(fields[2], "strvals.takeLast(2)");
            builder.expression(fields[3], "strvals.takeLast(1)");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, String.class)));

            builder.assertion(SupportCollection.makeString("E1,E2,E3"))
                .verify("c0", val -> assertValuesArrayScalar(val, "E1", "E2"))
                .verify("c1", val -> assertValuesArrayScalar(val, "E1"))
                .verify("c2", val -> assertValuesArrayScalar(val, "E2", "E3"))
                .verify("c3", val -> assertValuesArrayScalar(val, "E3"));

            builder.assertion(SupportCollection.makeString("E1,E2"))
                .verify("c0", val -> assertValuesArrayScalar(val, "E1", "E2"))
                .verify("c1", val -> assertValuesArrayScalar(val, "E1"))
                .verify("c2", val -> assertValuesArrayScalar(val, "E1", "E2"))
                .verify("c3", val -> assertValuesArrayScalar(val, "E2"));

            LambdaAssertionUtil.assertSingleAndEmptySupportColl(builder, fields);

            builder.run(env);
        }
    }

    private static class ExprEnumTakeInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select strvals.take(null) from SupportCollection";
            env.tryInvalidCompile(epl, "Failed to validate select-clause expression 'strvals.take(null)': Failed to validate enumeration method 'take', expected a non-null result for expression parameter 0 but received a null-typed expression");
        }
    }
}
