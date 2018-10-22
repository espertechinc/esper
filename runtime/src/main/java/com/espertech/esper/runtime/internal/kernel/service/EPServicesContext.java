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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.client.util.ClassForNameProvider;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorFactory;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.mgr.ContextServiceFactory;
import com.espertech.esper.common.internal.context.module.RuntimeExtensionServices;
import com.espertech.esper.common.internal.context.util.InternalEventRouteDest;
import com.espertech.esper.common.internal.context.util.InternalEventRouterImpl;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLockFactory;
import com.espertech.esper.common.internal.context.util.StatementContextRuntimeServices;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryService;
import com.espertech.esper.common.internal.epl.dataflow.core.EPDataFlowServiceImpl;
import com.espertech.esper.common.internal.epl.dataflow.filtersvcadapter.DataFlowFilterServiceAdapter;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheService;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConfigServiceRuntime;
import com.espertech.esper.common.internal.epl.historical.datacache.HistoricalDataCacheFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableIndexService;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerManagementService;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowDispatchService;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowFactoryService;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowManagementService;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.pattern.core.PatternFactoryService;
import com.espertech.esper.common.internal.epl.pattern.pool.PatternSubexpressionPoolRuntimeSvc;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStatePoolRuntimeSvc;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStateRepoFactory;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.TableManagementService;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.bean.core.BeanEventTypeStemService;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryPrivate;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventTypeResolvingBeanFactory;
import com.espertech.esper.common.internal.event.eventtypefactory.EventTypeFactory;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepositoryImpl;
import com.espertech.esper.common.internal.event.render.EPRenderEventServiceImpl;
import com.espertech.esper.common.internal.event.xml.XMLFragmentEventTypeFactory;
import com.espertech.esper.common.internal.filterspec.FilterBooleanExpressionFactory;
import com.espertech.esper.common.internal.filterspec.FilterSharedBoolExprRepository;
import com.espertech.esper.common.internal.filterspec.FilterSharedLookupableRepository;
import com.espertech.esper.common.internal.metrics.stmtmetrics.MetricReportingService;
import com.espertech.esper.common.internal.schedule.TimeSourceService;
import com.espertech.esper.common.internal.serde.DataInputOutputSerdeProvider;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;
import com.espertech.esper.common.internal.settings.RuntimeSettingsService;
import com.espertech.esper.common.internal.statement.dispatch.DispatchService;
import com.espertech.esper.common.internal.statement.multimatch.MultiMatchHandlerFactory;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolderBuilder;
import com.espertech.esper.common.internal.util.ManagedReadWriteLock;
import com.espertech.esper.common.internal.view.core.ViewFactoryService;
import com.espertech.esper.common.internal.view.previous.ViewServicePreviousFactory;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentLifecycleService;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentRecoveryService;
import com.espertech.esper.runtime.internal.deploymentlifesvc.ListenerRecoveryService;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterServiceSPI;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementFactory;
import com.espertech.esper.runtime.internal.kernel.thread.ThreadingService;
import com.espertech.esper.runtime.internal.schedulesvcimpl.SchedulingServiceSPI;
import com.espertech.esper.runtime.internal.statementlifesvc.StatementLifecycleService;
import com.espertech.esper.runtime.internal.timer.TimerService;

