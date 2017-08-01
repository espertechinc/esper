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
package com.espertech.esper.regression.spatial;

import com.espertech.esper.client.*;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.epl.join.util.QueryPlanIndexDescOnExpr;
import com.espertech.esper.epl.join.util.QueryPlanIndexDescSubquery;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.supportregression.bean.SupportSpatialAABB;
import com.espertech.esper.supportregression.bean.SupportSpatialDualPoint;
import com.espertech.esper.supportregression.bean.SupportSpatialPoint;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.supportregression.util.SupportSpatialUtil;

import java.util.*;

import static com.espertech.esper.supportregression.util.SupportSpatialUtil.*;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecSpatialPointRegionQuadTreeEventIndex implements RegressionExecution {
    private final static List<BoundingBox> BOXES = Arrays.asList(
            new BoundingBox(0, 0, 50, 50),
            new BoundingBox(50, 0, 100, 50),
            new BoundingBox(0, 50, 50, 100),
            new BoundingBox(50, 50, 100, 100),
            new BoundingBox(25, 25, 75, 75)
    );

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : Arrays.asList(SupportSpatialPoint.class, SupportSpatialAABB.class, MyEventRectangleWithOffset.class, SupportSpatialDualPoint.class)) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionEventIndexUnindexed(epService);

        runAssertionEventIndexUnusedOnTrigger(epService);
        runAssertionEventIndexUnusedNamedWindowFireAndForget(epService);

        runAssertionEventIndexOnTriggerNWInsertRemove(epService, false);
        runAssertionEventIndexOnTriggerNWInsertRemove(epService, true);
        runAssertionEventIndexOnTriggerContextParameterized(epService);
        runAssertionEventIndexSubqNamedWindowIndexShare(epService);
        runAssertionEventIndexOnTriggerTable(epService);
        runAssertionEventIndexChoiceOfTwo(epService);
        runAssertionEventIndexExpression(epService);
        runAssertionEventIndexUnique(epService);
        runAssertionEventIndexPerformance(epService);
        runAssertionEventIndexChoiceBetweenIndexTypes(epService);
        runAssertionEventIndexTableFireAndForget(epService);
        runAssertionEventIndexNWFireAndForgetPerformance(epService);
    }

    private void runAssertionEventIndexNWFireAndForgetPerformance(EPServiceProvider epService) throws Exception {
        String epl = "create window MyPointWindow#keepall as (id string, px double, py double);\n" +
                "insert into MyPointWindow select id, px, py from SupportSpatialPoint;\n" +
                "create index Idx on MyPointWindow( (px, py) pointregionquadtree(0, 0, 100, 100));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();

        Random random = new Random();
        List<SupportSpatialPoint> points = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            double px = random.nextDouble() * 100;
            double py = random.nextDouble() * 100;
            SupportSpatialPoint point = new SupportSpatialPoint("P" + Integer.toString(i), px, py);
            epService.getEPRuntime().sendEvent(point);
            points.add(point);
            // Comment-me-in: log.info("Point P" + i + " " + px + " " + py);
        }

        EPOnDemandPreparedQueryParameterized prepared = epService.getEPRuntime().prepareQueryWithParameters("select * from MyPointWindow where point(px,py).inside(rectangle(?,?,?,?))");
        long start = System.currentTimeMillis();
        String[] fields = "id".split(",");
        for (int i = 0; i < 500; i++) {
            double x = random.nextDouble() * 100;
            double y = random.nextDouble() * 100;
            // Comment-me-in: log.info("Query " + x + " " + y + " " + width + " " + height);

            prepared.setObject(1, x);
            prepared.setObject(2, y);
            prepared.setObject(3, 5);
            prepared.setObject(4, 5);
            EventBean[] events = epService.getEPRuntime().executeQuery(prepared).getArray();
            Object[][] expected = SupportSpatialUtil.getExpected(points, x, y, 5, 5);
            EPAssertionUtil.assertPropsPerRowAnyOrder(events, fields, expected);
        }
        long delta = System.currentTimeMillis() - start;
        assertTrue("delta=" + delta, delta < 1000);

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionEventIndexChoiceBetweenIndexTypes(EPServiceProvider epService) throws Exception {
        String epl = "@Name('win') create window MyPointWindow#keepall as (id string, category string, px double, py double);\n" +
                "@Name('insert') insert into MyPointWindow select id, category, px, py from SupportSpatialPoint;\n" +
                "@Name('idx1') create index IdxHash on MyPointWindow(category);\n" +
                "@Name('idx2') create index IdxQuadtree on MyPointWindow((px, py) pointregionquadtree(0, 0, 100, 100));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();

        sendPoint(epService, "P1", 10, 15, "X");
        sendPoint(epService, "P2", 10, 15, "Y");
        sendPoint(epService, "P3", 10, 15, "Z");

        assertIndexChoice(epService, "", "IdxQuadtree");
        assertIndexChoice(epService, "@Hint('index(IdxHash, bust)')", "IdxQuadtree");

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionEventIndexUnique(EPServiceProvider epService) throws Exception {
        String epl = "@Name('win') create window MyPointWindow#keepall as (id string, px double, py double);\n" +
                "@Name('insert') insert into MyPointWindow select id, px, py from SupportSpatialPoint;\n" +
                "@Name('idx') create unique index Idx on MyPointWindow( (px, py) pointregionquadtree(0, 0, 100, 100));\n" +
                "@Name('out') on SupportSpatialAABB select mpw.id as c0 from MyPointWindow as mpw where point(px, py).inside(rectangle(x, y, width, height));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        sendPoint(epService, "P1", 10, 15);
        try {
            sendPoint(epService, "P2", 10, 15);
            fail();
        } catch (RuntimeException ex) { // we have a handler
            SupportMessageAssertUtil.assertMessage(ex,
                    "Unexpected exception in statement 'win': Unique index violation, index 'Idx' is a unique index and key '(10.0,15.0)' already exists");
        }

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionEventIndexPerformance(EPServiceProvider epService) throws Exception {
        String epl = "create window MyPointWindow#keepall as (id string, px double, py double);\n" +
                "insert into MyPointWindow select id, px, py from SupportSpatialPoint;\n" +
                "create index Idx on MyPointWindow( (px, py) pointregionquadtree(0, 0, 100, 100));\n" +
                "@Name('out') on SupportSpatialAABB select mpw.id as c0 from MyPointWindow as mpw where point(px, py).inside(rectangle(x, y, width, height));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 100; y++) {
                epService.getEPRuntime().sendEvent(new SupportSpatialPoint(Integer.toString(x) + "_" + Integer.toString(y), (double) x, (double) y));
            }
        }

        long start = System.currentTimeMillis();
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 100; y++) {
                epService.getEPRuntime().sendEvent(new SupportSpatialAABB("R", x, y, 0.5, 0.5));
                assertEquals(Integer.toString(x) + "_" + Integer.toString(y), listener.assertOneGetNewAndReset().get("c0"));
            }
        }
        long delta = System.currentTimeMillis() - start;
        assertTrue("delta=" + delta, delta < 1000);

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionEventIndexUnusedNamedWindowFireAndForget(EPServiceProvider epService) throws Exception {
        String epl = "@Resilient create window PointWindow#keepall as (id string, px double, py double);\n" +
                "@Resilient create index MyIndex on PointWindow((px,py) pointregionquadtree(0,0,100,100,2,12));\n" +
                "@Resilient insert into PointWindow select id, px, py from SupportSpatialPoint;\n" +
                "@Resilient @name('out') on SupportSpatialAABB as aabb select pt.id as c0 from PointWindow as pt where point(px,py).inside(rectangle(x,y,width,height));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();

        epService.getEPRuntime().executeQuery("delete from PointWindow where id='P1'");

        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(deploymentId);
    }

    private void runAssertionEventIndexTableFireAndForget(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create table MyTable(id string primary key, tx double, ty double)");
        epService.getEPAdministrator().createEPL("insert into MyTable select id, px as tx, py as ty from SupportSpatialPoint");
        epService.getEPRuntime().sendEvent(new SupportSpatialPoint("P1", 50d, 50d));
        epService.getEPRuntime().sendEvent(new SupportSpatialPoint("P2", 49d, 49d));
        epService.getEPAdministrator().createEPL("create index MyIdxWithExpr on MyTable( (tx, ty) pointregionquadtree(0, 0, 100, 100))");

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select id as c0 from MyTable where point(tx, ty).inside(rectangle(45, 45, 10, 10))");
        SupportQueryPlanIndexHook.assertFAFAndReset("MyIdxWithExpr", "EventTableQuadTreePointRegionImpl");
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), "c0".split(","), new Object[][]{{"P1"}, {"P2"}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEventIndexExpression(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create table MyTable(id string primary key, tx double, ty double)");
        epService.getEPRuntime().executeQuery("insert into MyTable values ('P1', 50, 30)");
        epService.getEPRuntime().executeQuery("insert into MyTable values ('P2', 50, 28)");
        epService.getEPRuntime().executeQuery("insert into MyTable values ('P3', 50, 30)");
        epService.getEPRuntime().executeQuery("insert into MyTable values ('P4', 49, 29)");
        epService.getEPAdministrator().createEPL("create index MyIdxWithExpr on MyTable( (tx*10, ty*10) pointregionquadtree(0, 0, 1000, 1000))");
        SupportUpdateListener listener = new SupportUpdateListener();

        String eplOne = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportSpatialAABB select tbl.id as c0 from MyTable as tbl where point(tx, ty).inside(rectangle(x, y, width, height))";
        EPStatement statementOne = epService.getEPAdministrator().createEPL(eplOne);
        statementOne.addListener(listener);
        SupportQueryPlanIndexHook.assertOnExprTableAndReset(null, null);
        assertRectanglesManyRow(epService, listener, BOXES, "P4", "P1,P2,P3", null, null, "P1,P2,P3,P4");
        statementOne.destroy();

        String eplTwo = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportSpatialAABB select tbl.id as c0 from MyTable as tbl where point(tx*10, tbl.ty*10).inside(rectangle(x, y, width, height))";
        EPStatement statementTwo = epService.getEPAdministrator().createEPL(eplTwo);
        statementTwo.addListener(listener);
        SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIdxWithExpr", "non-unique hash={} btree={} advanced={pointregionquadtree(tx*10,ty*10)}");
        assertRectanglesManyRow(epService, listener, BOXES, null, null, null, null, null);
        assertRectanglesManyRow(epService, listener, Collections.singletonList(new BoundingBox(500, 300, 501, 301)), "P1,P3");
        statementTwo.destroy();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEventIndexChoiceOfTwo(EPServiceProvider epService) throws Exception {
        String epl =
                "create table MyPointTable(" +
                        " id string primary key," +
                        " x1 double, y1 double, \n" +
                        " x2 double, y2 double);\n" +
                        "create index Idx1 on MyPointTable( (x1, y1) pointregionquadtree(0, 0, 100, 100));\n" +
                        "create index Idx2 on MyPointTable( (x2, y2) pointregionquadtree(0, 0, 100, 100));\n" +
                        "on SupportSpatialDualPoint dp merge MyPointTable t where dp.id = t.id when not matched then insert select dp.id as id,x1,y1,x2,y2;\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();

        String textOne = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportSpatialAABB select tbl.id as c0 from MyPointTable as tbl where point(x1, y1).inside(rectangle(x, y, width, height))";
        EPStatement statementOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listener = new SupportUpdateListener();
        statementOne.addListener(listener);
        SupportQueryPlanIndexHook.assertOnExprTableAndReset("Idx1", "non-unique hash={} btree={} advanced={pointregionquadtree(x1,y1)}");

        String textTwo = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportSpatialAABB select tbl.id as c0 from MyPointTable as tbl where point(tbl.x2, y2).inside(rectangle(x, y, width, height))";
        EPStatement statementTwo = epService.getEPAdministrator().createEPL(textTwo);
        statementTwo.addListener(listener);
        SupportQueryPlanIndexHook.assertOnExprTableAndReset("Idx2", "non-unique hash={} btree={} advanced={pointregionquadtree(x2,y2)}");

        epService.getEPRuntime().sendEvent(new SupportSpatialDualPoint("P1", 10, 10, 60, 60));
        epService.getEPRuntime().sendEvent(new SupportSpatialDualPoint("P2", 55, 20, 4, 88));

        assertRectanglesSingleValue(epService, listener, BOXES, "P1", "P2", "P2", "P1", "P1");

        statementOne.destroy();
        statementTwo.destroy();
        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionEventIndexSubqNamedWindowIndexShare(EPServiceProvider epService) throws Exception {
        String epl = "@Hint('enable_window_subquery_indexshare') create window MyWindow#length(5) as select * from SupportSpatialPoint;\n" +
                "create index MyIndex on MyWindow((px,py) pointregionquadtree(0,0,100,100));\n" +
                "insert into MyWindow select * from SupportSpatialPoint;\n" +
                IndexBackingTableInfo.INDEX_CALLBACK_HOOK +
                "@name('out') select (select id from MyWindow as mw where point(mw.px,mw.py).inside(rectangle(aabb.x,aabb.y,aabb.width,aabb.height))).aggregate('', \n" +
                "  (result, item) => result || (case when result='' then '' else ',' end) || item) as c0 from SupportSpatialAABB aabb";
        DeploymentResult deploymentResult = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        QueryPlanIndexDescSubquery subquery = SupportQueryPlanIndexHook.assertSubqueryAndReset();
        assertEquals("non-unique hash={} btree={} advanced={pointregionquadtree(px,py)}", subquery.getTables()[0].getIndexDesc());
        assertEquals("MyIndex", subquery.getTables()[0].getIndexName());

        sendPoint(epService, "P1", 10, 40);
        assertRectanglesSingleValue(epService, listener, BOXES, "P1", "", "", "", "");

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentResult.getDeploymentId());
    }

    private void runAssertionEventIndexUnusedOnTrigger(EPServiceProvider epService) throws Exception {
        String epl = "create window MyWindow#length(5) as select * from SupportSpatialPoint;\n" +
                "create index MyIndex on MyWindow((px,py) pointregionquadtree(0,0,100,100));\n" +
                "insert into MyWindow select * from SupportSpatialPoint;\n";
        DeploymentResult deploymentResult = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        sendPoint(epService, "P1", 5, 5);
        sendPoint(epService, "P2", 55, 60);

        runIndexUnusedConstantsOnly(epService);
        runIndexUnusedPointValueDepends(epService);
        runIndexUnusedRectValueDepends(epService);

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentResult.getDeploymentId());
    }

    private void runIndexUnusedRectValueDepends(EPServiceProvider epService) {
        String epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@name('out') on SupportSpatialAABB as aabb select points.id as c0 " +
                "from MyWindow as points where point(px, py).inside(rectangle(px,py,1,1))";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset(null, null);

        assertRectanglesManyRow(epService, listener, BOXES, "P1,P2", "P1,P2", "P1,P2", "P1,P2", "P1,P2");

        stmt.destroy();
    }

    private void runIndexUnusedPointValueDepends(EPServiceProvider epService) {
        String epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@name('out') on SupportSpatialAABB as aabb select points.id as c0 " +
                "from MyWindow as points where point(px + x, py + y).inside(rectangle(x,y,width,height))";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset(null, null);

        assertRectanglesManyRow(epService, listener, BOXES, "P1", "P1", "P1", "P1", "P1");

        stmt.destroy();
    }

    private void runIndexUnusedConstantsOnly(EPServiceProvider epService) {
        String epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@name('out') on SupportSpatialAABB as aabb select points.id as c0 " +
                "from MyWindow as points where point(0, 0).inside(rectangle(x,y,width,height))";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset(null, null);

        assertRectanglesManyRow(epService, listener, BOXES, "P1,P2", null, null, null, null);

        stmt.destroy();
    }

    private void runAssertionEventIndexUnindexed(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select point(xOffset, yOffset).inside(rectangle(x, y, width, height)) as c0 from MyEventRectangleWithOffset");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendAssert(epService, listener, 1, 1, 0, 0, 2, 2, true);
        sendAssert(epService, listener, 3, 1, 0, 0, 2, 2, false);
        sendAssert(epService, listener, 2, 1, 0, 0, 2, 2, false);
        sendAssert(epService, listener, 1, 3, 0, 0, 2, 2, false);
        sendAssert(epService, listener, 1, 2, 0, 0, 2, 2, false);
        sendAssert(epService, listener, 0, 0, 1, 1, 2, 2, false);
        sendAssert(epService, listener, 1, 0, 1, 1, 2, 2, false);
        sendAssert(epService, listener, 0, 1, 1, 1, 2, 2, false);
        sendAssert(epService, listener, 1, 1, 1, 1, 2, 2, true);
        sendAssert(epService, listener, 2.9999, 2.9999, 1, 1, 2, 2, true);
        sendAssert(epService, listener, 3, 2.9999, 1, 1, 2, 2, false);
        sendAssert(epService, listener, 2.9999, 3, 1, 1, 2, 2, false);
        sendAssertWNull(epService, listener, null, 0d, 0d, 0d, 0d, 0d, null);
        sendAssertWNull(epService, listener, 0d, 0d, 0d, null, 0d, 0d, null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEventIndexOnTriggerContextParameterized(EPServiceProvider epService) throws Exception {
        String epl = "create context CtxBox initiated by MyEventRectangleWithOffset box;\n" +
                "context CtxBox create window MyWindow#keepall as SupportSpatialPoint;\n" +
                "context CtxBox create index MyIndex on MyWindow((px+context.box.xOffset, py+context.box.yOffset) pointregionquadtree(context.box.x, context.box.y, context.box.width, context.box.height));\n" +
                "context CtxBox on SupportSpatialPoint(category = context.box.id) merge MyWindow when not matched then insert select *;\n" +
                "@name('out') context CtxBox on SupportSpatialAABB(category = context.box.id) aabb " +
                "  select points.id as c0 from MyWindow points where point(px, py).inside(rectangle(x, y, width, height))";
        DeploymentResult deploymentResult = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        epService.getEPRuntime().sendEvent(new MyEventRectangleWithOffset("NW", 0d, 0d, 0d, 0d, 50d, 50d));
        epService.getEPRuntime().sendEvent(new MyEventRectangleWithOffset("SE", 0d, 0d, 50d, 50d, 50d, 50d));
        sendPoint(epService, "P1", 60, 90, "SE");
        sendPoint(epService, "P2", 5, 20, "NW");

        epService.getEPRuntime().sendEvent(new SupportSpatialAABB("R1", 60, 60, 40, 40, "SE"));
        assertEquals("P1", listener.assertOneGetNewAndReset().get("c0"));

        epService.getEPRuntime().sendEvent(new SupportSpatialAABB("R2", 0, 0, 5.0001, 20.0001, "NW"));
        assertEquals("P2", listener.assertOneGetNewAndReset().get("c0"));

        epService.getEPRuntime().sendEvent(new SupportSpatialAABB("R3", 0, 0, 5, 30, "NW"));
        epService.getEPRuntime().sendEvent(new SupportSpatialAABB("R3", 0, 0, 30, 20, "NW"));
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentResult.getDeploymentId());
    }

    private void runAssertionEventIndexOnTriggerNWInsertRemove(EPServiceProvider epService, boolean soda) {
        SupportModelHelper.createByCompileOrParse(epService, soda, "create window MyWindow#length(5) as select * from SupportSpatialPoint");
        SupportModelHelper.createByCompileOrParse(epService, soda, "create index MyIndex on MyWindow((px,py) pointregionquadtree(0,0,100,100))");
        SupportModelHelper.createByCompileOrParse(epService, soda, "insert into MyWindow select * from SupportSpatialPoint");

        String epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + " on SupportSpatialAABB as aabb " +
                "select points.id as c0 from MyWindow as points where point(px,py).inside(rectangle(x,y,width,height))";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIndex", "non-unique hash={} btree={} advanced={pointregionquadtree(px,py)}");

        sendPoint(epService, "P1", 10, 40);
        assertRectanglesManyRow(epService, listener, BOXES, "P1", null, null, null, null);

        sendPoint(epService, "P2", 80, 80);
        assertRectanglesManyRow(epService, listener, BOXES, "P1", null, null, "P2", null);

        sendPoint(epService, "P3", 10, 40);
        assertRectanglesManyRow(epService, listener, BOXES, "P1,P3", null, null, "P2", null);

        sendPoint(epService, "P4", 60, 40);
        assertRectanglesManyRow(epService, listener, BOXES, "P1,P3", "P4", null, "P2", "P4");

        sendPoint(epService, "P5", 20, 75);
        assertRectanglesManyRow(epService, listener, BOXES, "P1,P3", "P4", "P5", "P2", "P4");

        sendPoint(epService, "P6", 50, 50);
        assertRectanglesManyRow(epService, listener, BOXES, "P3", "P4", "P5", "P2,P6", "P4,P6");

        sendPoint(epService, "P7", 0, 0);
        assertRectanglesManyRow(epService, listener, BOXES, "P3,P7", "P4", "P5", "P6", "P4,P6");

        sendPoint(epService, "P8", 99.999, 0);
        assertRectanglesManyRow(epService, listener, BOXES, "P7", "P4,P8", "P5", "P6", "P4,P6");

        sendPoint(epService, "P9", 0, 99.999);
        assertRectanglesManyRow(epService, listener, BOXES, "P7", "P8", "P5,P9", "P6", "P6");

        sendPoint(epService, "P10", 99.999, 99.999);
        assertRectanglesManyRow(epService, listener, BOXES, "P7", "P8", "P9", "P6,P10", "P6");

        sendPoint(epService, "P11", 0, 0);
        assertRectanglesManyRow(epService, listener, BOXES, "P7,P11", "P8", "P9", "P10", null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEventIndexOnTriggerTable(EPServiceProvider epService) throws Exception {
        String epl =
                "create table MyPointTable(my_x double primary key, my_y double primary key, my_id string);\n" +
                        "@Audit create index MyIndex on MyPointTable( (my_x, my_y) pointregionquadtree(0, 0, 100, 100));\n" +
                        "on SupportSpatialPoint ssp merge MyPointTable where ssp.px = my_x and ssp.py = my_y when not matched then insert select px as my_x, py as my_y, id as my_id;\n" +
                        IndexBackingTableInfo.INDEX_CALLBACK_HOOK +
                        "@Audit @name('s0') on SupportSpatialAABB select my_id as c0 from MyPointTable as c0 where point(my_x, my_y).inside(rectangle(x, y, width, height))";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();

        EPStatement stmt = epService.getEPAdministrator().getStatement("s0");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIndex", "non-unique hash={} btree={} advanced={pointregionquadtree(my_x,my_y)}");

        sendPoint(epService, "P1", 55, 45);
        assertRectanglesManyRow(epService, listener, BOXES, null, "P1", null, null, "P1");

        sendPoint(epService, "P2", 45, 45);
        assertRectanglesManyRow(epService, listener, BOXES, "P2", "P1", null, null, "P1,P2");

        sendPoint(epService, "P3", 55, 55);
        assertRectanglesManyRow(epService, listener, BOXES, "P2", "P1", null, "P3", "P1,P2,P3");

        epService.getEPRuntime().executeQuery("delete from MyPointTable where my_x = 55 and my_y = 45");
        sendPoint(epService, "P4", 45, 55);
        assertRectanglesManyRow(epService, listener, BOXES, "P2", null, "P4", "P3", "P2,P3,P4");

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void assertIndexChoice(EPServiceProvider epService, String hint, String expectedIndexName) {
        String epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + hint +
                "on SupportSpatialAABB as aabb select mpw.id as c0 from MyPointWindow as mpw " +
                "where aabb.category = mpw.category and point(px, py).inside(rectangle(x, y, width, height))\n";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        QueryPlanIndexDescOnExpr plan = SupportQueryPlanIndexHook.assertOnExprAndReset();
        assertEquals(expectedIndexName, plan.getTables()[0].getIndexName());

        epService.getEPRuntime().sendEvent(new SupportSpatialAABB("R1", 9, 14, 1.0001, 1.0001, "Y"));
        assertEquals("P2", listener.assertOneGetNewAndReset().get("c0"));

        stmt.destroy();
    }

    public static class MyEventRectangleWithOffset {
        private final String id;
        private final Double xOffset;
        private final Double yOffset;
        private final Double x;
        private final Double y;
        private final Double width;
        private final Double height;

        public MyEventRectangleWithOffset(String id, Double xOffset, Double yOffset, Double x, Double y, Double width, Double height) {
            this.id = id;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public String getId() {
            return id;
        }

        public Double getxOffset() {
            return xOffset;
        }

        public Double getyOffset() {
            return yOffset;
        }

        public Double getX() {
            return x;
        }

        public Double getY() {
            return y;
        }

        public Double getWidth() {
            return width;
        }

        public Double getHeight() {
            return height;
        }
    }
}
