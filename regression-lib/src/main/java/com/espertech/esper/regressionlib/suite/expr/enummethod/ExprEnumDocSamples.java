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
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.lrreport.*;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ExprEnumDocSamples {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumExpressions());
        execs.add(new ExprEnumHowToUse());
        execs.add(new ExprEnumSubquery());
        execs.add(new ExprEnumNamedWindow());
        execs.add(new ExprEnumAccessAggWindow());
        execs.add(new ExprEnumPrevWindow());
        execs.add(new ExprEnumProperties());
        execs.add(new ExprEnumUDFSingleRow());
        execs.add(new ExprEnumScalarArray());
        execs.add(new ExprEnumDeclared());
        return execs;
    }

    private static class ExprEnumHowToUse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplFragment = "@name('s0') select items.where(i => i.location.x = 0 and i.location.y = 0) as zeroloc from LocationReport";
            env.compileDeploy(eplFragment).addListener("s0");

            env.sendEventBean(LocationReportFactory.makeSmall());

            Item[] items = toArrayItems((Collection<Item>) env.listener("s0").assertOneGetNewAndReset().get("zeroloc"));
            assertEquals(1, items.length);
            assertEquals("P00020", items[0].getAssetId());

            env.undeployAll();
            eplFragment = "@name('s0') select items.where(i => i.location.x = 0).where(i => i.location.y = 0) as zeroloc from LocationReport";
            env.compileDeploy(eplFragment).addListener("s0");

            env.sendEventBean(LocationReportFactory.makeSmall());

            items = toArrayItems((Collection<Item>) env.listener("s0").assertOneGetNewAndReset().get("zeroloc"));
            assertEquals(1, items.length);
            assertEquals("P00020", items[0].getAssetId());

            env.undeployAll();
        }
    }

    private static class ExprEnumSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplFragment = "@name('s0') select assetId," +
                "  (select * from Zone#keepall).where(z => inrect(z.rectangle, location)) as zones " +
                "from Item";
            env.compileDeploy(eplFragment).addListener("s0");

            env.sendEventBean(new Zone("Z1", new Rectangle(0, 0, 20, 20)));
            env.sendEventBean(new Zone("Z2", new Rectangle(21, 21, 40, 40)));
            env.sendEventBean(new Item("A1", new Location(10, 10)));

            Zone[] zones = toArrayZones((Collection<Zone>) env.listener("s0").assertOneGetNewAndReset().get("zones"));
            assertEquals(1, zones.length);
            assertEquals("Z1", zones[0].getName());

            // subquery with event as input
            String epl = "create schema SettlementEvent (symbol string, price double);" +
                "create schema PriceEvent (symbol string, price double);\n" +
                "create schema OrderEvent (orderId string, pricedata PriceEvent);\n" +
                "select (select pricedata from OrderEvent#unique(orderId))\n" +
                ".anyOf(v => v.symbol = 'GE') as has_ge from SettlementEvent(symbol = 'GE')";
            env.compileDeploy(epl);

            // subquery with aggregation
            env.compileDeploy("select (select name, count(*) as cnt from Zone#keepall group by name).where(v => cnt > 1) from LocationReport");

            env.undeployAll();
        }
    }

    private static class ExprEnumNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;
            Zone[] zones;

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window ZoneWindow#keepall as Zone", path);
            env.compileDeploy("insert into ZoneWindow select * from Zone", path);

            epl = "@name('s0') select ZoneWindow.where(z => inrect(z.rectangle, location)) as zones from Item";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new Zone("Z1", new Rectangle(0, 0, 20, 20)));
            env.sendEventBean(new Zone("Z2", new Rectangle(21, 21, 40, 40)));
            env.sendEventBean(new Item("A1", new Location(10, 10)));

            zones = toArrayZones((Collection<Zone>) env.listener("s0").assertOneGetNewAndReset().get("zones"));
            assertEquals(1, zones.length);
            assertEquals("Z1", zones[0].getName());

            env.undeployModuleContaining("s0");

            epl = "@name('s0') select ZoneWindow(name in ('Z4', 'Z5', 'Z3')).where(z => inrect(z.rectangle, location)) as zones from Item";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new Zone("Z3", new Rectangle(0, 0, 20, 20)));
            env.sendEventBean(new Item("A1", new Location(10, 10)));

            zones = toArrayZones((Collection<Zone>) env.listener("s0").assertOneGetNewAndReset().get("zones"));
            assertEquals(1, zones.length);
            assertEquals("Z3", zones[0].getName());

            env.undeployAll();
        }
    }

    private static class ExprEnumAccessAggWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select window(*).where(p => distance(0, 0, p.location.x, p.location.y) < 20) as centeritems " +
                "from Item(type='P')#time(10) group by assetId";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new Item("P0001", new Location(10, 10), "P", null));
            Item[] items = toArrayItems((Collection<Item>) env.listener("s0").assertOneGetNewAndReset().get("centeritems"));
            assertEquals(1, items.length);
            assertEquals("P0001", items[0].getAssetId());

            env.sendEventBean(new Item("P0002", new Location(10, 1000), "P", null));
            items = toArrayItems((Collection<Item>) env.listener("s0").assertOneGetNewAndReset().get("centeritems"));
            assertEquals(0, items.length);

            env.undeployAll();
        }
    }

    private static class ExprEnumPrevWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select prevwindow(items).where(p => distance(0, 0, p.location.x, p.location.y) < 20) as centeritems " +
                "from Item(type='P')#time(10) as items";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new Item("P0001", new Location(10, 10), "P", null));
            Item[] items = toArrayItems((Collection<Item>) env.listener("s0").assertOneGetNewAndReset().get("centeritems"));
            assertEquals(1, items.length);
            assertEquals("P0001", items[0].getAssetId());

            env.sendEventBean(new Item("P0002", new Location(10, 1000), "P", null));
            items = toArrayItems((Collection<Item>) env.listener("s0").assertOneGetNewAndReset().get("centeritems"));
            assertEquals(1, items.length);
            assertEquals("P0001", items[0].getAssetId());

            env.undeployAll();
        }
    }

    private static class ExprEnumProperties implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select items.where(p => distance(0, 0, p.location.x, p.location.y) < 20) as centeritems " +
                "from LocationReport";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(LocationReportFactory.makeSmall());
            Item[] items = toArrayItems((Collection<Item>) env.listener("s0").assertOneGetNewAndReset().get("centeritems"));
            assertEquals(1, items.length);
            assertEquals("P00020", items[0].getAssetId());

            env.undeployAll();
        }
    }

    private static class ExprEnumUDFSingleRow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select ZoneFactory.getZones().where(z => inrect(z.rectangle, item.location)) as zones\n" +
                "from Item as item";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new Item("A1", new Location(5, 5)));
            Zone[] zones = toArrayZones((Collection<Zone>) env.listener("s0").assertOneGetNewAndReset().get("zones"));
            assertEquals(1, zones.length);
            assertEquals("Z1", zones[0].getName());

            env.undeployAll();
        }
    }

    private static class ExprEnumDeclared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') expression passengers {\n" +
                "  lr => lr.items.where(l => l.type='P')\n" +
                "}\n" +
                "select passengers(lr) as p," +
                "passengers(lr).where(x => assetId = 'P01') as p2 from LocationReport lr";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(LocationReportFactory.makeSmall());
            Item[] items = toArrayItems((Collection<Item>) env.listener("s0").assertOneGetNewAndReset().get("p"));
            assertEquals(2, items.length);
            assertEquals("P00002", items[0].getAssetId());
            assertEquals("P00020", items[1].getAssetId());

            env.undeployAll();
        }
    }

    private static class ExprEnumExpressions implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            assertStmt(env, path, "select items.firstof().assetId as firstcenter from LocationReport");
            assertStmt(env, path, "select items.where(p => p.type=\"P\") from LocationReport");
            assertStmt(env, path, "select items.where((p,ind) => p.type=\"P\" and ind>2) from LocationReport");
            assertStmt(env, path, "select items.aggregate(\"\",(result,item) => result||(case when result=\"\" then \"\" else \",\" end)||item.assetId) as assets from LocationReport");
            assertStmt(env, path, "select items.allof(i => distance(i.location.x,i.location.y,0,0)<1000) as assets from LocationReport");
            assertStmt(env, path, "select items.average(i => distance(i.location.x,i.location.y,0,0)) as avgdistance from LocationReport");
            assertStmt(env, path, "select items.countof(i => distance(i.location.x,i.location.y,0,0)<20) as cntcenter from LocationReport");
            assertStmt(env, path, "select items.firstof(i => distance(i.location.x,i.location.y,0,0)<20) as firstcenter from LocationReport");
            assertStmt(env, path, "select items.lastof().assetId as firstcenter from LocationReport");
            assertStmt(env, path, "select items.lastof(i => distance(i.location.x,i.location.y,0,0)<20) as lastcenter from LocationReport");
            assertStmt(env, path, "select items.where(i => i.type=\"L\").groupby(i => assetIdPassenger) as luggagePerPerson from LocationReport");
            assertStmt(env, path, "select items.where((p,ind) => p.type=\"P\" and ind>2) from LocationReport");
            assertStmt(env, path, "select items.groupby(k => assetId,v => distance(v.location.x,v.location.y,0,0)) as distancePerItem from LocationReport");
            assertStmt(env, path, "select items.min(i => distance(i.location.x,i.location.y,0,0)) as mincenter from LocationReport");
            assertStmt(env, path, "select items.max(i => distance(i.location.x,i.location.y,0,0)) as maxcenter from LocationReport");
            assertStmt(env, path, "select items.minBy(i => distance(i.location.x,i.location.y,0,0)) as minItemCenter from LocationReport");
            assertStmt(env, path, "select items.minBy(i => distance(i.location.x,i.location.y,0,0)).assetId as minItemCenter from LocationReport");
            assertStmt(env, path, "select items.orderBy(i => distance(i.location.x,i.location.y,0,0)) as itemsOrderedByDist from LocationReport");
            assertStmt(env, path, "select items.selectFrom(i => assetId) as itemAssetIds from LocationReport");
            assertStmt(env, path, "select items.take(5) as first5Items, items.takeLast(5) as last5Items from LocationReport");
            assertStmt(env, path, "select items.toMap(k => k.assetId,v => distance(v.location.x,v.location.y,0,0)) as assetDistance from LocationReport");
            assertStmt(env, path, "select items.where(i => i.assetId=\"L001\").union(items.where(i => i.type=\"P\")) as itemsUnion from LocationReport");
            assertStmt(env, path, "select (select name from Zone#unique(name)).orderBy() as orderedZones from pattern [every timer:interval(30)]");

            env.compileDeployWBusPublicType("create schema MyEvent as (seqone String[], seqtwo String[])", path);

            assertStmt(env, path, "select seqone.sequenceEqual(seqtwo) from MyEvent");
            assertStmt(env, path, "select window(assetId).orderBy() as orderedAssetIds from Item#time(10) group by assetId");
            assertStmt(env, path, "select prevwindow(assetId).orderBy() as orderedAssetIds from Item#time(10) as items");
            assertStmt(env, path, "select getZoneNames().where(z => z!=\"Z1\") from pattern [every timer:interval(30)]");
            assertStmt(env, path, "select items.selectFrom(i => new{assetId,distanceCenter=distance(i.location.x,i.location.y,0,0)}) as itemInfo from LocationReport");
            assertStmt(env, path, "select items.leastFrequent(i => type) as leastFreqType from LocationReport");

            String epl = "expression myquery {itm => " +
                "(select * from Zone#keepall).where(z => inrect(z.rectangle,itm.location))" +
                "} " +
                "select assetId, myquery(item) as subq, myquery(item).where(z => z.name=\"Z01\") as assetItem " +
                "from Item as item";
            assertStmt(env, path, epl);

            assertStmt(env, path, "select za.items.except(zb.items) as itemsCompared from LocationReport as za unidirectional, LocationReport#length(10) as zb");

            env.undeployAll();
        }
    }

    private static class ExprEnumScalarArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            validate(env, "{1, 2, 3}.aggregate(0, (result, value) => result + value)", 6);
            validate(env, "{1, 2, 3}.allOf(v => v > 0)", true);
            validate(env, "{1, 2, 3}.allOf(v => v > 1)", false);
            validate(env, "{1, 2, 3}.anyOf(v => v > 1)", true);
            validate(env, "{1, 2, 3}.anyOf(v => v > 3)", false);
            validate(env, "{1, 2, 3}.average()", 2.0);
            validate(env, "{1, 2, 3}.countOf()", 3);
            validate(env, "{1, 2, 3}.countOf(v => v < 2)", 1);
            validate(env, "{1, 2, 3}.except({1})", new Object[]{2, 3});
            validate(env, "{1, 2, 3}.intersect({2,3})", new Object[]{2, 3});
            validate(env, "{1, 2, 3}.firstOf()", 1);
            validate(env, "{1, 2, 3}.firstOf(v => v / 2 = 1)", 2);
            validate(env, "{1, 2, 3}.intersect({2, 3})", new Object[]{2, 3});
            validate(env, "{1, 2, 3}.lastOf()", 3);
            validate(env, "{1, 2, 3}.lastOf(v => v < 3)", 2);
            validate(env, "{1, 2, 3, 2, 1}.leastFrequent()", 3);
            validate(env, "{1, 2, 3, 2, 1}.max()", 3);
            validate(env, "{1, 2, 3, 2, 1}.min()", 1);
            validate(env, "{1, 2, 3, 2, 1, 2}.mostFrequent()", 2);
            validate(env, "{2, 3, 2, 1}.orderBy()", new Object[]{1, 2, 2, 3});
            validate(env, "{2, 3, 2, 1}.distinctOf()", new Object[]{2, 3, 1});
            validate(env, "{2, 3, 2, 1}.reverse()", new Object[]{1, 2, 3, 2});
            validate(env, "{1, 2, 3}.sequenceEqual({1})", false);
            validate(env, "{1, 2, 3}.sequenceEqual({1, 2, 3})", true);
            validate(env, "{1, 2, 3}.sumOf()", 6);
            validate(env, "{1, 2, 3}.take(2)", new Object[]{1, 2});
            validate(env, "{1, 2, 3}.takeLast(2)", new Object[]{2, 3});
            validate(env, "{1, 2, 3}.takeWhile(v => v < 3)", new Object[]{1, 2});
            validate(env, "{1, 2, 3}.takeWhile((v,ind) => ind < 2)", new Object[]{1, 2});
            validate(env, "{1, 2, -1, 4, 5, 6}.takeWhile((v,ind) => ind < 5 and v > 0)", new Object[]{1, 2});
            validate(env, "{1, 2, 3}.takeWhileLast(v => v > 1)", new Object[]{2, 3});
            validate(env, "{1, 2, 3}.takeWhileLast((v,ind) => ind < 2)", new Object[]{2, 3});
            validate(env, "{1, 2, -1, 4, 5, 6}.takeWhileLast((v,ind) => ind < 5 and v > 0)", new Object[]{4, 5, 6});
            validate(env, "{1, 2, 3}.union({4, 5})", new Object[]{1, 2, 3, 4, 5});
            validate(env, "{1, 2, 3}.where(v => v != 2)", new Object[]{1, 3});
        }
    }

    private static void validate(RegressionEnvironment env, String select, Object expected) {
        String epl = "@name('s0') select " + select + " as result from SupportBean";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 0));
        Object result = env.listener("s0").assertOneGetNewAndReset().get("result");

        if (expected instanceof Object[]) {
            Object[] returned = ((Collection) result).toArray();
            EPAssertionUtil.assertEqualsExactOrder((Object[]) expected, returned);
        } else {
            assertEquals(expected, result);
        }

        env.undeployAll();
    }

    private static void assertStmt(RegressionEnvironment env, RegressionPath path, String epl) {
        env.compileDeploy("@name('s0')" + epl, path).undeployModuleContaining("s0");
        env.eplToModelCompileDeploy("@name('s0') " + epl, path).undeployModuleContaining("s0");
    }

    private static Zone[] toArrayZones(Collection<Zone> it) {
        return it.toArray(new Zone[it.size()]);
    }

    private static Item[] toArrayItems(Collection<Item> it) {
        return it.toArray(new Item[it.size()]);
    }
}
