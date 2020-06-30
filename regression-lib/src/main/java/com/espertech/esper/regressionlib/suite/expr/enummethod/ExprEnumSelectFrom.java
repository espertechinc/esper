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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypes;
import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypesAllSame;

public class ExprEnumSelectFrom {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumSelectFromEventsPlain());
        execs.add(new ExprEnumSelectFromEventsWIndexWSize());
        execs.add(new ExprEnumSelectFromEventsWithNew());
        execs.add(new ExprEnumSelectFromScalarPlain());
        execs.add(new ExprEnumSelectFromScalarWIndexWSize());
        return execs;
    }

    private static class ExprEnumSelectFromScalarWIndexWSize implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.selectFrom( (v, i) => v || '_' || Integer.toString(i))");
            builder.expression(fields[1], "strvals.selectFrom( (v, i, s) => v || '_' || Integer.toString(i) || '_' || Integer.toString(s))");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, String.class)));

            builder.assertion(SupportCollection.makeString("E1,E2,E3"))
                .verify(fields[0], value -> LambdaAssertionUtil.assertValuesArrayScalar(value, "E1_0", "E2_1", "E3_2"))
                .verify(fields[1], value -> LambdaAssertionUtil.assertValuesArrayScalar(value, "E1_0_3", "E2_1_3", "E3_2_3"));

            builder.assertion(SupportCollection.makeString(""))
                .verify(fields[0], value -> LambdaAssertionUtil.assertValuesArrayScalar(value))
                .verify(fields[1], value -> LambdaAssertionUtil.assertValuesArrayScalar(value));

            builder.assertion(SupportCollection.makeString("E1"))
                .verify(fields[0], value -> LambdaAssertionUtil.assertValuesArrayScalar(value, "E1_0"))
                .verify(fields[1], value -> LambdaAssertionUtil.assertValuesArrayScalar(value, "E1_0_1"));

            builder.assertion(SupportCollection.makeString(null))
                .verify(fields[0], Assert::assertNull)
                .verify(fields[1], Assert::assertNull);

            builder.run(env);
        }
    }

    private static class ExprEnumSelectFromEventsWIndexWSize implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.selectFrom( (v, i) => new {v0=v.id,v1=i})");
            builder.expression(fields[1], "contained.selectFrom( (v, i, s) => new {v0=v.id,v1=i + 100*s})");

            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, EPTypeClassParameterized.from(Map.class, String.class, Object.class))));

            builder.assertion(SupportBean_ST0_Container.make3Value("E1,12,0", "E2,11,0", "E3,2,0"))
                .verify(fields[0], value -> assertRows(value, new Object[][]{{"E1", 0}, {"E2", 1}, {"E3", 2}}))
                .verify(fields[1], value -> assertRows(value, new Object[][]{{"E1", 300}, {"E2", 301}, {"E3", 302}}));

            builder.assertion(SupportBean_ST0_Container.make3Value("E4,0,1"))
                .verify(fields[0], value -> assertRows(value, new Object[][]{{"E4", 0}}))
                .verify(fields[1], value -> assertRows(value, new Object[][]{{"E4", 100}}));

            builder.assertion(SupportBean_ST0_Container.make3ValueNull())
                .verify(fields[0], value -> assertRows(value, null))
                .verify(fields[1], value -> assertRows(value, null));

            builder.assertion(SupportBean_ST0_Container.make3Value())
                .verify(fields[0], value -> assertRows(value, new Object[0][]))
                .verify(fields[1], value -> assertRows(value, new Object[0][]));

            builder.run(env);
        }

        private void assertRows(Object value, Object[][] expected) {
            EPAssertionUtil.assertPropsPerRow(toMapArray(value), "v0,v1".split(","), expected);
        }
    }

    private static class ExprEnumSelectFromEventsWithNew implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String field = "c0";
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(field, "contained.selectFrom(x => new {c0 = id||'x', c1 = key0||'y'})");

            builder.statementConsumer(stmt -> assertTypes(stmt.getEventType(), field, EPTypeClassParameterized.from(Collection.class, EPTypeClassParameterized.from(Map.class, String.class, Object.class))));

            builder.assertion(SupportBean_ST0_Container.make3Value("E1,12,0", "E2,11,0", "E3,2,0"))
                .verify(field, value -> assertRows(value, new Object[][]{{"E1x", "12y"}, {"E2x", "11y"}, {"E3x", "2y"}}));

            builder.assertion(SupportBean_ST0_Container.make3Value("E4,0,1"))
                .verify(field, value -> assertRows(value, new Object[][]{{"E4x", "0y"}}));

            builder.assertion(SupportBean_ST0_Container.make3ValueNull())
                .verify(field, value -> assertRows(value, null));

            builder.assertion(SupportBean_ST0_Container.make3Value())
                .verify(field, value -> assertRows(value, new Object[0][]));

            builder.run(env);
        }

        private void assertRows(Object value, Object[][] expected) {
            EPAssertionUtil.assertPropsPerRow(toMapArray(value), "c0,c1".split(","), expected);
        }
    }

    private static class ExprEnumSelectFromEventsPlain implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.selectFrom(x => id)");
            builder.expression(fields[1], "contained.selectFrom(x => null)");

            builder.statementConsumer(stmt -> assertTypes(stmt.getEventType(), fields, new EPTypeClass[] {
                EPTypeClassParameterized.from(Collection.class, String.class), EPTypeClassParameterized.from(Collection.class, Object.class)
            }));

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E3,2"))
                .verify(fields[0], value -> LambdaAssertionUtil.assertValuesArrayScalar(value, "E1", "E2", "E3"))
                .verify(fields[1], value -> LambdaAssertionUtil.assertValuesArrayScalar(value));

            builder.assertion(SupportBean_ST0_Container.make2ValueNull())
                .verify(fields[0], Assert::assertNull)
                .verify(fields[1], Assert::assertNull);

            builder.assertion(SupportBean_ST0_Container.make2Value())
                .verify(fields[0], value -> LambdaAssertionUtil.assertValuesArrayScalar(value))
                .verify(fields[1], value -> LambdaAssertionUtil.assertValuesArrayScalar(value));

            builder.run(env);
        }
    }

    private static class ExprEnumSelectFromScalarPlain implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String field = "c0";
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(field, "strvals.selectFrom(v => extractNum(v))");

            builder.statementConsumer(stmt -> assertTypes(stmt.getEventType(), field, EPTypeClassParameterized.from(Collection.class, Integer.class)));

            builder.assertion(SupportCollection.makeString("E2,E1,E5,E4"))
                .verify(field, value -> LambdaAssertionUtil.assertValuesArrayScalar(value, 2, 1, 5, 4));

            builder.assertion(SupportCollection.makeString("E1"))
                .verify(field, value -> LambdaAssertionUtil.assertValuesArrayScalar(value, 1));

            builder.assertion(SupportCollection.makeString(null))
                .verify(field, Assert::assertNull);

            builder.assertion(SupportCollection.makeString(""))
                .verify(field, value -> LambdaAssertionUtil.assertValuesArrayScalar(value));

            builder.run(env);
        }
    }

    private static Map<String, Object>[] toMapArray(Object result) {
        if (result == null) {
            return null;
        }
        Collection<Map<String, Object>> val = (Collection<Map<String, Object>>) result;
        return val.toArray(new Map[val.size()]);
    }
}
