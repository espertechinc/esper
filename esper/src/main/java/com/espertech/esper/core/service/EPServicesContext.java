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
package com.espertech.esper.core.service;

import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.core.context.activator.ViewableActivatorFactory;
import com.espertech.esper.core.context.mgr.ContextControllerFactoryFactorySvc;
import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.core.context.mgr.ContextManagerFactoryService;
import com.espertech.esper.core.deploy.DeploymentStateService;
import com.espertech.esper.core.service.multimatch.MultiMatchHandlerFactory;
import com.espertech.esper.core.thread.ThreadingService;
import com.espertech.esper.dataflow.core.DataFlowService;
import com.espertech.esper.dispatch.DispatchService;
import com.espertech.esper.dispatch.DispatchServiceProvider;
import com.espertech.esper.epl.agg.factory.AggregationFactoryFactory;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.engineimport.EngineSettingsService;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.epl.db.DataCacheFactory;
import com.espertech.esper.epl.db.DatabaseConfigService;
import com.espertech.esper.epl.declexpr.ExprDeclaredService;
import com.espertech.esper.epl.lookup.EventTableIndexService;
import com.espertech.esper.epl.metric.MetricReportingServiceSPI;
import com.espertech.esper.epl.named.NamedWindowConsumerMgmtService;
import com.espertech.esper.epl.named.NamedWindowDispatchService;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.spec.PluggableObjectCollection;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeIdGenerator;
import com.espertech.esper.event.vaevent.ValueAddEventService;
import com.espertech.esper.filterspec.FilterBooleanExpressionFactory;
import com.espertech.esper.filter.FilterNonPropertyRegisteryService;
import com.espertech.esper.filter.FilterServiceSPI;
import com.espertech.esper.pattern.PatternNodeFactory;
import com.espertech.esper.pattern.pool.PatternSubexpressionPoolEngineSvc;
import com.espertech.esper.rowregex.MatchRecognizeStatePoolEngineSvc;
import com.espertech.esper.rowregex.RegexHandlerFactory;
import com.espertech.esper.schedule.SchedulingMgmtService;
import com.espertech.esper.schedule.SchedulingServiceSPI;
import com.espertech.esper.timer.TimeSourceService;
import com.espertech.esper.timer.TimerService;
import com.espertech.esper.util.ManagedReadWriteLock;
import com.espertech.esper.view.ViewService;
import com.espertech.esper.view.ViewServicePreviousFactory;
import com.espertech.esper.view.ViewServiceProvider;
import com.espertech.esper.view.stream.StreamFactoryService;

/**
 * Convenience class to hold implementations for all services.
 */
