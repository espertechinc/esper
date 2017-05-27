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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class ExecJoin3StreamInKeywordPerformance implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S1", SupportBean_S1.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S2", SupportBean_S2.class);

        String epl = "select s0.id as val from " +
                "S0#keepall s0, " +
                "S1#keepall s1, " +
                "S2#keepall s2 " +
                "where p00 in (p10, p20)";
        String[] fields = "val".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(i, "P00_" + i));
        }
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "x"));

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S2(1, "P00_6541"));
            EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{6541}});
        }
        long delta = System.currentTimeMillis() - startTime;
        assertTrue("delta=" + delta, delta < 500);
        log.info("delta=" + delta);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecJoin3StreamInKeywordPerformance.class);
}
