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
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.client.util.AccessorStyle;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.suite.event.bean.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestSuiteEventBean extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEventBeanInheritAndInterface() {
        RegressionRunner.run(session, EventBeanInheritAndInterface.executions());
    }

    public void testEventBeanEventPropertyDynamicPerformance() {
        RegressionRunner.run(session, new EventBeanEventPropertyDynamicPerformance());
    }

    public void testEventBeanPrivateClass() {
        RegressionRunner.run(session, new EventBeanPrivateClass());
    }

    public void testEventBeanJavaBeanAccessor() {
        RegressionRunner.run(session, new EventBeanJavaBeanAccessor());
    }

    public void testEventBeanFinalClass() {
        RegressionRunner.run(session, new EventBeanFinalClass());
    }

    public void testEventBeanMappedIndexedPropertyExpression() {
        RegressionRunner.run(session, new EventBeanMappedIndexedPropertyExpression());
    }

    public void testEventBeanPropertyResolutionWDefaults() {
        RegressionRunner.run(session, EventBeanPropertyResolutionWDefaults.executions());
    }

    public void testEventBeanPropertyIterableMapList() {
        RegressionRunner.run(session, new EventBeanPropertyIterableMapList());
    }

    public void testEventBeanPropertyResolutionFragment() {
        RegressionRunner.run(session, EventBeanPropertyResolutionFragment.executions());
    }

    public void testEventBeanPropertyAccessPerformance() {
        RegressionRunner.run(session, new EventBeanPropertyAccessPerformance());
    }

    private static void configure(Configuration configuration) {
        ConfigurationCommonEventTypeBean myLegacyNestedEvent = new ConfigurationCommonEventTypeBean();
        myLegacyNestedEvent.setAccessorStyle(AccessorStyle.EXPLICIT);
        myLegacyNestedEvent.addFieldProperty("fieldNestedClassValue", "fieldNestedValue");
        myLegacyNestedEvent.addMethodProperty("readNestedClassValue", "readNestedValue");
        configuration.getCommon().addEventType("MyLegacyNestedEvent", SupportLegacyBean.LegacyNested.class.getName(), myLegacyNestedEvent);

        for (Class clazz : new Class[]{SupportBean.class, SupportOverrideOne.class, SupportOverrideOneA.class, SupportOverrideBase.class, SupportOverrideOneB.class, ISupportBaseAB.class,
            ISupportA.class, ISupportB.class, ISupportC.class, ISupportAImplSuperG.class, ISupportAImplSuperGImplPlus.class,
            SupportBeanComplexProps.class, SupportBeanWriteOnly.class, SupportBeanDupProperty.class,
            SupportBeanCombinedProps.class}) {
            configuration.getCommon().addEventType(clazz.getSimpleName(), clazz);
        }

        configuration.getCommon().addEventType("SomeKeywords", SupportBeanReservedKeyword.class);
        configuration.getCommon().addEventType("Order", SupportBeanReservedKeyword.class);

        ConfigurationCommonEventTypeBean myFinalEvent = new ConfigurationCommonEventTypeBean();
        myFinalEvent.setAccessorStyle(AccessorStyle.JAVABEAN);
        configuration.getCommon().addEventType("MyFinalEvent", SupportBeanFinal.class.getName(), myFinalEvent);

        ConfigurationCommonEventTypeBean myLegacyTwo = new ConfigurationCommonEventTypeBean();
        myLegacyTwo.setAccessorStyle(AccessorStyle.JAVABEAN);
        myLegacyTwo.addFieldProperty("explicitFInt", "fieldIntPrimitive");
        myLegacyTwo.addMethodProperty("explicitMGetInt", "getIntPrimitive");
        myLegacyTwo.addMethodProperty("explicitMReadInt", "readIntPrimitive");
        configuration.getCommon().addEventType("MyLegacyTwo", SupportLegacyBeanInt.class.getName(), myLegacyTwo);

        Map<String, Object> def = new HashMap<String, Object>();
        def.put("mapped", new HashMap());
        def.put("indexed", int[].class);
        configuration.getCommon().addEventType("MapEvent", def);

        Map<String, Object> defType = new HashMap<String, Object>();
        defType.put("name", String.class);
        defType.put("value", String.class);
        defType.put("properties", Map.class);
        configuration.getCommon().addEventType("InputEvent", defType);

        configuration.getCommon().addEventType("ObjectArrayEvent",
            new String[]{"mapped", "indexed"}, new Object[]{new HashMap(), int[].class});

        ConfigurationCommonEventTypeBean myEventWithField = new ConfigurationCommonEventTypeBean();
        myEventWithField.setAccessorStyle(AccessorStyle.PUBLIC);
        configuration.getCommon().addEventType(EventBeanPropertyIterableMapList.MyEventWithField.class.getSimpleName(), EventBeanPropertyIterableMapList.MyEventWithField.class.getName(), myEventWithField);

        ConfigurationCommonEventTypeBean configNoCglib = new ConfigurationCommonEventTypeBean();
        configuration.getCommon().addEventType(EventBeanPropertyIterableMapList.MyEventWithMethod.class.getSimpleName(), EventBeanPropertyIterableMapList.MyEventWithMethod.class.getName(), configNoCglib);

        Map<String, Object> mapOuter = new HashMap<String, Object>();
        mapOuter.put("p0int", int.class);
        mapOuter.put("p0intarray", int[].class);
        mapOuter.put("p0map", Map.class);
        configuration.getCommon().addEventType("MSTypeOne", mapOuter);

        String[] props = {"p0int", "p0intarray", "p0map"};
        Object[] types = {int.class, int[].class, Map.class};
        configuration.getCommon().addEventType("OASimple", props, types);

        Map<String, Object> frostyLev0 = new HashMap<String, Object>();
        frostyLev0.put("p1id", int.class);
        configuration.getCommon().addEventType("FrostyLev0", frostyLev0);

        Map<String, Object> frosty = new HashMap<String, Object>();
        frosty.put("p0simple", "FrostyLev0");
        frosty.put("p0bean", SupportBeanComplexProps.class);
        configuration.getCommon().addEventType("Frosty", frosty);

        configuration.getCommon().addEventType("WheatLev0", new String[]{"p1id"}, new Object[]{int.class});
        configuration.getCommon().addEventType("WheatRoot", new String[]{"p0simple", "p0bean"}, new Object[]{"WheatLev0", SupportBeanComplexProps.class});

        Map<String, Object> homerunLev0 = new HashMap<String, Object>();
        homerunLev0.put("p1id", int.class);
        configuration.getCommon().addEventType("HomerunLev0", homerunLev0);

        Map<String, Object> homerunRoot = new HashMap<String, Object>();
        homerunRoot.put("p0simple", "HomerunLev0");
        homerunRoot.put("p0array", "HomerunLev0[]");
        configuration.getCommon().addEventType("HomerunRoot", homerunRoot);

        configuration.getCommon().addEventType("GoalLev0", new String[]{"p1id"}, new Object[]{int.class});
        configuration.getCommon().addEventType("GoalRoot", new String[]{"p0simple", "p0array"}, new Object[]{"GoalLev0", "GoalLev0[]"});

        Map<String, Object> flywheelTypeLev0 = new HashMap<String, Object>();
        flywheelTypeLev0.put("p1id", int.class);
        Map<String, Object> flywheelRoot = new HashMap<String, Object>();
        flywheelRoot.put("p0simple", flywheelTypeLev0);
        configuration.getCommon().addEventType("FlywheelRoot", flywheelRoot);

        Map<String, Object> gistInner = new HashMap<String, Object>();
        gistInner.put("p2id", int.class);
        configuration.getCommon().addEventType("GistInner", gistInner);

        Map<String, Object> typeMap = new HashMap<String, Object>();
        typeMap.put("id", int.class);
        typeMap.put("bean", SupportBean.class);
        typeMap.put("beanarray", SupportBean[].class);
        typeMap.put("complex", SupportBeanComplexProps.class);
        typeMap.put("complexarray", SupportBeanComplexProps[].class);
        typeMap.put("map", "GistInner");
        typeMap.put("maparray", "GistInner[]");
        configuration.getCommon().addEventType("GistMapOne", typeMap);
        configuration.getCommon().addEventType("GistMapTwo", typeMap);

        configuration.getCommon().addEventType("CashInner", new String[]{"p2id"}, new Object[]{int.class});

        String[] propsCash = {"id", "bean", "beanarray", "complex", "complexarray", "map", "maparray"};
        Object[] typesCash = {int.class, SupportBean.class, SupportBean[].class, SupportBeanComplexProps.class, SupportBeanComplexProps[].class, "CashInner", "CashInner[]"};
        configuration.getCommon().addEventType("CashMapOne", propsCash, typesCash);
        configuration.getCommon().addEventType("CashMapTwo", propsCash, typesCash);

        Map<String, Object> txTypeLev0 = new HashMap<String, Object>();
        txTypeLev0.put("p1simple", SupportBean.class);
        txTypeLev0.put("p1array", SupportBean[].class);
        txTypeLev0.put("p1complex", SupportBeanComplexProps.class);
        txTypeLev0.put("p1complexarray", SupportBeanComplexProps[].class);
        configuration.getCommon().addEventType("TXTypeLev0", txTypeLev0);

        Map<String, Object> txTypeRoot = new HashMap<String, Object>();
        txTypeRoot.put("p0simple", "TXTypeLev0");
        txTypeRoot.put("p0array", "TXTypeLev0[]");
        configuration.getCommon().addEventType("TXTypeRoot", txTypeRoot);

        String[] localTypeLev0 = {"p1simple", "p1array", "p1complex", "p1complexarray"};
        Object[] typesLev0 = {SupportBean.class, SupportBean[].class, SupportBeanComplexProps.class, SupportBeanComplexProps[].class};
        configuration.getCommon().addEventType("LocalTypeLev0", localTypeLev0, typesLev0);

        String[] localTypeRoot = {"p0simple", "p0array"};
        Object[] typesOuter = {"LocalTypeLev0", "LocalTypeLev0[]"};
        configuration.getCommon().addEventType("LocalTypeRoot", localTypeRoot, typesOuter);

        Map<String, Object> jimTypeLev1 = new HashMap<String, Object>();
        jimTypeLev1.put("p2id", int.class);
        configuration.getCommon().addEventType("JimTypeLev1", jimTypeLev1);

        Map<String, Object> jimTypeLev0 = new HashMap<String, Object>();
        jimTypeLev0.put("p1simple", "JimTypeLev1");
        jimTypeLev0.put("p1array", "JimTypeLev1[]");
        configuration.getCommon().addEventType("JimTypeLev0", jimTypeLev0);

        Map<String, Object> jimTypeRoot = new HashMap<String, Object>();
        jimTypeRoot.put("p0simple", "JimTypeLev0");
        jimTypeRoot.put("p0array", "JimTypeLev0[]");
        configuration.getCommon().addEventType("JimTypeRoot", jimTypeRoot);

        configuration.getCommon().addEventType("JackTypeLev1", new String[]{"p2id"}, new Object[]{int.class});
        configuration.getCommon().addEventType("JackTypeLev0", new String[]{"p1simple", "p1array"}, new Object[]{"JackTypeLev1", "JackTypeLev1[]"});
        configuration.getCommon().addEventType("JackTypeRoot", new String[]{"p0simple", "p0array"}, new Object[]{"JackTypeLev0", "JackTypeLev0[]"});

        Map<String, Object> mmInner = new HashMap<String, Object>();
        mmInner.put("p2id", int.class);

        Map<String, Object> mmInnerMap = new HashMap<String, Object>();
        mmInnerMap.put("p1bean", SupportBean.class);
        mmInnerMap.put("p1beanComplex", SupportBeanComplexProps.class);
        mmInnerMap.put("p1beanArray", SupportBean[].class);
        mmInnerMap.put("p1innerId", int.class);
        mmInnerMap.put("p1innerMap", mmInner);
        configuration.getCommon().addEventType("MMInnerMap", mmInnerMap);

        Map<String, Object> mmOuterMap = new HashMap<String, Object>();
        mmOuterMap.put("p0simple", "MMInnerMap");
        mmOuterMap.put("p0array", "MMInnerMap[]");
        configuration.getCommon().addEventType("MMOuterMap", mmOuterMap);

        Map<String, Object> myTypeDef = new HashMap<String, Object>();
        myTypeDef.put("candidate book", String.class);
        myTypeDef.put("XML Message Type", String.class);
        myTypeDef.put("select", int.class);
        myTypeDef.put("children's books", int[].class);
        myTypeDef.put("my <> map", Map.class);
        configuration.getCommon().addEventType("MyType", myTypeDef);

        configuration.getCommon().addEventType(EventBeanPropertyResolutionWDefaults.LocalEventWithEnum.class);
        configuration.getCommon().addEventType(EventBeanPropertyResolutionWDefaults.LocalEventWithGroup.class);

        ConfigurationCommonEventTypeBean anotherLegacyNestedEvent = new ConfigurationCommonEventTypeBean();
        anotherLegacyNestedEvent.setAccessorStyle(AccessorStyle.PUBLIC);
        configuration.getCommon().addEventType("AnotherLegacyNestedEvent", SupportLegacyBean.LegacyNested.class.getName(), anotherLegacyNestedEvent);

        configuration.getCommon().addImport(EventBeanPropertyResolutionWDefaults.LocalEventEnum.class);
        configuration.getCommon().addImport(EventBeanPropertyResolutionWDefaults.GROUP.class);
    }
}
