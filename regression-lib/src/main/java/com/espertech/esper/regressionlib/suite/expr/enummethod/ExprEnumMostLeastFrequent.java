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
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

public class ExprEnumMostLeastFrequent {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumMostLeastFreqEvents());
        execs.add(new ExprEnumMostLeastFreqScalarNoParam());
        execs.add(new ExprEnumMostLeastFreqScalar());
        execs.add(new ExprEnumMostLeastFrequentInvalid());
        return execs;
    }

    private static class ExprEnumMostLeastFreqEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.mostFrequent(x => p00)");
            builder.expression(fields[1], "contained.leastFrequent(x => p00)");
            builder.expression(fields[2], "contained.mostFrequent( (x, i) => p00 + i*2)");
            builder.expression(fields[3], "contained.leastFrequent( (x, i) => p00 + i*2)");
            builder.expression(fields[4], "contained.mostFrequent( (x, i, s) => p00 + i*2 + s*4)");
            builder.expression(fields[5], "contained.leastFrequent( (x, i, s) => p00 + i*2 + s*4)");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypePremade.INTEGERBOXED.getEPType()));

            SupportBean_ST0_Container bean = SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2", "E3,12");
            builder.assertion(bean).expect(fields, 12, 11, 12, 12, 28, 28);

            bean = SupportBean_ST0_Container.make2Value("E1,12");
            builder.assertion(bean).expect(fields, 12, 12, 12, 12, 16, 16);

            bean = SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2", "E3,12", "E1,12", "E2,11", "E3,11");
            builder.assertion(bean).expect(fields, 12, 2, 12, 12, 40, 40);

            bean = SupportBean_ST0_Container.make2Value("E2,11", "E1,12", "E2,15", "E3,12", "E1,12", "E2,11", "E3,11");
            builder.assertion(bean).expect(fields, 11, 15, 11, 11, 39, 39);

            builder.assertion(SupportBean_ST0_Container.make2ValueNull()).expect(fields, null, null, null, null, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value()).expect(fields, null, null, null, null, null, null);

            builder.run(env);
        }
    }

    private static class ExprEnumMostLeastFreqScalarNoParam implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.mostFrequent()");
            builder.expression(fields[1], "strvals.leastFrequent()");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypePremade.STRING.getEPType()));

            builder.assertion(SupportCollection.makeString("E2,E1,E2,E1,E3,E3,E4,E3")).expect(fields, "E3", "E4");

            builder.assertion(SupportCollection.makeString("E1")).expect(fields, "E1", "E1");

            builder.assertion(SupportCollection.makeString(null)).expect(fields, null, null);

            builder.assertion(SupportCollection.makeString("")).expect(fields, null, null);

            builder.run(env);
        }
    }

    private static class ExprEnumMostLeastFreqScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.mostFrequent(v => extractNum(v))");
            builder.expression(fields[1], "strvals.leastFrequent(v => extractNum(v))");
            builder.expression(fields[2], "strvals.mostFrequent( (v, i) => extractNum(v) + i*10)");
            builder.expression(fields[3], "strvals.leastFrequent( (v, i) => extractNum(v) + i*10)");
            builder.expression(fields[4], "strvals.mostFrequent( (v, i, s) => extractNum(v) + i*10 + s*100)");
            builder.expression(fields[5], "strvals.leastFrequent( (v, i, s) => extractNum(v) + i*10 + s*100)");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypePremade.INTEGERBOXED.getEPType()));

            builder.assertion(SupportCollection.makeString("E2,E1,E2,E1,E3,E3,E4,E3")).expect(fields, 3, 4, 2, 2, 802, 802);

            builder.assertion(SupportCollection.makeString("E1")).expect(fields, 1, 1, 1, 1, 101, 101);

            builder.assertion(SupportCollection.makeString(null)).expect(fields, null, null, null, null, null, null);

            builder.assertion(SupportCollection.makeString("")).expect(fields, null, null, null, null, null, null);

            builder.run(env);
        }
    }

    private static class ExprEnumMostLeastFrequentInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select strvals.mostFrequent(v => null) from SupportCollection";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'strvals.mostFrequent()': Null-type is not allowed");
        }
    }
}
