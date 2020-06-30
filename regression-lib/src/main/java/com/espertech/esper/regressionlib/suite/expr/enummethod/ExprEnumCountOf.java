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

import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypesAllSame;

public class ExprEnumCountOf {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumCountOfEvents());
        execs.add(new ExprEnumCountOfScalar());
        return execs;
    }

    private static class ExprEnumCountOfEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.countof()");
            builder.expression(fields[1], "contained.countof(x => x.p00 = 9)");
            builder.expression(fields[2], "contained.countof((x, i) => x.p00 + i = 10)");
            builder.expression(fields[3], "contained.countof((x, i, s) => x.p00 + i + s = 100)");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypePremade.INTEGERBOXED.getEPType()));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E2,9")).expect(fields, 3, 2, 1, 0);

            builder.assertion(SupportBean_ST0_Container.make2ValueNull()).expect(fields, null, null, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value()).expect(fields, 0, 0, 0, 0);

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,9")).expect(fields, 1, 1, 0, 0);

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1")).expect(fields, 1, 0, 0, 0);

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,10", "E2,9")).expect(fields, 2, 1, 2, 0);

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,98", "E2,97")).expect(fields, 2, 0, 0, 2);

            builder.run(env);
        }
    }

    private static class ExprEnumCountOfScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.countof()");
            builder.expression(fields[1], "strvals.countof(x => x = 'E1')");
            builder.expression(fields[2], "strvals.countof((x, i) => x = 'E1' and i >= 1)");
            builder.expression(fields[3], "strvals.countof((x, i, s) => x = 'E1' and i >= 1 and s > 2)");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypePremade.INTEGERBOXED.getEPType()));

            builder.assertion(SupportCollection.makeString("E1,E2")).expect(fields, 2, 1, 0, 0);

            builder.assertion(SupportCollection.makeString("E1,E2,E1,E3")).expect(fields, 4, 2, 1, 1);

            builder.assertion(SupportCollection.makeString("E1")).expect(fields, 1, 1, 0, 0);

            builder.assertion(SupportCollection.makeString("E1,E1")).expect(fields, 2, 2, 1, 0);

            builder.run(env);
        }
    }
}
