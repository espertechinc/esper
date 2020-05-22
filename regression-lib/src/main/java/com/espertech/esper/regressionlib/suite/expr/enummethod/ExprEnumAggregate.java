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

public class ExprEnumAggregate {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumAggregateEvents());
        execs.add(new ExprEnumAggregateScalar());
        return execs;
    }

    private static class ExprEnumAggregateEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.aggregate(0, (result, item) => result + item.p00)");
            builder.expression(fields[1], "contained.aggregate('', (result, item) => result || ', ' || item.id)");
            builder.expression(fields[2], "contained.aggregate('', (result, item) => result || (case when result='' then '' else ',' end) || item.id)");
            builder.expression(fields[3], "contained.aggregate(0, (result, item, i) => result + item.p00 + i*10)");
            builder.expression(fields[4], "contained.aggregate(0, (result, item, i, s) => result + item.p00 + i*10 + s*100)");

            builder.statementConsumer(stmt -> LambdaAssertionUtil.assertTypes(stmt.getEventType(), fields, new Class[]{Integer.class, String.class, String.class, Integer.class, Integer.class}));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2"))
                .expect(fields, 25, ", E1, E2, E2", "E1,E2,E2", 12 + 21 + 22, 312 + 321 + 322);

            builder.assertion(SupportBean_ST0_Container.make2ValueNull())
                .expect(fields, null, null, null, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value(new String[0]))
                .expect(fields, 0, "", "", 0, 0);

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,12"))
                .expect(fields, 12, ", E1", "E1", 12, 112);

            builder.run(env);
        }
    }

    private static class ExprEnumAggregateScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.aggregate('', (result, item) => result || '+' || item)");
            builder.expression(fields[1], "strvals.aggregate('', (result, item, i) => result || '+' || item || '_' || Integer.toString(i))");
            builder.expression(fields[2], "strvals.aggregate('', (result, item, i, s) => result || '+' || item || '_' || Integer.toString(i) || '_' || Integer.toString(s))");

            builder.statementConsumer(stmt -> LambdaAssertionUtil.assertTypesAllSame(stmt.getEventType(), fields, String.class));

            builder.assertion(SupportCollection.makeString("E1,E2,E3"))
                .expect(fields, "+E1+E2+E3", "+E1_0+E2_1+E3_2", "+E1_0_3+E2_1_3+E3_2_3");

            builder.assertion(SupportCollection.makeString("E1")).expect(fields, "+E1", "+E1_0", "+E1_0_1");

            builder.assertion(SupportCollection.makeString("")).expect(fields, "", "", "");

            builder.assertion(SupportCollection.makeString(null)).expect(fields, null, null, null);

            builder.run(env);
        }
    }
}
