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
package com.espertech.esper.regression.nwtable.namedwindow;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportSimpleBeanOne;
import com.espertech.esper.supportregression.bean.SupportSimpleBeanTwo;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecNamedWIndowFAFQueryJoinPerformance implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(false);
        configuration.addEventType("SSB1", SupportSimpleBeanOne.class);
        configuration.addEventType("SSB2", SupportSimpleBeanTwo.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create window W1#unique(s1) as SSB1");
        epService.getEPAdministrator().createEPL("insert into W1 select * from SSB1");

        epService.getEPAdministrator().createEPL("create window W2#unique(s2) as SSB2");
        epService.getEPAdministrator().createEPL("insert into W2 select * from SSB2");

        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(new SupportSimpleBeanOne("A" + i, 0, 0, 0));
            epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("A" + i, 0, 0, 0));
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from W1 as w1, W2 as w2 " +
                    "where w1.s1 = w2.s2");
            assertEquals(1000, result.getArray().length);
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        System.out.println("Delta=" + delta);
        assertTrue("Delta=" + delta, delta < 1000);
    }
}
