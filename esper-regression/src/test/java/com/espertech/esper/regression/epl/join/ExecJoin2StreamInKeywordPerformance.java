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
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class ExecJoin2StreamInKeywordPerformance implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportBean_S0.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInKeywordSingleIndexLookup(epService);
        runAssertionInKeywordMultiIndexLookup(epService);
    }

    private void runAssertionInKeywordSingleIndexLookup(EPServiceProvider epService) {
        String epl = "select intPrimitive as val from SupportBean#keepall sb, SupportBean_S0 s0 unidirectional " +
                "where sb.theString in (s0.p00, s0.p01)";
        String[] fields = "val".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E645", "E8975"));
            EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{645}, {8975}});
        }
        long delta = System.currentTimeMillis() - startTime;
        assertTrue("delta=" + delta, delta < 500);
        log.info("delta=" + delta);

        stmt.destroy();
    }

    private void runAssertionInKeywordMultiIndexLookup(EPServiceProvider epService) {
        String epl = "select id as val from SupportBean_S0#keepall s0, SupportBean sb unidirectional " +
                "where sb.theString in (s0.p00, s0.p01)";
        String[] fields = "val".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(i, "p00_" + i, "p01_" + i));
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("p01_645", 0));
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{645});
        }
        long delta = System.currentTimeMillis() - startTime;
        assertTrue("delta=" + delta, delta < 500);
        log.info("delta=" + delta);

        stmt.destroy();
    }

    private static final Logger log = LoggerFactory.getLogger(ExecJoin2StreamInKeywordPerformance.class);
}
