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
import com.espertech.esper.filter.FilterOperator;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.supportregression.bean.SupportSpatialAABB;
import com.espertech.esper.supportregression.bean.SupportSpatialDualPoint;
import com.espertech.esper.supportregression.bean.SupportSpatialPoint;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.supportregression.util.*;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class TestSpatialPointRegionQuadTree extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(TestSpatialPointRegionQuadTree.class);

    private final static List<BoundingBox> BOXES = Arrays.asList(
            new BoundingBox(0, 0, 50, 50),
            new BoundingBox(50, 0, 100, 50),
            new BoundingBox(0, 50, 50, 100),
            new BoundingBox(50, 50, 100, 100),
            new BoundingBox(25, 25, 75, 75)
    );

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getExecution().setAllowIsolatedService(true);
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        for (Class clazz : Arrays.asList(SupportSpatialPoint.class, SupportSpatialAABB.class, MyEventRectangleWithOffset.class, SupportSpatialDualPoint.class)) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.startTest(epService, this.getClass(), getName());
        }
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.endTest();
        }
        listener = null;
    }

    public void testFilterIndex() throws Exception {
        runAssertionFilterPerfStatement();
        runAssertionFilterPerfContextPartition();
        runAssertionFilterPerfPattern();
        runAssertionFilterUnoptimized();
        runAssertionFilterIndexTypeAssertion();
    }

    public void testInvalid() throws Exception {
        runAssertionInvalidEventIndexCreate();
        runAssertionInvalidEventIndexRuntime();
        runAssertionInvalidMethod();
        runAssertionInvalidFilterIndex();
    }

    public void testEventIndex() throws Exception {
        runAssertionUnindexed();

        runAssertionIndexUnusedTableFireAndForget();
        runAssertionIndexUnusedNamedWindowFireAndForget();
        runAssertionIndexUnusedOnTrigger();

        runAssertionIndexedOnTriggerNWInsertRemove(false);
        runAssertionIndexedOnTriggerNWInsertRemove(true);
        runAssertionIndexedOnTriggerContextParameterized();
        runAssertionIndexedSubqNamedWindowIndexShare();
        runAssertionIndexedOnTriggerTable();
        runAssertionIndexedChoiceOfTwo();
        runAssertionIndexedExpression();
        runAssertionIndexedUnique();
        runAssertionIndexedPerformance();
        runAssertionIndexedChoiceBetweenIndexTypes();
        runAssertionDocSample();
    }

    private void runAssertionFilterIndexTypeAssertion() {
        String eplNoIndex = "select * from SupportSpatialAABB(point(0, 0).inside(rectangle(x, y, width, height)))";
        SupportFilterHelper.assertFilterMulti(epService, eplNoIndex, "SupportSpatialAABB", new SupportFilterItem[][] {{SupportFilterItem.getBoolExprFilterItem()}});

        String eplIndexed = "expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "select * from SupportSpatialAABB(point(0, 0, filterindex:myindex).inside(rectangle(x, y, width, height)))";
        SupportFilterHelper.assertFilterMulti(epService, eplIndexed, "SupportSpatialAABB", new SupportFilterItem[][] {{new SupportFilterItem("x,y,width,height/myindex/0.0,0.0,100.0,100.0,4.0,20.0", FilterOperator.ADVANCED_INDEX)}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalidFilterIndex() {
        // unrecognized named parameter
        SupportMessageAssertUtil.tryInvalid(epService, "select * from SupportSpatialAABB#keepall where point(0, 0, a:1).inside(rectangle(x, y, width, height))",
                "Error validating expression: Failed to validate filter expression 'point(0,0,a:1).inside(rectangle(x,y...(50 chars)': point does not accept 'a' as a named parameter");

        // not a filter
        SupportMessageAssertUtil.tryInvalid(epService, "expression myindex {pointregionquadtree(0, 0, 100, 100)} select * from SupportSpatialAABB#keepall where point(0, 0, filterindex:myindex).inside(rectangle(x, y, width, height))",
                "Error validating expression: Failed to validate filter expression 'point(0,0,filterindex:myindex()).in...(68 chars)': The 'filterindex' named parameter can only be used in in filter expressions");

        // invalid index expression
        SupportMessageAssertUtil.tryInvalid(epService, "select * from SupportSpatialAABB(point(0, 0, filterindex:1).inside(rectangle(x, y, width, height)))",
                "Failed to validate filter expression 'point(0,0,filterindex:1).inside(rec...(60 chars)': Named parameter 'filterindex' requires an expression name");
        SupportMessageAssertUtil.tryInvalid(epService, "select * from SupportSpatialAABB(point(0, 0, filterindex:dummy).inside(rectangle(x, y, width, height)))",
                "Failed to validate filter expression 'point(0,0,filterindex:dummy).inside...(64 chars)': Named parameter 'filterindex' requires an expression name");
        SupportMessageAssertUtil.tryInvalid(epService, "expression myindex {0} select * from SupportSpatialAABB(point(0, 0, filterindex:myindex).inside(rectangle(x, y, width, height)))",
                "Failed to validate filter expression 'point(0,0,filterindex:myindex()).in...(68 chars)': Named parameter 'filterindex' requires an index expression");
        SupportMessageAssertUtil.tryInvalid(epService, "expression myindex {dummy(0)} select * from SupportSpatialAABB(point(0, 0, filterindex:myindex).inside(rectangle(x, y, width, height)))",
                "Failed to validate filter expression 'point(0,0,filterindex:myindex()).in...(68 chars)': Unrecognized advanced-type index 'dummy'");
        SupportMessageAssertUtil.tryInvalid(epService, "expression myindex {pointregionquadtree(0)} select * from SupportSpatialAABB(point(0, 0, filterindex:myindex).inside(rectangle(x, y, width, height)))",
                "Failed to validate filter expression 'point(0,0,filterindex:myindex()).in...(68 chars)': Index of type 'pointregionquadtree' requires at least 4 parameters but received 1 [");
        SupportMessageAssertUtil.tryInvalid(epService, "expression myindex {pointregionquadtree(0,0,0,0)} select * from SupportSpatialAABB(point(0, 0, filterindex:myindex).inside(rectangle(x, y, width, height)))",
                "Failed to validate filter expression 'point(0,0,filterindex:myindex()).in...(68 chars)': Invalid value for index 'myindex' parameter 'width' received 0.0 and expected value>0");
        SupportMessageAssertUtil.tryInvalid(epService, "expression myindex {pointregionquadtree(0,0,100,100).help()} select * from SupportSpatialAABB(point(0, 0, filterindex:myindex).inside(rectangle(x, y, width, height)))",
                "Failed to validate filter expression 'point(0,0,filterindex:myindex()).in...(68 chars)': Named parameter 'filterindex' invalid chained index expression");

        // filter-not-optimizable
        SupportMessageAssertUtil.tryInvalid(epService, "expression myindex {pointregionquadtree(0, 0, 100, 100)} select * from SupportSpatialAABB(point(x, y, filterindex:myindex).inside(rectangle(x, y, width, height)))",
                "Invalid filter-indexable expression 'x' in respect to index 'myindex': expected either a constant, context-builtin or property from a previous pattern match [expression myindex {pointregionquadtree(0, 0, 100, 100)} select * from SupportSpatialAABB(point(x, y, filterindex:myindex).inside(rectangle(x, y, width, height)))]");
        SupportMessageAssertUtil.tryInvalid(epService, "expression myindex {pointregionquadtree(0, 0, 100, 100)} select * from SupportSpatialAABB(point(0, 0, filterindex:myindex).inside(rectangle(0, y, width, height)))",
                "Invalid filter-index lookup expression '0' in respect to index 'myindex': expected an event property");
    }

    private void runAssertionDocSample() throws Exception {
        String epl =
                "create table PointTable(pointId string primary key, px double, py double);\n" +
                        "create index PointIndex on PointTable((px, py) pointregionquadtree(0, 0, 100, 100));\n" +
                        "create schema RectangleEvent(rx double, ry double, w double, h double);\n" +
                        "on RectangleEvent select pointId from PointTable where point(px, py).inside(rectangle(rx, ry, w, h));" +
                        "expression myQuadtreeSettings { pointregionquadtree(0, 0, 100, 100) } \n" +
                        "select * from SupportSpatialAABB(point(0, 0, filterindex:myQuadtreeSettings).inside(rectangle(x, y, width, height)));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionIndexedChoiceBetweenIndexTypes() throws Exception {
        String epl = "@Name('win') create window MyPointWindow#keepall as (id string, category string, px double, py double);\n" +
                "@Name('insert') insert into MyPointWindow select id, category, px, py from SupportSpatialPoint;\n" +
                "@Name('idx1') create index IdxHash on MyPointWindow(category);\n" +
                "@Name('idx2') create index IdxQuadtree on MyPointWindow((px, py) pointregionquadtree(0, 0, 100, 100));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();

        sendPoint("P1", 10, 15, "X");
        sendPoint("P2", 10, 15, "Y");
        sendPoint("P3", 10, 15, "Z");

        assertIndexChoice("", "IdxQuadtree");
        assertIndexChoice("@Hint('index(IdxHash, bust)')", "IdxQuadtree");

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionIndexedUnique() throws Exception {
        String epl = "@Name('win') create window MyPointWindow#keepall as (id string, px double, py double);\n" +
                "@Name('insert') insert into MyPointWindow select id, px, py from SupportSpatialPoint;\n" +
                "@Name('idx') create unique index Idx on MyPointWindow( (px, py) pointregionquadtree(0, 0, 100, 100));\n" +
                "@Name('out') on SupportSpatialAABB select mpw.id as c0 from MyPointWindow as mpw where point(px, py).inside(rectangle(x, y, width, height));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        sendPoint("P1", 10, 15);
        try {
            sendPoint("P2", 10, 15);
            fail();
        } catch (RuntimeException ex) { // we have a handler
            SupportMessageAssertUtil.assertMessage(ex,
                    "Unexpected exception in statement 'win': Unique index violation, index 'Idx' is a unique index and key '(10.0,15.0)' already exists");
        }

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionIndexedPerformance() throws Exception {
        String epl = "create window MyPointWindow#keepall as (id string, px double, py double);\n" +
                "insert into MyPointWindow select id, px, py from SupportSpatialPoint;\n" +
                "create index Idx on MyPointWindow( (px, py) pointregionquadtree(0, 0, 100, 100));\n" +
                "@Name('out') on SupportSpatialAABB select mpw.id as c0 from MyPointWindow as mpw where point(px, py).inside(rectangle(x, y, width, height));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();
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

    private void runAssertionIndexUnusedNamedWindowFireAndForget() throws Exception {
        String epl = "@Resilient create window PointWindow#keepall as (id string, px double, py double);\n" +
                "@Resilient create index MyIndex on PointWindow((px,py) pointregionquadtree(0,0,100,100,2,12));\n" +
                "@Resilient insert into PointWindow select id, px, py from SupportSpatialPoint;\n" +
                "@Resilient @name('out') on SupportSpatialAABB as aabb select pt.id as c0 from PointWindow as pt where point(px,py).inside(rectangle(x,y,width,height));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();

        epService.getEPRuntime().executeQuery("delete from PointWindow where id='P1'");

        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(deploymentId);
    }

    private void runAssertionIndexUnusedTableFireAndForget() {
        epService.getEPAdministrator().createEPL("create table MyTable(id string primary key, tx double, ty double)");
        epService.getEPRuntime().executeQuery("insert into MyTable values ('P1', 50, 50)");
        epService.getEPRuntime().executeQuery("insert into MyTable values ('P2', 49, 49)");
        epService.getEPAdministrator().createEPL("create index MyIdxWithExpr on MyTable( (tx, ty) pointregionquadtree(0, 0, 100, 100))");

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select id as c0 from MyTable where point(tx, ty).inside(rectangle(45, 45, 10, 10))");
        SupportQueryPlanIndexHook.assertFAFAndReset(null, null);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), "c0".split(","), new Object[][]{{"P1"}, {"P2"}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionIndexedExpression() {
        epService.getEPAdministrator().createEPL("create table MyTable(id string primary key, tx double, ty double)");
        epService.getEPRuntime().executeQuery("insert into MyTable values ('P1', 50, 30)");
        epService.getEPRuntime().executeQuery("insert into MyTable values ('P2', 50, 28)");
        epService.getEPRuntime().executeQuery("insert into MyTable values ('P3', 50, 30)");
        epService.getEPRuntime().executeQuery("insert into MyTable values ('P4', 49, 29)");
        epService.getEPAdministrator().createEPL("create index MyIdxWithExpr on MyTable( (tx*10, ty*10) pointregionquadtree(0, 0, 1000, 1000))");

        String eplOne = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportSpatialAABB select tbl.id as c0 from MyTable as tbl where point(tx, ty).inside(rectangle(x, y, width, height))";
        EPStatement statementOne = epService.getEPAdministrator().createEPL(eplOne);
        statementOne.addListener(listener);
        SupportQueryPlanIndexHook.assertOnExprTableAndReset(null, null);
        assertRectanglesManyRow(BOXES, "P4", "P1,P2,P3", null, null, "P1,P2,P3,P4");
        statementOne.destroy();

        String eplTwo = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportSpatialAABB select tbl.id as c0 from MyTable as tbl where point(tx*10, tbl.ty*10).inside(rectangle(x, y, width, height))";
        EPStatement statementTwo = epService.getEPAdministrator().createEPL(eplTwo);
        statementTwo.addListener(listener);
        SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIdxWithExpr", "non-unique hash={} btree={} advanced={pointregionquadtree(tx*10,ty*10)}");
        assertRectanglesManyRow(BOXES, null, null, null, null, null);
        assertRectanglesManyRow(Collections.singletonList(new BoundingBox(500, 300, 501, 301)), "P1,P3");
        statementTwo.destroy();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionIndexedChoiceOfTwo() throws Exception {
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
        statementOne.addListener(listener);
        SupportQueryPlanIndexHook.assertOnExprTableAndReset("Idx1", "non-unique hash={} btree={} advanced={pointregionquadtree(x1,y1)}");

        String textTwo = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportSpatialAABB select tbl.id as c0 from MyPointTable as tbl where point(tbl.x2, y2).inside(rectangle(x, y, width, height))";
        EPStatement statementTwo = epService.getEPAdministrator().createEPL(textTwo);
        statementTwo.addListener(listener);
        SupportQueryPlanIndexHook.assertOnExprTableAndReset("Idx2", "non-unique hash={} btree={} advanced={pointregionquadtree(x2,y2)}");

        epService.getEPRuntime().sendEvent(new SupportSpatialDualPoint("P1", 10, 10, 60, 60));
        epService.getEPRuntime().sendEvent(new SupportSpatialDualPoint("P2", 55, 20, 4, 88));

        assertRectanglesSingleValue(BOXES, "P1", "P2", "P2", "P1", "P1");

        statementOne.destroy();
        statementTwo.destroy();
        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionIndexedSubqNamedWindowIndexShare() throws Exception {
        String epl = "@Hint('enable_window_subquery_indexshare') create window MyWindow#length(5) as select * from SupportSpatialPoint;\n" +
                "create index MyIndex on MyWindow((px,py) pointregionquadtree(0,0,100,100));\n" +
                "insert into MyWindow select * from SupportSpatialPoint;\n" +
                IndexBackingTableInfo.INDEX_CALLBACK_HOOK +
                "@name('out') select (select id from MyWindow as mw where point(mw.px,mw.py).inside(rectangle(aabb.x,aabb.y,aabb.width,aabb.height))).aggregate('', \n" +
                "  (result, item) => result || (case when result='' then '' else ',' end) || item) as c0 from SupportSpatialAABB aabb";
        DeploymentResult deploymentResult = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        QueryPlanIndexDescSubquery subquery = SupportQueryPlanIndexHook.assertSubqueryAndReset();
        assertEquals("non-unique hash={} btree={} advanced={pointregionquadtree(px,py)}", subquery.getTables()[0].getIndexDesc());
        assertEquals("MyIndex", subquery.getTables()[0].getIndexName());

        sendPoint("P1", 10, 40);
        assertRectanglesSingleValue(BOXES, "P1", "", "", "", "");

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentResult.getDeploymentId());
    }

    private void runAssertionIndexUnusedOnTrigger() throws Exception {
        String epl = "create window MyWindow#length(5) as select * from SupportSpatialPoint;\n" +
                "create index MyIndex on MyWindow((px,py) pointregionquadtree(0,0,100,100));\n" +
                "insert into MyWindow select * from SupportSpatialPoint;\n";
        DeploymentResult deploymentResult = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        sendPoint("P1", 5, 5);
        sendPoint("P2", 55, 60);

        runIndexUnusedConstantsOnly();
        runIndexUnusedPointValueDepends();
        runIndexUnusedRectValueDepends();

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentResult.getDeploymentId());
    }

    private void runIndexUnusedRectValueDepends() {
        String epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@name('out') on SupportSpatialAABB as aabb select points.id as c0 " +
                "from MyWindow as points where point(px, py).inside(rectangle(px,py,1,1))";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset(null, null);

        assertRectanglesManyRow(BOXES, "P1,P2", "P1,P2", "P1,P2", "P1,P2", "P1,P2");

        stmt.destroy();
    }

    private void runIndexUnusedPointValueDepends() {
        String epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@name('out') on SupportSpatialAABB as aabb select points.id as c0 " +
                "from MyWindow as points where point(px + x, py + y).inside(rectangle(x,y,width,height))";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset(null, null);

        assertRectanglesManyRow(BOXES, "P1", "P1", "P1", "P1", "P1");

        stmt.destroy();
    }

    private void runIndexUnusedConstantsOnly() {
        String epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@name('out') on SupportSpatialAABB as aabb select points.id as c0 " +
                "from MyWindow as points where point(0, 0).inside(rectangle(x,y,width,height))";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset(null, null);

        assertRectanglesManyRow(BOXES, "P1,P2", null, null, null, null);

        stmt.destroy();
    }

    private void runAssertionInvalidMethod() {
        SupportMessageAssertUtil.tryInvalid(epService, "select * from MyEventRectangleWithOffset(point('a', 0).inside(rectangle(0, 0, 0, 0)))",
                "Failed to validate filter expression 'point(\"a\",0).inside(rectangle(0,0,0,0))': Error validating left-hand-side function 'point', expected a number-type result for expression parameter 0 but received java.lang.String");
        SupportMessageAssertUtil.tryInvalid(epService, "select * from MyEventRectangleWithOffset(point(0).inside(rectangle(0, 0, 0, 0)))",
                "Failed to validate filter expression 'point(0).inside(rectangle(0,0,0,0))': Error validating left-hand-side method 'point', expected 2 parameters but received 1 parameters");
        SupportMessageAssertUtil.tryInvalid(epService, "select * from MyEventRectangleWithOffset(point(0,0).inside(rectangle('a', 0, 0, 0)))",
                "Failed to validate filter expression 'point(0,0).inside(rectangle(\"a\",0,0,0))': Error validating right-hand-side function 'rectangle', expected a number-type result for expression parameter 0 but received java.lang.String");
        SupportMessageAssertUtil.tryInvalid(epService, "select * from MyEventRectangleWithOffset(point(0,0).inside(rectangle(0)))",
                "Failed to validate filter expression 'point(0,0).inside(rectangle(0))': Error validating right-hand-side function 'rectangle', expected 4 parameters but received 1 parameters");
        SupportMessageAssertUtil.tryInvalid(epService, "select * from MyEventRectangleWithOffset(point(0,0).inside(0))",
                "Failed to validate filter expression 'point(0,0).inside(0)': point.inside requires a single rectangle as parameter");
    }

    private void runAssertionUnindexed() {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select point(xOffset, yOffset).inside(rectangle(x, y, width, height)) as c0 from MyEventRectangleWithOffset");
        stmt.addListener(listener);

        sendAssert(1, 1, 0, 0, 2, 2, true);
        sendAssert(3, 1, 0, 0, 2, 2, false);
        sendAssert(2, 1, 0, 0, 2, 2, false);
        sendAssert(1, 3, 0, 0, 2, 2, false);
        sendAssert(1, 2, 0, 0, 2, 2, false);
        sendAssert(0, 0, 1, 1, 2, 2, false);
        sendAssert(1, 0, 1, 1, 2, 2, false);
        sendAssert(0, 1, 1, 1, 2, 2, false);
        sendAssert(1, 1, 1, 1, 2, 2, true);
        sendAssert(2.9999, 2.9999, 1, 1, 2, 2, true);
        sendAssert(3, 2.9999, 1, 1, 2, 2, false);
        sendAssert(2.9999, 3, 1, 1, 2, 2, false);
        sendAssertWNull(null, 0d, 0d, 0d, 0d, 0d, null);
        sendAssertWNull(0d, 0d, 0d, null, 0d, 0d, null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionIndexedOnTriggerContextParameterized() throws Exception {
        String epl = "create context CtxBox initiated by MyEventRectangleWithOffset box;\n" +
                "context CtxBox create window MyWindow#keepall as SupportSpatialPoint;\n" +
                "context CtxBox create index MyIndex on MyWindow((px+context.box.xOffset, py+context.box.yOffset) pointregionquadtree(context.box.x, context.box.y, context.box.width, context.box.height));\n" +
                "context CtxBox on SupportSpatialPoint(category = context.box.id) merge MyWindow when not matched then insert select *;\n" +
                "@name('out') context CtxBox on SupportSpatialAABB(category = context.box.id) aabb " +
                "  select points.id as c0 from MyWindow points where point(px, py).inside(rectangle(x, y, width, height))";
        DeploymentResult deploymentResult = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        epService.getEPRuntime().sendEvent(new MyEventRectangleWithOffset("NW", 0d, 0d, 0d, 0d, 50d, 50d));
        epService.getEPRuntime().sendEvent(new MyEventRectangleWithOffset("SE", 0d, 0d, 50d, 50d, 50d, 50d));
        sendPoint("P1", 60, 90, "SE");
        sendPoint("P2", 5, 20, "NW");

        epService.getEPRuntime().sendEvent(new SupportSpatialAABB("R1", 60, 60, 40, 40, "SE"));
        assertEquals("P1", listener.assertOneGetNewAndReset().get("c0"));

        epService.getEPRuntime().sendEvent(new SupportSpatialAABB("R2", 0, 0, 5.0001, 20.0001, "NW"));
        assertEquals("P2", listener.assertOneGetNewAndReset().get("c0"));

        epService.getEPRuntime().sendEvent(new SupportSpatialAABB("R3", 0, 0, 5, 30, "NW"));
        epService.getEPRuntime().sendEvent(new SupportSpatialAABB("R3", 0, 0, 30, 20, "NW"));
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentResult.getDeploymentId());
    }

    private void runAssertionIndexedOnTriggerNWInsertRemove(boolean soda) {
        SupportModelHelper.createByCompileOrParse(epService, soda, "create window MyWindow#length(5) as select * from SupportSpatialPoint");
        SupportModelHelper.createByCompileOrParse(epService, soda, "create index MyIndex on MyWindow((px,py) pointregionquadtree(0,0,100,100))");
        SupportModelHelper.createByCompileOrParse(epService, soda, "insert into MyWindow select * from SupportSpatialPoint");

        String epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + " on SupportSpatialAABB as aabb " +
                "select points.id as c0 from MyWindow as points where point(px,py).inside(rectangle(x,y,width,height))";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        stmt.addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIndex", "non-unique hash={} btree={} advanced={pointregionquadtree(px,py)}");

        sendPoint("P1", 10, 40);
        assertRectanglesManyRow(BOXES, "P1", null, null, null, null);

        sendPoint("P2", 80, 80);
        assertRectanglesManyRow(BOXES, "P1", null, null, "P2", null);

        sendPoint("P3", 10, 40);
        assertRectanglesManyRow(BOXES, "P1,P3", null, null, "P2", null);

        sendPoint("P4", 60, 40);
        assertRectanglesManyRow(BOXES, "P1,P3", "P4", null, "P2", "P4");

        sendPoint("P5", 20, 75);
        assertRectanglesManyRow(BOXES, "P1,P3", "P4", "P5", "P2", "P4");

        sendPoint("P6", 50, 50);
        assertRectanglesManyRow(BOXES, "P3", "P4", "P5", "P2,P6", "P4,P6");

        sendPoint("P7", 0, 0);
        assertRectanglesManyRow(BOXES, "P3,P7", "P4", "P5", "P6", "P4,P6");

        sendPoint("P8", 99.999, 0);
        assertRectanglesManyRow(BOXES, "P7", "P4,P8", "P5", "P6", "P4,P6");

        sendPoint("P9", 0, 99.999);
        assertRectanglesManyRow(BOXES, "P7", "P8", "P5,P9", "P6", "P6");

        sendPoint("P10", 99.999, 99.999);
        assertRectanglesManyRow(BOXES, "P7", "P8", "P9", "P6,P10", "P6");

        sendPoint("P11", 0, 0);
        assertRectanglesManyRow(BOXES, "P7,P11", "P8", "P9", "P10", null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionIndexedOnTriggerTable() throws Exception {
        String epl =
                "create table MyPointTable(my_x double primary key, my_y double primary key, my_id string);\n" +
                        "@Audit create index MyIndex on MyPointTable( (my_x, my_y) pointregionquadtree(0, 0, 100, 100));\n" +
                        "on SupportSpatialPoint ssp merge MyPointTable where ssp.px = my_x and ssp.py = my_y when not matched then insert select px as my_x, py as my_y, id as my_id;\n" +
                        IndexBackingTableInfo.INDEX_CALLBACK_HOOK +
                        "@Audit @name('s0') on SupportSpatialAABB select my_id as c0 from MyPointTable as c0 where point(my_x, my_y).inside(rectangle(x, y, width, height))";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();

        EPStatement stmt = epService.getEPAdministrator().getStatement("s0");
        stmt.addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIndex", "non-unique hash={} btree={} advanced={pointregionquadtree(my_x,my_y)}");

        sendPoint("P1", 55, 45);
        assertRectanglesManyRow(BOXES, null, "P1", null, null, "P1");

        sendPoint("P2", 45, 45);
        assertRectanglesManyRow(BOXES, "P2", "P1", null, null, "P1,P2");

        sendPoint("P3", 55, 55);
        assertRectanglesManyRow(BOXES, "P2", "P1", null, "P3", "P1,P2,P3");

        epService.getEPRuntime().executeQuery("delete from MyPointTable where my_x = 55 and my_y = 45");
        sendPoint("P4", 45, 55);
        assertRectanglesManyRow(BOXES, "P2", null, "P4", "P3", "P2,P3,P4");

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void assertRectanglesSingleValue(List<BoundingBox> rectangles, String... matches) {
        for (int i = 0; i < rectangles.size(); i++) {
            BoundingBox box = rectangles.get(i);
            sendRectangle("R" + box.toString(), box.getMinX(), box.getMinY(), box.getMaxX() - box.getMinX(), box.getMaxY() - box.getMinY());
            String c0 = listener.assertOneGetNewAndReset().get("c0").toString();
            assertEquals("for box " + i, matches[i], c0);
        }
    }

    private void assertRectanglesManyRow(List<BoundingBox> rectangles, String... matches) {
        for (int i = 0; i < rectangles.size(); i++) {
            BoundingBox box = rectangles.get(i);
            sendRectangle("R" + box.toString(), box.getMinX(), box.getMinY(), box.getMaxX() - box.getMinX(), box.getMaxY() - box.getMinY());
            if (matches[i] == null) {
                if (listener.isInvoked()) {
                    fail("Unexpected output for box " + i + ": " + joinProperty(listener.getAndResetLastNewData(), "c0"));
                }
            } else {
                if (!listener.isInvoked()) {
                    fail("No output for box " + i);
                }
                assertEquals(matches[i], joinProperty(listener.getAndResetLastNewData(), "c0"));
            }
        }
    }

    private void runAssertionInvalidEventIndexRuntime() throws Exception {
        String epl = "@name('mywindow') create window PointWindow#keepall as SupportSpatialPoint;\n" +
                "insert into PointWindow select * from SupportSpatialPoint;\n" +
                "create index MyIndex on PointWindow((px, py) pointregionquadtree(0, 0, 100, 100));\n";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        try {
            epService.getEPRuntime().sendEvent(new SupportSpatialPoint("E1", null, null));
        } catch (Exception ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Unexpected exception in statement 'mywindow': Invalid value for index 'MyIndex' column 'x' received null and expected non-null");
        }

        try {
            epService.getEPRuntime().sendEvent(new SupportSpatialPoint("E1", 200d, 200d));
        } catch (Exception ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Unexpected exception in statement 'mywindow': Invalid value for index 'MyIndex' column '(x,y)' received (200.0,200.0) and expected a value within index bounding box (range-end-non-inclusive) {minX=0.0, minY=0.0, maxX=100.0, maxY=100.0}");
        }
    }

    private void runAssertionInvalidEventIndexCreate() {
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportSpatialPoint");

        // invalid number of columns
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow(px pointregionquadtree(0, 0, 100, 100))",
                "Error starting statement: Index of type 'pointregionquadtree' requires 2 expressions as index columns but received 1");

        // invalid column type
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((id, py) pointregionquadtree(0, 0, 100, 100))",
                "Error starting statement: Index of type 'pointregionquadtree' for column 0 that is providing x-values expecting type java.lang.Number but received type java.lang.String");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((px, id) pointregionquadtree(0, 0, 100, 100))",
                "Error starting statement: Index of type 'pointregionquadtree' for column 1 that is providing y-values expecting type java.lang.Number but received type java.lang.String");

        // invalid expressions for column or parameter
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((dummy, dummy2) pointregionquadtree(0, 0, 100, 100))",
                "Error starting statement: Failed to validate create-index index-column expression 'dummy': Property named 'dummy' is not valid in any stream");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((px, py) pointregionquadtree(dummy, 0, 100, 100))",
                "Error starting statement: Failed to validate create-index index-parameter expression 'dummy': Property named 'dummy' is not valid in any stream");

        // invalid property use in parameter
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((px, py) pointregionquadtree(px, 0, 100, 100))",
                "Error starting statement: Index parameters may not refer to event properties");

        // invalid number of parameters
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((px, py) pointregionquadtree)",
                "Error starting statement: Index of type 'pointregionquadtree' requires at least 4 parameters but received 0");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((px, py) pointregionquadtree('a'))",
                "Error starting statement: Index of type 'pointregionquadtree' requires at least 4 parameters but received 1");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((px, py) pointregionquadtree(0, 0, 0, 0, 0, 0, 0))",
                "Error starting statement: Index of type 'pointregionquadtree' requires at least 4 parameters but received 7");

        // invalid parameter type
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((px, py) pointregionquadtree('a', 0, 100, 100))",
                "Error starting statement: Index of type 'pointregionquadtree' for parameter 0 that is providing xMin-values expecting type java.lang.Number but received type java.lang.String");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((px, py) pointregionquadtree(0, 'a', 100, 100))",
                "Error starting statement: Index of type 'pointregionquadtree' for parameter 1 that is providing yMin-values expecting type java.lang.Number but received type java.lang.String");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((px, py) pointregionquadtree(0, 0, 'a', 100))",
                "Error starting statement: Index of type 'pointregionquadtree' for parameter 2 that is providing width-values expecting type java.lang.Number but received type java.lang.String");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((px, py) pointregionquadtree(0, 0, 100, 'a'))",
                "Error starting statement: Index of type 'pointregionquadtree' for parameter 3 that is providing height-values expecting type java.lang.Number but received type java.lang.String");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((px, py) pointregionquadtree(0, 0, 100, 100, 'a'))",
                "Error starting statement: Index of type 'pointregionquadtree' for parameter 4 that is providing leafCapacity-values expecting type java.lang.Integer but received type java.lang.String");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((px, py) pointregionquadtree(0, 0, 100, 100, 1, 'a'))",
                "Error starting statement: Index of type 'pointregionquadtree' for parameter 5 that is providing maxTreeHeight-values expecting type java.lang.Integer but received type java.lang.String");

        // invalid parameter value
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((px, py) pointregionquadtree(cast(null, double), 0, 0, 0))",
                "Unexpected exception starting statement: Invalid value for index 'MyIndex' parameter 'xMin' received null and expected non-null");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((py, px) pointregionquadtree(0, 0, -100, 0))",
                "Unexpected exception starting statement: Invalid value for index 'MyIndex' parameter 'width' received -100.0 and expected value>0");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((py, px) pointregionquadtree(0, 0, 1, -200))",
                "Unexpected exception starting statement: Invalid value for index 'MyIndex' parameter 'height' received -200.0 and expected value>0");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((py, px) pointregionquadtree(0, 0, 1, 1, -1))",
                "Unexpected exception starting statement: Invalid value for index 'MyIndex' parameter 'leafCapacity' received -1 and expected value>=1");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((py, px) pointregionquadtree(0, 0, 1, 1, 10, -1))",
                "Unexpected exception starting statement: Invalid value for index 'MyIndex' parameter 'maxTreeHeight' received -1 and expected value>=2");

        // same index twice, by-name and by-columns
        epService.getEPAdministrator().createEPL("create window SomeWindow#keepall as SupportSpatialPoint");
        epService.getEPAdministrator().createEPL("create index SomeWindowIdx1 on SomeWindow((px, py) pointregionquadtree(0, 0, 1, 1))");
        SupportMessageAssertUtil.tryInvalid(epService, "create index SomeWindowIdx2 on SomeWindow((px, py) pointregionquadtree(0, 0, 1, 1))",
                "Error starting statement: An index for the same columns already exists");
        SupportMessageAssertUtil.tryInvalid(epService, "create index SomeWindowIdx1 on SomeWindow((py, px) pointregionquadtree(0, 0, 1, 1))",
                "Error starting statement: An index by name 'SomeWindowIdx1' already exists");

        // non-plain column or parameter expressions
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndexInv on MyWindow((sum(px), py) pointregionquadtree(0, 0, 1, 1))",
                "Error starting statement: Invalid create-index index-column expression 'sum(px)': Aggregation, sub-select, previous or prior functions are not supported in this context");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndexInv on MyWindow((px, py) pointregionquadtree(count(*), 0, 1, 1))",
                "Error starting statement: Invalid create-index index-parameter expression 'count(*)': Aggregation, sub-select, previous or prior functions are not supported in this context");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterUnoptimized() {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportSpatialAABB(point(5, 10).inside(rectangle(x, y, width, height)))");
        stmt.addListener(listener);

        sendRectangle("R1", 0, 0, 5, 10);
        sendRectangle("R2", 4, 3, 2, 20);
        assertEquals("R2", listener.assertOneGetNewAndReset().get("id"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterPerfStatement() {
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL("expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "select * from SupportSpatialAABB(point(?, ?, filterindex:myindex).inside(rectangle(x, y, width, height)))");

        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 20; y++) {
                prepared.setObject(1, x);
                prepared.setObject(2, y);
                epService.getEPAdministrator().create(prepared).addListener(listener);
            }
        }
        sendAssertSpatialAABB(100, 20, 1000);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterPerfPattern() {
        EPStatement stmt = epService.getEPAdministrator().createEPL("expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "select * from pattern [every p=SupportSpatialPoint -> SupportSpatialAABB(point(p.px, p.py, filterindex:myindex).inside(rectangle(x, y, width, height)))]");
        stmt.addListener(listener);

        sendSpatialPoints(100, 100);
        sendAssertSpatialAABB(100, 100, 1000);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterPerfContextPartition() {

        epService.getEPAdministrator().createEPL("create context PerPointCtx initiated by SupportSpatialPoint ssp");
        EPStatement stmt = epService.getEPAdministrator().createEPL("expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "context PerPointCtx select count(*) from SupportSpatialAABB(point(context.ssp.px, context.ssp.py, filterindex:myindex).inside(rectangle(x, y, width, height)))");
        stmt.addListener(listener);

        sendSpatialPoints(100, 100);
        sendAssertSpatialAABB(100, 100, 1000);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendPoint(String id, double x, double y) {
        epService.getEPRuntime().sendEvent(new SupportSpatialPoint(id, x, y));
    }

    private void sendPoint(String id, double x, double y, String category) {
        epService.getEPRuntime().sendEvent(new SupportSpatialPoint(id, x, y, category));
    }

    private void sendRectangle(String id, double x, double y, double width, double height) {
        epService.getEPRuntime().sendEvent(new SupportSpatialAABB(id, x, y, width, height));
    }

    private void sendAssert(double px, double py, double x, double y, double width, double height, boolean expected) {
        sendAssertWNull(px, py, x, y, width, height, expected);
    }

    private void sendAssertWNull(Double px, Double py, Double x, Double y, Double width, Double height, Boolean expected) {
        epService.getEPRuntime().sendEvent(new MyEventRectangleWithOffset("E", px, py, x, y, width, height));
        assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));
    }

    private void assertIndexChoice(String hint, String expectedIndexName) {
        String epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + hint +
                "on SupportSpatialAABB as aabb select mpw.id as c0 from MyPointWindow as mpw " +
                "where aabb.category = mpw.category and point(px, py).inside(rectangle(x, y, width, height))\n";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        QueryPlanIndexDescOnExpr plan = SupportQueryPlanIndexHook.assertOnExprAndReset();
        assertEquals(expectedIndexName, plan.getTables()[0].getIndexName());

        epService.getEPRuntime().sendEvent(new SupportSpatialAABB("R1", 9, 14, 1.0001, 1.0001, "Y"));
        assertEquals("P2", listener.assertOneGetNewAndReset().get("c0"));

        stmt.destroy();
    }

    private String joinProperty(EventBean[] events, String propertyName) {
        StringJoiner joiner = new StringJoiner(",");
        for (EventBean event : events) {
            joiner.add(event.get(propertyName).toString());
        }
        return joiner.toString();
    }

    private void sendSpatialPoints(int numX, int numY) {
        for (int x = 0; x < numX; x++) {
            for (int y = 0; y < numY; y++) {
                epService.getEPRuntime().sendEvent(new SupportSpatialPoint("P_" + x + "_" + y, (double) x, (double) y));
            }
        }
    }

    private void sendAssertSpatialAABB(int numX, int numY, long deltaMSec) {
        long start = System.currentTimeMillis();
        for (int x = 0; x < numX; x++) {
            for (int y = 0; y < numY; y++) {
                epService.getEPRuntime().sendEvent(new SupportSpatialAABB("", x, y, 1, 1));
                listener.assertOneGetNewAndReset();
            }
        }
        long delta = System.currentTimeMillis() - start;
        assertTrue("Delta: " + delta, delta < deltaMSec);
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
