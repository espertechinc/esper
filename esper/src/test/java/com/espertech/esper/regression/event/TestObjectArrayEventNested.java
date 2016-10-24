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
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TestObjectArrayEventNested extends TestCase
{
    public void testConfiguredViaPropsAndXML() {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(Configuration.EventRepresentation.OBJECTARRAY);
        configuration.addEventType("MyOAType", "bean,theString,map".split(","), new Object[] {SupportBean.class.getName(), "string", "java.util.Map"});

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        EventType eventType = epService.getEPAdministrator().getConfiguration().getEventType("MyOAType");
        assertEquals(Object[].class, eventType.getUnderlyingType());
        assertEquals(String.class, eventType.getPropertyType("theString"));
        assertEquals(Map.class, eventType.getPropertyType("map"));
        assertEquals(SupportBean.class, eventType.getPropertyType("bean"));

        EPStatement stmt = epService.getEPAdministrator().createEPL("select bean, theString, map('key'), bean.theString from MyOAType");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Object[].class, stmt.getEventType().getUnderlyingType());

        SupportBean bean = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(new Object[] {bean, "abc", Collections.singletonMap("key", "value")}, "MyOAType");
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), "bean,theString,map('key'),bean.theString".split(","), new Object[] {bean, "abc", "value", "E1"});

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testObjectArrayTypeUpdate()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(Configuration.EventRepresentation.OBJECTARRAY);

        String[] names = {"base1", "base2"};
        Object[] types = {String.class, makeMap(new Object[][] {{"n1", int.class}})};
        configuration.addEventType("MyOAEvent", names, types);

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        EPStatement statementOne = epService.getEPAdministrator().createEPL(
                "select base1 as v1, base2.n1 as v2, base3? as v3, base2.n2? as v4 from MyOAEvent");
        assertEquals(Object[].class, statementOne.getEventType().getUnderlyingType());
        EPStatement statementOneSelectAll = epService.getEPAdministrator().createEPL("select * from MyOAEvent");
        assertEquals("[base1, base2]", Arrays.toString(statementOneSelectAll.getEventType().getPropertyNames()));
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        statementOne.addListener(listenerOne);
        String[] fields = "v1,v2,v3,v4".split(",");

        epService.getEPRuntime().sendEvent(new Object[] {"abc", makeMap(new Object[][] {{"n1", 10}}), ""}, "MyOAEvent");
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"abc", 10, null, null});

        // update type
        String[] namesNew = {"base3", "base2"};
        Object[] typesNew = new Object[] {Long.class, makeMap(new Object[][] {{"n2", String.class}})};
        epService.getEPAdministrator().getConfiguration().updateObjectArrayEventType("MyOAEvent", namesNew, typesNew);

        EPStatement statementTwo = epService.getEPAdministrator().createEPL("select base1 as v1, base2.n1 as v2, base3 as v3, base2.n2 as v4 from MyOAEvent");
        EPStatement statementTwoSelectAll = epService.getEPAdministrator().createEPL("select * from MyOAEvent");
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        statementTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(new Object[] {"def", makeMap(new Object[][] {{"n1", 9}, {"n2", "xyz"}}), 20L}, "MyOAEvent");
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"def", 9, 20L, "xyz"});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fields, new Object[]{"def", 9, 20L, "xyz"});

        // assert event type
        assertEquals("[base1, base2, base3]", Arrays.toString(statementOneSelectAll.getEventType().getPropertyNames()));
        assertEquals("[base1, base2, base3]", Arrays.toString(statementTwoSelectAll.getEventType().getPropertyNames()));

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("base3", Long.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("base2", Map.class, null, false, false, false, true, false),
                new EventPropertyDescriptor("base1", String.class, null, false, false, false, false, false),
        }, statementTwoSelectAll.getEventType().getPropertyDescriptors());

        try
        {
            epService.getEPAdministrator().getConfiguration().updateObjectArrayEventType("dummy", new String[0], new Object[0]);
            fail();
        }
        catch (ConfigurationException ex)
        {
            assertEquals("Error updating Object-array event type: Event type named 'dummy' has not been declared", ex.getMessage());
        }

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        try
        {
            epService.getEPAdministrator().getConfiguration().updateObjectArrayEventType("SupportBean", new String[0], new Object[0]);
            fail();
        }
        catch (ConfigurationException ex)
        {
            assertEquals("Error updating Object-array event type: Event type by name 'SupportBean' is not an Object-array event type", ex.getMessage());
        }

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testObjectArrayInheritanceInitTime()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();

        configuration.addEventType("RootEvent", new String[] {"base"}, new Object[] {String.class});
        configuration.addEventType("Sub1Event", new String[]{"sub1"}, new Object[]{String.class});
        configuration.addEventType("Sub2Event", new String[] {"sub2"}, new Object[] {String.class});
        configuration.addEventType("SubAEvent", new String[] {"suba"}, new Object[] {String.class});
        configuration.addEventType("SubBEvent", new String[] {"subb"}, new Object[] {String.class});

        configuration.addObjectArraySuperType("Sub1Event", "RootEvent");
        configuration.addObjectArraySuperType("Sub2Event", "RootEvent");
        configuration.addObjectArraySuperType("SubAEvent", "Sub1Event");
        configuration.addObjectArraySuperType("SubBEvent", "SubAEvent");

        try {
            configuration.addObjectArraySuperType("SubBEvent", "Sub2Event");
            fail();
        }
        catch (ConfigurationException ex) {
            assertEquals("Object-array event types may not have multiple supertypes", ex.getMessage());
        }

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        EPAssertionUtil.assertEqualsExactOrder(new Object[]{
                new EventPropertyDescriptor("base", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("sub1", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("suba", String.class, null, false, false, false, false, false),
        }, ((EPServiceProviderSPI) epService).getEventAdapterService().getExistsTypeByName("SubAEvent").getPropertyDescriptors());

        runObjectArrInheritanceAssertion(epService);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testObjectArrayInheritanceRuntime()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        ConfigurationOperations configOps = epService.getEPAdministrator().getConfiguration();
        configOps.addEventType("RootEvent", new String[] {"base"}, new Object[] {String.class});
        configOps.addEventType("Sub1Event", new String[]{"sub1"}, new Object[]{String.class}, new ConfigurationEventTypeObjectArray(Collections.singleton("RootEvent")));
        configOps.addEventType("Sub2Event", new String[]{"sub2"}, new Object[]{String.class}, new ConfigurationEventTypeObjectArray(Collections.singleton("RootEvent")));
        configOps.addEventType("SubAEvent", new String[]{"suba"}, new Object[]{String.class}, new ConfigurationEventTypeObjectArray(Collections.singleton("Sub1Event")));
        configOps.addEventType("SubBEvent", new String[]{"subb"}, new Object[]{String.class}, new ConfigurationEventTypeObjectArray(Collections.singleton("SubAEvent")));

        runObjectArrInheritanceAssertion(epService);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void runObjectArrInheritanceAssertion(EPServiceProvider epService)
    {
        SupportUpdateListener listeners[] = new SupportUpdateListener[5];
        String[] statements = {
                "select base as vbase, sub1? as v1, sub2? as v2, suba? as va, subb? as vb from RootEvent",  // 0
                "select base as vbase, sub1 as v1, sub2? as v2, suba? as va, subb? as vb from Sub1Event",   // 1
                "select base as vbase, sub1? as v1, sub2 as v2, suba? as va, subb? as vb from Sub2Event",   // 2
                "select base as vbase, sub1 as v1, sub2? as v2, suba as va, subb? as vb from SubAEvent",    // 3
                "select base as vbase, sub1? as v1, sub2? as v2, suba? as va, subb as vb from SubBEvent"     // 4
        };
        for (int i = 0; i < statements.length; i++)
        {
            EPStatement statement = epService.getEPAdministrator().createEPL(statements[i]);
            listeners[i] = new SupportUpdateListener();
            statement.addListener(listeners[i]);
        }
        String[] fields = "vbase,v1,v2,va,vb".split(",");

        EventType type = epService.getEPAdministrator().getConfiguration().getEventType("SubAEvent");
        assertEquals("base", type.getPropertyDescriptors()[0].getPropertyName());
        assertEquals("sub1", type.getPropertyDescriptors()[1].getPropertyName());
        assertEquals("suba", type.getPropertyDescriptors()[2].getPropertyName());
        assertEquals(3, type.getPropertyDescriptors().length);

        type = epService.getEPAdministrator().getConfiguration().getEventType("SubBEvent");
        assertEquals("[base, sub1, suba, subb]", Arrays.toString(type.getPropertyNames()));
        assertEquals(4, type.getPropertyDescriptors().length);

        type = epService.getEPAdministrator().getConfiguration().getEventType("Sub1Event");
        assertEquals("[base, sub1]", Arrays.toString(type.getPropertyNames()));
        assertEquals(2, type.getPropertyDescriptors().length);

        type = epService.getEPAdministrator().getConfiguration().getEventType("Sub2Event");
        assertEquals("[base, sub2]", Arrays.toString(type.getPropertyNames()));
        assertEquals(2, type.getPropertyDescriptors().length);

        epService.getEPRuntime().sendEvent(new Object[] {"a","b","x"}, "SubAEvent");    // base, sub1, suba
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", null, "x", null});
        assertFalse(listeners[2].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", null, "x", null});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", null, "x", null});

        epService.getEPRuntime().sendEvent(new Object[] {"f1", "f2", "f4"}, "SubAEvent");
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", null, "f4", null});
        assertFalse(listeners[2].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", null, "f4", null});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", null, "f4", null});

        epService.getEPRuntime().sendEvent(new Object[] {"XBASE", "X1", "X2", "XY"}, "SubBEvent");
        Object[] values = new Object[] {"XBASE","X1",null,"X2","XY"};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[2].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, values);
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, values);
        EPAssertionUtil.assertProps(listeners[4].assertOneGetNewAndReset(), fields, values);

        epService.getEPRuntime().sendEvent(new Object[] {"YBASE","Y1"}, "Sub1Event");
        values = new Object[] {"YBASE","Y1", null, null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[2].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, values);

        epService.getEPRuntime().sendEvent(new Object[] {"YBASE", "Y2"}, "Sub2Event");
        values = new Object[] {"YBASE",null, "Y2", null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[1].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fields, values);

        epService.getEPRuntime().sendEvent(new Object[] {"ZBASE"}, "RootEvent");
        values = new Object[] {"ZBASE",null, null, null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[1].isInvoked() || listeners[2].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());

        // try property not available
        try
        {
            epService.getEPAdministrator().createEPL("select suba from Sub1Event");
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals("Error starting statement: Failed to validate select-clause expression 'suba': Property named 'suba' is not valid in any stream (did you mean 'sub1'?) [select suba from Sub1Event]",ex.getMessage());
        }

        // try supertype not exists
        try
        {
            epService.getEPAdministrator().getConfiguration().addEventType("Sub1Event", makeMap(""), new String[] {"doodle"});
            fail();
        }
        catch (ConfigurationException ex)
        {
            assertEquals("Supertype by name 'doodle' could not be found",ex.getMessage());
        }
    }

    public void testInvalid()
    {
        EPServiceProvider epService = getEngineInitialized(null, null, null);

        // can add the same nested type twice
        epService.getEPAdministrator().getConfiguration().addEventType("ABC", new String[] {"p0"}, new Class[] {int.class});
        epService.getEPAdministrator().getConfiguration().addEventType("ABC", new String[] {"p0"}, new Class[] {int.class});
        try
        {
            // changing the definition however stops the compatibility
            epService.getEPAdministrator().getConfiguration().addEventType("ABC", new String[] {"p0"}, new Class[] {long.class});
            fail();
        }
        catch (ConfigurationException ex)
        {
            assertEquals("Event type named 'ABC' has already been declared with differing column name or type information: Type by name 'ABC' in property 'p0' expected class java.lang.Integer but receives class java.lang.Long", ex.getMessage());
        }
        
        tryInvalid(epService, new String[] {"a"}, new Object[] {new SupportBean()}, "Nestable type configuration encountered an unexpected property type of 'SupportBean' for property 'a', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type");
    }

    public void testNestedPojo()
    {
        Pair<String[], Object[]> pair = getTestDefTwo();
        EPServiceProvider epService = getEngineInitialized("NestedObjectArr", pair.getFirst(), pair.getSecond());

        String statementText = "select " +
                                "simple, object, nodefmap, map, " +
                                "object.id as a1, nodefmap.key1? as a2, nodefmap.key2? as a3, nodefmap.key3?.key4 as a4, " +
                                "map.objectOne as b1, map.simpleOne as b2, map.nodefmapOne.key2? as b3, map.mapOne.simpleTwo? as b4, " +
                                "map.objectOne.indexed[1] as c1, map.objectOne.nested.nestedValue as c2," +
                                "map.mapOne.simpleTwo as d1, map.mapOne.objectTwo as d2, map.mapOne.nodefmapTwo as d3, " +
                                "map.mapOne.mapTwo as e1, map.mapOne.mapTwo.simpleThree as e2, map.mapOne.mapTwo.objectThree as e3, " +
                                "map.mapOne.objectTwo.array[1].mapped('1ma').value as f1, map.mapOne.mapTwo.objectThree.id as f2" +
                                " from NestedObjectArr";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Object[] testdata = getTestDataTwo();
        epService.getEPRuntime().sendEvent(testdata, "NestedObjectArr");

        // test all properties exist
        EventBean received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, "simple,object,nodefmap,map".split(","),
                new Object[]{"abc", new SupportBean_A("A1"), testdata[2], testdata[3]});
        EPAssertionUtil.assertProps(received, "a1,a2,a3,a4".split(","),
                new Object[]{"A1", "val1", null, null});
        EPAssertionUtil.assertProps(received, "b1,b2,b3,b4".split(","),
                new Object[]{getNestedKey(testdata, 3, "objectOne"), 10, "val2", 300});
        EPAssertionUtil.assertProps(received, "c1,c2".split(","), new Object[]{2, "nestedValue"});
        EPAssertionUtil.assertProps(received, "d1,d2,d3".split(","),
                new Object[]{300, getNestedKey(testdata, 3, "mapOne", "objectTwo"), getNestedKey(testdata, 3, "mapOne", "nodefmapTwo")});
        EPAssertionUtil.assertProps(received, "e1,e2,e3".split(","),
                new Object[]{getNestedKey(testdata, 3, "mapOne", "mapTwo"), 4000L, new SupportBean_B("B1")});
        EPAssertionUtil.assertProps(received, "f1,f2".split(","),
                new Object[]{"1ma0", "B1"});

        // assert type info
        EPStatement stmt = epService.getEPAdministrator().createEPL(("select * from NestedObjectArr"));
        EventType eventType = stmt.getEventType();

        String[] propertiesReceived = eventType.getPropertyNames();
        String[] propertiesExpected = new String[] {"simple", "object", "nodefmap", "map"};
        EPAssertionUtil.assertEqualsAnyOrder(propertiesReceived, propertiesExpected);
        assertEquals(String.class, eventType.getPropertyType("simple"));
        assertEquals(Map.class, eventType.getPropertyType("map"));
        assertEquals(Map.class, eventType.getPropertyType("nodefmap"));
        assertEquals(SupportBean_A.class, eventType.getPropertyType("object"));

        assertNull(eventType.getPropertyType("map.mapOne.simpleOne"));

        // nested POJO with generic return type
        listener.reset();
        epService.getEPAdministrator().getConfiguration().addEventType("MyNested", new String[] {"bean"}, new Object[] {MyNested.class});
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select * from MyNested(bean.insides.anyOf(i=>id = 'A'))");
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[] {new MyNested(Arrays.asList(new MyInside[] {new MyInside("A")}))}, "MyNested");
        assertTrue(listener.isInvoked());
    }

    public void testArrayProperty()
    {
        EPServiceProvider epService = getEngineInitialized(null, null, null);

        // test map containing first-level property that is an array of primitive or Class
        String[] props = {"p0", "p1"};
        Object[] types = {int[].class, SupportBean[].class };
        epService.getEPAdministrator().getConfiguration().addEventType("MyArrayOA", props, types);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0[0] as a, p0[1] as b, p1[0].intPrimitive as c, p1[1] as d, p0 as e from MyArrayOA");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        int[] p0 = new int[] {1, 2, 3};
        SupportBean[] beans = new SupportBean[] {new SupportBean("e1", 5), new SupportBean("e2", 6)};
        Object[] eventData = new Object[] {p0, beans};
        epService.getEPRuntime().sendEvent(eventData, "MyArrayOA");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 5, beans[1], p0});
        assertEquals(int.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(SupportBean.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(int[].class, stmt.getEventType().getPropertyType("e"));
        stmt.destroy();

        // test map at the second level of a nested map that is an array of primitive or Class
        epService.getEPAdministrator().getConfiguration().addEventType("MyArrayOAMapOuter", new String[] {"outer"}, new Object[] {"MyArrayOA"});

        stmt = epService.getEPAdministrator().createEPL("select outer.p0[0] as a, outer.p0[1] as b, outer.p1[0].intPrimitive as c, outer.p1[1] as d, outer.p0 as e from MyArrayOAMapOuter");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[] {eventData}, "MyArrayOAMapOuter");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d".split(","), new Object[]{1, 2, 5, beans[1]});
        assertEquals(int.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(SupportBean.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(int[].class, stmt.getEventType().getPropertyType("e"));
    }

    public void testMappedProperty()
    {
        EPServiceProvider epService = getEngineInitialized(null, null, null);

        // test map containing first-level property that is an array of primitive or Class
        Map<String, Object> mappedDef = makeMap(new Object[][] {{"p0", Map.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMappedPropertyMap", mappedDef);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0('k1') as a from MyMappedPropertyMap");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<String, Object> eventVal = new HashMap<String, Object>();
        eventVal.put("k1", "v1");
        Map<String, Object> theEvent = makeMap(new Object[][] {{"p0", eventVal}});
        epService.getEPRuntime().sendEvent(theEvent, "MyMappedPropertyMap");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a".split(","), new Object[]{"v1"});
        assertEquals(Object.class, stmt.getEventType().getPropertyType("a"));
        stmt.destroy();

        // test map at the second level of a nested map that is an array of primitive or Class
        Map<String, Object> mappedDefOuter = makeMap(new Object[][] {{"outer", mappedDef}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMappedPropertyMapOuter", mappedDefOuter);

        stmt = epService.getEPAdministrator().createEPL("select outer.p0('k1') as a from MyMappedPropertyMapOuter");
        stmt.addListener(listener);

        Map<String, Object> eventOuter = makeMap(new Object[][] {{"outer", theEvent}});
        epService.getEPRuntime().sendEvent(eventOuter, "MyMappedPropertyMapOuter");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a".split(","), new Object[]{"v1"});
        assertEquals(Object.class, stmt.getEventType().getPropertyType("a"));

        // test map that contains a bean which has a map property
        Map<String, Object> mappedDefOuterTwo = makeMap(new Object[][] {{"outerTwo", SupportBeanComplexProps.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMappedPropertyMapOuterTwo", mappedDefOuterTwo);

        stmt = epService.getEPAdministrator().createEPL("select outerTwo.mapProperty('xOne') as a from MyMappedPropertyMapOuterTwo");
        stmt.addListener(listener);

        Map<String, Object> eventOuterTwo = makeMap(new Object[][] {{"outerTwo", SupportBeanComplexProps.makeDefaultBean()}});
        epService.getEPRuntime().sendEvent(eventOuterTwo, "MyMappedPropertyMapOuterTwo");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a".split(","), new Object[]{"yOne"});
        assertEquals(String.class, stmt.getEventType().getPropertyType("a"));        
    }

    public void testMapNamePropertyNested()
    {
        EPServiceProvider epService = getEngineInitialized(null, null, null);

        // create a named map
        Map<String, Object> namedDef = makeMap(new Object[][] {{"n0", int.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyNamedMap", namedDef);

        // create a map using the name
        Map<String, Object> eventDef = makeMap(new Object[][] {{"p0", "MyNamedMap"}, {"p1", "MyNamedMap[]"}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapWithAMap", eventDef);

        // test named-map at the second level of a nested map
        epService.getEPAdministrator().getConfiguration().addEventType("MyObjectArrayMapOuter", new String[] {"outer"}, new Object[] {eventDef});

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select outer.p0.n0 as a, outer.p1[0].n0 as b, outer.p1[1].n0 as c, outer.p0 as d, outer.p1 as e from MyObjectArrayMapOuter");
        stmt.addListener(listener);

        Map<String, Object> n0_1 = makeMap(new Object[][] {{"n0", 1}});
        Map<String, Object> n0_21 = makeMap(new Object[][] {{"n0", 2}});
        Map<String, Object> n0_22 = makeMap(new Object[][] {{"n0", 3}});
        Map[] n0_2 = new Map[] {n0_21, n0_22};
        Map<String, Object> theEvent = makeMap(new Object[][] {{"p0", n0_1}, {"p1", n0_2 }});
        epService.getEPRuntime().sendEvent(new Object[] {theEvent}, "MyObjectArrayMapOuter");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 3, n0_1, n0_2});
        assertEquals(int.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(Map.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(Map[].class, stmt.getEventType().getPropertyType("e"));

        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select outer.p0.n0? as a, outer.p1[0].n0? as b, outer.p1[1]?.n0 as c, outer.p0? as d, outer.p1? as e from MyObjectArrayMapOuter");
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new Object[] {theEvent}, "MyObjectArrayMapOuter");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 3, n0_1, n0_2});
        assertEquals(int.class, stmt.getEventType().getPropertyType("a"));
    }

    public void testMapNameProperty()
    {
        EPServiceProvider epService = getEngineInitialized(null, null, null);

        // create a named map
        Map<String, Object> namedDef = makeMap(new Object[][] {{"n0", int.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyNamedMap", namedDef);

        // create a map using the name
        epService.getEPAdministrator().getConfiguration().addEventType("MyOAWithAMap", new String[] {"p0", "p1"}, new Object[] {"MyNamedMap", "MyNamedMap[]"});

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0.n0 as a, p1[0].n0 as b, p1[1].n0 as c, p0 as d, p1 as e from MyOAWithAMap");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<String, Object> n0_1 = makeMap(new Object[][] {{"n0", 1}});
        Map<String, Object> n0_21 = makeMap(new Object[][] {{"n0", 2}});
        Map<String, Object> n0_22 = makeMap(new Object[][] {{"n0", 3}});
        Map[] n0_2 = new Map[] {n0_21, n0_22};
        epService.getEPRuntime().sendEvent(new Object[] {n0_1, n0_2}, "MyOAWithAMap");

        EventBean eventResult = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(eventResult, "a,b,c,d".split(","), new Object[]{1, 2, 3, n0_1});
        Map[] valueE = (Map[]) eventResult.get("e");
        assertEquals(valueE[0], n0_2[0]);
        assertEquals(valueE[1], n0_2[1]);

        assertEquals(int.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(Map.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(Map[].class, stmt.getEventType().getPropertyType("e"));
    }

    public void testObjectArrayNested() {
        EPServiceProvider epService = getEngineInitialized(null, null, null);
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev1", new String[] {"p1id"}, new Object[] {int.class});
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev0", new String[] {"p0id", "p1"}, new Object[] {int.class, "TypeLev1"});
        epService.getEPAdministrator().getConfiguration().addEventType("TypeRoot", new String[] {"rootId", "p0"}, new Object[] {int.class, "TypeLev0"});
        
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TypeRoot.std:lastevent()");
        Object[] dataLev1 = {1000};
        Object[] dataLev0 = {100, dataLev1};
        epService.getEPRuntime().sendEvent(new Object[] {10, dataLev0}, "TypeRoot");
        EventBean theEvent = stmt.iterator().next();
        EPAssertionUtil.assertProps(theEvent, "rootId,p0.p0id,p0.p1.p1id".split(","), new Object[] {10, 100, 1000});
    }

    private void tryInvalid(EPServiceProvider epService, String[] names, Object[] types, String message)
    {
        try
        {
            epService.getEPAdministrator().getConfiguration().addEventType("NestedMap", names, types);
            fail();
        }
        catch (Exception ex)
        {
            log.error(ex.getMessage(), ex);
            assertTrue("expected '" + message + "' but received '" + ex.getMessage(), ex.getMessage().contains(message));
        }
    }

    private Object getNestedKey(Object[] array, int index, String keyTwo)
    {
        Map map = (Map) array[index];
        return map.get(keyTwo);
    }

    private Object getNestedKey(Object[] array, int index, String keyTwo, String keyThree)
    {
        Map map = (Map) array[index];
        map = (Map) map.get(keyTwo);
        return map.get(keyThree);
    }

    private Object[] getTestDataTwo()
    {
        Map<String, Object> levelThree = makeMap(new Object[][] {
                {"simpleThree", 4000L},
                {"objectThree", new SupportBean_B("B1")},
        });

        Map<String, Object> levelTwo = makeMap(new Object[][] {
                {"simpleTwo", 300},
                {"objectTwo", SupportBeanCombinedProps.makeDefaultBean()},
                {"nodefmapTwo", makeMap(new Object[][] {{"key3", "val3"}})},
                {"mapTwo", levelThree},
        });

        Map<String, Object> levelOne = makeMap(new Object[][] {
                {"simpleOne", 10},
                {"objectOne", SupportBeanComplexProps.makeDefaultBean()},
                {"nodefmapOne", makeMap(new Object[][] {{"key2", "val2"}})},
                {"mapOne", levelTwo}
        });

        Object[] levelZero = {"abc", new SupportBean_A("A1"), makeMap(new Object[][] {{"key1", "val1"}}), levelOne};
        return levelZero;
    }

    private Pair<String[], Object[]> getTestDefTwo()
    {
        Map<String, Object> levelThree= makeMap(new Object[][] {
                {"simpleThree", Long.class},
                {"objectThree", SupportBean_B.class},
        });

        Map<String, Object> levelTwo= makeMap(new Object[][] {
                {"simpleTwo", Integer.class},
                {"objectTwo", SupportBeanCombinedProps.class},
                {"nodefmapTwo", Map.class},
                {"mapTwo", levelThree},
        });

        Map<String, Object> levelOne = makeMap(new Object[][] {
                {"simpleOne", Integer.class},
                {"objectOne", SupportBeanComplexProps.class},
                {"nodefmapOne", Map.class},
                {"mapOne", levelTwo}
        });

        String[] levelZeroProps = {"simple", "object", "nodefmap", "map"};
        Object[] levelZeroTypes = {String.class, SupportBean_A.class, Map.class, levelOne};
        return new Pair<String[], Object[]>(levelZeroProps, levelZeroTypes);
    }

    private EPServiceProvider getEngineInitialized(String name, String[] propertyNames, Object[] propertyTypes)
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        if (name != null) {
            configuration.addEventType(name, propertyNames, propertyTypes);
        }
        
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        return epService;
    }

    private Map<String, Object> makeMap(String nameValuePairs)
    {
        Map<String, Object> result = new HashMap<String, Object>();
        String[] elements = nameValuePairs.split(",");
        for (int i = 0; i < elements.length; i++)
        {
            String[] pair = elements[i].split("=");
            if (pair.length == 2)
            {
                result.put(pair[0], pair[1]);
            }
        }
        return result;
    }

    private Map<String, Object> makeMap(Object[][] entries)
    {
        Map<String, Object> result = new HashMap<String, Object>();
        if (entries == null)
        {
            return result;
        }
        for (int i = 0; i < entries.length; i++)
        {
            result.put((String) entries[i][0], entries[i][1]);
        }
        return result;
    }

    private static class MyNested {
        private final List<MyInside> insides;

        private MyNested(List<MyInside> insides) {
            this.insides = insides;
        }

        public List<MyInside> getInsides() {
            return insides;
        }
    }

    private static class MyInside {
        private final String id;

        private MyInside(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    private final static Logger log = LoggerFactory.getLogger(TestMapEvent.class);
}