public class EPServicesContext {
    private final AggregationServiceFactoryService aggregationServiceFactoryService;
    private final BeanEventTypeFactoryPrivate beanEventTypeFactoryPrivate;
    private final BeanEventTypeStemService beanEventTypeStemService;
    private final ClassForNameProvider classForNameProvider;
    private final Configuration configSnapshot;
    private final ContextManagementService contextManagementService;
    private final PathRegistry<String, ContextMetaData> contextPathRegistry;
    private final ContextServiceFactory contextServiceFactory;
    private final EPDataFlowServiceImpl dataflowService;
    private final DataFlowFilterServiceAdapter dataFlowFilterServiceAdapter;
    private final DataInputOutputSerdeProvider dataInputOutputSerdeProvider;
    private final DatabaseConfigServiceRuntime databaseConfigServiceRuntime;
    private final DeploymentLifecycleService deploymentLifecycleService;
    private final DispatchService dispatchService;
    private final RuntimeEnvContext runtimeEnvContext;
    private final RuntimeSettingsService runtimeSettingsService;
    private final String runtimeURI;
    private final ClasspathImportServiceRuntime classpathImportServiceRuntime;
    private final EPStatementFactory epStatementFactory;
    private final PathRegistry<String, ExpressionDeclItem> exprDeclaredPathRegistry;
    private final ManagedReadWriteLock eventProcessingRWLock;
    private final EPServicesHA epServicesHA;
    private final EPRuntimeSPI epRuntime;
    private final EventBeanService eventBeanService;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final EPRenderEventServiceImpl eventRenderer;
    private final EventTableIndexService eventTableIndexService;
    private final EventTypeAvroHandler eventTypeAvroHandler;
    private final EventTypeFactory eventTypeFactory;
    private final PathRegistry<String, EventType> eventTypePathRegistry;
    private final EventTypeRepositoryImpl eventTypeRepositoryBus;
    private final EventTypeResolvingBeanFactory eventTypeResolvingBeanFactory;
    private final ExceptionHandlingService exceptionHandlingService;
    private final ExpressionResultCacheService expressionResultCacheService;
    private final FilterBooleanExpressionFactory filterBooleanExpressionFactory;
    private final FilterServiceSPI filterService;
    private final FilterSharedBoolExprRepository filterSharedBoolExprRepository;
    private final FilterSharedLookupableRepository filterSharedLookupableRepository;
    private final HistoricalDataCacheFactory historicalDataCacheFactory;
    private final InternalEventRouterImpl internalEventRouter;
    private final MetricReportingService metricReportingService;
    private final MultiMatchHandlerFactory multiMatchHandlerFactory;
    private final NamedWindowConsumerManagementService namedWindowConsumerManagementService;
    private final NamedWindowDispatchService namedWindowDispatchService;
    private final NamedWindowFactoryService namedWindowFactoryService;
    private final NamedWindowManagementService namedWindowManagementService;
    private final PathRegistry<String, NamedWindowMetaData> namedWindowPathRegistry;
    private final PatternFactoryService patternFactoryService;
    private final PatternSubexpressionPoolRuntimeSvc patternSubexpressionPoolEngineSvc;
    private final ResultSetProcessorHelperFactory resultSetProcessorHelperFactory;
    private final RowRecogStateRepoFactory rowRecogStateRepoFactory;
    private final RowRecogStatePoolRuntimeSvc rowRecogStatePoolEngineSvc;
    private final SchedulingServiceSPI schedulingService;
    private final PathRegistry<NameAndParamNum, ExpressionScriptProvided> scriptPathRegistry;
    private final StatementLifecycleService statementLifecycleService;
    private final StatementAgentInstanceLockFactory statementAgentInstanceLockFactory;
    private final StatementResourceHolderBuilder statementResourceHolderBuilder;
    private final TableExprEvaluatorContext tableExprEvaluatorContext;
    private final TableManagementService tableManagementService;
    private final PathRegistry<String, TableMetaData> tablePathRegistry;
    private final ThreadingService threadingService;
    private final TimeAbacus timeAbacus;
    private final TimeSourceService timeSourceService;
    private final TimerService timerService;
    private final VariableManagementService variableManagementService;
    private final PathRegistry<String, VariableMetaData> variablePathRegistry;
    private final ViewableActivatorFactory viewableActivatorFactory;
    private final ViewFactoryService viewFactoryService;
    private final ViewServicePreviousFactory viewServicePreviousFactory;
    private final XMLFragmentEventTypeFactory xmlFragmentEventTypeFactory;

    private StatementContextRuntimeServices statementContextRuntimeServices;
    private InternalEventRouteDest internalEventRouteDest;

