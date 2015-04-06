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

package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.hook.VirtualDataWindow;
import com.espertech.esper.client.hook.VirtualDataWindowEventConsumerAdd;
import com.espertech.esper.client.hook.VirtualDataWindowEventConsumerBase;
import com.espertech.esper.client.hook.VirtualDataWindowEventConsumerRemove;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.virtualdw.SupportVirtualDW;
import com.espertech.esper.support.virtualdw.SupportVirtualDWFactory;
import junit.framework.TestCase;

import javax.naming.NamingException;
import java.util.Collections;

public class TestVirtualDataWindowLateConsume extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addPlugInVirtualDataWindow("test", "vdw", SupportVirtualDWFactory.class.getName(), SupportVirtualDW.ITERATE);    // configure with iteration
        configuration.addEventType("SupportBean", SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown()
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testInsertConsume() {

        epService.getEPAdministrator().createEPL("create window MyVDW.test:vdw() as SupportBean");
        SupportVirtualDW window = (SupportVirtualDW) getFromContext("/virtualdw/MyVDW");
        SupportBean supportBean = new SupportBean("S1", 100);
        window.setData(Collections.singleton(supportBean));
        epService.getEPAdministrator().createEPL("insert into MyVDW select * from SupportBean");

        // test aggregated consumer - wherein the virtual data window does not return an iterator that prefills the aggregation state
        String[] fields = "val0".split(",");
        EPStatement stmtAggregate = epService.getEPAdministrator().createEPL("@Name('ABC') select sum(intPrimitive) as val0 from MyVDW");
        stmtAggregate.addListener(listener);
        EPAssertionUtil.assertProps(stmtAggregate.iterator().next(), fields, new Object[]{100});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{110});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{130});

        // assert events received for add-consumer and remove-consumer
        stmtAggregate.destroy();
        VirtualDataWindowEventConsumerAdd addConsumerEvent = (VirtualDataWindowEventConsumerAdd) window.getEvents().get(0);
        VirtualDataWindowEventConsumerRemove removeConsumerEvent = (VirtualDataWindowEventConsumerRemove) window.getEvents().get(1);

        for (VirtualDataWindowEventConsumerBase base : new VirtualDataWindowEventConsumerBase[] {addConsumerEvent, removeConsumerEvent}) {
            assertEquals(-1, base.getAgentInstanceId());
            assertEquals("MyVDW", base.getNamedWindowName());
            assertEquals("ABC", base.getStatementName());
        }
        assertSame(removeConsumerEvent.getConsumerObject(), addConsumerEvent.getConsumerObject());
        window.getEvents().clear();

        // test filter criteria passed to event
        EPStatement stmtAggregateWFilter = epService.getEPAdministrator().createEPL("@Name('ABC') select sum(intPrimitive) as val0 from MyVDW(theString = 'A')");
        VirtualDataWindowEventConsumerAdd eventWithFilter = (VirtualDataWindowEventConsumerAdd) window.getEvents().get(0);
        assertEquals(1, eventWithFilter.getFilterExpressions().length);
        assertNotNull(eventWithFilter.getExprEvaluatorContext());
        stmtAggregateWFilter.destroy();
    }

    private VirtualDataWindow getFromContext(String name) {
        try {
            return (VirtualDataWindow) epService.getContext().lookup(name);
        }
        catch (NamingException e) {
            throw new RuntimeException("Name '" + name + "' could not be looked up");
        }
    }
}
