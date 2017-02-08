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
package com.espertech.esper.regression.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestSubselectMultirow extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();        
        config.addEventType("SupportBean", SupportBean.class);
        config.addEventType("S0", SupportBean_S0.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testMultirowSingleColumn()
    {
        // test named window as well as stream
        epService.getEPAdministrator().createEPL("create window SupportWindow#length(3) as SupportBean");
        epService.getEPAdministrator().createEPL("insert into SupportWindow select * from SupportBean");

        String stmtText = "select p00, (select window(intPrimitive) from SupportBean#keepall sb) as val from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        String[] fields = "p00,val".split(",");

        Object[][] rows = new Object[][] {
                {"p00", String.class},
                {"val", int[].class}
                };
        for (int i = 0; i < rows.length; i++) {
            String message = "Failed assertion for " + rows[i][0];
            EventPropertyDescriptor prop = stmt.getEventType().getPropertyDescriptors()[i];
            assertEquals(message, rows[i][0], prop.getPropertyName());
            assertEquals(message, rows[i][1], prop.getPropertyType());
        }

        epService.getEPRuntime().sendEvent(new SupportBean("T1", 5));
        epService.getEPRuntime().sendEvent(new SupportBean("T2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("T3", 15));
        epService.getEPRuntime().sendEvent(new SupportBean("T1", 6));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, new int[]{5, 10, 15, 6}});

        // test named window and late start
        stmt.destroy();

        stmtText = "select p00, (select window(intPrimitive) from SupportWindow) as val from S0 as s0";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, new int[]{10, 15, 6}});   // length window 3

        epService.getEPRuntime().sendEvent(new SupportBean("T1", 5));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, new int[]{15, 6, 5}});   // length window 3
    }

    public void testMultirowUnderlyingCorrelated()
    {
        String stmtText = "select p00, " +
                "(select window(sb.*) from SupportBean#keepall sb where theString = s0.p00) as val " +
                "from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        Object[][] rows = new Object[][] {
                {"p00", String.class},
                {"val", SupportBean[].class}
                };
        for (int i = 0; i < rows.length; i++) {
            String message = "Failed assertion for " + rows[i][0];
            EventPropertyDescriptor prop = stmt.getEventType().getPropertyDescriptors()[i];
            assertEquals(message, rows[i][0], prop.getPropertyName());
            assertEquals(message, rows[i][1], prop.getPropertyType());
        }

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "T1"));
        assertNull(listener.assertOneGetNewAndReset().get("val"));

        SupportBean sb1 = new SupportBean("T1", 10);
        epService.getEPRuntime().sendEvent(sb1);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "T1"));

        EventBean received = listener.assertOneGetNewAndReset();
        assertEquals(SupportBean[].class, received.get("val").getClass());
        EPAssertionUtil.assertEqualsAnyOrder((Object[]) received.get("val"), new Object[]{sb1});

        SupportBean sb2 = new SupportBean("T2", 20);
        epService.getEPRuntime().sendEvent(sb2);
        SupportBean sb3 = new SupportBean("T2", 30);
        epService.getEPRuntime().sendEvent(sb3);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "T2"));

        received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertEqualsAnyOrder((Object[]) received.get("val"), new Object[]{sb2, sb3});
    }
}
