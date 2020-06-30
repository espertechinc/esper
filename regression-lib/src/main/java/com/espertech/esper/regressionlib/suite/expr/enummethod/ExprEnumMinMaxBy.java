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
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.common.client.type.EPTypePremade.INTEGERBOXED;
import static com.espertech.esper.common.client.type.EPTypePremade.STRING;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypes;
import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypesAllSame;

public class ExprEnumMinMaxBy {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumMinMaxByEvents());
        execs.add(new ExprEnumMinMaxByScalar());
        execs.add(new ExprEnumMinMaxByInvalid());
        return execs;
    }

    private static class ExprEnumMinMaxByEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.minBy(x => p00)");
            builder.expression(fields[1], "contained.maxBy(x => p00)");
            builder.expression(fields[2], "contained.minBy(x => p00).id");
            builder.expression(fields[3], "contained.maxBy(x => p00).p00");
            builder.expression(fields[4], "contained.minBy( (x, i) => case when i < 1 then p00 else p00*10 end).p00");
            builder.expression(fields[5], "contained.maxBy( (x, i) => case when i < 1 then p00 else p00*10 end).p00");
            builder.expression(fields[6], "contained.minBy( (x, i, s) => case when i < 1 and s > 2 then p00 else p00*10 end).p00");
            builder.expression(fields[7], "contained.maxBy( (x, i, s) => case when i < 1 and s > 2 then p00 else p00*10 end).p00");

            builder.statementConsumer(stmt -> assertTypes(stmt.getEventType(), fields,
                new EPTypeClass[]{SupportBean_ST0.EPTYPE, SupportBean_ST0.EPTYPE, STRING.getEPType(), INTEGERBOXED.getEPType(),
                    INTEGERBOXED.getEPType(), INTEGERBOXED.getEPType(), INTEGERBOXED.getEPType(), INTEGERBOXED.getEPType()}));

            SupportBean_ST0_Container beanOne = SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2");
            builder.assertion(beanOne).expect(fields, beanOne.getContained().get(2), beanOne.getContained().get(0), "E2", 12, 12, 11, 12, 11);

            SupportBean_ST0_Container beanTwo = SupportBean_ST0_Container.make2Value("E1,12");
            builder.assertion(beanTwo).expect(fields, beanTwo.getContained().get(0), beanTwo.getContained().get(0), "E1", 12, 12, 12, 12, 12);

            builder.assertion(SupportBean_ST0_Container.make2ValueNull()).expect(fields, null, null, null, null, null, null, null, null);

            builder.assertion(SupportBean_ST0_Container.make2Value()).expect(fields, null, null, null, null, null, null, null, null);

            SupportBean_ST0_Container beanThree = SupportBean_ST0_Container.make2Value("E1,12", "E2,11");
            builder.assertion(beanThree).expect(fields, beanThree.getContained().get(1), beanThree.getContained().get(0), "E2", 12, 12, 11, 11, 12);

            builder.run(env);
        }
    }

    private static class ExprEnumMinMaxByScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.minBy(v => extractNum(v))");
            builder.expression(fields[1], "strvals.maxBy(v => extractNum(v))");
            builder.expression(fields[2], "strvals.minBy( (v, i) => extractNum(v) + i*10)");
            builder.expression(fields[3], "strvals.maxBy( (v, i) => extractNum(v) + i*10)");
            builder.expression(fields[4], "strvals.minBy( (v, i, s) => extractNum(v) + (case when s > 2 then i*10 else 0 end))");
            builder.expression(fields[5], "strvals.maxBy( (v, i, s) => extractNum(v) + (case when s > 2 then i*10 else 0 end))");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypePremade.STRING.getEPType()));

            builder.assertion(SupportCollection.makeString("E2,E1,E5,E4")).expect(fields, "E1", "E5", "E2", "E4", "E2", "E4");

            builder.assertion(SupportCollection.makeString("E1")).expect(fields, "E1", "E1", "E1", "E1", "E1", "E1");

            builder.assertion(SupportCollection.makeString(null)).expect(fields, null, null, null, null, null, null);

            builder.assertion(SupportCollection.makeString("")).expect(fields, null, null, null, null, null, null);

            builder.assertion(SupportCollection.makeString("E8,E2")).expect(fields, "E2", "E8", "E8", "E2", "E2", "E8");

            builder.run(env);
        }
    }

    private static class ExprEnumMinMaxByInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select contained.minBy(x => null) from SupportBean_ST0_Container";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'contained.minBy()': Null-type is not allowed");
        }
    }
}
