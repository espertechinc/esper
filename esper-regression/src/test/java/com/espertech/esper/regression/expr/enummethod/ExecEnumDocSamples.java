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
package com.espertech.esper.regression.expr.enummethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.lrreport.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ExecEnumDocSamples implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Item", Item.class);
        configuration.addEventType("LocationReport", LocationReport.class);
        configuration.addEventType("Zone", Zone.class);
        configuration.addPlugInSingleRowFunction("inrect", LRUtil.class.getName(), "inrect");
        configuration.addPlugInSingleRowFunction("distance", LRUtil.class.getName(), "distance");
        configuration.addPlugInSingleRowFunction("getZoneNames", Zone.class.getName(), "getZoneNames");
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionHowToUse(epService);
        runAssertionSubquery(epService);
        runAssertionNamedWindow(epService);
        runAssertionAccessAggWindow(epService);
        runAssertionPrevWindow(epService);
        runAssertionProperties(epService);
        runAssertionUDFSingleRow(epService);
        runAssertionDeclared(epService);
        runAssertionExpressions(epService);
        runAssertionScalarArray(epService);
    }

    private void runAssertionHowToUse(EPServiceProvider epService) {
        String eplFragment = "select items.where(i => i.location.x = 0 and i.location.y = 0) as zeroloc from LocationReport";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);

        epService.getEPRuntime().sendEvent(LocationReportFactory.makeSmall());

        Item[] items = toArrayItems((Collection<Item>) listener.assertOneGetNewAndReset().get("zeroloc"));
        assertEquals(1, items.length);
        assertEquals("P00020", items[0].getAssetId());

        stmtFragment.destroy();
        eplFragment = "select items.where(i => i.location.x = 0).where(i => i.location.y = 0) as zeroloc from LocationReport";
        stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);

        epService.getEPRuntime().sendEvent(LocationReportFactory.makeSmall());

        items = toArrayItems((Collection<Item>) listener.assertOneGetNewAndReset().get("zeroloc"));
        assertEquals(1, items.length);
        assertEquals("P00020", items[0].getAssetId());

        stmtFragment.destroy();
    }

    private void runAssertionSubquery(EPServiceProvider epService) throws Exception {

        String eplFragment = "select assetId," +
                "  (select * from Zone#keepall).where(z => inrect(z.rectangle, location)) as zones " +
                "from Item";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);

        epService.getEPRuntime().sendEvent(new Zone("Z1", new Rectangle(0, 0, 20, 20)));
        epService.getEPRuntime().sendEvent(new Zone("Z2", new Rectangle(21, 21, 40, 40)));
        epService.getEPRuntime().sendEvent(new Item("A1", new Location(10, 10)));

        Zone[] zones = toArrayZones((Collection<Zone>) listener.assertOneGetNewAndReset().get("zones"));
        assertEquals(1, zones.length);
        assertEquals("Z1", zones[0].getName());

        // subquery with event as input
        String epl = "create schema SettlementEvent (symbol string, price double);" +
                "create schema PriceEvent (symbol string, price double);\n" +
                "create schema OrderEvent (orderId string, pricedata PriceEvent);\n" +
                "select (select pricedata from OrderEvent#unique(orderId))\n" +
                ".anyOf(v => v.symbol = 'GE') as has_ge from SettlementEvent(symbol = 'GE')";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        // subquery with aggregation
        epService.getEPAdministrator().createEPL("select (select name, count(*) as cnt from Zone#keepall group by name).where(v => cnt > 1) from LocationReport");

        stmtFragment.destroy();
    }

    private void runAssertionNamedWindow(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window ZoneWindow#keepall as Zone");
        epService.getEPAdministrator().createEPL("insert into ZoneWindow select * from Zone");

        String epl = "select ZoneWindow.where(z => inrect(z.rectangle, location)) as zones from Item";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Zone("Z1", new Rectangle(0, 0, 20, 20)));
        epService.getEPRuntime().sendEvent(new Zone("Z2", new Rectangle(21, 21, 40, 40)));
        epService.getEPRuntime().sendEvent(new Item("A1", new Location(10, 10)));

        Zone[] zones = toArrayZones((Collection<Zone>) listener.assertOneGetNewAndReset().get("zones"));
        assertEquals(1, zones.length);
        assertEquals("Z1", zones[0].getName());
        stmt.destroy();

        epl = "select ZoneWindow(name in ('Z4', 'Z5', 'Z3')).where(z => inrect(z.rectangle, location)) as zones from Item";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Zone("Z3", new Rectangle(0, 0, 20, 20)));
        epService.getEPRuntime().sendEvent(new Item("A1", new Location(10, 10)));

        zones = toArrayZones((Collection<Zone>) listener.assertOneGetNewAndReset().get("zones"));
        assertEquals(1, zones.length);
        assertEquals("Z3", zones[0].getName());

        stmt.destroy();
    }

    private void runAssertionAccessAggWindow(EPServiceProvider epService) {
        String epl = "select window(*).where(p => distance(0, 0, p.location.x, p.location.y) < 20) as centeritems " +
                "from Item(type='P')#time(10) group by assetId";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Item("P0001", new Location(10, 10), "P", null));
        Item[] items = toArrayItems((Collection<Item>) listener.assertOneGetNewAndReset().get("centeritems"));
        assertEquals(1, items.length);
        assertEquals("P0001", items[0].getAssetId());

        epService.getEPRuntime().sendEvent(new Item("P0002", new Location(10, 1000), "P", null));
        items = toArrayItems((Collection<Item>) listener.assertOneGetNewAndReset().get("centeritems"));
        assertEquals(0, items.length);

        stmt.destroy();
    }

    private void runAssertionPrevWindow(EPServiceProvider epService) {
        String epl = "select prevwindow(items).where(p => distance(0, 0, p.location.x, p.location.y) < 20) as centeritems " +
                "from Item(type='P')#time(10) as items";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Item("P0001", new Location(10, 10), "P", null));
        Item[] items = toArrayItems((Collection<Item>) listener.assertOneGetNewAndReset().get("centeritems"));
        assertEquals(1, items.length);
        assertEquals("P0001", items[0].getAssetId());

        epService.getEPRuntime().sendEvent(new Item("P0002", new Location(10, 1000), "P", null));
        items = toArrayItems((Collection<Item>) listener.assertOneGetNewAndReset().get("centeritems"));
        assertEquals(1, items.length);
        assertEquals("P0001", items[0].getAssetId());

        stmt.destroy();
    }

    private void runAssertionProperties(EPServiceProvider epService) {
        String epl = "select items.where(p => distance(0, 0, p.location.x, p.location.y) < 20) as centeritems " +
                "from LocationReport";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(LocationReportFactory.makeSmall());
        Item[] items = toArrayItems((Collection<Item>) listener.assertOneGetNewAndReset().get("centeritems"));
        assertEquals(1, items.length);
        assertEquals("P00020", items[0].getAssetId());

        stmt.destroy();
    }

    private void runAssertionUDFSingleRow(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addImport(ZoneFactory.class);

        String epl = "select ZoneFactory.getZones().where(z => inrect(z.rectangle, item.location)) as zones\n" +
                "from Item as item";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Item("A1", new Location(5, 5)));
        Zone[] zones = toArrayZones((Collection<Zone>) listener.assertOneGetNewAndReset().get("zones"));
        assertEquals(1, zones.length);
        assertEquals("Z1", zones[0].getName());

        stmt.destroy();
    }

    private void runAssertionDeclared(EPServiceProvider epService) {
        String epl = "expression passengers {\n" +
                "  lr => lr.items.where(l => l.type='P')\n" +
                "}\n" +
                "select passengers(lr) as p," +
                "passengers(lr).where(x => assetId = 'P01') as p2 from LocationReport lr";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(LocationReportFactory.makeSmall());
        Item[] items = toArrayItems((Collection<Item>) listener.assertOneGetNewAndReset().get("p"));
        assertEquals(2, items.length);
        assertEquals("P00002", items[0].getAssetId());
        assertEquals("P00020", items[1].getAssetId());

        stmt.destroy();
    }

    private void runAssertionExpressions(EPServiceProvider epService) {
        assertStmt(epService, "select items.firstof().assetId as firstcenter from LocationReport");
        assertStmt(epService, "select items.where(p => p.type=\"P\") from LocationReport");
        assertStmt(epService, "select items.where((p,ind) => p.type=\"P\" and ind>2) from LocationReport");
        assertStmt(epService, "select items.aggregate(\"\",(result,item) => result||(case when result=\"\" then \"\" else \",\" end)||item.assetId) as assets from LocationReport");
        assertStmt(epService, "select items.allof(i => distance(i.location.x,i.location.y,0,0)<1000) as assets from LocationReport");
        assertStmt(epService, "select items.average(i => distance(i.location.x,i.location.y,0,0)) as avgdistance from LocationReport");
        assertStmt(epService, "select items.countof(i => distance(i.location.x,i.location.y,0,0)<20) as cntcenter from LocationReport");
        assertStmt(epService, "select items.firstof(i => distance(i.location.x,i.location.y,0,0)<20) as firstcenter from LocationReport");
        assertStmt(epService, "select items.lastof().assetId as firstcenter from LocationReport");
        assertStmt(epService, "select items.lastof(i => distance(i.location.x,i.location.y,0,0)<20) as lastcenter from LocationReport");
        assertStmt(epService, "select items.where(i => i.type=\"L\").groupby(i => assetIdPassenger) as luggagePerPerson from LocationReport");
        assertStmt(epService, "select items.where((p,ind) => p.type=\"P\" and ind>2) from LocationReport");
        assertStmt(epService, "select items.groupby(k => assetId,v => distance(v.location.x,v.location.y,0,0)) as distancePerItem from LocationReport");
        assertStmt(epService, "select items.min(i => distance(i.location.x,i.location.y,0,0)) as mincenter from LocationReport");
        assertStmt(epService, "select items.max(i => distance(i.location.x,i.location.y,0,0)) as maxcenter from LocationReport");
        assertStmt(epService, "select items.minBy(i => distance(i.location.x,i.location.y,0,0)) as minItemCenter from LocationReport");
        assertStmt(epService, "select items.minBy(i => distance(i.location.x,i.location.y,0,0)).assetId as minItemCenter from LocationReport");
        assertStmt(epService, "select items.orderBy(i => distance(i.location.x,i.location.y,0,0)) as itemsOrderedByDist from LocationReport");
        assertStmt(epService, "select items.selectFrom(i => assetId) as itemAssetIds from LocationReport");
        assertStmt(epService, "select items.take(5) as first5Items, items.takeLast(5) as last5Items from LocationReport");
        assertStmt(epService, "select items.toMap(k => k.assetId,v => distance(v.location.x,v.location.y,0,0)) as assetDistance from LocationReport");
        assertStmt(epService, "select items.where(i => i.assetId=\"L001\").union(items.where(i => i.type=\"P\")) as itemsUnion from LocationReport");
        assertStmt(epService, "select (select name from Zone#unique(name)).orderBy() as orderedZones from pattern [every timer:interval(30)]");
        epService.getEPAdministrator().createEPL("create schema MyEvent as (seqone String[], seqtwo String[])");
        assertStmt(epService, "select seqone.sequenceEqual(seqtwo) from MyEvent");
        assertStmt(epService, "select window(assetId).orderBy() as orderedAssetIds from Item#time(10) group by assetId");
        assertStmt(epService, "select prevwindow(assetId).orderBy() as orderedAssetIds from Item#time(10) as items");
        assertStmt(epService, "select getZoneNames().where(z => z!=\"Z1\") from pattern [every timer:interval(30)]");
        assertStmt(epService, "select items.selectFrom(i => new{assetId,distanceCenter=distance(i.location.x,i.location.y,0,0)}) as itemInfo from LocationReport");
        assertStmt(epService, "select items.leastFrequent(i => type) as leastFreqType from LocationReport");

        String epl = "expression myquery {itm => " +
                "(select * from Zone#keepall).where(z => inrect(z.rectangle,itm.location))" +
                "} " +
                "select assetId, myquery(item) as subq, myquery(item).where(z => z.name=\"Z01\") as assetItem " +
                "from Item as item";
        assertStmt(epService, epl);

        assertStmt(epService, "select za.items.except(zb.items) as itemsCompared from LocationReport as za unidirectional, LocationReport#length(10) as zb");
    }

    private void runAssertionScalarArray(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        validate(epService, "{1, 2, 3}.aggregate(0, (result, value) => result + value)", 6);
        validate(epService, "{1, 2, 3}.allOf(v => v > 0)", true);
        validate(epService, "{1, 2, 3}.allOf(v => v > 1)", false);
        validate(epService, "{1, 2, 3}.anyOf(v => v > 1)", true);
        validate(epService, "{1, 2, 3}.anyOf(v => v > 3)", false);
        validate(epService, "{1, 2, 3}.average()", 2.0);
        validate(epService, "{1, 2, 3}.countOf()", 3);
        validate(epService, "{1, 2, 3}.countOf(v => v < 2)", 1);
        validate(epService, "{1, 2, 3}.except({1})", new Object[]{2, 3});
        validate(epService, "{1, 2, 3}.intersect({2,3})", new Object[]{2, 3});
        validate(epService, "{1, 2, 3}.firstOf()", 1);
        validate(epService, "{1, 2, 3}.firstOf(v => v / 2 = 1)", 2);
        validate(epService, "{1, 2, 3}.intersect({2, 3})", new Object[]{2, 3});
        validate(epService, "{1, 2, 3}.lastOf()", 3);
        validate(epService, "{1, 2, 3}.lastOf(v => v < 3)", 2);
        validate(epService, "{1, 2, 3, 2, 1}.leastFrequent()", 3);
        validate(epService, "{1, 2, 3, 2, 1}.max()", 3);
        validate(epService, "{1, 2, 3, 2, 1}.min()", 1);
        validate(epService, "{1, 2, 3, 2, 1, 2}.mostFrequent()", 2);
        validate(epService, "{2, 3, 2, 1}.orderBy()", new Object[]{1, 2, 2, 3});
        validate(epService, "{2, 3, 2, 1}.distinctOf()", new Object[]{2, 3, 1});
        validate(epService, "{2, 3, 2, 1}.reverse()", new Object[]{1, 2, 3, 2});
        validate(epService, "{1, 2, 3}.sequenceEqual({1})", false);
        validate(epService, "{1, 2, 3}.sequenceEqual({1, 2, 3})", true);
        validate(epService, "{1, 2, 3}.sumOf()", 6);
        validate(epService, "{1, 2, 3}.take(2)", new Object[]{1, 2});
        validate(epService, "{1, 2, 3}.takeLast(2)", new Object[]{2, 3});
        validate(epService, "{1, 2, 3}.takeWhile(v => v < 3)", new Object[]{1, 2});
        validate(epService, "{1, 2, 3}.takeWhile((v,ind) => ind < 2)", new Object[]{1, 2});
        validate(epService, "{1, 2, -1, 4, 5, 6}.takeWhile((v,ind) => ind < 5 and v > 0)", new Object[]{1, 2});
        validate(epService, "{1, 2, 3}.takeWhileLast(v => v > 1)", new Object[]{2, 3});
        validate(epService, "{1, 2, 3}.takeWhileLast((v,ind) => ind < 2)", new Object[]{2, 3});
        validate(epService, "{1, 2, -1, 4, 5, 6}.takeWhileLast((v,ind) => ind < 5 and v > 0)", new Object[]{4, 5, 6});
        validate(epService, "{1, 2, 3}.union({4, 5})", new Object[]{1, 2, 3, 4, 5});
        validate(epService, "{1, 2, 3}.where(v => v != 2)", new Object[]{1, 3});
    }

    private void validate(EPServiceProvider epService, String select, Object expected) {
        String epl = "select " + select + " as result from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        Object result = listener.assertOneGetNewAndReset().get("result");

        if (expected instanceof Object[]) {
            Object[] returned = ((Collection) result).toArray();
            EPAssertionUtil.assertEqualsExactOrder((Object[]) expected, returned);
        } else {
            assertEquals(expected, result);
        }

        stmt.destroy();
    }


    private void assertStmt(EPServiceProvider epService, String epl) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.destroy();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());

        stmt = epService.getEPAdministrator().create(model);
        assertEquals(epl, stmt.getText());

        stmt.destroy();
    }

    private Zone[] toArrayZones(Collection<Zone> it) {
        return it.toArray(new Zone[it.size()]);
    }

    private Item[] toArrayItems(Collection<Item> it) {
        return it.toArray(new Item[it.size()]);
    }
}
