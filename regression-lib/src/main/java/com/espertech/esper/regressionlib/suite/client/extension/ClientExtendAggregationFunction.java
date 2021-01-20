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
package com.espertech.esper.regressionlib.suite.client.extension;

import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionValidationContext;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.extend.aggfunc.SupportLowerUpperCompareAggregationFunction;
import com.espertech.esper.regressionlib.support.extend.aggfunc.SupportLowerUpperCompareAggregationFunctionForge;
import com.espertech.esper.regressionlib.support.extend.aggfunc.SupportSupportBeanAggregationFunctionFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ClientExtendAggregationFunction {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientExtendAggregationManagedWindow());
        execs.add(new ClientExtendAggregationManagedGrouped());
        execs.add(new ClientExtendAggregationManagedDistinctAndStarParam());
        execs.add(new ClientExtendAggregationManagedDotMethod());
        execs.add(new ClientExtendAggregationManagedMappedPropertyLookAlike());
        execs.add(new ClientExtendAggregationMultiParamMulti());
        execs.add(new ClientExtendAggregationMultiParamNoParam());
        execs.add(new ClientExtendAggregationMultiParamSingleArray());
        execs.add(new ClientExtendAggregationCodegeneratedCount());
        execs.add(new ClientExtendAggregationFailedValidation());
        execs.add(new ClientExtendAggregationInvalidUse());
        execs.add(new ClientExtendAggregationInvalidCannotResolve());
        execs.add(new ClientExtendAggregationTable());
        return execs;
    }

    private static class ClientExtendAggregationTable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create table MyTable(col1 concatstring(string));\n" +
                "into table MyTable select concatstring(theString) as col1 from SupportBean;\n";
            env.compileDeploy(epl, path);

            env.sendEventBean(new SupportBean("E1", 0));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 0));
            env.assertThat(() -> assertEquals("E1 E2", env.compileExecuteFAF("select col1 from MyTable", path).getArray()[0].get("col1")));

            env.undeployAll();
        }
    }

    private static class ClientExtendAggregationCodegeneratedCount implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select concatWCodegen(theString) as val from SupportBean";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            env.assertEqualsNew("s0", "val", "E1");

            env.sendEventBean(new SupportBean("E2", 0));
            env.assertEqualsNew("s0", "val", "E1E2");

            env.undeployAll();
        }
    }

    private static class ClientExtendAggregationMultiParamSingleArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream countback({1,2,intPrimitive}) as val from SupportBean";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportBean());
            assertPairSingleRow(env, new Object[]{-1}, new Object[]{0});

            env.undeployAll();
        }
    }

    private static class ClientExtendAggregationManagedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream concatstring(theString) as val from SupportBean#length(2)";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportBean("a", -1));
            assertPairSingleRow(env, new Object[]{"a"}, new Object[]{""});

            env.sendEventBean(new SupportBean("b", -1));
            assertPairSingleRow(env, new Object[]{"a b"}, new Object[]{"a"});

            env.milestone(0);

            env.sendEventBean(new SupportBean("c", -1));
            assertPairSingleRow(env, new Object[]{"b c"}, new Object[]{"a b"});

            env.sendEventBean(new SupportBean("d", -1));
            assertPairSingleRow(env, new Object[]{"c d"}, new Object[]{"b c"});

            env.undeployAll();
        }
    }

    private static class ClientExtendAggregationManagedGrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String textOne = "@name('s0') select irstream CONCATSTRING(theString) as val from SupportBean#length(10) group by intPrimitive";
            tryGrouped(env, textOne, null, milestone);

            String textTwo = "@name('s0') select irstream concatstring(theString) as val from SupportBean#win:length(10) group by intPrimitive";
            tryGrouped(env, textTwo, null, milestone);

            String textThree = "@name('s0') select irstream concatstring(theString) as val from SupportBean#length(10) group by intPrimitive";
            EPStatementObjectModel model = env.eplToModel(textThree);
            SerializableObjectCopier.copyMayFail(model);
            assertEquals(textThree, model.toEPL());
            tryGrouped(env, null, model, milestone);

            String textFour = "select irstream concatstring(theString) as val from SupportBean#length(10) group by intPrimitive";
            EPStatementObjectModel modelTwo = new EPStatementObjectModel();
            modelTwo.setSelectClause(SelectClause.create().streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
                .add(Expressions.plugInAggregation("concatstring", Expressions.property("theString")), "val"));
            modelTwo.setFromClause(FromClause.create(FilterStream.create("SupportBean").addView(null, "length", Expressions.constant(10))));
            modelTwo.setGroupByClause(GroupByClause.create("intPrimitive"));
            assertEquals(textFour, modelTwo.toEPL());
            SerializableObjectCopier.copyMayFail(modelTwo);
            modelTwo.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            tryGrouped(env, null, modelTwo, milestone);

            env.undeployAll();
        }

        private void tryGrouped(RegressionEnvironment env, String text, EPStatementObjectModel model, AtomicInteger milestone) {
            if (model != null) {
                env.compileDeploy(model);
            } else {
                env.compileDeploy(text);
            }
            env.addListener("s0");

            env.sendEventBean(new SupportBean("a", 1));
            assertPairSingleRow(env, new Object[]{"a"}, new Object[]{""});

            env.sendEventBean(new SupportBean("b", 2));
            assertPairSingleRow(env, new Object[]{"b"}, new Object[]{""});

            env.sendEventBean(new SupportBean("c", 1));
            assertPairSingleRow(env, new Object[]{"a c"}, new Object[]{"a"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("d", 2));
            assertPairSingleRow(env, new Object[]{"b d"}, new Object[]{"b"});

            env.sendEventBean(new SupportBean("e", 1));
            assertPairSingleRow(env, new Object[]{"a c e"}, new Object[]{"a c"});

            env.sendEventBean(new SupportBean("f", 2));
            assertPairSingleRow(env, new Object[]{"b d f"}, new Object[]{"b d"});

            env.undeployModuleContaining("s0");
        }
    }

    private static class ClientExtendAggregationManagedDistinctAndStarParam implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            // test *-parameter
            String textTwo = "@name('s0') select concatstring(*) as val from SupportBean";
            env.compileDeploy(textTwo).addListener("s0");

            env.sendEventBean(new SupportBean("d", -1));
            env.assertPropsNew("s0", "val".split(","), new Object[]{"SupportBean(d, -1)"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("e", 2));
            env.assertPropsNew("s0", "val".split(","), new Object[]{"SupportBean(d, -1) SupportBean(e, 2)"});

            env.tryInvalidCompile("select concatstring(*) as val from SupportBean#lastevent, SupportBean unidirectional",
                "Failed to validate select-clause expression 'concatstring(*)': The 'concatstring' aggregation function requires that in joins or subqueries the stream-wildcard (stream-alias.*) syntax is used instead");
            env.undeployAll();

            // test distinct
            String text = "@name('s0') select irstream concatstring(distinct theString) as val from SupportBean";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportBean("a", -1));
            assertPairSingleRow(env, new Object[]{"a"}, new Object[]{""});

            env.sendEventBean(new SupportBean("b", -1));
            assertPairSingleRow(env, new Object[]{"a b"}, new Object[]{"a"});

            env.sendEventBean(new SupportBean("b", -1));
            assertPairSingleRow(env, new Object[]{"a b"}, new Object[]{"a b"});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("c", -1));
            assertPairSingleRow(env, new Object[]{"a b c"}, new Object[]{"a b"});

            env.sendEventBean(new SupportBean("a", -1));
            assertPairSingleRow(env, new Object[]{"a b c"}, new Object[]{"a b c"});

            env.undeployAll();
        }
    }

    private static class ClientExtendAggregationManagedDotMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test dot-method
            SupportSupportBeanAggregationFunctionFactory.setInstanceCount(0);
            String[] fields = "val0,val1".split(",");
            env.compileDeploy("@name('s0') select (myagg(id)).getTheString() as val0, (myagg(id)).getIntPrimitive() as val1 from SupportBean_A").addListener("s0");

            env.sendEventBean(new SupportBean_A("A1"));
            env.assertPropsNew("s0", fields, new Object[]{"XX", 1});

            env.sendEventBean(new SupportBean_A("A2"));
            env.assertPropsNew("s0", fields, new Object[]{"XX", 2});

            env.assertThat(() -> assertEquals(1, SupportSupportBeanAggregationFunctionFactory.getInstanceCount()));

            env.undeployAll();
        }
    }

    private static class ClientExtendAggregationMultiParamMulti implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionMultipleParams(env, false, milestone);
            tryAssertionMultipleParams(env, true, milestone);
        }

        private void tryAssertionMultipleParams(RegressionEnvironment env, boolean soda, AtomicInteger milestone) {

            String text = "@name('s0') select irstream countboundary(1,10,intPrimitive,*) as val from SupportBean";
            env.compileDeploy(soda, text).addListener("s0");

            env.assertThat(() -> {
                AggregationFunctionValidationContext validContext = SupportLowerUpperCompareAggregationFunctionForge.getContexts().get(0);
                EPAssertionUtil.assertEqualsExactOrder(new EPTypeClass[]{new EPTypeClass(int.class), new EPTypeClass(int.class), new EPTypeClass(Integer.class), new EPTypeClass(SupportBean.class)}, validContext.getParameterTypes());
                EPAssertionUtil.assertEqualsExactOrder(new Object[]{1, 10, null, null}, validContext.getConstantValues());
                EPAssertionUtil.assertEqualsExactOrder(new boolean[]{true, true, false, false}, validContext.getIsConstantValue());
            });

            SupportBean e1 = new SupportBean("E1", 5);
            env.sendEventBean(e1);
            assertPairSingleRow(env, new Object[]{1}, new Object[]{0});
            env.assertThat(() -> EPAssertionUtil.assertEqualsExactOrder(new Object[]{1, 10, 5, e1}, SupportLowerUpperCompareAggregationFunction.getLastEnterParameters()));

            env.sendEventBean(new SupportBean("E1", 0));
            assertPairSingleRow(env, new Object[]{1}, new Object[]{1});

            env.sendEventBean(new SupportBean("E1", 11));
            assertPairSingleRow(env, new Object[]{1}, new Object[]{1});

            env.sendEventBean(new SupportBean("E1", 1));
            assertPairSingleRow(env, new Object[]{2}, new Object[]{1});

            env.undeployAll();
        }
    }

    private static class ClientExtendAggregationMultiParamNoParam implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream countback() as val from SupportBean";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportBean());
            assertPairSingleRow(env, new Object[]{-1}, new Object[]{0});

            env.sendEventBean(new SupportBean());
            assertPairSingleRow(env, new Object[]{-2}, new Object[]{-1});

            env.undeployAll();
        }
    }

    private static class ClientExtendAggregationManagedMappedPropertyLookAlike implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream concatstring('a') as val from SupportBean";
            env.compileDeploy(text).addListener("s0");
            env.assertStatement("s0", statement -> assertEquals(String.class, statement.getEventType().getPropertyType("val")));

            env.sendEventBean(new SupportBean());
            assertPairSingleRow(env, new Object[]{"a"}, new Object[]{""});

            env.sendEventBean(new SupportBean());
            assertPairSingleRow(env, new Object[]{"a a"}, new Object[]{"a"});

            env.milestone(0);

            env.sendEventBean(new SupportBean());
            assertPairSingleRow(env, new Object[]{"a a a"}, new Object[]{"a a"});

            env.undeployAll();
        }
    }

    private static class ClientExtendAggregationFailedValidation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.tryInvalidCompile("select concatstring(1) from SupportBean",
                "Failed to validate select-clause expression 'concatstring(1)': Plug-in aggregation function 'concatstring' failed validation: Invalid parameter type '");
        }
    }

    private static class ClientExtendAggregationInvalidUse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.tryInvalidCompile("select * from SupportBean group by invalidAggFuncForge(1)",
                "Error resolving aggregation: Class by name 'java.lang.String' does not implement the AggregationFunctionForge interface");

            env.tryInvalidCompile("select * from SupportBean group by nonExistAggFuncForge(1)",
                "Error resolving aggregation: Could not load aggregation factory class by name 'com.NoSuchClass'");
        }
    }

    private static class ClientExtendAggregationInvalidCannotResolve implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.tryInvalidCompile("select zzz(theString) from SupportBean",
                "Failed to validate select-clause expression 'zzz(theString)': Unknown single-row function, aggregation function or mapped or indexed property named 'zzz' could not be resolved");
        }
    }

    private static void assertPairSingleRow(RegressionEnvironment env, Object[] expectedNew, Object[] expectedOld) {
        String[] fields = "val".split(",");
        env.assertPropsPerRowIRPair("s0", fields, new Object[][]{expectedNew}, new Object[][]{expectedOld});
    }
}
