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
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.suite.event.objectarray.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.regressionlib.suite.event.map.EventMapCore.makeMap;

public class TestSuiteEventObjectArray extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEventObjectArrayCore() {
        RegressionRunner.run(session, EventObjectArrayCore.executions());
    }

    public void testEventObjectArrayNestedMap() {
        RegressionRunner.run(session, new EventObjectArrayNestedMap());
    }

    public void testEventObjectArrayInheritanceConfigInit() {
        RegressionRunner.run(session, new EventObjectArrayInheritanceConfigInit());
    }

    public void testEventObjectArrayEventNestedPojo() {
        RegressionRunner.run(session, new EventObjectArrayEventNestedPojo());
    }

    public void testEventObjectArrayEventNested() {
        RegressionRunner.run(session, EventObjectArrayEventNested.executions());
    }

    private static void configure(Configuration configuration) {

        for (Class clazz : new Class[]{SupportBean.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        String[] myObjectArrayEvent = {"myInt", "myString", "beanA"};
        Object[] MyObjectArrayTypes = {Integer.class, String.class, SupportBeanComplexProps.class};
        configuration.getCommon().addEventType("MyObjectArrayEvent", myObjectArrayEvent, MyObjectArrayTypes);

        String[] myArrayOAProps = {"p0", "p1"};
        Object[] MyArrayOATypes = {int[].class, SupportBean[].class};
        configuration.getCommon().addEventType("MyArrayOA", myArrayOAProps, MyArrayOATypes);

        configuration.getCommon().addEventType("MyArrayOAMapOuter", new String[]{"outer"}, new Object[]{"MyArrayOA"});

        Map<String, Object> mappedDef = makeMap(new Object[][]{{"p0", Map.class}});
        configuration.getCommon().addEventType("MyMappedPropertyMap", mappedDef);

        Map<String, Object> mappedDefOuter = makeMap(new Object[][]{{"outer", mappedDef}});
        configuration.getCommon().addEventType("MyMappedPropertyMapOuter", mappedDefOuter);

        Map<String, Object> mappedDefOuterTwo = makeMap(new Object[][]{{"outerTwo", SupportBeanComplexProps.class}});
        configuration.getCommon().addEventType("MyMappedPropertyMapOuterTwo", mappedDefOuterTwo);

        Map<String, Object> namedDef = makeMap(new Object[][]{{"n0", int.class}});
        configuration.getCommon().addEventType("MyNamedMap", namedDef);

        Map<String, Object> eventDef = makeMap(new Object[][]{{"p0", "MyNamedMap"}, {"p1", "MyNamedMap[]"}});
        configuration.getCommon().addEventType("MyMapWithAMap", eventDef);
        configuration.getCommon().addEventType("MyObjectArrayMapOuter", new String[]{"outer"}, new Object[]{eventDef});

        configuration.getCommon().addEventType("MyOAWithAMap", new String[]{"p0", "p1"}, new Object[]{"MyNamedMap", "MyNamedMap[]"});

        configuration.getCommon().addEventType("TypeLev1", new String[]{"p1id"}, new Object[]{int.class});
        configuration.getCommon().addEventType("TypeLev0", new String[]{"p0id", "p1"}, new Object[]{int.class, "TypeLev1"});
        configuration.getCommon().addEventType("TypeRoot", new String[]{"rootId", "p0"}, new Object[]{int.class, "TypeLev0"});

        Pair<String[], Object[]> pair = getTestDef();
        configuration.getCommon().addEventType("NestedObjectArr", pair.getFirst(), pair.getSecond());

        configuration.getCommon().addEventType("MyNested", new String[]{"bean"}, new Object[]{EventObjectArrayEventNestedPojo.MyNested.class});

        configuration.getCommon().addEventType("RootEvent", new String[]{"base"}, new Object[]{String.class});
        configuration.getCommon().addEventType("Sub1Event", new String[]{"sub1"}, new Object[]{String.class});
        configuration.getCommon().addEventType("Sub2Event", new String[]{"sub2"}, new Object[]{String.class});
        configuration.getCommon().addEventType("SubAEvent", new String[]{"suba"}, new Object[]{String.class});
        configuration.getCommon().addEventType("SubBEvent", new String[]{"subb"}, new Object[]{String.class});

        configuration.getCommon().addObjectArraySuperType("Sub1Event", "RootEvent");
        configuration.getCommon().addObjectArraySuperType("Sub2Event", "RootEvent");
        configuration.getCommon().addObjectArraySuperType("SubAEvent", "Sub1Event");
        configuration.getCommon().addObjectArraySuperType("SubBEvent", "SubAEvent");

        Map<String, Object> nestedOALev2def = new HashMap<String, Object>();
        nestedOALev2def.put("sb", "SupportBean");
        Map<String, Object> nestedOALev1def = new HashMap<String, Object>();
        nestedOALev1def.put("lev1name", nestedOALev2def);
        configuration.getCommon().addEventType("MyMapNestedObjectArray", new String[]{"lev0name"}, new Object[]{nestedOALev1def});
    }

    private static Pair<String[], Object[]> getTestDef() {
        Map<String, Object> levelThree = makeMap(new Object[][]{
            {"simpleThree", Long.class},
            {"objectThree", SupportBean_B.class},
        });

        Map<String, Object> levelTwo = makeMap(new Object[][]{
            {"simpleTwo", Integer.class},
            {"objectTwo", SupportBeanCombinedProps.class},
            {"nodefmapTwo", Map.class},
            {"mapTwo", levelThree},
        });

        Map<String, Object> levelOne = makeMap(new Object[][]{
            {"simpleOne", Integer.class},
            {"objectOne", SupportBeanComplexProps.class},
            {"nodefmapOne", Map.class},
            {"mapOne", levelTwo}
        });

        String[] levelZeroProps = {"simple", "object", "nodefmap", "map"};
        Object[] levelZeroTypes = {String.class, SupportBean_A.class, Map.class, levelOne};
        return new Pair<String[], Object[]>(levelZeroProps, levelZeroTypes);
    }
}
