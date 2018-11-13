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

import com.espertech.esper.common.internal.epl.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportSpatialEventRectangle;
import com.espertech.esper.regressionlib.support.filter.SupportFilterHelper;
import com.espertech.esper.regressionlib.support.util.SupportSpatialUtil;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.support.util.SupportSpatialUtil.*;
import static org.junit.Assert.assertEquals;

public class EPLSpatialMXCIFQuadTreeFilterIndex {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSpatialMXCIFFilterIndexPatternSimple());
        execs.add(new EPLSpatialMXCIFFilterIndexPerfPattern());
        execs.add(new EPLSpatialMXCIFFilterIndexTypeAssertion());
        execs.add(new EPLSpatialMXCIFFilterIndexWContext());
        return execs;
    }

    private static class EPLSpatialMXCIFFilterIndexTypeAssertion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplNoIndex = "@name('s0') select * from SupportSpatialEventRectangle(rectangle(0, 0, 1, 1).intersects(rectangle(x, y, width, height)))";
            env.compileDeploy(eplNoIndex);
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportSpatialEventRectangle", new FilterItem[][]{{FilterItem.getBoolExprFilterItem()}});
            env.undeployAll();

            String eplIndexed = "@name('s0') expression myindex {mxcifquadtree(0, 0, 100, 100)}" +
                "select * from SupportSpatialEventRectangle(rectangle(10, 20, 5, 6, filterindex:myindex).intersects(rectangle(x, y, width, height)))";
            env.compileDeploy(eplIndexed).addListener("s0");
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportSpatialEventRectangle", new FilterItem[][]{{new FilterItem("x,y,width,height/myindex/mxcifquadtree/0.0,0.0,100.0,100.0,4.0,20.0", FilterOperator.ADVANCED_INDEX)}});

            sendAssertEventRectangle(env, env.listener("s0"), 10, 20, 0, 0, true);
            sendAssertEventRectangle(env, env.listener("s0"), 9, 19, 0.9999, 0.9999, false);

            env.milestone(0);

            sendAssertEventRectangle(env, env.listener("s0"), 9, 19, 1, 1, true);
            sendAssertEventRectangle(env, env.listener("s0"), 15, 26, 0, 0, true);
            sendAssertEventRectangle(env, env.listener("s0"), 15.001, 26.001, 0, 0, false);

            env.undeployAll();
        }
    }

    public static class EPLSpatialMXCIFFilterIndexPatternSimple implements RegressionExecution {
        private final static List<BoundingBox> BOXES = Arrays.asList(
            new BoundingBox(0, 0, 50, 50),
            new BoundingBox(50, 0, 100, 50),
            new BoundingBox(0, 50, 50, 100),
            new BoundingBox(50, 50, 100, 100),
            new BoundingBox(25, 25, 75, 75)
        );

        public void run(RegressionEnvironment env) {

            String epl = "@name('out') expression myindex {mxcifquadtree(0, 0, 100, 100)}" +
                "select p.id as c0 from pattern [every p=SupportSpatialEventRectangle -> every SupportSpatialAABB(rectangle(p.x, p.y, p.width, p.height, filterindex:myindex).intersects(rectangle(x, y, width, height)))]";
            env.compileDeploy(epl).addListener("out");
            env.milestone(0);

            sendEventRectangle(env, "R0", 10, 10, 1, 1);
            sendEventRectangle(env, "R1", 60, 60, 1, 1);
            sendEventRectangle(env, "R2", 60, 10, 1, 1);
            sendEventRectangle(env, "R3", 10, 60, 1, 1);
            sendEventRectangle(env, "R4", 10, 10, 1, 1);
            assertEquals(6, SupportFilterHelper.getFilterCountApprox(env));
            assertRectanglesManyRow(env, env.listener("out"), BOXES, "R0,R4", "R2", "R3", "R1", "R1");

            env.milestone(1);

            assertEquals(6, SupportFilterHelper.getFilterCountApprox(env));
            assertRectanglesManyRow(env, env.listener("out"), BOXES, "R0,R4", "R2", "R3", "R1", "R1");

            env.undeployAll();
        }
    }

    private static class EPLSpatialMXCIFFilterIndexPerfPattern implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') expression myindex {mxcifquadtree(0, 0, 100, 100)}" +
                "select * from pattern [every p=SupportSpatialEventRectangle -> SupportSpatialAABB(rectangle(p.x, p.y, p.width, p.height, filterindex:myindex).intersects(rectangle(x, y, width, height)))]");
            env.addListener("s0");

            sendSpatialEventRectanges(env, 100, 50);
            sendAssertSpatialAABB(env, env.listener("s0"), 100, 50, 1000);

            env.undeployAll();
        }
    }

    public static class EPLSpatialMXCIFFilterIndexWContext implements RegressionExecution {
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

            String epl = "create context RectangleContext initiated by SupportSpatialEventRectangle ssr terminated by SupportBean(theString=ssr.id);\n" +
                "@name('out') expression myindex {mxcifquadtree(0, 0, 10, 10)}" +
                "context RectangleContext select context.ssr.id as c0 from SupportSpatialAABB(rectangle(context.ssr.x, context.ssr.y, context.ssr.width, context.ssr.height, filterindex:myindex).intersects(rectangle(x, y, width, height)))";
            env.compileDeploy(epl).addListener("out");

            List<SupportSpatialEventRectangle> rectangles = new ArrayList<>();
            AtomicInteger milestone = new AtomicInteger();
            for (int iteration = 0; iteration < NUM_ITERATIONS; iteration++) {

                query(env, rectangles);
                addRectangles(env, rectangles, milestone);
                query(env, rectangles);

                env.milestoneInc(milestone);

                query(env, rectangles);
                removeRectangles(env, rectangles);
                query(env, rectangles);

                env.milestoneInc(milestone);
            }

            env.undeployAll();
        }

        private void removeRectangles(RegressionEnvironment env, List<SupportSpatialEventRectangle> points) {
            for (SupportSpatialEventRectangle point : points) {
                env.sendEventBean(new SupportBean(point.getId(), 0));
            }
            points.clear();
        }

        private void query(RegressionEnvironment env, List<SupportSpatialEventRectangle> points) {
            Random random = new Random();
            for (int i = 0; i < NUM_QUERIES; i++) {
                int x = (int) random.nextDouble() * WIDTH;
                int y = (int) random.nextDouble() * HEIGHT;
                BoundingBox bb = new BoundingBox(x - 3, y - 2, x + 5, y + 5);
                SupportSpatialUtil.assertBBRectangles(env, bb, points);
            }
        }

        private void addRectangles(RegressionEnvironment env, List<SupportSpatialEventRectangle> rectangles, AtomicInteger rectangleCount) {
            Random random = new Random();
            for (int i = 0; i < NUM_POINTS; i++) {
                int x = (int) random.nextDouble() * WIDTH;
                int y = (int) random.nextDouble() * HEIGHT;
                double width = random.nextDouble() * WIDTH / 0.25;
                double height = random.nextDouble() * HEIGHT / 0.25;
                sendAddRectangle(env, rectangles, "R" + rectangleCount.incrementAndGet(), x, y, width, height);
            }
        }
    }

    protected static void sendAssertEventRectangle(RegressionEnvironment env, SupportListener listener, double x, double y, double width, double height, boolean expected) {
        env.sendEventBean(new SupportSpatialEventRectangle(null, x, y, width, height));
        assertEquals(expected, env.listener("s0").getIsInvokedAndReset());
    }

    protected static void sendSpatialEventRectanges(RegressionEnvironment env, int numX, int numY) {
        for (int x = 0; x < numX; x++) {
            for (int y = 0; y < numY; y++) {
                env.sendEventBean(new SupportSpatialEventRectangle(Integer.toString(x) + "_" + Integer.toString(y), (double) x, (double) y, 0.1, 0.2));
            }
        }
    }
}
