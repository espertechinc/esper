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
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalAssertionBuilder;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static com.espertech.esper.common.client.type.EPTypePremade.OBJECT;
import static com.espertech.esper.common.client.type.EPTypePremade.STRING;
import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypes;
import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypesAllSame;
import static org.junit.Assert.fail;

public class ExprEnumGroupBy {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumGroupByOneParamEvent());
        execs.add(new ExprEnumGroupByOneParamScalar());
        execs.add(new ExprEnumGroupByTwoParamEvent());
        execs.add(new ExprEnumGroupByTwoParamScalar());
        return execs;
    }

    private static class ExprEnumGroupByOneParamEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.groupBy(c => id)");
            builder.expression(fields[1], "contained.groupBy((c, i) => id || '_' || Integer.toString(i))");
            builder.expression(fields[2], "contained.groupBy((c, i, s) => id || '_' || Integer.toString(i) || '_' || Integer.toString(s))");
            builder.expression(fields[3], "contained.groupBy(c => null)");
            builder.expression(fields[4], "contained.groupBy((c, i) => case when i > 1 then null else id end)");

            EPTypeClass inner = EPTypeClassParameterized.from(Collection.class, SupportBean_ST0.class);
            EPTypeClass mapOfString = new EPTypeClassParameterized(Map.class, new EPTypeClass[] {STRING.getEPType(), inner});
            EPTypeClass mapOfObject = new EPTypeClassParameterized(Map.class, new EPTypeClass[] {OBJECT.getEPType(), inner});
            builder.statementConsumer(stmt -> assertTypes(stmt.getEventType(), fields, new EPTypeClass[] {mapOfString, mapOfString, mapOfString, mapOfObject, mapOfString}));

            EPAssertionUtil.AssertionCollectionValueString extractorEvents = new EPAssertionUtil.AssertionCollectionValueString() {
                public String extractValue(Object collectionItem) {
                    int p00 = ((SupportBean_ST0) collectionItem).getP00();
                    return Integer.toString(p00);
                }
            };

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E1,2", "E2,5"))
                .verify("c0", val -> compareMaps(val, "E1,E2", new String[]{"1,2", "5"}, extractorEvents))
                .verify("c1", val -> compareMaps(val, "E1_0,E1_1,E2_2", new String[]{"1", "2", "5"}, extractorEvents))
                .verify("c2", val -> compareMaps(val, "E1_0_3,E1_1_3,E2_2_3", new String[]{"1", "2", "5"}, extractorEvents))
                .verify("c3", val -> compareMaps(val, "null", new String[]{"1,2,5"}, extractorEvents))
                .verify("c4", val -> compareMaps(val, "E1,null", new String[]{"1,2", "5"}, extractorEvents));

            SupportEvalAssertionBuilder assertionNull = builder.assertion(SupportBean_ST0_Container.make2ValueNull());
            for (String field : fields) {
                assertionNull.verify(field, Assert::assertNull);
            }

            SupportEvalAssertionBuilder assertionEmpty = builder.assertion(SupportBean_ST0_Container.make2Value());
            for (String field : fields) {
                assertionEmpty.verify(field, val -> compareMaps(val, "", new String[0], extractorEvents));
            }

            builder.run(env);
        }
    }

    private static class ExprEnumGroupByOneParamScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.groupBy(c => extractAfterUnderscore(c))");
            builder.expression(fields[1], "strvals.groupBy((c, i) => extractAfterUnderscore(c) || '_' || Integer.toString(i))");
            builder.expression(fields[2], "strvals.groupBy((c, i, s) => extractAfterUnderscore(c) || '_' || Integer.toString(i) || '_' || Integer.toString(s))");

            EPTypeClass inner = EPTypeClassParameterized.from(Collection.class, String.class);
            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, new EPTypeClassParameterized(Map.class, new EPTypeClass[] {STRING.getEPType(), inner})));

            builder.assertion(SupportCollection.makeString("E1_2,E2_1,E3_2"))
                .verify("c0", val -> compareMaps(val, "2,1", new String[]{"E1_2,E3_2", "E2_1"}, getExtractorScalar()))
                .verify("c1", val -> compareMaps(val, "2_0,1_1,2_2", new String[]{"E1_2", "E2_1", "E3_2"}, getExtractorScalar()))
                .verify("c2", val -> compareMaps(val, "2_0_3,1_1_3,2_2_3", new String[]{"E1_2", "E2_1", "E3_2"}, getExtractorScalar()));

            SupportEvalAssertionBuilder assertionNull = builder.assertion(SupportCollection.makeString(null));
            for (String field : fields) {
                assertionNull.verify(field, Assert::assertNull);
            }

            SupportEvalAssertionBuilder assertionEmpty = builder.assertion(SupportCollection.makeString(""));
            for (String field : fields) {
                assertionEmpty.verify(field, val -> compareMaps(val, "", new String[0], getExtractorScalar()));
            }

            builder.run(env);
        }
    }

    private static class ExprEnumGroupByTwoParamEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.groupBy(k => id, v => p00)");
            builder.expression(fields[1], "contained.groupBy((k, i) => id || '_' || Integer.toString(i), (v, i) => p00 + i*10)");
            builder.expression(fields[2], "contained.groupBy((k, i, s) => id || '_' || Integer.toString(i) || '_' || Integer.toString(s), (v, i, s) => p00 + i*10 + s*100)");

            EPTypeClass inner = EPTypeClassParameterized.from(Collection.class, Integer.class);
            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, new EPTypeClassParameterized(Map.class, new EPTypeClass[] {STRING.getEPType(), inner})));

            EPAssertionUtil.AssertionCollectionValueString extractor = new EPAssertionUtil.AssertionCollectionValueString() {
                public String extractValue(Object collectionItem) {
                    int p00 = (Integer) collectionItem;
                    return Integer.toString(p00);
                }
            };

            builder.assertion(SupportBean_ST0_Container.make2Value("E1,1", "E1,2", "E2,5"))
                .verify("c0", val -> compareMaps(val, "E1,E2", new String[]{"1,2", "5"}, extractor))
                .verify("c1", val -> compareMaps(val, "E1_0,E1_1,E2_2", new String[]{"1", "12", "25"}, extractor))
                .verify("c2", val -> compareMaps(val, "E1_0_3,E1_1_3,E2_2_3", new String[]{"301", "312", "325"}, extractor));

            SupportEvalAssertionBuilder assertionNull = builder.assertion(SupportBean_ST0_Container.make2ValueNull());
            for (String field : fields) {
                assertionNull.verify(field, Assert::assertNull);
            }

            SupportEvalAssertionBuilder assertionEmpty = builder.assertion(SupportBean_ST0_Container.make2Value());
            for (String field : fields) {
                assertionEmpty.verify(field, val -> compareMaps(val, "", new String[0], getExtractorScalar()));
            }

            builder.run(env);
        }
    }

    private static class ExprEnumGroupByTwoParamScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.groupBy(k => extractAfterUnderscore(k), v => v)");
            builder.expression(fields[1], "strvals.groupBy((k, i) => extractAfterUnderscore(k) || '_' || Integer.toString(i), (v, i) => v || '_' || Integer.toString(i))");
            builder.expression(fields[2], "strvals.groupBy((k, i, s) => extractAfterUnderscore(k) || '_' || Integer.toString(i) || '_' || Integer.toString(s), (v, i, s) => v || '_' || Integer.toString(i) || '_' || Integer.toString(s))");

            EPTypeClass inner = EPTypeClassParameterized.from(Collection.class, String.class);
            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, new EPTypeClassParameterized(Map.class, new EPTypeClass[] {STRING.getEPType(), inner})));

            builder.assertion(SupportCollection.makeString("E1_2,E2_1,E3_2"))
                .verify("c0", val -> compareMaps(val, "2,1", new String[]{"E1_2,E3_2", "E2_1"}, getExtractorScalar()))
                .verify("c1", val -> compareMaps(val, "2_0,1_1,2_2", new String[]{"E1_2_0", "E2_1_1", "E3_2_2"}, getExtractorScalar()))
                .verify("c2", val -> compareMaps(val, "2_0_3,1_1_3,2_2_3", new String[]{"E1_2_0_3", "E2_1_1_3", "E3_2_2_3"}, getExtractorScalar()));

            SupportEvalAssertionBuilder assertionNull = builder.assertion(SupportCollection.makeString(null));
            for (String field : fields) {
                assertionNull.verify(field, Assert::assertNull);
            }

            SupportEvalAssertionBuilder assertionEmpty = builder.assertion(SupportCollection.makeString(""));
            for (String field : fields) {
                assertionEmpty.verify(field, val -> compareMaps(val, "", new String[0], getExtractorScalar()));
            }

            builder.run(env);
        }
    }

    public static String extractAfterUnderscore(String string) {
        int indexUnderscore = string.indexOf("_");
        if (indexUnderscore == -1) {
            fail();
        }
        return string.substring(indexUnderscore + 1);
    }

    private static EPAssertionUtil.AssertionCollectionValueString getExtractorScalar() {
        return new EPAssertionUtil.AssertionCollectionValueString() {
            public String extractValue(Object collectionItem) {
                return collectionItem.toString();
            }
        };
    }

    private static void compareMaps(Object val, String keyCSV, String[] values, EPAssertionUtil.AssertionCollectionValueString extractorEvents) {
        String[] keys = keyCSV.isEmpty() ? new String[0] : keyCSV.split(",");
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals("null")) {
                keys[i] = null;
            }
        }
        EPAssertionUtil.assertMapOfCollection((Map) val, keys, values, extractorEvents);
    }
}
