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
package com.espertech.esper.regression.nwtable.infra;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecNWTableInfraOnMergePerf implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {

        runAssertionPerformance(epService, true, EventRepresentationChoice.ARRAY);
        runAssertionPerformance(epService, true, EventRepresentationChoice.MAP);
        runAssertionPerformance(epService, true, EventRepresentationChoice.DEFAULT);
        runAssertionPerformance(epService, false, EventRepresentationChoice.ARRAY);
    }

    private void runAssertionPerformance(EPServiceProvider epService, boolean namedWindow, EventRepresentationChoice outputType) {

        String eplCreate = namedWindow ?
                outputType.getAnnotationText() + " create window MyWindow#keepall as (c1 string, c2 int)" :
                "create table MyWindow(c1 string primary key, c2 int)";
        EPStatement stmtNamedWindow = epService.getEPAdministrator().createEPL(eplCreate);
        assertTrue(outputType.matchesClass(stmtNamedWindow.getEventType().getUnderlyingType()));

        // preload events
        EPStatement stmt = epService.getEPAdministrator().createEPL("insert into MyWindow select theString as c1, intPrimitive as c2 from SupportBean");
        final int totalUpdated = 5000;
        for (int i = 0; i < totalUpdated; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, 0));
        }
        stmt.destroy();

        String epl = "on SupportBean sb merge MyWindow nw where nw.c1 = sb.theString " +
                "when matched then update set nw.c2=sb.intPrimitive";
        stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener mergeListener = new SupportUpdateListener();
        stmt.addListener(mergeListener);

        // prime
        for (int i = 0; i < 100; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, 1));
        }
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < totalUpdated; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, 1));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        // verify
        Iterator<EventBean> events = stmtNamedWindow.iterator();
        int count = 0;
        for (; events.hasNext(); ) {
            EventBean next = events.next();
            assertEquals(1, next.get("c2"));
            count++;
        }
        assertEquals(totalUpdated, count);
        assertTrue("Delta=" + delta, delta < 500);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindow", true);
    }
}
