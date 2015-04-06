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

package com.espertech.esper.regression.context;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestContextWDeclaredExpression extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        listener = new SupportUpdateListener();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testDeclaredExpression() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().createEPL("create context MyCtx as " +
                "group by intPrimitive < 0 as n, " +
                "group by intPrimitive > 0 as p " +
                "from SupportBean");
        epService.getEPAdministrator().createEPL("create expression getLabelOne { context.label }");
        epService.getEPAdministrator().createEPL("create expression getLabelTwo { 'x'||context.label||'x' }");

        epService.getEPAdministrator().createEPL("expression getLabelThree { context.label } " +
                "context MyCtx " +
                "select getLabelOne() as c0, getLabelTwo() as c1, getLabelThree() as c2 from SupportBean").addListener(listener);

        runAssertionExpression();
    }

    public void testAliasExpression() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().createEPL("create context MyCtx as " +
                "group by intPrimitive < 0 as n, " +
                "group by intPrimitive > 0 as p " +
                "from SupportBean");
        epService.getEPAdministrator().createEPL("create expression getLabelOne alias for { context.label }");
        epService.getEPAdministrator().createEPL("create expression getLabelTwo alias for { 'x'||context.label||'x' }");
        epService.getEPAdministrator().createEPL("expression getLabelThree alias for { context.label } " +
                "context MyCtx " +
                "select getLabelOne as c0, getLabelTwo as c1, getLabelThree as c2 from SupportBean").addListener(listener);

        runAssertionExpression();
    }

    private void runAssertionExpression() {
        String[] fields = "c0,c1,c2".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", -2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"n", "xnx", "n"});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"p", "xpx", "p"});
    }
}
