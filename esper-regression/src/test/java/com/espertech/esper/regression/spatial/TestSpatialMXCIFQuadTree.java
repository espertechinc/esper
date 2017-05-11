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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.filter.FilterOperator;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.supportregression.bean.SupportSpatialAABB;
import com.espertech.esper.supportregression.bean.SupportSpatialDualAABB;
import com.espertech.esper.supportregression.bean.SupportSpatialEventRectangle;
import com.espertech.esper.supportregression.bean.SupportSpatialPoint;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.supportregression.util.*;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static com.espertech.esper.supportregression.util.SupportSpatialUtil.assertRectanglesManyRow;
import static com.espertech.esper.supportregression.util.SupportSpatialUtil.sendAssertSpatialAABB;

public class TestSpatialMXCIFQuadTree extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(TestSpatialMXCIFQuadTree.class);

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
        for (Class clazz : Arrays.asList(SupportSpatialAABB.class, SupportSpatialEventRectangle.class, SupportSpatialDualAABB.class)) {
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

    public void testInvalid() throws Exception {
        // invalid-testing overlaps with pointregion-quadtree
        runAssertionInvalidEventIndexCreate();
        runAssertionInvalidEventIndexRuntime();
        runAssertionInvalidMethod();
        runAssertionInvalidFilterIndex();

        runAssertionDocSample();
    }

    private void runAssertionInvalidFilterIndex() {
        // invalid index for filter
        String epl = "expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "select * from SupportSpatialEventRectangle(rectangle(10, 20, 5, 6, filterindex:myindex).intersects(rectangle(x, y, width, height)))";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Failed to validate filter expression 'rectangle(10,20,5,6,filterindex:myi...(82 chars)': Invalid index type 'pointregionquadtree', expected 'mxcifquadtree'");
    }

    private void runAssertionInvalidMethod() {
        SupportMessageAssertUtil.tryInvalid(epService, "select * from SupportSpatialEventRectangle(rectangle('a', 0).inside(rectangle(0, 0, 0, 0)))",
                "Failed to validate filter expression 'rectangle(\"a\",0).inside(rectangle(0...(43 chars)': Failed to validate method-chain parameter expression 'rectangle(0,0,0,0)': Unknown single-row function, expression declaration, script or aggregation function named 'rectangle' could not be resolved (did you mean 'rectangle.intersects')");
        SupportMessageAssertUtil.tryInvalid(epService, "select * from SupportSpatialEventRectangle(rectangle(0).intersects(rectangle(0, 0, 0, 0)))",
                "Failed to validate filter expression 'rectangle(0).intersects(rectangle(0...(43 chars)': Error validating left-hand-side method 'rectangle', expected 4 parameters but received 1 parameters");
    }

    private void runAssertionInvalidEventIndexRuntime() throws Exception {
        String epl = "@name('mywindow') create window RectangleWindow#keepall as SupportSpatialEventRectangle;\n" +
                "insert into RectangleWindow select * from SupportSpatialEventRectangle;\n" +
                "create index MyIndex on RectangleWindow((x, y, width, height) mxcifquadtree(0, 0, 100, 100));\n";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        try {
            epService.getEPRuntime().sendEvent(new SupportSpatialEventRectangle("E1", null, null, null, null, "category"));
        } catch (Exception ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Unexpected exception in statement 'mywindow': Invalid value for index 'MyIndex' column 'x' received null and expected non-null");
        }

        try {
            epService.getEPRuntime().sendEvent(new SupportSpatialEventRectangle("E1", 200d, 200d, 1, 1));
        } catch (Exception ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Unexpected exception in statement 'mywindow': Invalid value for index 'MyIndex' column '(x,y,width,height)' received (200.0,200.0,1.0,1.0) and expected a value intersecting index bounding box (range-end-inclusive) {minX=0.0, minY=0.0, maxX=100.0, maxY=100.0}");
        }
    }

    private void runAssertionInvalidEventIndexCreate() {
        // most are covered by point-region test already
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportSpatialEventRectangle");

        // invalid number of columns
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow(x mxcifquadtree(0, 0, 100, 100))",
                "Error starting statement: Index of type 'mxcifquadtree' requires 4 expressions as index columns but received 1");

        // same index twice, by-columns
        epService.getEPAdministrator().createEPL("create window SomeWindow#keepall as SupportSpatialEventRectangle");
        epService.getEPAdministrator().createEPL("create index SomeWindowIdx1 on SomeWindow((x, y, width, height) mxcifquadtree(0, 0, 1, 1))");
        SupportMessageAssertUtil.tryInvalid(epService, "create index SomeWindowIdx2 on SomeWindow((x, y, width, height) mxcifquadtree(0, 0, 1, 1))",
                "Error starting statement: An index for the same columns already exists");

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testFilterIndex() throws Exception {
        runAssertionFilterIndexPerfPattern();
        runAssertionFilterIndexTypeAssertion();
    }

    public void testEventIndex() throws Exception {
        runAssertionEventIndexUnindexed();

        runAssertionEventIndexOnTriggerNWInsertRemove(false);
        runAssertionEventIndexOnTriggerNWInsertRemove(true);
        runAssertionEventIndexUnique();
        runAssertionEventIndexPerformance();
        runAssertionEventIndexTableFireAndForget();
    }

    private void runAssertionFilterIndexTypeAssertion() {
        String eplNoIndex = "select * from SupportSpatialEventRectangle(rectangle(0, 0, 1, 1).intersects(rectangle(x, y, width, height)))";
        SupportFilterHelper.assertFilterMulti(epService, eplNoIndex, "SupportSpatialEventRectangle", new SupportFilterItem[][] {{SupportFilterItem.getBoolExprFilterItem()}});

        String eplIndexed = "expression myindex {mxcifquadtree(0, 0, 100, 100)}" +
                "select * from SupportSpatialEventRectangle(rectangle(10, 20, 5, 6, filterindex:myindex).intersects(rectangle(x, y, width, height)))";
        EPStatement statement = SupportFilterHelper.assertFilterMulti(epService, eplIndexed, "SupportSpatialEventRectangle", new SupportFilterItem[][] {{new SupportFilterItem("x,y,width,height/myindex/mxcifquadtree/0.0,0.0,100.0,100.0,4.0,20.0", FilterOperator.ADVANCED_INDEX)}});
        statement.addListener(listener);

        sendAssertEventRectangle(10, 20, 0, 0, true);
        sendAssertEventRectangle(9, 19, 0.9999, 0.9999, false);
        sendAssertEventRectangle(9, 19, 1, 1, true);
        sendAssertEventRectangle(15, 26, 0, 0, true);
        sendAssertEventRectangle(15.001, 26.001, 0, 0, false);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterIndexPerfPattern() {
        EPStatement stmt = epService.getEPAdministrator().createEPL("expression myindex {mxcifquadtree(0, 0, 100, 100)}" +
                "select * from pattern [every p=SupportSpatialEventRectangle -> SupportSpatialAABB(rectangle(p.x, p.y, p.width, p.height, filterindex:myindex).intersects(rectangle(x, y, width, height)))]");
        stmt.addListener(listener);

        sendSpatialEventRectanges(100, 50);
        sendAssertSpatialAABB(epService, listener, 100, 50, 1000);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEventIndexTableFireAndForget() {
        epService.getEPAdministrator().createEPL("create table MyTable(id string primary key, tx double, ty double, tw double, th double)");
        epService.getEPRuntime().executeQuery("insert into MyTable values ('R1', 10, 20, 5, 6)");
        epService.getEPAdministrator().createEPL("create index MyIdxCIFQuadTree on MyTable( (tx, ty, tw, th) mxcifquadtree(0, 0, 100, 100))");

        runAssertionFAF(10, 20, 0, 0, true);
        runAssertionFAF(9, 19, 1, 1, true);
        runAssertionFAF(9, 19, 0.9999, 0.9999, false);
        runAssertionFAF(15, 26, 0, 0, true);
        runAssertionFAF(15.0001, 26.0001, 0, 0, false);
        runAssertionFAF(0, 0, 100, 100, true);
        runAssertionFAF(11, 21, 1, 1, true);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFAF(double x, double y, double width, double height, boolean expected) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select id as c0 from MyTable where rectangle(tx, ty, tw, th).intersects(rectangle(" + x + ", " + y + ", " + width + ", " + height + "))");
        SupportQueryPlanIndexHook.assertFAFAndReset("MyIdxCIFQuadTree", "EventTableQuadTreeMXCIFImpl");
        if (expected) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), "c0".split(","), new Object[][]{{"R1"}});
        }
        else {
            assertEquals(0, result.getArray().length);
        }
    }

    private void runAssertionEventIndexPerformance() throws Exception {
        String epl = "create window MyRectangleWindow#keepall as (id string, rx double, ry double, rw double, rh double);\n" +
                "insert into MyRectangleWindow select id, x as rx, y as ry, width as rw, height as rh from SupportSpatialEventRectangle;\n" +
                "create index Idx on MyRectangleWindow( (rx, ry, rw, rh) mxcifquadtree(0, 0, 100, 100));\n" +
                "@Name('out') on SupportSpatialAABB select mpw.id as c0 from MyRectangleWindow as mpw where rectangle(rx, ry, rw, rh).intersects(rectangle(x, y, width, height));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        sendSpatialEventRectanges(100, 50);

        long start = System.currentTimeMillis();
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 50; y++) {
                epService.getEPRuntime().sendEvent(new SupportSpatialAABB("R", x, y, 0.5, 0.5));
                assertEquals(Integer.toString(x) + "_" + Integer.toString(y), listener.assertOneGetNewAndReset().get("c0"));
            }
        }
        long delta = System.currentTimeMillis() - start;
        assertTrue("delta=" + delta, delta < 2000);

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionDocSample() throws Exception {
        String epl =    "create table RectangleTable(rectangleId string primary key, rx double, ry double, rwidth double, rheight double);\n" +
                        "create index RectangleIndex on RectangleTable((rx, ry, rwidth, rheight) mxcifquadtree(0, 0, 100, 100));\n" +
                        "create schema OtherRectangleEvent(otherX double, otherY double, otherWidth double, otherHeight double);\n" +
                        "on OtherRectangleEvent\n" +
                        "select rectangleId from RectangleTable\n" +
                        "where rectangle(rx, ry, rwidth, rheight).intersects(rectangle(otherX, otherY, otherWidth, otherHeight));" +
                        "expression myMXCIFQuadtreeSettings { mxcifquadtree(0, 0, 100, 100) } \n" +
                        "select * from SupportSpatialAABB(rectangle(10, 20, 5, 5, filterindex:myMXCIFQuadtreeSettings).intersects(rectangle(x, y, width, height)));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionEventIndexUnique() throws Exception {
        String epl = "@Name('win') create window MyRectWindow#keepall as (id string, rx double, ry double, rw double, rh double);\n" +
                "@Name('insert') insert into MyRectWindow select id, x as rx, y as ry, width as rw, height as rh from SupportSpatialEventRectangle;\n" +
                "@Name('idx') create unique index Idx on MyRectWindow( (rx, ry, rw, rh) mxcifquadtree(0, 0, 100, 100));\n" +
                IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@Name('out') on SupportSpatialAABB select mpw.id as c0 from MyRectWindow as mpw where rectangle(rx, ry, rw, rh).intersects(rectangle(x, y, width, height));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset("Idx", "unique hash={} btree={} advanced={mxcifquadtree(rx,ry,rw,rh)}");

        sendEventRectangle("P1", 10, 15, 1, 2);
        try {
            sendEventRectangle("P1", 10, 15, 1, 2);
            fail();
        } catch (RuntimeException ex) { // we have a handler
            SupportMessageAssertUtil.assertMessage(ex,
                    "Unexpected exception in statement 'win': Unique index violation, index 'Idx' is a unique index and key '(10.0,15.0,1.0,2.0)' already exists");
        }

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionEventIndexUnindexed() {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select rectangle(one.x, one.y, one.width, one.height).intersects(rectangle(two.x, two.y, two.width, two.height)) as c0 from SupportSpatialDualAABB");
        stmt.addListener(listener);

        // For example, in MySQL:
        // SET @g1 = ST_GeomFromText('Polygon((1 1,1 2,2 2,2 1,1 1))');
        // SET @g2 = ST_GeomFromText('Polygon((2 2,2 4,4 4,4 2,2 2))');
        // SELECT MBRIntersects(@g1,@g2), MBRIntersects(@g2,@g1);
        // includes exterior

        sendAssert(rect(1, 1, 5, 5), rect(2, 2, 2, 2), true);
        sendAssert(rect(1, 1, 1, 1), rect(2, 2, 2, 2), true);
        sendAssert(rect(1, 0.9999, 1, 0.99999), rect(2, 2, 2, 2), false);
        sendAssert(rect(1, 1, 1, 0.99999), rect(2, 2, 2, 2), false);
        sendAssert(rect(1, 0.9999, 1, 1), rect(2, 2, 2, 2), false);

        sendAssert(rect(4, 4, 1, 1), rect(2, 2, 2, 2), true);
        sendAssert(rect(4.0001, 4, 1, 1), rect(2, 2, 2, 2), false);
        sendAssert(rect(4, 4.0001, 1, 1), rect(2, 2, 2, 2), false);

        sendAssert(rect(10, 20, 5, 5), rect(0, 0, 50, 50), true);
        sendAssert(rect(10, 20, 5, 5), rect(20, 20, 50, 50), false);
        sendAssert(rect(10, 20, 5, 5), rect(9, 19, 1, 1), true);
        sendAssert(rect(10, 20, 5, 5), rect(15, 25, 1, 1), true);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEventIndexOnTriggerNWInsertRemove(boolean soda) {
        SupportModelHelper.createByCompileOrParse(epService, soda, "create window MyWindow#length(5) as select * from SupportSpatialEventRectangle");
        SupportModelHelper.createByCompileOrParse(epService, soda, "create index MyIndex on MyWindow((x,y,width,height) mxcifquadtree(0,0,100,100))");
        SupportModelHelper.createByCompileOrParse(epService, soda, "insert into MyWindow select * from SupportSpatialEventRectangle");

        String epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + " on SupportSpatialAABB as aabb " +
                "select rects.id as c0 from MyWindow as rects where rectangle(rects.x,rects.y,rects.width,rects.height).intersects(rectangle(aabb.x,aabb.y,aabb.width,aabb.height))";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        stmt.addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIndex", "non-unique hash={} btree={} advanced={mxcifquadtree(x,y,width,height)}");

        sendEventRectangle("R1", 10, 40, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R1", null, null, null, null);

        sendEventRectangle("R2", 80, 80, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R1", null, null, "R2", null);

        sendEventRectangle("R3", 10, 40, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R1,R3", null, null, "R2", null);

        sendEventRectangle("R4", 60, 40, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R1,R3", "R4", null, "R2", "R4");

        sendEventRectangle("R5", 20, 75, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R1,R3", "R4", "R5", "R2", "R4");

        sendEventRectangle("R6", 50, 50, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R3,R6", "R4,R6", "R5,R6", "R2,R6", "R4,R6");

        sendEventRectangle("R7", 0, 0, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R3,R6,R7", "R4,R6", "R5,R6", "R6", "R4,R6");

        sendEventRectangle("R8", 99.999, 0, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R6,R7", "R4,R6,R8", "R5,R6", "R6", "R4,R6");

        sendEventRectangle("R9", 0, 99.999, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R6,R7", "R6,R8", "R5,R6,R9", "R6", "R6");

        sendEventRectangle("R10", 99.999, 99.999, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R6,R7", "R6,R8", "R6,R9", "R6,R10", "R6");

        sendEventRectangle("R11", 0, 0, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R7,R11", "R8", "R9", "R10", null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendEventRectangle(String id, double x, double y, double width, double height) {
        epService.getEPRuntime().sendEvent(new SupportSpatialEventRectangle(id, x, y, width, height));
    }

    private void sendAssertEventRectangle(double x, double y, double width, double height, boolean expected) {
        epService.getEPRuntime().sendEvent(new SupportSpatialEventRectangle(null, x, y, width, height));
        assertEquals(expected, listener.getIsInvokedAndReset());
    }

    private SupportSpatialAABB rect(double x, double y, double width, double height) {
        return new SupportSpatialAABB(null, x, y, width, height);
    }

    private void sendAssert(SupportSpatialAABB one, SupportSpatialAABB two, boolean expected) {
        BoundingBox bbOne = BoundingBox.from(one.getX(), one.getY(), one.getWidth(), one.getHeight());
        assertEquals(expected, bbOne.intersectsBoxIncludingEnd(two.getX(), two.getY(), two.getWidth(), two.getHeight()));

        BoundingBox bbTwo = BoundingBox.from(two.getX(), two.getY(), two.getWidth(), two.getHeight());
        assertEquals(expected, bbTwo.intersectsBoxIncludingEnd(one.getX(), one.getY(), one.getWidth(), one.getHeight()));

        epService.getEPRuntime().sendEvent(new SupportSpatialDualAABB(one, two));
        assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));

        epService.getEPRuntime().sendEvent(new SupportSpatialDualAABB(two, one));
        assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));
    }

    private void sendSpatialEventRectanges(int numX, int numY) {
        for (int x = 0; x < numX; x++) {
            for (int y = 0; y < numY; y++) {
                epService.getEPRuntime().sendEvent(new SupportSpatialEventRectangle(Integer.toString(x) + "_" + Integer.toString(y), (double) x, (double) y, 0.1, 0.2));
            }
        }
    }
}
