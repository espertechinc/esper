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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeMeta;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorFactory;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorFactoryImpl;
import com.espertech.esper.common.internal.context.mgr.ContextServiceFactory;
import com.espertech.esper.common.internal.context.mgr.ContextServiceFactoryDefault;
import com.espertech.esper.common.internal.context.module.RuntimeExtensionServices;
import com.espertech.esper.common.internal.context.util.StatementContextResolver;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryService;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryServiceImpl;
import com.espertech.esper.common.internal.epl.dataflow.filtersvcadapter.DataFlowFilterServiceAdapter;
import com.espertech.esper.common.internal.epl.dataflow.filtersvcadapter.DataFlowFilterServiceAdapterNonHA;
import com.espertech.esper.common.internal.epl.historical.datacache.HistoricalDataCacheFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableIndexService;
import com.espertech.esper.common.internal.epl.index.base.EventTableIndexServiceImpl;
import com.espertech.esper.common.internal.epl.namedwindow.consume.*;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowManagementService;
import com.espertech.esper.common.internal.epl.pattern.core.PatternFactoryService;
import com.espertech.esper.common.internal.epl.pattern.core.PatternFactoryServiceImpl;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactoryDefault;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStateRepoFactory;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStateRepoFactoryDefault;
import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.TableManagementService;
import com.espertech.esper.common.internal.epl.table.core.TableManagementServiceImpl;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementServiceImpl;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandlerFactory;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.eventtypefactory.EventTypeFactory;
import com.espertech.esper.common.internal.event.eventtypefactory.EventTypeFactoryImpl;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepository;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepositoryImpl;
import com.espertech.esper.common.internal.filterspec.*;
import com.espertech.esper.common.internal.metrics.stmtmetrics.MetricReportingService;
import com.espertech.esper.common.internal.schedule.TimeSourceService;
import com.espertech.esper.common.internal.serde.DataInputOutputSerdeProvider;
import com.espertech.esper.common.internal.serde.DataInputOutputSerdeProviderDefault;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;
import com.espertech.esper.common.internal.settings.RuntimeSettingsService;
import com.espertech.esper.common.internal.statement.multimatch.MultiMatchHandlerFactory;
import com.espertech.esper.common.internal.statement.multimatch.MultiMatchHandlerFactoryImpl;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolderBuilder;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolderBuilderImpl;
import com.espertech.esper.common.internal.util.ManagedReadWriteLock;
import com.espertech.esper.common.internal.view.core.ViewFactoryService;
import com.espertech.esper.common.internal.view.core.ViewFactoryServiceImpl;
import com.espertech.esper.common.internal.view.previous.ViewServicePreviousFactory;
import com.espertech.esper.common.internal.view.previous.ViewServicePreviousFactoryImpl;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentLifecycleServiceImpl;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentRecoveryServiceImpl;
import com.espertech.esper.runtime.internal.deploymentlifesvc.ListenerRecoveryServiceImpl;
import com.espertech.esper.runtime.internal.deploymentlifesvc.StatementIdRecoveryServiceImpl;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterServiceLockCoarse;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterServiceSPI;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementFactory;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementFactoryDefault;
import com.espertech.esper.runtime.internal.kernel.thread.ThreadingService;
import com.espertech.esper.runtime.internal.kernel.thread.ThreadingServiceImpl;
import com.espertech.esper.runtime.internal.namedwindow.NamedWindowDispatchServiceImpl;
import com.espertech.esper.runtime.internal.schedulesvcimpl.SchedulingServiceImpl;
import com.espertech.esper.runtime.internal.schedulesvcimpl.SchedulingServiceSPI;
import com.espertech.esper.runtime.internal.statementlifesvc.StatementLifecycleServiceImpl;

import java.util.concurrent.atomic.AtomicBoolean;

public class EPServicesContextFactoryDefault extends EPServicesContextFactoryBase {

