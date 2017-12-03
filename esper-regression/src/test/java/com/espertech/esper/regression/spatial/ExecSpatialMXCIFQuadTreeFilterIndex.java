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
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.supportregression.bean.SupportSpatialAABB;
import com.espertech.esper.supportregression.bean.SupportSpatialDualAABB;
import com.espertech.esper.supportregression.bean.SupportSpatialEventRectangle;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportFilterHelper;
import com.espertech.esper.supportregression.util.SupportFilterItem;

import java.util.Arrays;

import static com.espertech.esper.supportregression.util.SupportSpatialUtil.sendAssertSpatialAABB;
import static org.junit.Assert.assertEquals;

public class ExecSpatialMXCIFQuadTreeFilterIndex implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : Arrays.asList(SupportSpatialAABB.class, SupportSpatialEventRectangle.class, SupportSpatialDualAABB.class)) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionFilterIndexPerfPattern(epService);
        runAssertionFilterIndexTypeAssertion(epService);
    }

    private void runAssertionFilterIndexTypeAssertion(EPServiceProvider epService) {
        String eplNoIndex = "select * from SupportSpatialEventRectangle(rectangle(0, 0, 1, 1).intersects(rectangle(x, y, width, height)))";
        SupportFilterHelper.assertFilterMulti(epService, eplNoIndex, "SupportSpatialEventRectangle", new SupportFilterItem[][]{{SupportFilterItem.getBoolExprFilterItem()}});

        String eplIndexed = "expression myindex {mxcifquadtree(0, 0, 100, 100)}" +
                "select * from SupportSpatialEventRectangle(rectangle(10, 20, 5, 6, filterindex:myindex).intersects(rectangle(x, y, width, height)))";
        EPStatement statement = SupportFilterHelper.assertFilterMulti(epService, eplIndexed, "SupportSpatialEventRectangle", new SupportFilterItem[][]{{new SupportFilterItem("x,y,width,height/myindex/mxcifquadtree/0.0,0.0,100.0,100.0,4.0,20.0", FilterOperator.ADVANCED_INDEX)}});
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendAssertEventRectangle(epService, listener, 10, 20, 0, 0, true);
        sendAssertEventRectangle(epService, listener, 9, 19, 0.9999, 0.9999, false);
        sendAssertEventRectangle(epService, listener, 9, 19, 1, 1, true);
        sendAssertEventRectangle(epService, listener, 15, 26, 0, 0, true);
        sendAssertEventRectangle(epService, listener, 15.001, 26.001, 0, 0, false);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterIndexPerfPattern(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("expression myindex {mxcifquadtree(0, 0, 100, 100)}" +
                "select * from pattern [every p=SupportSpatialEventRectangle -> SupportSpatialAABB(rectangle(p.x, p.y, p.width, p.height, filterindex:myindex).intersects(rectangle(x, y, width, height)))]");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSpatialEventRectanges(epService, 100, 50);
        sendAssertSpatialAABB(epService, listener, 100, 50, 1000);

        epService.getEPAdministrator().destroyAllStatements();
    }

    protected static void sendAssertEventRectangle(EPServiceProvider epService, SupportUpdateListener listener, double x, double y, double width, double height, boolean expected) {
        epService.getEPRuntime().sendEvent(new SupportSpatialEventRectangle(null, x, y, width, height));
        assertEquals(expected, listener.getIsInvokedAndReset());
    }

    protected static void sendSpatialEventRectanges(EPServiceProvider epService, int numX, int numY) {
        for (int x = 0; x < numX; x++) {
            for (int y = 0; y < numY; y++) {
                epService.getEPRuntime().sendEvent(new SupportSpatialEventRectangle(Integer.toString(x) + "_" + Integer.toString(y), (double) x, (double) y, 0.1, 0.2));
            }
        }
    }
}