public final class EPServicesContext {
    private String engineURI;
    private FilterServiceSPI filterService;
    private TimerService timerService;
    private SchedulingServiceSPI schedulingService;
    private DispatchService dispatchService;
    private ViewService viewService;
    private StreamFactoryService streamFactoryService;
    private EventAdapterService eventAdapterService;
    private EngineImportService engineImportService;
    private EngineSettingsService engineSettingsService;
    private DatabaseConfigService databaseConfigService;
    private PluggableObjectCollection plugInViews;
    private StatementLockFactory statementLockFactory;
    private ManagedReadWriteLock eventProcessingRWLock;
    private EngineLevelExtensionServicesContext engineLevelExtensionServicesContext;
    private EngineEnvContext engineEnvContext;
    private StatementContextFactory statementContextFactory;
    private PluggableObjectCollection plugInPatternObjects;
    private NamedWindowMgmtService namedWindowMgmtService;
    private NamedWindowDispatchService namedWindowDispatchService;
    private VariableService variableService;
    private TimeSourceService timeSourceService;
    private ValueAddEventService valueAddEventService;
    private MetricReportingServiceSPI metricsReportingService;
    private StatementEventTypeRef statementEventTypeRef;
    private StatementVariableRef statementVariableRef;
    private ConfigurationInformation configSnapshot;
    private ThreadingService threadingService;
    private InternalEventRouteDest internalEventEngineRouteDest;
    private StatementIsolationService statementIsolationService;
    private SchedulingMgmtService schedulingMgmtService;
    private DeploymentStateService deploymentStateService;
    private ExceptionHandlingService exceptionHandlingService;
    private PatternNodeFactory patternNodeFactory;
    private StatementMetadataFactory statementMetadataFactory;
    private ContextManagementService contextManagementService;
    private PatternSubexpressionPoolEngineSvc patternSubexpressionPoolSvc;
    private MatchRecognizeStatePoolEngineSvc matchRecognizeStatePoolEngineSvc;
    private TableService tableService;
    private ContextControllerFactoryFactorySvc contextControllerFactoryFactorySvc;
    private EPStatementFactory epStatementFactory;
    private ContextManagerFactoryService contextManagerFactoryService;
    private RegexHandlerFactory regexHandlerFactory;
    private ViewableActivatorFactory viewableActivatorFactory;
    private FilterNonPropertyRegisteryService filterNonPropertyRegisteryService;
    private ResultSetProcessorHelperFactory resultSetProcessorHelperFactory;
    private ViewServicePreviousFactory viewServicePreviousFactory;
    private EventTableIndexService eventTableIndexService;
    private EPRuntimeIsolatedFactory epRuntimeIsolatedFactory;
    private FilterBooleanExpressionFactory filterBooleanExpressionFactory;
    private DataCacheFactory dataCacheFactory;
    private MultiMatchHandlerFactory multiMatchHandlerFactory;
    private NamedWindowConsumerMgmtService namedWindowConsumerMgmtService;
    private AggregationFactoryFactory aggregationFactoryFactory;

    // Supplied after construction to avoid circular dependency
    private StatementLifecycleSvc statementLifecycleSvc;
    private InternalEventRouterImpl internalEventRouter;
    private EventTypeIdGenerator eventTypeIdGenerator;

    private DataFlowService dataFlowService;
    private ExprDeclaredService exprDeclaredService;
    private ExpressionResultCacheService expressionResultCacheSharable;

