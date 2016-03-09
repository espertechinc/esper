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

package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.MappedEventBean;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.map.MapEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class TestMapObjectArrayInterUse extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    // test ObjectArray event with Map, Map[], MapType and MapType[] properties
    public void testObjectArrayWithMap()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("MapType", Collections.<String, Object>singletonMap("im", String.class));
        epService.getEPAdministrator().getConfiguration().addEventType("OAType", "p0,p1,p2,p3".split(","), new Object[] {String.class, "MapType", "MapType[]", Collections.<String, Object>singletonMap("om", String.class)});

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0 as c0, p1.im as c1, p2[0].im as c2, p3.om as c3 from OAType");
        stmt.addListener(listener);
        
        epService.getEPRuntime().sendEvent(new Object[] {"E1", Collections.singletonMap("im", "IM1"), new Map[] {Collections.singletonMap("im", "IM2")}, Collections.singletonMap("om", "OM1")}, "OAType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2,c3".split(","), new Object[]{"E1", "IM1", "IM2", "OM1"});

        epService.getEPAdministrator().destroyAllStatements();
        
        // test inserting from array to map
        epService.getEPAdministrator().createEPL("insert into MapType(im) select p0 from OAType").addListener(listener);
        epService.getEPRuntime().sendEvent(new Object[]{"E1",null,null,null}, "OAType");
        assertTrue(listener.assertOneGetNew() instanceof MappedEventBean);
        assertEquals("E1", listener.assertOneGetNew().get("im"));
    }

    // test Map event with ObjectArrayType and ObjectArrayType[] properties
    public void testMapWithObjectArray()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("OAType", "p0,p1".split(","), new Object[] {String.class, Integer.class});
        Map<String, Object> def = new HashMap<String, Object>();
        def.put("oa1", "OAType");
        def.put("oa2", "OAType[]");
        epService.getEPAdministrator().getConfiguration().addEventType("MapType", def);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select oa1.p0 as c0, oa1.p1 as c1, oa2[0].p0 as c2, oa2[1].p1 as c3 from MapType");
        stmt.addListener(listener);
        
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("oa1", new Object[] {"A", 100});
        data.put("oa2", new Object[][] { {"B", 200}, {"C", 300} });
        epService.getEPRuntime().sendEvent(data, "MapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2,c3".split(","), new Object[] {"A", 100, "B", 300});

        epService.getEPAdministrator().destroyAllStatements();
        
        // test inserting from map to array
        epService.getEPAdministrator().createEPL("insert into OAType select 'a' as p0, 1 as p1 from MapType").addListener(listener);
        epService.getEPRuntime().sendEvent(data, "MapType");
        assertTrue(listener.assertOneGetNew() instanceof ObjectArrayBackedEventBean);
        assertEquals("a", listener.assertOneGetNew().get("p0"));
        assertEquals(1, listener.assertOneGetNew().get("p1"));
    }
}
