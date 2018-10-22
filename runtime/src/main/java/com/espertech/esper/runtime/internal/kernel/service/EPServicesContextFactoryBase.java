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
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeMeta;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeConditionHandling;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeExceptionHandling;
import com.espertech.esper.common.client.hook.condition.ConditionHandler;
import com.espertech.esper.common.client.hook.condition.ConditionHandlerFactory;
import com.espertech.esper.common.client.hook.condition.ConditionHandlerFactoryContext;
import com.espertech.esper.common.client.hook.exception.ExceptionHandler;
import com.espertech.esper.common.client.hook.exception.ExceptionHandlerFactory;
import com.espertech.esper.common.client.hook.exception.ExceptionHandlerFactoryContext;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.client.util.ClassForNameProvider;
import com.espertech.esper.common.client.util.ClassForNameProviderDefault;
import com.espertech.esper.common.client.util.TimeSourceType;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorFactory;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.mgr.ContextManagementServiceImpl;
import com.espertech.esper.common.internal.context.mgr.ContextServiceFactory;
import com.espertech.esper.common.internal.context.module.RuntimeExtensionServices;
import com.espertech.esper.common.internal.context.util.InternalEventRouterImpl;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLockFactory;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLockFactoryImpl;
import com.espertech.esper.common.internal.context.util.StatementContextResolver;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryService;
import com.espertech.esper.common.internal.epl.dataflow.core.EPDataFlowServiceImpl;
import com.espertech.esper.common.internal.epl.dataflow.filtersvcadapter.DataFlowFilterServiceAdapter;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheService;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacusFactory;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConfigServiceImpl;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConfigServiceRuntime;
import com.espertech.esper.common.internal.epl.historical.datacache.HistoricalDataCacheFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableIndexService;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerManagementService;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowDispatchService;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowFactoryService;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowManagementService;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowManagementServiceImpl;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.pattern.core.PatternFactoryService;
import com.espertech.esper.common.internal.epl.pattern.pool.PatternSubexpressionPoolRuntimeSvc;
import com.espertech.esper.common.internal.epl.pattern.pool.PatternSubexpressionPoolRuntimeSvcImpl;
import com.espertech.esper.common.internal.epl.pattern.pool.PatternSubexpressionPoolRuntimeSvcNoOp;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStatePoolRuntimeSvc;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStateRepoFactory;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.TableManagementService;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;
import com.espertech.esper.common.internal.epl.variable.core.VariableRepositoryPreconfigured;
import com.espertech.esper.common.internal.epl.variable.core.VariableUtil;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.bean.core.BeanEventTypeRepoUtil;
import com.espertech.esper.common.internal.event.bean.core.BeanEventTypeStemService;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryPrivate;
import com.espertech.esper.common.internal.event.bean.service.EventTypeRepositoryBeanTypeUtil;
import com.espertech.esper.common.internal.event.core.EventBeanServiceImpl;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventTypeIdResolver;
import com.espertech.esper.common.internal.event.core.EventTypeResolvingBeanFactory;
import com.espertech.esper.common.internal.event.eventtypefactory.EventTypeFactory;
import com.espertech.esper.common.internal.event.eventtyperepo.*;
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
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.ManagedReadWriteLock;
import com.espertech.esper.common.internal.view.core.ViewFactoryService;
import com.espertech.esper.common.internal.view.previous.ViewServicePreviousFactory;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentLifecycleServiceImpl;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterServiceSPI;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementFactory;
import com.espertech.esper.runtime.internal.kernel.thread.ThreadingService;
import com.espertech.esper.runtime.internal.metrics.stmtmetrics.MetricReportingServiceImpl;
import com.espertech.esper.runtime.internal.schedulesvcimpl.SchedulingServiceSPI;
import com.espertech.esper.runtime.internal.statementlifesvc.StatementLifecycleServiceImpl;
import com.espertech.esper.runtime.internal.timer.TimeSourceServiceImpl;
import com.espertech.esper.runtime.internal.timer.TimerService;
import com.espertech.esper.runtime.internal.timer.TimerServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.espertech.esper.common.internal.context.util.StatementCPCacheService.DEFAULT_AGENT_INSTANCE_ID;

