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
package com.espertech.esper.regression.event.infra;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecEventInfraPropertyIndexedKeyExpr implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {

        runAssertionOA(epService);
        runAssertionMap(epService);
        runAssertionWrapper(epService);
        runAssertionBean(epService);
    }

    private void runAssertionBean(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(MyIndexMappedSamplerBean.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyIndexMappedSamplerBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new MyIndexMappedSamplerBean());

        EventBean event = listener.assertOneGetNewAndReset();
        EventType type = event.getEventType();
        assertEquals(2, type.getGetterIndexed("listOfInt").get(event, 1));
        assertEquals(2, type.getGetterIndexed("iterableOfInt").get(event, 1));
    }

    private void runAssertionWrapper(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select {1, 2} as arr, *, Collections.singletonMap('A', 2) as mapped from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean event = listener.assertOneGetNewAndReset();
        EventType type = event.getEventType();
        assertEquals(2, type.getGetterIndexed("arr").get(event, 1));
        assertEquals(2, type.getGetterMapped("mapped").get(event, "A"));

        stmt.destroy();
    }

    private void runAssertionMap(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create schema MapEventInner(p0 string)");
        epService.getEPAdministrator().createEPL("create schema MapEvent(intarray int[], mapinner MapEventInner[])");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MapEvent");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map[] mapinner = new Map[] {Collections.singletonMap("p0", "A"), Collections.singletonMap("p0", "B")};
        Map map = new HashMap();
        map.put("intarray", new int[] {1, 2});
        map.put("mapinner", mapinner);
        epService.getEPRuntime().sendEvent(map, "MapEvent");
        EventBean event = listener.assertOneGetNewAndReset();
        EventType type = event.getEventType();
        assertEquals(2, type.getGetterIndexed("intarray").get(event, 1));
        assertNull(type.getGetterIndexed("dummy"));
        assertEquals(mapinner[1], type.getGetterIndexed("mapinner").get(event, 1));

        stmt.destroy();
    }

    private void runAssertionOA(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create objectarray schema OAEventInner(p0 string)");
        epService.getEPAdministrator().createEPL("create objectarray schema OAEvent(intarray int[], oainner OAEventInner[])");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from OAEvent");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Object[] oainner = new Object[] {new Object[] {"A"}, new Object[] {"B"}};
        epService.getEPRuntime().sendEvent(new Object[] {new int[] {1, 2}, oainner}, "OAEvent");
        EventBean event = listener.assertOneGetNewAndReset();
        EventType type = event.getEventType();
        assertEquals(2, type.getGetterIndexed("intarray").get(event, 1));
        assertNull(type.getGetterIndexed("dummy"));
        assertEquals(oainner[1], type.getGetterIndexed("oainner").get(event, 1));

        stmt.destroy();
    }

    public final static class MyIndexMappedSamplerBean {
        private final List<Integer> intlist = Arrays.asList(1, 2);

        public List<Integer> getListOfInt() {
            return intlist;
        }

        public Iterable<Integer> getIterableOfInt() {
            return intlist;
        }
    }
}
