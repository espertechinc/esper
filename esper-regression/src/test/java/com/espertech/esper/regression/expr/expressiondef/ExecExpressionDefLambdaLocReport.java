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
package com.espertech.esper.regression.expr.expressiondef;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.lrreport.Item;
import com.espertech.esper.supportregression.bean.lrreport.LRUtil;
import com.espertech.esper.supportregression.bean.lrreport.LocationReport;
import com.espertech.esper.supportregression.bean.lrreport.LocationReportFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExecExpressionDefLambdaLocReport implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("LocationReport", LocationReport.class);
        configuration.addImport(LRUtil.class);
    }

    public void run(EPServiceProvider epService) throws Exception {

        /**
         * Regular algorithm to find separated luggage and new owner.
         */
        LocationReport theEvent = LocationReportFactory.makeLarge();
        List<Item> separatedLuggage = LocationReportFactory.findSeparatedLuggage(theEvent);

        for (Item item : separatedLuggage) {
            //log.info("Luggage that are separated (dist>20): " + item);
            Item newOwner = LocationReportFactory.findPotentialNewOwner(theEvent, item);
            //log.info("Found new owner " + newOwner);
        }

        String eplFragment = "" +
                "expression lostLuggage {" +
                "  lr => lr.items.where(l => l.type='L' and " +
                "    lr.items.anyof(p => p.type='P' and p.assetId=l.assetIdPassenger and LRUtil.distance(l.location.x, l.location.y, p.location.x, p.location.y) > 20))" +
                "}" +
                "expression passengers {" +
                "  lr => lr.items.where(l => l.type='P')" +
                "}" +
                "" +
                "expression nearestOwner {" +
                "  lr => lostLuggage(lr).toMap(key => key.assetId, " +
                "     value => passengers(lr).minBy(p => LRUtil.distance(value.location.x, value.location.y, p.location.x, p.location.y)))" +
                "}" +
                "" +
                "select lostLuggage(lr) as val1, nearestOwner(lr) as val2 from LocationReport lr";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);

        LocationReport bean = LocationReportFactory.makeLarge();
        epService.getEPRuntime().sendEvent(bean);

        Item[] val1 = itemArray((Collection<Item>) listener.assertOneGetNew().get("val1"));
        assertEquals(3, val1.length);
        assertEquals("L00000", val1[0].getAssetId());
        assertEquals("L00007", val1[1].getAssetId());
        assertEquals("L00008", val1[2].getAssetId());

        Map val2 = (Map) listener.assertOneGetNewAndReset().get("val2");
        assertEquals(3, val2.size());
        assertEquals("P00008", ((Item) val2.get("L00000")).getAssetId());
        assertEquals("P00001", ((Item) val2.get("L00007")).getAssetId());
        assertEquals("P00001", ((Item) val2.get("L00008")).getAssetId());
    }

    private Item[] itemArray(Collection<Item> it) {
        return it.toArray(new Item[it.size()]);
    }
}