public abstract class EPServicesContextFactoryBase implements EPServicesContextFactory {
    private static final Logger log = LoggerFactory.getLogger(EPServicesContextFactoryDefault.class);

    protected abstract EPServicesHA initHA(String runtimeURI, Configuration configurationSnapshot, RuntimeEnvContext runtimeEnvContext, ManagedReadWriteLock eventProcessingRWLock, RuntimeSettingsService runtimeSettingsService);

    protected abstract ViewableActivatorFactory initViewableActivatorFactory();

    protected abstract FilterServiceSPI makeFilterService(RuntimeExtensionServices runtimeExt, EventTypeRepository eventTypeRepository, StatementLifecycleServiceImpl statementLifecycleService, RuntimeSettingsService runtimeSettingsService, EventTypeIdResolver eventTypeIdResolver, FilterSharedLookupableRepository filterSharedLookupableRepository);

    protected abstract StatementResourceHolderBuilder makeStatementResourceHolderBuilder();

    protected abstract RuntimeSettingsService makeRuntimeSettingsService(Configuration configurationSnapshot);

    protected abstract FilterSharedLookupableRepository makeFilterSharedLookupableRepository();

    protected abstract FilterSharedBoolExprRepository makeFilterSharedBoolExprRepository();

    protected abstract FilterBooleanExpressionFactory makeFilterBooleanExpressionFactory(StatementLifecycleServiceImpl statementLifecycleService);

    protected abstract AggregationServiceFactoryService makeAggregationServiceFactoryService(RuntimeExtensionServices runtimeExt);

    protected abstract ViewFactoryService makeViewFactoryService();

    protected abstract EventTypeFactory makeEventTypeFactory(RuntimeExtensionServices runtimeExt, EventTypeRepositoryImpl eventTypeRepositoryPreconfigured, DeploymentLifecycleServiceImpl deploymentLifecycleService);

    protected abstract EventTypeResolvingBeanFactory makeEventTypeResolvingBeanFactory(EventTypeRepository eventTypeRepository, EventTypeAvroHandler eventTypeAvroHandler);

    protected abstract PatternFactoryService makePatternFactoryService();

    protected abstract SchedulingServiceSPI makeSchedulingService(EPServicesHA epServicesHA, TimeSourceService timeSourceService, RuntimeExtensionServices runtimeExt, RuntimeSettingsService runtimeSettingsService, StatementContextResolver statementContextResolver);

    protected abstract MultiMatchHandlerFactory makeMultiMatchHandlerFactory(Configuration configurationInformation);

    protected abstract ContextServiceFactory makeContextServiceFactory(RuntimeExtensionServices runtimeExtensionServices);

    protected abstract ViewServicePreviousFactory makeViewServicePreviousFactory(RuntimeExtensionServices ext);

    protected abstract EPStatementFactory makeEPStatementFactory();

    protected abstract EventBeanTypedEventFactory makeEventBeanTypedEventFactory(EventTypeAvroHandler eventTypeAvroHandler);

    protected abstract EventTableIndexService makeEventTableIndexService(RuntimeExtensionServices ext);

    protected abstract DataInputOutputSerdeProvider makeSerdeProvider(RuntimeExtensionServices ext);

    protected abstract ResultSetProcessorHelperFactory makeResultSetProcessorHelperFactory(RuntimeExtensionServices ext);

    protected abstract NamedWindowDispatchService makeNamedWindowDispatchService(SchedulingServiceSPI schedulingService, Configuration configurationSnapshot, ManagedReadWriteLock eventProcessingRWLock, ExceptionHandlingService exceptionHandlingService, VariableManagementService variableManagementService, TableManagementService tableManagementService, MetricReportingService metricReportingService);

    protected abstract NamedWindowConsumerManagementService makeNamedWindowConsumerManagementService(NamedWindowManagementService namedWindowManagementService);

    protected abstract NamedWindowFactoryService makeNamedWindowFactoryService();

