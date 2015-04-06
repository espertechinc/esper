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
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestMapEventNested extends TestCase
{
    public void testMapTypeUpdate()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        Map<String, Object> type = makeMap(new Object[][] {
                {"base1", String.class},
                {"base2", makeMap(new Object[][] {{"n1", int.class}})}
                });
        configuration.addEventType("MyEvent", type);

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        EPStatement statementOne = epService.getEPAdministrator().createEPL(
                "select base1 as v1, base2.n1 as v2, base3? as v3, base2.n2? as v4  from MyEvent");
        EPStatement statementOneSelectAll = epService.getEPAdministrator().createEPL("select * from MyEvent");
        assertEquals("[base1, base2]", Arrays.toString(statementOneSelectAll.getEventType().getPropertyNames()));
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        statementOne.addListener(listenerOne);
        String[] fields = "v1,v2,v3,v4".split(",");

        epService.getEPRuntime().sendEvent(makeMap(new Object[][] {
                {"base1", "abc"},
                {"base2", makeMap(new Object[][] {{"n1", 10}})}
                }), "MyEvent");
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"abc", 10, null, null});

        // update type
        Map<String, Object> typeNew = makeMap(new Object[][] {
                {"base3", Long.class},
                {"base2", makeMap(new Object[][] {{"n2", String.class}})}
                });
        epService.getEPAdministrator().getConfiguration().updateMapEventType("MyEvent", typeNew);

        EPStatement statementTwo = epService.getEPAdministrator().createEPL("select base1 as v1, base2.n1 as v2, base3 as v3, base2.n2 as v4 from MyEvent");
        EPStatement statementTwoSelectAll = epService.getEPAdministrator().createEPL("select * from MyEvent");
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        statementTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(makeMap(new Object[][] {
                {"base1", "def"},
                {"base2", makeMap(new Object[][] {{"n1", 9}, {"n2", "xyz"}})},
                {"base3", 20L},
                }), "MyEvent");
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
            epService.getEPAdministrator().getConfiguration().updateMapEventType("dummy", typeNew);
            fail();
        }
        catch (ConfigurationException ex)
        {
            assertEquals("Error updating Map event type: Event type named 'dummy' has not been declared", ex.getMessage());
        }

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        try
        {
            epService.getEPAdministrator().getConfiguration().updateMapEventType("SupportBean", typeNew);
            fail();
        }
        catch (ConfigurationException ex)
        {
            assertEquals("Error updating Map event type: Event type by name 'SupportBean' is not a Map event type", ex.getMessage());
        }

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testMapInheritanceInitTime()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();

        Map<String, Object> root = makeMap(new Object[][] {{"base", String.class}});
        Map<String, Object> sub1 = makeMap(new Object[][] {{"sub1", String.class}});
        Map<String, Object> sub2 = makeMap(new Object[][] {{"sub2", String.class}});
        Properties suba = makeProperties(new Object[][] {{"suba", String.class}});
        Map<String, Object> subb = makeMap(new Object[][] {{"subb", String.class}});
        configuration.addEventType("RootEvent", root);
        configuration.addEventType("Sub1Event", sub1);
        configuration.addEventType("Sub2Event", sub2);
        configuration.addEventType("SubAEvent", suba);
        configuration.addEventType("SubBEvent", subb);

        configuration.addMapSuperType("Sub1Event", "RootEvent");
        configuration.addMapSuperType("Sub2Event", "RootEvent");
        configuration.addMapSuperType("SubAEvent", "Sub1Event");
        configuration.addMapSuperType("SubBEvent", "Sub1Event");
        configuration.addMapSuperType("SubBEvent", "Sub2Event");

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("base", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("sub1", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("suba", String.class, null, false, false, false, false, false),
        }, ((EPServiceProviderSPI) epService).getEventAdapterService().getExistsTypeByName("SubAEvent").getPropertyDescriptors());

        runMapInheritanceInitTime(epService);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testMapInheritanceRuntime()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        Map<String, Object> root = makeMap(new Object[][] {{"base", String.class}});
        Map<String, Object> sub1 = makeMap(new Object[][] {{"sub1", String.class}});
        Map<String, Object> sub2 = makeMap(new Object[][] {{"sub2", String.class}});
        Map<String, Object> suba = makeMap(new Object[][] {{"suba", String.class}});
        Map<String, Object> subb = makeMap(new Object[][] {{"subb", String.class}});

        epService.getEPAdministrator().getConfiguration().addEventType("RootEvent", root);
        epService.getEPAdministrator().getConfiguration().addEventType("Sub1Event", sub1, new String[] {"RootEvent"});
        epService.getEPAdministrator().getConfiguration().addEventType("Sub2Event", sub2, new String[] {"RootEvent"});
        epService.getEPAdministrator().getConfiguration().addEventType("SubAEvent", suba, new String[] {"Sub1Event"});
        epService.getEPAdministrator().getConfiguration().addEventType("SubBEvent", subb, new String[] {"Sub1Event", "Sub2Event"});

        runMapInheritanceInitTime(epService);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void runMapInheritanceInitTime(EPServiceProvider epService)
    {
        SupportUpdateListener listeners[] = new SupportUpdateListener[5];
        String[] statements = {
                "select base as vbase, sub1? as v1, sub2? as v2, suba? as va, subb? as vb from RootEvent",  // 0
                "select base as vbase, sub1 as v1, sub2? as v2, suba? as va, subb? as vb from Sub1Event",   // 1
                "select base as vbase, sub1? as v1, sub2 as v2, suba? as va, subb? as vb from Sub2Event",   // 2
                "select base as vbase, sub1 as v1, sub2? as v2, suba as va, subb? as vb from SubAEvent",    // 3
                "select base as vbase, sub1? as v1, sub2 as v2, suba? as va, subb as vb from SubBEvent"     // 4
        };
        for (int i = 0; i < statements.length; i++)
        {
            EPStatement statement = epService.getEPAdministrator().createEPL(statements[i]);
            listeners[i] = new SupportUpdateListener();
            statement.addListener(listeners[i]);
        }
        String[] fields = "vbase,v1,v2,va,vb".split(",");

        epService.getEPRuntime().sendEvent(makeMap("base=a,sub1=b,sub2=x,suba=c,subb=y"), "SubAEvent");
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", "x", "c", "y"});
        assertFalse(listeners[2].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", "x", "c", "y"});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"a", "b", "x", "c", "y"});

        epService.getEPRuntime().sendEvent(makeMap("base=f1,sub1=f2,sub2=f3,suba=f4,subb=f5"), "SubAEvent");
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", "f3", "f4", "f5"});
        assertFalse(listeners[2].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", "f3", "f4", "f5"});
        EPAssertionUtil.assertProps(listeners[3].assertOneGetNewAndReset(), fields, new Object[]{"f1", "f2", "f3", "f4", "f5"});

        epService.getEPRuntime().sendEvent(makeMap("base=XBASE,sub1=X1,sub2=X2,subb=XY"), "SubBEvent");
        Object[] values = new Object[] {"XBASE","X1","X2",null,"XY"};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[3].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, values);
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fields, values);
        EPAssertionUtil.assertProps(listeners[4].assertOneGetNewAndReset(), fields, values);

        epService.getEPRuntime().sendEvent(makeMap("base=YBASE,sub1=Y1"), "Sub1Event");
        values = new Object[] {"YBASE","Y1", null, null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[2].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fields, values);

        epService.getEPRuntime().sendEvent(makeMap("base=YBASE,sub2=Y2"), "Sub2Event");
        values = new Object[] {"YBASE",null, "Y2", null, null};
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fields, values);
        assertFalse(listeners[1].isInvoked() || listeners[3].isInvoked() || listeners[4].isInvoked());
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fields, values);

        epService.getEPRuntime().sendEvent(makeMap("base=ZBASE"), "RootEvent");
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

    public void testEscapedDot()
    {
        Map<String, Object> definition = makeMap(new Object[][] {
                {"a.b", int.class},
                {"a.b.c", int.class},
                {"nes.", int.class},
                {"nes.nes2", makeMap(new Object[][] {{"x.y", int.class}}) }
        });
        EPServiceProvider epService = getEngineInitialized("DotMap", definition);

        String statementText = "select a\\.b, a\\.b\\.c, nes\\., nes\\.nes2.x\\.y from DotMap";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Map<String, Object> data = makeMap(new Object[][] {
                {"a.b", 10},
                {"a.b.c", 20},
                {"nes.", 30},
                {"nes.nes2", makeMap(new Object[][] {{"x.y", 40}}) }
        });
        epService.getEPRuntime().sendEvent(data, "DotMap");

        String[] fields = "a.b,a.b.c,nes.,nes.nes2.x.y".split(",");
        EventBean received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{10, 20, 30, 40});
    }

    public void testNestedMapRuntime()
    {
        EPServiceProvider epService = getEngineInitialized(null, null);
        epService.getEPAdministrator().getConfiguration().addEventType("NestedMap", getTestDefinition());
        runAssertion(epService);
    }

    public void testNestedConfigEngine()
    {
        EPServiceProvider epService = getEngineInitialized("NestedMap", getTestDefinition());
        runAssertion(epService);
    }

    public void testInsertInto()
    {
        EPServiceProvider epService = getEngineInitialized("NestedMap", getTestDefTwo());

        String statementText = "insert into MyStream select " +
                                "map.mapOne as val1" +
                                " from NestedMap.win:length(5)";
        epService.getEPAdministrator().createEPL(statementText);

        statementText = "select val1 as a from MyStream";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Map<String, Object> testdata = getTestDataTwo();
        epService.getEPRuntime().sendEvent(testdata, "NestedMap");

        // test all properties exist
        String[] fields = "a".split(",");
        EventBean received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{getNestedKey(testdata, "map", "mapOne")});
    }

    public void testAddIdenticalMapTypes()
    {
        EPServiceProvider epService = getEngineInitialized(null, null);

        Map<String, Object> levelOne_1 = makeMap(new Object[][] {{"simpleOne", Integer.class}});
        Map<String, Object> levelOne_2 = makeMap(new Object[][] {{"simpleOne", Long.class}});
        Map<String, Object> levelZero_1 = makeMap(new Object[][] {{"map", levelOne_1}});
        Map<String, Object> levelZero_2 = makeMap(new Object[][] {{"map", levelOne_2}});

        // can add the same nested type twice
        epService.getEPAdministrator().getConfiguration().addEventType("ABC", levelZero_1);
        epService.getEPAdministrator().getConfiguration().addEventType("ABC", levelZero_1);
        try
        {
            // changing the definition however stops the compatibility
            epService.getEPAdministrator().getConfiguration().addEventType("ABC", levelZero_2);
            fail();
        }
        catch (ConfigurationException ex)
        {
            // expected
        }
    }

    public void testNestedPojo()
    {
        EPServiceProvider epService = getEngineInitialized("NestedMap", getTestDefTwo());

        String statementText = "select " +
                                "simple, object, nodefmap, map, " +
                                "object.id as a1, nodefmap.key1? as a2, nodefmap.key2? as a3, nodefmap.key3?.key4 as a4, " +
                                "map.objectOne as b1, map.simpleOne as b2, map.nodefmapOne.key2? as b3, map.mapOne.simpleTwo? as b4, " +
                                "map.objectOne.indexed[1] as c1, map.objectOne.nested.nestedValue as c2," +
                                "map.mapOne.simpleTwo as d1, map.mapOne.objectTwo as d2, map.mapOne.nodefmapTwo as d3, " +
                                "map.mapOne.mapTwo as e1, map.mapOne.mapTwo.simpleThree as e2, map.mapOne.mapTwo.objectThree as e3, " +
                                "map.mapOne.objectTwo.array[1].mapped('1ma').value as f1, map.mapOne.mapTwo.objectThree.id as f2" +
                                " from NestedMap.win:length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Map<String, Object> testdata = getTestDataTwo();
        epService.getEPRuntime().sendEvent(testdata, "NestedMap");

        // test all properties exist
        EventBean received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, "simple,object,nodefmap,map".split(","),
                new Object[]{"abc", new SupportBean_A("A1"), testdata.get("nodefmap"), testdata.get("map")});
        EPAssertionUtil.assertProps(received, "a1,a2,a3,a4".split(","),
                new Object[]{"A1", "val1", null, null});
        EPAssertionUtil.assertProps(received, "b1,b2,b3,b4".split(","),
                new Object[]{getNestedKey(testdata, "map", "objectOne"), 10, "val2", 300});
        EPAssertionUtil.assertProps(received, "c1,c2".split(","), new Object[]{2, "nestedValue"});
        EPAssertionUtil.assertProps(received, "d1,d2,d3".split(","),
                new Object[]{300, getNestedKey(testdata, "map", "mapOne", "objectTwo"), getNestedKey(testdata, "map", "mapOne", "nodefmapTwo")});
        EPAssertionUtil.assertProps(received, "e1,e2,e3".split(","),
                new Object[]{getNestedKey(testdata, "map", "mapOne", "mapTwo"), 4000L, new SupportBean_B("B1")});
        EPAssertionUtil.assertProps(received, "f1,f2".split(","),
                new Object[]{"1ma0", "B1"});

        // test partial properties exist
        testdata = getTestDataThree();
        epService.getEPRuntime().sendEvent(testdata, "NestedMap");

        received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, "simple,object,nodefmap,map".split(","),
                new Object[]{"abc", new SupportBean_A("A1"), testdata.get("nodefmap"), testdata.get("map")});
        EPAssertionUtil.assertProps(received, "a1,a2,a3,a4".split(","),
                new Object[]{"A1", "val1", null, null});
        EPAssertionUtil.assertProps(received, "b1,b2,b3,b4".split(","),
                new Object[]{getNestedKey(testdata, "map", "objectOne"), null, null, null});
        EPAssertionUtil.assertProps(received, "c1,c2".split(","), new Object[]{null, null});
        EPAssertionUtil.assertProps(received, "d1,d2,d3".split(","),
                new Object[]{null, getNestedKey(testdata, "map", "mapOne", "objectTwo"), getNestedKey(testdata, "map", "mapOne", "nodefmapTwo")});
        EPAssertionUtil.assertProps(received, "e1,e2,e3".split(","),
                new Object[]{getNestedKey(testdata, "map", "mapOne", "mapTwo"), 4000L, null});
        EPAssertionUtil.assertProps(received, "f1,f2".split(","),
                new Object[]{"1ma0", null});
    }

    public void testEventType()
    {
        EPServiceProvider epService = getEngineInitialized("NestedMap", getTestDefTwo());
        EPStatement stmt = epService.getEPAdministrator().createEPL(("select * from NestedMap"));
        EventType eventType = stmt.getEventType();

        String[] propertiesReceived = eventType.getPropertyNames();
        String[] propertiesExpected = new String[] {"simple", "object", "nodefmap", "map"};
        EPAssertionUtil.assertEqualsAnyOrder(propertiesReceived, propertiesExpected);
        assertEquals(String.class, eventType.getPropertyType("simple"));
        assertEquals(Map.class, eventType.getPropertyType("map"));
        assertEquals(Map.class, eventType.getPropertyType("nodefmap"));
        assertEquals(SupportBean_A.class, eventType.getPropertyType("object"));

        assertNull(eventType.getPropertyType("map.mapOne.simpleOne"));
    }

    public void testInvalidType()
    {
        EPServiceProvider epService = getEngineInitialized(null, null);

        Map<String, Object> invalid = makeMap(new Object[][] {{new SupportBean(), null} });
        tryInvalid(epService, invalid, "com.espertech.esper.support.bean.SupportBean cannot be cast to java.lang.String");

        invalid = makeMap(new Object[][] {{"abc", new SupportBean()} });
        tryInvalid(epService, invalid, "Nestable type configuration encountered an unexpected property type of 'SupportBean' for property 'abc', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type");
    }

    public void testArrayProperty()
    {
        EPServiceProvider epService = getEngineInitialized(null, null);

        // test map containing first-level property that is an array of primitive or Class
        Map<String, Object> arrayDef = makeMap(new Object[][] {{"p0", int[].class}, {"p1", SupportBean[].class }});
        epService.getEPAdministrator().getConfiguration().addEventType("MyArrayMap", arrayDef);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0[0] as a, p0[1] as b, p1[0].intPrimitive as c, p1[1] as d, p0 as e from MyArrayMap");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        int[] p0 = new int[] {1, 2, 3};
        SupportBean[] beans = new SupportBean[] {new SupportBean("e1", 5), new SupportBean("e2", 6)};
        Map<String, Object> theEvent = makeMap(new Object[][] {{"p0", p0}, {"p1", beans}});
        epService.getEPRuntime().sendEvent(theEvent, "MyArrayMap");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 5, beans[1], p0});
        assertEquals(int.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(SupportBean.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(int[].class, stmt.getEventType().getPropertyType("e"));
        stmt.destroy();

        // test map at the second level of a nested map that is an array of primitive or Class
        Map<String, Object> arrayDefOuter = makeMap(new Object[][] {{"outer", arrayDef}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyArrayMapOuter", arrayDefOuter);

        stmt = epService.getEPAdministrator().createEPL("select outer.p0[0] as a, outer.p0[1] as b, outer.p1[0].intPrimitive as c, outer.p1[1] as d, outer.p0 as e from MyArrayMapOuter");
        stmt.addListener(listener);

        Map<String, Object> eventOuter = makeMap(new Object[][] {{"outer", theEvent}});
        epService.getEPRuntime().sendEvent(eventOuter, "MyArrayMapOuter");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d".split(","), new Object[]{1, 2, 5, beans[1]});
        assertEquals(int.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(SupportBean.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(int[].class, stmt.getEventType().getPropertyType("e"));
    }

    public void testMappedProperty()
    {
        EPServiceProvider epService = getEngineInitialized(null, null);

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
        EPServiceProvider epService = getEngineInitialized(null, null);

        // create a named map
        Map<String, Object> namedDef = makeMap(new Object[][] {{"n0", int.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyNamedMap", namedDef);

        // create a map using the name
        Map<String, Object> eventDef = makeMap(new Object[][] {{"p0", "MyNamedMap"}, {"p1", "MyNamedMap[]"}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapWithAMap", eventDef);

        // test named-map at the second level of a nested map
        Map<String, Object> arrayDefOuter = makeMap(new Object[][] {{"outer", eventDef}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyArrayMapOuter", arrayDefOuter);

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select outer.p0.n0 as a, outer.p1[0].n0 as b, outer.p1[1].n0 as c, outer.p0 as d, outer.p1 as e from MyArrayMapOuter");
        stmt.addListener(listener);

        Map<String, Object> n0_1 = makeMap(new Object[][] {{"n0", 1}});
        Map<String, Object> n0_21 = makeMap(new Object[][] {{"n0", 2}});
        Map<String, Object> n0_22 = makeMap(new Object[][] {{"n0", 3}});
        Map[] n0_2 = new Map[] {n0_21, n0_22};
        Map<String, Object> theEvent = makeMap(new Object[][] {{"p0", n0_1}, {"p1", n0_2 }});
        Map<String, Object> eventOuter = makeMap(new Object[][] {{"outer", theEvent}});
        epService.getEPRuntime().sendEvent(eventOuter, "MyArrayMapOuter");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 3, n0_1, n0_2});
        assertEquals(int.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(Map.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(Map[].class, stmt.getEventType().getPropertyType("e"));

        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select outer.p0.n0? as a, outer.p1[0].n0? as b, outer.p1[1]?.n0 as c, outer.p0? as d, outer.p1? as e from MyArrayMapOuter");
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(eventOuter, "MyArrayMapOuter");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 3, n0_1, n0_2});
        assertEquals(int.class, stmt.getEventType().getPropertyType("a"));
    }

    public void testMapNameProperty()
    {
        EPServiceProvider epService = getEngineInitialized(null, null);

        // create a named map
        Map<String, Object> namedDef = makeMap(new Object[][] {{"n0", int.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyNamedMap", namedDef);

        // create a map using the name
        Map<String, Object> eventDef = makeMap(new Object[][] {{"p0", "MyNamedMap"}, {"p1", "MyNamedMap[]"}});
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapWithAMap", eventDef);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0.n0 as a, p1[0].n0 as b, p1[1].n0 as c, p0 as d, p1 as e from MyMapWithAMap");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<String, Object> n0_1 = makeMap(new Object[][] {{"n0", 1}});
        Map<String, Object> n0_21 = makeMap(new Object[][] {{"n0", 2}});
        Map<String, Object> n0_22 = makeMap(new Object[][] {{"n0", 3}});
        Map[] n0_2 = new Map[] {n0_21, n0_22};
        Map<String, Object> theEvent = makeMap(new Object[][] {{"p0", n0_1}, {"p1", n0_2 }});
        epService.getEPRuntime().sendEvent(theEvent, "MyMapWithAMap");

        EventBean eventResult = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(eventResult, "a,b,c,d".split(","), new Object[]{1, 2, 3, n0_1});
        Map[] valueE = (Map[]) eventResult.get("e");
        assertSame(valueE[0], n0_2[0]);
        assertSame(valueE[1], n0_2[1]);

        assertEquals(int.class, stmt.getEventType().getPropertyType("a"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("b"));
        assertEquals(int.class, stmt.getEventType().getPropertyType("c"));
        assertEquals(Map.class, stmt.getEventType().getPropertyType("d"));
        assertEquals(Map[].class, stmt.getEventType().getPropertyType("e"));
    }

    public void testIsExists()
    {
        EPServiceProvider epService = getEngineInitialized("NestedMap", getTestDefTwo());

        String statementText = "select " +
                                "exists(map.mapOne?) as a," +
                                "exists(map.mapOne?.simpleOne) as b," +
                                "exists(map.mapOne?.simpleTwo) as c," +
                                "exists(map.mapOne?.mapTwo) as d," +
                                "exists(map.mapOne.mapTwo?) as e," +
                                "exists(map.mapOne.mapTwo.simpleThree?) as f," +
                                "exists(map.mapOne.mapTwo.objectThree?) as g " +
                                " from NestedMap.win:length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Map<String, Object> testdata = getTestDataTwo();
        epService.getEPRuntime().sendEvent(testdata, "NestedMap");

        // test all properties exist
        String[] fields = "a,b,c,d,e,f,g".split(",");
        EventBean received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields,
                new Object[]{true, false, true, true, true, true, true});

        // test partial properties exist
        testdata = getTestDataThree();
        epService.getEPRuntime().sendEvent(testdata, "NestedMap");

        received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields,
                new Object[]{true, false, false, true, true, true, false});
    }

    private void runAssertion(EPServiceProvider epService)
    {
        String statementText = "select nested as a, " +
                    "nested.n1 as b," +
                    "nested.n2 as c," +
                    "nested.n2.n1n1 as d " +
                    "from NestedMap.win:length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Map<String, Object> mapEvent = getTestData();
        epService.getEPRuntime().sendEvent(mapEvent, "NestedMap");
        
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertSame(mapEvent.get("nested"), theEvent.get("a"));
        assertSame("abc", theEvent.get("b"));
        assertSame(((Map) mapEvent.get("nested")).get("n2"), theEvent.get("c"));
        assertSame("def", theEvent.get("d"));
        statement.stop();
    }

    private void tryInvalid(EPServiceProvider epService, Map<String, Object> config, String message)
    {
        try
        {
            epService.getEPAdministrator().getConfiguration().addEventType("NestedMap", config);
            fail();
        }
        catch (Exception ex)
        {
            log.error(ex, ex);
            assertTrue("expected '" + message + "' but received '" + ex.getMessage(), ex.getMessage().contains(message));
        }
    }

    private Map<String, Object> getTestDefinition()
    {
        Map<String, Object> propertiesNestedNested = new HashMap<String, Object>();
        propertiesNestedNested.put("n1n1", String.class);

        Map<String, Object> propertiesNested = new HashMap<String, Object>();
        propertiesNested.put("n1", String.class);
        propertiesNested.put("n2", propertiesNestedNested);

        Map<String, Object> root = new HashMap<String, Object>();
        root.put("nested", propertiesNested);

        return root;
    }

    private Map<String, Object> getTestData()
    {
        Map nestedNested = new HashMap<String, Object>();
        nestedNested.put("n1n1", "def");

        Map nested = new HashMap<String, Object>();
        nested.put("n1", "abc");
        nested.put("n2", nestedNested);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("nested", nested);

        return map;
    }

    private Map<String, Object> getTestDefTwo()
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

        Map<String, Object> levelZero = makeMap(new Object[][] {
                {"simple", String.class},
                {"object", SupportBean_A.class},
                {"nodefmap", Map.class},
                {"map", levelOne}
        });

        return levelZero;
    }

    private Map<String, Object> getTestDataTwo()
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

        Map<String, Object> levelZero = makeMap(new Object[][] {
                {"simple", "abc"},
                {"object", new SupportBean_A("A1")},
                {"nodefmap", makeMap(new Object[][] {{"key1", "val1"}})},
                {"map", levelOne}
        });

        return levelZero;
    }

    private Map<String, Object> getTestDataThree()
    {
        Map<String, Object> levelThree = makeMap(new Object[][] {
                {"simpleThree", 4000L},
        });

        Map<String, Object> levelTwo = makeMap(new Object[][] {
                {"objectTwo", SupportBeanCombinedProps.makeDefaultBean()},
                {"nodefmapTwo", makeMap(new Object[][] {{"key3", "val3"}})},
                {"mapTwo", levelThree},
        });

        Map<String, Object> levelOne = makeMap(new Object[][] {
                {"simpleOne", null},
                {"objectOne", null},
                {"mapOne", levelTwo}
        });

        Map<String, Object> levelZero = makeMap(new Object[][] {
                {"simple", "abc"},
                {"object", new SupportBean_A("A1")},
                {"nodefmap", makeMap(new Object[][] {{"key1", "val1"}})},
                {"map", levelOne}
        });

        return levelZero;
    }

    private Map<String, Object> makeMap(String nameValuePairs)
    {
        Map result = new HashMap<String, Object>();
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
        Map result = new HashMap<String, Object>();
        if (entries == null)
        {
            return result;
        }
        for (int i = 0; i < entries.length; i++)
        {
            result.put(entries[i][0], entries[i][1]);
        }
        return result;
    }

    private Properties makeProperties(Object[][] entries)
    {
        Properties result = new Properties();
        for (int i = 0; i < entries.length; i++)
        {
            Class clazz = (Class) entries[i][1];
            result.put(entries[i][0], clazz.getName());
        }
        return result;
    }

    private Object getNestedKey(Map<String, Object> root, String keyOne, String keyTwo)
    {
        Map map = (Map) root.get(keyOne);
        return map.get(keyTwo);
    }

    private Object getNestedKey(Map<String, Object> root, String keyOne, String keyTwo, String keyThree)
    {
        Map map = (Map) root.get(keyOne);
        map = (Map) map.get(keyTwo);
        return map.get(keyThree);
    }

    private EPServiceProvider getEngineInitialized(String name, Map<String, Object> definition)
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();

        if (name != null)
        {
            configuration.addEventType(name, definition);
        }
        
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        return epService;
    }

    private static Log log = LogFactory.getLog(TestMapEvent.class);
}
