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
package com.espertech.esper.regressionrun.suite.event;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.suite.event.map.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestSuiteEventMap extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEventMapCore() {
        RegressionRunner.run(session, EventMapCore.executions());
    }

    public void testEventMapPropertyDynamic() {
        RegressionRunner.run(session, new EventMapPropertyDynamic());
    }

    public void testEventMapObjectArrayInterUse() {
        RegressionRunner.run(session, new EventMapObjectArrayInterUse());
    }

    public void testEventMapInheritanceInitTime() {
        RegressionRunner.run(session, new EventMapInheritanceInitTime());
    }

    public void testEventMapNestedEscapeDot() {
        RegressionRunner.run(session, new EventMapNestedEscapeDot());
    }

    public void testEventMapNestedConfigStatic() {
        RegressionRunner.run(session, new EventMapNestedConfigStatic());
    }

    public void testEventMapNested() {
        RegressionRunner.run(session, EventMapNested.executions());
    }

    public void testEventMapProperties() {
        RegressionRunner.run(session, EventMapProperties.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        Properties myMapEvent = new Properties();
        myMapEvent.put("myInt", "int");
        myMapEvent.put("myString", "string");
        myMapEvent.put("beanA", SupportBeanComplexProps.class.getName());
        myMapEvent.put("myStringArray", "string[]");
        configuration.getCommon().addEventType("myMapEvent", myMapEvent);

        Map<String, Object> myMapLev2def = new HashMap<String, Object>();
        myMapLev2def.put("sb", "SupportBean");
        Map<String, Object> myMapLev1def = new HashMap<String, Object>();
        myMapLev1def.put("lev1name", myMapLev2def);
        Map<String, Object> myMapLev0def = new HashMap<String, Object>();
        myMapLev0def.put("lev0name", myMapLev1def);
        configuration.getCommon().addEventType("MyMap", myMapLev0def);

        Map<String, Object> root = EventMapCore.makeMap(new Object[][]{{"base", String.class}});
        Map<String, Object> sub1 = EventMapCore.makeMap(new Object[][]{{"sub1", String.class}});
        Map<String, Object> sub2 = EventMapCore.makeMap(new Object[][]{{"sub2", String.class}});
        Properties suba = EventMapCore.makeProperties(new Object[][]{{"suba", String.class}});
        Map<String, Object> subb = EventMapCore.makeMap(new Object[][]{{"subb", String.class}});
        configuration.getCommon().addEventType("RootEvent", root);
        configuration.getCommon().addEventType("Sub1Event", sub1);
        configuration.getCommon().addEventType("Sub2Event", sub2);
        configuration.getCommon().addEventType("SubAEvent", suba);
        configuration.getCommon().addEventType("SubBEvent", subb);

        configuration.getCommon().addMapSuperType("Sub1Event", "RootEvent");
        configuration.getCommon().addMapSuperType("Sub2Event", "RootEvent");
        configuration.getCommon().addMapSuperType("SubAEvent", "Sub1Event");
        configuration.getCommon().addMapSuperType("SubBEvent", "Sub1Event");
        configuration.getCommon().addMapSuperType("SubBEvent", "Sub2Event");

        Map<String, Object> nestedMapLevelThree = EventMapCore.makeMap(new Object[][]{
            {"simpleThree", Long.class},
            {"objectThree", SupportBean_B.class},
        });
        Map<String, Object> nestedMapLevelTwo = EventMapCore.makeMap(new Object[][]{
            {"simpleTwo", Integer.class},
            {"objectTwo", SupportBeanCombinedProps.class},
            {"nodefmapTwo", Map.class},
            {"mapTwo", nestedMapLevelThree},
        });
        Map<String, Object> nestedMapLevelOne = EventMapCore.makeMap(new Object[][]{
            {"simpleOne", Integer.class},
            {"objectOne", SupportBeanComplexProps.class},
            {"nodefmapOne", Map.class},
            {"mapOne", nestedMapLevelTwo}
        });
        Map<String, Object> nestedMapLevelZero = EventMapCore.makeMap(new Object[][]{
            {"simple", String.class},
            {"object", SupportBean_A.class},
            {"nodefmap", Map.class},
            {"map", nestedMapLevelOne}
        });
        configuration.getCommon().addEventType("NestedMap", nestedMapLevelZero);

        Map<String, Object> type = EventMapCore.makeMap(new Object[][]{
            {"base1", String.class},
            {"base2", EventMapCore.makeMap(new Object[][]{{"n1", int.class}})}
        });
        configuration.getCommon().addEventType("MyEvent", type);

        Properties properties = new Properties();
        properties.put("myInt", int.class.getName());
        properties.put("byteArr", byte[].class.getName());
        properties.put("myInt2", "int");
        properties.put("double", "double");
        properties.put("boolean", "boolean");
        properties.put("long", "long");
        properties.put("astring", "string");
        configuration.getCommon().addEventType("MyPrimMapEvent", properties);

        Properties myLevel2 = new Properties();
        myLevel2.put("innermap", Map.class.getName());
        configuration.getCommon().addEventType("MyLevel2", myLevel2);

        // create a named map
        Map<String, Object> namedDef = EventMapCore.makeMap(new Object[][]{{"n0", int.class}});
        configuration.getCommon().addEventType("MyNamedMap", namedDef);

        // create a map using the name
        Map<String, Object> eventDef = EventMapCore.makeMap(new Object[][]{{"p0", "MyNamedMap"}, {"p1", "MyNamedMap[]"}});
        configuration.getCommon().addEventType("MyMapWithAMap", eventDef);

        // test map containing first-level property that is an array of primitive or Class
        Map<String, Object> arrayDef = EventMapCore.makeMap(new Object[][]{{"p0", int[].class}, {"p1", SupportBean[].class}});
        configuration.getCommon().addEventType("MyArrayMap", arrayDef);

        // test map at the second level of a nested map that is an array of primitive or Class
        Map<String, Object> arrayDefOuter = EventMapCore.makeMap(new Object[][]{{"outer", arrayDef}});
        configuration.getCommon().addEventType("MyArrayMapOuter", arrayDefOuter);

        // test map containing first-level property that is an array of primitive or Class
        Map<String, Object> mappedDef = EventMapCore.makeMap(new Object[][]{{"p0", Map.class}});
        configuration.getCommon().addEventType("MyMappedPropertyMap", mappedDef);

        // test map at the second level of a nested map that is an array of primitive or Class
        Map<String, Object> mappedDefOuter = EventMapCore.makeMap(new Object[][]{{"outer", mappedDef}});
        configuration.getCommon().addEventType("MyMappedPropertyMapOuter", mappedDefOuter);

        Map<String, Object> mappedDefOuterTwo = EventMapCore.makeMap(new Object[][]{{"outerTwo", SupportBeanComplexProps.class}});
        configuration.getCommon().addEventType("MyMappedPropertyMapOuterTwo", mappedDefOuterTwo);

        // create a named map
        Map<String, Object> myNamedMap = EventMapCore.makeMap(new Object[][]{{"n0", int.class}});
        configuration.getCommon().addEventType("MyNamedMap", myNamedMap);

        // create a map using the name
        Map<String, Object> myMapWithAMap = EventMapCore.makeMap(new Object[][]{{"p0", "MyNamedMap"}, {"p1", "MyNamedMap[]"}});
        configuration.getCommon().addEventType("MyMapWithAMap", myMapWithAMap);

        // test named-map at the second level of a nested map
        Map<String, Object> myArrayMapTwo = EventMapCore.makeMap(new Object[][]{{"outer", myMapWithAMap}});
        configuration.getCommon().addEventType("MyArrayMapTwo", myArrayMapTwo);

        configuration.getCommon().addEventType("MapType", Collections.<String, Object>singletonMap("im", String.class));
        configuration.getCommon().addEventType("OAType", "p0,p1,p2,p3".split(","), new Object[]{String.class, "MapType", "MapType[]", Collections.<String, Object>singletonMap("om", String.class)});

        Map<String, Object> definition = EventMapCore.makeMap(new Object[][]{
            {"a.b", int.class},
            {"a.b.c", int.class},
            {"nes.", int.class},
            {"nes.nes2", EventMapCore.makeMap(new Object[][]{{"x.y", int.class}})}
        });
        configuration.getCommon().addEventType("DotMap", definition);

        Map<String, Object> nmwspPropertiesNestedNested = new HashMap<String, Object>();
        nmwspPropertiesNestedNested.put("n1n1", String.class);

        Map<String, Object> nmwspPropertiesNested = new HashMap<String, Object>();
        nmwspPropertiesNested.put("n1", String.class);
        nmwspPropertiesNested.put("n2", nmwspPropertiesNestedNested);

        Map<String, Object> nmwspRoot = new HashMap<String, Object>();
        nmwspRoot.put("nested", nmwspPropertiesNested);

        configuration.getCommon().addEventType("NestedMapWithSimpleProps", nmwspRoot);
    }
}
