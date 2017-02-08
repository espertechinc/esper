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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Map;

public class TestCompositeSelect extends TestCase
{
    public void testFollowedByFilter()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("A", SupportBean_A.class.getName());
        config.addEventType("B", SupportBean_B.class.getName());
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String stmtTxtOne = "insert into StreamOne select * from pattern [a=A -> b=B]";
        epService.getEPAdministrator().createEPL(stmtTxtOne);

        SupportUpdateListener listener = new SupportUpdateListener();
        String stmtTxtTwo = "select *, 1 as code from StreamOne";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTxtTwo);
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        EventBean theEvent = listener.assertOneGetNewAndReset();

        Object[] values = new Object[stmtTwo.getEventType().getPropertyNames().length];
        int count = 0;
        for (String name : stmtTwo.getEventType().getPropertyNames())
        {
            values[count++] = theEvent.get(name);
        }

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("a", SupportBean_A.class, null, false, false, false, false, true),
                new EventPropertyDescriptor("b", SupportBean_B.class, null, false, false, false, false, true)
        }, ((EPServiceProviderSPI) epService).getEventAdapterService().getExistsTypeByName("StreamOne").getPropertyDescriptors());
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testFragment()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("A", SupportBean_A.class.getName());
        config.addEventType("B", SupportBean_B.class.getName());

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String stmtTxtOne = "select * from pattern [[2] a=A -> b=B]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTxtOne);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("a", SupportBean_A[].class, SupportBean_A.class, false, false, true, false, true),
                new EventPropertyDescriptor("b", SupportBean_B.class, null, false, false, false, false, true)
        }, stmt.getEventType().getPropertyDescriptors());

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertTrue(theEvent.getUnderlying() instanceof Map);

        // test fragment B type and event
        FragmentEventType typeFragB = theEvent.getEventType().getFragmentType("b");
        assertFalse(typeFragB.isIndexed());
        assertEquals("B", typeFragB.getFragmentType().getName());
        assertEquals(String.class, typeFragB.getFragmentType().getPropertyType("id"));

        EventBean eventFragB = (EventBean) theEvent.getFragment("b");
        assertEquals("B", eventFragB.getEventType().getName());

        // test fragment A type and event
        FragmentEventType typeFragA = theEvent.getEventType().getFragmentType("a");
        assertTrue(typeFragA.isIndexed());
        assertEquals("A", typeFragA.getFragmentType().getName());
        assertEquals(String.class, typeFragA.getFragmentType().getPropertyType("id"));

        assertTrue(theEvent.getFragment("a") instanceof EventBean[]);
        EventBean eventFragA1 = (EventBean) theEvent.getFragment("a[0]");
        assertEquals("A", eventFragA1.getEventType().getName());
        assertEquals("A1", eventFragA1.get("id"));
        EventBean eventFragA2 = (EventBean) theEvent.getFragment("a[1]");
        assertEquals("A", eventFragA2.getEventType().getName());
        assertEquals("A2", eventFragA2.get("id"));
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }
}
