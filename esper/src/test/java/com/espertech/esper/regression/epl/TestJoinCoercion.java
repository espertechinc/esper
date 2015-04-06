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

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBeanRange;
import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;

public class TestJoinCoercion extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testJoinCoercionRange() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanRange", SupportBeanRange.class);

        String fields[] = "sbs,sbi,sbri".split(",");
        String epl = "select sb.theString as sbs, sb.intPrimitive as sbi, sbr.id as sbri from SupportBean.win:length(10) sb, SupportBeanRange.win:length(10) sbr " +
                "where intPrimitive between rangeStartLong and rangeEndLong";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        
        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("R1", "G", 100L, 200L));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 100, "R1"});

        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("R2", "G", 90L, 100L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 100, "R2"});

        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("R3", "G", 1L, 99L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, "R3"});

        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("R4", "G", 2000L, 3000L));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1000));
        assertFalse(listener.isInvoked());

        stmt.destroy();
        epl = "select sb.theString as sbs, sb.intPrimitive as sbi, sbr.id as sbri from SupportBean.win:length(10) sb, SupportBeanRange.win:length(10) sbr " +
                "where sbr.key = sb.theString and intPrimitive between rangeStartLong and rangeEndLong";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("R1", "G", 100L, 200L));
        epService.getEPRuntime().sendEvent(new SupportBean("G", 10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G", 101));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G", 101, "R1"});

        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("R2", "G", 90L, 102L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G", 101, "R2"});

        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("R3", "G", 1L, 99L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G", 10, "R3"});

        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("R4", "G", 2000L, 3000L));
        epService.getEPRuntime().sendEvent(new SupportBean("G", 1000));
        assertFalse(listener.isInvoked());
    }

    public void testJoinCoercion()
    {
        String joinStatement = "select volume from " +
                SupportMarketDataBean.class.getName() + ".win:length(3) as s0," +
                SupportBean.class.getName() + "().win:length(3) as s1 " +
                " where s0.volume = s1.intPrimitive";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);

        sendBeanEvent(100);
        sendMarketEvent(100);
        assertEquals(100L, listener.assertOneGetNewAndReset().get("volume"));
    }

    private void sendBeanEvent(int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketEvent(long volume)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean("", 0, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }
}
