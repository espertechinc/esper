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
package com.espertech.esper.common.client.configuration;

import com.espertech.esper.common.client.configuration.common.*;
import com.espertech.esper.common.client.configuration.compiler.*;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntime;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeMetricsReporting;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimePluginLoader;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.StreamSelector;
import com.espertech.esper.common.client.util.*;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.type.StringPatternSet;
import com.espertech.esper.common.internal.type.StringPatternSetLike;
import com.espertech.esper.common.internal.type.StringPatternSetRegex;
import com.espertech.esper.common.internal.util.ConfigurationParser;
import junit.framework.TestCase;

import javax.xml.xpath.XPathConstants;
import java.math.RoundingMode;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TestConfigurationParser extends TestCase {
    public void testRegressionFileConfig() throws Exception {
        Configuration config = new Configuration();
        URL url = this.getClass().getClassLoader().getResource(TestConfiguration.ESPER_TEST_CONFIG);
        ConfigurationParser.doConfigure(config, url.openStream(), url.toString());
        assertFileConfig(config);
    }

    public void testConfigurationDefaults() {
        Configuration config = new Configuration();

        ConfigurationCommon common = config.getCommon();
        assertEquals(PropertyResolutionStyle.CASE_SENSITIVE, common.getEventMeta().getClassPropertyResolutionStyle());
        assertEquals(AccessorStyle.JAVABEAN, common.getEventMeta().getDefaultAccessorStyle());
        assertEquals(EventUnderlyingType.MAP, common.getEventMeta().getDefaultEventRepresentation());
        assertTrue(common.getEventMeta().getAvroSettings().isEnableAvro());
        assertTrue(common.getEventMeta().getAvroSettings().isEnableNativeString());
        assertTrue(common.getEventMeta().getAvroSettings().isEnableSchemaDefaultNonNull());
        assertNull(common.getEventMeta().getAvroSettings().getObjectValueTypeWidenerFactoryClass());
        assertNull(common.getEventMeta().getAvroSettings().getTypeRepresentationMapperClass());
        assertFalse(common.getLogging().isEnableQueryPlan());
        assertFalse(common.getLogging().isEnableJDBC());
        assertEquals(TimeUnit.MILLISECONDS, common.getTimeSource().getTimeUnit());
        assertEquals(ThreadingProfile.NORMAL, common.getExecution().getThreadingProfile());

        ConfigurationCompiler compiler = config.getCompiler();
        assertFalse(compiler.getViewResources().isIterableUnbound());
        assertTrue(compiler.getViewResources().isOutputLimitOpt());
        assertFalse(compiler.getLogging().isEnableCode());
        assertEquals(16, compiler.getExecution().getFilterServiceMaxFilterWidth());
        assertTrue(compiler.getExecution().isEnabledDeclaredExprValueCache());
        ConfigurationCompilerByteCode byteCode = compiler.getByteCode();
        assertFalse(byteCode.isIncludeComments());
        assertFalse(byteCode.isIncludeDebugSymbols());
        assertTrue(byteCode.isAttachEPL());
        assertFalse(byteCode.isAttachModuleEPL());
        assertFalse(byteCode.isAttachPatternEPL());
        assertFalse(byteCode.isInstrumented());
        assertFalse(byteCode.isAllowSubscriber());
        assertEquals(NameAccessModifier.PRIVATE, byteCode.getAccessModifierContext());
        assertEquals(NameAccessModifier.PRIVATE, byteCode.getAccessModifierEventType());
        assertEquals(NameAccessModifier.PRIVATE, byteCode.getAccessModifierExpression());
        assertEquals(NameAccessModifier.PRIVATE, byteCode.getAccessModifierNamedWindow());
        assertEquals(NameAccessModifier.PRIVATE, byteCode.getAccessModifierScript());
        assertEquals(NameAccessModifier.PRIVATE, byteCode.getAccessModifierTable());
        assertEquals(NameAccessModifier.PRIVATE, byteCode.getAccessModifierVariable());
        assertEquals(EventTypeBusModifier.NONBUS, byteCode.getBusModifierEventType());
        assertEquals(8, byteCode.getThreadPoolCompilerNumThreads());
        assertNull(byteCode.getThreadPoolCompilerCapacity());
        assertEquals(16*1024, byteCode.getMaxMethodsPerClass());
        assertEquals(StreamSelector.ISTREAM_ONLY, compiler.getStreamSelection().getDefaultStreamSelector());
        assertFalse(compiler.getLanguage().isSortUsingCollator());
        assertFalse(compiler.getExpression().isIntegerDivision());
        assertFalse(compiler.getExpression().isDivisionByZeroReturnsNull());
        assertTrue(compiler.getExpression().isUdfCache());
        assertTrue(compiler.getExpression().isExtendedAggregation());
        assertFalse(compiler.getExpression().isDuckTyping());
        assertNull(compiler.getExpression().getMathContext());
        assertEquals("js", compiler.getScripts().getDefaultDialect());
        assertTrue(compiler.getScripts().isEnabled());
        assertTrue(compiler.getSerde().isEnableExtendedBuiltin());
        assertFalse(compiler.getSerde().isEnableExternalizable());
        assertFalse(compiler.getSerde().isEnableSerializable());
        assertFalse(compiler.getSerde().isEnableSerializationFallback());
        assertTrue(compiler.getSerde().getSerdeProviderFactories().isEmpty());

        ConfigurationRuntime runtime = config.getRuntime();
        assertTrue(runtime.getThreading().isInsertIntoDispatchPreserveOrder());
        assertEquals(100, runtime.getThreading().getInsertIntoDispatchTimeout());
        assertTrue(runtime.getThreading().isListenerDispatchPreserveOrder());
        assertEquals(1000, runtime.getThreading().getListenerDispatchTimeout());
        assertTrue(runtime.getThreading().isInternalTimerEnabled());
        assertEquals(100, runtime.getThreading().getInternalTimerMsecResolution());
        assertEquals(Locking.SPIN, runtime.getThreading().getInsertIntoDispatchLocking());
        assertEquals(Locking.SPIN, runtime.getThreading().getListenerDispatchLocking());
        assertFalse(runtime.getThreading().isThreadPoolInbound());
        assertFalse(runtime.getThreading().isThreadPoolOutbound());
        assertFalse(runtime.getThreading().isThreadPoolRouteExec());
        assertFalse(runtime.getThreading().isThreadPoolTimerExec());
        assertEquals(2, runtime.getThreading().getThreadPoolInboundNumThreads());
        assertEquals(2, runtime.getThreading().getThreadPoolOutboundNumThreads());
        assertEquals(2, runtime.getThreading().getThreadPoolRouteExecNumThreads());
        assertEquals(2, runtime.getThreading().getThreadPoolTimerExecNumThreads());
        assertNull(runtime.getThreading().getThreadPoolInboundCapacity());
        assertNull(runtime.getThreading().getThreadPoolOutboundCapacity());
        assertNull(runtime.getThreading().getThreadPoolRouteExecCapacity());
        assertNull(runtime.getThreading().getThreadPoolTimerExecCapacity());
        assertFalse(runtime.getThreading().isRuntimeFairlock());
        assertFalse(runtime.getMetricsReporting().isJmxRuntimeMetrics());
        assertTrue(runtime.getThreading().isNamedWindowConsumerDispatchPreserveOrder());
        assertEquals(Long.MAX_VALUE, runtime.getThreading().getNamedWindowConsumerDispatchTimeout());
        assertEquals(Locking.SPIN, runtime.getThreading().getNamedWindowConsumerDispatchLocking());
        assertFalse(runtime.getLogging().isEnableExecutionDebug());
        assertTrue(runtime.getLogging().isEnableTimerDebug());
        assertNull(runtime.getLogging().getAuditPattern());
        assertEquals(15000, runtime.getVariables().getMsecVersionRelease());
        assertNull(runtime.getPatterns().getMaxSubexpressions());
        assertTrue(runtime.getPatterns().isMaxSubexpressionPreventStart());
        assertNull(runtime.getMatchRecognize().getMaxStates());
        assertTrue(runtime.getMatchRecognize().isMaxStatesPreventStart());
        assertEquals(TimeSourceType.MILLI, runtime.getTimeSource().getTimeSourceType());
        assertFalse(runtime.getExecution().isPrioritized());
        assertFalse(runtime.getExecution().isDisableLocking());
        assertEquals(FilterServiceProfile.READMOSTLY, runtime.getExecution().getFilterServiceProfile());
        assertEquals(1, runtime.getExecution().getDeclaredExprValueCacheSize());
        assertTrue(runtime.getExpression().isSelfSubselectPreeval());
        assertEquals(TimeZone.getDefault(), runtime.getExpression().getTimeZone());
        assertNull(runtime.getExceptionHandling().getHandlerFactories());
        assertEquals(UndeployRethrowPolicy.WARN, runtime.getExceptionHandling().getUndeployRethrowPolicy());
        assertNull(runtime.getConditionHandling().getHandlerFactories());

        ConfigurationCommonEventTypeXMLDOM domType = new ConfigurationCommonEventTypeXMLDOM();
        assertFalse(domType.isXPathPropertyExpr());
        assertTrue(domType.isXPathResolvePropertiesAbsolute());
        assertTrue(domType.isEventSenderValidatesRoot());
        assertTrue(domType.isAutoFragment());
    }

    static void assertFileConfig(Configuration config) {
        ConfigurationCommon common = config.getCommon();
        ConfigurationCompiler compiler = config.getCompiler();
        ConfigurationRuntime runtime = config.getRuntime();

        /*
         * COMMON
         *
         */

        // assert name for class
        assertEquals(3, common.getEventTypeNames().size());
        assertEquals("com.mycompany.myapp.MySampleEventOne", common.getEventTypeNames().get("MySampleEventOne"));
        assertEquals("com.mycompany.myapp.MySampleEventTwo", common.getEventTypeNames().get("MySampleEventTwo"));
        assertEquals("com.mycompany.package.MyLegacyTypeEvent", common.getEventTypeNames().get("MyLegacyTypeEvent"));

        // assert auto imports
        assertEquals(8, common.getImports().size());
        assertEquals("java.lang.*", common.getImports().get(0));
        assertEquals("java.math.*", common.getImports().get(1));
        assertEquals("java.text.*", common.getImports().get(2));
        assertEquals("java.util.*", common.getImports().get(3));
        assertEquals("com.espertech.esper.common.client.annotation.*", common.getImports().get(4));
        assertEquals("com.espertech.esper.common.internal.epl.dataflow.ops.*", common.getImports().get(5));
        assertEquals("com.mycompany.myapp.*", common.getImports().get(6));
        assertEquals("com.mycompany.myapp.ClassOne", common.getImports().get(7));
        assertEquals("com.mycompany.myapp.annotations.*", common.getAnnotationImports().get(0));
        assertEquals("com.mycompany.myapp.annotations.ClassOne", common.getAnnotationImports().get(1));

        // assert XML DOM - no schema
        assertEquals(2, common.getEventTypesXMLDOM().size());
        ConfigurationCommonEventTypeXMLDOM noSchemaDesc = common.getEventTypesXMLDOM().get("MyNoSchemaXMLEventName");
        assertEquals("MyNoSchemaEvent", noSchemaDesc.getRootElementName());
        assertEquals("/myevent/element1", noSchemaDesc.getXPathProperties().get("element1").getXpath());
        assertEquals(XPathConstants.NUMBER, noSchemaDesc.getXPathProperties().get("element1").getType());
        assertNull(noSchemaDesc.getXPathProperties().get("element1").getOptionalCastToType());
        assertNull(noSchemaDesc.getXPathFunctionResolver());
        assertNull(noSchemaDesc.getXPathVariableResolver());
        assertFalse(noSchemaDesc.isXPathPropertyExpr());

        // assert XML DOM - with schema
        ConfigurationCommonEventTypeXMLDOM schemaDesc = common.getEventTypesXMLDOM().get("MySchemaXMLEventName");
        assertEquals("MySchemaEvent", schemaDesc.getRootElementName());
        assertEquals("MySchemaXMLEvent.xsd", schemaDesc.getSchemaResource());
        assertEquals("actual-xsd-text-here", schemaDesc.getSchemaText());
        assertEquals("samples:schemas:simpleSchema", schemaDesc.getRootElementNamespace());
        assertEquals("default-name-space", schemaDesc.getDefaultNamespace());
        assertEquals("/myevent/element2", schemaDesc.getXPathProperties().get("element2").getXpath());
        assertEquals(XPathConstants.STRING, schemaDesc.getXPathProperties().get("element2").getType());
        assertEquals(Long.class, schemaDesc.getXPathProperties().get("element2").getOptionalCastToType());
        assertEquals("/bookstore/book", schemaDesc.getXPathProperties().get("element3").getXpath());
        assertEquals(XPathConstants.NODESET, schemaDesc.getXPathProperties().get("element3").getType());
        assertNull(schemaDesc.getXPathProperties().get("element3").getOptionalCastToType());
        assertEquals("MyOtherXMLNodeEvent", schemaDesc.getXPathProperties().get("element3").getOptionaleventTypeName());
        assertEquals(1, schemaDesc.getNamespacePrefixes().size());
        assertEquals("samples:schemas:simpleSchema", schemaDesc.getNamespacePrefixes().get("ss"));
        assertFalse(schemaDesc.isXPathResolvePropertiesAbsolute());
        assertEquals("com.mycompany.OptionalFunctionResolver", schemaDesc.getXPathFunctionResolver());
        assertEquals("com.mycompany.OptionalVariableResolver", schemaDesc.getXPathVariableResolver());
        assertTrue(schemaDesc.isXPathPropertyExpr());
        assertFalse(schemaDesc.isEventSenderValidatesRoot());
        assertFalse(schemaDesc.isAutoFragment());
        assertEquals("startts", schemaDesc.getStartTimestampPropertyName());
        assertEquals("endts", schemaDesc.getEndTimestampPropertyName());

        // assert mapped events
        assertEquals(1, common.getEventTypesMapEvents().size());
        assertTrue(common.getEventTypesMapEvents().keySet().contains("MyMapEvent"));
        Map<String, String> expectedProps = new HashMap<String, String>();
        expectedProps.put("myInt", "int");
        expectedProps.put("myString", "string");
        assertEquals(expectedProps, common.getEventTypesMapEvents().get("MyMapEvent"));
        assertEquals(1, common.getMapTypeConfigurations().size());
        Set<String> superTypes = common.getMapTypeConfigurations().get("MyMapEvent").getSuperTypes();
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"MyMapSuperType1", "MyMapSuperType2"}, superTypes.toArray());
        assertEquals("startts", common.getMapTypeConfigurations().get("MyMapEvent").getStartTimestampPropertyName());
        assertEquals("endts", common.getMapTypeConfigurations().get("MyMapEvent").getEndTimestampPropertyName());

        // assert objectarray events
        assertEquals(1, common.getEventTypesNestableObjectArrayEvents().size());
        assertTrue(common.getEventTypesNestableObjectArrayEvents().containsKey("MyObjectArrayEvent"));
        Map<String, String> expectedPropsObjectArray = new HashMap<String, String>();
        expectedPropsObjectArray.put("myInt", "int");
        expectedPropsObjectArray.put("myString", "string");
        assertEquals(expectedPropsObjectArray, common.getEventTypesNestableObjectArrayEvents().get("MyObjectArrayEvent"));
        assertEquals(1, common.getObjectArrayTypeConfigurations().size());
        Set<String> superTypesOA = common.getObjectArrayTypeConfigurations().get("MyObjectArrayEvent").getSuperTypes();
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"MyObjectArraySuperType1", "MyObjectArraySuperType2"}, superTypesOA.toArray());
        assertEquals("startts", common.getObjectArrayTypeConfigurations().get("MyObjectArrayEvent").getStartTimestampPropertyName());
        assertEquals("endts", common.getObjectArrayTypeConfigurations().get("MyObjectArrayEvent").getEndTimestampPropertyName());

        // assert avro events
        assertEquals(2, common.getEventTypesAvro().size());
        ConfigurationCommonEventTypeAvro avroOne = common.getEventTypesAvro().get("MyAvroEvent");
        assertEquals("{\"type\":\"record\",\"name\":\"typename\",\"fields\":[{\"name\":\"num\",\"type\":\"int\"}]}", avroOne.getAvroSchemaText());
        assertNull(avroOne.getAvroSchema());
        assertNull(avroOne.getStartTimestampPropertyName());
        assertNull(avroOne.getEndTimestampPropertyName());
        assertTrue(avroOne.getSuperTypes().isEmpty());
        ConfigurationCommonEventTypeAvro avroTwo = common.getEventTypesAvro().get("MyAvroEventTwo");
        assertEquals("{\"type\":\"record\",\"name\":\"MyAvroEvent\",\"fields\":[{\"name\":\"carId\",\"type\":\"int\"},{\"name\":\"carType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}", avroTwo.getAvroSchemaText());
        assertEquals("startts", avroTwo.getStartTimestampPropertyName());
        assertEquals("endts", avroTwo.getEndTimestampPropertyName());
        assertEquals("[SomeSuperAvro, SomeSuperAvroTwo]", avroTwo.getSuperTypes().toString());

        // assert legacy type declaration
        assertEquals(1, common.getEventTypesBean().size());
        ConfigurationCommonEventTypeBean legacy = common.getEventTypesBean().get("MyLegacyTypeEvent");
        assertEquals(AccessorStyle.PUBLIC, legacy.getAccessorStyle());
        assertEquals(1, legacy.getFieldProperties().size());
        assertEquals("myFieldName", legacy.getFieldProperties().get(0).getAccessorFieldName());
        assertEquals("myfieldprop", legacy.getFieldProperties().get(0).getName());
        assertEquals(1, legacy.getMethodProperties().size());
        assertEquals("myAccessorMethod", legacy.getMethodProperties().get(0).getAccessorMethodName());
        assertEquals("mymethodprop", legacy.getMethodProperties().get(0).getName());
        assertEquals(PropertyResolutionStyle.CASE_INSENSITIVE, legacy.getPropertyResolutionStyle());
        assertEquals("com.mycompany.myapp.MySampleEventFactory.createMyLegacyTypeEvent", legacy.getFactoryMethod());
        assertEquals("myCopyMethod", legacy.getCopyMethod());
        assertEquals("startts", legacy.getStartTimestampPropertyName());
        assertEquals("endts", legacy.getEndTimestampPropertyName());

        // assert database reference - data source config
        assertEquals(3, common.getDatabaseReferences().size());
        ConfigurationCommonDBRef configDBRef = common.getDatabaseReferences().get("mydb1");
        ConfigurationCommonDBRef.DataSourceConnection dsDef = (ConfigurationCommonDBRef.DataSourceConnection) configDBRef.getConnectionFactoryDesc();
        assertEquals("java:comp/env/jdbc/mydb", dsDef.getContextLookupName());
        assertEquals(ConfigurationCommonDBRef.ConnectionLifecycleEnum.POOLED, configDBRef.getConnectionLifecycleEnum());
        assertNull(configDBRef.getConnectionSettings().getAutoCommit());
        assertNull(configDBRef.getConnectionSettings().getCatalog());
        assertNull(configDBRef.getConnectionSettings().getReadOnly());
        assertNull(configDBRef.getConnectionSettings().getTransactionIsolation());
        ConfigurationCommonCacheLRU lruCache = (ConfigurationCommonCacheLRU) configDBRef.getDataCacheDesc();
        assertEquals(10, lruCache.getSize());
        assertEquals(ConfigurationCommonDBRef.ColumnChangeCaseEnum.LOWERCASE, configDBRef.getColumnChangeCase());
        assertEquals(ConfigurationCommonDBRef.MetadataOriginEnum.SAMPLE, configDBRef.getMetadataRetrievalEnum());
        assertEquals(2, configDBRef.getSqlTypesMapping().size());
        assertEquals("int", configDBRef.getSqlTypesMapping().get(2));
        assertEquals("float", configDBRef.getSqlTypesMapping().get(6));

        // assert database reference - driver manager config
        configDBRef = common.getDatabaseReferences().get("mydb2");
        ConfigurationCommonDBRef.DriverManagerConnection dmDef = (ConfigurationCommonDBRef.DriverManagerConnection) configDBRef.getConnectionFactoryDesc();
        assertEquals("my.sql.Driver", dmDef.getClassName());
        assertEquals("jdbc:mysql://localhost", dmDef.getUrl());
        assertEquals("myuser1", dmDef.getOptionalUserName());
        assertEquals("mypassword1", dmDef.getOptionalPassword());
        assertEquals("{password=mypassword2, somearg=someargvalue, user=myuser2}", new TreeMap(dmDef.getOptionalProperties()).toString());
        assertEquals(ConfigurationCommonDBRef.ConnectionLifecycleEnum.RETAIN, configDBRef.getConnectionLifecycleEnum());
        assertEquals((Boolean) false, configDBRef.getConnectionSettings().getAutoCommit());
        assertEquals("test", configDBRef.getConnectionSettings().getCatalog());
        assertEquals(Boolean.TRUE, configDBRef.getConnectionSettings().getReadOnly());
        assertEquals(new Integer(3), configDBRef.getConnectionSettings().getTransactionIsolation());
        ConfigurationCommonCacheExpiryTime expCache = (ConfigurationCommonCacheExpiryTime) configDBRef.getDataCacheDesc();
        assertEquals(60.5, expCache.getMaxAgeSeconds());
        assertEquals(120.1, expCache.getPurgeIntervalSeconds());
        assertEquals(CacheReferenceType.HARD, expCache.getCacheReferenceType());
        assertEquals(ConfigurationCommonDBRef.ColumnChangeCaseEnum.UPPERCASE, configDBRef.getColumnChangeCase());
        assertEquals(ConfigurationCommonDBRef.MetadataOriginEnum.METADATA, configDBRef.getMetadataRetrievalEnum());
        assertEquals(1, configDBRef.getSqlTypesMapping().size());
        assertEquals("java.lang.String", configDBRef.getSqlTypesMapping().get(99));

        // assert database reference - data source factory and DBCP config
        configDBRef = common.getDatabaseReferences().get("mydb3");
        ConfigurationCommonDBRef.DataSourceFactory dsFactory = (ConfigurationCommonDBRef.DataSourceFactory) configDBRef.getConnectionFactoryDesc();
        assertEquals("org.apache.commons.dbcp.BasicDataSourceFactory", dsFactory.getFactoryClassname());
        assertEquals("jdbc:mysql://localhost/test", dsFactory.getProperties().getProperty("url"));
        assertEquals("myusername", dsFactory.getProperties().getProperty("username"));
        assertEquals("mypassword", dsFactory.getProperties().getProperty("password"));
        assertEquals("com.mysql.cj.jdbc.Driver", dsFactory.getProperties().getProperty("driverClassName"));
        assertEquals("2", dsFactory.getProperties().getProperty("initialSize"));

        assertEquals(PropertyResolutionStyle.DISTINCT_CASE_INSENSITIVE, common.getEventMeta().getClassPropertyResolutionStyle());
        assertEquals(AccessorStyle.PUBLIC, common.getEventMeta().getDefaultAccessorStyle());
        assertEquals(EventUnderlyingType.MAP, common.getEventMeta().getDefaultEventRepresentation());
        assertFalse(common.getEventMeta().getAvroSettings().isEnableAvro());
        assertFalse(common.getEventMeta().getAvroSettings().isEnableNativeString());
        assertFalse(common.getEventMeta().getAvroSettings().isEnableSchemaDefaultNonNull());
        assertEquals("myObjectValueTypeWidenerFactoryClass", common.getEventMeta().getAvroSettings().getObjectValueTypeWidenerFactoryClass());
        assertEquals("myTypeToRepresentationMapperClass", common.getEventMeta().getAvroSettings().getTypeRepresentationMapperClass());

        assertTrue(common.getLogging().isEnableQueryPlan());
        assertTrue(common.getLogging().isEnableJDBC());

        assertEquals(TimeUnit.MICROSECONDS, common.getTimeSource().getTimeUnit());

        // variables
        assertEquals(3, common.getVariables().size());
        ConfigurationCommonVariable variable = common.getVariables().get("var1");
        assertEquals(Integer.class.getName(), variable.getType());
        assertEquals("1", variable.getInitializationValue());
        assertFalse(variable.isConstant());
        variable = common.getVariables().get("var2");
        assertEquals(String.class.getName(), variable.getType());
        assertNull(variable.getInitializationValue());
        assertFalse(variable.isConstant());
        variable = common.getVariables().get("var3");
        assertTrue(variable.isConstant());

        // method references
        assertEquals(2, common.getMethodInvocationReferences().size());
        ConfigurationCommonMethodRef methodRef = common.getMethodInvocationReferences().get("abc");
        expCache = (ConfigurationCommonCacheExpiryTime) methodRef.getDataCacheDesc();
        assertEquals(91.0, expCache.getMaxAgeSeconds());
        assertEquals(92.2, expCache.getPurgeIntervalSeconds());
        assertEquals(CacheReferenceType.WEAK, expCache.getCacheReferenceType());

        methodRef = common.getMethodInvocationReferences().get("def");
        lruCache = (ConfigurationCommonCacheLRU) methodRef.getDataCacheDesc();
        assertEquals(20, lruCache.getSize());

        // variance types
        assertEquals(1, common.getVariantStreams().size());
        ConfigurationCommonVariantStream configVStream = common.getVariantStreams().get("MyVariantStream");
        assertEquals(2, configVStream.getVariantTypeNames().size());
        assertTrue(configVStream.getVariantTypeNames().contains("MyEvenTypetNameOne"));
        assertTrue(configVStream.getVariantTypeNames().contains("MyEvenTypetNameTwo"));
        assertEquals(ConfigurationCommonVariantStream.TypeVariance.ANY, configVStream.getTypeVariance());

        assertEquals(ThreadingProfile.LARGE, common.getExecution().getThreadingProfile());

        assertEquals(2, common.getEventTypeAutoNamePackages().size());
        assertEquals("com.mycompany.eventsone", common.getEventTypeAutoNamePackages().toArray()[0]);
        assertEquals("com.mycompany.eventstwo", common.getEventTypeAutoNamePackages().toArray()[1]);

        /*
         * COMPILER
         *
         */

        // assert custom view implementations
        List<ConfigurationCompilerPlugInView> configViews = compiler.getPlugInViews();
        assertEquals(2, configViews.size());
        for (int i = 0; i < configViews.size(); i++) {
            ConfigurationCompilerPlugInView entry = configViews.get(i);
            assertEquals("ext" + i, entry.getNamespace());
            assertEquals("myview" + i, entry.getName());
            assertEquals("com.mycompany.MyViewForge" + i, entry.getForgeClassName());
        }

        // assert custom virtual data window implementations
        List<ConfigurationCompilerPlugInVirtualDataWindow> configVDW = compiler.getPlugInVirtualDataWindows();
        assertEquals(2, configVDW.size());
        for (int i = 0; i < configVDW.size(); i++) {
            ConfigurationCompilerPlugInVirtualDataWindow entry = configVDW.get(i);
            assertEquals("vdw" + i, entry.getNamespace());
            assertEquals("myvdw" + i, entry.getName());
            assertEquals("com.mycompany.MyVdwForge" + i, entry.getForgeClassName());
            if (i == 1) {
                assertEquals("abc", entry.getConfig());
            }
        }

        // assert plug-in aggregation function loaded
        assertEquals(2, compiler.getPlugInAggregationFunctions().size());
        ConfigurationCompilerPlugInAggregationFunction pluginAgg = compiler.getPlugInAggregationFunctions().get(0);
        assertEquals("func1a", pluginAgg.getName());
        assertEquals("com.mycompany.MyMatrixAggregationMethod0Forge", pluginAgg.getForgeClassName());
        pluginAgg = compiler.getPlugInAggregationFunctions().get(1);
        assertEquals("func2a", pluginAgg.getName());
        assertEquals("com.mycompany.MyMatrixAggregationMethod1Forge", pluginAgg.getForgeClassName());

        // assert plug-in aggregation multi-function loaded
        assertEquals(1, compiler.getPlugInAggregationMultiFunctions().size());
        ConfigurationCompilerPlugInAggregationMultiFunction pluginMultiAgg = compiler.getPlugInAggregationMultiFunctions().get(0);
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"func1", "func2"}, pluginMultiAgg.getFunctionNames());
        assertEquals("com.mycompany.MyAggregationMultiFunctionForge", pluginMultiAgg.getMultiFunctionForgeClassName());
        assertEquals(1, pluginMultiAgg.getAdditionalConfiguredProperties().size());
        assertEquals("value1", pluginMultiAgg.getAdditionalConfiguredProperties().get("prop1"));

        // assert plug-in singlerow function loaded
        assertEquals(2, compiler.getPlugInSingleRowFunctions().size());
        ConfigurationCompilerPlugInSingleRowFunction pluginSingleRow = compiler.getPlugInSingleRowFunctions().get(0);
        assertEquals("com.mycompany.MyMatrixSingleRowMethod0", pluginSingleRow.getFunctionClassName());
        assertEquals("method1", pluginSingleRow.getFunctionMethodName());
        assertEquals("func3", pluginSingleRow.getName());
        assertEquals(ConfigurationCompilerPlugInSingleRowFunction.ValueCache.DISABLED, pluginSingleRow.getValueCache());
        assertEquals(ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED, pluginSingleRow.getFilterOptimizable());
        assertFalse(pluginSingleRow.isRethrowExceptions());
        pluginSingleRow = compiler.getPlugInSingleRowFunctions().get(1);
        assertEquals("com.mycompany.MyMatrixSingleRowMethod1", pluginSingleRow.getFunctionClassName());
        assertEquals("func4", pluginSingleRow.getName());
        assertEquals("method2", pluginSingleRow.getFunctionMethodName());
        assertEquals(ConfigurationCompilerPlugInSingleRowFunction.ValueCache.ENABLED, pluginSingleRow.getValueCache());
        assertEquals(ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.DISABLED, pluginSingleRow.getFilterOptimizable());
        assertTrue(pluginSingleRow.isRethrowExceptions());
        assertEquals("XYZEventTypeName", pluginSingleRow.getEventTypeName());

        // assert plug-in guard objects loaded
        assertEquals(4, compiler.getPlugInPatternObjects().size());
        ConfigurationCompilerPlugInPatternObject pluginPattern = compiler.getPlugInPatternObjects().get(0);
        assertEquals("com.mycompany.MyGuardForge0", pluginPattern.getForgeClassName());
        assertEquals("ext0", pluginPattern.getNamespace());
        assertEquals("guard1", pluginPattern.getName());
        assertEquals(PatternObjectType.GUARD, pluginPattern.getPatternObjectType());
        pluginPattern = compiler.getPlugInPatternObjects().get(1);
        assertEquals("com.mycompany.MyGuardForge1", pluginPattern.getForgeClassName());
        assertEquals("ext1", pluginPattern.getNamespace());
        assertEquals("guard2", pluginPattern.getName());
        assertEquals(PatternObjectType.GUARD, pluginPattern.getPatternObjectType());
        pluginPattern = compiler.getPlugInPatternObjects().get(2);
        assertEquals("com.mycompany.MyObserverForge0", pluginPattern.getForgeClassName());
        assertEquals("ext0", pluginPattern.getNamespace());
        assertEquals("observer1", pluginPattern.getName());
        assertEquals(PatternObjectType.OBSERVER, pluginPattern.getPatternObjectType());
        pluginPattern = compiler.getPlugInPatternObjects().get(3);
        assertEquals("com.mycompany.MyObserverForge1", pluginPattern.getForgeClassName());
        assertEquals("ext1", pluginPattern.getNamespace());
        assertEquals("observer2", pluginPattern.getName());
        assertEquals(PatternObjectType.OBSERVER, pluginPattern.getPatternObjectType());

        // assert plug-in date-time method and enum-method
        List<ConfigurationCompilerPlugInDateTimeMethod> configDTM = compiler.getPlugInDateTimeMethods();
        assertEquals(1, configDTM.size());
        ConfigurationCompilerPlugInDateTimeMethod dtmOne = configDTM.get(0);
        assertEquals("methodname1", dtmOne.getName());
        assertEquals("com.mycompany.MyDateTimeMethodForge", dtmOne.getForgeClassName());
        List<ConfigurationCompilerPlugInEnumMethod> configENM = compiler.getPlugInEnumMethods();
        assertEquals(1, configENM.size());
        ConfigurationCompilerPlugInEnumMethod enmOne = configENM.get(0);
        assertEquals("methodname2", enmOne.getName());
        assertEquals("com.mycompany.MyEnumMethodForge", enmOne.getForgeClassName());

        assertTrue(compiler.getViewResources().isIterableUnbound());
        assertFalse(compiler.getViewResources().isOutputLimitOpt());

        assertTrue(compiler.getLogging().isEnableCode());

        assertEquals(StreamSelector.RSTREAM_ISTREAM_BOTH, compiler.getStreamSelection().getDefaultStreamSelector());

        ConfigurationCompilerByteCode byteCode = compiler.getByteCode();
        assertTrue(byteCode.isIncludeComments());
        assertTrue(byteCode.isIncludeDebugSymbols());
        assertFalse(byteCode.isAttachEPL());
        assertTrue(byteCode.isAttachModuleEPL());
        assertTrue(byteCode.isAttachPatternEPL());
        assertTrue(byteCode.isInstrumented());
        assertTrue(byteCode.isAllowSubscriber());
        assertEquals(NameAccessModifier.PROTECTED, byteCode.getAccessModifierContext());
        assertEquals(NameAccessModifier.PUBLIC, byteCode.getAccessModifierEventType());
        assertEquals(NameAccessModifier.PROTECTED, byteCode.getAccessModifierExpression());
        assertEquals(NameAccessModifier.PUBLIC, byteCode.getAccessModifierNamedWindow());
        assertEquals(NameAccessModifier.PROTECTED, byteCode.getAccessModifierScript());
        assertEquals(NameAccessModifier.PUBLIC, byteCode.getAccessModifierTable());
        assertEquals(NameAccessModifier.PROTECTED, byteCode.getAccessModifierVariable());
        assertEquals(EventTypeBusModifier.BUS, byteCode.getBusModifierEventType());
        assertEquals(1234, byteCode.getThreadPoolCompilerNumThreads());
        assertEquals(4321, (int) byteCode.getThreadPoolCompilerCapacity());
        assertEquals(5555, byteCode.getMaxMethodsPerClass());
        assertEquals(StreamSelector.RSTREAM_ISTREAM_BOTH, compiler.getStreamSelection().getDefaultStreamSelector());

        assertEquals(100, compiler.getExecution().getFilterServiceMaxFilterWidth());
        assertFalse(compiler.getExecution().isEnabledDeclaredExprValueCache());

        assertTrue(compiler.getLanguage().isSortUsingCollator());

        assertTrue(compiler.getExpression().isIntegerDivision());
        assertTrue(compiler.getExpression().isDivisionByZeroReturnsNull());
        assertFalse(compiler.getExpression().isUdfCache());
        assertFalse(compiler.getExpression().isExtendedAggregation());
        assertTrue(compiler.getExpression().isDuckTyping());
        assertEquals(2, compiler.getExpression().getMathContext().getPrecision());
        assertEquals(RoundingMode.CEILING, compiler.getExpression().getMathContext().getRoundingMode());

        assertEquals("abc", compiler.getScripts().getDefaultDialect());
        assertFalse(compiler.getScripts().isEnabled());

        assertFalse(compiler.getSerde().isEnableExtendedBuiltin());
        assertTrue(compiler.getSerde().isEnableExternalizable());
        assertTrue(compiler.getSerde().isEnableSerializable());
        assertTrue(compiler.getSerde().isEnableSerializationFallback());
        List<String> serdeProviderFactories = compiler.getSerde().getSerdeProviderFactories();
        assertEquals(2, serdeProviderFactories.size());
        assertEquals("a.b.c.MySerdeProviderFactoryOne", serdeProviderFactories.get(0));
        assertEquals("a.b.c.MySerdeProviderFactoryTwo", serdeProviderFactories.get(1));

        /*
         * RUNTIME
         *
         */

        // assert runtime defaults
        assertFalse(runtime.getThreading().isInsertIntoDispatchPreserveOrder());
        assertEquals(3000, runtime.getThreading().getInsertIntoDispatchTimeout());
        assertEquals(Locking.SUSPEND, runtime.getThreading().getInsertIntoDispatchLocking());
        assertFalse(runtime.getThreading().isNamedWindowConsumerDispatchPreserveOrder());
        assertEquals(4000, runtime.getThreading().getNamedWindowConsumerDispatchTimeout());
        assertEquals(Locking.SUSPEND, runtime.getThreading().getNamedWindowConsumerDispatchLocking());

        assertFalse(runtime.getThreading().isListenerDispatchPreserveOrder());
        assertEquals(2000, runtime.getThreading().getListenerDispatchTimeout());
        assertEquals(Locking.SUSPEND, runtime.getThreading().getListenerDispatchLocking());
        assertTrue(runtime.getThreading().isThreadPoolInbound());
        assertTrue(runtime.getThreading().isThreadPoolOutbound());
        assertTrue(runtime.getThreading().isThreadPoolRouteExec());
        assertTrue(runtime.getThreading().isThreadPoolTimerExec());
        assertEquals(1, runtime.getThreading().getThreadPoolInboundNumThreads());
        assertEquals(2, runtime.getThreading().getThreadPoolOutboundNumThreads());
        assertEquals(3, runtime.getThreading().getThreadPoolTimerExecNumThreads());
        assertEquals(4, runtime.getThreading().getThreadPoolRouteExecNumThreads());
        assertEquals(1000, (int) runtime.getThreading().getThreadPoolInboundCapacity());
        assertEquals(1500, (int) runtime.getThreading().getThreadPoolOutboundCapacity());
        assertNull(runtime.getThreading().getThreadPoolTimerExecCapacity());
        assertEquals(2000, (int) runtime.getThreading().getThreadPoolRouteExecCapacity());
        assertTrue(runtime.getThreading().isRuntimeFairlock());

        assertFalse(runtime.getThreading().isInternalTimerEnabled());
        assertEquals(1234567, runtime.getThreading().getInternalTimerMsecResolution());
        assertTrue(runtime.getLogging().isEnableExecutionDebug());
        assertFalse(runtime.getLogging().isEnableTimerDebug());
        assertEquals("[%u] %m", runtime.getLogging().getAuditPattern());
        assertEquals(30000, runtime.getVariables().getMsecVersionRelease());
        assertEquals(3L, (long) runtime.getPatterns().getMaxSubexpressions());
        assertFalse(runtime.getPatterns().isMaxSubexpressionPreventStart());
        assertEquals(3L, (long) runtime.getMatchRecognize().getMaxStates());
        assertFalse(runtime.getMatchRecognize().isMaxStatesPreventStart());

        // assert adapter loaders parsed
        List<ConfigurationRuntimePluginLoader> plugins = runtime.getPluginLoaders();
        assertEquals(2, plugins.size());
        ConfigurationRuntimePluginLoader pluginOne = plugins.get(0);
        assertEquals("Loader1", pluginOne.getLoaderName());
        assertEquals("com.espertech.esper.support.plugin.SupportLoaderOne", pluginOne.getClassName());
        assertEquals(2, pluginOne.getConfigProperties().size());
        assertEquals("val1", pluginOne.getConfigProperties().get("name1"));
        assertEquals("val2", pluginOne.getConfigProperties().get("name2"));
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><sample-initializer><some-any-xml-can-be-here>This section for use by a plugin loader.</some-any-xml-can-be-here></sample-initializer>", pluginOne.getConfigurationXML());

        ConfigurationRuntimePluginLoader pluginTwo = plugins.get(1);
        assertEquals("Loader2", pluginTwo.getLoaderName());
        assertEquals("com.espertech.esper.support.plugin.SupportLoaderTwo", pluginTwo.getClassName());
        assertEquals(0, pluginTwo.getConfigProperties().size());

        assertEquals(TimeSourceType.NANO, runtime.getTimeSource().getTimeSourceType());
        assertTrue(runtime.getExecution().isPrioritized());
        assertTrue(runtime.getExecution().isFairlock());
        assertTrue(runtime.getExecution().isDisableLocking());
        assertEquals(FilterServiceProfile.READWRITE, runtime.getExecution().getFilterServiceProfile());
        assertEquals(101, runtime.getExecution().getDeclaredExprValueCacheSize());

        ConfigurationRuntimeMetricsReporting metrics = runtime.getMetricsReporting();
        assertTrue(metrics.isEnableMetricsReporting());
        assertEquals(4000L, metrics.getRuntimeInterval());
        assertEquals(500L, metrics.getStatementInterval());
        assertFalse(metrics.isThreading());
        assertEquals(2, metrics.getStatementGroups().size());
        assertTrue(metrics.isJmxRuntimeMetrics());
        ConfigurationRuntimeMetricsReporting.StmtGroupMetrics def = metrics.getStatementGroups().get("MyStmtGroup");
        assertEquals(5000, def.getInterval());
        assertTrue(def.isDefaultInclude());
        assertEquals(50, def.getNumStatements());
        assertTrue(def.isReportInactive());
        assertEquals(5, def.getPatterns().size());
        assertEquals(def.getPatterns().get(0), new Pair<StringPatternSet, Boolean>(new StringPatternSetRegex(".*"), true));
        assertEquals(def.getPatterns().get(1), new Pair<StringPatternSet, Boolean>(new StringPatternSetRegex(".*test.*"), false));
        assertEquals(def.getPatterns().get(2), new Pair<StringPatternSet, Boolean>(new StringPatternSetLike("%MyMetricsStatement%"), false));
        assertEquals(def.getPatterns().get(3), new Pair<StringPatternSet, Boolean>(new StringPatternSetLike("%MyFraudAnalysisStatement%"), true));
        assertEquals(def.getPatterns().get(4), new Pair<StringPatternSet, Boolean>(new StringPatternSetLike("%SomerOtherStatement%"), true));
        def = metrics.getStatementGroups().get("MyStmtGroupTwo");
        assertEquals(200, def.getInterval());
        assertFalse(def.isDefaultInclude());
        assertEquals(100, def.getNumStatements());
        assertFalse(def.isReportInactive());
        assertEquals(0, def.getPatterns().size());
        assertFalse(runtime.getExpression().isSelfSubselectPreeval());
        assertEquals(TimeZone.getTimeZone("GMT-4:00"), runtime.getExpression().getTimeZone());
        assertEquals(2, runtime.getExceptionHandling().getHandlerFactories().size());
        assertEquals("my.company.cep.LoggingExceptionHandlerFactory", runtime.getExceptionHandling().getHandlerFactories().get(0));
        assertEquals("my.company.cep.AlertExceptionHandlerFactory", runtime.getExceptionHandling().getHandlerFactories().get(1));
        assertEquals(UndeployRethrowPolicy.RETHROW_FIRST, runtime.getExceptionHandling().getUndeployRethrowPolicy());
        assertEquals(2, runtime.getConditionHandling().getHandlerFactories().size());
        assertEquals("my.company.cep.LoggingConditionHandlerFactory", runtime.getConditionHandling().getHandlerFactories().get(0));
        assertEquals("my.company.cep.AlertConditionHandlerFactory", runtime.getConditionHandling().getHandlerFactories().get(1));
    }
}
