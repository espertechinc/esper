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

package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import com.espertech.esper.support.util.SupportModelHelper;
import junit.framework.TestCase;

public class TestFirstEverLastEverAgg extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        config.addEventType("SupportBean_A", SupportBean_A.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testFirstEverLastEver()
    {
        runAssertionFirstLastEver(true);
        runAssertionFirstLastEver(false);

        SupportMessageAssertUtil.tryInvalid(epService, "select countever(distinct intPrimitive) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'countever(distinct intPrimitive)': Aggregation function 'countever' does now allow distinct [");
    }

    public void testOnDelete()
    {
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall() as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean_A delete from MyWindow where theString = id");

        String[] fields = "firsteverstring,lasteverstring,counteverall".split(",");
        String epl = "select firstever(theString) as firsteverstring, " +
                "lastever(theString) as lasteverstring," +
                "countever(*) as counteverall from MyWindow";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E2", 2L});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E3", 3L});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E3", 3L});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E3", 3L});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E3", 3L});
    }

    private void runAssertionFirstLastEver(boolean soda) {
        String[] fields = "firsteverstring,firsteverint,lasteverstring,lasteverint,counteverstar,counteverexpr,counteverexprfilter".split(",");

        String epl = "select " +
                "firstever(theString) as firsteverstring, " +
                "lastever(theString) as lasteverstring, " +
                "firstever(intPrimitive) as firsteverint, " +
                "lastever(intPrimitive) as lasteverint, " +
                "countever(*) as counteverstar, " +
                "countever(intBoxed) as counteverexpr, " +
                "countever(intBoxed,boolPrimitive) as counteverexprfilter " +
                "from SupportBean#length(2)";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        stmt.addListener(listener);

        makeSendBean("E1", 10, 100, true);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, "E1", 10, 1L, 1L, 1L});

        makeSendBean("E2", 11, null, true);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, "E2", 11, 2L, 1L, 1L});

        makeSendBean("E3", 12, 120, false);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, "E3", 12, 3L, 2L, 1L});

        stmt.destroy();
    }

    private void makeSendBean(String theString, int intPrimitive, Integer intBoxed, boolean boolPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setIntBoxed(intBoxed);
        sb.setBoolPrimitive(boolPrimitive);
        epService.getEPRuntime().sendEvent(sb);
    }
}