    protected abstract RowRecogStateRepoFactory makeRowRecogStateRepoFactory();

    protected abstract VariableManagementService makeVariableManagementService(Configuration configs, SchedulingServiceSPI schedulingService, EventBeanTypedEventFactory eventBeanTypedEventFactory, RuntimeSettingsService runtimeSettingsService, EPServicesHA epServicesHA);

    protected abstract TableManagementService makeTableManagementService(RuntimeExtensionServices runtimeExt, TableExprEvaluatorContext tableExprEvaluatorContext);

    protected abstract EventTypeAvroHandler makeEventTypeAvroHandler(ClasspathImportServiceRuntime classpathImportServiceRuntime, ConfigurationCommonEventTypeMeta.AvroSettings avroSettings, RuntimeExtensionServices runtimeExt);

    protected abstract HistoricalDataCacheFactory makeHistoricalDataCacheFactory(RuntimeExtensionServices runtimeExtensionServices);

    protected abstract DataFlowFilterServiceAdapter makeDataFlowFilterServiceAdapter();

    protected abstract ThreadingService makeThreadingService(Configuration configs);

    public EPServicesContext createServicesContext(EPRuntimeSPI epRuntime, Configuration configs) {

        RuntimeEnvContext runtimeEnvContext = new RuntimeEnvContext();
        ManagedReadWriteLock eventProcessingRWLock = new ManagedReadWriteLock("EventProcLock", false);
        DeploymentLifecycleServiceImpl deploymentLifecycleService = new DeploymentLifecycleServiceImpl();

        RuntimeSettingsService runtimeSettingsService = makeRuntimeSettingsService(configs);

        TimeAbacus timeAbacus = TimeAbacusFactory.make(configs.getCommon().getTimeSource().getTimeUnit());
        TimeZone timeZone = configs.getRuntime().getExpression().getTimeZone() == null ? TimeZone.getDefault() : configs.getRuntime().getExpression().getTimeZone();
        ClasspathImportServiceRuntime classpathImportServiceRuntime = new ClasspathImportServiceRuntime(configs.getCommon().getTransientConfiguration(), timeAbacus, configs.getCommon().getEventTypeAutoNamePackages(), timeZone, configs.getCommon().getMethodInvocationReferences(),
                configs.getCommon().getImports(), configs.getCommon().getAnnotationImports());

        EPServicesHA epServicesHA = initHA(epRuntime.getURI(), configs, runtimeEnvContext, eventProcessingRWLock, runtimeSettingsService);

        EventTypeAvroHandler eventTypeAvroHandler = makeEventTypeAvroHandler(classpathImportServiceRuntime, configs.getCommon().getEventMeta().getAvroSettings(), epServicesHA.getRuntimeExtensionServices());
        Map<String, Class> resolvedBeanEventTypes = BeanEventTypeRepoUtil.resolveBeanEventTypes(configs.getCommon().getEventTypeNames(), classpathImportServiceRuntime);
        EventBeanTypedEventFactory eventBeanTypedEventFactory = makeEventBeanTypedEventFactory(eventTypeAvroHandler);
        BeanEventTypeStemService beanEventTypeStemService = BeanEventTypeRepoUtil.makeBeanEventTypeStemService(configs, resolvedBeanEventTypes, eventBeanTypedEventFactory);
        EventTypeRepositoryImpl eventTypeRepositoryPreconfigured = new EventTypeRepositoryImpl(false);
        EventTypeFactory eventTypeFactory = makeEventTypeFactory(epServicesHA.getRuntimeExtensionServices(), eventTypeRepositoryPreconfigured, deploymentLifecycleService);
        BeanEventTypeFactoryPrivate beanEventTypeFactoryPrivate = new BeanEventTypeFactoryPrivate(eventBeanTypedEventFactory, eventTypeFactory, beanEventTypeStemService);
        EventTypeRepositoryBeanTypeUtil.buildBeanTypes(beanEventTypeStemService, eventTypeRepositoryPreconfigured, resolvedBeanEventTypes, beanEventTypeFactoryPrivate, configs.getCommon().getEventTypesBean());
        EventTypeRepositoryMapTypeUtil.buildMapTypes(eventTypeRepositoryPreconfigured, configs.getCommon().getMapTypeConfigurations(), configs.getCommon().getEventTypesMapEvents(), configs.getCommon().getEventTypesNestableMapEvents(), beanEventTypeFactoryPrivate, classpathImportServiceRuntime);
        EventTypeRepositoryOATypeUtil.buildOATypes(eventTypeRepositoryPreconfigured, configs.getCommon().getObjectArrayTypeConfigurations(), configs.getCommon().getEventTypesNestableObjectArrayEvents(), beanEventTypeFactoryPrivate, classpathImportServiceRuntime);
        XMLFragmentEventTypeFactory xmlFragmentEventTypeFactory = new XMLFragmentEventTypeFactory(beanEventTypeFactoryPrivate, null, eventTypeRepositoryPreconfigured);
        EventTypeRepositoryXMLTypeUtil.buildXMLTypes(eventTypeRepositoryPreconfigured, configs.getCommon().getEventTypesXMLDOM(), beanEventTypeFactoryPrivate, xmlFragmentEventTypeFactory, classpathImportServiceRuntime);
        EventTypeRepositoryAvroTypeUtil.buildAvroTypes(eventTypeRepositoryPreconfigured, configs.getCommon().getEventTypesAvro(), eventTypeAvroHandler, beanEventTypeFactoryPrivate.getEventBeanTypedEventFactory());
        EventTypeRepositoryVariantStreamUtil.buildVariantStreams(eventTypeRepositoryPreconfigured, configs.getCommon().getVariantStreams(), eventTypeFactory);

        EventTypeResolvingBeanFactory eventTypeResolvingBeanFactory = makeEventTypeResolvingBeanFactory(eventTypeRepositoryPreconfigured, eventTypeAvroHandler);

        ViewableActivatorFactory viewableActivatorFactory = initViewableActivatorFactory();

        StatementLifecycleServiceImpl statementLifecycleService = new StatementLifecycleServiceImpl();

        EventTypeIdResolver idResolver = new EventTypeIdResolver() {
            public EventType getTypeById(long eventTypeIdPublic, long eventTypeIdProtected) {
                if (eventTypeIdProtected == -1) {
                    return eventTypeRepositoryPreconfigured.getTypeById(eventTypeIdPublic);
                }
                DeploymentInternal deployerResult = deploymentLifecycleService.getDeploymentByCRC(eventTypeIdPublic);
                return deployerResult.getDeploymentTypes().get(eventTypeIdProtected);
            }
        };
        FilterSharedBoolExprRepository filterSharedBoolExprRepository = makeFilterSharedBoolExprRepository();
        FilterSharedLookupableRepository filterSharedLookupableRepository = makeFilterSharedLookupableRepository();
        FilterServiceSPI filterServiceSPI = makeFilterService(epServicesHA.getRuntimeExtensionServices(), eventTypeRepositoryPreconfigured, statementLifecycleService, runtimeSettingsService, idResolver, filterSharedLookupableRepository);
        FilterBooleanExpressionFactory filterBooleanExpressionFactory = makeFilterBooleanExpressionFactory(statementLifecycleService);

        StatementResourceHolderBuilder statementResourceHolderBuilder = makeStatementResourceHolderBuilder();

        AggregationServiceFactoryService aggregationServiceFactoryService = makeAggregationServiceFactoryService(epServicesHA.getRuntimeExtensionServices());

        ViewFactoryService viewFactoryService = makeViewFactoryService();
        PatternFactoryService patternFactoryService = makePatternFactoryService();

        ExceptionHandlingService exceptionHandlingService = initExceptionHandling(epRuntime.getURI(), configs.getRuntime().getExceptionHandling(), configs.getRuntime().getConditionHandling(), ClassForNameProviderDefault.INSTANCE);

        TimeSourceService timeSourceService = makeTimeSource(configs);
        SchedulingServiceSPI schedulingService = makeSchedulingService(epServicesHA, timeSourceService, epServicesHA.getRuntimeExtensionServices(), runtimeSettingsService, statementLifecycleService);

        InternalEventRouterImpl internalEventRouter = new InternalEventRouterImpl(eventBeanTypedEventFactory);

        MultiMatchHandlerFactory multiMatchHandlerFactory = makeMultiMatchHandlerFactory(configs);

        DispatchService dispatchService = new DispatchService();
        ContextServiceFactory contextServiceFactory = makeContextServiceFactory(epServicesHA.getRuntimeExtensionServices());
        ContextManagementService contextManagementService = new ContextManagementServiceImpl();

        ViewServicePreviousFactory viewServicePreviousFactory = makeViewServicePreviousFactory(epServicesHA.getRuntimeExtensionServices());

        DataInputOutputSerdeProvider dataInputOutputSerdeProvider = makeSerdeProvider(epServicesHA.getRuntimeExtensionServices());

        EPStatementFactory epStatementFactory = makeEPStatementFactory();

        long msecTimerResolution = configs.getRuntime().getThreading().getInternalTimerMsecResolution();
        if (msecTimerResolution <= 0) {
            throw new ConfigurationException("Timer resolution configuration not set to a valid value, expecting a non-zero value");
        }
        TimerService timerService = new TimerServiceImpl(epRuntime.getURI(), msecTimerResolution);

        StatementAgentInstanceLockFactory statementAgentInstanceLockFactory = new StatementAgentInstanceLockFactoryImpl(configs.getRuntime().getExecution().isFairlock(), configs.getRuntime().getExecution().isDisableLocking());

        EventTableIndexService eventTableIndexService = makeEventTableIndexService(epServicesHA.getRuntimeExtensionServices());
        ExpressionResultCacheService expressionResultCacheSharable = new ExpressionResultCacheService(configs.getRuntime().getExecution().getDeclaredExprValueCacheSize());

        ResultSetProcessorHelperFactory resultSetProcessorHelperFactory = makeResultSetProcessorHelperFactory(epServicesHA.getRuntimeExtensionServices());

        VariableRepositoryPreconfigured variableRepositoryPreconfigured = new VariableRepositoryPreconfigured();
        VariableUtil.configureVariables(variableRepositoryPreconfigured, configs.getCommon().getVariables(), classpathImportServiceRuntime, eventBeanTypedEventFactory, eventTypeRepositoryPreconfigured, beanEventTypeFactoryPrivate);
        VariableManagementService variableManagementService = makeVariableManagementService(configs, schedulingService, eventBeanTypedEventFactory, runtimeSettingsService, epServicesHA);
        for (Map.Entry<String, VariableMetaData> publicVariable : variableRepositoryPreconfigured.getMetadata().entrySet()) {
            variableManagementService.addVariable(null, publicVariable.getValue(), null);
            variableManagementService.allocateVariableState(null, publicVariable.getKey(), DEFAULT_AGENT_INSTANCE_ID, false, null, eventBeanTypedEventFactory);
        }
        PathRegistry<String, VariableMetaData> variablePathRegistry = new PathRegistry<>(PathRegistryObjectType.VARIABLE);

        TableExprEvaluatorContext tableExprEvaluatorContext = new TableExprEvaluatorContext();
        TableManagementService tableManagementService = makeTableManagementService(epServicesHA.getRuntimeExtensionServices(), tableExprEvaluatorContext);
        PathRegistry<String, TableMetaData> tablePathRegistry = new PathRegistry<>(PathRegistryObjectType.TABLE);

        MetricReportingServiceImpl metricsReporting = new MetricReportingServiceImpl(configs.getRuntime().getMetricsReporting(), epRuntime.getURI());

        NamedWindowFactoryService namedWindowFactoryService = makeNamedWindowFactoryService();
        NamedWindowDispatchService namedWindowDispatchService = makeNamedWindowDispatchService(schedulingService, configs, eventProcessingRWLock, exceptionHandlingService, variableManagementService, tableManagementService, metricsReporting);
        NamedWindowManagementService namedWindowManagementService = new NamedWindowManagementServiceImpl();
        NamedWindowConsumerManagementService namedWindowConsumerManagementService = makeNamedWindowConsumerManagementService(namedWindowManagementService);

        PathRegistry<String, NamedWindowMetaData> pathNamedWindowRegistry = new PathRegistry<>(PathRegistryObjectType.NAMEDWINDOW);
        PathRegistry<String, EventType> eventTypePathRegistry = new PathRegistry<>(PathRegistryObjectType.EVENTTYPE);
        PathRegistry<String, ContextMetaData> pathContextRegistry = new PathRegistry<>(PathRegistryObjectType.CONTEXT);
        EventBeanService eventBeanService = new EventBeanServiceImpl(eventTypeRepositoryPreconfigured, eventTypePathRegistry, eventBeanTypedEventFactory);

        PatternSubexpressionPoolRuntimeSvc patternSubexpressionPoolSvc;
        if (configs.getRuntime().getPatterns().getMaxSubexpressions() != null) {
            patternSubexpressionPoolSvc = new PatternSubexpressionPoolRuntimeSvcImpl(configs.getRuntime().getPatterns().getMaxSubexpressions(),
                    configs.getRuntime().getPatterns().isMaxSubexpressionPreventStart());
        } else {
            patternSubexpressionPoolSvc = PatternSubexpressionPoolRuntimeSvcNoOp.INSTANCE;
        }

        PathRegistry<String, ExpressionDeclItem> exprDeclaredPathRegistry = new PathRegistry<>(PathRegistryObjectType.EXPRDECL);
        PathRegistry<NameAndParamNum, ExpressionScriptProvided> scriptPathRegistry = new PathRegistry<>(PathRegistryObjectType.SCRIPT);

        RowRecogStatePoolRuntimeSvc rowRecogStatePoolEngineSvc = null;
        if (configs.getRuntime().getMatchRecognize().getMaxStates() != null) {
            rowRecogStatePoolEngineSvc = new RowRecogStatePoolRuntimeSvc(configs.getRuntime().getMatchRecognize().getMaxStates(),
                    configs.getRuntime().getMatchRecognize().isMaxStatesPreventStart());
        }

        RowRecogStateRepoFactory rowRecogStateRepoFactory = makeRowRecogStateRepoFactory();

        DatabaseConfigServiceRuntime databaseConfigServiceRuntime = new DatabaseConfigServiceImpl(configs.getCommon().getDatabaseReferences(), classpathImportServiceRuntime);
        HistoricalDataCacheFactory historicalDataCacheFactory = makeHistoricalDataCacheFactory(epServicesHA.getRuntimeExtensionServices());

        EPDataFlowServiceImpl dataflowService = new EPDataFlowServiceImpl();
        DataFlowFilterServiceAdapter dataFlowFilterServiceAdapter = makeDataFlowFilterServiceAdapter();

        ThreadingService threadingService = makeThreadingService(configs);
        EPRenderEventServiceImpl eventRenderer = new EPRenderEventServiceImpl();

        return new EPServicesContext(aggregationServiceFactoryService,
                beanEventTypeFactoryPrivate,
                beanEventTypeStemService,
                ClassForNameProviderDefault.INSTANCE,
                configs,
                contextManagementService,
                pathContextRegistry,
                contextServiceFactory,
                dataflowService,
                dataFlowFilterServiceAdapter,
                dataInputOutputSerdeProvider,
                databaseConfigServiceRuntime,
                deploymentLifecycleService,
                dispatchService,
                runtimeEnvContext,
                runtimeSettingsService,
                epRuntime.getURI(),
                classpathImportServiceRuntime,
                epStatementFactory,
                exprDeclaredPathRegistry,
                eventProcessingRWLock,
                epServicesHA,
                epRuntime,
                eventBeanService,
                eventBeanTypedEventFactory,
                eventRenderer,
                eventTableIndexService,
                eventTypeAvroHandler,
                eventTypeFactory,
                eventTypePathRegistry,
                eventTypeRepositoryPreconfigured,
                eventTypeResolvingBeanFactory,
                exceptionHandlingService,
                expressionResultCacheSharable,
                filterBooleanExpressionFactory,
                filterServiceSPI,
                filterSharedBoolExprRepository,
                filterSharedLookupableRepository,
                historicalDataCacheFactory,
                internalEventRouter,
                metricsReporting,
                multiMatchHandlerFactory,
                namedWindowConsumerManagementService,
                namedWindowDispatchService,
                namedWindowFactoryService,
                namedWindowManagementService,
                pathNamedWindowRegistry,
                patternFactoryService,
                patternSubexpressionPoolSvc,
                resultSetProcessorHelperFactory,
                rowRecogStateRepoFactory,
                rowRecogStatePoolEngineSvc,
                schedulingService,
                scriptPathRegistry,
                statementLifecycleService,
                statementAgentInstanceLockFactory,
                statementResourceHolderBuilder,
                tableExprEvaluatorContext,
                tableManagementService,
                tablePathRegistry,
                threadingService,
                timeAbacus,
                timeSourceService,
                timerService,
                variableManagementService,
                variablePathRegistry,
                viewableActivatorFactory,
                viewFactoryService,
                viewServicePreviousFactory,
                xmlFragmentEventTypeFactory
        );
    }