    public EPServicesContext(String engineURI,
                             SchedulingServiceSPI schedulingService,
                             EventAdapterService eventAdapterService,
                             EngineImportService engineImportService,
                             EngineSettingsService engineSettingsService,
                             DatabaseConfigService databaseConfigService,
                             PluggableObjectCollection plugInViews,
                             StatementLockFactory statementLockFactory,
                             ManagedReadWriteLock eventProcessingRWLock,
                             EngineLevelExtensionServicesContext engineLevelExtensionServicesContext,
                             EngineEnvContext engineEnvContext,
                             StatementContextFactory statementContextFactory,
                             PluggableObjectCollection plugInPatternObjects,
                             TimerService timerService,
                             FilterServiceSPI filterService,
                             StreamFactoryService streamFactoryService,
                             NamedWindowMgmtService namedWindowMgmtService,
                             NamedWindowDispatchService namedWindowDispatchService,
                             VariableService variableService,
                             TableService tableService,
                             TimeSourceService timeSourceService,
                             ValueAddEventService valueAddEventService,
                             MetricReportingServiceSPI metricsReportingService,
                             StatementEventTypeRef statementEventTypeRef,
                             StatementVariableRef statementVariableRef,
                             ConfigurationInformation configSnapshot,
                             ThreadingService threadingServiceImpl,
                             InternalEventRouterImpl internalEventRouter,
                             StatementIsolationService statementIsolationService,
                             SchedulingMgmtService schedulingMgmtService,
                             DeploymentStateService deploymentStateService,
                             ExceptionHandlingService exceptionHandlingService,
                             PatternNodeFactory patternNodeFactory,
                             EventTypeIdGenerator eventTypeIdGenerator,
                             StatementMetadataFactory statementMetadataFactory,
                             ContextManagementService contextManagementService,
                             PatternSubexpressionPoolEngineSvc patternSubexpressionPoolSvc,
                             MatchRecognizeStatePoolEngineSvc matchRecognizeStatePoolEngineSvc,
                             DataFlowService dataFlowService,
                             ExprDeclaredService exprDeclaredService,
                             ContextControllerFactoryFactorySvc contextControllerFactoryFactorySvc,
                             ContextManagerFactoryService contextManagerFactoryService,
                             EPStatementFactory epStatementFactory,
                             RegexHandlerFactory regexHandlerFactory,
                             ViewableActivatorFactory viewableActivatorFactory,
                             FilterNonPropertyRegisteryService filterNonPropertyRegisteryService,
                             ResultSetProcessorHelperFactory resultSetProcessorHelperFactory,
                             ViewServicePreviousFactory viewServicePreviousFactory,
                             EventTableIndexService eventTableIndexService,
                             EPRuntimeIsolatedFactory epRuntimeIsolatedFactory,
                             FilterBooleanExpressionFactory filterBooleanExpressionFactory,
                             DataCacheFactory dataCacheFactory,
                             MultiMatchHandlerFactory multiMatchHandlerFactory,
                             NamedWindowConsumerMgmtService namedWindowConsumerMgmtService,
                             AggregationFactoryFactory aggregationFactoryFactory) {
        this.engineURI = engineURI;
        this.schedulingService = schedulingService;
        this.eventAdapterService = eventAdapterService;
        this.engineImportService = engineImportService;
        this.engineSettingsService = engineSettingsService;
        this.databaseConfigService = databaseConfigService;
        this.filterService = filterService;
        this.timerService = timerService;
        this.dispatchService = DispatchServiceProvider.newService();
        this.viewService = ViewServiceProvider.newService();
        this.streamFactoryService = streamFactoryService;
        this.plugInViews = plugInViews;
        this.statementLockFactory = statementLockFactory;
        this.eventProcessingRWLock = eventProcessingRWLock;
        this.engineLevelExtensionServicesContext = engineLevelExtensionServicesContext;
        this.engineEnvContext = engineEnvContext;
        this.statementContextFactory = statementContextFactory;
        this.plugInPatternObjects = plugInPatternObjects;
        this.namedWindowMgmtService = namedWindowMgmtService;
        this.namedWindowDispatchService = namedWindowDispatchService;
        this.variableService = variableService;
        this.tableService = tableService;
        this.timeSourceService = timeSourceService;
        this.valueAddEventService = valueAddEventService;
        this.metricsReportingService = metricsReportingService;
        this.statementEventTypeRef = statementEventTypeRef;
        this.configSnapshot = configSnapshot;
        this.threadingService = threadingServiceImpl;
        this.internalEventRouter = internalEventRouter;
        this.statementIsolationService = statementIsolationService;
        this.schedulingMgmtService = schedulingMgmtService;
        this.statementVariableRef = statementVariableRef;
        this.deploymentStateService = deploymentStateService;
        this.exceptionHandlingService = exceptionHandlingService;
        this.patternNodeFactory = patternNodeFactory;
        this.eventTypeIdGenerator = eventTypeIdGenerator;
        this.statementMetadataFactory = statementMetadataFactory;
        this.contextManagementService = contextManagementService;
        this.patternSubexpressionPoolSvc = patternSubexpressionPoolSvc;
        this.matchRecognizeStatePoolEngineSvc = matchRecognizeStatePoolEngineSvc;
        this.dataFlowService = dataFlowService;
        this.exprDeclaredService = exprDeclaredService;
        this.expressionResultCacheSharable = new ExpressionResultCacheService(configSnapshot.getEngineDefaults().getExecution().getDeclaredExprValueCacheSize());
        this.contextControllerFactoryFactorySvc = contextControllerFactoryFactorySvc;
        this.contextManagerFactoryService = contextManagerFactoryService;
        this.epStatementFactory = epStatementFactory;
        this.regexHandlerFactory = regexHandlerFactory;
        this.viewableActivatorFactory = viewableActivatorFactory;
        this.filterNonPropertyRegisteryService = filterNonPropertyRegisteryService;
        this.resultSetProcessorHelperFactory = resultSetProcessorHelperFactory;
        this.viewServicePreviousFactory = viewServicePreviousFactory;
        this.eventTableIndexService = eventTableIndexService;
        this.epRuntimeIsolatedFactory = epRuntimeIsolatedFactory;
        this.filterBooleanExpressionFactory = filterBooleanExpressionFactory;
        this.dataCacheFactory = dataCacheFactory;
        this.multiMatchHandlerFactory = multiMatchHandlerFactory;
        this.namedWindowConsumerMgmtService = namedWindowConsumerMgmtService;
        this.aggregationFactoryFactory = aggregationFactoryFactory;
    }

