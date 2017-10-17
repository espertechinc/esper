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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.supportregression.bean.SupportSpatialAABB;
import com.espertech.esper.supportregression.bean.SupportSpatialDualAABB;
import com.espertech.esper.supportregression.bean.SupportSpatialEventRectangle;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.util.CollectionUtil;

import java.util.Arrays;
import java.util.List;

import static com.espertech.esper.regression.spatial.ExecSpatialMXCIFQuadTreeFilterIndex.sendSpatialEventRectanges;
import static com.espertech.esper.supportregression.util.SupportSpatialUtil.assertRectanglesManyRow;
import static org.junit.Assert.*;

public class ExecSpatialMXCIFQuadTreeEventIndex implements RegressionExecution {

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
        for (Class clazz : Arrays.asList(SupportSpatialAABB.class, SupportSpatialEventRectangle.class, SupportSpatialDualAABB.class)) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionEventIndexUnindexed(epService);

        runAssertionEventIndexOnTriggerNWInsertRemove(epService, false);
        runAssertionEventIndexOnTriggerNWInsertRemove(epService, true);
        runAssertionEventIndexUnique(epService);
        runAssertionEventIndexPerformance(epService);
        runAssertionEventIndexTableFireAndForget(epService);
        runAssertionEventIndexZeroWidthAndHeight(epService);
    }

    private void runAssertionEventIndexZeroWidthAndHeight(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create schema Geofence(x double, y double, vin string)");
        epService.getEPAdministrator().createEPL("create table Regions(regionId string primary key, rx double, ry double, rwidth double, rheight double)");
        epService.getEPAdministrator().createEPL("create index RectangleIndex on Regions((rx, ry, rwidth, rheight) mxcifquadtree(0, 0, 10, 12))");
        EPStatement stmt = epService.getEPAdministrator().createEPL(IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on Geofence as vin insert into VINWithRegion select regionId, vin from Regions where rectangle(rx, ry, rwidth, rheight).intersects(rectangle(vin.x, vin.y, 0, 0))");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset("RectangleIndex", "non-unique hash={} btree={} advanced={mxcifquadtree(rx,ry,rwidth,rheight)}");

        epService.getEPRuntime().executeQuery("insert into Regions values ('R1', 2, 2, 5, 5)");
        epService.getEPRuntime().sendEvent(CollectionUtil.populateNameValueMap("x", 3d, "y", 3d, "vin", "V1"), "Geofence");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "vin,regionId".split(","), new Object[] {"V1", "R1"});

        stmt.destroy();
    }

    private void runAssertionEventIndexTableFireAndForget(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create table MyTable(id string primary key, tx double, ty double, tw double, th double)");
        epService.getEPRuntime().executeQuery("insert into MyTable values ('R1', 10, 20, 5, 6)");
        epService.getEPAdministrator().createEPL("create index MyIdxCIFQuadTree on MyTable( (tx, ty, tw, th) mxcifquadtree(0, 0, 100, 100))");

        runAssertionFAF(epService, 10, 20, 0, 0, true);
        runAssertionFAF(epService, 9, 19, 1, 1, true);
        runAssertionFAF(epService, 9, 19, 0.9999, 0.9999, false);
        runAssertionFAF(epService, 15, 26, 0, 0, true);
        runAssertionFAF(epService, 15.0001, 26.0001, 0, 0, false);
        runAssertionFAF(epService, 0, 0, 100, 100, true);
        runAssertionFAF(epService, 11, 21, 1, 1, true);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFAF(EPServiceProvider epService, double x, double y, double width, double height, boolean expected) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select id as c0 from MyTable where rectangle(tx, ty, tw, th).intersects(rectangle(" + x + ", " + y + ", " + width + ", " + height + "))");
        SupportQueryPlanIndexHook.assertFAFAndReset("MyIdxCIFQuadTree", "EventTableQuadTreeMXCIFImpl");
        if (expected) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), "c0".split(","), new Object[][]{{"R1"}});
        } else {
            assertEquals(0, result.getArray().length);
        }
    }

    private void runAssertionEventIndexPerformance(EPServiceProvider epService) throws Exception {
        String epl = "create window MyRectangleWindow#keepall as (id string, rx double, ry double, rw double, rh double);\n" +
                "insert into MyRectangleWindow select id, x as rx, y as ry, width as rw, height as rh from SupportSpatialEventRectangle;\n" +
                "create index Idx on MyRectangleWindow( (rx, ry, rw, rh) mxcifquadtree(0, 0, 100, 100));\n" +
                "@Name('out') on SupportSpatialAABB select mpw.id as c0 from MyRectangleWindow as mpw where rectangle(rx, ry, rw, rh).intersects(rectangle(x, y, width, height));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        sendSpatialEventRectanges(epService, 100, 50);

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

    private void runAssertionEventIndexUnique(EPServiceProvider epService) throws Exception {
        String epl = "@Name('win') create window MyRectWindow#keepall as (id string, rx double, ry double, rw double, rh double);\n" +
                "@Name('insert') insert into MyRectWindow select id, x as rx, y as ry, width as rw, height as rh from SupportSpatialEventRectangle;\n" +
                "@Name('idx') create unique index Idx on MyRectWindow( (rx, ry, rw, rh) mxcifquadtree(0, 0, 100, 100));\n" +
                IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@Name('out') on SupportSpatialAABB select mpw.id as c0 from MyRectWindow as mpw where rectangle(rx, ry, rw, rh).intersects(rectangle(x, y, width, height));\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset("Idx", "unique hash={} btree={} advanced={mxcifquadtree(rx,ry,rw,rh)}");

        sendEventRectangle(epService, "P1", 10, 15, 1, 2);
        try {
            sendEventRectangle(epService, "P1", 10, 15, 1, 2);
            fail();
        } catch (RuntimeException ex) { // we have a handler
            SupportMessageAssertUtil.assertMessage(ex,
                    "Unexpected exception in statement 'win': Unique index violation, index 'Idx' is a unique index and key '(10.0,15.0,1.0,2.0)' already exists");
        }

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionEventIndexUnindexed(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select rectangle(one.x, one.y, one.width, one.height).intersects(rectangle(two.x, two.y, two.width, two.height)) as c0 from SupportSpatialDualAABB");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // For example, in MySQL:
        // SET @g1 = ST_GeomFromText('Polygon((1 1,1 2,2 2,2 1,1 1))');
        // SET @g2 = ST_GeomFromText('Polygon((2 2,2 4,4 4,4 2,2 2))');
        // SELECT MBRIntersects(@g1,@g2), MBRIntersects(@g2,@g1);
        // includes exterior

        sendAssert(epService, listener, rect(1, 1, 5, 5), rect(2, 2, 2, 2), true);
        sendAssert(epService, listener, rect(1, 1, 1, 1), rect(2, 2, 2, 2), true);
        sendAssert(epService, listener, rect(1, 0.9999, 1, 0.99999), rect(2, 2, 2, 2), false);
        sendAssert(epService, listener, rect(1, 1, 1, 0.99999), rect(2, 2, 2, 2), false);
        sendAssert(epService, listener, rect(1, 0.9999, 1, 1), rect(2, 2, 2, 2), false);

        sendAssert(epService, listener, rect(4, 4, 1, 1), rect(2, 2, 2, 2), true);
        sendAssert(epService, listener, rect(4.0001, 4, 1, 1), rect(2, 2, 2, 2), false);
        sendAssert(epService, listener, rect(4, 4.0001, 1, 1), rect(2, 2, 2, 2), false);

        sendAssert(epService, listener, rect(10, 20, 5, 5), rect(0, 0, 50, 50), true);
        sendAssert(epService, listener, rect(10, 20, 5, 5), rect(20, 20, 50, 50), false);
        sendAssert(epService, listener, rect(10, 20, 5, 5), rect(9, 19, 1, 1), true);
        sendAssert(epService, listener, rect(10, 20, 5, 5), rect(15, 25, 1, 1), true);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEventIndexOnTriggerNWInsertRemove(EPServiceProvider epService, boolean soda) {
        SupportModelHelper.createByCompileOrParse(epService, soda, "create window MyWindow#length(5) as select * from SupportSpatialEventRectangle");
        SupportModelHelper.createByCompileOrParse(epService, soda, "create index MyIndex on MyWindow((x,y,width,height) mxcifquadtree(0,0,100,100))");
        SupportModelHelper.createByCompileOrParse(epService, soda, "insert into MyWindow select * from SupportSpatialEventRectangle");

        String epl = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + " on SupportSpatialAABB as aabb " +
                "select rects.id as c0 from MyWindow as rects where rectangle(rects.x,rects.y,rects.width,rects.height).intersects(rectangle(aabb.x,aabb.y,aabb.width,aabb.height))";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIndex", "non-unique hash={} btree={} advanced={mxcifquadtree(x,y,width,height)}");

        sendEventRectangle(epService, "R1", 10, 40, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R1", null, null, null, null);

        sendEventRectangle(epService, "R2", 80, 80, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R1", null, null, "R2", null);

        sendEventRectangle(epService, "R3", 10, 40, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R1,R3", null, null, "R2", null);

        sendEventRectangle(epService, "R4", 60, 40, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R1,R3", "R4", null, "R2", "R4");

        sendEventRectangle(epService, "R5", 20, 75, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R1,R3", "R4", "R5", "R2", "R4");

        sendEventRectangle(epService, "R6", 50, 50, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R3,R6", "R4,R6", "R5,R6", "R2,R6", "R4,R6");

        sendEventRectangle(epService, "R7", 0, 0, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R3,R6,R7", "R4,R6", "R5,R6", "R6", "R4,R6");

        sendEventRectangle(epService, "R8", 99.999, 0, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R6,R7", "R4,R6,R8", "R5,R6", "R6", "R4,R6");

        sendEventRectangle(epService, "R9", 0, 99.999, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R6,R7", "R6,R8", "R5,R6,R9", "R6", "R6");

        sendEventRectangle(epService, "R10", 99.999, 99.999, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R6,R7", "R6,R8", "R6,R9", "R6,R10", "R6");

        sendEventRectangle(epService, "R11", 0, 0, 1, 1);
        assertRectanglesManyRow(epService, listener, BOXES, "R7,R11", "R8", "R9", "R10", null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendEventRectangle(EPServiceProvider epService, String id, double x, double y, double width, double height) {
        epService.getEPRuntime().sendEvent(new SupportSpatialEventRectangle(id, x, y, width, height));
    }

    private SupportSpatialAABB rect(double x, double y, double width, double height) {
        return new SupportSpatialAABB(null, x, y, width, height);
    }

    private void sendAssert(EPServiceProvider epService, SupportUpdateListener listener, SupportSpatialAABB one, SupportSpatialAABB two, boolean expected) {
        BoundingBox bbOne = BoundingBox.from(one.getX(), one.getY(), one.getWidth(), one.getHeight());
        assertEquals(expected, bbOne.intersectsBoxIncludingEnd(two.getX(), two.getY(), two.getWidth(), two.getHeight()));

        BoundingBox bbTwo = BoundingBox.from(two.getX(), two.getY(), two.getWidth(), two.getHeight());
        assertEquals(expected, bbTwo.intersectsBoxIncludingEnd(one.getX(), one.getY(), one.getWidth(), one.getHeight()));

        epService.getEPRuntime().sendEvent(new SupportSpatialDualAABB(one, two));
        assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));

        epService.getEPRuntime().sendEvent(new SupportSpatialDualAABB(two, one));
        assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));
    }
}