    protected RuntimeSettingsService makeRuntimeSettingsService(Configuration configurationSnapshot) {
        return new RuntimeSettingsService(configurationSnapshot.getCommon(), configurationSnapshot.getRuntime());
    }

    protected EPServicesHA initHA(String runtimeURI, Configuration configurationSnapshot, RuntimeEnvContext runtimeEnvContext, ManagedReadWriteLock eventProcessingRWLock, RuntimeSettingsService runtimeSettingsService) {
        return new EPServicesHA(RuntimeExtensionServicesNoHA.INSTANCE, DeploymentRecoveryServiceImpl.INSTANCE, ListenerRecoveryServiceImpl.INSTANCE, new StatementIdRecoveryServiceImpl(), null);
    }

    protected ViewableActivatorFactory initViewableActivatorFactory() {
        return ViewableActivatorFactoryImpl.INSTANCE;
    }

    protected FilterServiceSPI makeFilterService(RuntimeExtensionServices runtimeExt, EventTypeRepository eventTypeRepository, StatementLifecycleServiceImpl statementLifecycleService, RuntimeSettingsService runtimeSettingsService, EventTypeIdResolver eventTypeIdResolver, FilterSharedLookupableRepository filterSharedLookupableRepository) {
        return new FilterServiceLockCoarse(false);
    }

    public EPEventServiceImpl createEPRuntime(EPServicesContext services, AtomicBoolean serviceStatusProvider) {
        return new EPEventServiceImpl(services);
    }

    protected StatementResourceHolderBuilder makeStatementResourceHolderBuilder() {
        return StatementResourceHolderBuilderImpl.INSTANCE;
    }

    protected FilterSharedLookupableRepository makeFilterSharedLookupableRepository() {
        return FilterSharedLookupableRepositoryImpl.INSTANCE;
    }

    protected AggregationServiceFactoryService makeAggregationServiceFactoryService(RuntimeExtensionServices runtimeExt) {
        return AggregationServiceFactoryServiceImpl.INSTANCE;
    }

    protected ViewFactoryService makeViewFactoryService() {
        return ViewFactoryServiceImpl.INSTANCE;
    }

    protected PatternFactoryService makePatternFactoryService() {
        return PatternFactoryServiceImpl.INSTANCE;
    }

    protected EventTypeFactory makeEventTypeFactory(RuntimeExtensionServices runtimeExt, EventTypeRepositoryImpl eventTypeRepositoryPreconfigured, DeploymentLifecycleServiceImpl deploymentLifecycleService) {
        return EventTypeFactoryImpl.INSTANCE;
    }

    protected EventTypeResolvingBeanFactory makeEventTypeResolvingBeanFactory(EventTypeRepository eventTypeRepository, EventTypeAvroHandler eventTypeAvroHandler) {
        return new EventTypeResolvingBeanFactoryImpl(eventTypeRepository, eventTypeAvroHandler);
    }

    protected SchedulingServiceSPI makeSchedulingService(EPServicesHA epServicesHA, TimeSourceService timeSourceService, RuntimeExtensionServices runtimeExt, RuntimeSettingsService runtimeSettingsService, StatementContextResolver statementContextResolver) {
        return new SchedulingServiceImpl(timeSourceService);
    }

    protected FilterBooleanExpressionFactory makeFilterBooleanExpressionFactory(StatementLifecycleServiceImpl statementLifecycleService) {
        return FilterBooleanExpressionFactoryImpl.INSTANCE;
    }

    protected MultiMatchHandlerFactory makeMultiMatchHandlerFactory(Configuration configurationInformation) {
        return new MultiMatchHandlerFactoryImpl(configurationInformation.getRuntime().getExpression().isSelfSubselectPreeval());
    }


    protected ContextServiceFactory makeContextServiceFactory(RuntimeExtensionServices runtimeExtensionServices) {
        return ContextServiceFactoryDefault.INSTANCE;
    }

    protected ViewServicePreviousFactory makeViewServicePreviousFactory(RuntimeExtensionServices ext) {
        return ViewServicePreviousFactoryImpl.INSTANCE;
    }

