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

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportSpatialEventRectangle;

import java.util.ArrayList;
import java.util.List;

public class EPLSpatialMXCIFQuadTreeInvalid {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        // invalid-testing overlaps with pointregion-quadtree
        execs.add(new EPLSpatialInvalidEventIndexCreate());
        execs.add(new EPLSpatialInvalidEventIndexRuntime());
        execs.add(new EPLSpatialInvalidMethod());
        execs.add(new EPLSpatialInvalidFilterIndex());
        execs.add(new EPLSpatialDocSample());
        return execs;
    }

    private static class EPLSpatialInvalidFilterIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // invalid index for filter
            String epl = "expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "select * from SupportSpatialEventRectangle(rectangle(10, 20, 5, 6, filterindex:myindex).intersects(rectangle(x, y, width, height)))";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate filter expression 'rectangle(10,20,5,6,filterindex:myi...(82 chars)': Invalid index type 'pointregionquadtree', expected 'mxcifquadtree'");
        }
    }

    private static class EPLSpatialInvalidMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportSpatialEventRectangle(rectangle('a', 0).inside(rectangle(0, 0, 0, 0)))",
                "Failed to validate filter expression 'rectangle(\"a\",0).inside(rectangle(0...(43 chars)': Failed to validate method-chain parameter expression 'rectangle(0,0,0,0)': Unknown single-row function, expression declaration, script or aggregation function named 'rectangle' could not be resolved (did you mean 'rectangle.intersects')");
            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportSpatialEventRectangle(rectangle(0).intersects(rectangle(0, 0, 0, 0)))",
                "Failed to validate filter expression 'rectangle(0).intersects(rectangle(0...(43 chars)': Error validating left-hand-side method 'rectangle', expected 4 parameters but received 1 parameters");
        }
    }

    private static class EPLSpatialInvalidEventIndexRuntime implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('mywindow') create window RectangleWindow#keepall as SupportSpatialEventRectangle;\n" +
                "insert into RectangleWindow select * from SupportSpatialEventRectangle;\n" +
                "create index MyIndex on RectangleWindow((x, y, width, height) mxcifquadtree(0, 0, 100, 100));\n";
            env.compileDeploy(epl);

            try {
                env.sendEventBean(new SupportSpatialEventRectangle("E1", null, null, null, null, "category"));
            } catch (Exception ex) {
                SupportMessageAssertUtil.assertMessage(ex, "Unexpected exception in statement 'mywindow': Invalid value for index 'MyIndex' column 'x' received null and expected non-null");
            }

            try {
                env.sendEventBean(new SupportSpatialEventRectangle("E1", 200d, 200d, 1, 1));
            } catch (RuntimeException ex) {
                SupportMessageAssertUtil.assertMessage(ex, "Unexpected exception in statement 'mywindow': Invalid value for index 'MyIndex' column '(x,y,width,height)' received (200.0,200.0,1.0,1.0) and expected a value intersecting index bounding box (range-end-inclusive) {minX=0.0, minY=0.0, maxX=100.0, maxY=100.0}");
            }

            env.undeployAll();
        }
    }

    private static class EPLSpatialInvalidEventIndexCreate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // most are covered by point-region test already
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow#keepall as SupportSpatialEventRectangle", path);

            // invalid number of columns
            SupportMessageAssertUtil.tryInvalidCompile(env, path, "create index MyIndex on MyWindow(x mxcifquadtree(0, 0, 100, 100))",
                "Index of type 'mxcifquadtree' requires 4 expressions as index columns but received 1");

            // same index twice, by-columns
            env.compileDeploy("create window SomeWindow#keepall as SupportSpatialEventRectangle", path);
            env.compileDeploy("create index SomeWindowIdx1 on SomeWindow((x, y, width, height) mxcifquadtree(0, 0, 1, 1))", path);
            SupportMessageAssertUtil.tryInvalidCompile(env, path, "create index SomeWindowIdx2 on SomeWindow((x, y, width, height) mxcifquadtree(0, 0, 1, 1))",
                "An index for the same columns already exists");

            env.undeployAll();
        }
    }

    private static class EPLSpatialDocSample implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table RectangleTable(rectangleId string primary key, rx double, ry double, rwidth double, rheight double);\n" +
                "create index RectangleIndex on RectangleTable((rx, ry, rwidth, rheight) mxcifquadtree(0, 0, 100, 100));\n" +
                "create schema OtherRectangleEvent(otherX double, otherY double, otherWidth double, otherHeight double);\n" +
                "on OtherRectangleEvent\n" +
                "select rectangleId from RectangleTable\n" +
                "where rectangle(rx, ry, rwidth, rheight).intersects(rectangle(otherX, otherY, otherWidth, otherHeight));" +
                "expression myMXCIFQuadtreeSettings { mxcifquadtree(0, 0, 100, 100) } \n" +
                "select * from SupportSpatialAABB(rectangle(10, 20, 5, 5, filterindex:myMXCIFQuadtreeSettings).intersects(rectangle(x, y, width, height)));\n";
            env.compileDeploy(epl).undeployAll();
        }
    }
}
