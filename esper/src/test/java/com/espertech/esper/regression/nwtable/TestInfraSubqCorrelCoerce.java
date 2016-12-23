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

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class TestInfraSubqCorrelCoerce extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S0Bean", SupportBean_S0.class);
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testSubqueryIndex() {
        // named window tests
        runAssertion(true, false, false, false); // no share
        runAssertion(true, false, false, true); // no share create index
        runAssertion(true, true, false, false); // share
        runAssertion(true, true, false, true); // share create index
        runAssertion(true, true, true, false); // disable share
        runAssertion(true, true, true, true); // disable share create index

        // table tests
        runAssertion(false, false, false, false); // table
        runAssertion(false, false, false, true); // table + create index
    }

    private void runAssertion(boolean namedWindow, boolean enableIndexShareCreate, boolean disableIndexShareConsumer, boolean createExplicitIndex) {
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
        consumeStmt.addListener(listener);

        sendWindow("W1", 10L, "c31");
        sendEvent("E1", 10, "c31");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "W1"});

        sendEvent("E2", 11, "c32");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", null});

        sendWindow("W2", 11L, "c32");
        sendEvent("E3", 11, "c32");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", "W2"});

        sendWindow("W3", 11L, "c31");
        sendWindow("W4", 10L, "c32");

        sendEvent("E4", 11, "c31");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4", "W3"});

        sendEvent("E5", 10, "c31");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5", "W1"});

        sendEvent("E6", 10, "c32");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E6", "W4"});

        // test late start
        consumeStmt.destroy();
        consumeStmt = epService.getEPAdministrator().createEPL(consumeEpl);
        consumeStmt.addListener(listener);

        sendEvent("E6", 10, "c32");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E6", "W4"});

        if (stmtIndex != null) {
            stmtIndex.destroy();
        }
        consumeStmt.destroy();

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void sendWindow(String col0, long col1, String col2) {
        HashMap<String, Object> theEvent = new LinkedHashMap<String, Object>();
        theEvent.put("col0", col0);
        theEvent.put("col1", col1);
        theEvent.put("col2", col2);
        if (EventRepresentationEnum.getEngineDefault(epService).isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(theEvent.values().toArray(), "WindowSchema");
        }
        else {
            epService.getEPRuntime().sendEvent(theEvent, "WindowSchema");
        }
    }

    private void sendEvent(String e0, int e1, String e2) {
        HashMap<String, Object> theEvent = new LinkedHashMap<String, Object>();
        theEvent.put("e0", e0);
        theEvent.put("e1", e1);
        theEvent.put("e2", e2);
        if (EventRepresentationEnum.getEngineDefault(epService).isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(theEvent.values().toArray(), "EventSchema");
        }
        else {
            epService.getEPRuntime().sendEvent(theEvent, "EventSchema");
        }
    }
}