    public EPServicesContext(AggregationServiceFactoryService aggregationServiceFactoryService, BeanEventTypeFactoryPrivate beanEventTypeFactoryPrivate, BeanEventTypeStemService beanEventTypeStemService, ClassForNameProvider classForNameProvider, Configuration configSnapshot, ContextManagementService contextManagementService, PathRegistry<String, ContextMetaData> contextPathRegistry, ContextServiceFactory contextServiceFactory, EPDataFlowServiceImpl dataflowService, DataFlowFilterServiceAdapter dataFlowFilterServiceAdapter, DataInputOutputSerdeProvider dataInputOutputSerdeProvider, DatabaseConfigServiceRuntime databaseConfigServiceRuntime, DeploymentLifecycleService deploymentLifecycleService, DispatchService dispatchService, RuntimeEnvContext runtimeEnvContext, RuntimeSettingsService runtimeSettingsService, String runtimeURI, ClasspathImportServiceRuntime classpathImportServiceRuntime, EPStatementFactory epStatementFactory, PathRegistry<String, ExpressionDeclItem> exprDeclaredPathRegistry, ManagedReadWriteLock eventProcessingRWLock, EPServicesHA epServicesHA, EPRuntimeSPI epRuntime, EventBeanService eventBeanService, EventBeanTypedEventFactory eventBeanTypedEventFactory, EPRenderEventServiceImpl eventRenderer, EventTableIndexService eventTableIndexService, EventTypeAvroHandler eventTypeAvroHandler, EventTypeFactory eventTypeFactory, PathRegistry<String, EventType> eventTypePathRegistry, EventTypeRepositoryImpl eventTypeRepositoryBus, EventTypeResolvingBeanFactory eventTypeResolvingBeanFactory, ExceptionHandlingService exceptionHandlingService, ExpressionResultCacheService expressionResultCacheService, FilterBooleanExpressionFactory filterBooleanExpressionFactory, FilterServiceSPI filterService, FilterSharedBoolExprRepository filterSharedBoolExprRepository, FilterSharedLookupableRepository filterSharedLookupableRepository, HistoricalDataCacheFactory historicalDataCacheFactory, InternalEventRouterImpl internalEventRouter, MetricReportingService metricReportingService, MultiMatchHandlerFactory multiMatchHandlerFactory, NamedWindowConsumerManagementService namedWindowConsumerManagementService, NamedWindowDispatchService namedWindowDispatchService, NamedWindowFactoryService namedWindowFactoryService, NamedWindowManagementService namedWindowManagementService, PathRegistry<String, NamedWindowMetaData> namedWindowPathRegistry, PatternFactoryService patternFactoryService, PatternSubexpressionPoolRuntimeSvc patternSubexpressionPoolEngineSvc, ResultSetProcessorHelperFactory resultSetProcessorHelperFactory, RowRecogStateRepoFactory rowRecogStateRepoFactory, RowRecogStatePoolRuntimeSvc rowRecogStatePoolEngineSvc, SchedulingServiceSPI schedulingService, PathRegistry<NameAndParamNum, ExpressionScriptProvided> scriptPathRegistry, StatementLifecycleService statementLifecycleService, StatementAgentInstanceLockFactory statementAgentInstanceLockFactory, StatementResourceHolderBuilder statementResourceHolderBuilder, TableExprEvaluatorContext tableExprEvaluatorContext, TableManagementService tableManagementService, PathRegistry<String, TableMetaData> tablePathRegistry, ThreadingService threadingService, TimeAbacus timeAbacus, TimeSourceService timeSourceService, TimerService timerService, VariableManagementService variableManagementService, PathRegistry<String, VariableMetaData> variablePathRegistry, ViewableActivatorFactory viewableActivatorFactory, ViewFactoryService viewFactoryService, ViewServicePreviousFactory viewServicePreviousFactory, XMLFragmentEventTypeFactory xmlFragmentEventTypeFactory) {
        this.aggregationServiceFactoryService = aggregationServiceFactoryService;
        this.beanEventTypeFactoryPrivate = beanEventTypeFactoryPrivate;
        this.beanEventTypeStemService = beanEventTypeStemService;
        this.classForNameProvider = classForNameProvider;
        this.configSnapshot = configSnapshot;
        this.contextManagementService = contextManagementService;
        this.contextPathRegistry = contextPathRegistry;
        this.contextServiceFactory = contextServiceFactory;
        this.dataflowService = dataflowService;
        this.dataFlowFilterServiceAdapter = dataFlowFilterServiceAdapter;
        this.dataInputOutputSerdeProvider = dataInputOutputSerdeProvider;
        this.databaseConfigServiceRuntime = databaseConfigServiceRuntime;
        this.deploymentLifecycleService = deploymentLifecycleService;
        this.dispatchService = dispatchService;
        this.runtimeEnvContext = runtimeEnvContext;
        this.runtimeSettingsService = runtimeSettingsService;
        this.runtimeURI = runtimeURI;
        this.classpathImportServiceRuntime = classpathImportServiceRuntime;
        this.epStatementFactory = epStatementFactory;
        this.exprDeclaredPathRegistry = exprDeclaredPathRegistry;
        this.eventProcessingRWLock = eventProcessingRWLock;
        this.epServicesHA = epServicesHA;
        this.epRuntime = epRuntime;
        this.eventBeanService = eventBeanService;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.eventRenderer = eventRenderer;
        this.eventTableIndexService = eventTableIndexService;
        this.eventTypeAvroHandler = eventTypeAvroHandler;
        this.eventTypeFactory = eventTypeFactory;
        this.eventTypePathRegistry = eventTypePathRegistry;
        this.eventTypeRepositoryBus = eventTypeRepositoryBus;
        this.eventTypeResolvingBeanFactory = eventTypeResolvingBeanFactory;
        this.exceptionHandlingService = exceptionHandlingService;
        this.expressionResultCacheService = expressionResultCacheService;
        this.filterBooleanExpressionFactory = filterBooleanExpressionFactory;
        this.filterService = filterService;
        this.filterSharedBoolExprRepository = filterSharedBoolExprRepository;
        this.filterSharedLookupableRepository = filterSharedLookupableRepository;
        this.historicalDataCacheFactory = historicalDataCacheFactory;
        this.internalEventRouter = internalEventRouter;
        this.metricReportingService = metricReportingService;
        this.multiMatchHandlerFactory = multiMatchHandlerFactory;
        this.namedWindowConsumerManagementService = namedWindowConsumerManagementService;
        this.namedWindowDispatchService = namedWindowDispatchService;
        this.namedWindowFactoryService = namedWindowFactoryService;
        this.namedWindowManagementService = namedWindowManagementService;
        this.namedWindowPathRegistry = namedWindowPathRegistry;
        this.patternFactoryService = patternFactoryService;
        this.patternSubexpressionPoolEngineSvc = patternSubexpressionPoolEngineSvc;
        this.resultSetProcessorHelperFactory = resultSetProcessorHelperFactory;
        this.rowRecogStateRepoFactory = rowRecogStateRepoFactory;
        this.rowRecogStatePoolEngineSvc = rowRecogStatePoolEngineSvc;
        this.schedulingService = schedulingService;
        this.scriptPathRegistry = scriptPathRegistry;
        this.statementLifecycleService = statementLifecycleService;
        this.statementAgentInstanceLockFactory = statementAgentInstanceLockFactory;
        this.statementResourceHolderBuilder = statementResourceHolderBuilder;
        this.tableExprEvaluatorContext = tableExprEvaluatorContext;
        this.tableManagementService = tableManagementService;
        this.tablePathRegistry = tablePathRegistry;
        this.threadingService = threadingService;
        this.timeAbacus = timeAbacus;
        this.timeSourceService = timeSourceService;
        this.timerService = timerService;
        this.variableManagementService = variableManagementService;
        this.variablePathRegistry = variablePathRegistry;
        this.viewableActivatorFactory = viewableActivatorFactory;
        this.viewFactoryService = viewFactoryService;
        this.viewServicePreviousFactory = viewServicePreviousFactory;
        this.xmlFragmentEventTypeFactory = xmlFragmentEventTypeFactory;
    }

