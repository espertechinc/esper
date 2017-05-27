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
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportSpatialAABB;
import com.espertech.esper.supportregression.bean.SupportSpatialDualAABB;
import com.espertech.esper.supportregression.bean.SupportSpatialEventRectangle;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.util.Arrays;

public class ExecSpatialMXCIFQuadTreeInvalid implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : Arrays.asList(SupportSpatialAABB.class, SupportSpatialEventRectangle.class, SupportSpatialDualAABB.class)) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        // invalid-testing overlaps with pointregion-quadtree
        runAssertionInvalidEventIndexCreate(epService);
        runAssertionInvalidEventIndexRuntime(epService);
        runAssertionInvalidMethod(epService);
        runAssertionInvalidFilterIndex(epService);

        runAssertionDocSample(epService);
    }

    private void runAssertionInvalidFilterIndex(EPServiceProvider epService) {
        // invalid index for filter
        String epl = "expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "select * from SupportSpatialEventRectangle(rectangle(10, 20, 5, 6, filterindex:myindex).intersects(rectangle(x, y, width, height)))";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Failed to validate filter expression 'rectangle(10,20,5,6,filterindex:myi...(82 chars)': Invalid index type 'pointregionquadtree', expected 'mxcifquadtree'");
    }

    private void runAssertionInvalidMethod(EPServiceProvider epService) {
        SupportMessageAssertUtil.tryInvalid(epService, "select * from SupportSpatialEventRectangle(rectangle('a', 0).inside(rectangle(0, 0, 0, 0)))",
                "Failed to validate filter expression 'rectangle(\"a\",0).inside(rectangle(0...(43 chars)': Failed to validate method-chain parameter expression 'rectangle(0,0,0,0)': Unknown single-row function, expression declaration, script or aggregation function named 'rectangle' could not be resolved (did you mean 'rectangle.intersects')");
        SupportMessageAssertUtil.tryInvalid(epService, "select * from SupportSpatialEventRectangle(rectangle(0).intersects(rectangle(0, 0, 0, 0)))",
                "Failed to validate filter expression 'rectangle(0).intersects(rectangle(0...(43 chars)': Error validating left-hand-side method 'rectangle', expected 4 parameters but received 1 parameters");
    }

    private void runAssertionInvalidEventIndexRuntime(EPServiceProvider epService) throws Exception {
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

    private void runAssertionInvalidEventIndexCreate(EPServiceProvider epService) {
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

    private void runAssertionDocSample(EPServiceProvider epService) throws Exception {
        String epl = "create table RectangleTable(rectangleId string primary key, rx double, ry double, rwidth double, rheight double);\n" +
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
}
