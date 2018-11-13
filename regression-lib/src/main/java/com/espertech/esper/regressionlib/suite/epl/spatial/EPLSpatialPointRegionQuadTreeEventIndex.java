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
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQueryParameterized;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexDescOnExpr;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexDescSubquery;
import com.espertech.esper.common.internal.epl.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.common.internal.epl.spatial.quadtree.core.QuadrantEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportEventRectangleWithOffset;
import com.espertech.esper.regressionlib.support.bean.SupportSpatialAABB;
import com.espertech.esper.regressionlib.support.bean.SupportSpatialDualPoint;
import com.espertech.esper.regressionlib.support.bean.SupportSpatialPoint;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;
import com.espertech.esper.regressionlib.support.util.SupportSpatialUtil;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.support.util.SupportSpatialUtil.*;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EPLSpatialPointRegionQuadTreeEventIndex {
    private final static List<BoundingBox> BOXES = Arrays.asList(
        new BoundingBox(0, 0, 50, 50),
        new BoundingBox(50, 0, 100, 50),
        new BoundingBox(0, 50, 50, 100),
        new BoundingBox(50, 50, 100, 100),
        new BoundingBox(25, 25, 75, 75)
    );

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSpatialPREventIndexUnindexed());
        execs.add(new EPLSpatialPREventIndexUnusedOnTrigger());
        execs.add(new EPLSpatialPREventIndexUnusedNamedWindowFireAndForget());
        execs.add(new EPLSpatialPREventIndexOnTriggerNWInsertRemove(false));
        execs.add(new EPLSpatialPREventIndexOnTriggerNWInsertRemove(true));
        execs.add(new EPLSpatialPREventIndexOnTriggerTable());
        execs.add(new EPLSpatialPREventIndexChoiceOfTwo());
        execs.add(new EPLSpatialPREventIndexUnique());
        execs.add(new EPLSpatialPREventIndexPerformance());
        execs.add(new EPLSpatialPREventIndexChoiceBetweenIndexTypes());
        execs.add(new EPLSpatialPREventIndexNWFireAndForgetPerformance());
        execs.add(new EPLSpatialPREventIndexTableFireAndForget());
        execs.add(new EPLSpatialPREventIndexOnTriggerContextParameterized());
        execs.add(new EPLSpatialPREventIndexExpression());
        execs.add(new EPLSpatialPREventIndexEdgeSubdivide());
        execs.add(new EPLSpatialPREventIndexRandomDoublePointsWRandomQuery());
        execs.add(new EPLSpatialPREventIndexRandomIntPointsInSquareUnique());
        execs.add(new EPLSpatialPREventIndexRandomMovingPoints());
        execs.add(new EPLSpatialPREventIndexTableSimple());
        execs.add(new EPLSpatialPREventIndexTableSubdivideDeepAddDestroy());
        execs.add(new EPLSpatialPREventIndexTableSubdivideDestroy());
        execs.add(new EPLSpatialPREventIndexTableSubdivideMergeDestroy());
        execs.add(new EPLSpatialPREventIndexSubqNamedWindowIndexShare());
        return execs;
    }

    private static class EPLSpatialPREventIndexNWFireAndForgetPerformance implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window MyPointWindow#keepall as (id string, px double, py double);\n" +
                "insert into MyPointWindow select id, px, py from SupportSpatialPoint;\n" +
                "create index Idx on MyPointWindow( (px, py) pointregionquadtree(0, 0, 100, 100));\n";
            env.compileDeploy(epl, path);

            Random random = new Random();
            List<SupportSpatialPoint> points = new ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                double px = random.nextDouble() * 100;
                double py = random.nextDouble() * 100;
                SupportSpatialPoint point = new SupportSpatialPoint("P" + Integer.toString(i), px, py);
                env.sendEventBean(point);
                points.add(point);
                // Comment-me-in: log.info("Point P" + i + " " + px + " " + py);
            }

            EPCompiled compiled = env.compileFAF("select * from MyPointWindow where point(px,py).inside(rectangle(?::double,?::double,?::double,?::double))", path);
            EPFireAndForgetPreparedQueryParameterized prepared = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);
            long start = System.currentTimeMillis();
            String[] fields = "id".split(",");
            for (int i = 0; i < 500; i++) {
                double x = random.nextDouble() * 100;
                double y = random.nextDouble() * 100;
                // Comment-me-in: log.info("Query " + x + " " + y + " " + width + " " + height);

                prepared.setObject(1, x);
                prepared.setObject(2, y);
                prepared.setObject(3, 5d);
                prepared.setObject(4, 5d);
                EventBean[] events = env.runtime().getFireAndForgetService().executeQuery(prepared).getArray();
                Object[][] expected = SupportSpatialUtil.getExpected(points, x, y, 5, 5);
                EPAssertionUtil.assertPropsPerRowAnyOrder(events, fields, expected);
            }
            long delta = System.currentTimeMillis() - start;
            assertTrue("delta=" + delta, delta < 1000);

            env.undeployAll();
        }
    }

    private static class EPLSpatialPREventIndexChoiceBetweenIndexTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "@Name('win') create window MyPointWindow#keepall as (id string, category string, px double, py double);\n" +
                "@Name('insert') insert into MyPointWindow select id, category, px, py from SupportSpatialPoint;\n" +
                "@Name('idx1') create index IdxHash on MyPointWindow(category);\n" +
                "@Name('idx2') create index IdxQuadtree on MyPointWindow((px, py) pointregionquadtree(0, 0, 100, 100));\n";
            env.compileDeploy(epl, path);

            sendPoint(env, "P1", 10, 15, "X");
            sendPoint(env, "P2", 10, 15, "Y");
            sendPoint(env, "P3", 10, 15, "Z");

            assertIndexChoice(env, path, "", "IdxQuadtree");
            assertIndexChoice(env, path, "@Hint('index(IdxHash, bust)')", "IdxQuadtree");

            env.undeployAll();
        }
    }

    private static class EPLSpatialPREventIndexUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@Name('win') create window MyPointWindow#keepall as (id string, px double, py double);\n" +
                "@Name('insert') insert into MyPointWindow select id, px, py from SupportSpatialPoint;\n" +
                "@Name('idx') create unique index Idx on MyPointWindow( (px, py) pointregionquadtree(0, 0, 100, 100));\n" +
                "@Name('out') on SupportSpatialAABB select mpw.id as c0 from MyPointWindow as mpw where point(px, py).inside(rectangle(x, y, width, height));\n";
            env.compileDeploy(epl).addListener("out");

            sendPoint(env, "P1", 10, 15);
            try {
                sendPoint(env, "P2", 10, 15);
                fail();
            } catch (RuntimeException ex) { // we have a handler
                SupportMessageAssertUtil.assertMessage(ex,
                    "Unexpected exception in statement 'win': Unique index violation, index 'Idx' is a unique index and key '(10.0,15.0)' already exists");
            }

            env.undeployAll();
        }
    }

    private static class EPLSpatialPREventIndexPerformance implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String epl = "create window MyPointWindow#keepall as (id string, px double, py double);\n" +
                "insert into MyPointWindow select id, px, py from SupportSpatialPoint;\n" +
                "create index Idx on MyPointWindow( (px, py) pointregionquadtree(0, 0, 100, 100));\n" +
                "@Name('s0') on SupportSpatialAABB select mpw.id as c0 from MyPointWindow as mpw where point(px, py).inside(rectangle(x, y, width, height));\n";
            env.compileDeploy(epl).addListener("s0");

            for (int x = 0; x < 100; x++) {
                for (int y = 0; y < 100; y++) {
                    env.sendEventBean(new SupportSpatialPoint(Integer.toString(x) + "_" + Integer.toString(y), (double) x, (double) y));
                }
            }

            long start = System.currentTimeMillis();
            for (int x = 0; x < 100; x++) {
                for (int y = 0; y < 100; y++) {
                    env.sendEventBean(new SupportSpatialAABB("R", x, y, 0.5, 0.5));
                    assertEquals(Integer.toString(x) + "_" + Integer.toString(y), env.listener("s0").assertOneGetNewAndReset().get("c0"));
                }
            }
            long delta = System.currentTimeMillis() - start;
            assertTrue("delta=" + delta, delta < 1000);

            env.undeployAll();
        }
    }

    private static class EPLSpatialPREventIndexUnusedNamedWindowFireAndForget implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window PointWindow#keepall as (id string, px double, py double);\n" +
                "create index MyIndex on PointWindow((px,py) pointregionquadtree(0,0,100,100,2,12));\n" +
                "insert into PointWindow select id, px, py from SupportSpatialPoint;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from PointWindow as pt where point(px,py).inside(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl, path);

            env.compileExecuteFAF("delete from PointWindow where id='P1'", path);

            env.undeployAll();
        }
    }

    private static class EPLSpatialPREventIndexTableFireAndForget implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table MyTable(id string primary key, tx double, ty double)", path);
            env.compileDeploy("insert into MyTable select id, px as tx, py as ty from SupportSpatialPoint", path);
            env.sendEventBean(new SupportSpatialPoint("P1", 50d, 50d));
            env.sendEventBean(new SupportSpatialPoint("P2", 49d, 49d));
            env.compileDeploy("create index MyIdxWithExpr on MyTable( (tx, ty) pointregionquadtree(0, 0, 100, 100))", path);

            EPFireAndForgetQueryResult result = env.compileExecuteFAF(IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select id as c0 from MyTable where point(tx, ty).inside(rectangle(45, 45, 10, 10))", path);
            SupportQueryPlanIndexHook.assertFAFAndReset("MyIdxWithExpr", "EventTableQuadTreePointRegion");
            EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), "c0".split(","), new Object[][]{{"P1"}, {"P2"}});

            env.undeployAll();
        }
    }

    private static class EPLSpatialPREventIndexExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table MyTable(id string primary key, tx double, ty double)", path);
            env.compileExecuteFAF("insert into MyTable values ('P1', 50, 30)", path);
            env.compileExecuteFAF("insert into MyTable values ('P2', 50, 28)", path);
            env.compileExecuteFAF("insert into MyTable values ('P3', 50, 30)", path);
            env.compileExecuteFAF("insert into MyTable values ('P4', 49, 29)", path);
            env.compileDeploy("create index MyIdxWithExpr on MyTable((tx*10, ty*10) pointregionquadtree(0, 0, 1000, 1000))", path);

            String eplOne = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportSpatialAABB select tbl.id as c0 from MyTable as tbl where point(tx, ty).inside(rectangle(x, y, width, height))";
            env.compileDeploy(eplOne, path).addListener("s0");
            SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIdxWithExpr", "non-unique hash={} btree={} advanced={pointregionquadtree(tx*10,ty*10)}");
            // Invalid use of index, the properties match and the bounding boxes do not match due to "x*10" missing.
            env.undeployModuleContaining("s0");

            String eplTwo = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportSpatialAABB select tbl.id as c0 from MyTable as tbl where point(tx*10, tbl.ty*10).inside(rectangle(x, y, width, height))";
            env.compileDeploy(eplTwo, path).addListener("s0");
            SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIdxWithExpr", "non-unique hash={} btree={} advanced={pointregionquadtree(tx*10,ty*10)}");
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, null, null, null, null, null);
            assertRectanglesManyRow(env, env.listener("s0"), Collections.singletonList(new BoundingBox(500, 300, 501, 301)), "P1,P3");

            env.undeployAll();
        }
    }

    private static class EPLSpatialPREventIndexChoiceOfTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "create table MyPointTable(" +
                    " id string primary key," +
                    " x1 double, y1 double, \n" +
                    " x2 double, y2 double);\n" +
                    "create index Idx1 on MyPointTable( (x1, y1) pointregionquadtree(0, 0, 100, 100));\n" +
                    "create index Idx2 on MyPointTable( (x2, y2) pointregionquadtree(0, 0, 100, 100));\n" +
                    "on SupportSpatialDualPoint dp merge MyPointTable t where dp.id = t.id when not matched then insert select dp.id as id,x1,y1,x2,y2;\n";
            env.compileDeploy(epl, path);

            SupportUpdateListener listener = new SupportUpdateListener();
            String textOne = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportSpatialAABB select tbl.id as c0 from MyPointTable as tbl where point(x1, y1).inside(rectangle(x, y, width, height))";
            env.compileDeploy(textOne, path).statement("s0").addListener(listener);

            SupportQueryPlanIndexHook.assertOnExprTableAndReset("Idx1", "non-unique hash={} btree={} advanced={pointregionquadtree(x1,y1)}");

            String textTwo = "@name('s1') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportSpatialAABB select tbl.id as c0 from MyPointTable as tbl where point(tbl.x2, y2).inside(rectangle(x, y, width, height))";
            env.compileDeploy(textTwo, path).statement("s1").addListener(listener);
            SupportQueryPlanIndexHook.assertOnExprTableAndReset("Idx2", "non-unique hash={} btree={} advanced={pointregionquadtree(x2,y2)}");

            env.sendEventBean(new SupportSpatialDualPoint("P1", 10, 10, 60, 60));
            env.sendEventBean(new SupportSpatialDualPoint("P2", 55, 20, 4, 88));

            assertRectanglesSingleValue(env, listener, BOXES, "P1", "P2", "P2", "P1", "P1");

            env.undeployAll();
        }
    }

    private static class EPLSpatialPREventIndexSubqNamedWindowIndexShare implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@Hint('enable_window_subquery_indexshare') create window MyWindow#length(5) as select * from SupportSpatialPoint;\n" +
                "create index MyIndex on MyWindow((px,py) pointregionquadtree(0,0,100,100));\n" +
                "insert into MyWindow select * from SupportSpatialPoint;\n" +
                IndexBackingTableInfo.INDEX_CALLBACK_HOOK +
                "@name('out') select (select id from MyWindow as mw where point(mw.px,mw.py).inside(rectangle(aabb.x,aabb.y,aabb.width,aabb.height))).aggregate('', \n" +
                "  (result, item) => result || (case when result='' then '' else ',' end) || item) as c0 from SupportSpatialAABB aabb";
            env.compileDeploy(epl).addListener("out");

            QueryPlanIndexDescSubquery subquery = SupportQueryPlanIndexHook.assertSubqueryAndReset();
            assertEquals("non-unique hash={} btree={} advanced={pointregionquadtree(px,py)}", subquery.getTables()[0].getIndexDesc());
            assertEquals("MyIndex", subquery.getTables()[0].getIndexName());

            sendPoint(env, "P1", 10, 40);
            assertRectanglesSingleValue(env, env.listener("out"), BOXES, "P1", "", "", "", "");

            env.undeployAll();
        }
    }

    private static class EPLSpatialPREventIndexUnusedOnTrigger implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window MyWindow#length(5) as select * from SupportSpatialPoint;\n" +
                "create index MyIndex on MyWindow((px,py) pointregionquadtree(0,0,100,100));\n" +
                "insert into MyWindow select * from SupportSpatialPoint;\n";
            env.compileDeploy(epl, path);

            sendPoint(env, "P1", 5, 5);
            sendPoint(env, "P2", 55, 60);

            runIndexUnusedConstantsOnly(env, path);
            runIndexUnusedPointValueDepends(env, path);
            runIndexUnusedRectValueDepends(env, path);

            env.undeployAll();
        }

        private void runIndexUnusedRectValueDepends(RegressionEnvironment env, RegressionPath path) {
            String epl = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportSpatialAABB as aabb select points.id as c0 " +
                "from MyWindow as points where point(px, py).inside(rectangle(px,py,1,1))";
            env.compileDeploy(epl, path).addListener("s0");

            SupportQueryPlanIndexHook.assertOnExprTableAndReset(null, null);

            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P1,P2", "P1,P2", "P1,P2", "P1,P2", "P1,P2");

            env.undeployModuleContaining("s0");
        }

        private void runIndexUnusedPointValueDepends(RegressionEnvironment env, RegressionPath path) {
            String epl = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "on SupportSpatialAABB as aabb select points.id as c0 " +
                "from MyWindow as points where point(px + x, py + y).inside(rectangle(x,y,width,height))";
            env.compileDeploy(epl, path).addListener("s0");

            SupportQueryPlanIndexHook.assertOnExprTableAndReset(null, null);

            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P1", "P1", "P1", "P1", "P1");

            env.undeployModuleContaining("s0");
        }

        private void runIndexUnusedConstantsOnly(RegressionEnvironment env, RegressionPath path) {
            String epl = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@name('out') on SupportSpatialAABB as aabb select points.id as c0 " +
                "from MyWindow as points where point(0, 0).inside(rectangle(x,y,width,height))";
            env.compileDeploy(epl, path).addListener("s0");

            SupportQueryPlanIndexHook.assertOnExprTableAndReset(null, null);

            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P1,P2", null, null, null, null);

            env.undeployModuleContaining("s0");
        }
    }

    private static class EPLSpatialPREventIndexUnindexed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select point(xOffset, yOffset).inside(rectangle(x, y, width, height)) as c0 from SupportEventRectangleWithOffset");
            env.addListener("s0");

            sendAssert(env, env.listener("s0"), 1, 1, 0, 0, 2, 2, true);
            sendAssert(env, env.listener("s0"), 3, 1, 0, 0, 2, 2, false);
            sendAssert(env, env.listener("s0"), 2, 1, 0, 0, 2, 2, false);

            env.milestone(0);

            sendAssert(env, env.listener("s0"), 1, 3, 0, 0, 2, 2, false);
            sendAssert(env, env.listener("s0"), 1, 2, 0, 0, 2, 2, false);
            sendAssert(env, env.listener("s0"), 0, 0, 1, 1, 2, 2, false);
            sendAssert(env, env.listener("s0"), 1, 0, 1, 1, 2, 2, false);
            sendAssert(env, env.listener("s0"), 0, 1, 1, 1, 2, 2, false);
            sendAssert(env, env.listener("s0"), 1, 1, 1, 1, 2, 2, true);
            sendAssert(env, env.listener("s0"), 2.9999, 2.9999, 1, 1, 2, 2, true);

            env.milestone(1);

            sendAssert(env, env.listener("s0"), 3, 2.9999, 1, 1, 2, 2, false);
            sendAssert(env, env.listener("s0"), 2.9999, 3, 1, 1, 2, 2, false);
            sendAssertWNull(env, env.listener("s0"), null, 0d, 0d, 0d, 0d, 0d, null);
            sendAssertWNull(env, env.listener("s0"), 0d, 0d, 0d, null, 0d, 0d, null);

            env.undeployAll();
        }
    }

    private static class EPLSpatialPREventIndexOnTriggerContextParameterized implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context CtxBox initiated by SupportEventRectangleWithOffset box;\n" +
                "context CtxBox create window MyWindow#keepall as SupportSpatialPoint;\n" +
                "context CtxBox create index MyIndex on MyWindow((px+context.box.xOffset, py+context.box.yOffset) pointregionquadtree(context.box.x, context.box.y, context.box.width, context.box.height));\n" +
                "context CtxBox on SupportSpatialPoint(category = context.box.id) merge MyWindow when not matched then insert select *;\n" +
                "@name('s0') context CtxBox on SupportSpatialAABB(category = context.box.id) aabb " +
                "  select points.id as c0 from MyWindow points where point(px, py).inside(rectangle(x, y, width, height))";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportEventRectangleWithOffset("NW", 0d, 0d, 0d, 0d, 50d, 50d));
            env.sendEventBean(new SupportEventRectangleWithOffset("SE", 0d, 0d, 50d, 50d, 50d, 50d));
            sendPoint(env, "P1", 60, 90, "SE");
            sendPoint(env, "P2", 5, 20, "NW");

            env.sendEventBean(new SupportSpatialAABB("R1", 60, 60, 40, 40, "SE"));
            assertEquals("P1", env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.sendEventBean(new SupportSpatialAABB("R2", 0, 0, 5.0001, 20.0001, "NW"));
            assertEquals("P2", env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.milestone(0);

            env.sendEventBean(new SupportSpatialAABB("R3", 0, 0, 5, 30, "NW"));
            env.sendEventBean(new SupportSpatialAABB("R3", 0, 0, 30, 20, "NW"));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLSpatialPREventIndexOnTriggerNWInsertRemove implements RegressionExecution {
        private final boolean soda;

        public EPLSpatialPREventIndexOnTriggerNWInsertRemove(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy(soda, "create window MyWindow#length(5) as select * from SupportSpatialPoint", path);
            env.compileDeploy(soda, "create index MyIndex on MyWindow((px,py) pointregionquadtree(0,0,100,100))", path);
            env.compileDeploy(soda, "insert into MyWindow select * from SupportSpatialPoint", path);

            String epl = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + " on SupportSpatialAABB as aabb " +
                "select points.id as c0 from MyWindow as points where point(px,py).inside(rectangle(x,y,width,height))";
            env.compileDeploy(soda, epl, path).addListener("s0");

            SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIndex", "non-unique hash={} btree={} advanced={pointregionquadtree(px,py)}");

            sendPoint(env, "P1", 10, 40);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P1", null, null, null, null);

            env.milestone(0);

            sendPoint(env, "P2", 80, 80);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P1", null, null, "P2", null);

            sendPoint(env, "P3", 10, 40);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P1,P3", null, null, "P2", null);

            env.milestone(1);

            sendPoint(env, "P4", 60, 40);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P1,P3", "P4", null, "P2", "P4");

            sendPoint(env, "P5", 20, 75);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P1,P3", "P4", "P5", "P2", "P4");

            sendPoint(env, "P6", 50, 50);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P3", "P4", "P5", "P2,P6", "P4,P6");

            env.milestone(2);

            sendPoint(env, "P7", 0, 0);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P3,P7", "P4", "P5", "P6", "P4,P6");

            sendPoint(env, "P8", 99.999, 0);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P7", "P4,P8", "P5", "P6", "P4,P6");

            env.milestone(3);

            sendPoint(env, "P9", 0, 99.999);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P7", "P8", "P5,P9", "P6", "P6");

            sendPoint(env, "P10", 99.999, 99.999);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P7", "P8", "P9", "P6,P10", "P6");

            sendPoint(env, "P11", 0, 0);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P7,P11", "P8", "P9", "P10", null);

            env.undeployAll();
        }
    }

    private static class EPLSpatialPREventIndexOnTriggerTable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "create table MyPointTable(my_x double primary key, my_y double primary key, my_id string);\n" +
                    "@Audit create index MyIndex on MyPointTable( (my_x, my_y) pointregionquadtree(0, 0, 100, 100));\n" +
                    "on SupportSpatialPoint ssp merge MyPointTable where ssp.px = my_x and ssp.py = my_y when not matched then insert select px as my_x, py as my_y, id as my_id;\n" +
                    IndexBackingTableInfo.INDEX_CALLBACK_HOOK +
                    "@Audit @name('s0') on SupportSpatialAABB select my_id as c0 from MyPointTable as c0 where point(my_x, my_y).inside(rectangle(x, y, width, height))";
            env.compileDeploy(epl, path).addListener("s0");

            SupportQueryPlanIndexHook.assertOnExprTableAndReset("MyIndex", "non-unique hash={} btree={} advanced={pointregionquadtree(my_x,my_y)}");

            sendPoint(env, "P1", 55, 45);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, null, "P1", null, null, "P1");

            sendPoint(env, "P2", 45, 45);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P2", "P1", null, null, "P1,P2");

            env.milestone(0);

            sendPoint(env, "P3", 55, 55);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P2", "P1", null, "P3", "P1,P2,P3");

            env.compileExecuteFAF("delete from MyPointTable where my_x = 55 and my_y = 45", path);
            sendPoint(env, "P4", 45, 55);
            assertRectanglesManyRow(env, env.listener("s0"), BOXES, "P2", null, "P4", "P3", "P2,P3,P4");

            env.undeployAll();
        }
    }

    public static class EPLSpatialPREventIndexEdgeSubdivide implements RegressionExecution {

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window PointWindow#keepall as (id string, px double, py double);\n" +
                "create index MyIndex on PointWindow((px,py) pointregionquadtree(0,0,100,100,2,5));\n" +
                "insert into PointWindow select id, px, py from SupportSpatialPoint;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from PointWindow as pt where point(px,py).inside(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl, path).addListener("out");

            Set<BoundingBox> boxesLevel4 = getLevel5Boxes();
            int count = 0;
            List<SupportSpatialPoint> points = new ArrayList<>();
            for (BoundingBox bb : boxesLevel4) {
                sendAddPoint(env, points, "A" + count, bb.getMinX(), bb.getMinY());
                sendAddPoint(env, points, "B" + count, bb.getMinX(), bb.getMinY());
                sendAddPoint(env, points, "C" + count, bb.getMinX(), bb.getMinY());
                count++;
            }
            assertAllPoints(env, points, 0, 0, 100, 100);

            env.milestone(0);

            removeAllABPoints(env, path, points);
            assertAllPoints(env, points, 0, 0, 100, 100);

            env.milestone(1);

            assertAllPoints(env, points, 0, 0, 100, 100);
            removeEverySecondPoints(env, path, points);
            assertAllPoints(env, points, 0, 0, 100, 100);

            env.milestone(2);

            assertAllPoints(env, points, 0, 0, 100, 100);
            removeEverySecondPoints(env, path, points);
            assertAllPoints(env, points, 0, 0, 100, 100);

            env.milestone(3);

            assertAllPoints(env, points, 0, 0, 100, 100);
            removeAllPoints(env, path, points);
            assertAllPoints(env, points, 0, 0, 100, 100);

            env.milestone(4);

            removeAllPoints(env, path, points);

            env.undeployAll();
        }

        private void removeEverySecondPoints(RegressionEnvironment env, RegressionPath path, List<SupportSpatialPoint> points) {
            Iterator<SupportSpatialPoint> it = points.iterator();
            int count = 0;
            List<String> ids = new ArrayList<>();
            for (; it.hasNext(); ) {
                SupportSpatialPoint p = it.next();
                if (count % 2 == 1) {
                    it.remove();
                    ids.add(p.getId());
                }
                count++;
            }
            String query = SupportSpatialUtil.buildDeleteQueryWithInClause("PointWindow", "id", ids);
            env.compileExecuteFAF(query, path);
        }

        private void removeAllPoints(RegressionEnvironment env, RegressionPath path, List<SupportSpatialPoint> points) {
            Iterator<SupportSpatialPoint> it = points.iterator();
            List<String> ids = new ArrayList<>();
            for (; it.hasNext(); ) {
                SupportSpatialPoint p = it.next();
                it.remove();
                ids.add(p.getId());
            }
            if (ids.isEmpty()) {
                return;
            }
            String query = SupportSpatialUtil.buildDeleteQueryWithInClause("PointWindow", "id", ids);
            env.compileExecuteFAF(query, path);
        }

        private void removeAllABPoints(RegressionEnvironment env, RegressionPath path, List<SupportSpatialPoint> points) {
            Iterator<SupportSpatialPoint> it = points.iterator();
            List<String> ids = new ArrayList<>();
            for (; it.hasNext(); ) {
                SupportSpatialPoint p = it.next();
                if (p.getId().charAt(0) == 'A' || p.getId().charAt(0) == 'B') {
                    it.remove();
                    ids.add(p.getId());
                }
            }
            String query = SupportSpatialUtil.buildDeleteQueryWithInClause("PointWindow", "id", ids);
            env.compileExecuteFAF(query, path);
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

    public static class EPLSpatialPREventIndexRandomDoublePointsWRandomQuery implements RegressionExecution {
        private static final Logger log = LoggerFactory.getLogger(EPLSpatialPREventIndexRandomDoublePointsWRandomQuery.class);

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
            String epl = "create window PointWindow#keepall as (id string, px double, py double);\n" +
                "create index MyIndex on PointWindow((px,py) pointregionquadtree(0,0,100,100));\n" +
                "insert into PointWindow select id, px, py from SupportSpatialPoint;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from PointWindow as pt where point(px,py).inside(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl, path).addListener("out");

            Random random = new Random();
            List<SupportSpatialPoint> points = randomPoints(random, NUM_POINTS, X, Y, WIDTH, HEIGHT);
            for (SupportSpatialPoint point : points) {
                sendPoint(env, point.getId(), point.getPx(), point.getPy());
                // Comment-me-in: log.info("Point: " + point);
            }

            env.milestone(0);

            for (int i = 0; i < NUM_QUERIES_AFTER_LOAD; i++) {
                randomQuery(env, random, points);
            }

            AtomicInteger milestone = new AtomicInteger();
            EPCompiled deleteQuery = env.compileFAF("delete from PointWindow where id=?::string", path);
            EPFireAndForgetPreparedQueryParameterized preparedDelete = env.runtime().getFireAndForgetService().prepareQueryWithParameters(deleteQuery);

            while (!points.isEmpty()) {
                SupportSpatialPoint removed = randomRemove(random, points);
                preparedDelete.setObject(1, removed.getId());
                env.runtime().getFireAndForgetService().executeQuery(preparedDelete);

                for (int i = 0; i < NUM_QUERIES_AFTER_EACH_REMOVE; i++) {
                    randomQuery(env, random, points);
                }

                if (Arrays.binarySearch(CHECKPOINT_REMAINING, points.size()) >= 0) {
                    log.info("Checkpoint at " + points.size());
                    env.milestoneInc(milestone);
                    preparedDelete = env.runtime().getFireAndForgetService().prepareQueryWithParameters(deleteQuery);
                }
            }

            env.undeployAll();
        }

        private SupportSpatialPoint randomRemove(Random random, List<SupportSpatialPoint> points) {
            int index = random.nextInt(points.size());
            return points.remove(index);
        }

        private void randomQuery(RegressionEnvironment env, Random random, List<SupportSpatialPoint> points) {
            double bbWidth = random.nextDouble() * WIDTH * 1.5;
            double bbHeight = random.nextDouble() * HEIGHT * 1.5;
            double bbMinX = random.nextDouble() * WIDTH + X * 0.8;
            double bbMinY = random.nextDouble() * HEIGHT + Y * 0.8;
            double bbMaxX = bbMinX + bbWidth;
            double bbMaxY = bbMinY + bbHeight;
            BoundingBox boundingBox = new BoundingBox(bbMinX, bbMinY, bbMaxX, bbMaxY);
            // Comment-me-in: log.info("Query: " + boundingBox);
            assertBBPoints(env, boundingBox, points);
        }
    }

    public static class EPLSpatialPREventIndexRandomIntPointsInSquareUnique implements RegressionExecution {
        private final static int SIZE = 1000;

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window PointWindow#keepall as (id string, px double, py double);\n" +
                "create unique index MyIndex on PointWindow((px,py) pointregionquadtree(0,0,1000,1000));\n" +
                "insert into PointWindow select id, px, py from SupportSpatialPoint;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from PointWindow as pt where point(px,py).inside(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl, path).addListener("out");

            Random random = new Random();
            Collection<SupportSpatialPoint> points = generateCoordinates(random);
            for (SupportSpatialPoint point : points) {
                sendPoint(env, point.getId(), point.getPx(), point.getPy());
            }

            env.milestone(0);

            // find all individually
            SupportListener listener = env.listener("out");
            for (SupportSpatialPoint p : points) {
                env.sendEventBean(new SupportSpatialAABB("", p.getPx(), p.getPy(), 1, 1));
                assertEquals(p.getId(), listener.assertOneGetNewAndReset().get("c0"));
            }

            // get all content
            assertAllPoints(env, points, 0, 0, SIZE, SIZE);

            env.milestone(1);

            // add duplicate: note these events are still is named window
            for (SupportSpatialPoint p : points) {
                try {
                    sendPoint(env, p.getId(), p.getPx(), p.getPy());
                    fail();
                } catch (Throwable t) {
                    // expected
                }
            }

            // remove all
            List<String> ids = new ArrayList<>();
            for (SupportSpatialPoint p : points) {
                ids.add(p.getId());
            }
            while (!ids.isEmpty()) {
                List<String> first = ids.size() < 100 ? ids : ids.subList(0, 99);
                String deleteQuery = SupportSpatialUtil.buildDeleteQueryWithInClause("PointWindow", "id", first);
                env.compileExecuteFAF(deleteQuery, path);
                ids.removeAll(first);
            }

            env.sendEventBean(new SupportSpatialAABB("", 0, 0, SIZE, SIZE));
            assertFalse(env.listener("out").getAndClearIsInvoked());

            env.milestone(2);

            env.sendEventBean(new SupportSpatialAABB("", 0, 0, SIZE, SIZE));
            assertFalse(env.listener("out").getAndClearIsInvoked());

            env.undeployAll();
        }

        private static Collection<SupportSpatialPoint> generateCoordinates(Random random) {
            Map<UniformPair<Integer>, SupportSpatialPoint> points = new HashMap<>();
            while (points.size() < SIZE) {
                int x = random.nextInt(SIZE);
                int y = random.nextInt(SIZE);
                points.put(new UniformPair<Integer>(x, y), new SupportSpatialPoint(Integer.toString(x) + "_" + Integer.toString(y), (double) x, (double) y));
            }
            return points.values();
        }
    }

    public static class EPLSpatialPREventIndexRandomMovingPoints implements RegressionExecution {
        private static final Logger log = LoggerFactory.getLogger(EPLSpatialPREventIndexRandomMovingPoints.class);

        private final static int NUM_POINTS = 1000;
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
            String epl = "create table PointTable as (id string primary key, px double, py double);\n" +
                "create index MyIndex on PointTable((px,py) pointregionquadtree(0,0,100,100));\n" +
                "insert into PointTable select id, px, py from SupportSpatialPoint;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from PointTable as pt where point(px,py).inside(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl, path).addListener("out");

            Random random = new Random();
            List<SupportSpatialPoint> points = generateCoordinates(random, NUM_POINTS, WIDTH, HEIGHT);
            for (SupportSpatialPoint point : points) {
                sendPoint(env, point.getId(), point.getPx(), point.getPy());
            }

            env.milestone(0);
            EPCompiled deleteQuery = env.compileFAF("delete from PointTable where id=?::string", path);
            EPFireAndForgetPreparedQueryParameterized preparedDelete = env.runtime().getFireAndForgetService().prepareQueryWithParameters(deleteQuery);

            AtomicInteger milestone = new AtomicInteger();
            for (int i = 0; i < NUM_MOVES; i++) {
                SupportSpatialPoint pointMoved = points.get(random.nextInt(points.size()));
                movePoint(env, path, pointMoved, random, preparedDelete);

                double startX = pointMoved.getPx() - 5;
                double startY = pointMoved.getPy() - 5;
                BoundingBox bb = new BoundingBox(startX, startY, startX + 10, startY + 10);
                assertBBPoints(env, bb, points);

                if (Arrays.binarySearch(CHECKPOINT_AT, i) >= 0) {
                    log.info("Checkpoint at " + points.size());
                    env.milestoneInc(milestone);
                    preparedDelete = env.runtime().getFireAndForgetService().prepareQueryWithParameters(deleteQuery);
                }
            }

            env.undeployAll();
        }

        private void movePoint(RegressionEnvironment env, RegressionPath path, SupportSpatialPoint point, Random random, EPFireAndForgetPreparedQueryParameterized preparedDelete) {
            int direction = random.nextInt(4);
            double newX = point.getPx();
            double newY = point.getPy();
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

            // Comment-me-in:
            // log.info("Moving " + point.getId() + " from " + printPoint(point.getX(), point.getY()) + " to " + printPoint(newX, newY));
            preparedDelete.setObject(1, point.getId());
            env.runtime().getFireAndForgetService().executeQuery(preparedDelete);

            point.setPx(newX);
            point.setPy(newY);
            sendPoint(env, point.getId(), point.getPx(), point.getPy());
        }

        private List<SupportSpatialPoint> generateCoordinates(Random random, int numPoints, int width, int height) {
            List<SupportSpatialPoint> result = new ArrayList<>(numPoints);
            for (int i = 0; i < numPoints; i++) {
                int x = random.nextInt(width);
                int y = random.nextInt(height);
                result.add(new SupportSpatialPoint("P" + i, (double) x, (double) y));
            }
            return result;
        }
    }

    public static class EPLSpatialPREventIndexTableSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create window PointWindow#keepall as (id string, px double, py double);\n" +
                "create index MyIndex on PointWindow((px,py) pointregionquadtree(0,0,100,100,2,12));\n" +
                "insert into PointWindow select id, px, py from SupportSpatialPoint;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from PointWindow as pt where point(px,py).inside(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl).addListener("out");

            sendPoint(env, "P0", 1.9290410254557688, 79.2596477701767);
            sendPoint(env, "P1", 22.481380138332298, 38.21826613078289);
            sendPoint(env, "P2", 96.68069967422869, 60.135596079815734);
            sendPoint(env, "P3", 2.013086221651661, 79.96973017670842);
            sendPoint(env, "P4", 72.7141155566794, 34.769073156981754);
            sendPoint(env, "P5", 99.3778672522394, 97.26599233260606);
            sendPoint(env, "P6", 92.5721971936789, 45.52450892745069);
            sendPoint(env, "P7", 64.81513547235994, 74.40317040273223);
            sendPoint(env, "P8", 34.431526832055994, 77.1868630618566);
            sendPoint(env, "P9", 63.94019334876596, 60.49807218353348);
            sendPoint(env, "P10", 72.6304354938367, 33.08578043563804);
            sendPoint(env, "P11", 67.34486150692311, 23.93727603716781);
            sendPoint(env, "P12", 3.2289468086465156, 21.0564103499303);
            sendPoint(env, "P13", 54.93362964889536, 76.95495628291773);
            sendPoint(env, "P14", 62.626040886628786, 37.228228790772334);
            sendPoint(env, "P15", 31.89777659905859, 15.41080966535403);
            sendPoint(env, "P16", 54.54495428051385, 50.57461489577466);
            sendPoint(env, "P17", 72.07758279891948, 47.84348117893323);
            sendPoint(env, "P18", 96.10730711977887, 59.22231623726726);
            sendPoint(env, "P19", 1.4354270415599113, 20.003636602020634);
            sendPoint(env, "P20", 17.252052662019757, 10.711353613675922);
            sendPoint(env, "P21", 9.460168333656016, 76.32486040394515);

            env.milestone(0);

            BoundingBox bb = new BoundingBox(32.53403315866078, 2.7359221041404314, 69.34282527128134, 80.49662463068397);
            env.sendEventBean(new SupportSpatialAABB("", bb.getMinX(), bb.getMinY(), bb.getMaxX() - bb.getMinX(), bb.getMaxY() - bb.getMinY()));
            String received = sortJoinProperty(env.listener("out").getAndResetLastNewData(), "c0");
            assertEquals("P7,P8,P9,P11,P13,P14,P16", received);

            env.undeployAll();
        }
    }

    public static class EPLSpatialPREventIndexTableSubdivideDeepAddDestroy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table PointTable(id string primary key, px double, py double);\n" +
                "create index MyIndex on PointTable((px,py) pointregionquadtree(0,0,100,100,2,12));\n" +
                "insert into PointTable select id, px, py from SupportSpatialPoint;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from PointTable as pt where point(px,py).inside(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl).addListener("out");

            List<SupportSpatialPoint> points = new ArrayList<>();
            BoundingBox.BoundingBoxNode bbtree = new BoundingBox(0, 0, 100, 100).treeForPath("nw,se,sw,ne,nw,nw,nw,nw,nw,nw,nw,nw".split(","));
            BoundingBox somewhere = bbtree.nw.se.sw.ne.nw.nw.nw.nw.nw.nw.nw.nw.bb;

            addSendPoint(env, points, "P1", somewhere.getMinX(), somewhere.getMinY());
            addSendPoint(env, points, "P2", somewhere.getMinX(), somewhere.getMinY());
            addSendPoint(env, points, "P3", somewhere.getMinX(), somewhere.getMinY());
            assertBBTreePoints(env, bbtree, points);

            env.milestone(0);

            assertBBTreePoints(env, bbtree, points);

            env.undeployAll();
        }
    }

    public static class EPLSpatialPREventIndexTableSubdivideDestroy implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "create table PointTable(id string primary key, px double, py double);\n" +
                "create index MyIndex on PointTable((px,py) pointregionquadtree(0,0,100,100,4,40));\n" +
                "insert into PointTable select id, px, py from SupportSpatialPoint;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from PointTable as pt where point(px,py).inside(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl).addListener("out");

            sendPoint(env, "P1", 80, 40);
            sendPoint(env, "P2", 81, 41);
            sendPoint(env, "P3", 80, 40);

            env.milestone(0);

            assertRectanglesManyRow(env, env.listener("out"), BOXES, null, "P1,P2,P3", null, null, null);
            sendPoint(env, "P4", 80, 40);
            sendPoint(env, "P5", 81, 41);

            env.milestone(1);

            assertRectanglesManyRow(env, env.listener("out"), BOXES, null, "P1,P2,P3,P4,P5", null, null, null);

            env.undeployAll();
        }
    }

    public static class EPLSpatialPREventIndexTableSubdivideMergeDestroy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create table PointTable(id string primary key, px double, py double);\n" +
                "create index MyIndex on PointTable((px,py) pointregionquadtree(0,0,100,100,4,40));\n" +
                "insert into PointTable select id, px, py from SupportSpatialPoint;\n" +
                "@name('out') on SupportSpatialAABB as aabb select pt.id as c0 from PointTable as pt where point(px,py).inside(rectangle(x,y,width,height));\n";
            env.compileDeploy(epl, path).addListener("out");

            sendPoint(env, "P1", 80, 80);
            sendPoint(env, "P2", 81, 80);
            sendPoint(env, "P3", 80, 81);
            sendPoint(env, "P4", 80, 80);
            sendPoint(env, "P5", 45, 55);
            assertRectanglesManyRow(env, env.listener("out"), BOXES, null, null, "P5", "P1,P2,P3,P4", "P5");

            env.milestone(0);

            assertRectanglesManyRow(env, env.listener("out"), BOXES, null, null, "P5", "P1,P2,P3,P4", "P5");
            env.compileExecuteFAF("delete from PointTable where id = 'P4'", path);
            assertRectanglesManyRow(env, env.listener("out"), BOXES, null, null, "P5", "P1,P2,P3", "P5");

            env.milestone(1);

            assertRectanglesManyRow(env, env.listener("out"), BOXES, null, null, "P5", "P1,P2,P3", "P5");

            env.undeployAll();
        }
    }

    private static void assertIndexChoice(RegressionEnvironment env, RegressionPath path, String hint, String expectedIndexName) {
        String epl = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + hint +
            "on SupportSpatialAABB as aabb select mpw.id as c0 from MyPointWindow as mpw " +
            "where aabb.category = mpw.category and point(px, py).inside(rectangle(x, y, width, height))\n";
        env.compileDeploy(epl, path).addListener("s0");

        QueryPlanIndexDescOnExpr plan = SupportQueryPlanIndexHook.assertOnExprAndReset();
        assertEquals(expectedIndexName, plan.getTables()[0].getIndexName());

        env.sendEventBean(new SupportSpatialAABB("R1", 9, 14, 1.0001, 1.0001, "Y"));
        assertEquals("P2", env.listener("s0").assertOneGetNewAndReset().get("c0"));

        env.undeployModuleContaining("s0");
    }
}
