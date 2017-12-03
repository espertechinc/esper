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
import com.espertech.esper.client.EPPreparedStatement;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.supportregression.bean.SupportSpatialAABB;
import com.espertech.esper.supportregression.bean.SupportSpatialDualPoint;
import com.espertech.esper.supportregression.bean.SupportSpatialPoint;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportFilterHelper;
import com.espertech.esper.supportregression.util.SupportFilterItem;

import java.util.Arrays;

import static com.espertech.esper.supportregression.util.SupportSpatialUtil.*;
import static org.junit.Assert.assertEquals;

public class ExecSpatialPointRegionQuadTreeFilterIndex implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : Arrays.asList(SupportSpatialPoint.class, SupportSpatialAABB.class, SupportSpatialDualPoint.class)) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionFilterIndexPerfStatement(epService);
        runAssertionFilterIndexPerfContextPartition(epService);
        runAssertionFilterIndexPerfPattern(epService);
        runAssertionFilterIndexUnoptimized(epService);
        runAssertionFilterIndexTypeAssertion(epService);
    }

    private void runAssertionFilterIndexTypeAssertion(EPServiceProvider epService) {
        String eplNoIndex = "select * from SupportSpatialAABB(point(0, 0).inside(rectangle(x, y, width, height)))";
        SupportFilterHelper.assertFilterMulti(epService, eplNoIndex, "SupportSpatialAABB", new SupportFilterItem[][]{{SupportFilterItem.getBoolExprFilterItem()}});

        String eplIndexed = "expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "select * from SupportSpatialAABB(point(0, 0, filterindex:myindex).inside(rectangle(x, y, width, height)))";
        SupportFilterHelper.assertFilterMulti(epService, eplIndexed, "SupportSpatialAABB", new SupportFilterItem[][]{{new SupportFilterItem("x,y,width,height/myindex/pointregionquadtree/0.0,0.0,100.0,100.0,4.0,20.0", FilterOperator.ADVANCED_INDEX)}});

        epService.getEPAdministrator().destroyAllStatements();
    }


    private void runAssertionFilterIndexUnoptimized(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportSpatialAABB(point(5, 10).inside(rectangle(x, y, width, height)))");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendRectangle(epService, "R1", 0, 0, 5, 10);
        sendRectangle(epService, "R2", 4, 3, 2, 20);
        assertEquals("R2", listener.assertOneGetNewAndReset().get("id"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterIndexPerfStatement(EPServiceProvider epService) {
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL("expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "select * from SupportSpatialAABB(point(?, ?, filterindex:myindex).inside(rectangle(x, y, width, height)))");
        SupportUpdateListener listener = new SupportUpdateListener();

        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 20; y++) {
                prepared.setObject(1, x);
                prepared.setObject(2, y);
                epService.getEPAdministrator().create(prepared).addListener(listener);
            }
        }
        sendAssertSpatialAABB(epService, listener, 100, 20, 1000);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterIndexPerfPattern(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "select * from pattern [every p=SupportSpatialPoint -> SupportSpatialAABB(point(p.px, p.py, filterindex:myindex).inside(rectangle(x, y, width, height)))]");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSpatialPoints(epService, 100, 100);
        sendAssertSpatialAABB(epService, listener, 100, 100, 1000);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterIndexPerfContextPartition(EPServiceProvider epService) {

        epService.getEPAdministrator().createEPL("create context PerPointCtx initiated by SupportSpatialPoint ssp");
        EPStatement stmt = epService.getEPAdministrator().createEPL("expression myindex {pointregionquadtree(0, 0, 100, 100)}" +
                "context PerPointCtx select count(*) from SupportSpatialAABB(point(context.ssp.px, context.ssp.py, filterindex:myindex).inside(rectangle(x, y, width, height)))");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSpatialPoints(epService, 100, 100);
        sendAssertSpatialAABB(epService, listener, 100, 100, 1000);

        epService.getEPAdministrator().destroyAllStatements();
    }
}
