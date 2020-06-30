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
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.*;

public class ExprEnumWhere {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumWhereEvents());
        execs.add(new ExprEnumWhereScalar());
        execs.add(new ExprEnumWhereScalarBoolean());
        return execs;
    }

    private static class ExprEnumWhereEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.where(x => p00 = 9)");
            builder.expression(fields[1], "contained.where((x, i) => x.p00 = 9 and i >= 1)");
            builder.expression(fields[2], "contained.where((x, i, s) => x.p00 = 9 and i >= 1 and s > 2)");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, SupportBean_ST0.class)));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E3,1"))
                .verify("c0", val -> assertST0Id(val, "E2"))
                .verify("c1", val -> assertST0Id(val, "E2"))
                .verify("c2", val -> assertST0Id(val, "E2"));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,9", "E2,1", "E3,1"))
                .verify("c0", val -> assertST0Id(val, "E1"))
                .verify("c1", val -> assertST0Id(val, ""))
                .verify("c2", val -> assertST0Id(val, ""));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,1", "E3,9"))
                .verify("c0", val -> assertST0Id(val, "E3"))
                .verify("c1", val -> assertST0Id(val, "E3"))
                .verify("c2", val -> assertST0Id(val, "E3"));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,9", "E3,9"))
                .verify("c0", val -> assertST0Id(val, "E1,E3"))
                .verify("c1", val -> assertST0Id(val, "E3"))
                .verify("c2", val -> assertST0Id(val, ""));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E3,1", "E4,9"))
                .verify("c0", val -> assertST0Id(val, "E2,E4"))
                .verify("c1", val -> assertST0Id(val, "E2,E4"))
                .verify("c2", val -> assertST0Id(val, "E2,E4"));

            builder.assertion(SupportBean_ST0_Container.make2ValueNull())
                .verify("c0", Assert::assertNull)
                .verify("c1", Assert::assertNull)
                .verify("c2", Assert::assertNull);

            builder.assertion(SupportBean_ST0_Container.make2Value())
                .verify("c0", val -> assertST0Id(val, ""))
                .verify("c1", val -> assertST0Id(val, ""))
                .verify("c2", val -> assertST0Id(val, ""));

            builder.run(env);
        }
    }

    private static class ExprEnumWhereScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.where(x => x not like '%1%')");
            builder.expression(fields[1], "strvals.where((x, i) => x not like '%1%' and i >= 1)");
            builder.expression(fields[2], "strvals.where((x, i, s) => x not like '%1%' and i >= 1 and s >= 3)");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, String.class)));

            builder.assertion(SupportCollection.makeString("E1,E2,E3"))
                .verify("c0", val -> assertValuesArrayScalar(val, "E2", "E3"))
                .verify("c1", val -> assertValuesArrayScalar(val, "E2", "E3"))
                .verify("c2", val -> assertValuesArrayScalar(val, "E2", "E3"));

            builder.assertion(SupportCollection.makeString("E4,E2,E1"))
                .verify("c0", val -> assertValuesArrayScalar(val, "E4", "E2"))
                .verify("c1", val -> assertValuesArrayScalar(val, "E2"))
                .verify("c2", val -> assertValuesArrayScalar(val, "E2"));

            builder.assertion(SupportCollection.makeString(""))
                .verify("c0", val -> assertValuesArrayScalar(val))
                .verify("c1", val -> assertValuesArrayScalar(val))
                .verify("c2", val -> assertValuesArrayScalar(val));

            builder.assertion(SupportCollection.makeString("E4,E2"))
                .verify("c0", val -> assertValuesArrayScalar(val, "E4", "E2"))
                .verify("c1", val -> assertValuesArrayScalar(val, "E2"))
                .verify("c2", val -> assertValuesArrayScalar(val));

            builder.run(env);
        }
    }

    private static class ExprEnumWhereScalarBoolean implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "boolvals.where(x => x)");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, Boolean.class)));

            builder.assertion(SupportCollection.makeBoolean("true,true,false"))
                .verify("c0", val -> assertValuesArrayScalar(val, true, true));

            builder.run(env);
        }
    }
}
