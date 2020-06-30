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
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.common.client.type.EPTypePremade.INTEGERBOXED;
import static com.espertech.esper.common.client.type.EPTypePremade.STRING;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

public class ExprEnumAggregate {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumAggregateEvents());
        execs.add(new ExprEnumAggregateScalar());
        execs.add(new ExprEnumAggregateInvalid());
        return execs;
    }

    private static class ExprEnumAggregateInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            // invalid incompatible params
            epl = "select contained.aggregate(0, (result, item) => result || ',') from SupportBean_ST0_Container";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'contained.aggregate(0,)': Failed to validate enumeration method 'aggregate' parameter 1: Failed to validate declared expression body expression 'result||\",\"': Implicit conversion from datatype 'Integer' to string is not allowed");

            // null-init-value for aggregate
            epl = "select contained.aggregate(null, (result, item) => result) from SupportBean_ST0_Container";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'contained.aggregate(null,)': Initialization value is null-typed");
        }
    }

    private static class ExprEnumAggregateEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.aggregate(0, (result, item) => result + item.p00)");
            builder.expression(fields[1], "contained.aggregate('', (result, item) => result || ', ' || item.id)");
            builder.expression(fields[2], "contained.aggregate('', (result, item) => result || (case when result='' then '' else ',' end) || item.id)");
            builder.expression(fields[3], "contained.aggregate(0, (result, item, i) => result + item.p00 + i*10)");
            builder.expression(fields[4], "contained.aggregate(0, (result, item, i, s) => result + item.p00 + i*10 + s*100)");
            builder.expression(fields[5], "contained.aggregate(0, (result, item) => null)");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypes(stmt.getEventType(), fields, new EPTypeClass[]{INTEGERBOXED.getEPType(), STRING.getEPType(), STRING.getEPType(), INTEGERBOXED.getEPType(), INTEGERBOXED.getEPType(), INTEGERBOXED.getEPType(), INTEGERBOXED.getEPType()}));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2"))
                .expect(fields, 25, ", E1, E2, E2", "E1,E2,E2", 12 + 21 + 22, 312 + 321 + 322, null);

            builder.assertion(SupportBean_ST0_Container.make2ValueNull())
                .expect(fields, null, null, null, null, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value())
                .expect(fields, 0, "", "", 0, 0, 0);

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,12"))
                .expect(fields, 12, ", E1", "E1", 12, 112, null);

            builder.run(env);
        }
    }

    private static class ExprEnumAggregateScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.aggregate('', (result, item) => result || '+' || item)");
            builder.expression(fields[1], "strvals.aggregate('', (result, item, i) => result || '+' || item || '_' || Integer.toString(i))");
            builder.expression(fields[2], "strvals.aggregate('', (result, item, i, s) => result || '+' || item || '_' || Integer.toString(i) || '_' || Integer.toString(s))");
            builder.expression(fields[3], "strvals.aggregate('', (result, item, i, s) => null)");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, STRING.getEPType()));

            builder.assertion(SupportCollection.makeString("E1,E2,E3"))
                .expect(fields, "+E1+E2+E3", "+E1_0+E2_1+E3_2", "+E1_0_3+E2_1_3+E3_2_3", null);

            builder.assertion(SupportCollection.makeString("E1")).expect(fields, "+E1", "+E1_0", "+E1_0_1", null);

            builder.assertion(SupportCollection.makeString("")).expect(fields, "", "", "", "");

            builder.assertion(SupportCollection.makeString(null)).expect(fields, null, null, null, null);

            builder.run(env);
        }
    }
}
