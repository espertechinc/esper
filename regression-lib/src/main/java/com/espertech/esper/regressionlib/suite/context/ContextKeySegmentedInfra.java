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
package com.espertech.esper.regressionlib.suite.context;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.client.context.ContextPartitionSelectorSegmented;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.support.context.SupportSelectorById;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ContextKeySegmentedInfra {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextKeySegmentedInfraAggregatedSubquery());
        execs.add(new ContextKeySegmentedInfraOnDeleteAndUpdate());
        execs.add(new ContextKeySegmentedInfraCreateIndex());
        execs.add(new ContextKeySegmentedInfraOnSelect());
        execs.add(new ContextKeySegmentedInfraNWConsumeAll());
        execs.add(new ContextKeySegmentedInfraNWConsumeSameContext());
        execs.add(new ContextKeySegmentedInfraOnMergeUpdateSubq());
        execs.add(new ContextKeyedSegmentedTable());
        return execs;
    }

    private static class ContextKeySegmentedInfraAggregatedSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionAggregatedSubquery(env, milestone, true);
            tryAssertionAggregatedSubquery(env, milestone, false);
        }
    }

    private static class ContextKeySegmentedInfraOnDeleteAndUpdate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionOnDeleteAndUpdate(env, milestone, true);
            tryAssertionOnDeleteAndUpdate(env, milestone, false);
        }
    }

    private static class ContextKeySegmentedInfraCreateIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionCreateIndex(env, milestone, true);
            tryAssertionCreateIndex(env, milestone, false);
        }
    }

    private static class ContextKeySegmentedInfraOnSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionSegmentedOnSelect(env, milestone, true);
            tryAssertionSegmentedOnSelect(env, milestone, false);
        }
    }

    private static class ContextKeySegmentedInfraNWConsumeAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);

            env.compileDeploy("@Name('named window') context SegmentedByString create window MyWindow#lastevent as SupportBean", path);
            env.addListener("named window");
            env.compileDeploy("@Name('insert') insert into MyWindow select * from SupportBean", path);

            env.compileDeploy("@Name('s0') select * from MyWindow", path).addListener("s0");

            String[] fields = new String[]{"theString", "intPrimitive"};
            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("named window").assertOneGetNewAndReset(), fields, new Object[]{"G1", 10});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 10});

            env.milestone(0);

            env.sendEventBean(new SupportBean("G2", 20));
            EPAssertionUtil.assertProps(env.listener("named window").assertOneGetNewAndReset(), fields, new Object[]{"G2", 20});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 20});

            env.milestone(1);

            env.undeployModuleContaining("s0");

            // Out-of-context consumer not initialized
            env.compileDeploy("@Name('s0') select count(*) as cnt from MyWindow", path);
            EPAssertionUtil.assertProps(env.iterator("s0").next(), "cnt".split(","), new Object[]{0L});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedInfraNWConsumeSameContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);

            env.compileDeploy("@Name('named window') context SegmentedByString create window MyWindow#keepall as SupportBean", path);
            env.addListener("named window");
            env.compileDeploy("@Name('insert') insert into MyWindow select * from SupportBean", path);

            String[] fieldsNW = new String[]{"theString", "intPrimitive"};
            String[] fieldsCnt = new String[]{"theString", "cnt"};
            env.compileDeploy("@Name('select') context SegmentedByString select theString, count(*) as cnt from MyWindow group by theString", path);
            env.addListener("select");

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("named window").assertOneGetNewAndReset(), fieldsNW, new Object[]{"G1", 10});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fieldsCnt, new Object[]{"G1", 1L});

            env.milestone(0);

            env.sendEventBean(new SupportBean("G2", 20));
            EPAssertionUtil.assertProps(env.listener("named window").assertOneGetNewAndReset(), fieldsNW, new Object[]{"G2", 20});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fieldsCnt, new Object[]{"G2", 1L});

            env.milestone(1);

            env.sendEventBean(new SupportBean("G1", 11));
            EPAssertionUtil.assertProps(env.listener("named window").assertOneGetNewAndReset(), fieldsNW, new Object[]{"G1", 11});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fieldsCnt, new Object[]{"G1", 2L});

            env.milestone(2);

            env.sendEventBean(new SupportBean("G2", 21));
            EPAssertionUtil.assertProps(env.listener("named window").assertOneGetNewAndReset(), fieldsNW, new Object[]{"G2", 21});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fieldsCnt, new Object[]{"G2", 2L});

            env.undeployModuleContaining("select");

            // In-context consumer not initialized
            env.compileDeploy("@Name('select') context SegmentedByString select count(*) as cnt from MyWindow", path);
            env.addListener("select");
            try {
                env.statement("select").iterator();
            } catch (UnsupportedOperationException ex) {
                assertEquals("Iterator not supported on statements that have a context attached", ex.getMessage());
            }
            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedInfraOnMergeUpdateSubq implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@Name('context') create context SegmentedByString " +
                "partition by theString from SupportBean, p00 from SupportBean_S0, p10 from SupportBean_S1;\n";
            epl += "@Name('named window') context SegmentedByString create window MyWindow#keepall as SupportBean;\n";
            epl += "@Name('insert') insert into MyWindow select * from SupportBean;\n";
            epl += "@Name('on-merge') context SegmentedByString " +
                "on SupportBean_S0 " +
                "merge MyWindow " +
                "when matched then " +
                "  update set intPrimitive = (select id from SupportBean_S1#lastevent)";
            env.compileDeploy(epl).addListener("named window").addListener("on-merge");

            String[] fieldsNW = new String[]{"theString", "intPrimitive"};

            env.sendEventBean(new SupportBean("G1", 1));
            EPAssertionUtil.assertProps(env.listener("named window").assertOneGetNewAndReset(), fieldsNW, new Object[]{"G1", 1});

            env.sendEventBean(new SupportBean_S1(99, "G1"));
            env.sendEventBean(new SupportBean_S0(0, "G1"));
            EPAssertionUtil.assertProps(env.listener("named window").getLastNewData()[0], fieldsNW, new Object[]{"G1", 99});
            EPAssertionUtil.assertProps(env.listener("named window").getLastOldData()[0], fieldsNW, new Object[]{"G1", 1});
            env.listener("named window").reset();

            env.milestone(0);

            env.sendEventBean(new SupportBean("G2", 2));
            EPAssertionUtil.assertProps(env.listener("named window").assertOneGetNewAndReset(), fieldsNW, new Object[]{"G2", 2});

            env.sendEventBean(new SupportBean_S1(98, "Gx"));
            env.sendEventBean(new SupportBean_S0(0, "G2"));
            EPAssertionUtil.assertProps(env.listener("named window").getLastNewData()[0], fieldsNW, new Object[]{"G2", 2});
            EPAssertionUtil.assertProps(env.listener("named window").getLastOldData()[0], fieldsNW, new Object[]{"G2", 2});
            env.listener("named window").reset();

            env.milestone(1);

            env.sendEventBean(new SupportBean("G3", 3));
            EPAssertionUtil.assertProps(env.listener("named window").assertOneGetNewAndReset(), fieldsNW, new Object[]{"G3", 3});

            env.sendEventBean(new SupportBean_S0(0, "Gx"));
            assertFalse(env.listener("named window").isInvoked());

            env.undeployAll();
        }
    }

    public static class ContextKeyedSegmentedTable implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') create context SegmentedByString partition by theString from SupportBean", path);
            env.compileDeploy("@Name('table') context SegmentedByString " +
                "create table MyTable(theString string, intPrimitive int primary key)", path);
            env.compileDeploy("@Name('insert') context SegmentedByString insert into MyTable select theString, intPrimitive from SupportBean", path);

            env.sendEventBean(new SupportBean("G1", 10));
            assertValues(env, "G1", new Object[][]{{"G1", 10}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("G2", 20));
            assertValues(env, "G1", new Object[][]{{"G1", 10}});
            assertValues(env, "G2", new Object[][]{{"G2", 20}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("G1", 11));

            env.milestone(2);

            assertValues(env, "G1", new Object[][]{{"G1", 10}, {"G1", 11}});
            assertValues(env, "G2", new Object[][]{{"G2", 20}});

            env.sendEventBean(new SupportBean("G2", 21));

            env.milestone(3);

            assertValues(env, "G1", new Object[][]{{"G1", 10}, {"G1", 11}});
            assertValues(env, "G2", new Object[][]{{"G2", 20}, {"G2", 21}});

            env.undeployAll();
        }

        private void assertValues(RegressionEnvironment env, final String group, Object[][] expected) {
            Iterator<EventBean> it = env.statement("table").iterator(new ContextPartitionSelectorSegmented() {
                public List<Object[]> getPartitionKeys() {
                    return Collections.singletonList(new Object[]{group});
                }
            });
            EPAssertionUtil.assertPropsPerRowAnyOrder(it, "theString,intPrimitive".split(","), expected);
        }
    }

    private static void tryAssertionSegmentedOnSelect(RegressionEnvironment env, AtomicInteger milestone, boolean namedWindow) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@Name('context') create context SegmentedByString " +
            "partition by theString from SupportBean, p00 from SupportBean_S0", path);

        String eplCreate = namedWindow ?
            "@Name('named window') context SegmentedByString create window MyInfra#keepall as SupportBean" :
            "@Name('table') context SegmentedByString create table MyInfra(theString string primary key, intPrimitive int primary key)";
        env.compileDeploy(eplCreate, path);
        env.compileDeploy("@Name('insert') context SegmentedByString insert into MyInfra select theString, intPrimitive from SupportBean", path);

        String[] fieldsNW = new String[]{"theString", "intPrimitive"};
        env.compileDeploy("@name('s0') context SegmentedByString " +
            "on SupportBean_S0 select mywin.* from MyInfra as mywin", path);
        env.addListener("s0");

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("G1", 1));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("G2", 2));
        env.sendEventBean(new SupportBean("G1", 3));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_S0(0, "G1"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fieldsNW, new Object[][]{{"G1", 1}, {"G1", 3}});

        env.sendEventBean(new SupportBean_S0(0, "G2"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fieldsNW, new Object[][]{{"G2", 2}});

        env.undeployAll();
    }

    private static void tryAssertionCreateIndex(RegressionEnvironment env, AtomicInteger milestone, boolean namedWindow) {
        RegressionPath path = new RegressionPath();
        String epl = "@name('create-ctx') create context SegmentedByCustomer " +
            "  initiated by SupportBean_S0 s0 " +
            "  terminated by SupportBean_S1(p00 = p10);" +
            "" +
            "@name('create-infra') context SegmentedByCustomer\n" +
            (namedWindow ?
                "create window MyInfra#keepall as SupportBean;" :
                "create table MyInfra(theString string primary key, intPrimitive int);") +
            "" +
            (namedWindow ?
                "@name('insert-into-window') insert into MyInfra select theString, intPrimitive from SupportBean;" :
                "@name('insert-into-table') context SegmentedByCustomer insert into MyInfra select theString, intPrimitive from SupportBean;") +
            "" +
            "@name('create-index') context SegmentedByCustomer create index MyIndex on MyInfra(intPrimitive);";
        env.compileDeploy(epl, path);

        env.sendEventBean(new SupportBean_S0(1, "A"));
        env.sendEventBean(new SupportBean_S0(2, "B"));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E1", 1));

        EPCompiled faf = env.compileFAF("select * from MyInfra where intPrimitive = 1", path);
        EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(faf, new ContextPartitionSelector[]{new SupportSelectorById(1)});
        EPAssertionUtil.assertPropsPerRow(result.getArray(), "theString,intPrimitive".split(","), new Object[][]{{"E1", 1}});

        env.sendEventBean(new SupportBean_S1(3, "A"));

        env.undeployAll();
    }

    private static void tryAssertionOnDeleteAndUpdate(RegressionEnvironment env, AtomicInteger milestone, boolean namedWindow) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@Name('context') create context SegmentedByString " +
            "partition by theString from SupportBean, p00 from SupportBean_S0, p10 from SupportBean_S1", path);

        String[] fieldsNW = new String[]{"theString", "intPrimitive"};
        String eplCreate = namedWindow ?
            "@Name('named window') context SegmentedByString create window MyInfra#keepall as SupportBean" :
            "@Name('named window') context SegmentedByString create table MyInfra(theString string primary key, intPrimitive int primary key)";
        env.compileDeploy(eplCreate, path);
        String eplInsert = namedWindow ?
            "@Name('insert') insert into MyInfra select theString, intPrimitive from SupportBean" :
            "@Name('insert') context SegmentedByString insert into MyInfra select theString, intPrimitive from SupportBean";
        env.compileDeploy(eplInsert, path);

        env.compileDeploy("@Name('s0') context SegmentedByString select irstream * from MyInfra", path).addListener("s0");

        // Delete testing
        env.compileDeploy("@Name('on-delete') context SegmentedByString on SupportBean_S0 delete from MyInfra", path);

        env.sendEventBean(new SupportBean("G1", 1));
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsNW, new Object[]{"G1", 1});
        } else {
            assertFalse(env.listener("s0").isInvoked());
        }

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_S0(0, "G0"));
        env.sendEventBean(new SupportBean_S0(0, "G2"));
        assertFalse(env.listener("s0").isInvoked());

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_S0(0, "G1"));
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fieldsNW, new Object[]{"G1", 1});
        }

        env.sendEventBean(new SupportBean("G2", 20));
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsNW, new Object[]{"G2", 20});
        }

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("G3", 3));
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsNW, new Object[]{"G3", 3});
        }

        env.sendEventBean(new SupportBean("G2", 21));
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsNW, new Object[]{"G2", 21});
        }

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_S0(0, "G2"));
        if (namedWindow) {
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fieldsNW, new Object[][]{{"G2", 20}, {"G2", 21}});
        }
        env.listener("s0").reset();

        env.undeployModuleContaining("on-delete");

        // update testing
        env.compileDeploy("@Name('on-merge') context SegmentedByString on SupportBean_S0 update MyInfra set intPrimitive = intPrimitive + 1", path);

        env.sendEventBean(new SupportBean("G4", 4));
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsNW, new Object[]{"G4", 4});
        }

        env.sendEventBean(new SupportBean_S0(0, "G0"));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_S0(0, "G1"));
        env.sendEventBean(new SupportBean_S0(0, "G2"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(0, "G4"));
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fieldsNW, new Object[]{"G4", 5});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fieldsNW, new Object[]{"G4", 4});
            env.listener("s0").reset();
        }

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("G5", 5));
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsNW, new Object[]{"G5", 5});
        }

        env.sendEventBean(new SupportBean_S0(0, "G5"));
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fieldsNW, new Object[]{"G5", 6});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fieldsNW, new Object[]{"G5", 5});
            env.listener("s0").reset();
        }

        env.undeployModuleContaining("on-merge");
        env.undeployAll();
    }

    private static void tryAssertionAggregatedSubquery(RegressionEnvironment env, AtomicInteger milestone, boolean namedWindow) {
        String epl = "";
        epl += "create context SegmentedByString partition by theString from SupportBean, p00 from SupportBean_S0;\n";
        epl += namedWindow ?
            "context SegmentedByString create window MyInfra#keepall as SupportBean;\n" :
            "context SegmentedByString create table MyInfra (theString string primary key, intPrimitive int);\n";
        epl += "@Name('insert') context SegmentedByString insert into MyInfra select theString, intPrimitive from SupportBean;\n";
        epl += "@Audit @name('s0') context SegmentedByString " +
            "select *, (select max(intPrimitive) from MyInfra) as mymax from SupportBean_S0;\n";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 10));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("E2", 20));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_S0(0, "E2"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "mymax".split(","), new Object[]{20});

        env.sendEventBean(new SupportBean_S0(0, "E1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "mymax".split(","), new Object[]{10});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_S0(0, "E3"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "mymax".split(","), new Object[]{null});

        env.undeployAll();
    }
}
