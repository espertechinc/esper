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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecNWTableInfraSubqUncorrel implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ABean", SupportBean_S0.class);

        // named window tests
        runAssertion(epService, true, false, false); // testNoShare
        runAssertion(epService, true, true, false); // testShare
        runAssertion(epService, true, true, true); // testDisableShare

        // table tests
        runAssertion(epService, false, false, false);
    }

    private void runAssertion(EPServiceProvider epService, boolean namedWindow, boolean enableIndexShareCreate, boolean disableIndexShareConsumer) {
        String stmtTextCreate = namedWindow ?
                "create window MyInfra#keepall as select theString as a, longPrimitive as b, longBoxed as c from " + SupportBean.class.getName() :
                "create table MyInfra(a string primary key, b long, c long)";
        if (enableIndexShareCreate) {
            stmtTextCreate = "@Hint('enable_window_subquery_indexshare') " + stmtTextCreate;
        }
        // create window
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyInfra select theString as a, longPrimitive as b, longBoxed as c from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String stmtTextSelectOne = "select irstream (select a from MyInfra) as value, symbol from " + SupportMarketDataBean.class.getName();
        if (disableIndexShareConsumer) {
            stmtTextSelectOne = "@Hint('disable_window_subquery_indexshare') " + stmtTextSelectOne;
        }
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);
        EPAssertionUtil.assertEqualsAnyOrder(stmtSelectOne.getEventType().getPropertyNames(), new String[]{"value", "symbol"});
        assertEquals(String.class, stmtSelectOne.getEventType().getPropertyType("value"));
        assertEquals(String.class, stmtSelectOne.getEventType().getPropertyType("symbol"));

        sendMarketBean(epService, "M1");
        String[] fieldsStmt = new String[]{"value", "symbol"};
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{null, "M1"});

        sendSupportBean(epService, "S1", 1L, 2L);
        assertFalse(listenerStmtOne.isInvoked());
        String[] fieldsWin = new String[]{"a", "b", "c"};
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsWin, new Object[]{"S1", 1L, 2L});
        } else {
            assertFalse(listenerWindow.isInvoked());
        }

        // create consumer 2 -- note that this one should not start empty now
        String stmtTextSelectTwo = "select irstream (select a from MyInfra) as value, symbol from " + SupportMarketDataBean.class.getName();
        if (disableIndexShareConsumer) {
            stmtTextSelectTwo = "@Hint('disable_window_subquery_indexshare') " + stmtTextSelectTwo;
        }
        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL(stmtTextSelectTwo);
        SupportUpdateListener listenerStmtTwo = new SupportUpdateListener();
        stmtSelectTwo.addListener(listenerStmtTwo);

        sendMarketBean(epService, "M1");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{"S1", "M1"});
        EPAssertionUtil.assertProps(listenerStmtTwo.assertOneGetNewAndReset(), fieldsStmt, new Object[]{"S1", "M1"});

        sendSupportBean(epService, "S2", 10L, 20L);
        assertFalse(listenerStmtOne.isInvoked());
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsWin, new Object[]{"S2", 10L, 20L});
        }

        sendMarketBean(epService, "M2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{null, "M2"});
        assertFalse(listenerWindow.isInvoked());
        EPAssertionUtil.assertProps(listenerStmtTwo.assertOneGetNewAndReset(), fieldsStmt, new Object[]{null, "M2"});

        // create delete stmt
        String stmtTextDelete = "on " + SupportBean_A.class.getName() + " delete from MyInfra where id = a";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        // delete S1
        epService.getEPRuntime().sendEvent(new SupportBean_A("S1"));
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fieldsWin, new Object[]{"S1", 1L, 2L});
        }

        sendMarketBean(epService, "M3");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{"S2", "M3"});
        EPAssertionUtil.assertProps(listenerStmtTwo.assertOneGetNewAndReset(), fieldsStmt, new Object[]{"S2", "M3"});

        // delete S2
        epService.getEPRuntime().sendEvent(new SupportBean_A("S2"));
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fieldsWin, new Object[]{"S2", 10L, 20L});
        }

        sendMarketBean(epService, "M4");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{null, "M4"});
        EPAssertionUtil.assertProps(listenerStmtTwo.assertOneGetNewAndReset(), fieldsStmt, new Object[]{null, "M4"});

        sendSupportBean(epService, "S3", 100L, 200L);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsWin, new Object[]{"S3", 100L, 200L});
        }

        sendMarketBean(epService, "M5");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsStmt, new Object[]{"S3", "M5"});
        EPAssertionUtil.assertProps(listenerStmtTwo.assertOneGetNewAndReset(), fieldsStmt, new Object[]{"S3", "M5"});
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, long longPrimitive, Long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongPrimitive(longPrimitive);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketBean(EPServiceProvider epService, String symbol) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
        epService.getEPRuntime().sendEvent(bean);
    }
}