    public void destroy() {
        if (epServicesHA != null) {
            epServicesHA.destroy();
        }
    }

    public void initialize() {
    }

    public RuntimeExtensionServices getRuntimeExtensionServices() {
        return epServicesHA.getRuntimeExtensionServices();
    }

    public DeploymentRecoveryService getDeploymentRecoveryService() {
        return epServicesHA.getDeploymentRecoveryService();
    }

    public ListenerRecoveryService getListenerRecoveryService() {
        return epServicesHA.getListenerRecoveryService();
    }

    public void setInternalEventRouteDest(InternalEventRouteDest internalEventRouteDest) {
        this.internalEventRouteDest = internalEventRouteDest;
    }

    public StatementContextRuntimeServices getStatementContextRuntimeServices() {
        if (statementContextRuntimeServices == null) {
            statementContextRuntimeServices = new StatementContextRuntimeServices(
                    contextManagementService,
                    contextServiceFactory,
                    databaseConfigServiceRuntime,
                    dataFlowFilterServiceAdapter,
                    dataflowService,
                    runtimeURI,
                    runtimeEnvContext,
                    classpathImportServiceRuntime,
                    runtimeSettingsService,
                    epServicesHA.getRuntimeExtensionServices(),
                    epRuntime,
                    eventRenderer,
                    epRuntime.getEventServiceSPI(),
                    (EPEventServiceSPI) epRuntime.getEventService(),
                    eventBeanService,
                    eventBeanTypedEventFactory,
                    eventTableIndexService,
                    eventTypeAvroHandler,
                    eventTypePathRegistry,
                    eventTypeRepositoryBus,
                    eventTypeResolvingBeanFactory,
                    exceptionHandlingService,
                    expressionResultCacheService,
                    filterService,
                    filterBooleanExpressionFactory,
                    filterSharedBoolExprRepository,
                    filterSharedLookupableRepository,
                    historicalDataCacheFactory,
                    internalEventRouter,
                    internalEventRouteDest,
                    metricReportingService,
                    namedWindowConsumerManagementService,
                    namedWindowManagementService,
                    contextPathRegistry,
                    namedWindowPathRegistry,
                    rowRecogStateRepoFactory,
                    resultSetProcessorHelperFactory,
                    schedulingService,
                    statementAgentInstanceLockFactory,
                    statementResourceHolderBuilder,
                    tableExprEvaluatorContext,
                    tableManagementService,
                    variableManagementService,
                    viewFactoryService,
                    viewServicePreviousFactory);
        }
        return statementContextRuntimeServices;
    }