    public PatternNodeFactory getPatternNodeFactory() {
        return patternNodeFactory;
    }

    /**
     * Sets the service dealing with starting and stopping statements.
     *
     * @param statementLifecycleSvc statement lifycycle svc
     */
    public void setStatementLifecycleSvc(StatementLifecycleSvc statementLifecycleSvc) {
        this.statementLifecycleSvc = statementLifecycleSvc;
    }

    /**
     * Returns the event routing destination.
     *
     * @return event routing destination
     */
    public InternalEventRouteDest getInternalEventEngineRouteDest() {
        return internalEventEngineRouteDest;
    }

    /**
     * Sets the event routing destination.
     *
     * @param internalEventEngineRouteDest event routing destination
     */
    public void setInternalEventEngineRouteDest(InternalEventRouteDest internalEventEngineRouteDest) {
        this.internalEventEngineRouteDest = internalEventEngineRouteDest;
    }

    /**
     * Returns router for internal event processing.
     *
     * @return router for internal event processing
     */
    public InternalEventRouterImpl getInternalEventRouter() {
        return internalEventRouter;
    }

    /**
     * Returns filter evaluation service implementation.
     *
     * @return filter evaluation service
     */
    public final FilterServiceSPI getFilterService() {
        return filterService;
    }

    /**
     * Returns time provider service implementation.
     *
     * @return time provider service
     */
    public final TimerService getTimerService() {
        return timerService;
    }

    /**
     * Returns scheduling service implementation.
     *
     * @return scheduling service
     */
    public final SchedulingServiceSPI getSchedulingService() {
        return schedulingService;
    }

    /**
     * Returns dispatch service responsible for dispatching events to listeners.
     *
     * @return dispatch service.
     */
    public DispatchService getDispatchService() {
        return dispatchService;
    }

    /**
     * Returns services for view creation, sharing and removal.
     *
     * @return view service
     */
    public ViewService getViewService() {
        return viewService;
    }

    /**
     * Returns stream service.
     *
     * @return stream service
     */
    public StreamFactoryService getStreamService() {
        return streamFactoryService;
    }

    /**
     * Returns event type resolution service.
     *
     * @return service resolving event type
     */
    public EventAdapterService getEventAdapterService() {
        return eventAdapterService;
    }

    /**
     * Returns the import and class name resolution service.
     *
     * @return import service
     */
    public EngineImportService getEngineImportService() {
        return engineImportService;
    }

    /**
     * Returns the database settings service.
     *
     * @return database info service
     */
    public DatabaseConfigService getDatabaseRefService() {
        return databaseConfigService;
    }

    /**
     * Information to resolve plug-in view namespace and name.
     *
     * @return plug-in view information
     */
    public PluggableObjectCollection getPlugInViews() {
        return plugInViews;
    }

    /**
     * Information to resolve plug-in pattern object namespace and name.
     *
     * @return plug-in pattern object information
     */
    public PluggableObjectCollection getPlugInPatternObjects() {
        return plugInPatternObjects;
    }

    /**
     * Factory for statement-level locks.
     *
     * @return factory
     */
    public StatementLockFactory getStatementLockFactory() {
        return statementLockFactory;
    }

    /**
     * Returns the event processing lock for coordinating statement administration with event processing.
     *
     * @return lock
     */
    public ManagedReadWriteLock getEventProcessingRWLock() {
        return eventProcessingRWLock;
    }

    /**
     * Returns statement lifecycle svc
     *
     * @return service for statement start and stop
     */
    public StatementLifecycleSvc getStatementLifecycleSvc() {
        return statementLifecycleSvc;
    }

    /**
     * Returns extension service for adding custom the services.
     *
     * @return extension service context
     */
    public EngineLevelExtensionServicesContext getEngineLevelExtensionServicesContext() {
        return engineLevelExtensionServicesContext;
    }

