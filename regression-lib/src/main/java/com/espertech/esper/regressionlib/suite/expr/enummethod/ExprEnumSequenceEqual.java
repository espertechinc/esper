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

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypesAllSame;

public class ExprEnumSequenceEqual {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumSequenceEqualWSelectFrom());
        execs.add(new ExprEnumSequenceEqualTwoProperties());
        execs.add(new ExprEnumSequenceEqualInvalid());
        return execs;
    }

    private static class ExprEnumSequenceEqualWSelectFrom implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.selectFrom(x => key0).sequenceEqual(contained.selectFrom(y => id))");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypePremade.BOOLEANBOXED.getEPType()));

            builder.assertion(SupportBean_ST0_Container.make3Value("I1,E1,0", "I2,E2,0")).expect(fields, false);

            builder.assertion(SupportBean_ST0_Container.make3Value("I3,I3,0", "X4,X4,0")).expect(fields, true);

            builder.assertion(SupportBean_ST0_Container.make3Value("I3,I3,0", "X4,Y4,0")).expect(fields, false);

            builder.assertion(SupportBean_ST0_Container.make3Value("I3,I3,0", "Y4,X4,0")).expect(fields, false);

            builder.run(env);
        }
    }

    private static class ExprEnumSequenceEqualTwoProperties implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.sequenceEqual(strvalstwo)");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypePremade.BOOLEANBOXED.getEPType()));

            builder.assertion(SupportCollection.makeString("E1,E2,E3", "E1,E2,E3")).expect(fields, true);

            builder.assertion(SupportCollection.makeString("E1,E3", "E1,E2,E3")).expect(fields, false);

            builder.assertion(SupportCollection.makeString("E1,E3", "E1,E3")).expect(fields, true);

            builder.assertion(SupportCollection.makeString("E1,E2,E3", "E1,E3")).expect(fields, false);

            builder.assertion(SupportCollection.makeString("E1,E2,null,E3", "E1,E2,null,E3")).expect(fields, true);

            builder.assertion(SupportCollection.makeString("E1,E2,E3", "E1,E2,null")).expect(fields, false);

            builder.assertion(SupportCollection.makeString("E1,E2,null", "E1,E2,E3")).expect(fields, false);

            builder.assertion(SupportCollection.makeString("E1", "")).expect(fields, false);

            builder.assertion(SupportCollection.makeString("", "E1")).expect(fields, false);

            builder.assertion(SupportCollection.makeString("E1", "E1")).expect(fields, true);

            builder.assertion(SupportCollection.makeString("", "")).expect(fields, true);

            builder.assertion(SupportCollection.makeString(null, "")).expect(fields, new Object[] {null});

            builder.assertion(SupportCollection.makeString("", null)).expect(fields, false);

            builder.assertion(SupportCollection.makeString(null, null)).expect(fields, new Object[] {null});

            builder.run(env);
        }
    }

    private static class ExprEnumSequenceEqualInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select window(*).sequenceEqual(strvals) from SupportCollection#lastevent";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'window(*).sequenceEqual(strvals)': Invalid input for built-in enumeration method 'sequenceEqual' and 1-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type 'SupportCollection'");
        }
    }
}