    public AggregationServiceFactoryService getAggregationServiceFactoryService() {
        return aggregationServiceFactoryService;
    }

    public BeanEventTypeFactoryPrivate getBeanEventTypeFactoryPrivate() {
        return beanEventTypeFactoryPrivate;
    }

    public BeanEventTypeStemService getBeanEventTypeStemService() {
        return beanEventTypeStemService;
    }

    public ClassForNameProvider getClassForNameProvider() {
        return classForNameProvider;
    }

    public Configuration getConfigSnapshot() {
        return configSnapshot;
    }

    public ContextManagementService getContextManagementService() {
        return contextManagementService;
    }

    public ContextServiceFactory getContextServiceFactory() {
        return contextServiceFactory;
    }

    public DatabaseConfigServiceRuntime getDatabaseConfigServiceRuntime() {
        return databaseConfigServiceRuntime;
    }

    public EPDataFlowServiceImpl getDataflowService() {
        return dataflowService;
    }

    public DataInputOutputSerdeProvider getDataInputOutputSerdeProvider() {
        return dataInputOutputSerdeProvider;
    }

    public DeploymentLifecycleService getDeploymentLifecycleService() {
        return deploymentLifecycleService;
    }

    public DispatchService getDispatchService() {
        return dispatchService;
    }

    public RuntimeEnvContext getRuntimeEnvContext() {
        return runtimeEnvContext;
    }

    public ClasspathImportServiceRuntime getClasspathImportServiceRuntime() {
        return classpathImportServiceRuntime;
    }

    public RuntimeSettingsService getRuntimeSettingsService() {
        return runtimeSettingsService;
    }

    public String getRuntimeURI() {
        return runtimeURI;
    }

    public EPStatementFactory getEpStatementFactory() {
        return epStatementFactory;
    }

    public ManagedReadWriteLock getEventProcessingRWLock() {
        return eventProcessingRWLock;
    }

    public EPServicesHA getEpServicesHA() {
        return epServicesHA;
    }

    public EPRuntime getEpRuntime() {
        return epRuntime;
    }

    public EventBeanService getEventBeanService() {
        return eventBeanService;
    }

    public EventBeanTypedEventFactory getEventBeanTypedEventFactory() {
        return eventBeanTypedEventFactory;
    }

    public EPRenderEventServiceImpl getEventRenderer() {
        return eventRenderer;
    }

    public EventTableIndexService getEventTableIndexService() {
        return eventTableIndexService;
    }

    public EventTypeAvroHandler getEventTypeAvroHandler() {
        return eventTypeAvroHandler;
    }

    public EventTypeFactory getEventTypeFactory() {
        return eventTypeFactory;
    }

    public EventTypeRepositoryImpl getEventTypeRepositoryBus() {
        return eventTypeRepositoryBus;
    }

