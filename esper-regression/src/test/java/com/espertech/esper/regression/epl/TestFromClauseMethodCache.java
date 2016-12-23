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
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.epl.SupportStaticMethodInvocations;

public class TestFromClauseMethodCache extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testLRUCache()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        ConfigurationMethodRef methodConfig = new ConfigurationMethodRef();
        methodConfig.setLRUCache(3);
        config.addMethodRef(SupportStaticMethodInvocations.class.getName(), methodConfig);
        config.addImport(SupportStaticMethodInvocations.class.getPackage().getName() + ".*");

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();

        String joinStatement = "select id, p00, theString from " +
                SupportBean.class.getName() + "()#length(100) as s1, " +
                " method:SupportStaticMethodInvocations.fetchObjectLog(theString, intPrimitive)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);

        // set sleep off
        SupportStaticMethodInvocations.getInvocationSizeReset();

        // The LRU cache caches per same keys
        String[] fields = new String[] {"id", "p00", "theString"};
        sendBeanEvent("E1", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, "|E1|", "E1"});
        
        sendBeanEvent("E2", 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, "|E2|", "E2"});

        sendBeanEvent("E3", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, "|E3|", "E3"});
        assertEquals(3, SupportStaticMethodInvocations.getInvocationSizeReset());

        // should be cached
        sendBeanEvent("E3", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, "|E3|", "E3"});
        assertEquals(0, SupportStaticMethodInvocations.getInvocationSizeReset());

        // should not be cached
        sendBeanEvent("E4", 4);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{4, "|E4|", "E4"});
        assertEquals(1, SupportStaticMethodInvocations.getInvocationSizeReset());

        // should be cached
        sendBeanEvent("E2", 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, "|E2|", "E2"});
        assertEquals(0, SupportStaticMethodInvocations.getInvocationSizeReset());

        // should not be cached
        sendBeanEvent("E1", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, "|E1|", "E1"});
        assertEquals(1, SupportStaticMethodInvocations.getInvocationSizeReset());

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testExpiryCache()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        ConfigurationMethodRef methodConfig = new ConfigurationMethodRef();
        methodConfig.setExpiryTimeCache(1, 10);
        config.addMethodRef(SupportStaticMethodInvocations.class.getName(), methodConfig);
        config.addImport(SupportStaticMethodInvocations.class.getPackage().getName() + ".*");

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();

        String joinStatement = "select id, p00, theString from " +
                SupportBean.class.getName() + "()#length(100) as s1, " +
                " method:SupportStaticMethodInvocations.fetchObjectLog(theString, intPrimitive)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);

        // set sleep off
        SupportStaticMethodInvocations.getInvocationSizeReset();

        sendTimer(1000);
        String[] fields = new String[] {"id", "p00", "theString"};
        sendBeanEvent("E1", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, "|E1|", "E1"});

        sendTimer(1500);
        sendBeanEvent("E2", 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, "|E2|", "E2"});

        sendTimer(2000);
        sendBeanEvent("E3", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, "|E3|", "E3"});
        assertEquals(3, SupportStaticMethodInvocations.getInvocationSizeReset());

        // should be cached
        sendBeanEvent("E3", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, "|E3|", "E3"});
        assertEquals(0, SupportStaticMethodInvocations.getInvocationSizeReset());

        sendTimer(2100);
        // should not be cached
        sendBeanEvent("E4", 4);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{4, "|E4|", "E4"});
        assertEquals(1, SupportStaticMethodInvocations.getInvocationSizeReset());

        // should be cached
        sendBeanEvent("E2", 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, "|E2|", "E2"});
        assertEquals(0, SupportStaticMethodInvocations.getInvocationSizeReset());

        // should not be cached
        sendBeanEvent("E1", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, "|E1|", "E1"});
        assertEquals(1, SupportStaticMethodInvocations.getInvocationSizeReset());

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendBeanEvent(String theString, int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
