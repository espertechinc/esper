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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class ExecNWTableInfraSubqCorrelCoerce implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S0Bean", SupportBean_S0.class);

        // named window tests
        runAssertion(epService, true, false, false, false); // no share
        runAssertion(epService, true, false, false, true); // no share create index
        runAssertion(epService, true, true, false, false); // share
        runAssertion(epService, true, true, false, true); // share create index
        runAssertion(epService, true, true, true, false); // disable share
        runAssertion(epService, true, true, true, true); // disable share create index

        // table tests
        runAssertion(epService, false, false, false, false); // table
        runAssertion(epService, false, false, false, true); // table + create index
    }

    private void runAssertion(EPServiceProvider epService, boolean namedWindow, boolean enableIndexShareCreate, boolean disableIndexShareConsumer, boolean createExplicitIndex) {
        epService.getEPAdministrator().createEPL("create schema EventSchema(e0 string, e1 int, e2 string)");
        epService.getEPAdministrator().createEPL("create schema WindowSchema(col0 string, col1 long, col2 string)");

        String createEpl = namedWindow ?
                "create window MyInfra#keepall as WindowSchema" :
                "create table MyInfra (col0 string primary key, col1 long, col2 string)";
        if (enableIndexShareCreate) {
            createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
        }
        epService.getEPAdministrator().createEPL(createEpl);
        epService.getEPAdministrator().createEPL("insert into MyInfra select * from WindowSchema");

        EPStatement stmtIndex = null;
        if (createExplicitIndex) {
            stmtIndex = epService.getEPAdministrator().createEPL("create index MyIndex on MyInfra (col2, col1)");
        }

        String[] fields = "e0,val".split(",");
        String consumeEpl = "select e0, (select col0 from MyInfra where col2 = es.e2 and col1 = es.e1) as val from EventSchema es";
        if (disableIndexShareConsumer) {
            consumeEpl = "@Hint('disable_window_subquery_indexshare') " + consumeEpl;
        }
        EPStatement consumeStmt = epService.getEPAdministrator().createEPL(consumeEpl);
        SupportUpdateListener listener = new SupportUpdateListener();
        consumeStmt.addListener(listener);

        sendWindow(epService, "W1", 10L, "c31");
        sendEvent(epService, "E1", 10, "c31");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "W1"});

        sendEvent(epService, "E2", 11, "c32");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", null});

        sendWindow(epService, "W2", 11L, "c32");
        sendEvent(epService, "E3", 11, "c32");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", "W2"});

        sendWindow(epService, "W3", 11L, "c31");
        sendWindow(epService, "W4", 10L, "c32");

        sendEvent(epService, "E4", 11, "c31");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4", "W3"});

        sendEvent(epService, "E5", 10, "c31");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5", "W1"});

        sendEvent(epService, "E6", 10, "c32");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E6", "W4"});

        // test late start
        consumeStmt.destroy();
        consumeStmt = epService.getEPAdministrator().createEPL(consumeEpl);
        consumeStmt.addListener(listener);

        sendEvent(epService, "E6", 10, "c32");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E6", "W4"});

        if (stmtIndex != null) {
            stmtIndex.destroy();
        }
        consumeStmt.destroy();

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void sendWindow(EPServiceProvider epService, String col0, long col1, String col2) {
        HashMap<String, Object> theEvent = new LinkedHashMap<String, Object>();
        theEvent.put("col0", col0);
        theEvent.put("col1", col1);
        theEvent.put("col2", col2);
        if (EventRepresentationChoice.getEngineDefault(epService).isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(theEvent.values().toArray(), "WindowSchema");
        } else {
            epService.getEPRuntime().sendEvent(theEvent, "WindowSchema");
        }
    }

    private void sendEvent(EPServiceProvider epService, String e0, int e1, String e2) {
        HashMap<String, Object> theEvent = new LinkedHashMap<String, Object>();
        theEvent.put("e0", e0);
        theEvent.put("e1", e1);
        theEvent.put("e2", e2);
        if (EventRepresentationChoice.getEngineDefault(epService).isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(theEvent.values().toArray(), "EventSchema");
        } else {
            epService.getEPRuntime().sendEvent(theEvent, "EventSchema");
        }
    }
}
