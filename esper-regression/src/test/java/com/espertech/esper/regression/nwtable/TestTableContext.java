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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import junit.framework.TestCase;

public class TestTableContext extends TestCase {
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        for (Class clazz : new Class[] {SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        listener = new SupportUpdateListener();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testContextVariants() {
        runAssertionPartitioned();
        runAssertionNonOverlapping();

        runInvalidAssertion();
    }

    private void runInvalidAssertion() {
        epService.getEPAdministrator().createEPL("create context SimpleCtx start after 1 sec end after 1 sec");
        epService.getEPAdministrator().createEPL("context SimpleCtx create table MyTable(pkey string primary key, thesum sum(int), col0 string)");

        SupportMessageAssertUtil.tryInvalid(epService, "select * from MyTable",
                "Error starting statement: Table by name 'MyTable' has been declared for context 'SimpleCtx' and can only be used within the same context [");
        SupportMessageAssertUtil.tryInvalid(epService, "select (select * from MyTable) from SupportBean",
                "Error starting statement: Failed to plan subquery number 1 querying MyTable: Mismatch in context specification, the context for the table 'MyTable' is 'SimpleCtx' and the query specifies no context  [select (select * from MyTable) from SupportBean]");
        SupportMessageAssertUtil.tryInvalid(epService, "insert into MyTable select theString as pkey from SupportBean",
                "Error starting statement: Table by name 'MyTable' has been declared for context 'SimpleCtx' and can only be used within the same context [");
    }

    private void runAssertionNonOverlapping() {
        epService.getEPAdministrator().createEPL("create context CtxNowTillS0 start @now end SupportBean_S0");
        epService.getEPAdministrator().createEPL("context CtxNowTillS0 create table MyTable(pkey string primary key, thesum sum(int), col0 string)");
        epService.getEPAdministrator().createEPL("context CtxNowTillS0 into table MyTable select sum(intPrimitive) as thesum from SupportBean group by theString");
        epService.getEPAdministrator().createEPL("context CtxNowTillS0 select pkey as c0, thesum as c1 from MyTable output snapshot when terminated").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 50));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 60));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1)); // terminated
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), "c0,c1".split(","),
                new Object[][] {{"E1", 110}, {"E2", 20}});

        epService.getEPAdministrator().createEPL("context CtxNowTillS0 create index MyIdx on MyTable(col0)");
        epService.getEPAdministrator().createEPL("context CtxNowTillS0 select * from MyTable, SupportBean_S1 where col0 = p11");

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 90));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 30));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1)); // terminated
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), "c0,c1".split(","),
                new Object[][] {{"E1", 30}, {"E3", 100}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void runAssertionPartitioned() {
        epService.getEPAdministrator().createEPL("create context CtxPerString " +
                "partition by theString from SupportBean, p00 from SupportBean_S0");
        epService.getEPAdministrator().createEPL("context CtxPerString create table MyTable(thesum sum(int))");
        epService.getEPAdministrator().createEPL("context CtxPerString into table MyTable select sum(intPrimitive) as thesum from SupportBean");
        epService.getEPAdministrator().createEPL("context CtxPerString select MyTable.thesum as c0 from SupportBean_S0").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 50));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 60));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E1"));
        assertEquals(110, listener.assertOneGetNewAndReset().get("c0"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E2"));
        assertEquals(20, listener.assertOneGetNewAndReset().get("c0"));

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_MyTable__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_MyTable__public", false);
    }
}
