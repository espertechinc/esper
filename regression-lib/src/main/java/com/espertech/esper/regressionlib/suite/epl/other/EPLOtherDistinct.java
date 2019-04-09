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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.client.soda.FilterStream;
import com.espertech.esper.common.client.soda.FromClause;
import com.espertech.esper.common.client.soda.SelectClause;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_N;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.client.scopetest.SupportSubscriberMRD;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class EPLOtherDistinct {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherOutputSimpleColumn());
        execs.add(new EPLOtherBatchWindow());
        execs.add(new EPLOtherBatchWindowJoin());
        execs.add(new EPLOtherBatchWindowInsertInto());
        execs.add(new EPLOtherOnDemandAndOnSelect());
        execs.add(new EPLOtherSubquery());
        execs.add(new EPLOtherBeanEventWildcardThisProperty());
        execs.add(new EPLOtherBeanEventWildcardSODA());
        execs.add(new EPLOtherBeanEventWildcardPlusCols());
        execs.add(new EPLOtherMapEventWildcard());
        execs.add(new EPLOtherOutputLimitEveryColumn());
        execs.add(new EPLOtherOutputRateSnapshotColumn());
        execs.add(new EPLOtherDistinctWildcardJoinPatternOne());
        execs.add(new EPLOtherDistinctWildcardJoinPatternTwo());
        execs.add(new EPLOtherDistinctOutputLimitMultikeyWArraySingleArray());
        execs.add(new EPLOtherDistinctOutputLimitMultikeyWArrayTwoArray());
        execs.add(new EPLOtherDistinctFireAndForgetMultikeyWArray());
        execs.add(new EPLOtherDistinctIterateMultikeyWArray());
        execs.add(new EPLOtherDistinctOnSelectMultikeyWArray());
        execs.add(new EPLOtherDistinctVariantStream());
        return execs;
    }

    private static class EPLOtherDistinctVariantStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create variant schema MyVariant as SupportEventWithManyArray;\n" +
                "insert into MyVariant select * from SupportEventWithManyArray;\n" +
                "@name('s0') select distinct * from MyVariant#keepall;\n" +
                "@name('s1') select distinct intOne from MyVariant#keepall;\n" +
                "@name('s2') select distinct intOne, intTwo from MyVariant#keepall;\n";
            env.compileDeploy(epl);

            sendManyArray(env, new int[]{1, 2}, new int[]{3, 4});
            sendManyArray(env, new int[]{3, 4}, new int[]{1, 2});
            sendManyArray(env, new int[]{1, 2}, new int[]{3, 5});
            sendManyArray(env, new int[]{3, 4}, new int[]{1, 2});
            sendManyArray(env, new int[]{1, 2}, new int[]{3, 4});

            assertEquals(3, EPAssertionUtil.iteratorToArray(env.iterator("s0")).length);
            assertEquals(2, EPAssertionUtil.iteratorToArray(env.iterator("s1")).length);
            assertEquals(3, EPAssertionUtil.iteratorToArray(env.iterator("s2")).length);

            env.undeployAll();
        }
    }

    private static class EPLOtherDistinctOnSelectMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create window MyWindow#keepall as SupportEventWithManyArray;\n" +
                "insert into MyWindow select * from SupportEventWithManyArray;\n" +
                "@name('s0') on SupportBean_S0 select distinct intOne from MyWindow;\n" +
                "@name('s1') on SupportBean_S1 select distinct intOne, intTwo from MyWindow;\n";
            env.compileDeploy(epl).addListener("s0").addListener("s1");

            sendManyArray(env, new int[]{1, 2}, new int[]{3, 4});
            sendManyArray(env, new int[]{3, 4}, new int[]{1, 2});
            sendManyArray(env, new int[]{1, 2}, new int[]{3, 5});
            sendManyArray(env, new int[]{3, 4}, new int[]{1, 2});
            sendManyArray(env, new int[]{1, 2}, new int[]{3, 4});

            env.sendEventBean(new SupportBean_S0(0));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "intOne".split(","),
                new Object[][]{{new int[]{1, 2}}, {new int[]{3, 4}}});

            env.sendEventBean(new SupportBean_S1(0));
            EPAssertionUtil.assertPropsPerRow(env.listener("s1").getAndResetLastNewData(), "intOne,intTwo".split(","),
                new Object[][]{
                    {new int[]{1, 2}, new int[]{3, 4}},
                    {new int[]{3, 4}, new int[]{1, 2}},
                    {new int[]{1, 2}, new int[]{3, 5}}
                });

            env.undeployAll();
        }
    }

    private static class EPLOtherDistinctIterateMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@name('s0') select distinct intOne from SupportEventWithManyArray#keepall;\n" +
                    "@name('s1') select distinct intOne, intTwo from SupportEventWithManyArray#keepall;\n";
            env.compileDeploy(epl);

            sendManyArray(env, new int[]{1, 2}, new int[]{3, 4});
            sendManyArray(env, new int[]{3, 4}, new int[]{1, 2});
            sendManyArray(env, new int[]{1, 2}, new int[]{3, 5});
            sendManyArray(env, new int[]{3, 4}, new int[]{1, 2});
            sendManyArray(env, new int[]{1, 2}, new int[]{3, 4});

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), "intOne".split(","),
                new Object[][]{{new int[]{1, 2}}, {new int[]{3, 4}}});

            EPAssertionUtil.assertPropsPerRow(env.iterator("s1"), "intOne,intTwo".split(","),
                new Object[][]{
                    {new int[]{1, 2}, new int[]{3, 4}},
                    {new int[]{3, 4}, new int[]{1, 2}},
                    {new int[]{1, 2}, new int[]{3, 5}}
                });

            env.undeployAll();
        }
    }

    private static class EPLOtherDistinctFireAndForgetMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "@name('s0') create window MyWindow#keepall as SupportEventWithManyArray;\n" +
                "insert into MyWindow select * from SupportEventWithManyArray;\n";
            env.compileDeploy(epl, path);

            sendManyArray(env, new int[]{1, 2}, new int[]{3, 4});
            sendManyArray(env, new int[]{3, 4}, new int[]{1, 2});
            sendManyArray(env, new int[]{1, 2}, new int[]{3, 5});
            sendManyArray(env, new int[]{3, 4}, new int[]{1, 2});
            sendManyArray(env, new int[]{1, 2}, new int[]{3, 4});

            EPFireAndForgetQueryResult result = env.compileExecuteFAF("select distinct intOne from MyWindow", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), "intOne".split(","),
                new Object[][]{{new int[]{1, 2}}, {new int[]{3, 4}}});

            result = env.compileExecuteFAF("select distinct intOne, intTwo from MyWindow", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), "intOne,intTwo".split(","),
                new Object[][]{
                    {new int[]{1, 2}, new int[]{3, 4}},
                    {new int[]{3, 4}, new int[]{1, 2}},
                    {new int[]{1, 2}, new int[]{3, 5}}
                });

            env.undeployAll();
        }
    }

    private static class EPLOtherDistinctOutputLimitMultikeyWArrayTwoArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@name('s0') select distinct intOne, intTwo from SupportEventWithManyArray output every 1 seconds";
            env.compileDeploy(epl).addListener("s0");

            sendManyArray(env, new int[]{1, 2}, new int[]{3, 4});
            sendManyArray(env, new int[]{3, 4}, new int[]{1, 2});
            sendManyArray(env, new int[]{1, 2}, new int[]{3, 5});
            sendManyArray(env, new int[]{3, 4}, new int[]{1, 2});
            sendManyArray(env, new int[]{1, 2}, new int[]{3, 4});

            env.advanceTime(1000);

            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "intOne,intTwo".split(","),
                new Object[][]{
                    {new int[]{1, 2}, new int[]{3, 4}},
                    {new int[]{3, 4}, new int[]{1, 2}},
                    {new int[]{1, 2}, new int[]{3, 5}}
                });

            env.undeployAll();
        }
    }

    private static class EPLOtherDistinctOutputLimitMultikeyWArraySingleArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@name('s0') select distinct intOne from SupportEventWithManyArray output every 1 seconds";
            env.compileDeploy(epl).addListener("s0");

            sendManyArray(env, new int[]{1, 2});
            sendManyArray(env, new int[]{2, 1});
            sendManyArray(env, new int[]{2, 3});
            sendManyArray(env, new int[]{1, 2});
            sendManyArray(env, new int[]{1, 2});

            env.advanceTime(1000);

            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "intOne".split(","),
                new Object[][]{{new int[]{1, 2}}, {new int[]{2, 1}}, {new int[]{2, 3}}});

            env.undeployAll();
        }
    }

    private static class EPLOtherDistinctWildcardJoinPatternOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select distinct * from " +
                "SupportBean(intPrimitive=0) as fooB unidirectional " +
                "inner join " +
                "pattern [" +
                "every-distinct(fooA.theString) fooA=SupportBean(intPrimitive=1)" +
                "->" +
                "every-distinct(wooA.theString) wooA=SupportBean(intPrimitive=2)" +
                " where timer:within(1 hour)" +
                "]#time(1 hour) as fooWooPair " +
                "on fooB.longPrimitive = fooWooPair.fooA.longPrimitive";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "E1", 1, 10L);
            sendEvent(env, "E1", 2, 10L);

            env.milestone(0);

            sendEvent(env, "E2", 1, 10L);
            sendEvent(env, "E2", 2, 10L);

            sendEvent(env, "E3", 1, 10L);
            sendEvent(env, "E3", 2, 10L);

            sendEvent(env, "Query", 0, 10L);
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    public static class EPLOtherDistinctWildcardJoinPatternTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select distinct * from " +
                "SupportBean(intPrimitive=0) as fooB unidirectional " +
                "inner join " +
                "pattern [" +
                "every-distinct(fooA.theString) fooA=SupportBean(intPrimitive=1)" +
                "->" +
                "every-distinct(wooA.theString) wooA=SupportBean(intPrimitive=2)" +
                " where timer:within(1 hour)" +
                "]#time(1 hour) as fooWooPair " +
                "on fooB.longPrimitive = fooWooPair.fooA.longPrimitive" +
                " order by fooWooPair.wooA.theString asc";
            env.compileDeploy(epl);
            SupportSubscriberMRD subscriber = new SupportSubscriberMRD();
            env.statement("s0").setSubscriber(subscriber);

            sendEvent(env, "E1", 1, 10L);
            sendEvent(env, "E2", 2, 10L);
            sendEvent(env, "E3", 2, 10L);
            sendEvent(env, "Query", 0, 10L);

            assertTrue(subscriber.isInvoked());
            Assert.assertEquals(1, subscriber.getInsertStreamList().size());
            Object[][] inserted = subscriber.getInsertStreamList().get(0);
            assertEquals(2, inserted.length);
            Assert.assertEquals("Query", ((SupportBean) inserted[0][0]).getTheString());
            Assert.assertEquals("Query", ((SupportBean) inserted[1][0]).getTheString());
            Map mapOne = (Map) inserted[0][1];
            Assert.assertEquals("E2", ((EventBean) mapOne.get("wooA")).get("theString"));
            Assert.assertEquals("E1", ((EventBean) mapOne.get("fooA")).get("theString"));
            Map mapTwo = (Map) inserted[1][1];
            Assert.assertEquals("E3", ((EventBean) mapTwo.get("wooA")).get("theString"));
            Assert.assertEquals("E1", ((EventBean) mapTwo.get("fooA")).get("theString"));

            env.undeployAll();
        }

        private static void sendEvent(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
            SupportBean bean = new SupportBean(theString, intPrimitive);
            bean.setLongPrimitive(longPrimitive);
            env.sendEventBean(bean);
        }
    }

    private static class EPLOtherOnDemandAndOnSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString", "intPrimitive"};
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow#keepall as select * from SupportBean", path);
            env.compileDeploy("insert into MyWindow select * from SupportBean", path);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E1", 2));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E1", 1));

            String query = "select distinct theString, intPrimitive from MyWindow order by theString, intPrimitive";
            EPFireAndForgetQueryResult result = env.compileExecuteFAF(query, path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 2}});

            env.compileDeploy("@name('s0') on SupportBean_A select distinct theString, intPrimitive from MyWindow order by theString, intPrimitive asc", path).addListener("s0");

            env.sendEventBean(new SupportBean_A("x"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E2", 2}});

            env.undeployAll();
        }
    }

    private static class EPLOtherSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString", "intPrimitive"};
            env.compileDeploy("@name('s0') select * from SupportBean where theString in (select distinct id from SupportBean_A#keepall)");
            env.addListener("s0");

            env.sendEventBean(new SupportBean_A("E1"));
            env.sendEventBean(new SupportBean("E1", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 2});

            env.milestone(0);

            env.sendEventBean(new SupportBean_A("E1"));
            env.sendEventBean(new SupportBean("E1", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 3});

            env.undeployAll();
        }

    }    // Since the "this" property will always be unique, this test verifies that condition

    private static class EPLOtherBeanEventWildcardThisProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString", "intPrimitive"};
            String statementText = "@name('s0') select distinct * from SupportBean#keepall";
            env.compileDeploy(statementText);

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

            env.undeployAll();
        }
    }

    private static class EPLOtherBeanEventWildcardSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"id"};
            String statementText = "@name('s0') select distinct * from SupportBean_A#keepall";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportBean_A("E1"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}});

            env.sendEventBean(new SupportBean_A("E2"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}, {"E2"}});

            env.milestone(0);

            env.sendEventBean(new SupportBean_A("E1"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1"}, {"E2"}});

            EPStatementObjectModel model = env.eplToModel(statementText);
            Assert.assertEquals(statementText, model.toEPL());

            model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard().distinct(true));
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean_A")));
            Assert.assertEquals("select distinct * from SupportBean_A", model.toEPL());

            env.undeployAll();
        }
    }

    private static class EPLOtherBeanEventWildcardPlusCols implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"intPrimitive", "val1", "val2"};
            String statementText = "@name('s0') select distinct *, intBoxed%5 as val1, intBoxed as val2 from SupportBean_N#keepall";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportBean_N(1, 8));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{1, 3, 8}});

            env.sendEventBean(new SupportBean_N(1, 3));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{1, 3, 8}, {1, 3, 3}});

            env.milestone(0);

            env.sendEventBean(new SupportBean_N(1, 8));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{1, 3, 8}, {1, 3, 3}});

            env.undeployAll();
        }
    }

    private static class EPLOtherMapEventWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = new String[]{"k1", "v1"};
            String statementText = "@name('s0') select distinct * from MyMapTypeKVDistinct#keepall";
            env.compileDeploy(statementText).addListener("s0");

            sendMapEvent(env, "E1", 1);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}});

            sendMapEvent(env, "E2", 2);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

            env.milestone(0);

            sendMapEvent(env, "E1", 1);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

            env.undeployAll();
        }
    }

    private static class EPLOtherOutputSimpleColumn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString", "intPrimitive"};
            String statementText = "@name('s0') select distinct theString, intPrimitive from SupportBean#keepall";
            env.compileDeploy(statementText).addListener("s0");

            tryAssertionSimpleColumn(env, env.listener("s0"), env.statement("s0"), fields);
            env.undeployAll();

            // test join
            statementText = "@name('s0') select distinct theString, intPrimitive from SupportBean#keepall a, SupportBean_A#keepall b where a.theString = b.id";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportBean_A("E1"));
            env.sendEventBean(new SupportBean_A("E2"));
            tryAssertionSimpleColumn(env, env.listener("s0"), env.statement("s0"), fields);

            env.undeployAll();
        }
    }

    private static class EPLOtherOutputLimitEveryColumn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString", "intPrimitive"};
            String statementText = "@name('s0') @IterableUnbound select distinct theString, intPrimitive from SupportBean output every 3 events";
            env.compileDeploy(statementText).addListener("s0");

            tryAssertionOutputEvery(env, fields);
            env.undeployAll();

            // test join
            statementText = "@name('s0') select distinct theString, intPrimitive from SupportBean#lastevent a, SupportBean_A#keepall b where a.theString = b.id output every 3 events";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportBean_A("E1"));
            env.sendEventBean(new SupportBean_A("E2"));
            tryAssertionOutputEvery(env, fields);

            env.undeployAll();
        }
    }

    private static class EPLOtherOutputRateSnapshotColumn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString", "intPrimitive"};
            String statementText = "@name('s0') select distinct theString, intPrimitive from SupportBean#keepall output snapshot every 3 events order by theString asc";
            env.compileDeploy(statementText).addListener("s0");

            tryAssertionSnapshotColumn(env, env.listener("s0"), env.statement("s0"), fields);
            env.undeployAll();

            statementText = "@name('s0') select distinct theString, intPrimitive from SupportBean#keepall a, SupportBean_A#keepall b where a.theString = b.id output snapshot every 3 events order by theString asc";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportBean_A("E1"));

            env.milestone(0);

            env.sendEventBean(new SupportBean_A("E2"));
            env.sendEventBean(new SupportBean_A("E3"));
            tryAssertionSnapshotColumn(env, env.listener("s0"), env.statement("s0"), fields);

            env.undeployAll();
        }
    }

    private static class EPLOtherBatchWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString", "intPrimitive"};
            String statementText = "@name('s0') select distinct theString, intPrimitive from SupportBean#length_batch(3)";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}});
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E2", 2}, {"E1", 1}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 3));
            env.sendEventBean(new SupportBean("E2", 3));
            env.sendEventBean(new SupportBean("E2", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E2", 3}});

            env.undeployAll();

            // test batch window with aggregation
            env.advanceTime(0);
            String[] fieldsTwo = new String[]{"c1", "c2"};
            String epl = "@name('s0') insert into ABC select distinct theString as c1, first(intPrimitive) as c2 from SupportBean#time_batch(1 second)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));

            env.advanceTime(1000);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fieldsTwo, new Object[][]{{"E1", 1}, {"E2", 1}});

            env.advanceTime(2000);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLOtherBatchWindowJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString", "intPrimitive"};
            String statementText = "@name('s0') select distinct theString, intPrimitive from SupportBean#length_batch(3) a, SupportBean_A#keepall b where a.theString = b.id";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventBean(new SupportBean_A("E1"));
            env.sendEventBean(new SupportBean_A("E2"));

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E1", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E2", 2}, {"E1", 1}});

            env.sendEventBean(new SupportBean("E2", 3));
            env.sendEventBean(new SupportBean("E2", 3));
            env.sendEventBean(new SupportBean("E2", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E2", 3}});

            env.undeployAll();
        }
    }

    private static class EPLOtherBatchWindowInsertInto implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString", "intPrimitive"};
            RegressionPath path = new RegressionPath();

            String statementText = "insert into MyStream select distinct theString, intPrimitive from SupportBean#length_batch(3)";
            env.compileDeploy(statementText, path);

            statementText = "@name('s0') select * from MyStream";
            env.compileDeploy(statementText, path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E3", 3));
            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").getNewDataListFlattened()[0], fields, new Object[]{"E2", 2});
            EPAssertionUtil.assertProps(env.listener("s0").getNewDataListFlattened()[1], fields, new Object[]{"E3", 3});

            env.undeployAll();
        }
    }

    private static void tryAssertionOutputEvery(RegressionEnvironment env, String[] fields) {
        env.sendEventBean(new SupportBean("E1", 1));
        env.sendEventBean(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}});
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        env.listener("s0").reset();

        env.sendEventBean(new SupportBean("E2", 2));
        env.sendEventBean(new SupportBean("E1", 1));
        env.sendEventBean(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E2", 2}, {"E1", 1}});
        env.listener("s0").reset();

        env.milestone(0);

        env.sendEventBean(new SupportBean("E2", 3));
        env.sendEventBean(new SupportBean("E2", 3));
        env.sendEventBean(new SupportBean("E2", 3));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E2", 3}});
        env.listener("s0").reset();
    }

    private static void tryAssertionSimpleColumn(RegressionEnvironment env, SupportListener listener, EPStatement stmt, String[] fields) {
        env.sendEventBean(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        env.sendEventBean(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        env.sendEventBean(new SupportBean("E2", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 1}});
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 1});

        env.sendEventBean(new SupportBean("E1", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 1}, {"E1", 2}});
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 2});

        env.sendEventBean(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 1}, {"E1", 2}, {"E2", 2}});
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

        env.sendEventBean(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 1}, {"E1", 2}, {"E2", 2}});
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

        env.sendEventBean(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 1}, {"E1", 2}, {"E2", 2}});
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
    }

    private static void tryAssertionSnapshotColumn(RegressionEnvironment env, SupportListener listener, EPStatement stmt, String[] fields) {
        env.sendEventBean(new SupportBean("E1", 1));
        env.sendEventBean(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}});
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        env.sendEventBean(new SupportBean("E2", 2));
        env.sendEventBean(new SupportBean("E1", 1));
        env.sendEventBean(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        env.listener("s0").reset();

        env.sendEventBean(new SupportBean("E3", 3));
        env.sendEventBean(new SupportBean("E1", 1));
        env.sendEventBean(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});
        env.listener("s0").reset();
    }

    private static void sendEvent(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendMapEvent(RegressionEnvironment env, String s, int i) {
        Map<String, Object> def = new HashMap<>();
        def.put("k1", s);
        def.put("v1", i);
        env.sendEventMap(def, "MyMapTypeKVDistinct");
    }

    private static void sendManyArray(RegressionEnvironment env, int[] intOne, int[] intTwo) {
        env.sendEventBean(new SupportEventWithManyArray("id").withIntOne(intOne).withIntTwo(intTwo));
    }

    private static void sendManyArray(RegressionEnvironment env, int[] ints) {
        env.sendEventBean(new SupportEventWithManyArray("id").withIntOne(ints));
    }
}