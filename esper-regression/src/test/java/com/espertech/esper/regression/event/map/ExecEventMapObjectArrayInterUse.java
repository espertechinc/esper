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
package com.espertech.esper.regression.event.map;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.event.MappedEventBean;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecEventMapObjectArrayInterUse implements RegressionExecution {
    public void run(EPServiceProvider epService) {
        runAssertionObjectArrayWithMap(epService);
        runAssertionMapWithObjectArray(epService);
    }

    // test ObjectArray event with Map, Map[], MapType and MapType[] properties
    private void runAssertionObjectArrayWithMap(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("MapType", Collections.<String, Object>singletonMap("im", String.class));
        epService.getEPAdministrator().getConfiguration().addEventType("OAType", "p0,p1,p2,p3".split(","), new Object[]{String.class, "MapType", "MapType[]", Collections.<String, Object>singletonMap("om", String.class)});

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0 as c0, p1.im as c1, p2[0].im as c2, p3.om as c3 from OAType");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[]{"E1", Collections.singletonMap("im", "IM1"), new Map[]{Collections.singletonMap("im", "IM2")}, Collections.singletonMap("om", "OM1")}, "OAType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2,c3".split(","), new Object[]{"E1", "IM1", "IM2", "OM1"});

        epService.getEPAdministrator().destroyAllStatements();

        // test inserting from array to map
        epService.getEPAdministrator().createEPL("insert into MapType(im) select p0 from OAType").addListener(listener);
        epService.getEPRuntime().sendEvent(new Object[]{"E1", null, null, null}, "OAType");
        assertTrue(listener.assertOneGetNew() instanceof MappedEventBean);
        assertEquals("E1", listener.assertOneGetNewAndReset().get("im"));

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MapType", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("OAType", false);
    }

    // test Map event with ObjectArrayType and ObjectArrayType[] properties
    private void runAssertionMapWithObjectArray(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("OAType", "p0,p1".split(","), new Object[]{String.class, Integer.class});
        Map<String, Object> def = new HashMap<String, Object>();
        def.put("oa1", "OAType");
        def.put("oa2", "OAType[]");
        epService.getEPAdministrator().getConfiguration().addEventType("MapType", def);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select oa1.p0 as c0, oa1.p1 as c1, oa2[0].p0 as c2, oa2[1].p1 as c3 from MapType");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("oa1", new Object[]{"A", 100});
        data.put("oa2", new Object[][]{{"B", 200}, {"C", 300}});
        epService.getEPRuntime().sendEvent(data, "MapType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2,c3".split(","), new Object[]{"A", 100, "B", 300});

        epService.getEPAdministrator().destroyAllStatements();

        // test inserting from map to array
        epService.getEPAdministrator().createEPL("insert into OAType select 'a' as p0, 1 as p1 from MapType").addListener(listener);
        epService.getEPRuntime().sendEvent(data, "MapType");
        assertTrue(listener.assertOneGetNew() instanceof ObjectArrayBackedEventBean);
        assertEquals("a", listener.assertOneGetNew().get("p0"));
        assertEquals(1, listener.assertOneGetNewAndReset().get("p1"));
    }
}