    /**
     * Returns the engine environment context for getting access to engine-external resources, such as adapters
     *
     * @return engine environment context
     */
    public EngineEnvContext getEngineEnvContext() {
        return engineEnvContext;
    }

    /**
     * Returns engine-level threading settings.
     *
     * @return threading service
     */
    public ThreadingService getThreadingService() {
        return threadingService;
    }

    /**
     * Destroy services.
     */
    public void destroy() {
        if (exprDeclaredService != null) {
            exprDeclaredService.destroy();
        }
        if (dataFlowService != null) {
            dataFlowService.destroy();
        }
        if (variableService != null) {
            variableService.destroy();
        }
        if (metricsReportingService != null) {
            metricsReportingService.destroy();
        }
        if (threadingService != null) {
            threadingService.destroy();
        }
        if (statementLifecycleSvc != null) {
            statementLifecycleSvc.destroy();
        }
        if (filterService != null) {
            filterService.destroy();
        }
        if (schedulingService != null) {
            schedulingService.destroy();
        }
        if (schedulingMgmtService != null) {
            schedulingMgmtService.destroy();
        }
        if (streamFactoryService != null) {
            streamFactoryService.destroy();
        }
        if (namedWindowMgmtService != null) {
            namedWindowMgmtService.destroy();
        }
        if (namedWindowDispatchService != null) {
            namedWindowDispatchService.destroy();
        }
        if (engineLevelExtensionServicesContext != null) {
            engineLevelExtensionServicesContext.destroy();
        }
        if (statementIsolationService != null) {
            statementIsolationService.destroy();
        }
        if (deploymentStateService != null) {
            deploymentStateService.destroy();
        }
    }

    /**
     * Destroy services.
     */
    public void initialize() {
        this.statementLifecycleSvc = null;
        this.engineURI = null;
        this.schedulingService = null;
        this.eventAdapterService = null;
        this.engineImportService = null;
        this.engineSettingsService = null;
        this.databaseConfigService = null;
        this.filterService = null;
        this.timerService = null;
        this.dispatchService = null;
        this.viewService = null;
        this.streamFactoryService = null;
        this.plugInViews = null;
        this.statementLockFactory = null;
        this.engineLevelExtensionServicesContext = null;
        this.engineEnvContext = null;
        this.statementContextFactory = null;
        this.plugInPatternObjects = null;
        this.namedWindowMgmtService = null;
        this.valueAddEventService = null;
        this.metricsReportingService = null;
        this.statementEventTypeRef = null;
        this.threadingService = null;
        this.expressionResultCacheSharable = null;
    }

    /**
     * }
     * <p>
     * public ExpressionResultCacheService getExpressionResultCacheSharable() {
     * return expressionResultCacheSharable;
     * Returns the factory to use for creating a statement context.
     *
     * @return statement context factory
     */
    public StatementContextFactory getStatementContextFactory() {
        return statementContextFactory;
    }

    /**
     * Returns the engine URI.
     *
     * @return engine URI
     */
    public String getEngineURI() {
        return engineURI;
    }

    /**
     * Returns engine settings.
     *
     * @return settings
     */
    public EngineSettingsService getEngineSettingsService() {
        return engineSettingsService;
    }

    /**
     * Returns the named window management service.
     *
     * @return service for managing named windows
     */
    public NamedWindowMgmtService getNamedWindowMgmtService() {
        return namedWindowMgmtService;
    }

    /**
     * Returns the variable service.
     *
     * @return variable service
     */
    public VariableService getVariableService() {
        return variableService;
    }

    /**
     * Returns the time source provider class.
     *
     * @return time source
     */
    public TimeSourceService getTimeSource() {
        return timeSourceService;
    }

    /**
     * Returns the service for handling updates to events.
     *
     * @return revision service
     */
    public ValueAddEventService getValueAddEventService() {
        return valueAddEventService;
    }

    /**
     * Returns metrics reporting.
     *
     * @return metrics reporting
     */
    public MetricReportingServiceSPI getMetricsReportingService() {
        return metricsReportingService;
    }

