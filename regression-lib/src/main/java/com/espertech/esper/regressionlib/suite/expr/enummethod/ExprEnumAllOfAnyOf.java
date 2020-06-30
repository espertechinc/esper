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
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypesAllSame;

public class ExprEnumAllOfAnyOf {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumAllOfAnyOfEvents());
        execs.add(new ExprEnumAllOfAnyOfScalar());
        execs.add(new ExprEnumAllOfAnyOfInvalid());
        return execs;
    }

    private static class ExprEnumAllOfAnyOfInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;
            epl = "select contained.allOf(x => 1) from SupportBean_ST0_Container";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'contained.allOf()': Failed to validate enumeration method 'allOf', expected a boolean-type result for expression parameter 0 but received int");

            epl = "select contained.anyOf(x => 1) from SupportBean_ST0_Container";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'contained.anyOf()': Failed to validate enumeration method 'anyOf', expected a boolean-type result for expression parameter 0 but received int");

            epl = "select contained.anyOf(x => null) from SupportBean_ST0_Container";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'contained.anyOf()': Failed to validate enumeration method 'anyOf', expected a non-null result for expression parameter 0 but received a null-typed expression");
        }
    }

    private static class ExprEnumAllOfAnyOfEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.allof(v => p00 = 7)");
            builder.expression(fields[1], "contained.anyof(v => p00 = 7)");
            builder.expression(fields[2], "contained.allof((v, i) => p00 = (7 + i*10))");
            builder.expression(fields[3], "contained.anyof((v, i) => p00 = (7 + i*10))");
            builder.expression(fields[4], "contained.allof((v, i, s) => p00 = (7 + i*10 + s*100))");
            builder.expression(fields[5], "contained.anyof((v, i, s) => p00 = (7 + i*10 + s*100))");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypePremade.BOOLEANBOXED.getEPType()));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,7", "E3,2"))
                .expect(fields, false, true, false, false, false, false);

            builder.assertion(SupportBean_ST0_Container.make2ValueNull())
                .expect(fields, null, null, null, null, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,7", "E2,7", "E3,7"))
                .expect(fields, true, true, false, true, false, false);

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,0", "E2,0", "E3,0"))
                .expect(fields, false, false, false, false, false, false);

            builder.assertion(SupportBean_ST0_Container.make2Value())
                .expect(fields, true, false, true, false, true, false);

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E2,2", "E3,327"))
                .expect(fields, false, false, false, false, false, true);

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,307", "E2,317", "E3,327"))
                .expect(fields, false, false, false, false, true, true);

            builder.run(env);
        }
    }

    private static class ExprEnumAllOfAnyOfScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.allof(v => v='A')");
            builder.expression(fields[1], "strvals.anyof(v => v='A')");
            builder.expression(fields[2], "strvals.allof((v, i) => (v='A' and i < 2) or (v='C' and i >= 2))");
            builder.expression(fields[3], "strvals.anyof((v, i) => (v='A' and i < 2) or (v='C' and i >= 2))");
            builder.expression(fields[4], "strvals.allof((v, i, s) => (v='A' and i < s - 2) or (v='C' and i >= s - 2))");
            builder.expression(fields[5], "strvals.anyof((v, i, s) => (v='A' and i < s - 2) or (v='C' and i >= s - 2))");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypePremade.BOOLEANBOXED.getEPType()));

            builder.assertion(SupportCollection.makeString("B,A,C"))
                .expect(fields, false, true, false, true, false, true);

            builder.assertion(SupportCollection.makeString(null))
                .expect(fields, null, null, null, null, null, null);

            builder.assertion(SupportCollection.makeString("A,A"))
                .expect(fields, true, true, true, true, false, false);

            builder.assertion(SupportCollection.makeString("B"))
                .expect(fields, false, false, false, false, false, false);

            builder.assertion(SupportCollection.makeString(""))
                .expect(fields, true, false, true, false, true, false);

            builder.assertion(SupportCollection.makeString("B,B,B"))
                .expect(fields, false, false, false, false, false, false);

            builder.assertion(SupportCollection.makeString("A,A,C,C"))
                .expect(fields, false, true, true, true, true, true);

            builder.run(env);
        }
    }
}
