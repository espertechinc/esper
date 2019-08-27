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
package com.espertech.esper.regressionrun.suite.client;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompiler;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInAggregationMultiFunction;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInSingleRowFunction;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.suite.client.extension.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.client.SupportSingleRowFunction;
import com.espertech.esper.regressionlib.support.client.SupportSingleRowFunctionTwo;
import com.espertech.esper.regressionlib.support.extend.aggfunc.*;
import com.espertech.esper.regressionlib.support.extend.aggmultifunc.SupportAggMFEventsAsListForge;
import com.espertech.esper.regressionlib.support.extend.aggmultifunc.SupportAggMFMultiRTForge;
import com.espertech.esper.regressionlib.support.extend.pattern.MyCountToPatternGuardForge;
import com.espertech.esper.regressionlib.support.extend.vdw.SupportVirtualDW;
import com.espertech.esper.regressionlib.support.extend.vdw.SupportVirtualDWExceptionForge;
import com.espertech.esper.regressionlib.support.extend.vdw.SupportVirtualDWForge;
import com.espertech.esper.regressionlib.support.extend.vdw.SupportVirtualDWInvalidForge;
import com.espertech.esper.regressionlib.support.extend.view.MyFlushedSimpleViewForge;
import com.espertech.esper.regressionlib.support.extend.view.MyTrendSpotterViewForge;
import com.espertech.esper.regressionlib.support.util.SupportPluginLoader;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestSuiteClientExtension extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testClientExtendUDFReturnTypeIsEvents() {
        RegressionRunner.run(session, new ClientExtendUDFReturnTypeIsEvents());
    }

    public void testClientExtendUDFVarargs() {
        RegressionRunner.run(session, new ClientExtendUDFVarargs());
    }

    public void testClientExtendAggregationFunction() {
        RegressionRunner.run(session, ClientExtendAggregationFunction.executions());
    }

    public void testClientExtendAggregationMultiFunction() {
        RegressionRunner.run(session, ClientExtendAggregationMultiFunction.executions());
    }

    public void testClientExtendView() {
        RegressionRunner.run(session, new ClientExtendView());
    }

    public void testClientExtendVirtualDataWindow() {
        RegressionRunner.run(session, new ClientExtendVirtualDataWindow());
    }

    public void testClientExtendPatternGuard() {
        RegressionRunner.run(session, new ClientExtendPatternGuard());
    }

    public void testClientExtendSingleRowFunction() {
        RegressionRunner.run(session, ClientExtendSingleRowFunction.executions());
    }

    public void testClientExtendAdapterLoaderLoad() {
        RegressionSession session = RegressionRunner.session();

        Properties props = new Properties();
        props.put("name", "val");
        session.getConfiguration().getRuntime().addPluginLoader("MyLoader", SupportPluginLoader.class.getName(), props);

        props = new Properties();
        props.put("name2", "val2");
        session.getConfiguration().getRuntime().addPluginLoader("MyLoader2", SupportPluginLoader.class.getName(), props);

        RegressionRunner.run(session, new ClientExtendAdapterLoader());

        session.destroy();
    }

    public void testClientExtendDateTimeMethod() {
        RegressionRunner.run(session, ClientExtendDateTimeMethod.executions());
    }

    public void testClientExtendEnumMethod() {
        RegressionRunner.run(session, ClientExtendEnumMethod.executions());
    }

    private static void configure(Configuration configuration) {

        for (Class clazz : new Class[]{SupportBean.class, SupportBean_A.class, SupportBean_S0.class, SupportMarketDataBean.class,
            SupportSimpleBeanOne.class, SupportBean_ST0.class, SupportBeanRange.class, SupportDateTime.class, SupportCollection.class,
            SupportBean_ST0_Container.class}) {
            configuration.getCommon().addEventType(clazz.getSimpleName(), clazz);
        }

        Map<String, Object> mapType = new HashMap<>();
        mapType.put("col1", "string");
        mapType.put("col2", "string");
        mapType.put("col3", "int");
        configuration.getCommon().addEventType("MapType", mapType);

        ConfigurationCompiler configurationCompiler = configuration.getCompiler();
        configurationCompiler.addPlugInSingleRowFunction("singlerow", SupportSingleRowFunctionTwo.class.getName(), "testSingleRow");
        configurationCompiler.addPlugInSingleRowFunction("power3", SupportSingleRowFunction.class.getName(), "computePower3");
        configurationCompiler.addPlugInSingleRowFunction("chainTop", SupportSingleRowFunction.class.getName(), "getChainTop");
        configurationCompiler.addPlugInSingleRowFunction("throwExceptionLogMe", SupportSingleRowFunction.class.getName(), "throwexception", ConfigurationCompilerPlugInSingleRowFunction.ValueCache.DISABLED, ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED, false);
        configurationCompiler.addPlugInSingleRowFunction("throwExceptionRethrow", SupportSingleRowFunction.class.getName(), "throwexception", ConfigurationCompilerPlugInSingleRowFunction.ValueCache.DISABLED, ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED, true);
        configurationCompiler.addPlugInSingleRowFunction("power3Rethrow", SupportSingleRowFunction.class.getName(), "computePower3", ConfigurationCompilerPlugInSingleRowFunction.ValueCache.DISABLED, ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED, true);
        configurationCompiler.addPlugInSingleRowFunction("power3Context", SupportSingleRowFunction.class.getName(), "computePower3WithContext", ConfigurationCompilerPlugInSingleRowFunction.ValueCache.DISABLED, ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED, true);
        for (String method : ("surroundx,isNullValue,getValueAsString,eventsCheckStrings,varargsOnlyInt,varargsOnlyString,varargsOnlyObject,varargsOnlyNumber,varargsOnlyISupportBaseAB,varargsW1Param,varargsW2Param," +
            "varargsOnlyWCtx,varargsW1ParamWCtx,varargsW2ParamWCtx,varargsObjectsWCtx,varargsW1ParamObjectsWCtx,varargsOnlyBoxedFloat,varargsOnlyBoxedShort,varargsOnlyBoxedByte," +
            "varargOverload").split(",")) {
            configurationCompiler.addPlugInSingleRowFunction(method, SupportSingleRowFunction.class.getName(), method);
        }
        configurationCompiler.addPlugInSingleRowFunction("extractNum", ClientExtendEnumMethod.class.getName(), "extractNum");

        addEventTypeUDF("myItemProducerEventBeanArray", "MyItem", "myItemProducerEventBeanArray", configuration);
        addEventTypeUDF("myItemProducerEventBeanCollection", "MyItem", "myItemProducerEventBeanCollection", configuration);
        addEventTypeUDF("myItemProducerInvalidNoType", null, "myItemProducerEventBeanArray", configuration);
        addEventTypeUDF("myItemProducerInvalidWrongType", "dummy", "myItemProducerEventBeanArray", configuration);

        configurationCompiler.addPlugInAggregationFunctionForge("concatstring", SupportConcatWManagedAggregationFunctionForge.class.getName());
        configurationCompiler.addPlugInAggregationFunctionForge("myagg", SupportSupportBeanAggregationFunctionForge.class.getName());
        configurationCompiler.addPlugInAggregationFunctionForge("countback", SupportCountBackAggregationFunctionForge.class.getName());
        configurationCompiler.addPlugInAggregationFunctionForge("countboundary", SupportLowerUpperCompareAggregationFunctionForge.class.getName());
        configurationCompiler.addPlugInAggregationFunctionForge("concatWCodegen", SupportConcatWCodegenAggregationFunctionForge.class.getName());
        configurationCompiler.addPlugInAggregationFunctionForge("invalidAggFuncForge", String.class.getName());
        configurationCompiler.addPlugInAggregationFunctionForge("nonExistAggFuncForge", "com.NoSuchClass");

        ConfigurationCompilerPlugInAggregationMultiFunction configGeneral = new ConfigurationCompilerPlugInAggregationMultiFunction("ss,sa,sc,se1,se2,ee".split(","), SupportAggMFMultiRTForge.class.getName());
        configurationCompiler.addPlugInAggregationMultiFunction(configGeneral);
        ConfigurationCompilerPlugInAggregationMultiFunction codegenTestAccum = new ConfigurationCompilerPlugInAggregationMultiFunction("collectEvents".split(","), SupportAggMFEventsAsListForge.class.getName());
        configurationCompiler.addPlugInAggregationMultiFunction(codegenTestAccum);

        configuration.getCompiler().addPlugInView("mynamespace", "flushedsimple", MyFlushedSimpleViewForge.class.getName());
        configuration.getCompiler().addPlugInView("mynamespace", "invalid", String.class.getName());
        configuration.getCompiler().addPlugInView("mynamespace", "trendspotter", MyTrendSpotterViewForge.class.getName());

        configurationCompiler.addPlugInVirtualDataWindow("test", "vdwnoparam", SupportVirtualDWForge.class.getName());
        configurationCompiler.addPlugInVirtualDataWindow("test", "vdwwithparam", SupportVirtualDWForge.class.getName(), SupportVirtualDW.ITERATE);    // configure with iteration
        configurationCompiler.addPlugInVirtualDataWindow("test", "vdw", SupportVirtualDWForge.class.getName());
        configurationCompiler.addPlugInVirtualDataWindow("invalid", "invalid", SupportBean.class.getName());
        configurationCompiler.addPlugInVirtualDataWindow("test", "testnoindex", SupportVirtualDWInvalidForge.class.getName());
        configurationCompiler.addPlugInVirtualDataWindow("test", "exceptionvdw", SupportVirtualDWExceptionForge.class.getName());

        configurationCompiler.addPlugInPatternGuard("myplugin", "count_to", MyCountToPatternGuardForge.class.getName());
        configurationCompiler.addPlugInPatternGuard("namespace", "name", String.class.getName());

        configurationCompiler.addPlugInDateTimeMethod("roll", ClientExtendDateTimeMethod.MyLocalDTMForgeFactoryRoll.class.getName());
        configurationCompiler.addPlugInDateTimeMethod("asArrayOfString", ClientExtendDateTimeMethod.MyLocalDTMForgeFactoryArrayOfString.class.getName());
        configurationCompiler.addPlugInDateTimeMethod("dtmInvalidMethodNotExists", ClientExtendDateTimeMethod.MyLocalDTMForgeFactoryInvalidMethodNotExists.class.getName());
        configurationCompiler.addPlugInDateTimeMethod("dtmInvalidNotProvided", ClientExtendDateTimeMethod.MyLocalDTMForgeFactoryInvalidNotProvided.class.getName());
        configurationCompiler.addPlugInDateTimeMethod("someDTMInvalidReformat", ClientExtendDateTimeMethod.MyLocalDTMForgeFactoryInvalidReformat.class.getName());
        configurationCompiler.addPlugInDateTimeMethod("someDTMInvalidNoOp", ClientExtendDateTimeMethod.MyLocalDTMForgeFactoryInvalidNoOp.class.getName());

        configurationCompiler.addPlugInEnumMethod("enumPlugInMedian", ClientExtendEnumMethod.MyLocalEnumMethodForgeMedian.class.getName());
        configurationCompiler.addPlugInEnumMethod("enumPlugInOne", ClientExtendEnumMethod.MyLocalEnumMethodForgeOne.class.getName());
        configurationCompiler.addPlugInEnumMethod("enumPlugInEarlyExit", ClientExtendEnumMethod.MyLocalEnumMethodForgeEarlyExit.class.getName());
        configurationCompiler.addPlugInEnumMethod("enumPlugInReturnEvents", ClientExtendEnumMethod.MyLocalEnumMethodForgePredicateReturnEvents.class.getName());
        configurationCompiler.addPlugInEnumMethod("enumPlugInReturnSingleEvent", ClientExtendEnumMethod.MyLocalEnumMethodForgePredicateReturnSingleEvent.class.getName());
        configurationCompiler.addPlugInEnumMethod("enumPlugInTwoLambda", ClientExtendEnumMethod.MyLocalEnumMethodForgeTwoLambda.class.getName());
        configurationCompiler.addPlugInEnumMethod("enumPlugInLambdaEventWPredicateAndIndex", ClientExtendEnumMethod.MyLocalEnumMethodForgeThree.class.getName());
        configurationCompiler.addPlugInEnumMethod("enumPlugInLambdaScalarWPredicateAndIndex", ClientExtendEnumMethod.MyLocalEnumMethodForgeThree.class.getName());
        configurationCompiler.addPlugInEnumMethod("enumPlugInLambdaScalarWStateAndValue", ClientExtendEnumMethod.MyLocalEnumMethodForgeStateWValue.class.getName());

        configuration.getCommon().addImport(ClientExtendSingleRowFunction.class);

        configuration.getRuntime().getThreading().setRuntimeFairlock(true);
        configuration.getCommon().getLogging().setEnableQueryPlan(true);
    }

    private static void addEventTypeUDF(String name, String eventTypeName, String functionMethodName, Configuration configuration) {
        ConfigurationCompilerPlugInSingleRowFunction entry = new ConfigurationCompilerPlugInSingleRowFunction();
        entry.setName(name);
        entry.setFunctionClassName(ClientExtendUDFReturnTypeIsEvents.class.getName());
        entry.setFunctionMethodName(functionMethodName);
        entry.setEventTypeName(eventTypeName);
        configuration.getCompiler().addPlugInSingleRowFunction(entry);
    }
}
