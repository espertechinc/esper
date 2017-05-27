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
package com.espertech.esper.regression.db;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static org.junit.Assert.*;

public class ExecDatabaseJoinPerfNoCache implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addDatabaseReference("MyDB", configDB);

        EPServiceProvider epServiceRetained = EPServiceProviderManager.getProvider("TestDatabaseJoinRetained", configuration);
        epServiceRetained.initialize();

        configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.POOLED);
        configuration = SupportConfigFactory.getConfiguration();
        configuration.addDatabaseReference("MyDB", configDB);
        EPServiceProvider epServicePooled = EPServiceProviderManager.getProvider("TestDatabaseJoinPooled", configuration);
        epServicePooled.initialize();

        runAssertion100EventsRetained(epServiceRetained);
        runAssertion100EventsPooled(epServicePooled);
        runAssertionSelectRStream(epServiceRetained);
        runAssertionSelectIStream(epServiceRetained);
        runAssertionWhereClauseNoIndexNoCache(epServiceRetained);

        epServicePooled.destroy();
        epServiceRetained.destroy();
    }

    private void runAssertion100EventsRetained(EPServiceProvider epServiceRetained) {
        long startTime = System.currentTimeMillis();
        try100Events(epServiceRetained);
        long endTime = System.currentTimeMillis();
        log.info(".test100EventsRetained delta=" + (endTime - startTime));
        assertTrue(endTime - startTime < 5000);
    }

    private void runAssertion100EventsPooled(EPServiceProvider epServicePooled) {
        long startTime = System.currentTimeMillis();
        try100Events(epServicePooled);
        long endTime = System.currentTimeMillis();
        log.info(".test100EventsPooled delta=" + (endTime - startTime));
        assertTrue(endTime - startTime < 10000);
    }

    private void runAssertionSelectRStream(EPServiceProvider epServiceRetained) {
        String stmtText = "select rstream myvarchar from " +
                SupportBean_S0.class.getName() + "#length(1000) as s0," +
                " sql:MyDB ['select myvarchar from mytesttable where ${id} = mytesttable.mybigint'] as s1";

        EPStatement statement = epServiceRetained.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        // 1000 events should enter the window fast, no joins
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            SupportBean_S0 bean = new SupportBean_S0(10);
            epServiceRetained.getEPRuntime().sendEvent(bean);
            assertFalse(listener.isInvoked());
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        assertTrue("delta=" + delta, endTime - startTime < 1000);

        // 1001st event should finally join and produce a result
        SupportBean_S0 bean = new SupportBean_S0(10);
        epServiceRetained.getEPRuntime().sendEvent(bean);
        assertEquals("J", listener.assertOneGetNewAndReset().get("myvarchar"));

        statement.destroy();
    }

    private void runAssertionSelectIStream(EPServiceProvider epServiceRetained) {
        // set time to zero
        epServiceRetained.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String stmtText = "select istream myvarchar from " +
                SupportBean_S0.class.getName() + "#time(1 sec) as s0," +
                " sql:MyDB ['select myvarchar from mytesttable where ${id} = mytesttable.mybigint'] as s1";

        EPStatement statement = epServiceRetained.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        // Send 100 events which all fireStatementStopped a join
        for (int i = 0; i < 100; i++) {
            SupportBean_S0 bean = new SupportBean_S0(5);
            epServiceRetained.getEPRuntime().sendEvent(bean);
            assertEquals("E", listener.assertOneGetNewAndReset().get("myvarchar"));
        }

        // now advance the time, this should not produce events or join
        long startTime = System.currentTimeMillis();
        epServiceRetained.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        long endTime = System.currentTimeMillis();

        log.info(".testSelectIStream delta=" + (endTime - startTime));
        assertTrue(endTime - startTime < 200);
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private void runAssertionWhereClauseNoIndexNoCache(EPServiceProvider epServiceRetained) {
        String stmtText = "select id, mycol3, mycol2 from " +
                SupportBean_S0.class.getName() + "#keepall as s0," +
                " sql:MyDB ['select mycol3, mycol2 from mytesttable_large'] as s1 where s0.id = s1.mycol3";

        EPStatement statement = epServiceRetained.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        for (int i = 0; i < 20; i++) {
            int num = i + 1;
            String col2 = Integer.toString(Math.round((float) num / 10));
            SupportBean_S0 bean = new SupportBean_S0(num);
            epServiceRetained.getEPRuntime().sendEvent(bean);
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), new String[]{"id", "mycol3", "mycol2"}, new Object[]{num, num, col2});
        }

        statement.destroy();
    }

    private void try100Events(EPServiceProvider engine) {
        String stmtText = "select myint from " +
                SupportBean_S0.class.getName() + " as s0," +
                " sql:MyDB ['select myint from mytesttable where ${id} = mytesttable.mybigint'] as s1";

        EPStatement statement = engine.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        for (int i = 0; i < 100; i++) {
            int id = i % 10 + 1;

            SupportBean_S0 bean = new SupportBean_S0(id);
            engine.getEPRuntime().sendEvent(bean);

            EventBean received = listener.assertOneGetNewAndReset();
            assertEquals(id * 10, received.get("myint"));
        }

        statement.destroy();
    }

    private static final Logger log = LoggerFactory.getLogger(ExecDatabaseJoinPerfNoCache.class);
}
