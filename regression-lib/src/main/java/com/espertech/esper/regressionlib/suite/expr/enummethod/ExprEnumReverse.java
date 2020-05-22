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
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.*;

public class ExprEnumReverse {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumReverseEvents());
        execs.add(new ExprEnumReverseScalar());
        return execs;
    }

    private static class ExprEnumReverseEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.reverse()");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, Collection.class));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E3,1"))
                .verify("c0", val -> assertST0Id(val, "E3,E2,E1"));

            builder.assertion(SupportBean_ST0_Container.make2Value("E2,9", "E1,1"))
                .verify("c0", val -> assertST0Id(val, "E1,E2"));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1"))
                .verify("c0", val -> assertST0Id(val, "E1"));

            builder.assertion(SupportBean_ST0_Container.make2ValueNull())
                .verify("c0", val -> assertST0Id(val, null));

            builder.assertion(SupportBean_ST0_Container.make2Value())
                .verify("c0", val -> assertST0Id(val, ""));

            builder.run(env);
        }
    }

    private static class ExprEnumReverseScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.reverse()");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, Collection.class));

            builder.assertion(SupportCollection.makeString("E2,E1,E5,E4"))
                .verify("c0", val -> assertValuesArrayScalar(val, "E4", "E5", "E1", "E2"));

            LambdaAssertionUtil.assertSingleAndEmptySupportColl(builder, fields);

            builder.run(env);
        }
    }
}