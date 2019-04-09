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
package com.espertech.esper.regressionrun.suite.infra;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonVariantStream;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.suite.infra.namedwindow.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.bookexample.BookDesc;
import com.espertech.esper.regressionlib.support.bookexample.OrderBean;
import com.espertech.esper.regressionlib.support.bookexample.OrderWithItems;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.util.CollectionUtil.buildMap;

// see INFRA suite for additional Named Window tests
public class TestSuiteInfraNamedWindow extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testInfraNamedWindowConsumer() {
        RegressionRunner.run(session, InfraNamedWindowConsumer.executions());
    }

    public void testInfraNamedWindowOnDelete() {
        RegressionRunner.run(session, InfraNamedWindowOnDelete.executions());
    }

    public void testInfraNamedWindowViews() {
        RegressionRunner.run(session, InfraNamedWindowViews.executions());
    }

    public void testInfraNamedWindowJoin() {
        RegressionRunner.run(session, InfraNamedWindowJoin.executions());
    }

    public void testInfraNamedWindowTypes() {
        RegressionRunner.run(session, InfraNamedWindowTypes.executions());
    }

    public void testInfraNamedWindowOM() {
        RegressionRunner.run(session, InfraNamedWindowOM.executions());
    }

    public void testInfraNamedWindowOnSelect() {
        RegressionRunner.run(session, InfraNamedWindowOnSelect.executions());
    }

    public void testInfraNamedWindowSubquery() {
        RegressionRunner.run(session, InfraNamedWindowSubquery.executions());
    }

    public void testInfraNamedWindowOutputrate() {
        RegressionRunner.run(session, new InfraNamedWindowOutputrate());
    }

    public void testInfraNamedWindowRemoveStream() {
        RegressionRunner.run(session, new InfraNamedWindowRemoveStream());
    }

    public void testInfraNamedWindowProcessingOrder() {
        RegressionRunner.run(session, InfraNamedWindowProcessingOrder.executions());
    }

    public void testInfraNamedWindowOnUpdate() {
        RegressionRunner.run(session, InfraNamedWindowOnUpdate.executions());
    }

    public void testInfraNamedWindowOnMerge() {
        RegressionRunner.run(session, InfraNamedWindowOnMerge.executions());
    }

    public void testInfraNamedWindowInsertFrom() {
        RegressionRunner.run(session, InfraNamedWindowInsertFrom.executions());
    }

    public void testInfraNamedWindowContainedEvent() {
        RegressionRunner.run(session, new InfraNamedWindowContainedEvent());
    }

    public void testInfraNamedWindowIndex() {
        RegressionRunner.run(session, new InfraNamedWindowIndex());
    }

    public void testInfraNamedWindowLateStartIndex() {
        RegressionRunner.run(session, new InfraNamedWindowLateStartIndex());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, OrderBean.class, OrderWithItems.class,
            SupportBeanAtoFBase.class, SupportBean_A.class, SupportMarketDataBean.class,
            SupportSimpleBeanTwo.class, SupportSimpleBeanOne.class, SupportVariableSetEvent.class,
            SupportBean_S0.class, SupportBean_S1.class, SupportBeanRange.class, SupportBean_B.class,
            SupportOverrideOneA.class, SupportOverrideOne.class, SupportOverrideBase.class,
            SupportQueueEnter.class, SupportQueueLeave.class, SupportBeanAtoFBase.class,
            SupportBeanAbstractSub.class, SupportBean_ST0.class, SupportBeanTwo.class,
            SupportCountAccessEvent.class, BookDesc.class, SupportBean_Container.class,
            SupportEventWithManyArray.class, SupportEventWithIntArray.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        Map<String, Object> outerMapInnerType = new HashMap<>();
        outerMapInnerType.put("key", String.class);
        configuration.getCommon().addEventType("InnerMap", outerMapInnerType);
        Map<String, Object> outerMap = new HashMap<>();
        outerMap.put("innermap", "InnerMap");
        configuration.getCommon().addEventType("OuterMap", outerMap);

        Map<String, Object> typesSimpleKeyValue = new HashMap<>();
        typesSimpleKeyValue.put("key", String.class);
        typesSimpleKeyValue.put("value", long.class);
        configuration.getCommon().addEventType("MySimpleKeyValueMap", typesSimpleKeyValue);

        Map<String, Object> innerTypeOne = new HashMap<>();
        innerTypeOne.put("i1", int.class);
        Map<String, Object> innerTypeTwo = new HashMap<>();
        innerTypeTwo.put("i2", int.class);
        Map<String, Object> outerType = new HashMap<>();
        outerType.put("one", "T1");
        outerType.put("two", "T2");
        configuration.getCommon().addEventType("T1", innerTypeOne);
        configuration.getCommon().addEventType("T2", innerTypeTwo);
        configuration.getCommon().addEventType("OuterType", outerType);

        Map<String, Object> types = new HashMap<String, Object>();
        types.put("key", String.class);
        types.put("primitive", long.class);
        types.put("boxed", Long.class);
        configuration.getCommon().addEventType("MyMapWithKeyPrimitiveBoxed", types);

        Map<String, Object> dataType = buildMap(new Object[][]{{"a", String.class}, {"b", int.class}});
        configuration.getCommon().addEventType("MyMapAB", dataType);

        ConfigurationCommonEventTypeBean legacy = new ConfigurationCommonEventTypeBean();
        legacy.setCopyMethod("myCopyMethod");
        configuration.getCommon().addEventType("SupportBeanCopyMethod", SupportBeanCopyMethod.class.getName(), legacy);

        configuration.getCompiler().addPlugInSingleRowFunction("setBeanLongPrimitive999", InfraNamedWindowOnUpdate.class.getName(), "setBeanLongPrimitive999");
        configuration.getCompiler().addPlugInSingleRowFunction("increaseIntCopyDouble", InfraNamedWindowOnMerge.class.getName(), "increaseIntCopyDouble");

        ConfigurationCommonVariantStream config = new ConfigurationCommonVariantStream();
        config.addEventTypeName("SupportBean_A");
        config.addEventTypeName("SupportBean_B");
        configuration.getCommon().addVariantStream("VarStream", config);

        configuration.getCommon().getLogging().setEnableQueryPlan(true);
    }
}