    /**
     * Returns service for statement to event type mapping.
     *
     * @return statement-type mapping
     */
    public StatementEventTypeRef getStatementEventTypeRefService() {
        return statementEventTypeRef;
    }

    /**
     * Returns the configuration.
     *
     * @return configuration
     */
    public ConfigurationInformation getConfigSnapshot() {
        return configSnapshot;
    }

    /**
     * Service for keeping track of variable-statement use.
     *
     * @return svc
     */
    public StatementVariableRef getStatementVariableRefService() {
        return statementVariableRef;
    }

    /**
     * Returns the schedule management service.
     *
     * @return schedule management service
     */
    public SchedulingMgmtService getSchedulingMgmtService() {
        return schedulingMgmtService;
    }

    /**
     * Returns the service for maintaining statement isolation information.
     *
     * @return isolation service
     */
    public StatementIsolationService getStatementIsolationService() {
        return statementIsolationService;
    }

    /**
     * Sets the service for maintaining statement isolation information.
     *
     * @param statementIsolationService isolation service
     */
    public void setStatementIsolationService(StatementIsolationService statementIsolationService) {
        this.statementIsolationService = statementIsolationService;
    }

    public DeploymentStateService getDeploymentStateService() {
        return deploymentStateService;
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return exceptionHandlingService;
    }

    public EventTypeIdGenerator getEventTypeIdGenerator() {
        return eventTypeIdGenerator;
    }

    public StatementMetadataFactory getStatementMetadataFactory() {
        return statementMetadataFactory;
    }

    public ContextManagementService getContextManagementService() {
        return contextManagementService;
    }

    public PatternSubexpressionPoolEngineSvc getPatternSubexpressionPoolSvc() {
        return patternSubexpressionPoolSvc;
    }

    public MatchRecognizeStatePoolEngineSvc getMatchRecognizeStatePoolEngineSvc() {
        return matchRecognizeStatePoolEngineSvc;
    }

    public DataFlowService getDataFlowService() {
        return dataFlowService;
    }

    public ExprDeclaredService getExprDeclaredService() {
        return exprDeclaredService;
    }

    public ExpressionResultCacheService getExpressionResultCacheSharable() {
        return expressionResultCacheSharable;
    }

    public TableService getTableService() {
        return tableService;
    }

    public ContextControllerFactoryFactorySvc getContextControllerFactoryFactorySvc() {
        return contextControllerFactoryFactorySvc;
    }

    public ContextManagerFactoryService getContextManagerFactoryService() {
        return contextManagerFactoryService;
    }

    public EPStatementFactory getEpStatementFactory() {
        return epStatementFactory;
    }

    public RegexHandlerFactory getRegexHandlerFactory() {
        return regexHandlerFactory;
    }

    public ViewableActivatorFactory getViewableActivatorFactory() {
        return viewableActivatorFactory;
    }

    public FilterNonPropertyRegisteryService getFilterNonPropertyRegisteryService() {
        return filterNonPropertyRegisteryService;
    }

    public NamedWindowDispatchService getNamedWindowDispatchService() {
        return namedWindowDispatchService;
    }

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return resultSetProcessorHelperFactory;
    }

    public ViewServicePreviousFactory getViewServicePreviousFactory() {
        return viewServicePreviousFactory;
    }

    public EventTableIndexService getEventTableIndexService() {
        return eventTableIndexService;
    }

    public EPRuntimeIsolatedFactory getEpRuntimeIsolatedFactory() {
        return epRuntimeIsolatedFactory;
    }

    public FilterBooleanExpressionFactory getFilterBooleanExpressionFactory() {
        return filterBooleanExpressionFactory;
    }

    public DataCacheFactory getDataCacheFactory() {
        return dataCacheFactory;
    }

    public MultiMatchHandlerFactory getMultiMatchHandlerFactory() {
        return multiMatchHandlerFactory;
    }

    public NamedWindowConsumerMgmtService getNamedWindowConsumerMgmtService() {
        return namedWindowConsumerMgmtService;
    }

    public AggregationFactoryFactory getAggregationFactoryFactory() {
        return aggregationFactoryFactory;
    }
}
