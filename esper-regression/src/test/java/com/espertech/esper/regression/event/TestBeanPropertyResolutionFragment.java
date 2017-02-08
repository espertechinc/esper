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
package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanCombinedProps;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.support.SupportEventTypeAssertionUtil;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestBeanPropertyResolutionFragment extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testMapSimpleTypes()
    {
        Map<String, Object> mapOuter = new HashMap<String, Object>();
        mapOuter.put("p0int", int.class);
        mapOuter.put("p0intarray", int[].class);
        mapOuter.put("p0map", Map.class);
        epService.getEPAdministrator().getConfiguration().addEventType("TypeRoot", mapOuter);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TypeRoot");
        stmt.addListener(listener);

        Map<String, Object> dataInner = new HashMap<String, Object>();
        dataInner.put("p1someval", "A");

        Map<String, Object> dataRoot = new HashMap<String, Object>();
        dataRoot.put("p0simple", 99);
        dataRoot.put("p0array", new int[] {101, 102});
        dataRoot.put("p0map", dataInner);

        // send event
        epService.getEPRuntime().sendEvent(dataRoot, "TypeRoot");
        EventBean eventBean = listener.assertOneGetNewAndReset();
        //System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    //comment me in
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);

        // resolve property via fragment
        assertNull(eventType.getFragmentType("p0int"));
        assertNull(eventType.getFragmentType("p0intarray"));
        assertNull(eventBean.getFragment("p0map?"));
        assertNull(eventBean.getFragment("p0intarray[0]?"));
        assertNull(eventBean.getFragment("p0map('a')?"));
    }

    public void testObjectArraySimpleTypes()
    {
        String[] props = {"p0int", "p0intarray", "p0map"};
        Object[] types = {int.class, int[].class, Map.class};
        epService.getEPAdministrator().getConfiguration().addEventType("TypeRoot", props, types);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TypeRoot");
        stmt.addListener(listener);

        Map<String, Object> dataInner = new HashMap<String, Object>();
        dataInner.put("p1someval", "A");
        Object[] dataRoot = new Object[] {99, new int[] {101, 102}, dataInner};

        // send event
        epService.getEPRuntime().sendEvent(dataRoot, "TypeRoot");
        EventBean eventBean = listener.assertOneGetNewAndReset();
        //System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    //comment me in
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);

        // resolve property via fragment
        assertNull(eventType.getFragmentType("p0int"));
        assertNull(eventType.getFragmentType("p0intarray"));
        assertNull(eventBean.getFragment("p0map?"));
        assertNull(eventBean.getFragment("p0intarray[0]?"));
        assertNull(eventBean.getFragment("p0map('a')?"));
    }

    public void testWrapperFragmentWithMap()
    {
        Map<String, Object> typeLev0 = new HashMap<String, Object>();
        typeLev0.put("p1id", int.class);
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev0", typeLev0);

        Map<String, Object> mapOuter = new HashMap<String, Object>();
        mapOuter.put("p0simple", "TypeLev0");
        mapOuter.put("p0bean", SupportBeanComplexProps.class);
        epService.getEPAdministrator().getConfiguration().addEventType("TypeRoot", mapOuter);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select *, p0simple.p1id + 1 as plusone, p0bean as mybean from TypeRoot");
        stmt.addListener(listener);

        Map<String, Object> dataInner = new HashMap<String, Object>();
        dataInner.put("p1id", 10);

        Map<String, Object> dataRoot = new HashMap<String, Object>();
        dataRoot.put("p0simple", dataInner);
        dataRoot.put("p0bean", SupportBeanComplexProps.makeDefaultBean());

        // send event
        epService.getEPRuntime().sendEvent(dataRoot, "TypeRoot");
        EventBean eventBean = listener.assertOneGetNewAndReset();
        //  System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    comment me in
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);

        // resolve property via fragment
        assertTrue(eventType.getPropertyDescriptor("p0simple").isFragment());
        assertEquals(11, eventBean.get("plusone"));
        assertEquals(10, eventBean.get("p0simple.p1id"));

        EventBean innerSimpleEvent = (EventBean) eventBean.getFragment("p0simple");
        assertEquals(10, innerSimpleEvent.get("p1id"));

        EventBean innerBeanEvent = (EventBean) eventBean.getFragment("mybean");
        assertEquals("nestedNestedValue", innerBeanEvent.get("nested.nestedNested.nestedNestedValue"));
        assertEquals("nestedNestedValue", ((EventBean)eventBean.getFragment("mybean.nested.nestedNested")).get("nestedNestedValue"));
    }

    public void testWrapperFragmentWithObjectArray()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev0", new String[] {"p1id"}, new Object[] {int.class});
        epService.getEPAdministrator().getConfiguration().addEventType("TypeRoot", new String[] {"p0simple", "p0bean"}, new Object[] {"TypeLev0", SupportBeanComplexProps.class});

        EPStatement stmt = epService.getEPAdministrator().createEPL("select *, p0simple.p1id + 1 as plusone, p0bean as mybean from TypeRoot");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[] {new Object[]{10}, SupportBeanComplexProps.makeDefaultBean()}, "TypeRoot");

        EventBean eventBean = listener.assertOneGetNewAndReset();
        //  System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    comment me in
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);

        // resolve property via fragment
        assertTrue(eventType.getPropertyDescriptor("p0simple").isFragment());
        assertEquals(11, eventBean.get("plusone"));
        assertEquals(10, eventBean.get("p0simple.p1id"));

        EventBean innerSimpleEvent = (EventBean) eventBean.getFragment("p0simple");
        assertEquals(10, innerSimpleEvent.get("p1id"));

        EventBean innerBeanEvent = (EventBean) eventBean.getFragment("mybean");
        assertEquals("nestedNestedValue", innerBeanEvent.get("nested.nestedNested.nestedNestedValue"));
        assertEquals("nestedNestedValue", ((EventBean)eventBean.getFragment("mybean.nested.nestedNested")).get("nestedNestedValue"));
    }

    public void testNativeBeanFragment()
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from " + SupportBeanComplexProps.class.getName());
        stmt.addListener(listener);
        stmt = epService.getEPAdministrator().createEPL("select * from " + SupportBeanCombinedProps.class.getName());
        stmt.addListener(listener);

        // assert nested fragments
        epService.getEPRuntime().sendEvent(SupportBeanComplexProps.makeDefaultBean());
        EventBean eventBean = listener.assertOneGetNewAndReset();
        SupportEventTypeAssertionUtil.assertConsistency(eventBean.getEventType());
        //System.out.println(SupportEventTypeAssertionUtil.print(eventBean));

        assertTrue(eventBean.getEventType().getPropertyDescriptor("nested").isFragment());
        EventBean eventNested = (EventBean) eventBean.getFragment("nested");
        assertEquals("nestedValue", eventNested.get("nestedValue"));
        eventNested = (EventBean) eventBean.getFragment("nested?");
        assertEquals("nestedValue", eventNested.get("nestedValue"));

        assertTrue(eventNested.getEventType().getPropertyDescriptor("nestedNested").isFragment());
        assertEquals("nestedNestedValue", ((EventBean) eventNested.getFragment("nestedNested")).get("nestedNestedValue"));
        assertEquals("nestedNestedValue", ((EventBean) eventNested.getFragment("nestedNested?")).get("nestedNestedValue"));

        EventBean nestedFragment = (EventBean) eventBean.getFragment("nested.nestedNested");
        assertEquals("nestedNestedValue", nestedFragment.get("nestedNestedValue"));

        // assert indexed fragments
        SupportBeanCombinedProps eventObject = SupportBeanCombinedProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(eventObject);
        eventBean = listener.assertOneGetNewAndReset();
        SupportEventTypeAssertionUtil.assertConsistency(eventBean.getEventType());
        //System.out.println(SupportEventTypeAssertionUtil.print(eventBean));

        assertTrue(eventBean.getEventType().getPropertyDescriptor("array").isFragment());
        assertTrue(eventBean.getEventType().getPropertyDescriptor("array").isIndexed());
        EventBean[] eventArray = (EventBean[]) eventBean.getFragment("array");
        assertEquals(3, eventArray.length);

        EventBean eventElement = eventArray[0];
        assertSame(eventObject.getArray()[0].getMapped("0ma"), eventElement.get("mapped('0ma')"));
        assertSame(eventObject.getArray()[0].getMapped("0ma"), ((EventBean)eventBean.getFragment("array[0]")).get("mapped('0ma')"));
        assertSame(eventObject.getArray()[0].getMapped("0ma"), ((EventBean)eventBean.getFragment("array[0]?")).get("mapped('0ma')"));
    }

    public void testMapFragmentMapNested()
    {
        Map<String, Object> typeLev0 = new HashMap<String, Object>();
        typeLev0.put("p1id", int.class);
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev0", typeLev0);

        Map<String, Object> mapOuter = new HashMap<String, Object>();
        mapOuter.put("p0simple", "TypeLev0");
        mapOuter.put("p0array", "TypeLev0[]");
        epService.getEPAdministrator().getConfiguration().addEventType("TypeRoot", mapOuter);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TypeRoot");
        stmt.addListener(listener);

        Map<String, Object> dataInner = new HashMap<String, Object>();
        dataInner.put("p1id", 10);

        Map<String, Object> dataRoot = new HashMap<String, Object>();
        dataRoot.put("p0simple", dataInner);
        dataRoot.put("p0array", new Map[] {dataInner,dataInner});

        // send event
        epService.getEPRuntime().sendEvent(dataRoot, "TypeRoot");
        EventBean eventBean = listener.assertOneGetNewAndReset();
        //  System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    comment me in
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);

        // resolve property via fragment
        assertTrue(eventType.getPropertyDescriptor("p0simple").isFragment());
        assertTrue(eventType.getPropertyDescriptor("p0array").isFragment());

        EventBean innerSimpleEvent = (EventBean) eventBean.getFragment("p0simple");
        assertEquals(10, innerSimpleEvent.get("p1id"));

        EventBean[] innerArrayAllEvent = (EventBean[]) eventBean.getFragment("p0array");
        assertEquals(10, innerArrayAllEvent[0].get("p1id"));

        EventBean innerArrayElementEvent = (EventBean) eventBean.getFragment("p0array[0]");
        assertEquals(10, innerArrayElementEvent.get("p1id"));

        // resolve property via getter
        assertEquals(10, eventBean.get("p0simple.p1id"));
        assertEquals(10, eventBean.get("p0array[1].p1id"));

        assertNull(eventType.getFragmentType("p0array.p1id"));
        assertNull(eventType.getFragmentType("p0array[0].p1id"));
    }

    public void testObjectArrayFragmentObjectArrayNested()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev0", new String[] {"p1id"}, new Object[] {int.class});
        epService.getEPAdministrator().getConfiguration().addEventType("TypeRoot", new String[] {"p0simple", "p0array"}, new Object[] {"TypeLev0", "TypeLev0[]"});

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TypeRoot");
        stmt.addListener(listener);
        assertEquals(Object[].class, stmt.getEventType().getUnderlyingType());

        epService.getEPRuntime().sendEvent(new Object[] {new Object[] {10}, new Object[] {new Object[] {20}, new Object[] {21}}}, "TypeRoot");

        EventBean eventBean = listener.assertOneGetNewAndReset();
        //  System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    comment me in
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);

        // resolve property via fragment
        assertTrue(eventType.getPropertyDescriptor("p0simple").isFragment());
        assertTrue(eventType.getPropertyDescriptor("p0array").isFragment());

        EventBean innerSimpleEvent = (EventBean) eventBean.getFragment("p0simple");
        assertEquals(10, innerSimpleEvent.get("p1id"));

        EventBean[] innerArrayAllEvent = (EventBean[]) eventBean.getFragment("p0array");
        assertEquals(20, innerArrayAllEvent[0].get("p1id"));

        EventBean innerArrayElementEvent = (EventBean) eventBean.getFragment("p0array[0]");
        assertEquals(20, innerArrayElementEvent.get("p1id"));

        // resolve property via getter
        assertEquals(10, eventBean.get("p0simple.p1id"));
        assertEquals(21, eventBean.get("p0array[1].p1id"));

        assertNull(eventType.getFragmentType("p0array.p1id"));
        assertNull(eventType.getFragmentType("p0array[0].p1id"));
    }

    public void testMapFragmentMapUnnamed()
    {
        Map<String, Object> typeLev0 = new HashMap<String, Object>();
        typeLev0.put("p1id", int.class);

        Map<String, Object> mapOuter = new HashMap<String, Object>();
        mapOuter.put("p0simple", typeLev0);
        epService.getEPAdministrator().getConfiguration().addEventType("TypeRoot", mapOuter);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TypeRoot");
        stmt.addListener(listener);

        Map<String, Object> dataInner = new HashMap<String, Object>();
        dataInner.put("p1id", 10);

        Map<String, Object> dataRoot = new HashMap<String, Object>();
        dataRoot.put("p0simple", dataInner);

        // send event
        epService.getEPRuntime().sendEvent(dataRoot, "TypeRoot");
        EventBean eventBean = listener.assertOneGetNewAndReset();
        //  System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    comment me in
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);

        assertFalse(eventType.getPropertyDescriptor("p0simple").isFragment());
        assertNull(eventBean.getFragment("p0simple"));

        // resolve property via getter
        assertEquals(10, eventBean.get("p0simple.p1id"));
    }

    public void testMapFragmentTransposedMapEventBean()
    {
        Map<String, Object> typeInner = new HashMap<String, Object>();
        typeInner.put("p2id", int.class);
        epService.getEPAdministrator().getConfiguration().addEventType("TypeInner", typeInner);

        Map<String, Object> typeMap = new HashMap<String, Object>();
        typeMap.put("id", int.class);
        typeMap.put("bean", SupportBean.class);
        typeMap.put("beanarray", SupportBean[].class);
        typeMap.put("complex", SupportBeanComplexProps.class);
        typeMap.put("complexarray", SupportBeanComplexProps[].class);
        typeMap.put("map", "TypeInner");
        typeMap.put("maparray", "TypeInner[]");

        epService.getEPAdministrator().getConfiguration().addEventType("TypeMapOne", typeMap);
        epService.getEPAdministrator().getConfiguration().addEventType("TypeMapTwo", typeMap);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from pattern[one=TypeMapOne until two=TypeMapTwo]");
        stmt.addListener(listener);

        Map<String, Object> dataInner = new HashMap<String, Object>();
        dataInner.put("p2id", 2000);
        Map<String, Object> dataMap = new HashMap<String, Object>();
        dataMap.put("id", 1);
        dataMap.put("bean", new SupportBean("E1", 100));
        dataMap.put("beanarray", new SupportBean[] {new SupportBean("E1", 100), new SupportBean("E2", 200)});
        dataMap.put("complex", SupportBeanComplexProps.makeDefaultBean());
        dataMap.put("complexarray", new SupportBeanComplexProps[] {SupportBeanComplexProps.makeDefaultBean()});
        dataMap.put("map", dataInner);
        dataMap.put("maparray", new Map[] {dataInner, dataInner});

        // send event
        epService.getEPRuntime().sendEvent(dataMap, "TypeMapOne");

        Map<String, Object> dataMapTwo = new HashMap<String, Object>(dataMap);
        dataMapTwo.put("id", 2);
        epService.getEPRuntime().sendEvent(dataMapTwo, "TypeMapOne");

        Map<String, Object> dataMapThree = new HashMap<String, Object>(dataMap);
        dataMapThree.put("id", 3);
        epService.getEPRuntime().sendEvent(dataMapThree, "TypeMapTwo");

        EventBean eventBean = listener.assertOneGetNewAndReset();
        // System.out.println(SupportEventTypeAssertionUtil.print(eventBean));
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);
        
        assertEquals(1, ((EventBean)eventBean.getFragment("one[0]")).get("id"));
        assertEquals(2, ((EventBean)eventBean.getFragment("one[1]")).get("id"));
        assertEquals(3, ((EventBean)eventBean.getFragment("two")).get("id"));

        assertEquals("E1", ((EventBean)eventBean.getFragment("one[0].bean")).get("theString"));
        assertEquals("E1", ((EventBean)eventBean.getFragment("one[1].bean")).get("theString"));
        assertEquals("E1", ((EventBean)eventBean.getFragment("two.bean")).get("theString"));

        assertEquals("E2", ((EventBean)eventBean.getFragment("one[0].beanarray[1]")).get("theString"));
        assertEquals("E2", ((EventBean)eventBean.getFragment("two.beanarray[1]")).get("theString"));

        assertEquals("nestedNestedValue", ((EventBean)eventBean.getFragment("one[0].complex.nested.nestedNested")).get("nestedNestedValue"));
        assertEquals("nestedNestedValue", ((EventBean)eventBean.getFragment("two.complex.nested.nestedNested")).get("nestedNestedValue"));

        assertEquals("nestedNestedValue", ((EventBean)eventBean.getFragment("one[0].complexarray[0].nested.nestedNested")).get("nestedNestedValue"));
        assertEquals("nestedNestedValue", ((EventBean)eventBean.getFragment("two.complexarray[0].nested.nestedNested")).get("nestedNestedValue"));

        assertEquals(2000, ((EventBean)eventBean.getFragment("one[0].map")).get("p2id"));
        assertEquals(2000, ((EventBean)eventBean.getFragment("two.map")).get("p2id"));

        assertEquals(2000, ((EventBean)eventBean.getFragment("one[0].maparray[1]")).get("p2id"));
        assertEquals(2000, ((EventBean)eventBean.getFragment("two.maparray[1]")).get("p2id"));
    }

    public void testObjectArrayFragmentTransposedMapEventBean()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("TypeInner", new String[] {"p2id"}, new Object[] {int.class});

        String[] props = {"id", "bean", "beanarray", "complex", "complexarray", "map", "maparray"};
        Object[] types = {int.class, SupportBean.class, SupportBean[].class, SupportBeanComplexProps.class, SupportBeanComplexProps[].class, "TypeInner", "TypeInner[]"};
        epService.getEPAdministrator().getConfiguration().addEventType("TypeMapOne", props, types);
        epService.getEPAdministrator().getConfiguration().addEventType("TypeMapTwo", props, types);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from pattern[one=TypeMapOne until two=TypeMapTwo]");
        stmt.addListener(listener);

        Object[] dataInner = new Object[] {2000};
        Object[] dataArray = new Object[] {1, new SupportBean("E1", 100),
                new SupportBean[] {new SupportBean("E1", 100), new SupportBean("E2", 200)},
                SupportBeanComplexProps.makeDefaultBean(),
                new SupportBeanComplexProps[] {SupportBeanComplexProps.makeDefaultBean()},
                dataInner, new Object[] {dataInner, dataInner}};

        // send event
        epService.getEPRuntime().sendEvent(dataArray, "TypeMapOne");

        Object[] dataArrayTwo = new Object[dataArray.length];
        System.arraycopy(dataArray, 0, dataArrayTwo, 0, dataArray.length);
        dataArrayTwo[0] = 2;
        epService.getEPRuntime().sendEvent(dataArrayTwo, "TypeMapOne");

        Object[] dataArrayThree = new Object[dataArray.length];
        System.arraycopy(dataArray, 0, dataArrayThree, 0, dataArray.length);
        dataArrayThree[0] = 3;
        epService.getEPRuntime().sendEvent(dataArrayThree, "TypeMapTwo");

        EventBean eventBean = listener.assertOneGetNewAndReset();
        // System.out.println(SupportEventTypeAssertionUtil.print(eventBean));
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);

        assertEquals(1, ((EventBean) eventBean.getFragment("one[0]")).get("id"));
        assertEquals(2, ((EventBean) eventBean.getFragment("one[1]")).get("id"));
        assertEquals(3, ((EventBean) eventBean.getFragment("two")).get("id"));

        assertEquals("E1", ((EventBean)eventBean.getFragment("one[0].bean")).get("theString"));
        assertEquals("E1", ((EventBean)eventBean.getFragment("one[1].bean")).get("theString"));
        assertEquals("E1", ((EventBean)eventBean.getFragment("two.bean")).get("theString"));

        assertEquals("E2", ((EventBean)eventBean.getFragment("one[0].beanarray[1]")).get("theString"));
        assertEquals("E2", ((EventBean)eventBean.getFragment("two.beanarray[1]")).get("theString"));

        assertEquals("nestedNestedValue", ((EventBean)eventBean.getFragment("one[0].complex.nested.nestedNested")).get("nestedNestedValue"));
        assertEquals("nestedNestedValue", ((EventBean)eventBean.getFragment("two.complex.nested.nestedNested")).get("nestedNestedValue"));

        assertEquals("nestedNestedValue", ((EventBean)eventBean.getFragment("one[0].complexarray[0].nested.nestedNested")).get("nestedNestedValue"));
        assertEquals("nestedNestedValue", ((EventBean)eventBean.getFragment("two.complexarray[0].nested.nestedNested")).get("nestedNestedValue"));

        assertEquals(2000, ((EventBean)eventBean.getFragment("one[0].map")).get("p2id"));
        assertEquals(2000, ((EventBean)eventBean.getFragment("two.map")).get("p2id"));

        assertEquals(2000, ((EventBean)eventBean.getFragment("one[0].maparray[1]")).get("p2id"));
        assertEquals(2000, ((EventBean)eventBean.getFragment("two.maparray[1]")).get("p2id"));
    }

    public void testMapFragmentMapBeans()
    {
        Map<String, Object> typeLev0 = new HashMap<String, Object>();
        typeLev0.put("p1simple", SupportBean.class);
        typeLev0.put("p1array", SupportBean[].class);
        typeLev0.put("p1complex", SupportBeanComplexProps.class);
        typeLev0.put("p1complexarray", SupportBeanComplexProps[].class);
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev0", typeLev0);

        Map<String, Object> mapOuter = new HashMap<String, Object>();
        mapOuter.put("p0simple", "TypeLev0");
        mapOuter.put("p0array", "TypeLev0[]");
        epService.getEPAdministrator().getConfiguration().addEventType("TypeRoot", mapOuter);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TypeRoot");
        stmt.addListener(listener);

        Map<String, Object> dataInner = new HashMap<String, Object>();
        dataInner.put("p1simple", new SupportBean("E1", 11));
        dataInner.put("p1array", new SupportBean[] {new SupportBean("A1", 21), new SupportBean("A2", 22)});
        dataInner.put("p1complex", SupportBeanComplexProps.makeDefaultBean());
        dataInner.put("p1complexarray", new SupportBeanComplexProps[] {SupportBeanComplexProps.makeDefaultBean(), SupportBeanComplexProps.makeDefaultBean()});

        Map<String, Object> dataRoot = new HashMap<String, Object>();
        dataRoot.put("p0simple", dataInner);
        dataRoot.put("p0array", new Map[] {dataInner,dataInner});

        // send event
        epService.getEPRuntime().sendEvent(dataRoot, "TypeRoot");
        EventBean eventBean = listener.assertOneGetNewAndReset();
        //  System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    comment me in
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);

        assertEquals(11, ((EventBean) eventBean.getFragment("p0simple.p1simple")).get("intPrimitive"));
        assertEquals("A2", ((EventBean) eventBean.getFragment("p0simple.p1array[1]")).get("theString"));
        assertEquals("simple", ((EventBean) eventBean.getFragment("p0simple.p1complex")).get("simpleProperty"));
        assertEquals("simple", ((EventBean) eventBean.getFragment("p0simple.p1complexarray[0]")).get("simpleProperty"));
        assertEquals("nestedValue", ((EventBean) eventBean.getFragment("p0simple.p1complexarray[0].nested")).get("nestedValue"));
        assertEquals("nestedNestedValue", ((EventBean) eventBean.getFragment("p0simple.p1complexarray[0].nested.nestedNested")).get("nestedNestedValue"));

        EventBean assertEvent = (EventBean) eventBean.getFragment("p0simple");
        assertEquals("E1", assertEvent.get("p1simple.theString"));
        assertEquals(11, ((EventBean) assertEvent.getFragment("p1simple")).get("intPrimitive"));
        assertEquals(22, ((EventBean) assertEvent.getFragment("p1array[1]")).get("intPrimitive"));
        assertEquals("nestedNestedValue", ((EventBean) assertEvent.getFragment("p1complex.nested.nestedNested")).get("nestedNestedValue"));

        assertEvent = ((EventBean[]) eventBean.getFragment("p0array"))[0];
        assertEquals("E1", assertEvent.get("p1simple.theString"));
        assertEquals(11, ((EventBean) assertEvent.getFragment("p1simple")).get("intPrimitive"));
        assertEquals(22, ((EventBean) assertEvent.getFragment("p1array[1]")).get("intPrimitive"));

        assertEvent = (EventBean) eventBean.getFragment("p0array[0]");
        assertEquals("E1", assertEvent.get("p1simple.theString"));
        assertEquals(11, ((EventBean) assertEvent.getFragment("p1simple")).get("intPrimitive"));
        assertEquals(22, ((EventBean) assertEvent.getFragment("p1array[1]")).get("intPrimitive"));
    }

    public void testObjectArrayFragmentBeans()
    {
        String[] propsLev0 = {"p1simple", "p1array", "p1complex", "p1complexarray"};
        Object[] typesLev0 = {SupportBean.class, SupportBean[].class, SupportBeanComplexProps.class, SupportBeanComplexProps[].class};
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev0", propsLev0, typesLev0);

        String[] propsOuter = {"p0simple", "p0array"};
        Object[] typesOuter = {"TypeLev0", "TypeLev0[]"};
        epService.getEPAdministrator().getConfiguration().addEventType("TypeRoot", propsOuter, typesOuter);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TypeRoot");
        stmt.addListener(listener);
        assertEquals(Object[].class, stmt.getEventType().getUnderlyingType());

        Object[] dataInner = {new SupportBean("E1", 11), new SupportBean[] {new SupportBean("A1", 21), new SupportBean("A2", 22)},
            SupportBeanComplexProps.makeDefaultBean(), new SupportBeanComplexProps[] {SupportBeanComplexProps.makeDefaultBean(), SupportBeanComplexProps.makeDefaultBean()}};
        Object[] dataRoot = new Object[] {dataInner, new Object[] {dataInner,dataInner}};

        // send event
        epService.getEPRuntime().sendEvent(dataRoot, "TypeRoot");
        EventBean eventBean = listener.assertOneGetNewAndReset();
        //  System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    comment me in
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);

        assertEquals(11, ((EventBean) eventBean.getFragment("p0simple.p1simple")).get("intPrimitive"));
        assertEquals("A2", ((EventBean) eventBean.getFragment("p0simple.p1array[1]")).get("theString"));
        assertEquals("simple", ((EventBean) eventBean.getFragment("p0simple.p1complex")).get("simpleProperty"));
        assertEquals("simple", ((EventBean) eventBean.getFragment("p0simple.p1complexarray[0]")).get("simpleProperty"));
        assertEquals("nestedValue", ((EventBean) eventBean.getFragment("p0simple.p1complexarray[0].nested")).get("nestedValue"));
        assertEquals("nestedNestedValue", ((EventBean) eventBean.getFragment("p0simple.p1complexarray[0].nested.nestedNested")).get("nestedNestedValue"));

        EventBean assertEvent = (EventBean) eventBean.getFragment("p0simple");
        assertEquals("E1", assertEvent.get("p1simple.theString"));
        assertEquals(11, ((EventBean) assertEvent.getFragment("p1simple")).get("intPrimitive"));
        assertEquals(22, ((EventBean) assertEvent.getFragment("p1array[1]")).get("intPrimitive"));
        assertEquals("nestedNestedValue", ((EventBean) assertEvent.getFragment("p1complex.nested.nestedNested")).get("nestedNestedValue"));

        assertEvent = ((EventBean[]) eventBean.getFragment("p0array"))[0];
        assertEquals("E1", assertEvent.get("p1simple.theString"));
        assertEquals(11, ((EventBean) assertEvent.getFragment("p1simple")).get("intPrimitive"));
        assertEquals(22, ((EventBean) assertEvent.getFragment("p1array[1]")).get("intPrimitive"));

        assertEvent = (EventBean) eventBean.getFragment("p0array[0]");
        assertEquals("E1", assertEvent.get("p1simple.theString"));
        assertEquals(11, ((EventBean) assertEvent.getFragment("p1simple")).get("intPrimitive"));
        assertEquals(22, ((EventBean) assertEvent.getFragment("p1array[1]")).get("intPrimitive"));
    }

    public void testMapFragmentMap3Level()
    {
        Map<String, Object> typeLev1 = new HashMap<String, Object>();
        typeLev1.put("p2id", int.class);
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev1", typeLev1);

        Map<String, Object> typeLev0 = new HashMap<String, Object>();
        typeLev0.put("p1simple", "TypeLev1");
        typeLev0.put("p1array", "TypeLev1[]");
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev0", typeLev0);

        Map<String, Object> mapOuter = new HashMap<String, Object>();
        mapOuter.put("p0simple", "TypeLev0");
        mapOuter.put("p0array", "TypeLev0[]");
        epService.getEPAdministrator().getConfiguration().addEventType("TypeRoot", mapOuter);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TypeRoot");
        stmt.addListener(listener);

        Map<String, Object> dataLev1 = new HashMap<String, Object>();
        dataLev1.put("p2id", 10);

        Map<String, Object> dataLev0 = new HashMap<String, Object>();
        dataLev0.put("p1simple", dataLev1);
        dataLev0.put("p1array", new Map[] {dataLev1,dataLev1});

        Map<String, Object> dataRoot = new HashMap<String, Object>();
        dataRoot.put("p0simple", dataLev0);
        dataRoot.put("p0array", new Map[] {dataLev0,dataLev0});

        // send event
        epService.getEPRuntime().sendEvent(dataRoot, "TypeRoot");
        EventBean eventBean = listener.assertOneGetNewAndReset();
        //  System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    comment me in
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);

        assertEquals(10, ((EventBean)eventBean.getFragment("p0simple.p1simple")).get("p2id"));
        assertEquals(10, ((EventBean)eventBean.getFragment("p0array[1].p1simple")).get("p2id"));
        assertEquals(10, ((EventBean)eventBean.getFragment("p0array[1].p1array[0]")).get("p2id"));
        assertEquals(10, ((EventBean)eventBean.getFragment("p0simple.p1array[0]")).get("p2id"));

        // resolve property via fragment
        EventBean assertEvent = (EventBean) eventBean.getFragment("p0simple");
        assertEquals(10, assertEvent.get("p1simple.p2id"));
        assertEquals(10, ((EventBean)assertEvent.getFragment("p1simple")).get("p2id"));

        assertEvent = ((EventBean[]) eventBean.getFragment("p0array"))[1];
        assertEquals(10, assertEvent.get("p1simple.p2id"));
        assertEquals(10, ((EventBean)assertEvent.getFragment("p1simple")).get("p2id"));

        assertEvent = (EventBean) eventBean.getFragment("p0array[0]");
        assertEquals(10, assertEvent.get("p1simple.p2id"));
        assertEquals(10, ((EventBean)assertEvent.getFragment("p1simple")).get("p2id"));

        assertEquals("TypeLev1", eventType.getFragmentType("p0array.p1simple").getFragmentType().getName());
        assertEquals(int.class, eventType.getFragmentType("p0array.p1simple").getFragmentType().getPropertyType("p2id"));
        assertEquals(int.class, eventType.getFragmentType("p0array[0].p1array[0]").getFragmentType().getPropertyDescriptor("p2id").getPropertyType());
        assertFalse(eventType.getFragmentType("p0simple.p1simple").isIndexed());
        assertTrue(eventType.getFragmentType("p0simple.p1array").isIndexed());

        tryInvalid((EventBean) eventBean.getFragment("p0simple"), "p1simple.p1id");
    }

    public void testObjectArrayFragment3Level()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev1", new String[] {"p2id"}, new Object[] {int.class});
        epService.getEPAdministrator().getConfiguration().addEventType("TypeLev0", new String[] {"p1simple", "p1array"}, new Object[] {"TypeLev1", "TypeLev1[]"});
        epService.getEPAdministrator().getConfiguration().addEventType("TypeRoot", new String[] {"p0simple", "p0array"}, new Object[] {"TypeLev0", "TypeLev0[]"});

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TypeRoot");
        stmt.addListener(listener);
        assertEquals(Object[].class, stmt.getEventType().getUnderlyingType());

        Object[] dataLev1 = new Object[] {10};
        Object[] dataLev0 = new Object[] {dataLev1, new Object[] {dataLev1,dataLev1}};
        Object[] dataRoot = new Object[] {dataLev0, new Object[] {dataLev0,dataLev0}};

        // send event
        epService.getEPRuntime().sendEvent(dataRoot, "TypeRoot");
        EventBean eventBean = listener.assertOneGetNewAndReset();
        //  System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    comment me in
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);

        assertEquals(10, ((EventBean)eventBean.getFragment("p0simple.p1simple")).get("p2id"));
        assertEquals(10, ((EventBean)eventBean.getFragment("p0array[1].p1simple")).get("p2id"));
        assertEquals(10, ((EventBean)eventBean.getFragment("p0array[1].p1array[0]")).get("p2id"));
        assertEquals(10, ((EventBean)eventBean.getFragment("p0simple.p1array[0]")).get("p2id"));

        // resolve property via fragment
        EventBean assertEvent = (EventBean) eventBean.getFragment("p0simple");
        assertEquals(10, assertEvent.get("p1simple.p2id"));
        assertEquals(10, ((EventBean)assertEvent.getFragment("p1simple")).get("p2id"));

        assertEvent = ((EventBean[]) eventBean.getFragment("p0array"))[1];
        assertEquals(10, assertEvent.get("p1simple.p2id"));
        assertEquals(10, ((EventBean)assertEvent.getFragment("p1simple")).get("p2id"));

        assertEvent = (EventBean) eventBean.getFragment("p0array[0]");
        assertEquals(10, assertEvent.get("p1simple.p2id"));
        assertEquals(10, ((EventBean)assertEvent.getFragment("p1simple")).get("p2id"));

        assertEquals("TypeLev1", eventType.getFragmentType("p0array.p1simple").getFragmentType().getName());
        assertEquals(int.class, eventType.getFragmentType("p0array.p1simple").getFragmentType().getPropertyType("p2id"));
        assertEquals(int.class, eventType.getFragmentType("p0array[0].p1array[0]").getFragmentType().getPropertyDescriptor("p2id").getPropertyType());
        assertFalse(eventType.getFragmentType("p0simple.p1simple").isIndexed());
        assertTrue(eventType.getFragmentType("p0simple.p1array").isIndexed());

        tryInvalid((EventBean) eventBean.getFragment("p0simple"), "p1simple.p1id");
    }

    public void testFragmentMapMulti()
    {
        Map<String, Object> mapInnerInner = new HashMap<String, Object>();
        mapInnerInner.put("p2id", int.class);

        Map<String, Object> mapInner = new HashMap<String, Object>();
        mapInner.put("p1bean", SupportBean.class);
        mapInner.put("p1beanComplex", SupportBeanComplexProps.class);
        mapInner.put("p1beanArray", SupportBean[].class);
        mapInner.put("p1innerId", int.class);
        mapInner.put("p1innerMap", mapInnerInner);
        epService.getEPAdministrator().getConfiguration().addEventType("InnerMap", mapInner);

        Map<String, Object> mapOuter = new HashMap<String, Object>();
        mapOuter.put("p0simple", "InnerMap");
        mapOuter.put("p0array", "InnerMap[]");
        epService.getEPAdministrator().getConfiguration().addEventType("OuterMap", mapOuter);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from OuterMap");
        stmt.addListener(listener);

        Map<String, Object> dataInnerInner = new HashMap<String, Object>();
        dataInnerInner.put("p2id", 10);

        Map<String, Object> dataInner = new HashMap<String, Object>();
        dataInner.put("p1bean", new SupportBean("string1", 2000));
        dataInner.put("p1beanComplex", SupportBeanComplexProps.makeDefaultBean());
        dataInner.put("p1beanArray", new SupportBean[] {new SupportBean("string2", 1), new SupportBean("string3", 2)});
        dataInner.put("p1innerId", 50);
        dataInner.put("p1innerMap", dataInnerInner);

        Map<String, Object> dataOuter = new HashMap<String, Object>();
        dataOuter.put("p0simple", dataInner);
        dataOuter.put("p0array", new Map[] {dataInner,dataInner});

        // send event
        epService.getEPRuntime().sendEvent(dataOuter, "OuterMap");
        EventBean eventBean = listener.assertOneGetNewAndReset();
        // System.out.println(SupportEventTypeAssertionUtil.print(eventBean));     comment me in
        EventType eventType = eventBean.getEventType();
        SupportEventTypeAssertionUtil.assertConsistency(eventType);

        // Fragment-to-simple
        assertTrue(eventType.getPropertyDescriptor("p0simple").isFragment());
        assertEquals(int.class, eventType.getFragmentType("p0simple").getFragmentType().getPropertyDescriptor("p1innerId").getPropertyType());
        EventBean p0simpleEvent = (EventBean) eventBean.getFragment("p0simple");
        assertEquals(50, p0simpleEvent.get("p1innerId"));
        p0simpleEvent = (EventBean) eventBean.getFragment("p0array[0]");
        assertEquals(50, p0simpleEvent.get("p1innerId"));

        // Fragment-to-bean
        EventBean[] p0arrayEvents = (EventBean[]) eventBean.getFragment("p0array");
        assertSame(p0arrayEvents[0].getEventType(), p0simpleEvent.getEventType());
        assertEquals("string1", eventBean.get("p0array[0].p1bean.theString"));
        assertEquals("string1", ((EventBean) eventBean.getFragment("p0array[0].p1bean")).get("theString"));

        EventBean innerOne = (EventBean) eventBean.getFragment("p0array[0]");
        assertEquals("string1", ((EventBean) innerOne.getFragment("p1bean")).get("theString"));
        assertEquals("string1", innerOne.get("p1bean.theString"));
        innerOne = (EventBean) eventBean.getFragment("p0simple");
        assertEquals("string1", ((EventBean) innerOne.getFragment("p1bean")).get("theString"));
        assertEquals("string1", innerOne.get("p1bean.theString"));
    }

    private void tryInvalid(EventBean theEvent, String property)
    {
        try
        {
            theEvent.get(property);
            fail();
        }
        catch (PropertyAccessException ex)
        {
            // expected
        }
    }
}
