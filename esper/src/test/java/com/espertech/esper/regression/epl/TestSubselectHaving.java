/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestSubselectHaving extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();        
        config.addEventType("SupportBean", SupportBean.class);
        config.addEventType("S0", SupportBean_S0.class);
        config.addEventType("S1", SupportBean_S1.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testHavingSubselectWithGroupBy() {
        epService.getEPAdministrator().getConfiguration().addEventType(MaxAmountEvent.class);
        runAssertionHavingSubselectWithGroupBy(true);
        runAssertionHavingSubselectWithGroupBy(false);
    }

    private void runAssertionHavingSubselectWithGroupBy(boolean namedWindow)
    {
        String eplCreate = namedWindow ?
                "create window MyInfra.std:unique(key) as MaxAmountEvent" :
                "create table MyInfra(key string primary key, maxAmount double)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfra select * from MaxAmountEvent");

        String stmtText = "select theString as c0, sum(intPrimitive) as c1 " +
                "from SupportBean.std:groupwin(theString).win:length(2) as sb " +
                "group by theString " +
                "having sum(intPrimitive) > (select maxAmount from MyInfra as mw where sb.theString = mw.key)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
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

    private final static class MaxAmountEvent {
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
