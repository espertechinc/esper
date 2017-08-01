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
package com.espertech.esper.regression.event.objectarray;

import com.espertech.esper.client.ConfigurationException;
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

import static com.espertech.esper.regression.event.map.ExecEventMap.makeMap;
import static org.junit.Assert.*;

public class ExecEventObjectArrayEventNested implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInvalid(epService);
        runAssertionArrayProperty(epService);
        runAssertionMappedProperty(epService);
        runAssertionMapNamePropertyNested(epService);
        runAssertionMapNameProperty(epService);
        runAssertionObjectArrayNested(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        // can add the same nested type twice
        epService.getEPAdministrator().getConfiguration().addEventType("ABC", new String[]{"p0"}, new Class[]{int.class});
        epService.getEPAdministrator().getConfiguration().addEventType("ABC", new String[]{"p0"}, new Class[]{int.class});
        try {
            // changing the definition however stops the compatibility
            epService.getEPAdministrator().getConfiguration().addEventType("ABC", new String[]{"p0"}, new Class[]{long.class});
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Event type named 'ABC' has already been declared with differing column name or type information: Type by name 'ABC' in property 'p0' expected class java.lang.Integer but receives class java.lang.Long", ex.getMessage());
        }

        tryInvalid(epService, new String[]{"a"}, new Object[]{new SupportBean()}, "Nestable type configuration encountered an unexpected property type of 'SupportBean' for property 'a', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type");
    }

    private void runAssertionArrayProperty(EPServiceProvider epService) {

        // test map containing first-level property that is an array of primitive or Class
        String[] props = {"p0", "p1"};
        Object[] types = {int[].class, SupportBean[].class};
        epService.getEPAdministrator().getConfiguration().addEventType("MyArrayOA", props, types);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0[0] as a, p0[1] as b, p1[0].intPrimitive as c, p1[1] as d, p0 as e from MyArrayOA");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        int[] p0 = new int[]{1, 2, 3};
        SupportBean[] beans = new SupportBean[]{new SupportBean("e1", 5), new SupportBean("e2", 6)};
        Object[] eventData = new Object[]{p0, beans};
        epService.getEPRuntime().sendEvent(eventData, "MyArrayOA");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 5, beans[1], p0});
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(SupportBean.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(int[].class, stmt.getEventType().getPropertyType("e"));
        stmt.destroy();

        // test map at the second level of a nested map that is an array of primitive or Class
        epService.getEPAdministrator().getConfiguration().addEventType("MyArrayOAMapOuter", new String[]{"outer"}, new Object[]{"MyArrayOA"});

        stmt = epService.getEPAdministrator().createEPL("select outer.p0[0] as a, outer.p0[1] as b, outer.p1[0].intPrimitive as c, outer.p1[1] as d, outer.p0 as e from MyArrayOAMapOuter");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[]{eventData}, "MyArrayOAMapOuter");

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
        Map<String, Object> mappedDef = makeMap(new Object[][]{{"p0", Map.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMappedPropertyMap", mappedDef);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0('k1') as a from MyMappedPropertyMap");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<String, Object> eventVal = new HashMap<String, Object>();
        eventVal.put("k1", "v1");
        Map<String, Object> theEvent = makeMap(new Object[][]{{"p0", eventVal}});
        epService.getEPRuntime().sendEvent(theEvent, "MyMappedPropertyMap");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a".split(","), new Object[]{"v1"});
        assertEquals(Object.class, stmt.getEventType().getPropertyType("a"));
        stmt.destroy();

        // test map at the second level of a nested map that is an array of primitive or Class
        Map<String, Object> mappedDefOuter = makeMap(new Object[][]{{"outer", mappedDef}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMappedPropertyMapOuter", mappedDefOuter);

        stmt = epService.getEPAdministrator().createEPL("select outer.p0('k1') as a from MyMappedPropertyMapOuter");
        stmt.addListener(listener);

        Map<String, Object> eventOuter = makeMap(new Object[][]{{"outer", theEvent}});
        epService.getEPRuntime().sendEvent(eventOuter, "MyMappedPropertyMapOuter");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a".split(","), new Object[]{"v1"});
        assertEquals(Object.class, stmt.getEventType().getPropertyType("a"));

        // test map that contains a bean which has a map property
        Map<String, Object> mappedDefOuterTwo = makeMap(new Object[][]{{"outerTwo", SupportBeanComplexProps.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMappedPropertyMapOuterTwo", mappedDefOuterTwo);

        stmt = epService.getEPAdministrator().createEPL("select outerTwo.mapProperty('xOne') as a from MyMappedPropertyMapOuterTwo");
        stmt.addListener(listener);

        Map<String, Object> eventOuterTwo = makeMap(new Object[][]{{"outerTwo", SupportBeanComplexProps.makeDefaultBean()}});
        epService.getEPRuntime().sendEvent(eventOuterTwo, "MyMappedPropertyMapOuterTwo");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a".split(","), new Object[]{"yOne"});
        assertEquals(String.class, stmt.getEventType().getPropertyType("a"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMapNamePropertyNested(EPServiceProvider epService) {
        // create a named map
        Map<String, Object> namedDef = makeMap(new Object[][]{{"n0", int.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyNamedMap", namedDef);

        // create a map using the name
        Map<String, Object> eventDef = makeMap(new Object[][]{{"p0", "MyNamedMap"}, {"p1", "MyNamedMap[]"}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapWithAMap", eventDef);

        // test named-map at the second level of a nested map
        epService.getEPAdministrator().getConfiguration().addEventType("MyObjectArrayMapOuter", new String[]{"outer"}, new Object[]{eventDef});

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select outer.p0.n0 as a, outer.p1[0].n0 as b, outer.p1[1].n0 as c, outer.p0 as d, outer.p1 as e from MyObjectArrayMapOuter");
        stmt.addListener(listener);

        Map<String, Object> n0_1 = makeMap(new Object[][]{{"n0", 1}});
        Map<String, Object> n0_21 = makeMap(new Object[][]{{"n0", 2}});
        Map<String, Object> n0_22 = makeMap(new Object[][]{{"n0", 3}});
        Map[] n0_2 = new Map[]{n0_21, n0_22};
        Map<String, Object> theEvent = makeMap(new Object[][]{{"p0", n0_1}, {"p1", n0_2}});
        epService.getEPRuntime().sendEvent(new Object[]{theEvent}, "MyObjectArrayMapOuter");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 3, n0_1, n0_2});
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(Map.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(Map[].class, stmt.getEventType().getPropertyType("e"));

        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select outer.p0.n0? as a, outer.p1[0].n0? as b, outer.p1[1]?.n0 as c, outer.p0? as d, outer.p1? as e from MyObjectArrayMapOuter");
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new Object[]{theEvent}, "MyObjectArrayMapOuter");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 3, n0_1, n0_2});
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("a"));

        stmt.destroy();
    }

    private void runAssertionMapNameProperty(EPServiceProvider epService) {

        // create a named map
        Map<String, Object> namedDef = makeMap(new Object[][]{{"n0", int.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyNamedMap", namedDef);

        // create a map using the name
        epService.getEPAdministrator().getConfiguration().addEventType("MyOAWithAMap", new String[]{"p0", "p1"}, new Object[]{"MyNamedMap", "MyNamedMap[]"});

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0.n0 as a, p1[0].n0 as b, p1[1].n0 as c, p0 as d, p1 as e from MyOAWithAMap");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<String, Object> n0_1 = makeMap(new Object[][]{{"n0", 1}});
        Map<String, Object> n0_21 = makeMap(new Object[][]{{"n0", 2}});
        Map<String, Object> n0_22 = makeMap(new Object[][]{{"n0", 3}});
        Map[] n0_2 = new Map[]{n0_21, n0_22};
        epService.getEPRuntime().sendEvent(new Object[]{n0_1, n0_2}, "MyOAWithAMap");

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

        stmt.destroy();
    }

    private void runAssertionObjectArrayNested(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev1", new String[]{"p1id"}, new Object[]{int.class});
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev0", new String[]{"p0id", "p1"}, new Object[]{int.class, "TypeLev1"});
        epService.getEPAdministrator().getConfiguration().addEventType("TypeRoot", new String[]{"rootId", "p0"}, new Object[]{int.class, "TypeLev0"});

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TypeRoot#lastevent");
        Object[] dataLev1 = {1000};
        Object[] dataLev0 = {100, dataLev1};
        epService.getEPRuntime().sendEvent(new Object[]{10, dataLev0}, "TypeRoot");
        EventBean theEvent = stmt.iterator().next();
        EPAssertionUtil.assertProps(theEvent, "rootId,p0.p0id,p0.p1.p1id".split(","), new Object[]{10, 100, 1000});

        stmt.destroy();
    }

    private void tryInvalid(EPServiceProvider epService, String[] names, Object[] types, String message) {
        try {
            epService.getEPAdministrator().getConfiguration().addEventType("NestedMap", names, types);
            fail();
        } catch (Exception ex) {
            // Comment-in: log.error(ex.getMessage(), ex);
            assertTrue("expected '" + message + "' but received '" + ex.getMessage(), ex.getMessage().contains(message));
        }
    }
}
