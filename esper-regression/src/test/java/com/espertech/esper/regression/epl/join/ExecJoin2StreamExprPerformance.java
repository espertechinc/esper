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
import com.espertech.esper.supportregression.bean.SupportBeanRange;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class ExecJoin2StreamExprPerformance implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_ST0", SupportBean_ST0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanRange", SupportBeanRange.class);

        String epl;

        epl = "select intPrimitive as val from SupportBean#keepall sb, SupportBean_ST0#lastevent s0 where sb.theString = 'E6750'";
        tryAssertion(epService, epl, new SupportBean_ST0("E", -1), 6750);

        epl = "select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.theString = 'E6749'";
        tryAssertion(epService, epl, new SupportBean_ST0("E", -1), 6749);

        epService.getEPAdministrator().createEPL("create variable string myconst = 'E6751'");
        epl = "select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.theString = myconst";
        tryAssertion(epService, epl, new SupportBean_ST0("E", -1), 6751);

        epl = "select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.theString = (id || '6752')";
        tryAssertion(epService, epl, new SupportBean_ST0("E", -1), 6752);

        epl = "select intPrimitive as val from SupportBean#keepall sb, SupportBean_ST0#lastevent s0 where sb.theString = (id || '6753')";
        tryAssertion(epService, epl, new SupportBean_ST0("E", -1), 6753);

        epl = "select intPrimitive as val from SupportBean#keepall sb, SupportBean_ST0#lastevent s0 where sb.theString = 'E6754' and sb.intPrimitive=6754";
        tryAssertion(epService, epl, new SupportBean_ST0("E", -1), 6754);

        epl = "select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.theString = (id || '6755') and sb.intPrimitive=6755";
        tryAssertion(epService, epl, new SupportBean_ST0("E", -1), 6755);

        epl = "select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.intPrimitive between 6756 and 6756";
        tryAssertion(epService, epl, new SupportBean_ST0("E", -1), 6756);

        epl = "select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.intPrimitive >= 6757 and intPrimitive <= 6757";
        tryAssertion(epService, epl, new SupportBean_ST0("E", -1), 6757);

        epl = "select intPrimitive as val from SupportBean_ST0#lastevent s0, SupportBean#keepall sb where sb.theString = 'E6758' and sb.intPrimitive >= 6758 and intPrimitive <= 6758";
        tryAssertion(epService, epl, new SupportBean_ST0("E", -1), 6758);

        epl = "select sum(intPrimitive) as val from SupportBeanRange#lastevent s0, SupportBean#keepall sb where sb.intPrimitive >= (rangeStart + 1) and intPrimitive <= (rangeEnd - 1)";
        tryAssertion(epService, epl, new SupportBeanRange("R1", 6000, 6005), 6001 + 6002 + 6003 + 6004);

        epl = "select sum(intPrimitive) as val from SupportBeanRange#lastevent s0, SupportBean#keepall sb where sb.intPrimitive >= 6001 and intPrimitive <= (rangeEnd - 1)";
        tryAssertion(epService, epl, new SupportBeanRange("R1", 6000, 6005), 6001 + 6002 + 6003 + 6004);

        epl = "select sum(intPrimitive) as val from SupportBeanRange#lastevent s0, SupportBean#keepall sb where sb.intPrimitive between (rangeStart + 1) and (rangeEnd - 1)";
        tryAssertion(epService, epl, new SupportBeanRange("R1", 6000, 6005), 6001 + 6002 + 6003 + 6004);

        epl = "select sum(intPrimitive) as val from SupportBeanRange#lastevent s0, SupportBean#keepall sb where sb.intPrimitive between (rangeStart + 1) and 6004";
        tryAssertion(epService, epl, new SupportBeanRange("R1", 6000, 6005), 6001 + 6002 + 6003 + 6004);

        epl = "select sum(intPrimitive) as val from SupportBeanRange#lastevent s0, SupportBean#keepall sb where sb.intPrimitive in (6001 : (rangeEnd - 1)]";
        tryAssertion(epService, epl, new SupportBeanRange("R1", 6000, 6005), 6002 + 6003 + 6004);
    }

    private void tryAssertion(EPServiceProvider epService, String epl, Object theEvent, Object expected) {

        String[] fields = "val".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // preload
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(theEvent);
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{expected});
        }
        long delta = System.currentTimeMillis() - startTime;
        assertTrue("delta=" + delta, delta < 500);
        log.info("delta=" + delta);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private static final Logger log = LoggerFactory.getLogger(ExecJoin2StreamExprPerformance.class);
}
