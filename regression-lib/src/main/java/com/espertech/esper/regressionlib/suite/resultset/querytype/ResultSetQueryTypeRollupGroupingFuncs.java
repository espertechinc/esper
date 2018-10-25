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
package com.espertech.esper.regressionlib.suite.resultset.querytype;

import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportCarEvent;
import com.espertech.esper.regressionlib.support.bean.SupportCarInfoEvent;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ResultSetQueryTypeRollupGroupingFuncs {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetQueryTypeDocSampleCarEventAndGroupingFunc());
        execs.add(new ResultSetQueryTypeInvalid());
        execs.add(new ResultSetQueryTypeFAFCarEventAndGroupingFunc());
        execs.add(new ResultSetQueryTypeGroupingFuncExpressionUse());
        return execs;
    }

    private static class ResultSetQueryTypeFAFCarEventAndGroupingFunc implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window CarWindow#keepall as SupportCarEvent;\n" +
                "insert into CarWindow select * from SupportCarEvent;\n";
            env.compileDeploy(epl, path);

            env.sendEventBean(new SupportCarEvent("skoda", "france", 10000));
            env.sendEventBean(new SupportCarEvent("skoda", "germany", 5000));
            env.sendEventBean(new SupportCarEvent("bmw", "france", 100));
            env.sendEventBean(new SupportCarEvent("bmw", "germany", 1000));
            env.sendEventBean(new SupportCarEvent("opel", "france", 7000));
            env.sendEventBean(new SupportCarEvent("opel", "germany", 7000));

            epl = "@name('s0') select name, place, sum(count), grouping(name), grouping(place), grouping_id(name, place) as gid " +
                "from CarWindow group by grouping sets((name, place),name, place,())";
            EPFireAndForgetQueryResult result = env.compileExecuteFAF(epl, path);

            Assert.assertEquals(Integer.class, result.getEventType().getPropertyType("grouping(name)"));
            Assert.assertEquals(Integer.class, result.getEventType().getPropertyType("gid"));

            String[] fields = new String[]{"name", "place", "sum(count)", "grouping(name)", "grouping(place)", "gid"};
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{
                {"skoda", "france", 10000, 0, 0, 0},
                {"skoda", "germany", 5000, 0, 0, 0},
                {"bmw", "france", 100, 0, 0, 0},
                {"bmw", "germany", 1000, 0, 0, 0},
                {"opel", "france", 7000, 0, 0, 0},
                {"opel", "germany", 7000, 0, 0, 0},
                {"skoda", null, 15000, 0, 1, 1},
                {"bmw", null, 1100, 0, 1, 1},
                {"opel", null, 14000, 0, 1, 1},
                {null, "france", 17100, 1, 0, 2},
                {null, "germany", 13000, 1, 0, 2},
                {null, null, 30100, 1, 1, 3}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeDocSampleCarEventAndGroupingFunc implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            // try simple
            String epl = "@name('s0') select name, place, sum(count), grouping(name), grouping(place), grouping_id(name,place) as gid " +
                "from SupportCarEvent group by grouping sets((name, place), name, place, ())";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionDocSampleCarEvent(env, milestone);
            env.undeployAll();

            // try audit
            env.compileDeploy("@Audit " + epl).addListener("s0");
            tryAssertionDocSampleCarEvent(env, milestone);
            env.undeployAll();

            // try model
            env.eplToModelCompileDeploy(epl).addListener("s0");

            tryAssertionDocSampleCarEvent(env, milestone);

            env.undeployAll();
        }

        private static void tryAssertionDocSampleCarEvent(RegressionEnvironment env, AtomicInteger milestone) {
            String[] fields = new String[]{"name", "place", "sum(count)", "grouping(name)", "grouping(place)", "gid"};
            env.sendEventBean(new SupportCarEvent("skoda", "france", 100));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {"skoda", "france", 100, 0, 0, 0},
                {"skoda", null, 100, 0, 1, 1},
                {null, "france", 100, 1, 0, 2},
                {null, null, 100, 1, 1, 3}});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportCarEvent("skoda", "germany", 75));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {"skoda", "germany", 75, 0, 0, 0},
                {"skoda", null, 175, 0, 1, 1},
                {null, "germany", 75, 1, 0, 2},
                {null, null, 175, 1, 1, 3}});
        }
    }

    private static class ResultSetQueryTypeGroupingFuncExpressionUse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            GroupingSupportFunc.getParameters().clear();

            // test uncorrelated subquery and expression-declaration and single-row func
            String epl = "create expression myExpr {x=> '|' || x.name || '|'};\n" +
                "@name('s0') select myfunc(" +
                "  name, place, sum(count), grouping(name), grouping(place), grouping_id(name, place)," +
                "  (select refId from SupportCarInfoEvent#lastevent), " +
                "  myExpr(ce)" +
                "  )" +
                "from SupportCarEvent ce group by grouping sets((name, place),name, place,())";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportCarInfoEvent("a", "b", "c01"));

            env.sendEventBean(new SupportCarEvent("skoda", "france", 10000));
            EPAssertionUtil.assertEqualsExactOrder(new Object[][]{
                {"skoda", "france", 10000, 0, 0, 0, "c01", "|skoda|"},
                {"skoda", null, 10000, 0, 1, 1, "c01", "|skoda|"},
                {null, "france", 10000, 1, 0, 2, "c01", "|skoda|"},
                {null, null, 10000, 1, 1, 3, "c01", "|skoda|"}}, GroupingSupportFunc.assertGetAndClear(4));
            env.undeployAll();

            // test "prev" and "prior"
            String[] fields = "c0,c1,c2,c3".split(",");
            String eplTwo = "@name('s0') select prev(1, name) as c0, prior(1, name) as c1, name as c2, sum(count) as c3 " +
                "from SupportCarEvent#keepall ce group by rollup(name)";
            env.compileDeploy(eplTwo).addListener("s0");

            env.sendEventBean(new SupportCarEvent("skoda", "france", 10));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {null, null, "skoda", 10}, {null, null, null, 10}
            });

            env.sendEventBean(new SupportCarEvent("vw", "france", 15));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {"skoda", "skoda", "vw", 15}, {"skoda", "skoda", null, 25}
            });

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // invalid use of function
            String expected = "Failed to validate select-clause expression 'grouping(theString)': The grouping function requires the group-by clause to specify rollup, cube or grouping sets, and may only be used in the select-clause, having-clause or order-by-clause [select grouping(theString) from SupportBean]";
            SupportMessageAssertUtil.tryInvalidCompile(env, "select grouping(theString) from SupportBean", expected);
            SupportMessageAssertUtil.tryInvalidCompile(env, "select theString, sum(intPrimitive) from SupportBean(grouping(theString) = 1) group by rollup(theString)",
                "Failed to validate filter expression 'grouping(theString)=1': The grouping function requires the group-by clause to specify rollup, cube or grouping sets, and may only be used in the select-clause, having-clause or order-by-clause [select theString, sum(intPrimitive) from SupportBean(grouping(theString) = 1) group by rollup(theString)]");
            SupportMessageAssertUtil.tryInvalidCompile(env, "select theString, sum(intPrimitive) from SupportBean where grouping(theString) = 1 group by rollup(theString)",
                "Failed to validate filter expression 'grouping(theString)=1': The grouping function requires the group-by clause to specify rollup, cube or grouping sets, and may only be used in the select-clause, having-clause or order-by-clause [select theString, sum(intPrimitive) from SupportBean where grouping(theString) = 1 group by rollup(theString)]");
            SupportMessageAssertUtil.tryInvalidCompile(env, "select theString, sum(intPrimitive) from SupportBean group by rollup(grouping(theString))",
                "The grouping function requires the group-by clause to specify rollup, cube or grouping sets, and may only be used in the select-clause, having-clause or order-by-clause [select theString, sum(intPrimitive) from SupportBean group by rollup(grouping(theString))]");

            // invalid parameters
            SupportMessageAssertUtil.tryInvalidCompile(env, "select theString, sum(intPrimitive), grouping(longPrimitive) from SupportBean group by rollup(theString)",
                "Failed to find expression 'longPrimitive' among group-by expressions");
            SupportMessageAssertUtil.tryInvalidCompile(env, "select theString, sum(intPrimitive), grouping(theString||'x') from SupportBean group by rollup(theString)",
                "Failed to find expression 'theString||\"x\"' among group-by expressions [select theString, sum(intPrimitive), grouping(theString||'x') from SupportBean group by rollup(theString)]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select theString, sum(intPrimitive), grouping_id(theString, theString) from SupportBean group by rollup(theString)",
                "Duplicate expression 'theString' among grouping function parameters [select theString, sum(intPrimitive), grouping_id(theString, theString) from SupportBean group by rollup(theString)]");
        }
    }

    public static class GroupingSupportFunc {
        private static List<Object[]> parameters = new ArrayList<>();

        public static void myfunc(String name,
                                  String place,
                                  Integer cnt,
                                  Integer grpName,
                                  Integer grpPlace,
                                  Integer grpId,
                                  String refId,
                                  String namePlusDelim) {
            parameters.add(new Object[]{name, place, cnt, grpName, grpPlace, grpId, refId, namePlusDelim});
        }

        public static List<Object[]> getParameters() {
            return parameters;
        }

        static Object[][] assertGetAndClear(int numRows) {
            assertEquals(numRows, parameters.size());
            Object[][] result = parameters.toArray(new Object[numRows][]);
            parameters.clear();
            return result;
        }
    }
}
