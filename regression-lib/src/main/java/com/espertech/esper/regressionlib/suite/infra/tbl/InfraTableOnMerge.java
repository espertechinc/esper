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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableOnMerge {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraTableOnMergeSimple());
        execs.add(new InfraOnMergePlainPropsAnyKeyed());
        execs.add(new InfraMergeWhereWithMethodRead());
        execs.add(new InfraMergeSelectWithAggReadAndEnum());
        execs.add(new InfraMergeTwoTables());
        return execs;
    }

    private static class InfraMergeTwoTables implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                    "@name('T0') create table TableZero(k0 string primary key, v0 int);\n" +
                    "@name('T1') create table TableOne(k1 string primary key, v1 int);\n" +
                    "on SupportBean merge TableZero " +
                    "  where theString = k0 when not matched " +
                    "  then insert select theString as k0, intPrimitive as v0" +
                    "  then insert into TableOne(k1, v1) select theString, intPrimitive;\n";
            env.compileDeploy(epl);

            env.sendEventBean(new SupportBean("E1", 1));
            assertTables(env, new Object[][] {{"E1", 1}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E2", 3));
            assertTables(env, new Object[][] {{"E1", 1}, {"E2", 2}});

            env.undeployAll();
        }

        private void assertTables(RegressionEnvironment env, Object[][] expected) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("T0"), "k0,v0".split(","), expected);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("T1"), "k1,v1".split(","), expected);
        }
    }

    private static class InfraTableOnMergeSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String[] fields = "k1,v1".split(",");

            env.compileDeploy("@name('tbl') create table varaggKV (k1 string primary key, v1 int)", path);
            env.compileDeploy("on SupportBean as sb merge varaggKV as va where sb.theString = va.k1 " +
                "when not matched then insert select theString as k1, intPrimitive as v1 " +
                "when matched then update set v1 = intPrimitive", path);

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertPropsPerRow(env.iterator("tbl"), fields, new Object[][]{{"E1", 10}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 11));
            EPAssertionUtil.assertPropsPerRow(env.iterator("tbl"), fields, new Object[][]{{"E1", 11}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 100));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("tbl"), fields, new Object[][]{{"E1", 11}, {"E2", 100}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E2", 101));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("tbl"), fields, new Object[][]{{"E1", 11}, {"E2", 101}});

            env.undeployAll();
        }
    }

    private static class InfraMergeWhereWithMethodRead implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table varaggMMR (keyOne string primary key, cnt count(*))", path);
            env.compileDeploy("into table varaggMMR select count(*) as cnt " +
                "from SupportBean#lastevent group by theString", path);

            env.compileDeploy("@name('s0') select varaggMMR[p00].keyOne as c0 from SupportBean_S0", path).addListener("s0");
            env.compileDeploy("on SupportBean_S1 merge varaggMMR where cnt = 0 when matched then delete", path);

            env.sendEventBean(new SupportBean("G1", 0));
            env.sendEventBean(new SupportBean("G2", 0));
            assertKeyFound(env, "G1,G2,G3", new boolean[]{true, true, false});

            env.sendEventBean(new SupportBean_S1(0)); // delete
            assertKeyFound(env, "G1,G2,G3", new boolean[]{false, true, false});

            env.milestone(0);

            env.sendEventBean(new SupportBean("G3", 0));
            assertKeyFound(env, "G1,G2,G3", new boolean[]{false, true, true});

            env.sendEventBean(new SupportBean_S1(0));  // delete
            assertKeyFound(env, "G1,G2,G3", new boolean[]{false, false, true});

            env.undeployAll();
        }
    }

    private static class InfraMergeSelectWithAggReadAndEnum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table varaggMS (eventset window(*) @type(SupportBean), total sum(int))", path);
            env.compileDeploy("into table varaggMS select window(*) as eventset, " +
                "sum(intPrimitive) as total from SupportBean#length(2)", path);
            env.compileDeploy("on SupportBean_S0 merge varaggMS " +
                "when matched then insert into ResultStream select eventset, total, eventset.takeLast(1) as c0", path);
            env.compileDeploy("@name('s0') select * from ResultStream", path).addListener("s0");

            SupportBean e1 = new SupportBean("E1", 15);
            env.sendEventBean(e1);

            assertResultAggRead(env, new Object[]{e1}, 15);

            env.milestone(0);

            SupportBean e2 = new SupportBean("E2", 20);
            env.sendEventBean(e2);

            assertResultAggRead(env, new Object[]{e1, e2}, 35);

            env.milestone(1);

            SupportBean e3 = new SupportBean("E3", 30);
            env.sendEventBean(e3);

            assertResultAggRead(env, new Object[]{e2, e3}, 50);

            env.undeployAll();
        }
    }

    private static class InfraOnMergePlainPropsAnyKeyed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runOnMergeInsertUpdDeleteSingleKey(env, false);
            runOnMergeInsertUpdDeleteSingleKey(env, true);

            runOnMergeInsertUpdDeleteTwoKey(env, false);
            runOnMergeInsertUpdDeleteTwoKey(env, true);

            runOnMergeInsertUpdDeleteUngrouped(env, false);
            runOnMergeInsertUpdDeleteUngrouped(env, true);
        }
    }

    private static void runOnMergeInsertUpdDeleteUngrouped(RegressionEnvironment env, boolean soda) {
        RegressionPath path = new RegressionPath();
        String eplDeclare = "create table varaggIUD (p0 string, sumint sum(int))";
        env.compileDeploy(soda, eplDeclare, path);

        String[] fields = "c0,c1".split(",");
        String eplRead = "@name('s0') select varaggIUD.p0 as c0, varaggIUD.sumint as c1, varaggIUD as c2 from SupportBean_S0";
        env.compileDeploy(soda, eplRead, path).addListener("s0");

        // assert selected column types
        Object[][] expectedAggType = new Object[][]{{"c0", String.class}, {"c1", Integer.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        // assert no row
        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

        // create merge
        String eplMerge = "on SupportBean merge varaggIUD" +
            " when not matched then" +
            " insert select theString as p0" +
            " when matched and theString like \"U%\" then" +
            " update set p0=\"updated\"" +
            " when matched and theString like \"D%\" then" +
            " delete";
        env.compileDeploy(soda, eplMerge, path);

        // merge for varagg
        env.sendEventBean(new SupportBean("E1", 0));

        // assert
        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", null});

        // also aggregate-into the same key
        env.compileDeploy(soda, "into table varaggIUD select sum(50) as sumint from SupportBean_S1", path);
        env.sendEventBean(new SupportBean_S1(0));

        env.milestone(0);

        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 50});

        // update for varagg
        env.sendEventBean(new SupportBean("U2", 10));

        env.milestone(1);

        env.sendEventBean(new SupportBean_S0(0));
        EventBean received = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{"updated", 50});
        EPAssertionUtil.assertPropsMap((Map) received.get("c2"), "p0,sumint".split(","), new Object[]{"updated", 50});

        // delete for varagg
        env.sendEventBean(new SupportBean("D3", 0));

        env.milestone(2);

        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

        env.undeployAll();
    }

    private static void runOnMergeInsertUpdDeleteSingleKey(RegressionEnvironment env, boolean soda) {
        String[] fieldsTable = "key,p0,p1,p2,sumint".split(",");
        RegressionPath path = new RegressionPath();
        String eplDeclare = "create table varaggMIU (key int primary key, p0 string, p1 int, p2 int[], sumint sum(int))";
        env.compileDeploy(soda, eplDeclare, path);

        String[] fields = "c0,c1,c2,c3".split(",");
        String eplRead = "@name('s0') select varaggMIU[id].p0 as c0, varaggMIU[id].p1 as c1, varaggMIU[id].p2 as c2, varaggMIU[id].sumint as c3 from SupportBean_S0";
        env.compileDeploy(soda, eplRead, path).addListener("s0");

        // assert selected column types
        Object[][] expectedAggType = new Object[][]{{"c0", String.class}, {"c1", Integer.class}, {"c2", Integer[].class}, {"c3", Integer.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        // assert no row
        env.sendEventBean(new SupportBean_S0(10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

        // create merge
        String eplMerge = "@name('merge') on SupportBean merge varaggMIU" +
            " where intPrimitive=key" +
            " when not matched then" +
            " insert select intPrimitive as key, \"v1\" as p0, 1000 as p1, {1,2} as p2" +
            " when matched and theString like \"U%\" then" +
            " update set p0=\"v2\", p1=2000, p2={3,4}" +
            " when matched and theString like \"D%\" then" +
            " delete";
        env.compileDeploy(soda, eplMerge, path).addListener("merge");

        // merge for varagg[10]
        env.sendEventBean(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(env.listener("merge").assertOneGetNewAndReset(), fieldsTable, new Object[]{10, "v1", 1000, new int[]{1, 2}, null});

        // assert key "10"
        env.sendEventBean(new SupportBean_S0(10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"v1", 1000, new Integer[]{1, 2}, null});

        // also aggregate-into the same key
        env.compileDeploy(soda, "into table varaggMIU select sum(50) as sumint from SupportBean_S1 group by id", path);
        env.sendEventBean(new SupportBean_S1(10));

        env.milestone(0);

        env.sendEventBean(new SupportBean_S0(10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"v1", 1000, new Integer[]{1, 2}, 50});

        // update for varagg[10]
        env.sendEventBean(new SupportBean("U2", 10));
        EPAssertionUtil.assertProps(env.listener("merge").getLastNewData()[0], fieldsTable, new Object[]{10, "v2", 2000, new int[]{3, 4}, 50});
        EPAssertionUtil.assertProps(env.listener("merge").getAndResetLastOldData()[0], fieldsTable, new Object[]{10, "v1", 1000, new int[]{1, 2}, 50});

        env.milestone(1);

        env.sendEventBean(new SupportBean_S0(10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"v2", 2000, new Integer[]{3, 4}, 50});

        // delete for varagg[10]
        env.sendEventBean(new SupportBean("D3", 10));
        EPAssertionUtil.assertProps(env.listener("merge").assertOneGetOldAndReset(), fieldsTable, new Object[]{10, "v2", 2000, new int[]{3, 4}, 50});

        env.milestone(2);

        env.sendEventBean(new SupportBean_S0(10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

        env.undeployAll();
    }

    private static void runOnMergeInsertUpdDeleteTwoKey(RegressionEnvironment env, boolean soda) {
        RegressionPath path = new RegressionPath();
        String eplDeclare = "create table varaggMIUD (keyOne int primary key, keyTwo string primary key, prop string)";
        env.compileDeploy(soda, eplDeclare, path);

        String[] fields = "c0,c1,c2".split(",");
        String eplRead = "@name('s0') select varaggMIUD[id,p00].keyOne as c0, varaggMIUD[id,p00].keyTwo as c1, varaggMIUD[id,p00].prop as c2 from SupportBean_S0";
        env.compileDeploy(soda, eplRead, path).addListener("s0");

        // assert selected column types
        Object[][] expectedAggType = new Object[][]{{"c0", Integer.class}, {"c1", String.class}, {"c2", String.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        // assert no row
        env.sendEventBean(new SupportBean_S0(10, "A"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null});

        // create merge
        String eplMerge = "@name('merge') on SupportBean merge varaggMIUD" +
            " where intPrimitive=keyOne and theString=keyTwo" +
            " when not matched then" +
            " insert select intPrimitive as keyOne, theString as keyTwo, \"inserted\" as prop" +
            " when matched and longPrimitive>0 then" +
            " update set prop=\"updated\"" +
            " when matched and longPrimitive<0 then" +
            " delete";
        env.compileDeploy(soda, eplMerge, path);
        Object[][] expectedType = new Object[][]{{"keyOne", Integer.class}, {"keyTwo", String.class}, {"prop", String.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, env.statement("merge").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        // merge for varagg[10, "A"]
        env.sendEventBean(new SupportBean("A", 10));

        env.milestone(0);

        // assert key {"10", "A"}
        env.sendEventBean(new SupportBean_S0(10, "A"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, "A", "inserted"});

        // update for varagg[10, "A"]
        env.sendEventBean(makeSupportBean("A", 10, 1));

        env.milestone(1);

        env.sendEventBean(new SupportBean_S0(10, "A"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, "A", "updated"});

        // test typable output
        env.compileDeploy("@name('convert') insert into LocalBean select varaggMIUD[10, 'A'] as val0 from SupportBean_S1", path).addListener("convert");
        env.sendEventBean(new SupportBean_S1(2));
        EPAssertionUtil.assertProps(env.listener("convert").assertOneGetNewAndReset(), "val0.keyOne".split(","), new Object[]{10});

        // delete for varagg[10, "A"]
        env.sendEventBean(makeSupportBean("A", 10, -1));

        env.milestone(2);

        env.sendEventBean(new SupportBean_S0(10, "A"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null});

        env.undeployAll();
    }

    private static SupportBean makeSupportBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    private static void assertResultAggRead(RegressionEnvironment env, Object[] objects, int total) {
        String[] fields = "eventset,total".split(",");
        env.sendEventBean(new SupportBean_S0(0));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, fields, new Object[]{objects, total});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{objects[objects.length - 1]}, ((Collection) event.get("c0")).toArray());
    }

    private static void assertKeyFound(RegressionEnvironment env, String keyCsv, boolean[] expected) {
        String[] split = keyCsv.split(",");
        for (int i = 0; i < split.length; i++) {
            String key = split[i];
            env.sendEventBean(new SupportBean_S0(0, key));
            String expectedString = expected[i] ? key : null;
            assertEquals("failed for key '" + key + "'", expectedString, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }
    }

    public static class LocalSubBean {
        private int keyOne;
        private String keyTwo;
        private String prop;

        public int getKeyOne() {
            return keyOne;
        }

        public void setKeyOne(int keyOne) {
            this.keyOne = keyOne;
        }

        public String getKeyTwo() {
            return keyTwo;
        }

        public void setKeyTwo(String keyTwo) {
            this.keyTwo = keyTwo;
        }

        public String getProp() {
            return prop;
        }

        public void setProp(String prop) {
            this.prop = prop;
        }
    }

    public static class LocalBean {
        private LocalSubBean val0;

        public LocalSubBean getVal0() {
            return val0;
        }

        public void setVal0(LocalSubBean val0) {
            this.val0 = val0;
        }
    }
}
