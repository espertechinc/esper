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
package com.espertech.esper.regressionlib.suite.event.bean;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanCombinedProps;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class EventBeanPropertyResolutionFragment {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLBeanMapSimpleTypes());
        execs.add(new EPLBeanObjectArraySimpleTypes());
        execs.add(new EPLBeanWrapperFragmentWithMap());
        execs.add(new EPLBeanWrapperFragmentWithObjectArray());
        execs.add(new EPLBeanNativeBeanFragment());
        execs.add(new EPLBeanMapFragmentMapNested());
        execs.add(new EPLBeanObjectArrayFragmentObjectArrayNested());
        execs.add(new EPLBeanMapFragmentMapUnnamed());
        execs.add(new EPLBeanMapFragmentTransposedMapEventBean());
        execs.add(new EPLBeanObjectArrayFragmentTransposedMapEventBean());
        execs.add(new EPLBeanMapFragmentMapBeans());
        execs.add(new EPLBeanObjectArrayFragmentBeans());
        execs.add(new EPLBeanMapFragmentMap3Level());
        execs.add(new EPLBeanObjectArrayFragment3Level());
        execs.add(new EPLBeanFragmentMapMulti());
        return execs;
    }

    private static class EPLBeanMapSimpleTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from MSTypeOne").addListener("s0");

            Map<String, Object> dataInner = new HashMap<String, Object>();
            dataInner.put("p1someval", "A");

            Map<String, Object> dataRoot = new HashMap<String, Object>();
            dataRoot.put("p0simple", 99);
            dataRoot.put("p0array", new int[]{101, 102});
            dataRoot.put("p0map", dataInner);

            // send event
            env.sendEventMap(dataRoot, "MSTypeOne");
            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
            //System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    //comment me in
            EventType eventType = eventBean.getEventType();
            SupportEventTypeAssertionUtil.assertConsistency(eventType);

            // resolve property via fragment
            assertNull(eventType.getFragmentType("p0int"));
            assertNull(eventType.getFragmentType("p0intarray"));
            assertNull(eventBean.getFragment("p0map?"));
            assertNull(eventBean.getFragment("p0intarray[0]?"));
            assertNull(eventBean.getFragment("p0map('a')?"));

            env.undeployAll();
        }
    }

    private static class EPLBeanObjectArraySimpleTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select * from OASimple").addListener("s0");

            Map<String, Object> dataInner = new HashMap<String, Object>();
            dataInner.put("p1someval", "A");
            Object[] dataRoot = new Object[]{99, new int[]{101, 102}, dataInner};

            // send event
            env.sendEventObjectArray(dataRoot, "OASimple");
            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
            //System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    //comment me in
            EventType eventType = eventBean.getEventType();
            SupportEventTypeAssertionUtil.assertConsistency(eventType);

            // resolve property via fragment
            assertNull(eventType.getFragmentType("p0int"));
            assertNull(eventType.getFragmentType("p0intarray"));
            assertNull(eventBean.getFragment("p0map?"));
            assertNull(eventBean.getFragment("p0intarray[0]?"));
            assertNull(eventBean.getFragment("p0map('a')?"));

            env.undeployAll();
        }
    }

    private static class EPLBeanWrapperFragmentWithMap implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select *, p0simple.p1id + 1 as plusone, p0bean as mybean from Frosty");
            env.addListener("s0");

            Map<String, Object> dataInner = new HashMap<String, Object>();
            dataInner.put("p1id", 10);

            Map<String, Object> dataRoot = new HashMap<String, Object>();
            dataRoot.put("p0simple", dataInner);
            dataRoot.put("p0bean", SupportBeanComplexProps.makeDefaultBean());

            // send event
            env.sendEventMap(dataRoot, "Frosty");
            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
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
            assertEquals("nestedNestedValue", ((EventBean) eventBean.getFragment("mybean.nested.nestedNested")).get("nestedNestedValue"));

            env.undeployAll();
        }
    }

    private static class EPLBeanWrapperFragmentWithObjectArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select *, p0simple.p1id + 1 as plusone, p0bean as mybean from WheatRoot");
            env.addListener("s0");

            env.sendEventObjectArray(new Object[]{new Object[]{10}, SupportBeanComplexProps.makeDefaultBean()}, "WheatRoot");

            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
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
            assertEquals("nestedNestedValue", ((EventBean) eventBean.getFragment("mybean.nested.nestedNested")).get("nestedNestedValue"));

            env.undeployAll();
        }
    }

    private static class EPLBeanNativeBeanFragment implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from SupportBeanComplexProps").addListener("s0");

            // assert nested fragments
            env.sendEventBean(SupportBeanComplexProps.makeDefaultBean());
            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
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
            env.undeployAll();

            // assert indexed fragments
            env.compileDeploy("@name('s0') select * from SupportBeanCombinedProps").addListener("s0");
            SupportBeanCombinedProps eventObject = SupportBeanCombinedProps.makeDefaultBean();
            env.sendEventBean(eventObject);
            eventBean = env.listener("s0").assertOneGetNewAndReset();
            SupportEventTypeAssertionUtil.assertConsistency(eventBean.getEventType());
            //System.out.println(SupportEventTypeAssertionUtil.print(eventBean));

            assertTrue(eventBean.getEventType().getPropertyDescriptor("array").isFragment());
            assertTrue(eventBean.getEventType().getPropertyDescriptor("array").isIndexed());
            EventBean[] eventArray = (EventBean[]) eventBean.getFragment("array");
            assertEquals(3, eventArray.length);

            EventBean eventElement = eventArray[0];
            assertSame(eventObject.getArray()[0].getMapped("0ma"), eventElement.get("mapped('0ma')"));
            assertSame(eventObject.getArray()[0].getMapped("0ma"), ((EventBean) eventBean.getFragment("array[0]")).get("mapped('0ma')"));
            assertSame(eventObject.getArray()[0].getMapped("0ma"), ((EventBean) eventBean.getFragment("array[0]?")).get("mapped('0ma')"));

            env.undeployAll();
        }
    }

    private static class EPLBeanMapFragmentMapNested implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from HomerunRoot").addListener("s0");

            Map<String, Object> dataInner = new HashMap<String, Object>();
            dataInner.put("p1id", 10);

            Map<String, Object> dataRoot = new HashMap<String, Object>();
            dataRoot.put("p0simple", dataInner);
            dataRoot.put("p0array", new Map[]{dataInner, dataInner});

            // send event
            env.sendEventMap(dataRoot, "HomerunRoot");
            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
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

            env.undeployAll();
        }
    }

    private static class EPLBeanObjectArrayFragmentObjectArrayNested implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select * from GoalRoot").addListener("s0");

            assertEquals(Object[].class, env.statement("s0").getEventType().getUnderlyingType());

            env.sendEventObjectArray(new Object[]{new Object[]{10}, new Object[]{new Object[]{20}, new Object[]{21}}}, "GoalRoot");

            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
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

            env.undeployAll();
        }
    }

    private static class EPLBeanMapFragmentMapUnnamed implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select * from FlywheelRoot").addListener("s0");

            Map<String, Object> dataInner = new HashMap<String, Object>();
            dataInner.put("p1id", 10);

            Map<String, Object> dataRoot = new HashMap<String, Object>();
            dataRoot.put("p0simple", dataInner);

            // send event
            env.sendEventMap(dataRoot, "FlywheelRoot");
            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
            //  System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    comment me in
            EventType eventType = eventBean.getEventType();
            SupportEventTypeAssertionUtil.assertConsistency(eventType);

            assertFalse(eventType.getPropertyDescriptor("p0simple").isFragment());
            assertNull(eventBean.getFragment("p0simple"));

            // resolve property via getter
            assertEquals(10, eventBean.get("p0simple.p1id"));

            env.undeployAll();
        }
    }

    private static class EPLBeanMapFragmentTransposedMapEventBean implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select * from pattern[one=GistMapOne until two=GistMapTwo]").addListener("s0");

            Map<String, Object> dataInner = new HashMap<String, Object>();
            dataInner.put("p2id", 2000);
            Map<String, Object> dataMap = new HashMap<String, Object>();
            dataMap.put("id", 1);
            dataMap.put("bean", new SupportBean("E1", 100));
            dataMap.put("beanarray", new SupportBean[]{new SupportBean("E1", 100), new SupportBean("E2", 200)});
            dataMap.put("complex", SupportBeanComplexProps.makeDefaultBean());
            dataMap.put("complexarray", new SupportBeanComplexProps[]{SupportBeanComplexProps.makeDefaultBean()});
            dataMap.put("map", dataInner);
            dataMap.put("maparray", new Map[]{dataInner, dataInner});

            // send event
            env.sendEventMap(dataMap, "GistMapOne");

            Map<String, Object> dataMapTwo = new HashMap<String, Object>(dataMap);
            dataMapTwo.put("id", 2);
            env.sendEventMap(dataMapTwo, "GistMapOne");

            Map<String, Object> dataMapThree = new HashMap<String, Object>(dataMap);
            dataMapThree.put("id", 3);
            env.sendEventMap(dataMapThree, "GistMapTwo");

            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
            // System.out.println(SupportEventTypeAssertionUtil.print(eventBean));
            EventType eventType = eventBean.getEventType();
            SupportEventTypeAssertionUtil.assertConsistency(eventType);

            assertEquals(1, ((EventBean) eventBean.getFragment("one[0]")).get("id"));
            assertEquals(2, ((EventBean) eventBean.getFragment("one[1]")).get("id"));
            assertEquals(3, ((EventBean) eventBean.getFragment("two")).get("id"));

            assertEquals("E1", ((EventBean) eventBean.getFragment("one[0].bean")).get("theString"));
            assertEquals("E1", ((EventBean) eventBean.getFragment("one[1].bean")).get("theString"));
            assertEquals("E1", ((EventBean) eventBean.getFragment("two.bean")).get("theString"));

            assertEquals("E2", ((EventBean) eventBean.getFragment("one[0].beanarray[1]")).get("theString"));
            assertEquals("E2", ((EventBean) eventBean.getFragment("two.beanarray[1]")).get("theString"));

            assertEquals("nestedNestedValue", ((EventBean) eventBean.getFragment("one[0].complex.nested.nestedNested")).get("nestedNestedValue"));
            assertEquals("nestedNestedValue", ((EventBean) eventBean.getFragment("two.complex.nested.nestedNested")).get("nestedNestedValue"));

            assertEquals("nestedNestedValue", ((EventBean) eventBean.getFragment("one[0].complexarray[0].nested.nestedNested")).get("nestedNestedValue"));
            assertEquals("nestedNestedValue", ((EventBean) eventBean.getFragment("two.complexarray[0].nested.nestedNested")).get("nestedNestedValue"));

            assertEquals(2000, ((EventBean) eventBean.getFragment("one[0].map")).get("p2id"));
            assertEquals(2000, ((EventBean) eventBean.getFragment("two.map")).get("p2id"));

            assertEquals(2000, ((EventBean) eventBean.getFragment("one[0].maparray[1]")).get("p2id"));
            assertEquals(2000, ((EventBean) eventBean.getFragment("two.maparray[1]")).get("p2id"));

            env.undeployAll();
        }
    }

    private static class EPLBeanObjectArrayFragmentTransposedMapEventBean implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select * from pattern[one=CashMapOne until two=CashMapTwo]").addListener("s0");

            Object[] dataInner = new Object[]{2000};
            Object[] dataArray = new Object[]{1, new SupportBean("E1", 100),
                new SupportBean[]{new SupportBean("E1", 100), new SupportBean("E2", 200)},
                SupportBeanComplexProps.makeDefaultBean(),
                new SupportBeanComplexProps[]{SupportBeanComplexProps.makeDefaultBean()},
                dataInner, new Object[]{dataInner, dataInner}};

            // send event
            env.sendEventObjectArray(dataArray, "CashMapOne");

            Object[] dataArrayTwo = new Object[dataArray.length];
            System.arraycopy(dataArray, 0, dataArrayTwo, 0, dataArray.length);
            dataArrayTwo[0] = 2;
            env.sendEventObjectArray(dataArrayTwo, "CashMapOne");

            Object[] dataArrayThree = new Object[dataArray.length];
            System.arraycopy(dataArray, 0, dataArrayThree, 0, dataArray.length);
            dataArrayThree[0] = 3;
            env.sendEventObjectArray(dataArrayThree, "CashMapTwo");

            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
            // System.out.println(SupportEventTypeAssertionUtil.print(eventBean));
            EventType eventType = eventBean.getEventType();
            SupportEventTypeAssertionUtil.assertConsistency(eventType);

            assertEquals(1, ((EventBean) eventBean.getFragment("one[0]")).get("id"));
            assertEquals(2, ((EventBean) eventBean.getFragment("one[1]")).get("id"));
            assertEquals(3, ((EventBean) eventBean.getFragment("two")).get("id"));

            assertEquals("E1", ((EventBean) eventBean.getFragment("one[0].bean")).get("theString"));
            assertEquals("E1", ((EventBean) eventBean.getFragment("one[1].bean")).get("theString"));
            assertEquals("E1", ((EventBean) eventBean.getFragment("two.bean")).get("theString"));

            assertEquals("E2", ((EventBean) eventBean.getFragment("one[0].beanarray[1]")).get("theString"));
            assertEquals("E2", ((EventBean) eventBean.getFragment("two.beanarray[1]")).get("theString"));

            assertEquals("nestedNestedValue", ((EventBean) eventBean.getFragment("one[0].complex.nested.nestedNested")).get("nestedNestedValue"));
            assertEquals("nestedNestedValue", ((EventBean) eventBean.getFragment("two.complex.nested.nestedNested")).get("nestedNestedValue"));

            assertEquals("nestedNestedValue", ((EventBean) eventBean.getFragment("one[0].complexarray[0].nested.nestedNested")).get("nestedNestedValue"));
            assertEquals("nestedNestedValue", ((EventBean) eventBean.getFragment("two.complexarray[0].nested.nestedNested")).get("nestedNestedValue"));

            assertEquals(2000, ((EventBean) eventBean.getFragment("one[0].map")).get("p2id"));
            assertEquals(2000, ((EventBean) eventBean.getFragment("two.map")).get("p2id"));

            assertEquals(2000, ((EventBean) eventBean.getFragment("one[0].maparray[1]")).get("p2id"));
            assertEquals(2000, ((EventBean) eventBean.getFragment("two.maparray[1]")).get("p2id"));

            env.undeployAll();
        }
    }

    private static class EPLBeanMapFragmentMapBeans implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select * from TXTypeRoot").addListener("s0");

            Map<String, Object> dataInner = new HashMap<String, Object>();
            dataInner.put("p1simple", new SupportBean("E1", 11));
            dataInner.put("p1array", new SupportBean[]{new SupportBean("A1", 21), new SupportBean("A2", 22)});
            dataInner.put("p1complex", SupportBeanComplexProps.makeDefaultBean());
            dataInner.put("p1complexarray", new SupportBeanComplexProps[]{SupportBeanComplexProps.makeDefaultBean(), SupportBeanComplexProps.makeDefaultBean()});

            Map<String, Object> dataRoot = new HashMap<String, Object>();
            dataRoot.put("p0simple", dataInner);
            dataRoot.put("p0array", new Map[]{dataInner, dataInner});

            // send event
            env.sendEventMap(dataRoot, "TXTypeRoot");
            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
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

            env.undeployAll();
        }
    }

    private static class EPLBeanObjectArrayFragmentBeans implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select * from LocalTypeRoot").addListener("s0");

            assertEquals(Object[].class, env.statement("s0").getEventType().getUnderlyingType());

            Object[] dataInner = {new SupportBean("E1", 11), new SupportBean[]{new SupportBean("A1", 21), new SupportBean("A2", 22)},
                SupportBeanComplexProps.makeDefaultBean(), new SupportBeanComplexProps[]{SupportBeanComplexProps.makeDefaultBean(), SupportBeanComplexProps.makeDefaultBean()}};
            Object[] dataRoot = new Object[]{dataInner, new Object[]{dataInner, dataInner}};

            // send event
            env.sendEventObjectArray(dataRoot, "LocalTypeRoot");
            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
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

            env.undeployAll();
        }
    }

    private static class EPLBeanMapFragmentMap3Level implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select * from JimTypeRoot").addListener("s0");

            Map<String, Object> dataLev1 = new HashMap<String, Object>();
            dataLev1.put("p2id", 10);

            Map<String, Object> dataLev0 = new HashMap<String, Object>();
            dataLev0.put("p1simple", dataLev1);
            dataLev0.put("p1array", new Map[]{dataLev1, dataLev1});

            Map<String, Object> dataRoot = new HashMap<String, Object>();
            dataRoot.put("p0simple", dataLev0);
            dataRoot.put("p0array", new Map[]{dataLev0, dataLev0});

            // send event
            env.sendEventMap(dataRoot, "JimTypeRoot");
            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
            //  System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    comment me in
            EventType eventType = eventBean.getEventType();
            SupportEventTypeAssertionUtil.assertConsistency(eventType);

            assertEquals(10, ((EventBean) eventBean.getFragment("p0simple.p1simple")).get("p2id"));
            assertEquals(10, ((EventBean) eventBean.getFragment("p0array[1].p1simple")).get("p2id"));
            assertEquals(10, ((EventBean) eventBean.getFragment("p0array[1].p1array[0]")).get("p2id"));
            assertEquals(10, ((EventBean) eventBean.getFragment("p0simple.p1array[0]")).get("p2id"));

            // resolve property via fragment
            EventBean assertEvent = (EventBean) eventBean.getFragment("p0simple");
            assertEquals(10, assertEvent.get("p1simple.p2id"));
            assertEquals(10, ((EventBean) assertEvent.getFragment("p1simple")).get("p2id"));

            assertEvent = ((EventBean[]) eventBean.getFragment("p0array"))[1];
            assertEquals(10, assertEvent.get("p1simple.p2id"));
            assertEquals(10, ((EventBean) assertEvent.getFragment("p1simple")).get("p2id"));

            assertEvent = (EventBean) eventBean.getFragment("p0array[0]");
            assertEquals(10, assertEvent.get("p1simple.p2id"));
            assertEquals(10, ((EventBean) assertEvent.getFragment("p1simple")).get("p2id"));

            assertEquals("JimTypeLev1", eventType.getFragmentType("p0array.p1simple").getFragmentType().getName());
            assertEquals(Integer.class, eventType.getFragmentType("p0array.p1simple").getFragmentType().getPropertyType("p2id"));
            assertEquals(Integer.class, eventType.getFragmentType("p0array[0].p1array[0]").getFragmentType().getPropertyDescriptor("p2id").getPropertyType());
            assertFalse(eventType.getFragmentType("p0simple.p1simple").isIndexed());
            assertTrue(eventType.getFragmentType("p0simple.p1array").isIndexed());

            tryInvalid((EventBean) eventBean.getFragment("p0simple"), "p1simple.p1id");

            env.undeployAll();
        }
    }

    private static class EPLBeanObjectArrayFragment3Level implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select * from JackTypeRoot").addListener("s0");

            assertEquals(Object[].class, env.statement("s0").getEventType().getUnderlyingType());

            Object[] dataLev1 = new Object[]{10};
            Object[] dataLev0 = new Object[]{dataLev1, new Object[]{dataLev1, dataLev1}};
            Object[] dataRoot = new Object[]{dataLev0, new Object[]{dataLev0, dataLev0}};

            // send event
            env.sendEventObjectArray(dataRoot, "JackTypeRoot");
            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
            //  System.out.println(SupportEventTypeAssertionUtil.print(eventBean));    comment me in
            EventType eventType = eventBean.getEventType();
            SupportEventTypeAssertionUtil.assertConsistency(eventType);

            assertEquals(10, ((EventBean) eventBean.getFragment("p0simple.p1simple")).get("p2id"));
            assertEquals(10, ((EventBean) eventBean.getFragment("p0array[1].p1simple")).get("p2id"));
            assertEquals(10, ((EventBean) eventBean.getFragment("p0array[1].p1array[0]")).get("p2id"));
            assertEquals(10, ((EventBean) eventBean.getFragment("p0simple.p1array[0]")).get("p2id"));

            // resolve property via fragment
            EventBean assertEvent = (EventBean) eventBean.getFragment("p0simple");
            assertEquals(10, assertEvent.get("p1simple.p2id"));
            assertEquals(10, ((EventBean) assertEvent.getFragment("p1simple")).get("p2id"));

            assertEvent = ((EventBean[]) eventBean.getFragment("p0array"))[1];
            assertEquals(10, assertEvent.get("p1simple.p2id"));
            assertEquals(10, ((EventBean) assertEvent.getFragment("p1simple")).get("p2id"));

            assertEvent = (EventBean) eventBean.getFragment("p0array[0]");
            assertEquals(10, assertEvent.get("p1simple.p2id"));
            assertEquals(10, ((EventBean) assertEvent.getFragment("p1simple")).get("p2id"));

            assertEquals("JackTypeLev1", eventType.getFragmentType("p0array.p1simple").getFragmentType().getName());
            assertEquals(Integer.class, eventType.getFragmentType("p0array.p1simple").getFragmentType().getPropertyType("p2id"));
            assertEquals(Integer.class, eventType.getFragmentType("p0array[0].p1array[0]").getFragmentType().getPropertyDescriptor("p2id").getPropertyType());
            assertFalse(eventType.getFragmentType("p0simple.p1simple").isIndexed());
            assertTrue(eventType.getFragmentType("p0simple.p1array").isIndexed());

            tryInvalid((EventBean) eventBean.getFragment("p0simple"), "p1simple.p1id");

            env.undeployAll();
        }
    }

    private static class EPLBeanFragmentMapMulti implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select * from MMOuterMap").addListener("s0");

            Map<String, Object> dataInnerInner = new HashMap<String, Object>();
            dataInnerInner.put("p2id", 10);

            Map<String, Object> dataInner = new HashMap<String, Object>();
            dataInner.put("p1bean", new SupportBean("string1", 2000));
            dataInner.put("p1beanComplex", SupportBeanComplexProps.makeDefaultBean());
            dataInner.put("p1beanArray", new SupportBean[]{new SupportBean("string2", 1), new SupportBean("string3", 2)});
            dataInner.put("p1innerId", 50);
            dataInner.put("p1innerMap", dataInnerInner);

            Map<String, Object> dataOuter = new HashMap<String, Object>();
            dataOuter.put("p0simple", dataInner);
            dataOuter.put("p0array", new Map[]{dataInner, dataInner});

            // send event
            env.sendEventMap(dataOuter, "MMOuterMap");
            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
            // System.out.println(SupportEventTypeAssertionUtil.print(eventBean));     comment me in
            EventType eventType = eventBean.getEventType();
            SupportEventTypeAssertionUtil.assertConsistency(eventType);

            // Fragment-to-simple
            assertTrue(eventType.getPropertyDescriptor("p0simple").isFragment());
            assertEquals(Integer.class, eventType.getFragmentType("p0simple").getFragmentType().getPropertyDescriptor("p1innerId").getPropertyType());
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

            env.undeployAll();
        }
    }

    private static void tryInvalid(EventBean theEvent, String property) {
        try {
            theEvent.get(property);
            fail();
        } catch (PropertyAccessException ex) {
            // expected
        }
    }
}