    protected static ExceptionHandlingService initExceptionHandling(String runtimeURI, ConfigurationRuntimeExceptionHandling exceptionHandling,
                                                                    ConfigurationRuntimeConditionHandling conditionHandling,
                                                                    ClassForNameProvider classForNameProvider)
            throws ConfigurationException {
        List<ExceptionHandler> exceptionHandlers;
        if (exceptionHandling.getHandlerFactories() == null || exceptionHandling.getHandlerFactories().isEmpty()) {
            exceptionHandlers = Collections.emptyList();
        } else {
            exceptionHandlers = new ArrayList<ExceptionHandler>();
            ExceptionHandlerFactoryContext context = new ExceptionHandlerFactoryContext(runtimeURI);
            for (String className : exceptionHandling.getHandlerFactories()) {
                try {
                    ExceptionHandlerFactory factory = (ExceptionHandlerFactory) JavaClassHelper.instantiate(ExceptionHandlerFactory.class, className, classForNameProvider);
                    ExceptionHandler handler = factory.getHandler(context);
                    if (handler == null) {
                        log.warn("Exception handler factory '" + className + "' returned a null handler, skipping factory");
                        continue;
                    }
                    exceptionHandlers.add(handler);
                } catch (RuntimeException ex) {
                    throw new ConfigurationException("Exception initializing exception handler from exception handler factory '" + className + "': " + ex.getMessage(), ex);
                }
            }
        }

        List<ConditionHandler> conditionHandlers;
        if (conditionHandling.getHandlerFactories() == null || conditionHandling.getHandlerFactories().isEmpty()) {
            conditionHandlers = Collections.emptyList();
        } else {
            conditionHandlers = new ArrayList<ConditionHandler>();
            ConditionHandlerFactoryContext context = new ConditionHandlerFactoryContext(runtimeURI);
            for (String className : conditionHandling.getHandlerFactories()) {
                try {
                    ConditionHandlerFactory factory = (ConditionHandlerFactory) JavaClassHelper.instantiate(ConditionHandlerFactory.class, className, classForNameProvider);
                    ConditionHandler handler = factory.getHandler(context);
                    if (handler == null) {
                        log.warn("Condition handler factory '" + className + "' returned a null handler, skipping factory");
                        continue;
                    }
                    conditionHandlers.add(handler);
                } catch (RuntimeException ex) {
                    throw new ConfigurationException("Exception initializing exception handler from exception handler factory '" + className + "': " + ex.getMessage(), ex);
                }
            }
        }
        return new ExceptionHandlingService(runtimeURI, exceptionHandlers, conditionHandlers);
    }

    private static TimeSourceService makeTimeSource(Configuration configSnapshot) {
        if (configSnapshot.getRuntime().getTimeSource().getTimeSourceType() == TimeSourceType.NANO) {
            // this is a static variable to keep overhead down for getting a current time
            TimeSourceServiceImpl.isSystemCurrentTime = false;
        }
        return new TimeSourceServiceImpl();
    }
}