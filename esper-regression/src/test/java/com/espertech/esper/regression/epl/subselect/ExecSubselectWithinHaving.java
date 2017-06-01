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
package com.espertech.esper.regression.epl.subselect;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;

public class ExecSubselectWithinHaving implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("S0", SupportBean_S0.class);
        configuration.addEventType("S1", SupportBean_S1.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(MaxAmountEvent.class);
        runAssertionHavingSubselectWithGroupBy(epService, true);
        runAssertionHavingSubselectWithGroupBy(epService, false);
    }

    private void runAssertionHavingSubselectWithGroupBy(EPServiceProvider epService, boolean namedWindow) {
        String eplCreate = namedWindow ?
                "create window MyInfra#unique(key) as MaxAmountEvent" :
                "create table MyInfra(key string primary key, maxAmount double)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfra select * from MaxAmountEvent");

        String stmtText = "select theString as c0, sum(intPrimitive) as c1 " +
                "from SupportBean#groupwin(theString)#length(2) as sb " +
                "group by theString " +
                "having sum(intPrimitive) > (select maxAmount from MyInfra as mw where sb.theString = mw.key)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "c0,c1".split(",");

        // set some amounts
        epService.getEPRuntime().sendEvent(new MaxAmountEvent("G1", 10));
        epService.getEPRuntime().sendEvent(new MaxAmountEvent("G2", 20));
        epService.getEPRuntime().sendEvent(new MaxAmountEvent("G3", 30));

        // send some events
        epService.getEPRuntime().sendEvent(new SupportBean("G1", 5));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 19));
        epService.getEPRuntime().sendEvent(new SupportBean("G3", 28));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 21});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 18));
        epService.getEPRuntime().sendEvent(new SupportBean("G1", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("G3", 2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G3", 29));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G3", 31});

        epService.getEPRuntime().sendEvent(new SupportBean("G3", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G3", 33});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 6));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("G3", 26));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 99));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 105});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 100});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    public final static class MaxAmountEvent {
        private String key;
        private double maxAmount;

        private MaxAmountEvent(String key, double maxAmount) {
            this.key = key;
            this.maxAmount = maxAmount;
        }

        public String getKey() {
            return key;
        }

        public double getMaxAmount() {
            return maxAmount;
        }
    }
}
