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
import com.espertech.esper.common.internal.epl.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportSpatialPoint;
import com.espertech.esper.regressionlib.support.filter.SupportFilterHelper;
import com.espertech.esper.regressionlib.support.util.SupportSpatialUtil;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.support.util.SupportSpatialUtil.*;
import static org.junit.Assert.assertEquals;

public class EPLSpatialPointRegionQuadTreeFilterIndex {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSpatialPRFilterIndexPerfStatement());
        execs.add(new EPLSpatialPRFilterIndexPerfContextPartition());
        execs.add(new EPLSpatialPRFilterIndexPerfPattern());
        execs.add(new EPLSpatialPRFilterIndexUnoptimized());
        execs.add(new EPLSpatialPRFilterIndexTypeAssertion());
        execs.add(new EPLSpatialPRFilterIndexPatternSimple());
        execs.add(new EPLSpatialPRFilterIndexContext());
        return execs;
    }

    private static class EPLSpatialPRFilterIndexTypeAssertion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplNoIndex = "@name('s0') select * from SupportSpatialAABB(point(0, 0).inside(rectangle(x, y, width, height)))";
            env.compileDeploy(eplNoIndex);
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportSpatialAABB", new FilterItem[][]{{FilterItem.getBoolExprFilterItem()}});
            env.undeployAll();

            String eplIndexed = "@name('s0') expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "select * from SupportSpatialAABB(point(0, 0, filterindex:myindex).inside(rectangle(x, y, width, height)))";
            env.compileDeploy(eplIndexed);
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportSpatialAABB", new FilterItem[][]{{new FilterItem("x,y,width,height/myindex/pointregionquadtree/0.0,0.0,100.0,100.0,4.0,20.0", FilterOperator.ADVANCED_INDEX)}});

            env.undeployAll();
        }

    }

    private static class EPLSpatialPRFilterIndexUnoptimized implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from SupportSpatialAABB(point(5, 10).inside(rectangle(x, y, width, height)))");
            env.addListener("s0");

            sendRectangle(env, "R1", 0, 0, 5, 10);
            sendRectangle(env, "R2", 4, 3, 2, 20);
            assertEquals("R2", env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.undeployAll();
        }
    }

    private static class EPLSpatialPRFilterIndexPerfStatement implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "select * from SupportSpatialAABB(point(?::int, ?::int, filterindex:myindex).inside(rectangle(x, y, width, height)))");
            SupportUpdateListener listener = new SupportUpdateListener();

            int count = 0;
            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 10; y++) {
                    int finalX = x;
                    int finalY = y;
                    String name = x + "_" + y;
                    DeploymentOptions options = new DeploymentOptions().setStatementSubstitutionParameter(prepared -> {
                        prepared.setObject(1, finalX);
                        prepared.setObject(2, finalY);
                    }).setStatementNameRuntime(ctx -> name);
                    env.deploy(compiled, options).statement(name).addListener(listener);
                    // System.out.println("Deployed #" + count);
                    count++;
                }
            }
            sendAssertSpatialAABB(env, listener, 10, 10, 1000);

            env.undeployAll();
        }
    }

    private static class EPLSpatialPRFilterIndexPerfPattern implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "select * from pattern [every p=SupportSpatialPoint -> SupportSpatialAABB(point(p.px, p.py, filterindex:myindex).inside(rectangle(x, y, width, height)))]");
            env.addListener("s0");

            sendSpatialPoints(env, 100, 100);
            sendAssertSpatialAABB(env, env.listener("s0"), 100, 100, 1000);

            env.undeployAll();
        }
    }

    private static class EPLSpatialPRFilterIndexPerfContextPartition implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context PerPointCtx initiated by SupportSpatialPoint ssp", path);
            env.compileDeploy("@name('s0') expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "context PerPointCtx select count(*) from SupportSpatialAABB(point(context.ssp.px, context.ssp.py, filterindex:myindex).inside(rectangle(x, y, width, height)))", path);
            env.addListener("s0");

            sendSpatialPoints(env, 100, 100);
            sendAssertSpatialAABB(env, env.listener("s0"), 100, 100, 1000);

            env.undeployAll();
        }
    }

    public static class EPLSpatialPRFilterIndexPatternSimple implements RegressionExecution {
        private final static List<BoundingBox> BOXES = Arrays.asList(
            new BoundingBox(0, 0, 50, 50),
            new BoundingBox(50, 0, 100, 50),
            new BoundingBox(0, 50, 50, 100),
            new BoundingBox(50, 50, 100, 100),
            new BoundingBox(25, 25, 75, 75)
        );

        public void run(RegressionEnvironment env) {

            String epl = "@name('out') expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "select p.id as c0 from pattern [every p=SupportSpatialPoint -> every SupportSpatialAABB(point(p.px, p.py, filterindex:myindex).inside(rectangle(x, y, width, height)))]";
            env.compileDeploy(epl).addListener("out");

            env.milestone(0);

            sendPoint(env, "P0", 10, 10);
            sendPoint(env, "P1", 60, 60);
            sendPoint(env, "P2", 60, 10);
            sendPoint(env, "P3", 10, 60);
            sendPoint(env, "P4", 10, 10);
            assertEquals(6, SupportFilterHelper.getFilterCountApprox(env));
            assertRectanglesManyRow(env, env.listener("out"), BOXES, "P0,P4", "P2", "P3", "P1", "P1");

            env.milestone(1);

            assertEquals(6, SupportFilterHelper.getFilterCountApprox(env));
            assertRectanglesManyRow(env, env.listener("out"), BOXES, "P0,P4", "P2", "P3", "P1", "P1");

            env.undeployAll();
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
        }
    }

    public static class EPLSpatialPRFilterIndexContext implements RegressionExecution {
        private final static int WIDTH = 10;
        private final static int HEIGHT = 10;
        private final static int NUM_POINTS = 100;
        private final static int NUM_QUERIES = 100;
        private final static int NUM_ITERATIONS = 3;

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {

            String epl = "create context PointContext initiated by SupportSpatialPoint ssp terminated by SupportBean(theString=ssp.id);\n" +
                "@name('out') expression myindex {pointregionquadtree(0, 0, 10, 10)}" +
                "context PointContext select context.ssp.id as c0 from SupportSpatialAABB(point(context.ssp.px, context.ssp.py, filterindex:myindex).inside(rectangle(x, y, width, height)))";
            env.compileDeploy(epl).addListener("out");

            List<SupportSpatialPoint> points = new ArrayList<>();
            int count = 0;
            AtomicInteger milestone = new AtomicInteger();
            for (int iteration = 0; iteration < NUM_ITERATIONS; iteration++) {

                query(env, points);
                addPoints(env, points, milestone);
                query(env, points);

                env.milestoneInc(milestone);

                query(env, points);
                removePoints(env, points);
                query(env, points);

                env.milestoneInc(milestone);
            }

            env.undeployAll();
        }

        private void removePoints(RegressionEnvironment env, List<SupportSpatialPoint> points) {
            for (SupportSpatialPoint point : points) {
                env.sendEventBean(new SupportBean(point.getId(), 0));
            }
            points.clear();
        }

        private void query(RegressionEnvironment env, List<SupportSpatialPoint> points) {
            Random random = new Random();
            for (int i = 0; i < NUM_QUERIES; i++) {
                int x = (int) random.nextDouble() * WIDTH;
                int y = (int) random.nextDouble() * HEIGHT;
                BoundingBox bb = new BoundingBox(x - 3, y - 2, x + 5, y + 5);
                SupportSpatialUtil.assertBBPoints(env, bb, points);
            }
        }

        private void addPoints(RegressionEnvironment env, List<SupportSpatialPoint> points, AtomicInteger pointCount) {
            Random random = new Random();
            for (int i = 0; i < NUM_POINTS; i++) {
                int x = (int) random.nextDouble() * WIDTH;
                int y = (int) random.nextDouble() * HEIGHT;
                sendAddPoint(env, points, "P" + pointCount.incrementAndGet(), x, y);
            }
        }
    }

}
