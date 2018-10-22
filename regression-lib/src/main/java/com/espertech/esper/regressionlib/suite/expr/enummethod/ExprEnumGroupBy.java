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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExprEnumGroupBy {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumKeySelectorOnly());
        execs.add(new ExprEnumKeyValueSelector());
        return execs;
    }

    private static class ExprEnumKeySelectorOnly implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // - duplicate key allowed, creates a list of values
            // - null key & value allowed

            String eplFragment = "@name('s0') select contained.groupBy(c => id) as val from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val".split(","), new Class[]{Map.class});
            EPAssertionUtil.AssertionCollectionValueString extractorEvents = new EPAssertionUtil.AssertionCollectionValueString() {
                public String extractValue(Object collectionItem) {
                    int p00 = ((SupportBean_ST0) collectionItem).getP00();
                    return Integer.toString(p00);
                }
            };

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E1,2", "E2,5"));
            EPAssertionUtil.assertMapOfCollection((Map) env.listener("s0").assertOneGetNewAndReset().get("val"), "E1,E2".split(","),
                new String[]{"1,2", "5"}, extractorEvents);

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("val"));

            env.sendEventBean(SupportBean_ST0_Container.make2Value());
            assertEquals(0, ((Map) env.listener("s0").assertOneGetNewAndReset().get("val")).size());
            env.undeployAll();

            // test scalar
            String eplScalar = "@name('s0') select strvals.groupBy(c => extractAfterUnderscore(c)) as val from SupportCollection";
            env.compileDeploy(eplScalar).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val".split(","), new Class[]{Map.class});

            env.sendEventBean(SupportCollection.makeString("E1_2,E2_1,E3_2"));
            EPAssertionUtil.assertMapOfCollection((Map) env.listener("s0").assertOneGetNewAndReset().get("val"), "2,1".split(","),
                new String[]{"E1_2,E3_2", "E2_1"}, getExtractorScalar());

            env.sendEventBean(SupportCollection.makeString(null));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("val"));

            env.sendEventBean(SupportCollection.makeString(""));
            assertEquals(0, ((Map) env.listener("s0").assertOneGetNewAndReset().get("val")).size());

            env.undeployAll();
        }
    }

    private static class ExprEnumKeyValueSelector implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplFragment = "@name('s0') select contained.groupBy(k => id, v => p00) as val from SupportBean_ST0_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            EPAssertionUtil.AssertionCollectionValueString extractor = new EPAssertionUtil.AssertionCollectionValueString() {
                public String extractValue(Object collectionItem) {
                    int p00 = (Integer) collectionItem;
                    return Integer.toString(p00);
                }
            };

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E1,2", "E2,5"));
            EPAssertionUtil.assertMapOfCollection((Map) env.listener("s0").assertOneGetNewAndReset().get("val"), "E1,E2".split(","),
                new String[]{"1,2", "5"}, extractor);

            env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("val"));

            env.sendEventBean(SupportBean_ST0_Container.make2Value());
            assertEquals(0, ((Map) env.listener("s0").assertOneGetNewAndReset().get("val")).size());

            env.undeployModuleContaining("s0");

            // test scalar
            String eplScalar = "@name('s0') select strvals.groupBy(k => extractAfterUnderscore(k), v => v) as val from SupportCollection";
            env.compileDeploy(eplScalar).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val".split(","), new Class[]{Map.class});

            env.sendEventBean(SupportCollection.makeString("E1_2,E2_1,E3_2"));
            EPAssertionUtil.assertMapOfCollection((Map) env.listener("s0").assertOneGetNewAndReset().get("val"), "2,1".split(","),
                new String[]{"E1_2,E3_2", "E2_1"}, getExtractorScalar());

            env.sendEventBean(SupportCollection.makeString(null));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("val"));

            env.sendEventBean(SupportCollection.makeString(""));
            assertEquals(0, ((Map) env.listener("s0").assertOneGetNewAndReset().get("val")).size());

            env.undeployAll();
        }
    }

    public static String extractAfterUnderscore(String string) {
        int indexUnderscore = string.indexOf("_");
        if (indexUnderscore == -1) {
            Assert.fail();
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
}
