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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypesAllSame;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

public class ExprEnumToMap {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumToMapEvent());
        execs.add(new ExprEnumToMapScalar());
        execs.add(new ExprEnumToMapInvalid());
        return execs;
    }

    private static class ExprEnumToMapEvent implements RegressionExecution {

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.toMap(c => id, d=> p00)");
            builder.expression(fields[1], "contained.toMap((c, index) => id || '_' || Integer.toString(index), (d, index) => p00 + 10*index)");
            builder.expression(fields[2], "contained.toMap((c, index, size) => id || '_' || Integer.toString(index) || '_' || Integer.toString(size), (d, index, size) => p00 + 10*index + 100*size)");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Map.class, String.class, Integer.class)));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E3,12", "E2,5"))
                .verify("c0", val -> compareMap(val, "E1,E3,E2", 1, 12, 5))
                .verify("c1", val -> compareMap(val, "E1_0,E3_1,E2_2", 1, 22, 25))
                .verify("c2", val -> compareMap(val, "E1_0_3,E3_1_3,E2_2_3", 301, 322, 325));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E3,4", "E2,7", "E1,2"))
                .verify("c0", val -> compareMap(val, "E1,E3,E2", 2, 4, 7))
                .verify("c1", val -> compareMap(val, "E1_0,E3_1,E2_2,E1_3", 1, 14, 27, 32))
                .verify("c2", val -> compareMap(val, "E1_0_4,E3_1_4,E2_2_4,E1_3_4", 401, 414, 427, 432));

            builder.assertion(new SupportBean_ST0_Container(Collections.singletonList(new SupportBean_ST0(null, null))))
                .verify("c0", val -> compareMap(val, "E1,E2,E3", null, null, null))
                .verify("c1", val -> compareMap(val, "E1,E2,E3", null, null, null))
                .verify("c2", val -> compareMap(val, "E1,E2,E3", null, null, null));

            builder.run(env);
        }
    }

    private static class ExprEnumToMapScalar implements RegressionExecution {

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.toMap(k => k, v => extractNum(v))");
            builder.expression(fields[1], "strvals.toMap((k, i) => k || '_' || Integer.toString(i), (v, idx) => extractNum(v) + 10*idx)");
            builder.expression(fields[2], "strvals.toMap((k, i, s) => k || '_' || Integer.toString(i) || '_' || Integer.toString(s), (v, idx, sz) => extractNum(v) + 10*idx + 100*sz)");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Map.class, String.class, Integer.class)));

            builder.assertion(SupportCollection.makeString("E2,E1,E3"))
                .verify("c0", val -> compareMap(val, "E1,E2,E3", 1, 2, 3))
                .verify("c1", val -> compareMap(val, "E1_1,E2_0,E3_2", 11, 2, 23))
                .verify("c2", val -> compareMap(val, "E1_1_3,E2_0_3,E3_2_3", 311, 302, 323));

            builder.assertion(SupportCollection.makeString("E1"))
                .verify("c0", val -> compareMap(val, "E1", 1))
                .verify("c1", val -> compareMap(val, "E1_0", 1))
                .verify("c2", val -> compareMap(val, "E1_0_1", 101));

            builder.assertion(SupportCollection.makeString(null))
                .verify("c0", Assert::assertNull)
                .verify("c1", Assert::assertNull)
                .verify("c2", Assert::assertNull);

            builder.assertion(SupportCollection.makeString(""))
                .verify("c0", val -> compareMap(val, ""))
                .verify("c1", val -> compareMap(val, ""))
                .verify("c2", val -> compareMap(val, ""));

            builder.run(env);
        }
    }

    private static class ExprEnumToMapInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "select strvals.toMap(k => k, (v, i) => extractNum(v)) from SupportCollection";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'strvals.toMap(,)': Parameters mismatch for enumeration method 'toMap', the method requires a lambda expression providing key-selector and a lambda expression providing value-selector, but receives a lambda expression and a 2-parameter lambda expression");
        }
    }

    private static void compareMap(Object received, String keyCSV, Object... values) {
        String[] keys = keyCSV.isEmpty() ? new String[0] : keyCSV.split(",");
        EPAssertionUtil.assertPropsMap((Map) received, keys, values);
    }
};
