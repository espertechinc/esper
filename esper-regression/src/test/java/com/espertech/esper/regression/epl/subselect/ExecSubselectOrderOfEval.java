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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecSubselectOrderOfEval implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setShareViews(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionCorrelatedSubqueryOrder(epService);
        runAssertionOrderOfEvaluationSubselectFirst(epService);
    }

    private void runAssertionCorrelatedSubqueryOrder(EPServiceProvider epService) {
        // ESPER-564
        epService.getEPAdministrator().getConfiguration().addEventType("TradeEvent", TradeEvent.class);
        SupportUpdateListener listener = new SupportUpdateListener();

        epService.getEPAdministrator().createEPL("select * from TradeEvent#lastevent");

        epService.getEPAdministrator().createEPL(
                "select window(tl.*) as longItems, " +
                        "       (SELECT window(ts.*) AS shortItems FROM TradeEvent#time(20 minutes) as ts WHERE ts.securityID=tl.securityID) " +
                        "from TradeEvent#time(20 minutes) as tl " +
                        "where tl.securityID = 1000" +
                        "group by tl.securityID "
        ).addListener(listener);

        epService.getEPRuntime().sendEvent(new TradeEvent(System.currentTimeMillis(), 1000, 50, 1));
        assertEquals(1, ((Object[]) listener.assertOneGetNew().get("longItems")).length);
        assertEquals(1, ((Object[]) listener.assertOneGetNew().get("shortItems")).length);
        listener.reset();

        epService.getEPRuntime().sendEvent(new TradeEvent(System.currentTimeMillis() + 10, 1000, 50, 1));
        assertEquals(2, ((Object[]) listener.assertOneGetNew().get("longItems")).length);
        assertEquals(2, ((Object[]) listener.assertOneGetNew().get("shortItems")).length);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOrderOfEvaluationSubselectFirst(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        String epl = "select * from SupportBean(intPrimitive<10) where intPrimitive not in (select intPrimitive from SupportBean#unique(intPrimitive))";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(epl);
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        assertFalse(listener.getAndClearIsInvoked());

        stmtOne.destroy();

        String eplTwo = "select * from SupportBean where intPrimitive not in (select intPrimitive from SupportBean(intPrimitive<10)#unique(intPrimitive))";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(eplTwo);
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        assertFalse(listener.getAndClearIsInvoked());

        stmtTwo.destroy();
    }

    public static class TradeEvent {
        private long time;
        private int securityID;
        private double price;
        private long volume;

        public TradeEvent(long time, int securityID, double price, long volume) {
            this.time = time;
            this.securityID = securityID;
            this.price = price;
            this.volume = volume;
        }

        public int getSecurityID() {
            return securityID;
        }

        public long getTime() {
            return time;
        }

        public double getPrice() {
            return price;
        }

        public long getVolume() {
            return volume;
        }
    }
}