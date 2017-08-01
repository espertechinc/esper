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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExecEventMapProperties implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionArrayProperty(epService);
        runAssertionMappedProperty(epService);
        runAssertionsMapNamePropertyNested(epService);
        runAssertionMapNameProperty(epService);
    }

    private void runAssertionArrayProperty(EPServiceProvider epService) {
        // test map containing first-level property that is an array of primitive or Class
        Map<String, Object> arrayDef = ExecEventMap.makeMap(new Object[][]{{"p0", int[].class}, {"p1", SupportBean[].class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyArrayMap", arrayDef);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0[0] as a, p0[1] as b, p1[0].intPrimitive as c, p1[1] as d, p0 as e from MyArrayMap");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        int[] p0 = new int[]{1, 2, 3};
        SupportBean[] beans = new SupportBean[]{new SupportBean("e1", 5), new SupportBean("e2", 6)};
        Map<String, Object> theEvent = ExecEventMap.makeMap(new Object[][]{{"p0", p0}, {"p1", beans}});
        epService.getEPRuntime().sendEvent(theEvent, "MyArrayMap");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 5, beans[1], p0});
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(SupportBean.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(int[].class, stmt.getEventType().getPropertyType("e"));
        stmt.destroy();

        // test map at the second level of a nested map that is an array of primitive or Class
        Map<String, Object> arrayDefOuter = ExecEventMap.makeMap(new Object[][]{{"outer", arrayDef}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyArrayMapOuter", arrayDefOuter);

        stmt = epService.getEPAdministrator().createEPL("select outer.p0[0] as a, outer.p0[1] as b, outer.p1[0].intPrimitive as c, outer.p1[1] as d, outer.p0 as e from MyArrayMapOuter");
        stmt.addListener(listener);

        Map<String, Object> eventOuter = ExecEventMap.makeMap(new Object[][]{{"outer", theEvent}});
        epService.getEPRuntime().sendEvent(eventOuter, "MyArrayMapOuter");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d".split(","), new Object[]{1, 2, 5, beans[1]});
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(SupportBean.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(int[].class, stmt.getEventType().getPropertyType("e"));

        stmt.destroy();
    }

    private void runAssertionMappedProperty(EPServiceProvider epService) {
        // test map containing first-level property that is an array of primitive or Class
        Map<String, Object> mappedDef = ExecEventMap.makeMap(new Object[][]{{"p0", Map.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMappedPropertyMap", mappedDef);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0('k1') as a from MyMappedPropertyMap");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<String, Object> eventVal = new HashMap<String, Object>();
        eventVal.put("k1", "v1");
        Map<String, Object> theEvent = ExecEventMap.makeMap(new Object[][]{{"p0", eventVal}});
        epService.getEPRuntime().sendEvent(theEvent, "MyMappedPropertyMap");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a".split(","), new Object[]{"v1"});
        assertEquals(Object.class, stmt.getEventType().getPropertyType("a"));
        stmt.destroy();

        // test map at the second level of a nested map that is an array of primitive or Class
        Map<String, Object> mappedDefOuter = ExecEventMap.makeMap(new Object[][]{{"outer", mappedDef}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMappedPropertyMapOuter", mappedDefOuter);

        stmt = epService.getEPAdministrator().createEPL("select outer.p0('k1') as a from MyMappedPropertyMapOuter");
        stmt.addListener(listener);

        Map<String, Object> eventOuter = ExecEventMap.makeMap(new Object[][]{{"outer", theEvent}});
        epService.getEPRuntime().sendEvent(eventOuter, "MyMappedPropertyMapOuter");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a".split(","), new Object[]{"v1"});
        assertEquals(Object.class, stmt.getEventType().getPropertyType("a"));

        // test map that contains a bean which has a map property
        Map<String, Object> mappedDefOuterTwo = ExecEventMap.makeMap(new Object[][]{{"outerTwo", SupportBeanComplexProps.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMappedPropertyMapOuterTwo", mappedDefOuterTwo);

        stmt = epService.getEPAdministrator().createEPL("select outerTwo.mapProperty('xOne') as a from MyMappedPropertyMapOuterTwo");
        stmt.addListener(listener);

        Map<String, Object> eventOuterTwo = ExecEventMap.makeMap(new Object[][]{{"outerTwo", SupportBeanComplexProps.makeDefaultBean()}});
        epService.getEPRuntime().sendEvent(eventOuterTwo, "MyMappedPropertyMapOuterTwo");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a".split(","), new Object[]{"yOne"});
        assertEquals(String.class, stmt.getEventType().getPropertyType("a"));

        stmt.destroy();
    }

    private void runAssertionsMapNamePropertyNested(EPServiceProvider epService) {
        // create a named map
        Map<String, Object> namedDef = ExecEventMap.makeMap(new Object[][]{{"n0", int.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyNamedMap", namedDef);

        // create a map using the name
        Map<String, Object> eventDef = ExecEventMap.makeMap(new Object[][]{{"p0", "MyNamedMap"}, {"p1", "MyNamedMap[]"}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapWithAMap", eventDef);

        // test named-map at the second level of a nested map
        Map<String, Object> arrayDefOuter = ExecEventMap.makeMap(new Object[][]{{"outer", eventDef}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyArrayMapTwo", arrayDefOuter);

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select outer.p0.n0 as a, outer.p1[0].n0 as b, outer.p1[1].n0 as c, outer.p0 as d, outer.p1 as e from MyArrayMapTwo");
        stmt.addListener(listener);

        Map<String, Object> n0_1 = ExecEventMap.makeMap(new Object[][]{{"n0", 1}});
        Map<String, Object> n0_21 = ExecEventMap.makeMap(new Object[][]{{"n0", 2}});
        Map<String, Object> n0_22 = ExecEventMap.makeMap(new Object[][]{{"n0", 3}});
        Map[] n0_2 = new Map[]{n0_21, n0_22};
        Map<String, Object> theEvent = ExecEventMap.makeMap(new Object[][]{{"p0", n0_1}, {"p1", n0_2}});
        Map<String, Object> eventOuter = ExecEventMap.makeMap(new Object[][]{{"outer", theEvent}});
        epService.getEPRuntime().sendEvent(eventOuter, "MyArrayMapTwo");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 3, n0_1, n0_2});
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(Map.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(Map[].class, stmt.getEventType().getPropertyType("e"));

        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select outer.p0.n0? as a, outer.p1[0].n0? as b, outer.p1[1]?.n0 as c, outer.p0? as d, outer.p1? as e from MyArrayMapTwo");
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(eventOuter, "MyArrayMapTwo");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 3, n0_1, n0_2});
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("a"));

        stmt.destroy();
    }

    private void runAssertionMapNameProperty(EPServiceProvider epService) {
        // create a named map
        Map<String, Object> namedDef = ExecEventMap.makeMap(new Object[][]{{"n0", int.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyNamedMap", namedDef);

        // create a map using the name
        Map<String, Object> eventDef = ExecEventMap.makeMap(new Object[][]{{"p0", "MyNamedMap"}, {"p1", "MyNamedMap[]"}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapWithAMap", eventDef);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0.n0 as a, p1[0].n0 as b, p1[1].n0 as c, p0 as d, p1 as e from MyMapWithAMap");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<String, Object> n0_1 = ExecEventMap.makeMap(new Object[][]{{"n0", 1}});
        Map<String, Object> n0_21 = ExecEventMap.makeMap(new Object[][]{{"n0", 2}});
        Map<String, Object> n0_22 = ExecEventMap.makeMap(new Object[][]{{"n0", 3}});
        Map[] n0_2 = new Map[]{n0_21, n0_22};
        Map<String, Object> theEvent = ExecEventMap.makeMap(new Object[][]{{"p0", n0_1}, {"p1", n0_2}});
        epService.getEPRuntime().sendEvent(theEvent, "MyMapWithAMap");

        EventBean eventResult = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(eventResult, "a,b,c,d".split(","), new Object[]{1, 2, 3, n0_1});
        Map[] valueE = (Map[]) eventResult.get("e");
        assertEquals(valueE[0], n0_2[0]);
        assertEquals(valueE[1], n0_2[1]);

        assertEquals(Integer.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(Map.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(Map[].class, stmt.getEventType().getPropertyType("e"));
    }
}