    public EventTypeResolvingBeanFactory getEventTypeResolvingBeanFactory() {
        return eventTypeResolvingBeanFactory;
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return exceptionHandlingService;
    }

    public PathRegistry<String, ExpressionDeclItem> getExprDeclaredPathRegistry() {
        return exprDeclaredPathRegistry;
    }

    public FilterBooleanExpressionFactory getFilterBooleanExpressionFactory() {
        return filterBooleanExpressionFactory;
    }

    public FilterServiceSPI getFilterService() {
        return filterService;
    }

    public FilterSharedBoolExprRepository getFilterSharedBoolExprRepository() {
        return filterSharedBoolExprRepository;
    }

    public FilterSharedLookupableRepository getFilterSharedLookupableRepository() {
        return filterSharedLookupableRepository;
    }

    public InternalEventRouterImpl getInternalEventRouter() {
        return internalEventRouter;
    }

    public InternalEventRouteDest getInternalEventRouteDest() {
        return internalEventRouteDest;
    }

    public RowRecogStatePoolRuntimeSvc getRowRecogStatePoolEngineSvc() {
        return rowRecogStatePoolEngineSvc;
    }

    public MetricReportingService getMetricReportingService() {
        return metricReportingService;
    }

    public MultiMatchHandlerFactory getMultiMatchHandlerFactory() {
        return multiMatchHandlerFactory;
    }

    public NamedWindowConsumerManagementService getNamedWindowConsumerManagementService() {
        return namedWindowConsumerManagementService;
    }

    public NamedWindowDispatchService getNamedWindowDispatchService() {
        return namedWindowDispatchService;
    }

    public NamedWindowFactoryService getNamedWindowFactoryService() {
        return namedWindowFactoryService;
    }

    public NamedWindowManagementService getNamedWindowManagementService() {
        return namedWindowManagementService;
    }

    public PathRegistry<String, ContextMetaData> getContextPathRegistry() {
        return contextPathRegistry;
    }

    public PathRegistry<String, EventType> getEventTypePathRegistry() {
        return eventTypePathRegistry;
    }

    public PathRegistry<String, NamedWindowMetaData> getNamedWindowPathRegistry() {
        return namedWindowPathRegistry;
    }

    public PatternFactoryService getPatternFactoryService() {
        return patternFactoryService;
    }

    public PatternSubexpressionPoolRuntimeSvc getPatternSubexpressionPoolRuntimeSvc() {
        return patternSubexpressionPoolEngineSvc;
    }

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return resultSetProcessorHelperFactory;
    }

    public SchedulingServiceSPI getSchedulingService() {
        return schedulingService;
    }

    public PathRegistry<NameAndParamNum, ExpressionScriptProvided> getScriptPathRegistry() {
        return scriptPathRegistry;
    }

    public StatementLifecycleService getStatementLifecycleService() {
        return statementLifecycleService;
    }

    public StatementAgentInstanceLockFactory getStatementAgentInstanceLockFactory() {
        return statementAgentInstanceLockFactory;
    }

    public StatementResourceHolderBuilder getStatementResourceHolderBuilder() {
        return statementResourceHolderBuilder;
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        return tableExprEvaluatorContext;
    }

    public TableManagementService getTableManagementService() {
        return tableManagementService;
    }

    public PathRegistry<String, TableMetaData> getTablePathRegistry() {
        return tablePathRegistry;
    }

    public ThreadingService getThreadingService() {
        return threadingService;
    }

    public TimeAbacus getTimeAbacus() {
        return timeAbacus;
    }

    public TimerService getTimerService() {
        return timerService;
    }

    public TimeSourceService getTimeSourceService() {
        return timeSourceService;
    }

    public VariableManagementService getVariableManagementService() {
        return variableManagementService;
    }

    public PathRegistry<String, VariableMetaData> getVariablePathRegistry() {
        return variablePathRegistry;
    }

    public ViewableActivatorFactory getViewableActivatorFactory() {
        return viewableActivatorFactory;
    }

    public ViewFactoryService getViewFactoryService() {
        return viewFactoryService;
    }

    public ViewServicePreviousFactory getViewServicePreviousFactory() {
        return viewServicePreviousFactory;
    }

    public XMLFragmentEventTypeFactory getXmlFragmentEventTypeFactory() {
        return xmlFragmentEventTypeFactory;
    }
}
