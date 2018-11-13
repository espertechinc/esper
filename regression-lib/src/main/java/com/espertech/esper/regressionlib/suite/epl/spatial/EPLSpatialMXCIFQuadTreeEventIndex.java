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
package com.espertech.esper.regressionlib.suite.epl.spatial;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQueryParameterized;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.epl.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.common.internal.epl.spatial.quadtree.core.QuadrantEnum;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportSpatialAABB;
import com.espertech.esper.regressionlib.support.bean.SupportSpatialDualAABB;
import com.espertech.esper.regressionlib.support.bean.SupportSpatialEventRectangle;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;
import com.espertech.esper.regressionlib.support.util.SupportSpatialUtil;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.support.util.SupportSpatialUtil.*;
import static org.junit.Assert.*;

public class EPLSpatialMXCIFQuadTreeEventIndex {

    private final static List<BoundingBox> BOXES = Arrays.asList(
        new BoundingBox(0, 0, 50, 50),
        new BoundingBox(50, 0, 100, 50),
        new BoundingBox(0, 50, 50, 100),
        new BoundingBox(50, 50, 100, 100),
        new BoundingBox(25, 25, 75, 75)
    );

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSpatialMXCIFEventIndexNamedWindowSimple());
        execs.add(new EPLSpatialMXCIFEventIndexUnindexed());
        execs.add(new EPLSpatialMXCIFEventIndexOnTriggerNWInsertRemove(false));
        execs.add(new EPLSpatialMXCIFEventIndexOnTriggerNWInsertRemove(true));
        execs.add(new EPLSpatialMXCIFEventIndexUnique());
        execs.add(new EPLSpatialMXCIFEventIndexPerformance());
        execs.add(new EPLSpatialMXCIFEventIndexTableFireAndForget());
        execs.add(new EPLSpatialMXCIFEventIndexZeroWidthAndHeight());
        execs.add(new EPLSpatialMXCIFEventIndexTableSubdivideMergeDestroy());
        execs.add(new EPLSpatialMXCIFEventIndexTableSubdivideDeepAddDestroy());
        execs.add(new EPLSpatialMXCIFEventIndexTableSubdivideDestroy());
        execs.add(new EPLSpatialMXCIFEventIndexEdgeSubdivide(true));
        execs.add(new EPLSpatialMXCIFEventIndexEdgeSubdivide(false));
        execs.add(new EPLSpatialMXCIFEventIndexRandomMovingPoints());
        execs.add(new EPLSpatialMXCIFEventIndexRandomIntPointsInSquareUnique());
        execs.add(new EPLSpatialMXCIFEventIndexRandomRectsWRandomQuery());
        return execs;
    }

    private static class EPLSpatialMXCIFEventIndexNamedWindowSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "create window RectangleWindow#keepall as (id string, rx double, ry double, rw double, rh double);\n" +
                "create index MyIndex on RectangleWindow((rx,ry,rw,rh) mxcifquadtree(0,0,100,100));\n" +
                "insert into RectangleWindow select id, x as rx, y as ry, width as rw, height as rh from SupportSpatialEventRectangle;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from RectangleWindow as pt where rectangle(rx,ry,rw,rh).intersects(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl).addListener("out");

            sendEventRectangle(env, "R0", 73.32704983331149, 23.46990952575032, 1, 1);
            sendEventRectangle(env, "R1", 53.09747562396894, 17.100976152185034, 1, 1);
            sendEventRectangle(env, "R2", 56.75757294858788, 25.508506696809608, 1, 1);
            sendEventRectangle(env, "R3", 83.66639067675291, 76.53772974832937, 1, 1);
            sendEventRectangle(env, "R4", 51.01654641861326, 43.49009281983866, 1, 1);

            env.milestone(0);

            double beginX = 50.45945198254618;
            double endX = 88.31594559038719;
            double beginY = 4.577595744501329;
            double endY = 22.93393078279351;

            env.sendEventBean(new SupportSpatialAABB("", beginX, beginY, endX - beginX, endY - beginY));
            String received = sortJoinProperty(env.listener("out").getAndResetLastNewData(), "c0");
            assertEquals("R1", received);

            env.undeployAll();
        }
    }

    private static class EPLSpatialMXCIFEventIndexZeroWidthAndHeight implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeployWBusPublicType("create schema Geofence(x double, y double, vin string)", path);

            env.compileDeploy("create table Regions(regionId string primary key, rx double, ry double, rwidth double, rheight double)", path);
            env.compileDeploy("create index RectangleIndex on Regions((rx, ry, rwidth, rheight) mxcifquadtree(0, 0, 10, 12))", path);
            env.compileDeploy("@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on Geofence as vin insert into VINWithRegion select regionId, vin from Regions where rectangle(rx, ry, rwidth, rheight).intersects(rectangle(vin.x, vin.y, 0, 0))", path);
            env.addListener("s0");

            SupportQueryPlanIndexHook.assertOnExprTableAndReset("RectangleIndex", "non-unique hash={} btree={} advanced={mxcifquadtree(rx,ry,rwidth,rheight)}");

            env.compileExecuteFAF("insert into Regions values ('R1', 2, 2, 5, 5)", path);
            env.sendEventMap(CollectionUtil.populateNameValueMap("x", 3d, "y", 3d, "vin", "V1"), "Geofence");

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "vin,regionId".split(","), new Object[]{"V1", "R1"});

            env.undeployAll();
        }
    }

    private static class EPLSpatialMXCIFEventIndexTableFireAndForget implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table MyTable(id string primary key, tx double, ty double, tw double, th double)", path);
            env.compileExecuteFAF("insert into MyTable values ('R1', 10, 20, 5, 6)", path);
            env.compileDeploy("create index MyIdxCIFQuadTree on MyTable( (tx, ty, tw, th) mxcifquadtree(0, 0, 100, 100))", path);

            runAssertionFAF(env, path, 10, 20, 0, 0, true);
            runAssertionFAF(env, path, 9, 19, 1, 1, true);
            runAssertionFAF(env, path, 9, 19, 0.9999, 0.9999, false);
            runAssertionFAF(env, path, 15, 26, 0, 0, true);
            runAssertionFAF(env, path, 15.0001, 26.0001, 0, 0, false);
            runAssertionFAF(env, path, 0, 0, 100, 100, true);
            runAssertionFAF(env, path, 11, 21, 1, 1, true);

            env.undeployAll();
        }
    }

    private static void runAssertionFAF(RegressionEnvironment env, RegressionPath path, double x, double y, double width, double height, boolean expected) {
        EPFireAndForgetQueryResult result = env.compileExecuteFAF(IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select id as c0 from MyTable where rectangle(tx, ty, tw, th).intersects(rectangle(" + x + ", " + y + ", " + width + ", " + height + "))", path);
        SupportQueryPlanIndexHook.assertFAFAndReset("MyIdxCIFQuadTree", "EventTableQuadTreeMXCIF");
        if (expected) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), "c0".split(","), new Object[][]{{"R1"}});
        } else {
            assertEquals(0, result.getArray().length);
        }
    }

    private static class EPLSpatialMXCIFEventIndexPerformance implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String epl = "create window MyRectangleWindow#keepall as (id string, rx double, ry double, rw double, rh double);\n" +
                "insert into MyRectangleWindow select id, x as rx, y as ry, width as rw, height as rh from SupportSpatialEventRectangle;\n" +
                "create index Idx on MyRectangleWindow( (rx, ry, rw, rh) mxcifquadtree(0, 0, 100, 100));\n" +
                "@Name('out') on SupportSpatialAABB select mpw.id as c0 from MyRectangleWindow as mpw where rectangle(rx, ry, rw, rh).intersects(rectangle(x, y, width, height));\n";
            env.compileDeploy(epl).addListener("out");

            sendSpatialEventRectanges(env, 100, 50);

            long start = System.currentTimeMillis();
            SupportListener listener = env.listener("out");
            for (int x = 0; x < 100; x++) {
                for (int y = 0; y < 50; y++) {
                    env.sendEventBean(new SupportSpatialAABB("R", x, y, 0.5, 0.5));
                    assertEquals(Integer.toString(x) + "_" + Integer.toString(y), listener.assertOneGetNewAndReset().get("c0"));
                }
            }
            long delta = System.currentTimeMillis() - start;
            assertTrue("delta=" + delta, delta < 2000);

            env.undeployAll();
        }
    }

    private static class EPLSpatialMXCIFEventIndexUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@Name('win') create window MyRectWindow#keepall as (id string, rx double, ry double, rw double, rh double);\n" +
                "@Name('insert') insert into MyRectWindow select id, x as rx, y as ry, width as rw, height as rh from SupportSpatialEventRectangle;\n" +
                "@Name('idx') create unique index Idx on MyRectWindow( (rx, ry, rw, rh) mxcifquadtree(0, 0, 100, 100));\n" +
                IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@Name('out') on SupportSpatialAABB select mpw.id as c0 from MyRectWindow as mpw where rectangle(rx, ry, rw, rh).intersects(rectangle(x, y, width, height));\n";
            env.compileDeploy(epl).addListener("out");

            SupportQueryPlanIndexHook.assertOnExprTableAndReset("Idx", "unique hash={} btree={} advanced={mxcifquadtree(rx,ry,rw,rh)}");

            sendEventRectangle(env, "P1", 10, 15, 1, 2);
            try {
                sendEventRectangle(env, "P1", 10, 15, 1, 2);
                fail();
            } catch (RuntimeException ex) { // we have a handler
                SupportMessageAssertUtil.assertMessage(ex,
                    "Unexpected exception in statement 'win': Unique index violation, index 'Idx' is a unique index and key '(10.0,15.0,1.0,2.0)' already exists");
            }

            env.undeployAll();
        }
    }

    private static class EPLSpatialMXCIFEventIndexUnindexed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select rectangle(one.x, one.y, one.width, one.height).intersects(rectangle(two.x, two.y, two.width, two.height)) as c0 from SupportSpatialDualAABB");
            env.addListener("s0");

            // For example, in MySQL:
            // SET @g1 = ST_GeomFromText('Polygon((1 1,1 2,2 2,2 1,1 1))');
            // SET @g2 = ST_GeomFromText('Polygon((2 2,2 4,4 4,4 2,2 2))');
            // SELECT MBRIntersects(@g1,@g2), MBRIntersects(@g2,@g1);
            // includes exterior

            sendAssert(env, rect(1, 1, 5, 5), rect(2, 2, 2, 2), true);
            sendAssert(env, rect(1, 1, 1, 1), rect(2, 2, 2, 2), true);

            env.milestone(0);

            sendAssert(env, rect(1, 0.9999, 1, 0.99999), rect(2, 2, 2, 2), false);
            sendAssert(env, rect(1, 1, 1, 0.99999), rect(2, 2, 2, 2), false);
            sendAssert(env, rect(1, 0.9999, 1, 1), rect(2, 2, 2, 2), false);

            sendAssert(env, rect(4, 4, 1, 1), rect(2, 2, 2, 2), true);
            sendAssert(env, rect(4.0001, 4, 1, 1), rect(2, 2, 2, 2), false);
            sendAssert(env, rect(4, 4.0001, 1, 1), rect(2, 2, 2, 2), false);

            env.milestone(1);

            sendAssert(env, rect(10, 20, 5, 5), rect(0, 0, 50, 50), true);
            sendAssert(env, rect(10, 20, 5, 5), rect(20, 20, 50, 50), false);
            sendAssert(env, rect(10, 20, 5, 5), rect(9, 19, 1, 1), true);
            sendAssert(env, rect(10, 20, 5, 5), rect(15, 25, 1, 1), true);
            env.undeployAll();
        }
    }

    private static class EPLSpatialMXCIFEventIndexOnTriggerNWInsertRemove implements RegressionExecution {

        private final boolean soda;

        public EPLSpatialMXCIFEventIndexOnTriggerNWInsertRemove(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy(soda, "create window MyWindow#length(5) as select * from SupportSpatialEventRectangle", path);
            env.compileDeploy(soda, "create index MyIndex on MyWindow((x,y,width,height) mxcifquadtree(0,0,100,100))", path);
            env.compileDeploy(soda, "insert into MyWindow select * from SupportSpatialEventRectangle", path);

            String epl = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + " on SupportSpatialAABB as aabb " +
                "select rects.id as c0 from MyWindow as rects where rectangle(rects.x,rects.y,rects.width,rects.height).intersects(rectangle(aabb.x,aabb.y,aabb.width,aabb.height))";
            env.compileDeploy(soda, epl, path).addListener("s0");

            SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIndex", "non-unique hash={} btree={} advanced={mxcifquadtree(x,y,width,height)}");

            sendEventRectangle(env, "R1", 10, 40, 1, 1);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "R1", null, null, null, null);

            sendEventRectangle(env, "R2", 80, 80, 1, 1);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "R1", null, null, "R2", null);

            env.milestone(0);

            sendEventRectangle(env, "R3", 10, 40, 1, 1);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "R1,R3", null, null, "R2", null);

            env.milestone(1);

            sendEventRectangle(env, "R4", 60, 40, 1, 1);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "R1,R3", "R4", null, "R2", "R4");

            sendEventRectangle(env, "R5", 20, 75, 1, 1);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "R1,R3", "R4", "R5", "R2", "R4");

            env.milestone(2);

            sendEventRectangle(env, "R6", 50, 50, 1, 1);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "R3,R6", "R4,R6", "R5,R6", "R2,R6", "R4,R6");

            sendEventRectangle(env, "R7", 0, 0, 1, 1);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "R3,R6,R7", "R4,R6", "R5,R6", "R6", "R4,R6");

            env.milestone(3);

            sendEventRectangle(env, "R8", 99.999, 0, 1, 1);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "R6,R7", "R4,R6,R8", "R5,R6", "R6", "R4,R6");

            sendEventRectangle(env, "R9", 0, 99.999, 1, 1);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "R6,R7", "R6,R8", "R5,R6,R9", "R6", "R6");

            env.milestone(4);

            sendEventRectangle(env, "R10", 99.999, 99.999, 1, 1);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "R6,R7", "R6,R8", "R6,R9", "R6,R10", "R6");

            sendEventRectangle(env, "R11", 0, 0, 1, 1);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "R7,R11", "R8", "R9", "R10", null);

            env.undeployAll();
        }
    }


    public static class EPLSpatialMXCIFEventIndexTableSubdivideMergeDestroy implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String epl = "create table RectangleTable(id string primary key, rx double, ry double, rw double, rh double);\n" +
                "create index MyIndex on RectangleTable((rx,ry,rw,rh) mxcifquadtree(0,0,100,100,4,20));\n" +
                "insert into RectangleTable select id, x as rx, y as ry, width as rw, height as rh from SupportSpatialEventRectangle;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from RectangleTable as pt where rectangle(rx,ry,rw,rh).intersects(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl, path).addListener("out");

            sendEventRectangle(env, "P1", 80, 80, 1, 1);
            sendEventRectangle(env, "P2", 81, 80, 1, 1);
            sendEventRectangle(env, "P3", 80, 81, 1, 1);
            sendEventRectangle(env, "P4", 80, 80, 1, 1);
            sendEventRectangle(env, "P5", 45, 55, 1, 1);
            assertRectanglesManyRow(env, env.listener("out"), BOXES, null, null, "P5", "P1,P2,P3,P4", "P5");

            env.milestone(0);

            assertRectanglesManyRow(env, env.listener("out"), BOXES, null, null, "P5", "P1,P2,P3,P4", "P5");
            env.compileExecuteFAF("delete from RectangleTable where id = 'P4'", path);
            assertRectanglesManyRow(env, env.listener("out"), BOXES, null, null, "P5", "P1,P2,P3", "P5");

            env.milestone(1);

            assertRectanglesManyRow(env, env.listener("out"), BOXES, null, null, "P5", "P1,P2,P3", "P5");

            env.undeployAll();
        }
    }

    public static class EPLSpatialMXCIFEventIndexTableSubdivideDeepAddDestroy implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "create table RectangleTable(id string primary key, x double, y double, width double, height double);\n" +
                "create index MyIndex on RectangleTable((x,y,width,height) mxcifquadtree(0,0,100,100,2,12));\n" +
                "insert into RectangleTable select id, x, y, width, height from SupportSpatialEventRectangle;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from RectangleTable as pt where rectangle(pt.x,pt.y,pt.width,pt.height).intersects(rectangle(aabb.x,aabb.y,aabb.width,aabb.height));\n";
            env.compileDeploy(epl).addListener("out");

            List<SupportSpatialEventRectangle> rectangles = new ArrayList<>();
            BoundingBox.BoundingBoxNode bbtree = new BoundingBox(0, 0, 100, 100).treeForPath("nw,se,sw,ne,nw,nw,nw,nw,nw,nw,nw,nw".split(","));
            BoundingBox somewhere = bbtree.nw.se.sw.ne.nw.nw.nw.nw.nw.nw.nw.nw.bb;

            addSendRectangle(env, rectangles, "P1", somewhere.getMinX(), somewhere.getMinY(), 0.0001, 0.0001);
            addSendRectangle(env, rectangles, "P2", somewhere.getMinX(), somewhere.getMinY(), 0.0001, 0.0001);
            addSendRectangle(env, rectangles, "P3", somewhere.getMinX(), somewhere.getMinY(), 0.0001, 0.0001);
            assertBBTreeRectangles(env, bbtree, rectangles);

            env.milestone(0);

            assertBBTreeRectangles(env, bbtree, rectangles);

            env.undeployAll();
        }
    }

    public static class EPLSpatialMXCIFEventIndexTableSubdivideDestroy implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "create table RectangleTable(id string primary key, x double, y double, width double, height double);\n" +
                "create index MyIndex on RectangleTable((x,y,width,height) mxcifquadtree(0,0,100,100,4,40));\n" +
                "insert into RectangleTable select id, x, y, width, height from SupportSpatialEventRectangle;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from RectangleTable as pt where rectangle(pt.x,pt.y,pt.width,pt.height).intersects(rectangle(aabb.x,aabb.y,aabb.width,aabb.height));\n";
            env.compileDeploy(epl).addListener("out");

            sendEventRectangle(env, "P1", 80, 40, 1, 1);
            sendEventRectangle(env, "P2", 81, 41, 1, 1);
            sendEventRectangle(env, "P3", 80, 40, 1, 1);

            env.milestone(0);

            assertRectanglesManyRow(env, env.listener("out"), BOXES, null, "P1,P2,P3", null, null, null);
            sendEventRectangle(env, "P4", 80, 40, 1, 1);
            sendEventRectangle(env, "P5", 81, 41, 1, 1);

            env.milestone(1);

            assertRectanglesManyRow(env, env.listener("out"), BOXES, null, "P1,P2,P3,P4,P5", null, null, null);

            env.undeployAll();
        }
    }

    public static class EPLSpatialMXCIFEventIndexEdgeSubdivide implements RegressionExecution {
        private final boolean straddle;

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public EPLSpatialMXCIFEventIndexEdgeSubdivide(boolean straddle) {
            this.straddle = straddle;
        }

        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String epl = "create window RectangleWindow#keepall as (id string, x double, y double, width double, height double);\n" +
                "create index MyIndex on RectangleWindow((x,y,width,height) mxcifquadtree(0,0,100,100,2,5));\n" +
                "insert into RectangleWindow select id, x, y, width, height from SupportSpatialEventRectangle;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from RectangleWindow as pt where rectangle(pt.x,pt.y,pt.width,pt.height).intersects(rectangle(aabb.x,aabb.y,aabb.width,aabb.height));\n";
            env.compileDeploy(epl, path).addListener("out");

            Set<BoundingBox> boxesLevel4 = getLevel5Boxes();
            int count = 0;
            List<SupportSpatialEventRectangle> rectangles = new ArrayList<>();
            double offset = straddle ? 0 : 0.001;
            for (BoundingBox bb : boxesLevel4) {
                sendAddRectangle(env, rectangles, "A" + count, bb.getMinX() + offset, bb.getMinY() + offset, 0.001, 0.001);
                sendAddRectangle(env, rectangles, "B" + count, bb.getMinX() + offset, bb.getMinY() + offset, 0.001, 0.001);
                sendAddRectangle(env, rectangles, "C" + count, bb.getMinX() + offset, bb.getMinY() + offset, 0.001, 0.001);
                count++;
            }
            assertAllRectangles(env, rectangles, 0, 0, 100, 100);

            env.milestone(0);

            removeAllABRectangles(env, path, rectangles);
            assertAllRectangles(env, rectangles, 0, 0, 100, 100);

            env.milestone(1);

            assertAllRectangles(env, rectangles, 0, 0, 100, 100);
            removeEverySecondRectangle(env, path, rectangles);
            assertAllRectangles(env, rectangles, 0, 0, 100, 100);

            env.milestone(2);

            assertAllRectangles(env, rectangles, 0, 0, 100, 100);
            removeEverySecondRectangle(env, path, rectangles);
            assertAllRectangles(env, rectangles, 0, 0, 100, 100);

            env.milestone(3);

            assertAllRectangles(env, rectangles, 0, 0, 100, 100);
            removeAllRectangles(env, path, rectangles);
            assertAllRectangles(env, rectangles, 0, 0, 100, 100);

            env.milestone(4);

            removeAllRectangles(env, path, rectangles);

            env.undeployAll();
        }

        private void removeEverySecondRectangle(RegressionEnvironment env, RegressionPath path, List<SupportSpatialEventRectangle> rectangles) {
            Iterator<SupportSpatialEventRectangle> it = rectangles.iterator();
            int count = 0;
            List<String> idList = new ArrayList<>();
            for (; it.hasNext(); ) {
                SupportSpatialEventRectangle p = it.next();
                if (count % 2 == 1) {
                    it.remove();
                    idList.add(p.getId());
                }
                count++;
            }
            String deleteQuery = SupportSpatialUtil.buildDeleteQueryWithInClause("RectangleWindow", "id", idList);
            env.compileExecuteFAF(deleteQuery, path);
        }

        private void removeAllRectangles(RegressionEnvironment env, RegressionPath path, List<SupportSpatialEventRectangle> points) {
            Iterator<SupportSpatialEventRectangle> it = points.iterator();
            List<String> idList = new ArrayList<>();
            for (; it.hasNext(); ) {
                SupportSpatialEventRectangle p = it.next();
                it.remove();
                idList.add(p.getId());
            }
            if (idList.isEmpty()) {
                return;
            }
            String deleteQuery = SupportSpatialUtil.buildDeleteQueryWithInClause("RectangleWindow", "id", idList);
            env.compileExecuteFAF(deleteQuery, path);
        }

        private void removeAllABRectangles(RegressionEnvironment env, RegressionPath path, List<SupportSpatialEventRectangle> rectangles) {
            Iterator<SupportSpatialEventRectangle> it = rectangles.iterator();
            List<String> idList = new ArrayList<>();
            for (; it.hasNext(); ) {
                SupportSpatialEventRectangle p = it.next();
                if (p.getId().charAt(0) == 'A' || p.getId().charAt(0) == 'B') {
                    it.remove();
                    idList.add(p.getId());
                }
            }
            String deleteQuery = SupportSpatialUtil.buildDeleteQueryWithInClause("RectangleWindow", "id", idList);
            env.compileExecuteFAF(deleteQuery, path);
        }

        private Set<BoundingBox> getLevel5Boxes() {
            BoundingBox bb = new BoundingBox(0, 0, 100, 100);
            BoundingBox.BoundingBoxNode bbtree = bb.treeForDepth(4);
            LinkedHashSet<BoundingBox> bbs = new LinkedHashSet<>();
            for (QuadrantEnum lvl1 : QuadrantEnum.values()) {
                BoundingBox.BoundingBoxNode q1 = bbtree.getQuadrant(lvl1);
                for (QuadrantEnum lvl2 : QuadrantEnum.values()) {
                    BoundingBox.BoundingBoxNode q2 = q1.getQuadrant(lvl2);
                    for (QuadrantEnum lvl3 : QuadrantEnum.values()) {
                        BoundingBox.BoundingBoxNode q3 = q2.getQuadrant(lvl3);
                        for (QuadrantEnum lvl4 : QuadrantEnum.values()) {
                            BoundingBox.BoundingBoxNode q4 = q3.getQuadrant(lvl4);
                            bbs.add(q4.bb);
                        }
                    }
                }
            }
            assertEquals(256, bbs.size());
            return bbs;
        }
    }

    public static class EPLSpatialMXCIFEventIndexRandomRectsWRandomQuery implements RegressionExecution {
        private static final Logger log = LoggerFactory.getLogger(EPLSpatialMXCIFEventIndexRandomRectsWRandomQuery.class);

        private final static int NUM_POINTS = 1000;
        private final static int X = 0;
        private final static int Y = 0;
        private final static int WIDTH = 100;
        private final static int HEIGHT = 100;
        private final static int NUM_QUERIES_AFTER_LOAD = 100;
        private final static int NUM_QUERIES_AFTER_EACH_REMOVE = 5;
        private final static int[] CHECKPOINT_REMAINING = new int[]{100, 300, 700}; // must be sorted

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String epl = "create window RectangleWindow#keepall as (id string, px double, py double, pw double, ph double);\n" +
                "create index MyIndex on RectangleWindow((px,py,pw,ph) mxcifquadtree(0,0,100,100));\n" +
                "insert into RectangleWindow select id, x as px, y as py, width as pw, height as ph from SupportSpatialEventRectangle;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from RectangleWindow as pt where rectangle(px,py,pw,ph).intersects(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl, path).addListener("out");

            Random random = new Random();
            List<SupportSpatialEventRectangle> rectangles = randomRectangles(random, NUM_POINTS, X, Y, WIDTH, HEIGHT);
            for (SupportSpatialEventRectangle rectangle : rectangles) {
                sendEventRectangle(env, rectangle.getId(), rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
                // Comment-me-in: log.info("Point: " + rectangle);
            }
            env.milestone(0);

            for (int i = 0; i < NUM_QUERIES_AFTER_LOAD; i++) {
                randomQuery(env, path, random, rectangles);
            }

            AtomicInteger milestone = new AtomicInteger();
            EPCompiled deleteQuery = env.compileFAF("delete from RectangleWindow where id=?::string", path);
            EPFireAndForgetPreparedQueryParameterized preparedDelete = env.runtime().getFireAndForgetService().prepareQueryWithParameters(deleteQuery);

            while (!rectangles.isEmpty()) {
                SupportSpatialEventRectangle removed = randomRemove(random, rectangles);
                preparedDelete.setObject(1, removed.getId());
                env.runtime().getFireAndForgetService().executeQuery(preparedDelete);

                for (int i = 0; i < NUM_QUERIES_AFTER_EACH_REMOVE; i++) {
                    randomQuery(env, path, random, rectangles);
                }

                if (Arrays.binarySearch(CHECKPOINT_REMAINING, rectangles.size()) >= 0) {
                    log.info("Checkpoint at " + rectangles.size());
                    env.milestoneInc(milestone);
                    preparedDelete = env.runtime().getFireAndForgetService().prepareQueryWithParameters(deleteQuery);
                }
            }

            env.undeployAll();
        }

        private SupportSpatialEventRectangle randomRemove(Random random, List<SupportSpatialEventRectangle> rectangles) {
            int index = random.nextInt(rectangles.size());
            return rectangles.remove(index);
        }

        private void randomQuery(RegressionEnvironment env, RegressionPath path, Random random, List<SupportSpatialEventRectangle> rectangles) {
            double bbWidth = random.nextDouble() * WIDTH * 1.5;
            double bbHeight = random.nextDouble() * HEIGHT * 1.5;
            double bbMinX = random.nextDouble() * WIDTH + X * 0.8;
            double bbMinY = random.nextDouble() * HEIGHT + Y * 0.8;
            double bbMaxX = bbMinX + bbWidth;
            double bbMaxY = bbMinY + bbHeight;
            BoundingBox boundingBox = new BoundingBox(bbMinX, bbMinY, bbMaxX, bbMaxY);
            // Comment-me-in: log.info("Query: " + boundingBox);
            assertBBRectangles(env, boundingBox, rectangles);
        }
    }

    public static class EPLSpatialMXCIFEventIndexRandomMovingPoints implements RegressionExecution {
        private static final Logger log = LoggerFactory.getLogger(EPLSpatialMXCIFEventIndexRandomMovingPoints.class);

        private final static int NUM_RECTANGLES = 1000;
        private final static int NUM_MOVES = 5000;
        private final static int WIDTH = 100;
        private final static int HEIGHT = 100;
        private final static int[] CHECKPOINT_AT = new int[]{500, 3000, 4000};

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create table RectangleTable as (id string primary key, px double, py double, pw double, ph double);\n" +
                "create index MyIndex on RectangleTable((px,py,pw,ph) mxcifquadtree(0,0,100,100));\n" +
                "insert into RectangleTable select id, x as px, y as py, width as pw, height as ph from SupportSpatialEventRectangle;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from RectangleTable as pt where rectangle(px,py,pw,ph).intersects(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl, path).addListener("out");

            Random random = new Random();
            List<SupportSpatialEventRectangle> rectangles = generateCoordinates(random, NUM_RECTANGLES, WIDTH, HEIGHT);
            for (SupportSpatialEventRectangle r : rectangles) {
                sendEventRectangle(env, r.getId(), r.getX(), r.getY(), r.getWidth(), r.getHeight());
            }
            env.milestone(0);

            AtomicInteger milestone = new AtomicInteger();
            EPCompiled deleteQuery = env.compileFAF("delete from RectangleTable where id=?:id:string", path);
            EPFireAndForgetPreparedQueryParameterized preparedDelete = env.runtime().getFireAndForgetService().prepareQueryWithParameters(deleteQuery);

            for (int i = 0; i < NUM_MOVES; i++) {
                SupportSpatialEventRectangle pointMoved = rectangles.get(random.nextInt(rectangles.size()));
                movePoint(env, pointMoved, random, preparedDelete);

                double startX = pointMoved.getX() - 5;
                double startY = pointMoved.getY() - 5;
                BoundingBox bb = new BoundingBox(startX, startY, startX + 10, startY + 10);
                assertBBRectangles(env, bb, rectangles);

                if (Arrays.binarySearch(CHECKPOINT_AT, i) >= 0) {
                    log.info("Checkpoint at " + rectangles.size());
                    env.milestoneInc(milestone);
                    preparedDelete = env.runtime().getFireAndForgetService().prepareQueryWithParameters(deleteQuery);
                }
            }

            env.undeployAll();
        }

        private void movePoint(RegressionEnvironment env, SupportSpatialEventRectangle rectangle, Random random, EPFireAndForgetPreparedQueryParameterized preparedDelete) {
            double newX;
            double newY;

            while (true) {
                newX = rectangle.getX();
                newY = rectangle.getY();
                int direction = random.nextInt(4);
                if (direction == 0 && newX > 0) {
                    newX--;
                }
                if (direction == 1 && newY > 0) {
                    newY--;
                }
                if (direction == 2 && newX < (WIDTH - 1)) {
                    newX++;
                }
                if (direction == 3 && newY < (HEIGHT - 1)) {
                    newY++;
                }

                if (BoundingBox.intersectsBoxIncludingEnd(0, 0, WIDTH, HEIGHT, newX, newY, rectangle.getWidth(), rectangle.getHeight())) {
                    break;
                }
            }

            // Comment-me-in:
            // log.info("Moving " + rectangle.getId() + " from " + printPoint(rectangle.getX(), rectangle.getY()) + " to " + printPoint(newX, newY));
            preparedDelete.setObject("id", rectangle.getId());
            env.runtime().getFireAndForgetService().executeQuery(preparedDelete);

            rectangle.setX(newX);
            rectangle.setY(newY);
            sendEventRectangle(env, rectangle.getId(), rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
        }

        private List<SupportSpatialEventRectangle> generateCoordinates(Random random, int numPoints, int width, int height) {
            List<SupportSpatialEventRectangle> result = new ArrayList<>(numPoints);
            for (int i = 0; i < numPoints; i++) {
                int x = random.nextInt(width);
                int y = random.nextInt(height);
                double w = random.nextDouble() * width;
                double h = random.nextDouble() * height;
                result.add(new SupportSpatialEventRectangle("P" + i, (double) x, (double) y, w, h));
            }
            return result;
        }
    }

    public static class EPLSpatialMXCIFEventIndexRandomIntPointsInSquareUnique implements RegressionExecution {
        private final static int SIZE = 1000;

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String epl = "create window RectangleWindow#keepall as (id string, px double, py double, pw double, ph double);\n" +
                "create unique index MyIndex on RectangleWindow((px,py,pw,ph) mxcifquadtree(0,0,1000,1000));\n" +
                "insert into RectangleWindow select id, x as px, y as py, width as pw, height as ph from SupportSpatialEventRectangle;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from RectangleWindow as pt where rectangle(px,py,pw,ph).intersects(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl, path).addListener("out");

            Random random = new Random();
            Collection<SupportSpatialEventRectangle> rectangles = generateCoordinates(random);
            for (SupportSpatialEventRectangle rectangle : rectangles) {
                sendEventRectangle(env, rectangle.getId(), rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
            }

            env.milestone(0);

            // find all individually
            SupportListener listener = env.listener("out");
            for (SupportSpatialEventRectangle r : rectangles) {
                env.sendEventBean(new SupportSpatialAABB("", r.getX(), r.getY(), 0.1, 0.1));
                assertEquals(r.getId(), listener.assertOneGetNewAndReset().get("c0"));
            }

            // get all content
            assertAllRectangles(env, rectangles, 0, 0, SIZE, SIZE);

            env.milestone(1);

            // add duplicate: note these events are still is named window
            for (SupportSpatialEventRectangle r : rectangles) {
                try {
                    sendEventRectangle(env, r.getId(), r.getX(), r.getY(), r.getWidth(), r.getHeight());
                    fail();
                } catch (Throwable t) {
                    // expected
                }
            }

            // remove all
            List<String> idList = new ArrayList<>();
            for (SupportSpatialEventRectangle p : rectangles) {
                idList.add(p.getId());
            }
            while (!idList.isEmpty()) {
                List<String> first = idList.size() > 100 ? idList.subList(0, 100) : idList;
                env.compileExecuteFAF(SupportSpatialUtil.buildDeleteQueryWithInClause("RectangleWindow", "id", first), path);
                idList.removeAll(first);
            }

            env.sendEventBean(new SupportSpatialAABB("", 0, 0, SIZE, SIZE));
            assertFalse(env.listener("out").getAndClearIsInvoked());

            env.milestone(2);

            env.sendEventBean(new SupportSpatialAABB("", 0, 0, SIZE, SIZE));
            assertFalse(env.listener("out").getAndClearIsInvoked());

            env.undeployAll();
        }

        private static Collection<SupportSpatialEventRectangle> generateCoordinates(Random random) {
            Map<UniformPair<Integer>, SupportSpatialEventRectangle> points = new HashMap<>();
            while (points.size() < SIZE) {
                int x = random.nextInt(SIZE);
                int y = random.nextInt(SIZE);
                points.put(new UniformPair<>(x, y), new SupportSpatialEventRectangle(Integer.toString(x) + "_" + Integer.toString(y), (double) x, (double) y, 0.001d, 0.001d));
            }
            return points.values();
        }
    }

    private static void sendEventRectangle(RegressionEnvironment env, String id, double x, double y, double width, double height) {
        env.sendEventBean(new SupportSpatialEventRectangle(id, x, y, width, height));
    }

    private static SupportSpatialAABB rect(double x, double y, double width, double height) {
        return new SupportSpatialAABB(null, x, y, width, height);
    }

    private static void sendAssert(RegressionEnvironment env, SupportSpatialAABB one, SupportSpatialAABB two, boolean expected) {
        BoundingBox bbOne = BoundingBox.from(one.getX(), one.getY(), one.getWidth(), one.getHeight());
        assertEquals(expected, bbOne.intersectsBoxIncludingEnd(two.getX(), two.getY(), two.getWidth(), two.getHeight()));

        BoundingBox bbTwo = BoundingBox.from(two.getX(), two.getY(), two.getWidth(), two.getHeight());
        assertEquals(expected, bbTwo.intersectsBoxIncludingEnd(one.getX(), one.getY(), one.getWidth(), one.getHeight()));

        env.sendEventBean(new SupportSpatialDualAABB(one, two));
        assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));

        env.sendEventBean(new SupportSpatialDualAABB(two, one));
        assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
    }

    private static void sendSpatialEventRectanges(RegressionEnvironment env, int numX, int numY) {
        for (int x = 0; x < numX; x++) {
            for (int y = 0; y < numY; y++) {
                env.sendEventBean(new SupportSpatialEventRectangle(Integer.toString(x) + "_" + Integer.toString(y), (double) x, (double) y, 0.1, 0.2));
            }
        }
    }
}