    protected EPStatementFactory makeEPStatementFactory() {
        return EPStatementFactoryDefault.INSTANCE;
    }

    protected EventBeanTypedEventFactory makeEventBeanTypedEventFactory(EventTypeAvroHandler eventTypeAvroHandler) {
        return new EventBeanTypedEventFactoryRuntime(eventTypeAvroHandler);
    }

    protected EventTableIndexService makeEventTableIndexService(RuntimeExtensionServices runtimeExtensionServices) {
        return EventTableIndexServiceImpl.INSTANCE;
    }

    protected DataInputOutputSerdeProvider makeSerdeProvider(RuntimeExtensionServices ext) {
        return DataInputOutputSerdeProviderDefault.INSTANCE;
    }

    protected ResultSetProcessorHelperFactory makeResultSetProcessorHelperFactory(RuntimeExtensionServices ext) {
        return ResultSetProcessorHelperFactoryDefault.INSTANCE;
    }

    protected NamedWindowDispatchService makeNamedWindowDispatchService(SchedulingServiceSPI schedulingService, Configuration configurationSnapshot, ManagedReadWriteLock eventProcessingRWLock, ExceptionHandlingService exceptionHandlingService, VariableManagementService variableManagementService, TableManagementService tableManagementService, MetricReportingService metricReportingService) {
        return new NamedWindowDispatchServiceImpl(schedulingService, variableManagementService, tableManagementService, configurationSnapshot.getRuntime().getExecution().isPrioritized(), eventProcessingRWLock, exceptionHandlingService, metricReportingService);
    }

    protected NamedWindowConsumerManagementService makeNamedWindowConsumerManagementService(NamedWindowManagementService namedWindowManagementService) {
        return NamedWindowConsumerManagementServiceImpl.INSTANCE;
    }

    protected NamedWindowFactoryService makeNamedWindowFactoryService() {
        return NamedWindowFactoryServiceImpl.INSTANCE;
    }

    protected FilterSharedBoolExprRepository makeFilterSharedBoolExprRepository() {
        return FilterSharedBoolExprRepositoryImpl.INSTANCE;
    }

    protected VariableManagementService makeVariableManagementService(Configuration configs, SchedulingServiceSPI schedulingService, EventBeanTypedEventFactory eventBeanTypedEventFactory, RuntimeSettingsService runtimeSettingsService, EPServicesHA epServicesHA) {
        return new VariableManagementServiceImpl(configs.getRuntime().getVariables().getMsecVersionRelease(), schedulingService, eventBeanTypedEventFactory, null);
    }

    protected TableManagementService makeTableManagementService(RuntimeExtensionServices runtimeExt, TableExprEvaluatorContext tableExprEvaluatorContext) {
        return new TableManagementServiceImpl(tableExprEvaluatorContext);
    }

    protected RowRecogStateRepoFactory makeRowRecogStateRepoFactory() {
        return RowRecogStateRepoFactoryDefault.INSTANCE;
    }

    protected EventTypeAvroHandler makeEventTypeAvroHandler(ClasspathImportServiceRuntime classpathImportServiceRuntime, ConfigurationCommonEventTypeMeta.AvroSettings avroSettings, RuntimeExtensionServices runtimeExt) {
        return EventTypeAvroHandlerFactory.resolve(classpathImportServiceRuntime, avroSettings, EventTypeAvroHandler.RUNTIME_NONHA_HANDLER_IMPL);
    }

    protected HistoricalDataCacheFactory makeHistoricalDataCacheFactory(RuntimeExtensionServices runtimeExtensionServices) {
        return new HistoricalDataCacheFactory();
    }

    protected DataFlowFilterServiceAdapter makeDataFlowFilterServiceAdapter() {
        return DataFlowFilterServiceAdapterNonHA.INSTANCE;
    }

    protected ThreadingService makeThreadingService(Configuration configs) {
        return new ThreadingServiceImpl(configs.getRuntime().getThreading());
    }
}

